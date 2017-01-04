package org.jboss.aerogear.proxy.command;

import java.io.File;
import java.util.logging.Level;
import java.util.logging.Logger;

import org.jboss.aerogear.proxy.endpoint.NotificationRegisterEndpoint;
import org.jboss.aerogear.proxy.gcm.MockingGCMProxyServer;
import org.littleshoot.proxy.HttpProxyServer;

import org.jboss.aerogear.proxy.gcm.MockingGCMServerBackgroundThread;

import io.airlift.airline.Command;
import io.airlift.airline.Option;

/**
 *
 * @author <a href="mailto:miklosovic@gmail.com>Stefan Miklosovic</a>
 */
@Command(name = "gcmProxy", description = "starts GCM proxy")
public class GCMProxyCommand extends NotificationRegisterEndpoint implements Runnable {

    private static final Logger logger = Logger.getLogger(GCMProxyCommand.class.getName());

    @Option(name = "--httpProxyHost", description = "defaults to 127.0.0.1")
    private String httpProxyHost = "127.0.0.1";

    @Option(name = "--httpProxyPort", description = "defaults to 16000")
    private int httpProxyPort = 16000;

    @Option(name = "--gcmMockServerHost", description = "defaults to 127.0.0.1")
    private String gcmMockServerHost = "127.0.0.1";

    @Option(name = "--gcmMockServerPort", description = "defaults to 16001")
    private int gcmMockServerPort = 16001;

    @Option(name = "--gcmCertificate", required = true)
    private String gcmCertificate;

    @Option(name = "--gcmCertificateKey", required = true)
    private String gcmCertificateKey;

    public void run() {

        validate();

        startNotificationRegisterEndpoint(notificationEndpointHost, notificationEndpointPort);

        MockingGCMServerBackgroundThread backgroundThread = new MockingGCMServerBackgroundThread(gcmMockServerHost,
            gcmMockServerPort,
            new File(gcmCertificate),
            new File(gcmCertificateKey));

        backgroundThread.start();

        logger.log(Level.INFO, "Background thread started in GCMProxyCommand");

        HttpProxyServer server = new MockingGCMProxyServer.Builder()
            .withHost(httpProxyHost)
            .withPort(httpProxyPort)
            .withMockServerHost(gcmMockServerHost)
            .withMockServerPort(gcmMockServerPort)
            .build()
            .start();

        logger.log(Level.INFO, "Proxy server started in GCMProxyCommand");

        Runtime.getRuntime().addShutdownHook(new ServerCleanupThread(server, backgroundThread, this));
    }

    private final static class ServerCleanupThread extends Thread {

        private static final Logger logger = Logger.getLogger(ServerCleanupThread.class.getName());

        private final HttpProxyServer proxyServer;

        private MockingGCMServerBackgroundThread backgroundThread;

        private GCMProxyCommand command;

        public ServerCleanupThread(HttpProxyServer proxyServer, MockingGCMServerBackgroundThread backgroundThread, GCMProxyCommand command) {
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
        File certificateFile = new File(gcmCertificate);
        File certificateKeyFile = new File(gcmCertificateKey);

        if (!certificateFile.exists() || !certificateFile.isFile() || !certificateFile.canRead()) {
            throw new IllegalArgumentException("GCM certificate file " + gcmCertificate + " does not exist "
                + "or it is not a file or it can not be read");
        }

        if (!certificateKeyFile.exists() || !certificateKeyFile.isFile() || !certificateKeyFile.canRead()) {
            throw new IllegalArgumentException("GCM certificate key file " + gcmCertificateKey + " does not exist "
                + "or it is not a file or it can not be read");
        }
    }
}
