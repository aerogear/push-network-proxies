package org.jboss.aerogear.proxy.command;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.security.KeyStore;
import java.security.KeyStoreException;
import java.security.cert.X509Certificate;
import java.util.Random;

import com.relayrides.pushy.apns.MockApnsServer;
import com.relayrides.pushy.apns.MockApnsServerBuilder;
import io.netty.channel.nio.NioEventLoopGroup;
import org.jboss.aerogear.proxy.endpoint.NotificationRegisterEndpoint;

import io.airlift.airline.Command;
import io.airlift.airline.Option;
import org.jboss.aerogear.proxy.utils.P12Util;
import org.jboss.aerogear.proxy.utils.SSLHelper;

/**
 *
 * @author <a href="mailto:miklosovic@gmail.com>Stefan Miklosovic</a>
 */
@Command(name = "apnsProxy", description = "starts APNS proxy")
public class ApnsProxyCommand extends NotificationRegisterEndpoint {

    private static final int TOKEN_LENGTH = 32;

    // simulator related

    @Option(name = "--apnsMockGatewayHost", description = "defaults to 127.0.0.1")
    private String apnsMockGatewayHost = "127.0.0.1";

    @Option(name = "--apnsMockGatewayPort", description = "defaults to 16002")
    private int apnsMockGatewayPort = 18443;

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

        try {
            final KeyStore.PrivateKeyEntry privateKeyEntry = P12Util.getFirstPrivateKeyEntryFromP12InputStream(
                    getInputStream(), apnsKeystorePassword);

            final MockApnsServerBuilder serverBuilder = new MockApnsServerBuilder()
                    .setServerCredentials(new X509Certificate[] { (X509Certificate) privateKeyEntry.getCertificate() }, privateKeyEntry.getPrivateKey(), null)
                    .setEventLoopGroup(new NioEventLoopGroup(4));

            final MockApnsServer server = serverBuilder.build();

            server.registerDeviceTokenForTopic("org.aerogear.test", generateRandomToken(), null);

            server.start(apnsMockGatewayPort).await();


        } catch (KeyStoreException | InterruptedException | IOException e) {
            e.printStackTrace();
        }

    }

    private InputStream getInputStream() {
        InputStream stream;
        try {
            File externalApnsCertificateFile = (apnsKeystore == null ? null : new File(apnsKeystore));
            if (externalApnsCertificateFile != null) {
                stream = new FileInputStream(externalApnsCertificateFile);
            } else {
                stream = SSLHelper.class.getResourceAsStream("/" + resourceServerStore);
            }
            assert stream != null;

            return stream;

        } catch (Exception ex) {
            throw new RuntimeException("Unable to build Keystore file", ex.getCause());
        }
    }

    private String generateRandomToken() {
        final byte[] tokenBytes = new byte[TOKEN_LENGTH];
        new Random().nextBytes(tokenBytes);

        final StringBuilder builder = new StringBuilder(TOKEN_LENGTH * 2);

        for (final byte b : tokenBytes) {
            builder.append(String.format("%02x", b));
        }

        return builder.toString();
    }
}
