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

import static io.netty.handler.codec.http.HttpHeaderNames.CONNECTION;
import static io.netty.handler.codec.http.HttpHeaderNames.CONTENT_LENGTH;
import static io.netty.handler.codec.http.HttpHeaderNames.UPGRADE;
import static io.netty.handler.codec.http.HttpMethod.GET;
import static io.netty.handler.codec.sockjs.transport.HttpResponseBuilder.responseFor;
import static io.netty.handler.codec.sockjs.util.Arguments.checkNotNull;

import io.netty.channel.ChannelFuture;
import io.netty.channel.ChannelFutureListener;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelInboundHandlerAdapter;
import io.netty.handler.codec.http.FullHttpRequest;
import io.netty.handler.codec.http.HttpHeaderNames;
import io.netty.handler.codec.http.HttpHeaderValues;
import io.netty.handler.codec.http.HttpRequest;
import io.netty.handler.codec.http.cors.CorsHandler;
import io.netty.handler.codec.http.websocketx.CloseWebSocketFrame;
import io.netty.handler.codec.http.websocketx.PingWebSocketFrame;
import io.netty.handler.codec.http.websocketx.PongWebSocketFrame;
import io.netty.handler.codec.http.websocketx.TextWebSocketFrame;
import io.netty.handler.codec.http.websocketx.WebSocketFrame;
import io.netty.handler.codec.http.websocketx.WebSocketHandshakeException;
import io.netty.handler.codec.http.websocketx.WebSocketServerHandshaker;
import io.netty.handler.codec.http.websocketx.WebSocketServerHandshakerFactory;
import io.netty.handler.codec.sockjs.SockJsConfig;
import io.netty.handler.codec.sockjs.SockJsSessionContext;
import io.netty.handler.codec.sockjs.SockJsService;
import io.netty.handler.codec.sockjs.handler.SessionHandler.Event;
import io.netty.handler.codec.sockjs.handler.SockJsHandler;
import io.netty.util.AttributeKey;
import io.netty.util.internal.logging.InternalLogger;
import io.netty.util.internal.logging.InternalLoggerFactory;

import java.util.Iterator;
import java.util.Set;

/**
 * WebSocketTransport is responsible for the WebSocket handshake and
 * also for receiving WebSocket frames.
 */
public class RawWebSocketTransport extends ChannelInboundHandlerAdapter {
    private static final InternalLogger logger = InternalLoggerFactory.getInstance(RawWebSocketTransport.class);
    private static final AttributeKey<HttpRequest> REQUEST_KEY = AttributeKey.valueOf("raw.ws.request.key");
    private final SockJsConfig config;
    private final String protocols;
    private final SockJsService service;
    private WebSocketServerHandshaker handshaker;

    public RawWebSocketTransport(final SockJsConfig config, final SockJsService service) {
        checkNotNull(config, "config");
        checkNotNull(service, "service");
        this.config = config;
        protocols = protocolsAsString(config.webSocketProtocols());
        this.service = service;
    }

    @Override
    public void channelRead(final ChannelHandlerContext ctx, final Object msg) throws Exception {
        if (msg instanceof FullHttpRequest) {
            handleHttpRequest(ctx, (FullHttpRequest) msg);
        } else if (msg instanceof WebSocketFrame) {
            handleWebSocketFrame(ctx, (WebSocketFrame) msg);
        } else {
            ctx.fireChannelRead(msg);
        }
    }

    private static boolean checkRequestHeaders(final ChannelHandlerContext ctx, final HttpRequest req) {
        if (req.method() != GET) {
            ctx.writeAndFlush(responseFor(req)
                    .methodNotAllowed()
                    .header(CONTENT_LENGTH, 0)
                    .allow(GET)
                    .buildResponse())
            .addListener(ChannelFutureListener.CLOSE);
            return false;
        }

        final String upgradeHeader = req.headers().get(UPGRADE);
        if (upgradeHeader == null || !"websocket".equals(upgradeHeader.toLowerCase())) {
            ctx.writeAndFlush(responseFor(req)
                    .badRequest()
                    .content("Can \"Upgrade\" only to \"WebSocket\".")
                    .contentType(HttpResponseBuilder.CONTENT_TYPE_PLAIN)
                    .buildFullResponse()).addListener(ChannelFutureListener.CLOSE);
            return false;
        }

        String connectHeader = req.headers().get(CONNECTION);
        if (connectHeader != null && "keep-alive".equals(connectHeader.toLowerCase())) {
            req.headers().set(CONNECTION, HttpHeaderValues.UPGRADE);
            connectHeader = HttpHeaderValues.UPGRADE.toString();
        }
        if (connectHeader == null || !"upgrade".equals(connectHeader.toLowerCase())) {
            ctx.writeAndFlush(responseFor(req)
                    .badRequest()
                    .content("\"Connection\" must be \"Upgrade\".")
                    .contentType(HttpResponseBuilder.CONTENT_TYPE_PLAIN)
                    .buildFullResponse()).addListener(ChannelFutureListener.CLOSE);
            return false;
        }
        return true;
    }

    private void handleHttpRequest(final ChannelHandlerContext ctx, FullHttpRequest req) {
        try {
            if (!checkRequestHeaders(ctx, req)) {
                return;
            }
            ctx.attr(REQUEST_KEY).set(req);
            final String wsUrl = webSocketLocation(config.isTls(), req, TransportType.WEBSOCKET.path());
            final WebSocketServerHandshakerFactory wsFactory = new WebSocketServerHandshakerFactory(wsUrl,
                    protocols, false);
            handshaker = wsFactory.newHandshaker(req);
            if (handshaker == null) {
                WebSocketServerHandshakerFactory.sendUnsupportedVersionResponse(ctx.channel());
            } else {
                final ChannelFuture handshakeFuture = handshaker.handshake(ctx.channel(), req);
                handshakeFuture.addListener(new ChannelFutureListener() {
                    @Override
                    public void operationComplete(final ChannelFuture future) throws Exception {
                        if (future.isSuccess()) {
                            ctx.pipeline().remove(SockJsHandler.class);
                            ctx.pipeline().remove(CorsHandler.class);
                            ctx.pipeline().addLast(new RawWebSocketSendHandler());
                            service.onOpen(new SockJsSessionContext() {
                                @Override
                                public void send(String message) {
                                    ctx.writeAndFlush(new TextWebSocketFrame(message));
                                }

                                @Override
                                public void close() {
                                    ctx.close();
                                }

                                @Override
                                public ChannelHandlerContext context() {
                                    return ctx;
                                }
                            });
                        }
                    }
                });
            }
        } finally {
            req.release();
        }
    }

    private static String webSocketLocation(final boolean tls, final FullHttpRequest req, final String path) {
        final String protocol = tls ? "wss://" : "ws://";
        return protocol + req.headers().get(HttpHeaderNames.HOST) + path;
    }

    private void handleWebSocketFrame(final ChannelHandlerContext ctx, final WebSocketFrame wsFrame) throws Exception {
        try {
            if (wsFrame instanceof CloseWebSocketFrame) {
                wsFrame.retain();
                service.onClose();
                handshaker.close(ctx.channel(), (CloseWebSocketFrame) wsFrame);
                return;
            }
            if (wsFrame instanceof PingWebSocketFrame) {
                wsFrame.content().retain();
                ctx.channel().writeAndFlush(new PongWebSocketFrame(wsFrame.content()));
                return;
            }
            if (!(wsFrame instanceof TextWebSocketFrame)) {
                throw new UnsupportedOperationException(String.format("%s frame types not supported",
                        wsFrame.getClass().getName()));
            }
            final String message = ((TextWebSocketFrame) wsFrame).text();
            service.onMessage(message);
        } finally {
            wsFrame.release();
        }
    }

    @Override
    public void exceptionCaught(final ChannelHandlerContext ctx, final Throwable cause) throws Exception {
        if (cause instanceof WebSocketHandshakeException) {
            final HttpRequest request = ctx.attr(REQUEST_KEY).get();
            logger.error("Failed with ws handshake for request: " + request, cause);
            ctx.writeAndFlush(responseFor(request)
                    .internalServerError()
                    .content(cause.getMessage())
                    .contentType(HttpResponseBuilder.CONTENT_TYPE_PLAIN)
                    .buildFullResponse()).addListener(ChannelFutureListener.CLOSE);
        } else {
            ctx.fireExceptionCaught(cause);
        }
    }

    @Override
    public void userEventTriggered(final ChannelHandlerContext ctx, final Object event) throws Exception {
        if (event == Event.CLOSE_SESSION) {
            ctx.writeAndFlush(new CloseWebSocketFrame(1000, "SockJS Service close the connection"))
                    .addListener(ChannelFutureListener.CLOSE);
        }
        ctx.fireUserEventTriggered(event);
    }

    public static boolean matches(final String path) {
        return TransportType.WEBSOCKET.path().equals(path);
    }

    private static String protocolsAsString(final Set<String> protocols) {
        if (protocols.isEmpty()) {
            return null;
        }
        final StringBuilder sb = new StringBuilder();
        final Iterator<String> iterator = protocols.iterator();
        if (iterator.hasNext()) {
            sb.append(iterator.next());
            while (iterator.hasNext()) {
                sb.append(',').append(iterator.next());
            }
        }
        return sb.toString();
    }

}
