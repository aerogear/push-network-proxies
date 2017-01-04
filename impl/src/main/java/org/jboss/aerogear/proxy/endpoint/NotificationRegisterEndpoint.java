package org.jboss.aerogear.proxy.endpoint;

import java.util.logging.Logger;

import io.airlift.airline.Option;
import io.netty.bootstrap.ServerBootstrap;
import io.netty.channel.Channel;
import io.netty.channel.ChannelOption;
import io.netty.channel.EventLoopGroup;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.nio.NioServerSocketChannel;
import io.netty.handler.logging.LogLevel;
import io.netty.handler.logging.LoggingHandler;

/**
 *
 * @author <a href="mailto:miklosovic@gmail.com">Stefan Miklosovic</a>
 */
public abstract class NotificationRegisterEndpoint implements Runnable {

    private static final Logger logger = Logger.getLogger(NotificationRegisterEndpoint.class.getName());

    private EventLoopGroup bossGroup = null;
    private EventLoopGroup workerGroup = null;

    private Thread notificationEndpointThread;

    @Option(name = "--notificationEndpointHost", description = "defaults to 127.0.0.1")
    public String notificationEndpointHost = "127.0.0.1";

    @Option(name = "--notificationEndpointPort", description = "defaults to 17000")
    public int notificationEndpointPort = 17000;

    protected void startNotificationRegisterEndpoint(final String host, final int port) {

        Runnable notificationRegisterEndpointRunnable = new Runnable() {

            @Override
            public void run() {
                bossGroup = new NioEventLoopGroup(1);
                workerGroup = new NioEventLoopGroup();

                try {
                    ServerBootstrap b = new ServerBootstrap();
                    b.option(ChannelOption.SO_BACKLOG, 1024);
                    b.group(bossGroup, workerGroup)
                        .channel(NioServerSocketChannel.class)
                        .handler(new LoggingHandler(LogLevel.INFO))
                        .childHandler(new NotificationRegisterServerInitializer());

                    Channel ch = b.bind(host, port).sync().channel();

                    logger.info(String.format("Notification register endpoint started at %s:%s", host, port));

                    ch.closeFuture().sync();
                } catch (InterruptedException ex) {
                    logger.info("Notification register endpoint was interrupted.");
                } finally {
                    bossGroup.shutdownGracefully();
                    workerGroup.shutdownGracefully();
                }

            }
        };

        notificationEndpointThread = new Thread(notificationRegisterEndpointRunnable);
        notificationEndpointThread.start();
    }

    protected void stopNotificationRegisterEndpoint() {
        if (notificationEndpointThread != null) {
            notificationEndpointThread.interrupt();
        }
    }
}
