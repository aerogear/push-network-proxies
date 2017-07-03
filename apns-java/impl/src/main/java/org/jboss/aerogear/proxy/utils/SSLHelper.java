package org.jboss.aerogear.proxy.utils;

import java.io.InputStream;
import java.security.GeneralSecurityException;
import java.security.KeyStore;

import javax.net.ssl.KeyManagerFactory;
import javax.net.ssl.SSLContext;
import javax.net.ssl.SSLServerSocketFactory;
import javax.net.ssl.TrustManagerFactory;

public class SSLHelper {

    public static SSLServerSocketFactory newSSLSocketFactory(final InputStream cert, final String password,
        final String ksType, final String ksAlgorithm) throws InvalidSSLConfig {
        final SSLContext context = newSSLContext(cert, password, ksType, ksAlgorithm);
        return context.getServerSocketFactory();
    }

    public static SSLContext newSSLContext(final InputStream cert, final String password,
        final String ksType, final String ksAlgorithm) throws InvalidSSLConfig {
        try {
            final KeyStore ks = KeyStore.getInstance(ksType);
            ks.load(cert, password.toCharArray());
            return newSSLContext(ks, password, ksAlgorithm);
        } catch (final Exception e) {
            throw new InvalidSSLConfig(e);
        }
    }

    public static SSLContext newSSLContext(final KeyStore ks, final String password,
        final String ksAlgorithm) throws InvalidSSLConfig {
        try {
            // Get a KeyManager and initialize it
            final KeyManagerFactory kmf = KeyManagerFactory.getInstance(ksAlgorithm);
            kmf.init(ks, password.toCharArray());

            // Get a TrustManagerFactory with the DEFAULT KEYSTORE, so we have all the certificates in cacerts trusted
            final TrustManagerFactory tmf = TrustManagerFactory.getInstance(ksAlgorithm);
            tmf.init((KeyStore) null);

            // Get the SSLContext to help create SSLSocketFactory
            final SSLContext sslContext = SSLContext.getInstance("TLS");
            sslContext.init(kmf.getKeyManagers(), tmf.getTrustManagers(), null);
            return sslContext;
        } catch (final GeneralSecurityException e) {
            throw new InvalidSSLConfig(e);
        }
    }

    /**
     * Signals that the the provided SSL context settings (e.g. keystore path, password, encryption type, etc) are invalid
     *
     * This Exception can be caused by any of the following:
     *
     * <ol>
     * <li>{@link KeyStoreException}</li>
     * <li>{@link NoSuchAlgorithmException}</li>
     * <li>{@link CertificateException}</li>
     * <li>{@link IOException}</li>
     * <li>{@link UnrecoverableKeyException}</li>
     * <li>{@link KeyManagementException}</li>
     * </ol>
     */
    public static class InvalidSSLConfig extends RuntimeException {

        private static final long serialVersionUID = 397721152145385451L;

        public InvalidSSLConfig() {
            super();
        }

        public InvalidSSLConfig(String message, Throwable cause) {
            super(message, cause);
        }

        public InvalidSSLConfig(String message) {
            super(message);
        }

        public InvalidSSLConfig(Throwable cause) {
            super(cause);
        }
    }

}
