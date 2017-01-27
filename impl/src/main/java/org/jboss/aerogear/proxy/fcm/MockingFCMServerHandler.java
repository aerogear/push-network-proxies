package org.jboss.aerogear.proxy.fcm;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.PropertyNamingStrategy;
import org.jboss.aerogear.proxy.endpoint.model.FCMNotification;
import org.jboss.aerogear.proxy.utils.Tokens;

import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.SimpleChannelInboundHandler;
import io.netty.handler.codec.http.DefaultFullHttpResponse;
import io.netty.handler.codec.http.FullHttpResponse;
import io.netty.handler.codec.http.HttpContent;
import io.netty.handler.codec.http.HttpHeaders;
import io.netty.handler.codec.http.HttpObject;
import io.netty.handler.codec.http.HttpRequest;
import io.netty.handler.codec.http.LastHttpContent;
import io.netty.util.CharsetUtil;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.logging.Logger;

import static io.netty.handler.codec.http.HttpHeaders.Names.CONNECTION;
import static io.netty.handler.codec.http.HttpHeaders.Names.CONTENT_LENGTH;
import static io.netty.handler.codec.http.HttpHeaders.Names.CONTENT_TYPE;
import static io.netty.handler.codec.http.HttpResponseStatus.BAD_REQUEST;
import static io.netty.handler.codec.http.HttpResponseStatus.CONTINUE;
import static io.netty.handler.codec.http.HttpResponseStatus.OK;
import static io.netty.handler.codec.http.HttpVersion.HTTP_1_1;

public class MockingFCMServerHandler extends SimpleChannelInboundHandler<Object> {

    private static final Logger logger = Logger.getLogger(MockingFCMServerHandler.class.getName());

    private HttpRequest request;

    /**
     * Buffer that stores the response content
     */
    private final StringBuilder buf = new StringBuilder();

    private final StringBuilder requestContentBuffer = new StringBuilder();

    // TODO does this need to start at 1337?
    private static int multicastIdCounter = 1337;
    private static int messageIdCounter = 1337;

    @Override
    public void channelReadComplete(ChannelHandlerContext ctx) {
        ctx.flush();
    }

    @Override
    protected void channelRead0(ChannelHandlerContext context, Object msg) {

        if (msg instanceof HttpRequest) {
            this.request = (HttpRequest) msg;

            if (HttpHeaders.is100ContinueExpected(request)) {
                context.write(new DefaultFullHttpResponse(HTTP_1_1, CONTINUE));
            }
        }

        if (msg instanceof HttpContent) {
            HttpContent httpContent = (HttpContent) msg;
            ByteBuf content = httpContent.content();

            requestContentBuffer.append(content.toString(CharsetUtil.UTF_8));

            if (msg instanceof LastHttpContent) {
                buf.setLength(0);

                if (request.getUri().contains("fcm")) {
                    ObjectMapper mapper = new ObjectMapper();
                    mapper.setPropertyNamingStrategy(PropertyNamingStrategy.CAMEL_CASE_TO_LOWER_CASE_WITH_UNDERSCORES);
                    try {
                        FCMNotification notification = mapper.readValue(requestContentBuffer.toString(), FCMNotification.class);

                        logger.info("PROXY RECEIVED NOTIFICATION " + notification.toString());
                        FCMNotificationRegister.addNotification(notification);

                        requestContentBuffer.delete(0, requestContentBuffer.length());

                        try {
                            buf.append(mapper.writeValueAsString(this.createResponse(notification.getDeviceTokens())));
                        } catch (JsonProcessingException e) {
                            e.printStackTrace();
                        }
                    } catch (IOException e) {
                        e.printStackTrace();
                    }

                }

                writeResponse((HttpObject) msg, context);
            }
        }
    }

    @Override
    public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause) {
        cause.printStackTrace();
        ctx.close();
    }

    private boolean writeResponse(HttpObject currentObj, ChannelHandlerContext ctx) {
        // Decide whether to close the connection or not.
        boolean keepAlive = HttpHeaders.isKeepAlive(request);

        // Build the response object.
        FullHttpResponse response = new DefaultFullHttpResponse(
            HTTP_1_1, currentObj.getDecoderResult().isSuccess() ? OK : BAD_REQUEST,
            Unpooled.copiedBuffer(buf.toString(), CharsetUtil.UTF_8));

        response.headers().set(CONTENT_TYPE, "application/json");

        if (keepAlive) {
            // Add 'Content-Length' header only for a keep-alive connection.
            response.headers().set(CONTENT_LENGTH, response.content().readableBytes());
            // Add keep alive header as per:
            // - http://www.w3.org/Protocols/HTTP/1.1/draft-ietf-http-v11-spec-01.html#Connection
            response.headers().set(CONNECTION, HttpHeaders.Values.KEEP_ALIVE);
        }

        // Write the response.
        ctx.write(response);

        return keepAlive;
    }

    private Map<String, Object> createResponse(List<String> regIds) {

        Map<String, Object> jsonResponse = new HashMap<String, Object>();

        List<HashMap<String, String>> out = new ArrayList<HashMap<String, String>>();

        int success = 0;
        int failures = 0;

        for (String s : regIds) {
            HashMap<String, String> hm = new HashMap<String, String>();

            if (s.toLowerCase().startsWith(Tokens.TOKEN_INVALIDATION_PREFIX)) {
                failures++;
                hm.put("error", "InvalidRegistration");
            } else {
                success++;
                hm.put("message_id", "1:" + messageIdCounter++);
            }

            out.add(hm);
        }

        jsonResponse.put("success", success);
        jsonResponse.put("multicast_id", multicastIdCounter++);
        jsonResponse.put("failure", failures);
        jsonResponse.put("results", out);

        jsonResponse.put("canonical_ids", 0);

        return jsonResponse;
    }
}
