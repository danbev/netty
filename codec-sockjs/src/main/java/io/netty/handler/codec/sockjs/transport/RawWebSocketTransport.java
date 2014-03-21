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

import static io.netty.handler.codec.http.HttpMethod.GET;
import static io.netty.handler.codec.sockjs.transport.Transports.badRequestResponse;
import static io.netty.handler.codec.sockjs.transport.Transports.internalServerErrorResponse;
import static io.netty.handler.codec.sockjs.transport.Transports.methodNotAllowedResponse;
import io.netty.channel.ChannelFuture;
import io.netty.channel.ChannelFutureListener;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelPromise;
import io.netty.channel.SimpleChannelInboundHandler;
import io.netty.handler.codec.http.FullHttpRequest;
import io.netty.handler.codec.http.HttpHeaders;
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
import io.netty.handler.codec.sockjs.handler.SessionHandler.Event;
import io.netty.handler.codec.sockjs.handler.SockJsHandler;
import io.netty.handler.codec.sockjs.protocol.MessageFrame;
import io.netty.util.AttributeKey;
import io.netty.util.internal.logging.InternalLogger;
import io.netty.util.internal.logging.InternalLoggerFactory;

/**
 * WebSocketTransport is responsible for the WebSocket handshake and
 * also for receiving WebSocket frames.
 */
public class RawWebSocketTransport extends SimpleChannelInboundHandler<Object> {
    private static final InternalLogger logger = InternalLoggerFactory.getInstance(RawWebSocketTransport.class);
    private static final AttributeKey<HttpRequest> REQUEST_KEY = AttributeKey.valueOf("raw.ws.request.key");
    private final SockJsConfig config;
    private WebSocketServerHandshaker handshaker;

    public RawWebSocketTransport(final SockJsConfig config) {
        this.config = config;
    }

    @Override
    protected void messageReceived(final ChannelHandlerContext ctx, final Object msg) throws Exception {
        if (msg instanceof FullHttpRequest) {
            handleHttpRequest(ctx, (FullHttpRequest) msg);
        } else if (msg instanceof WebSocketFrame) {
            handleWebSocketFrame(ctx, (WebSocketFrame) msg);
        } else {
            ctx.fireChannelRead(msg);
        }
    }

    private static boolean checkRequestHeaders(final ChannelHandlerContext ctx, final HttpRequest req) {
        if (req.getMethod() != GET) {
            ctx.writeAndFlush(methodNotAllowedResponse(req.getProtocolVersion()))
            .addListener(ChannelFutureListener.CLOSE);
            return false;
        }

        final String upgradeHeader = req.headers().get(HttpHeaders.Names.UPGRADE);
        if (upgradeHeader == null || !"websocket".equals(upgradeHeader.toLowerCase())) {
            ctx.writeAndFlush(badRequestResponse(req.getProtocolVersion(), "Can \"Upgrade\" only to \"WebSocket\"."))
            .addListener(ChannelFutureListener.CLOSE);
            return false;
        }

        String connectHeader = req.headers().get(HttpHeaders.Names.CONNECTION);
        if (connectHeader != null && "keep-alive".equals(connectHeader.toLowerCase())) {
            req.headers().set(HttpHeaders.Names.CONNECTION, HttpHeaders.Values.UPGRADE);
            connectHeader = HttpHeaders.Values.UPGRADE.toString();
        }
        if (connectHeader == null || !"upgrade".equals(connectHeader.toLowerCase())) {
            ctx.writeAndFlush(badRequestResponse(req.getProtocolVersion(), "\"Connection\" must be \"Upgrade\"."))
            .addListener(ChannelFutureListener.CLOSE);
            return false;
        }
        return true;
    }

    private void handleHttpRequest(final ChannelHandlerContext ctx, FullHttpRequest req) {
        if (!checkRequestHeaders(ctx, req)) {
            return;
        }
        ctx.attr(REQUEST_KEY).set(req);
        final String wsUrl = getWebSocketLocation(config.isTls(), req, Transports.Type.WEBSOCKET.path());
        final WebSocketServerHandshakerFactory wsFactory = new WebSocketServerHandshakerFactory(wsUrl,
                config.webSocketProtocolCSV(), false);
        handshaker = wsFactory.newHandshaker(req);
        if (handshaker == null) {
            WebSocketServerHandshakerFactory.sendUnsupportedVersionResponse(ctx.channel());
        } else {
            final ChannelFuture handshakeFuture = handshaker.handshake(ctx.channel(), req);
            handshakeFuture.addListener(new ChannelFutureListener() {
                @Override
                public void operationComplete(final ChannelFuture future) throws Exception {
                    if (future.isSuccess()) {
                        ctx.pipeline().replace(SockJsHandler.class, "rawWebSocket", new RawWebSocketSendHandler());
                        ctx.pipeline().remove(CorsHandler.class);
                        ctx.fireUserEventTriggered(Event.ON_SESSION_OPEN);
                    }
                }
            });
        }
    }

    private static String getWebSocketLocation(final boolean tls, final FullHttpRequest req, final String path) {
        final String protocol = tls ? "wss://" : "ws://";
        return protocol + req.headers().get(HttpHeaders.Names.HOST) + path;
    }

    private void handleWebSocketFrame(final ChannelHandlerContext ctx, final WebSocketFrame wsFrame) throws Exception {
        if (wsFrame instanceof CloseWebSocketFrame) {
            wsFrame.retain();
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
        ctx.fireChannelRead(message);
    }

    @Override
    public void write(ChannelHandlerContext ctx, Object msg, ChannelPromise promise) throws Exception {
        if (msg instanceof String) {
            ctx.channel().write(new MessageFrame((String) msg));
        } else {
            super.write(ctx, msg, promise);
        }
    }

    @Override
    public void exceptionCaught(final ChannelHandlerContext ctx, final Throwable cause) throws Exception {
        if (cause instanceof WebSocketHandshakeException) {
            final HttpRequest request = ctx.attr(REQUEST_KEY).get();
            logger.error("Failed with ws handshake for request: " + request, cause);
            ctx.writeAndFlush(internalServerErrorResponse(request.getProtocolVersion(), cause.getMessage()))
            .addListener(ChannelFutureListener.CLOSE);
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
        ctx.fireChannelRead(event);
    }

}
