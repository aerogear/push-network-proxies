package org.jboss.aerogear.proxy.command;

import java.net.UnknownHostException;

import org.jboss.aerogear.proxy.apns.ApnsServerSimulator;
import org.jboss.aerogear.proxy.apns.ApnsSocketFactory;
import org.jboss.aerogear.proxy.endpoint.NotificationRegisterEndpoint;

import io.airlift.airline.Command;
import io.airlift.airline.Option;

/**
 *
 * @author <a href="mailto:miklosovic@gmail.com>Stefan Miklosovic</a>
 */
@Command(name = "apnsProxy", description = "starts APNS proxy")
public class ApnsProxyCommand extends NotificationRegisterEndpoint {

    // simulator related

    @Option(name = "--apnsMockGatewayHost", description = "defaults to 127.0.0.1")
    private String apnsMockGatewayHost = "127.0.0.1";

    @Option(name = "--apnsMockGatewayPort", description = "defaults to 16002")
    private int apnsMockGatewayPort = 16002;

    @Option(name = "--apnsMockFeedbackHost", description = "defaults to 127.0.0.1")
    private String apnsMockFeedbackHost = "127.0.0.1";

    @Option(name = "--apnsMockFeedbackPort", description = "defaults to 16003")
    private int apnsMockFeedbackPort = 16003;

    // Certificate related

    @Option(name = "--apnsKeystore", description = "defaults to serverStore.p12 loaded from the jar")
    private String apnsKeystore;

    @Option(name = "--apnsKeystorePassword", description = "defaults to 123456")
    private String apnsKeystorePassword = "123456";

    @Option(name = "--apnsKeystoreType", description = "defaults to PKCS12")
    private String apnsKeystoreType = "PKCS12";

    @Option(name = "--apnsKeystoreAlgorithm", description = "defaults to sunx509")
    private String apnsKeystoreAlgorithm = "sunx509";

    private String resourceServerStore = "serverStore.p12";

    @Override
    public void run() {

        startNotificationRegisterEndpoint(notificationEndpointHost, notificationEndpointPort);

        ApnsSocketFactory apnsSocketFactory = new ApnsSocketFactory.Builder()
            .withApnsKeystore(apnsKeystore)
            .withApnsKeystoreAlgorithm(apnsKeystoreAlgorithm)
            .withApnsKeystorePassword(apnsKeystorePassword)
            .withApnsKeystoreType(apnsKeystoreType)
            .withResourceServerStore(resourceServerStore)
            .build();

        ApnsServerSimulator apnsServerSimulator;

        try {
            apnsServerSimulator = new ApnsServerSimulator(
                apnsSocketFactory.build(),
                apnsMockGatewayHost,
                apnsMockGatewayPort,
                apnsMockFeedbackHost,
                apnsMockFeedbackPort);
        } catch (UnknownHostException ex) {
            throw new IllegalStateException("Unable to instantiate APNS server simulator.", ex);
        }

        Runtime.getRuntime().addShutdownHook(new ApnsProxyShutdownHook(apnsServerSimulator, this));

        apnsServerSimulator.start();
    }

    private static class ApnsProxyShutdownHook extends Thread {

        private final ApnsServerSimulator apnsServerSimulator;

        private final ApnsProxyCommand apnsProxyCommand;

        public ApnsProxyShutdownHook(final ApnsServerSimulator apnsServerSimulator, ApnsProxyCommand apnsProxyCommand) {
            this.apnsServerSimulator = apnsServerSimulator;
            this.apnsProxyCommand = apnsProxyCommand;
        }

        @Override
        public void run() {
            if (apnsServerSimulator.isStarted()) {
                apnsServerSimulator.stop();
            }

            apnsProxyCommand.stopNotificationRegisterEndpoint();
        }
    }
}
