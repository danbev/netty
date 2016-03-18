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
import io.netty.channel.ChannelInboundHandlerAdapter;
import io.netty.handler.codec.http.FullHttpRequest;
import io.netty.handler.codec.http.QueryStringDecoder;
import io.netty.handler.codec.sockjs.SockJsConfig;
import io.netty.handler.codec.sockjs.util.Arguments;
import io.netty.handler.codec.sockjs.util.JsonConverter;

import java.util.List;
import static io.netty.handler.codec.http.HttpHeaderNames.*;
import static io.netty.handler.codec.sockjs.transport.HttpResponseBuilder.*;
import static io.netty.util.CharsetUtil.UTF_8;

/**
 * A common base class for SockJS transports that send messages to a SockJS service.
 */
public abstract class AbstractSendTransport extends ChannelInboundHandlerAdapter {

    protected final SockJsConfig config;

    protected AbstractSendTransport(final SockJsConfig config) {
        Arguments.checkNotNull(config, "config");
        this.config = config;
    }

    @Override
    public void channelRead(final ChannelHandlerContext ctx, final Object msg) throws Exception {
        if (msg instanceof FullHttpRequest) {
            final FullHttpRequest request = (FullHttpRequest) msg;
            try {
                final String content = content(request);
                if (content.isEmpty()) {
                    ctx.writeAndFlush(responseFor(request)
                            .internalServerError()
                            .content("Payload expected.")
                            .contentType(CONTENT_TYPE_PLAIN)
                            .buildFullResponse()).addListener(ChannelFutureListener.CLOSE);
                } else {
                    try {
                        final String[] messages = JsonConverter.decode(content);
                        for (String message : messages) {
                            ctx.fireChannelRead(message);
                        }
                        respond(ctx, request);
                    } catch (final JsonParseException ignored) {
                        ctx.writeAndFlush(responseFor(request)
                                .internalServerError()
                                .content("Broken JSON encoding.")
                                .contentType(CONTENT_TYPE_PLAIN)
                                .buildFullResponse());
                    }
                }
            } finally {
                request.release();
            }
        } else {
            ctx.fireChannelRead(msg);
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

    private static String content(final FullHttpRequest request) {
        final String contentType = contentType(request);
        if (CONTENT_TYPE_FORM.equals(contentType)) {
            final List<String> data = dataFormParameter(request);
            if (data != null) {
                return data.get(0);
            } else {
                return "";
            }
        }
        return request.content().toString(UTF_8);
    }

    private static String contentType(final FullHttpRequest request) {
        final String contentType = request.headers().get(CONTENT_TYPE);
        if (contentType == null) {
            return CONTENT_TYPE_PLAIN;
        }
        return contentType;
    }

    private static List<String> dataFormParameter(final FullHttpRequest request) {
        final QueryStringDecoder decoder = new QueryStringDecoder('?' + request.content().toString(UTF_8));
        return decoder.parameters().get("d");
    }

}
