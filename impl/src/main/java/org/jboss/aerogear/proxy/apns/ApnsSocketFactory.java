package org.jboss.aerogear.proxy.apns;

import java.io.File;
import java.io.FileInputStream;
import java.io.InputStream;

import javax.net.ssl.SSLServerSocketFactory;

import org.jboss.aerogear.proxy.utils.SSLHelper;

/**
 *
 * @author <a href="mailto:miklosovic@gmail.com>Stefan Miklosovic</a>
 */
public class ApnsSocketFactory {

    private String apnsKeystore;

    private String apnsKeystorePassword;

    private String apnsKeystoreType;

    private String apnsKeystoreAlgorithm;

    private String resourceServerStore;

    private ApnsSocketFactory(Builder builder) {
        apnsKeystore = builder.apnsKeystore;
        apnsKeystorePassword = builder.apnsKeystorePassword;
        apnsKeystoreType = builder.apnsKeystoreType;
        apnsKeystoreAlgorithm = builder.apnsKeystoreAlgorithm;
        resourceServerStore = builder.resourceServerStore;
    }

    public SSLServerSocketFactory build() {
        try {
            InputStream stream;
            File externalApnsCertificateFile = (apnsKeystore == null ? null : new File(apnsKeystore));
            if (externalApnsCertificateFile != null) {
                stream = new FileInputStream(externalApnsCertificateFile);
            } else {
                stream = SSLHelper.class.getResourceAsStream("/" + resourceServerStore);
            }
            assert stream != null;
            return SSLHelper.newSSLSocketFactory(stream,
                apnsKeystorePassword,
                apnsKeystoreType,
                apnsKeystoreAlgorithm);
        } catch (Exception ex) {
            throw new RuntimeException("Unable to build SSLServerSocketFactory", ex.getCause());
        }
    }

    public static class Builder {

        private String resourceServerStore;
        private String apnsKeystoreAlgorithm;
        private String apnsKeystoreType;
        private String apnsKeystorePassword;
        private String apnsKeystore;

        public Builder withResourceServerStore(String resourceServerStore) {
            this.resourceServerStore = resourceServerStore;
            return this;
        }

        public Builder withApnsKeystoreAlgorithm(String apnsKeystoreAlgorithm) {
            this.apnsKeystoreAlgorithm = apnsKeystoreAlgorithm;
            return this;
        }

        public Builder withApnsKeystoreType(String apnsKeystoreType) {
            this.apnsKeystoreType = apnsKeystoreType;
            return this;
        }

        public Builder withApnsKeystorePassword(String apnsKeystorePassword) {
            this.apnsKeystorePassword = apnsKeystorePassword;
            return this;
        }

        public Builder withApnsKeystore(String apnsKeystore) {
            this.apnsKeystore = apnsKeystore;
            return this;
        }

        public ApnsSocketFactory build() {
            return new ApnsSocketFactory(this);
        }
    }
}
