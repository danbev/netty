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
package io.netty.handler.codec.sockjs.handler;

import io.netty.buffer.Unpooled;
import io.netty.channel.Channel;
import io.netty.channel.ChannelFuture;
import io.netty.channel.ChannelFutureListener;
import io.netty.channel.ChannelHandler;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.SimpleChannelInboundHandler;
import io.netty.handler.codec.http.DefaultFullHttpResponse;
import io.netty.handler.codec.http.FullHttpResponse;
import io.netty.handler.codec.http.HttpHeaders;
import io.netty.handler.codec.http.HttpRequest;
import io.netty.handler.codec.http.QueryStringDecoder;
import io.netty.handler.codec.sockjs.SockJsChannelConfig;
import io.netty.handler.codec.sockjs.transport.EventSourceTransport;
import io.netty.handler.codec.sockjs.transport.HtmlFileTransport;
import io.netty.handler.codec.sockjs.transport.JsonpPollingTransport;
import io.netty.handler.codec.sockjs.transport.JsonpSendTransport;
import io.netty.handler.codec.sockjs.transport.RawWebSocketTransport;
import io.netty.handler.codec.sockjs.transport.Transports;
import io.netty.handler.codec.sockjs.transport.WebSocketTransport;
import io.netty.handler.codec.sockjs.transport.XhrPollingTransport;
import io.netty.handler.codec.sockjs.transport.XhrSendTransport;
import io.netty.handler.codec.sockjs.transport.XhrStreamingTransport;
import io.netty.util.CharsetUtil;
import io.netty.util.ReferenceCountUtil;
import io.netty.util.internal.logging.InternalLogger;
import io.netty.util.internal.logging.InternalLoggerFactory;

import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static io.netty.handler.codec.http.HttpHeaders.Names.*;
import static io.netty.handler.codec.http.HttpHeaders.Values.*;
import static io.netty.handler.codec.http.HttpResponseStatus.*;
import static java.util.UUID.*;

/**
 * This handler is the main entry point for SockJS HTTP Request.
 *
 * It is responsible for inspecting the request uri and adding ChannelHandlers for
 * different transport protocols that SockJS support. Once this has been done this
 * handler will be removed from the channel pipeline.
 */
public class SockJsHandler extends SimpleChannelInboundHandler<HttpRequest> {

    private static final InternalLogger logger = InternalLoggerFactory.getInstance(SockJsHandler.class);
    private static final ConcurrentMap<String, SockJsSession> sessions = new ConcurrentHashMap<String, SockJsSession>();
    private static final PathParams NON_SUPPORTED_PATH = new NonSupportedPath();
    private static final Pattern SERVER_SESSION_PATTERN = Pattern.compile("^/([^/.]+)/([^/.]+)/([^/.]+)");

    @Override
    public void messageReceived(final ChannelHandlerContext ctx, final HttpRequest request) throws Exception {
        final SockJsChannelConfig config = (SockJsChannelConfig) ctx.channel().config();
        if (requestPathMatchesPrefix(request, config)) {
            handleService(request, ctx, config);
        } else {
            writeNotFoundResponse(request, ctx);
        }
    }

    private static boolean requestPathMatchesPrefix(final HttpRequest request, SockJsChannelConfig config) {
        final String path = new QueryStringDecoder(request.getUri()).path();
        return path.startsWith(config.getPrefix());
    }

    private static void handleService(final HttpRequest request,
                                      final ChannelHandlerContext ctx,
                                      final SockJsChannelConfig config) throws Exception {
        if (logger.isDebugEnabled()) {
            logger.debug("RequestUri : [{}]", request.getUri());
        }
        final String pathWithoutPrefix = request.getUri().replaceFirst(config.getPrefix(), "");
        final String path = new QueryStringDecoder(pathWithoutPrefix).path();
        if (Greeting.matches(path)) {
            writeResponse(ctx.channel(), request, Greeting.response(request));
        } else if (Info.matches(path)) {
            writeResponse(ctx.channel(), request, Info.response(config, request));
        } else if (Iframe.matches(path)) {
            writeResponse(ctx.channel(), request, Iframe.response(config, request));
        } else if (Transports.Type.WEBSOCKET.path().equals(path)) {
            addTransportHandler(new RawWebSocketTransport(config), ctx);
            ctx.fireChannelRead(ReferenceCountUtil.retain(request));
        } else {
            final PathParams sessionPath = matches(path);
            if (sessionPath.matches()) {
                handleSession(config, request, ctx, sessionPath);
            } else {
                writeNotFoundResponse(request, ctx);
            }
        }
    }

    private static void handleSession(final SockJsChannelConfig config,
                                      final HttpRequest request,
                                      final ChannelHandlerContext ctx,
                                      final PathParams pathParams) throws Exception {
        switch (pathParams.transport()) {
        case XHR:
            addTransportHandler(new XhrPollingTransport(config, request), ctx);
            addSessionHandler(new PollingSessionState(sessions, getSession(pathParams.sessionId(), config)), ctx);
            break;
        case JSONP:
            addSessionHandler(new PollingSessionState(sessions, getSession(pathParams.sessionId(), config)), ctx);
            addTransportHandler(new JsonpPollingTransport(config, request), ctx);
            break;
        case XHR_SEND:
            checkSessionExists(pathParams.sessionId(), request);
            addSessionHandler(new SendingSessionState(sessions, sessions.get(pathParams.sessionId())), ctx);
            addTransportHandler(new XhrSendTransport(config), ctx);
            break;
        case XHR_STREAMING:
            addTransportHandler(new XhrStreamingTransport(config, request), ctx);
            addSessionHandler(new StreamingSessionState(sessions, getSession(pathParams.sessionId(), config)), ctx);
            break;
        case EVENTSOURCE:
            addTransportHandler(new EventSourceTransport(config, request), ctx);
            addSessionHandler(new StreamingSessionState(sessions, getSession(pathParams.sessionId(), config)), ctx);
            break;
        case HTMLFILE:
            addSessionHandler(new StreamingSessionState(sessions, getSession(pathParams.sessionId(), config)), ctx);
            addTransportHandler(new HtmlFileTransport(config, request), ctx);
            break;
        case JSONP_SEND:
            checkSessionExists(pathParams.sessionId(), request);
            addSessionHandler(new SendingSessionState(sessions, sessions.get(pathParams.sessionId())), ctx);
            addTransportHandler(new JsonpSendTransport(config), ctx);
            break;
        case WEBSOCKET:
            addSessionHandler(new WebSocketSessionState(new SockJsSession(randomUUID().toString(), config)), ctx);
            addTransportHandler(new WebSocketTransport(config), ctx);
            break;
        }
        ctx.fireChannelRead(ReferenceCountUtil.retain(request));
    }

    private static void addTransportHandler(final ChannelHandler transportHandler, final ChannelHandlerContext ctx) {
        ctx.pipeline().addAfter(ctx.name(), "transportHandler", transportHandler);
    }

    private static void addSessionHandler(final SessionState sessionState, final ChannelHandlerContext ctx) {
        ctx.pipeline().addAfter(ctx.name(), "sessionHandler", new SessionHandler(sessionState));
    }

    private static void checkSessionExists(final String sessionId, final HttpRequest request)
            throws SessionNotFoundException {
        if (!sessions.containsKey(sessionId)) {
            throw new SessionNotFoundException(sessionId, request);
        }
    }

    private static SockJsSession getSession(final String sessionId, final SockJsChannelConfig config) {
        SockJsSession session = sessions.get(sessionId);
        if (session == null) {
            final SockJsSession newSession = new SockJsSession(sessionId, config);
            session = sessions.putIfAbsent(sessionId, newSession);
            if (session == null) {
                session = newSession;
            }
            logger.debug("Created new session [{}]", sessionId);
        } else {
            logger.debug("Using existing session [{}]", sessionId);
        }
        return session;
    }

    private static void writeNotFoundResponse(final HttpRequest request, final ChannelHandlerContext ctx) {
        final FullHttpResponse response = new DefaultFullHttpResponse(request.getProtocolVersion(), NOT_FOUND,
                Unpooled.copiedBuffer("Not found", CharsetUtil.UTF_8));
        writeResponse(ctx.channel(), request, response);
    }

    private static void writeResponse(final Channel channel, final HttpRequest request,
                                      final FullHttpResponse response) {
        response.headers().set(CONTENT_LENGTH, response.content().readableBytes());
        boolean hasKeepAliveHeader = HttpHeaders.equalsIgnoreCase(KEEP_ALIVE, request.headers().get(CONNECTION));
        if (!request.getProtocolVersion().isKeepAliveDefault() && hasKeepAliveHeader) {
            response.headers().set(CONNECTION, KEEP_ALIVE);
        }
        final ChannelFuture wf = channel.writeAndFlush(response);
        if (!HttpHeaders.isKeepAlive(request)) {
            wf.addListener(ChannelFutureListener.CLOSE);
        }
    }

    @Override
    public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause) throws Exception {
        if (cause instanceof SessionNotFoundException) {
            final SessionNotFoundException se = (SessionNotFoundException) cause;
            logger.debug("Could not find session [{}]", se.sessionId());
            writeNotFoundResponse(se.httpRequest(), ctx);
        } else {
            logger.error("exception caught:", cause);
            ctx.fireExceptionCaught(cause);
        }
    }

    static PathParams matches(final String path) {
        final Matcher matcher = SERVER_SESSION_PATTERN.matcher(path);
        if (matcher.find()) {
            final String serverId = matcher.group(1);
            final String sessionId = matcher.group(2);
            final String transport = matcher.group(3);
            return new MatchingSessionPath(serverId, sessionId, transport);
        } else {
            return NON_SUPPORTED_PATH;
        }
    }

    private static final class SessionNotFoundException extends Exception {
        private static final long serialVersionUID = 1101611486620901143L;
        private final String sessionId;
        private final HttpRequest request;

        private SessionNotFoundException(final String sessionId, final HttpRequest request) {
            this.sessionId = sessionId;
            this.request = request;
        }

        public String sessionId() {
            return sessionId;
        }

        public HttpRequest httpRequest() {
            return request;
        }
    }

    /**
     * Represents HTTP path parameters in SockJS.
     *
     * The path consists of the following parts:
     * http://server:port/prefix/serverId/sessionId/transport
     *
     */
    public interface PathParams {
        boolean matches();

        /**
         * The serverId is chosen by the client and exists to make it easier to configure
         * load balancers to enable sticky sessions.
         *
         * @return String the server id for this path.
         */
        String serverId();

        /**
         * The sessionId is a unique random number which identifies the session.
         *
         * @return String the session identifier for this path.
         */
        String sessionId();

        /**
         * The type of transport.
         *
         * @return Transports.Type the type of the transport.
         */
        Transports.Type transport();
    }

    public static class MatchingSessionPath implements PathParams {
        private final String serverId;
        private final String sessionId;
        private final Transports.Type transport;

        public MatchingSessionPath(final String serverId, final String sessionId, final String transport) {
            this.serverId = serverId;
            this.sessionId = sessionId;
            this.transport = Transports.Type.valueOf(transport.toUpperCase());
        }

        @Override
        public boolean matches() {
            return true;
        }

        @Override
        public String serverId() {
            return serverId;
        }

        @Override
        public String sessionId() {
            return sessionId;
        }

        @Override
        public Transports.Type transport() {
            return transport;
        }
    }

    public static class NonSupportedPath implements PathParams {

        @Override
        public boolean matches() {
            return false;
        }

        @Override
        public String serverId() {
            throw new UnsupportedOperationException("serverId is not available in path");
        }

        @Override
        public String sessionId() {
            throw new UnsupportedOperationException("sessionId is not available in path");
        }

        @Override
        public Transports.Type transport() {
            throw new UnsupportedOperationException("transport is not available in path");
        }
    }

}
