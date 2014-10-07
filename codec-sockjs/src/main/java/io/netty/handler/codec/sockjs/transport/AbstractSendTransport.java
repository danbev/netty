/*
 * Copyright 2013 The Netty Project
 *
 * The Netty Project licenses this file to you under the Apache License, version
 * 2.0 (the "License"); you may not use this file except in compliance with the
 * License. You may obtain a copy of the License at:
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS, WITHOUT
 * WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the
 * License for the specific language governing permissions and limitations under
 * the License.
 */
package io.netty.handler.codec.sockjs.transport;

import com.fasterxml.jackson.core.JsonParseException;
import io.netty.channel.ChannelFutureListener;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.SimpleChannelInboundHandler;
import io.netty.handler.codec.http.FullHttpRequest;
import io.netty.handler.codec.http.FullHttpResponse;
import io.netty.handler.codec.http.HttpHeaders;
import io.netty.handler.codec.http.HttpRequest;
import io.netty.handler.codec.http.HttpResponseStatus;
import io.netty.handler.codec.http.QueryStringDecoder;
import io.netty.handler.codec.sockjs.SockJsConfig;
import io.netty.handler.codec.sockjs.util.ArgumentUtil;
import io.netty.handler.codec.sockjs.util.JsonUtil;
import java.util.List;
import static io.netty.handler.codec.http.HttpHeaders.Names.*;
import static io.netty.util.CharsetUtil.UTF_8;

/**
 * A common base class for SockJS transports that send messages to a SockJS service.
 */
public abstract class AbstractSendTransport extends SimpleChannelInboundHandler<FullHttpRequest> {

    protected final SockJsConfig config;

    protected AbstractSendTransport(final SockJsConfig config) {
        ArgumentUtil.checkNotNull(config, "config");
        this.config = config;
    }

    @Override
    public void messageReceived(final ChannelHandlerContext ctx, final FullHttpRequest request) throws Exception {
        final String content = getContent(request);
        if (content.isEmpty()) {
            ctx.writeAndFlush(HttpResponseBuilder.responseFor(request)
                    .internalServerError()
                    .content("Payload expected.")
                    .contentType(HttpResponseBuilder.CONTENT_TYPE_PLAIN)
                    .buildFullResponse(ctx.alloc())).addListener(ChannelFutureListener.CLOSE);
        } else {
            try {
                final String[] messages = JsonUtil.decode(content);
                for (String message : messages) {
                    ctx.fireChannelRead(message);
                }
                respond(ctx, request);
            } catch (final JsonParseException ignored) {
                ctx.writeAndFlush(HttpResponseBuilder.responseFor(request)
                        .internalServerError()
                        .content("Broken JSON encoding.")
                        .contentType(HttpResponseBuilder.CONTENT_TYPE_PLAIN)
                        .buildFullResponse(ctx.alloc()));
            }
        }
    }

    /**
     * Allows concrete subclasses to very how they will respond after a message has been sent
     * to the target SockJS service.
     * Different transport protocols require different responses, for example jsonp_send requires an
     * OK response while xhr_send NO_CONTENT.
     *
     * @param ctx the current {@link ChannelHandlerContext}.
     * @param request the http request.
     * @throws Exception if a failure occurs while trying to respond.
     */
    public abstract void respond(ChannelHandlerContext ctx, FullHttpRequest request) throws Exception;

    private static String getContent(final FullHttpRequest request) {
        final String contentType = getContentType(request);
        if (HttpResponseBuilder.CONTENT_TYPE_FORM.equals(contentType)) {
            final List<String> data = getDataFormParameter(request);
            if (data != null) {
                return data.get(0);
            } else {
                return "";
            }
        }
        return request.content().toString(UTF_8);
    }

    private static String getContentType(final FullHttpRequest request) {
        final String contentType = request.headers().get(CONTENT_TYPE);
        if (contentType == null) {
            return HttpResponseBuilder.CONTENT_TYPE_PLAIN;
        }
        return contentType;
    }

    private static List<String> getDataFormParameter(final FullHttpRequest request) {
        final QueryStringDecoder decoder = new QueryStringDecoder('?' + request.content().toString(UTF_8));
        return decoder.parameters().get("d");
    }

    protected void respond(final ChannelHandlerContext ctx,
            final HttpRequest request,
            final HttpResponseStatus status,
            final String message) {
        final FullHttpResponse response = HttpResponseBuilder.responseFor(request)
                .status(status)
                .content(message)
                .contentType(HttpResponseBuilder.CONTENT_TYPE_PLAIN)
                .setCookie(config)
                .header(CONNECTION, HttpHeaders.Values.CLOSE)
                .header(CACHE_CONTROL, HttpResponseBuilder.NO_CACHE_HEADER)
                .buildFullResponse(ctx.alloc());
        if (ctx.channel().isActive() && ctx.channel().isRegistered()) {
            ctx.writeAndFlush(response).addListener(ChannelFutureListener.CLOSE);
        }
    }

}
