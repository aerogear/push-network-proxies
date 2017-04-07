package org.jboss.aerogear.proxy.endpoint;

import static io.netty.handler.codec.http.HttpHeaders.Names.CONNECTION;
import static io.netty.handler.codec.http.HttpHeaders.Names.CONTENT_LENGTH;
import static io.netty.handler.codec.http.HttpHeaders.Names.CONTENT_TYPE;
import static io.netty.handler.codec.http.HttpResponseStatus.CONTINUE;
import static io.netty.handler.codec.http.HttpResponseStatus.OK;
import static io.netty.handler.codec.http.HttpVersion.HTTP_1_1;

import org.jboss.aerogear.proxy.endpoint.model.NotificationRegisterResponse;
import org.jboss.aerogear.proxy.fcm.FCMNotificationRegister;

import io.netty.buffer.Unpooled;
import io.netty.channel.ChannelFutureListener;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.SimpleChannelInboundHandler;
import io.netty.handler.codec.http.DefaultFullHttpResponse;
import io.netty.handler.codec.http.FullHttpResponse;
import io.netty.handler.codec.http.HttpRequest;
import io.netty.handler.codec.http.HttpHeaders;

/**
 *
 * @author <a href="mailto:miklosovic@gmail.com">Stefan Miklosovic</a>
 */
public class NotificationRegisterServerHandler extends SimpleChannelInboundHandler<Object> {

    @Override
    public void channelReadComplete(ChannelHandlerContext ctx) {
        ctx.flush();
    }

    @Override
    public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause) {
        cause.printStackTrace();
        ctx.close();
    }

    @Override
    public void channelRead0(ChannelHandlerContext ctx, Object msg) throws Exception {
        if (msg instanceof HttpRequest) {
            HttpRequest req = (HttpRequest) msg;

            String uri = req.getUri();

            if (HttpHeaders.is100ContinueExpected(req)) {
                ctx.write(new DefaultFullHttpResponse(HTTP_1_1, CONTINUE));
            }

            boolean keepAlive = HttpHeaders.isKeepAlive(req);

            FullHttpResponse response = null;

            if (uri.endsWith("clear")) {
                FCMNotificationRegister.clear();
                String responseBody = "{\"result\": \"cleared\" }";
                response = new DefaultFullHttpResponse(HTTP_1_1, OK, Unpooled.wrappedBuffer(responseBody.getBytes()));
                HttpHeaders.setHeader(response, CONTENT_TYPE, "application/json");
                HttpHeaders.setIntHeader(response, CONTENT_LENGTH, responseBody.getBytes().length);
            } else {
                final String responsePayload = constructResponse();
                final byte[] responsePayloadBytes = responsePayload.getBytes();
                final int responsePayloadLength = responsePayloadBytes.length;

                response = new DefaultFullHttpResponse(HTTP_1_1, OK, Unpooled.wrappedBuffer(responsePayloadBytes));

                HttpHeaders.setHeader(response, CONTENT_TYPE, "application/json");
                HttpHeaders.setIntHeader(response, CONTENT_LENGTH, responsePayloadLength);
            }

            if (!keepAlive) {
                ctx.write(response).addListener(ChannelFutureListener.CLOSE);
            } else {
                response.headers().set(CONNECTION, HttpHeaders.Values.KEEP_ALIVE);
                ctx.write(response);
            }
        }
    }

    private String constructResponse() {

        NotificationRegisterResponse response = new NotificationRegisterResponse();
        response.setFcmNotifications(FCMNotificationRegister.getNotifications());

        return response.toString();
    }
}
