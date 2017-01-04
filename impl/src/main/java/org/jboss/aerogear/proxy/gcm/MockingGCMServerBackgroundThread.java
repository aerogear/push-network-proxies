package org.jboss.aerogear.proxy.gcm;

import io.netty.bootstrap.ServerBootstrap;
import io.netty.channel.Channel;
import io.netty.channel.EventLoopGroup;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.nio.NioServerSocketChannel;
import io.netty.handler.logging.LogLevel;
import io.netty.handler.logging.LoggingHandler;
import io.netty.handler.ssl.SslContext;

import java.io.File;

import javax.net.ssl.SSLException;

public class MockingGCMServerBackgroundThread extends Thread {

    private final String gcmMockServerHost;

    private final int gcmMockServePort;

    private final File gcmCertificateFile;

    private final File gcmCertificateKeyFile;

    private Channel channel;

    public MockingGCMServerBackgroundThread(String gcmMockServerHost,
        int gcmMockServePort,
        File gcmCertificateFile,
        File gcmCertificateKeyFile) {

        this.gcmMockServerHost = gcmMockServerHost;
        this.gcmMockServePort = gcmMockServePort;
        this.gcmCertificateFile = gcmCertificateFile;
        this.gcmCertificateKeyFile = gcmCertificateKeyFile;
    }

    public String getGcmMockServerHost() {
        return gcmMockServerHost;
    }

    public int getGcmMockServePort() {
        return gcmMockServePort;
    }

    @Override
    public void run() {

        SslContext sslCtx = null;

        try {
            sslCtx = SslContext.newServerContext(gcmCertificateFile, gcmCertificateKeyFile);
        } catch (SSLException e) {
            e.printStackTrace();
        }

        // Configure the server.
        EventLoopGroup bossGroup = new NioEventLoopGroup(1);
        EventLoopGroup workerGroup = new NioEventLoopGroup();

        try {
            ServerBootstrap serverBootstrap = new ServerBootstrap();
            serverBootstrap.group(bossGroup, workerGroup)
                .channel(NioServerSocketChannel.class)
                .handler(new LoggingHandler(LogLevel.INFO))
                .childHandler(new MockingGCMServerInitializer(sslCtx));

            channel = serverBootstrap.bind(gcmMockServerHost, gcmMockServePort).sync().channel();

            channel.closeFuture().sync();
        } catch (InterruptedException e) {
            e.printStackTrace();
        } finally {
            bossGroup.shutdownGracefully();
            workerGroup.shutdownGracefully();
        }
    }

    public void closeChannel() {
        if (channel != null) {
            try {
                channel.close().get();
            } catch (Exception ex) {
                throw new RuntimeException("Error while closing a channel.", ex.getCause());
            } finally {
                channel = null;
            }
        }
    }
}
