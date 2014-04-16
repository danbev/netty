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

import io.netty.channel.ChannelHandler;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.SimpleChannelInboundHandler;
import io.netty.handler.codec.http.HttpMethod;
import io.netty.handler.codec.http.HttpRequest;
import io.netty.handler.codec.http.QueryStringDecoder;
import io.netty.handler.codec.sockjs.SockJsServiceConfig;
import io.netty.handler.codec.sockjs.transport.EventSourceTransport;
import io.netty.handler.codec.sockjs.transport.HtmlFileTransport;
import io.netty.handler.codec.sockjs.transport.JsonpPollingTransport;
import io.netty.handler.codec.sockjs.transport.JsonpSendTransport;
import io.netty.handler.codec.sockjs.transport.RawWebSocketTransport;
import io.netty.handler.codec.sockjs.transport.TransportType;
import io.netty.handler.codec.sockjs.transport.WebSocketTransport;
import io.netty.handler.codec.sockjs.transport.XhrPollingTransport;
import io.netty.handler.codec.sockjs.transport.XhrSendTransport;
import io.netty.handler.codec.sockjs.transport.XhrStreamingTransport;
import io.netty.util.ReferenceCountUtil;
import io.netty.util.internal.logging.InternalLogger;
import io.netty.util.internal.logging.InternalLoggerFactory;

import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static io.netty.handler.codec.sockjs.util.TransportUtil.writeMethodNotAllowedResponse;
import static io.netty.handler.codec.sockjs.util.TransportUtil.writeNotFoundResponse;
import static io.netty.handler.codec.sockjs.util.TransportUtil.writeResponse;
import static java.util.UUID.randomUUID;

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
    private final SockJsServiceConfig config;

    public SockJsHandler(final SockJsServiceConfig config) {
        this.config = config;
    }

    @Override
    public void messageReceived(final ChannelHandlerContext ctx, final HttpRequest request) throws Exception {
        if (request.getMethod() == HttpMethod.OPTIONS) {
            writeMethodNotAllowedResponse(request, ctx);
            return;
        }
        if (logger.isDebugEnabled()) {
            logger.debug("Requesturi : [{}]", request.getUri());
        }
        if (requestPathMatchesPrefix(request, config)) {
            final String path = extractPath(request, config);
            if (Greeting.matches(path)) {
                writeResponse(ctx.channel(), request, Greeting.response(request));
                return;
            }
            if (Info.matches(path)) {
                writeResponse(ctx.channel(), request, Info.response(config, request, ctx.alloc()));
                return;
            }
            if (Iframe.matches(path)) {
                writeResponse(ctx.channel(), request, Iframe.response(config, request, ctx.alloc()));
                return;
            }
            if (RawWebSocketTransport.matches(path)) {
                addTransportHandler(new RawWebSocketTransport(config), ctx);
                ctx.fireChannelRead(ReferenceCountUtil.retain(request));
                return;
            }
            final PathParams sessionPath = matches(path);
            if (sessionPath.matches()) {
                handleSession(config, request, ctx, sessionPath);
                return;
            }
        }
        writeNotFoundResponse(request, ctx);
    }

    private static String extractPath(final HttpRequest request, final SockJsServiceConfig config) {
        final String pathWithoutPrefix = request.getUri().replaceFirst(config.getPrefix(), "");
        return new QueryStringDecoder(pathWithoutPrefix).path();
    }

    private static boolean requestPathMatchesPrefix(final HttpRequest request, SockJsServiceConfig config) {
        final String path = new QueryStringDecoder(request.getUri()).path();
        return path.startsWith(config.getPrefix());
    }

    private static void handleSession(final SockJsServiceConfig config,
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

    private static SockJsSession getSession(final String sessionId, final SockJsServiceConfig config) {
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
         * @return TransportType.Type the type of the transport.
         */
        TransportType transport();
    }

    public static class MatchingSessionPath implements PathParams {
        private final String serverId;
        private final String sessionId;
        private final TransportType transport;

        public MatchingSessionPath(final String serverId, final String sessionId, final String transport) {
            this.serverId = serverId;
            this.sessionId = sessionId;
            this.transport = TransportType.valueOf(transport.toUpperCase());
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
        public TransportType transport() {
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
        public TransportType transport() {
            throw new UnsupportedOperationException("transport is not available in path");
        }
    }

}
