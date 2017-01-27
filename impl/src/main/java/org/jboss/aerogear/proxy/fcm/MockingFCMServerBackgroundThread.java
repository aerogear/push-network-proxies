package org.jboss.aerogear.proxy.fcm;

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

public class MockingFCMServerBackgroundThread extends Thread {

    private final String fcmMockServerHost;

    private final int fcmMockServePort;

    private final File fcmCertificateFile;

    private final File fcmCertificateKeyFile;

    private Channel channel;

    public MockingFCMServerBackgroundThread(String fcmMockServerHost,
                                            int fcmMockServePort,
                                            File fcmCertificateFile,
                                            File fcmCertificateKeyFile) {

        this.fcmMockServerHost = fcmMockServerHost;
        this.fcmMockServePort = fcmMockServePort;
        this.fcmCertificateFile = fcmCertificateFile;
        this.fcmCertificateKeyFile = fcmCertificateKeyFile;
    }

    public String getFcmMockServerHost() {
        return fcmMockServerHost;
    }

    public int getFcmMockServePort() {
        return fcmMockServePort;
    }

    @Override
    public void run() {

        SslContext sslCtx = null;

        try {
            sslCtx = SslContext.newServerContext(fcmCertificateFile, fcmCertificateKeyFile);
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
                .childHandler(new MockingFCMServerInitializer(sslCtx));

            channel = serverBootstrap.bind(fcmMockServerHost, fcmMockServePort).sync().channel();

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
