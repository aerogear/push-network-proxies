package org.jboss.aerogear.proxy.apns;

import javax.net.ServerSocketFactory;

import org.jboss.aerogear.proxy.endpoint.model.ApnsNotification;
import org.jboss.aerogear.proxy.utils.Encoders;
import org.jboss.aerogear.proxy.utils.Tokens;

import java.io.ByteArrayOutputStream;
import java.io.DataOutputStream;
import java.io.EOFException;
import java.io.IOException;
import java.net.InetAddress;
import java.net.ServerSocket;
import java.net.SocketException;
import java.net.UnknownHostException;
import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.Semaphore;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.logging.Level;
import java.util.logging.Logger;

public class ApnsServerSimulator {

    private static final Logger logger = Logger.getLogger(ApnsServerSimulator.class.getName());

    private static AtomicInteger threadNameCount = new AtomicInteger(0);

    private final Semaphore startUp = new Semaphore(0);
    private final ServerSocketFactory sslFactory;

    private final InetAddress gatewayHost;
    private final int gatewayPort;
    private final InetAddress feedbackHost;
    private final int feedbackPort;

    private int effectiveGatewayPort;
    private int effectiveFeedbackPort;

    private final List<byte[]> badTokens = new ArrayList<byte[]>();

    private boolean started = false;

    public ApnsServerSimulator(ServerSocketFactory sslFactory,
        String gatewayHost, int gatewayPort,
        String feedbackHost, int feedbackPort) throws UnknownHostException {
        this.sslFactory = sslFactory;

        this.gatewayHost = InetAddress.getByName(gatewayHost);
        this.gatewayPort = gatewayPort;
        this.feedbackHost = InetAddress.getByName(feedbackHost);
        this.feedbackPort = feedbackPort;
    }

    Thread gatewayThread;
    Thread feedbackThread;
    ServerSocket gatewaySocket;
    ServerSocket feedbackSocket;

    public void start() {
        logger.info("Starting ApnsServerSimulator");
        gatewayThread = new GatewayListener();
        feedbackThread = new FeedbackRunner();
        gatewayThread.start();
        feedbackThread.start();
        startUp.acquireUninterruptibly(2);

        started = true;
    }

    public void stop() {
        logger.info("Stopping ApnsServerSimulator");

        try {
            if (gatewaySocket != null) {
                gatewaySocket.close();
            }
        } catch (IOException e) {
            e.printStackTrace();
        }

        try {
            if (feedbackSocket != null) {
                feedbackSocket.close();
            }
        } catch (IOException e) {
            e.printStackTrace();
        }

        if (gatewayThread != null) {
            gatewayThread.interrupt();
        }

        if (feedbackThread != null) {
            feedbackThread.interrupt();
        }
    }

    public boolean isStarted() {
        return started;
    }

    public int getEffectiveGatewayPort() {
        return effectiveGatewayPort;
    }

    public int getEffectiveFeedbackPort() {
        return effectiveFeedbackPort;
    }

    protected void fail(final byte status, final int identifier, final InputOutputSocket inputOutputSocket) throws IOException {
        logger.log(Level.WARNING, String.format("%s - %s", status, identifier));

        // Here comes the fun ... we need to write the feedback packet as one single packet
        // or the client will notice the connection to be closed before it read the complete packet.
        // But - only on linux, however. (I was not able to see that problem on Windows 7 or OS X)
        // What also helped was inserting a little sleep between the flush and closing the connection.
        //
        // I believe this is irregular (writing to a tcp socket then closing it should result in ALL data
        // being visible at the client) but interestingly in Netty there is (was) a similar problem:
        // https://github.com/netty/netty/issues/1952
        //
        // Funnily that appeared as somebody ported this library to use netty.
        ByteBuffer bb = ByteBuffer.allocate(6);
        bb.put((byte) 8);
        bb.put(status);
        bb.putInt(identifier);
        inputOutputSocket.syncWrite(bb.array());
        inputOutputSocket.close();
        logger.warning("FAIL - closed");
    }

    protected void onNotification(final ApnsNotification notification, final InputOutputSocket inputOutputSocket) throws IOException {
        ApnsNotificationRegister.addNotification(notification);
    }

    protected List<byte[]> getBadTokens() {
        synchronized (badTokens) {
            List<byte[]> result = new ArrayList<byte[]>(badTokens);
            badTokens.clear();
            return result;
        }
    }

    private class GatewayListener extends Thread {

        private GatewayListener() {
            super(new ThreadGroup("GatewayListener" + threadNameCount.incrementAndGet()), "");
            setName(getThreadGroup().getName());
        }

        public void run() {
            logger.info("Launched " + Thread.currentThread().getName());
            try {
                try {
                    gatewaySocket = sslFactory.createServerSocket(gatewayPort, 0, gatewayHost);
                } catch (IOException e) {
                    e.printStackTrace();
                    throw new RuntimeException(e);
                }

                effectiveGatewayPort = gatewaySocket.getLocalPort();

                startUp.release();

                logger.info("GatewayListener listening to connections");

                while (!isInterrupted()) {
                    try {
                        logger.info("Gateway socket accepting connection");
                        handleGatewayConnection(new InputOutputSocket(gatewaySocket.accept()));
                    } catch (SocketException ex) {
                        interrupt();
                    } catch (IOException ioe) {
                        ioe.printStackTrace();
                    }
                }
            } finally {
                logger.info("Terminating " + Thread.currentThread().getName());
                getThreadGroup().list();
                getThreadGroup().interrupt();
            }
        }

        private void handleGatewayConnection(final InputOutputSocket inputOutputSocket) throws IOException {
            Thread gatewayConnectionTread = new Thread() {
                @Override
                public void run() {
                    try {
                        parseNotifications(inputOutputSocket);
                    } finally {
                        inputOutputSocket.close();
                    }
                }
            };
            gatewayConnectionTread.start();
        }

        private void parseNotifications(final InputOutputSocket inputOutputSocket) {
            logger.info(String.format("Running parseNotifications %s", inputOutputSocket.getSocket()));

            while (!Thread.interrupted()) {
                try {
                    final ApnsInputStream inputStream = inputOutputSocket.getInputStream();
                    byte notificationType = inputStream.readByte();

                    logger.info(String.format("Received Notification (type %s)", notificationType));

                    switch (notificationType) {
                        case 0:
                            readLegacyNotification(inputOutputSocket);
                            break;
                        case 1:
                            readEnhancedNotification(inputOutputSocket);
                            break;
                        case 2:
                            readFramedNotifications(inputOutputSocket);
                            break;
                    }
                } catch (IOException ioe) {
                    Thread.currentThread().interrupt();
                }
            }
        }

        private void readFramedNotifications(final InputOutputSocket inputOutputSocket) throws IOException {

            Map<Byte, ApnsInputStream.Item> map = new HashMap<Byte, ApnsInputStream.Item>();

            ApnsInputStream frameStream = inputOutputSocket.getInputStream().readFrame();
            try {
                while (!Thread.currentThread().isInterrupted()) {
                    final ApnsInputStream.Item item = frameStream.readItem();
                    map.put(item.getItemId(), item);
                }
            } catch (EOFException eof) {
                // Done reading.
            }

            byte[] deviceToken = get(map, ApnsInputStream.Item.ID_DEVICE_TOKEN).getBlob();
            byte[] payload = get(map, ApnsInputStream.Item.ID_PAYLOAD).getBlob();
            int identifier = get(map, ApnsInputStream.Item.ID_NOTIFICATION_IDENTIFIER).getInt();
            int expiry = get(map, ApnsInputStream.Item.ID_EXPIRATION_DATE).getInt();
            byte priority = get(map, ApnsInputStream.Item.ID_PRIORITY).getByte();

            final ApnsNotification notification = new ApnsNotification(2, identifier, expiry,
                Encoders.encodeHex(deviceToken).toLowerCase(), Encoders.encodeHex(payload), priority);

            logger.info(String.format("Read framed notification %s", notification));

            resolveBadToken(deviceToken);

            onNotification(notification, inputOutputSocket);
        }

        private ApnsInputStream.Item get(final Map<Byte, ApnsInputStream.Item> map, final byte idDeviceToken) {
            ApnsInputStream.Item item = map.get(idDeviceToken);
            if (item == null) {
                item = ApnsInputStream.Item.DEFAULT;
            }
            return item;
        }

        private void readEnhancedNotification(final InputOutputSocket inputOutputSocket) throws IOException {
            ApnsInputStream inputStream = inputOutputSocket.getInputStream();

            int identifier = inputStream.readInt();
            int expiry = inputStream.readInt();
            final byte[] deviceToken = inputStream.readBlob();
            final byte[] payload = inputStream.readBlob();

            logger.info(new String(payload));

            final ApnsNotification notification = new ApnsNotification(1, identifier, expiry, Encoders.encodeHex(deviceToken).toLowerCase(), new String(payload));
            logger.info(String.format("Read enhanced notification %s", notification));

            resolveBadToken(deviceToken);

            onNotification(notification, inputOutputSocket);
        }

        private void readLegacyNotification(final InputOutputSocket inputOutputSocket) throws IOException {
            ApnsInputStream inputStream = inputOutputSocket.getInputStream();

            final byte[] deviceToken = inputStream.readBlob();
            final byte[] payload = inputStream.readBlob();
            final ApnsNotification notification = new ApnsNotification(0, Encoders.encodeHex(deviceToken).toLowerCase(), Encoders.encodeHex(payload));
            logger.info(String.format("Read legacy notification %s", notification));

            resolveBadToken(deviceToken);

            onNotification(notification, inputOutputSocket);
        }

        private void resolveBadToken(byte[] deviceToken) {
            synchronized (badTokens) {
                String encoded = Encoders.encodeHex(deviceToken);

                if (encoded.toLowerCase().startsWith(Tokens.TOKEN_INVALIDATION_PREFIX)) {
                    badTokens.add(deviceToken);
                }
            }
        }

        @Override
        public void interrupt() {
            logger.info("Interrupted, closing socket");
            super.interrupt();
            try {
                gatewaySocket.close();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }

    private class FeedbackRunner extends Thread {

        private FeedbackRunner() {
            super(new ThreadGroup("FeedbackRunner" + threadNameCount.incrementAndGet()), "");
            setName(getThreadGroup().getName());
        }

        public void run() {
            try {
                logger.info("Launched FeedbackRunner" + Thread.currentThread().getName());
                try {
                    feedbackSocket = sslFactory.createServerSocket(feedbackPort, 0, feedbackHost);
                } catch (IOException e) {
                    e.printStackTrace();
                    throw new RuntimeException(e);
                }

                effectiveFeedbackPort = feedbackSocket.getLocalPort();

                startUp.release();

                while (!isInterrupted()) {
                    try {
                        handleFeedbackConnection(new InputOutputSocket(feedbackSocket.accept()));
                    } catch (SocketException ex) {
                        interrupt();
                    } catch (IOException ioe) {
                        ioe.printStackTrace();
                    }
                }
            } finally {
                logger.info("Terminating FeedBack runner thread" + Thread.currentThread().getName());
                getThreadGroup().list();
                getThreadGroup().interrupt();
            }
        }

        private void handleFeedbackConnection(final InputOutputSocket inputOutputSocket) {
            Thread feedbackConnectionTread = new Thread() {
                @Override
                public void run() {
                    try {
                        logger.info("Feedback connection sending feedback");
                        sendFeedback(inputOutputSocket);
                    } catch (IOException ioe) {
                        // An exception is unexpected here. Close the current connection and bail out.
                        ioe.printStackTrace();
                    } finally {
                        inputOutputSocket.close();
                    }

                }
            };
            feedbackConnectionTread.start();
        }

        private void sendFeedback(final InputOutputSocket inputOutputSocket) throws IOException {
            List<byte[]> badTokens = getBadTokens();

            for (byte[] token : badTokens) {
                writeFeedback(inputOutputSocket, token);
            }

            // Write -1 to indicate a closing socket. This might be a workaround, I'm not sure if it should be done this way.
            inputOutputSocket.syncWrite(new byte[] { -1 });
        }

        private void writeFeedback(final InputOutputSocket inputOutputSocket, final byte[] token) throws IOException {
            ByteArrayOutputStream os = new ByteArrayOutputStream();
            DataOutputStream dos = new DataOutputStream(os);
            final int unixTime = (int) (new Date().getTime() / 1000);
            dos.writeInt(unixTime);
            dos.writeShort((short) token.length);
            dos.write(token);
            dos.close();
            inputOutputSocket.syncWrite(os.toByteArray());
        }
    }

}