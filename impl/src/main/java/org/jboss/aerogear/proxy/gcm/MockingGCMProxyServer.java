package org.jboss.aerogear.proxy.gcm;

import io.netty.handler.codec.http.HttpObject;
import io.netty.handler.codec.http.HttpRequest;
import io.netty.handler.codec.http.HttpResponse;

import java.net.InetSocketAddress;
import org.littleshoot.proxy.HttpFilters;
import org.littleshoot.proxy.HttpFiltersAdapter;
import org.littleshoot.proxy.HttpFiltersSourceAdapter;
import org.littleshoot.proxy.HttpProxyServer;
import org.littleshoot.proxy.impl.DefaultHttpProxyServer;

public class MockingGCMProxyServer {

    private String host;

    private int port;

    private String mockServer;

    private int mockPort;

    private MockingGCMProxyServer(Builder builder) {
        host = builder.host;
        port = builder.port;
        mockServer = builder.mockServer;
        mockPort = builder.mockPort;
    }

    public HttpProxyServer start() {

        return DefaultHttpProxyServer.bootstrap()
            .withAddress(new InetSocketAddress(host, port))
            .withFiltersSource(new HttpFiltersSourceAdapter() {

                @Override
                public HttpFilters filterRequest(HttpRequest originalRequest) {

                    return new HttpFiltersAdapter(originalRequest) {

                        final String mockedAddress = mockServer + ":" + mockPort;

                        @Override
                        public HttpResponse clientToProxyRequest(HttpObject httpObject) {

                            final HttpRequest request = (HttpRequest) httpObject;

                            if (request.getUri().contains("google")) {
                                request.setUri(mockedAddress);
                            }

                            super.clientToProxyRequest(request);

                            return null;
                        }
                    };
                }
            }).start();
    }

    public static class Builder {

        private String host;

        private int port;

        private String mockServer;

        private int mockPort;

        public Builder withHost(String host) {
            this.host = host;
            return this;
        }

        public Builder withPort(int port) {
            this.port = port;
            return this;
        }

        public Builder withMockServerHost(String mockServer) {
            this.mockServer = mockServer;
            return this;
        }

        public Builder withMockServerPort(int mockPort) {
            this.mockPort = mockPort;
            return this;
        }

        public MockingGCMProxyServer build() {
            return new MockingGCMProxyServer(this);
        }
    }
}
