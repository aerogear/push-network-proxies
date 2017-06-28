package org.jboss.aerogear.proxy.command;

import java.io.File;
import java.util.logging.Level;
import java.util.logging.Logger;

import org.jboss.aerogear.proxy.endpoint.NotificationRegisterEndpoint;
import org.jboss.aerogear.proxy.fcm.MockingFCMProxyServer;
import org.littleshoot.proxy.HttpProxyServer;

import org.jboss.aerogear.proxy.fcm.MockingFCMServerBackgroundThread;

import io.airlift.airline.Command;
import io.airlift.airline.Option;

/**
 *
 * @author <a href="mailto:miklosovic@gmail.com>Stefan Miklosovic</a>
 */
@Command(name = "fcmProxy", description = "starts FCM proxy")
public class FCMProxyCommand extends NotificationRegisterEndpoint implements Runnable {

    private static final Logger logger = Logger.getLogger(FCMProxyCommand.class.getName());

    @Option(name = "--httpProxyHost", description = "defaults to 127.0.0.1")
    private String httpProxyHost = "127.0.0.1";

    @Option(name = "--httpProxyPort", description = "defaults to 16000")
    private int httpProxyPort = 16000;

    @Option(name = "--fcmMockServerHost", description = "defaults to 127.0.0.1")
    private String fcmMockServerHost = "127.0.0.1";

    @Option(name = "--fcmMockServerPort", description = "defaults to 16001")
    private int fcmMockServerPort = 16001;

    @Option(name = "--fcmCertificate", required = true)
    private String fcmCertificate;

    @Option(name = "--fcmCertificateKey", required = true)
    private String fcmCertificateKey;

    public void run() {

        validate();

        startNotificationRegisterEndpoint(notificationEndpointHost, notificationEndpointPort);

        MockingFCMServerBackgroundThread backgroundThread =
            new MockingFCMServerBackgroundThread(
                fcmMockServerHost,
                fcmMockServerPort,
                new File(fcmCertificate),
                new File(fcmCertificateKey));

        backgroundThread.start();

        logger.log(Level.INFO, "Background thread started in FCMProxyCommand");

        HttpProxyServer server = new MockingFCMProxyServer.Builder()
            .withHost(httpProxyHost)
            .withPort(httpProxyPort)
            .withMockServerHost(fcmMockServerHost)
            .withMockServerPort(fcmMockServerPort)
            .build()
            .start();

        logger.log(Level.INFO, "Proxy server started in FCMProxyCommand");

        Runtime.getRuntime().addShutdownHook(new ServerCleanupThread(server, backgroundThread, this));
    }

    private final static class ServerCleanupThread extends Thread {

        private static final Logger logger = Logger.getLogger(ServerCleanupThread.class.getName());

        private final HttpProxyServer proxyServer;

        private MockingFCMServerBackgroundThread backgroundThread;

        private FCMProxyCommand command;

        public ServerCleanupThread(HttpProxyServer proxyServer, MockingFCMServerBackgroundThread backgroundThread, FCMProxyCommand command) {
            this.proxyServer = proxyServer;
            this.backgroundThread = backgroundThread;
            this.command = command;
        }

        @Override
        public void run() {
            if (proxyServer != null) {
                proxyServer.stop();
                logger.log(Level.INFO, "Proxy server stopped.");
            }

            if (backgroundThread != null) {
                if (backgroundThread.isAlive() && !backgroundThread.isInterrupted()) {
                    backgroundThread.closeChannel();
                    backgroundThread.interrupt();
                    logger.log(Level.INFO, "Background thread interrupted in ServerCleanupThread.");
                }
                backgroundThread = null;
            }

            command.stopNotificationRegisterEndpoint();
        }
    }

    private void validate() {
        File certificateFile = new File(fcmCertificate);
        File certificateKeyFile = new File(fcmCertificateKey);

        if (!certificateFile.exists() || !certificateFile.isFile() || !certificateFile.canRead()) {
            throw new IllegalArgumentException("FCM certificate file " + fcmCertificate + " does not exist "
                + "or it is not a file or it can not be read");
        }

        if (!certificateKeyFile.exists() || !certificateKeyFile.isFile() || !certificateKeyFile.canRead()) {
            throw new IllegalArgumentException("FCM certificate key file " + fcmCertificateKey + " does not exist "
                + "or it is not a file or it can not be read");
        }
    }
}
