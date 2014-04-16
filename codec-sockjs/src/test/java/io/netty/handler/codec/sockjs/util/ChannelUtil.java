/*
 * Copyright 2013 The Netty Project
 *
 * The Netty Project licenses this file to you under the Apache License, version 2.0 (the "License");
 * you may not use this file except in compliance with the License. You may obtain a copy of the License at:
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software distributed under the License is distributed
 * on an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the License for
 * the specific language governing permissions and limitations under the License.
 */
package io.netty.handler.codec.sockjs.util;

import io.netty.buffer.ByteBufAllocator;
import io.netty.channel.Channel;
import io.netty.channel.ChannelHandler;
import io.netty.channel.ChannelHandlerAdapter;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelPromise;
import io.netty.channel.DefaultMessageSizeEstimator;
import io.netty.channel.embedded.EmbeddedChannel;
import io.netty.channel.socket.ServerSocketChannel;
import io.netty.channel.socket.ServerSocketChannelConfig;
import io.netty.channel.socket.SocketChannel;
import io.netty.handler.codec.http.HttpObjectAggregator;
import io.netty.handler.codec.http.HttpResponseEncoder;
import io.netty.handler.codec.http.cors.CorsConfig;
import io.netty.handler.codec.http.websocketx.WebSocket00FrameEncoder;
import io.netty.handler.codec.http.websocketx.WebSocket07FrameEncoder;
import io.netty.handler.codec.http.websocketx.WebSocket08FrameEncoder;
import io.netty.handler.codec.http.websocketx.WebSocket13FrameEncoder;
import io.netty.handler.codec.sockjs.DefaultSockJsServerChannelConfig;
import io.netty.handler.codec.sockjs.DefaultSockJsServerConfig;
import io.netty.handler.codec.sockjs.DefaultSockJsSocketChannelConfig;
import io.netty.handler.codec.sockjs.SockJsChannelOption;
import io.netty.handler.codec.sockjs.SockJsCloseHandler;
import io.netty.handler.codec.sockjs.SockJsEchoHandler;
import io.netty.handler.codec.sockjs.SockJsServerChannel;
import io.netty.handler.codec.sockjs.SockJsServerChannelConfig;
import io.netty.handler.codec.sockjs.SockJsServerConfig;
import io.netty.handler.codec.sockjs.SockJsServerSocketChannelAdapter;
import io.netty.handler.codec.sockjs.SockJsService;
import io.netty.handler.codec.sockjs.SockJsSocketChannelConfig;

import java.net.Socket;

import static io.netty.handler.codec.sockjs.DefaultSockJsSocketChannelConfig.*;
import static org.mockito.Matchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

public final class ChannelUtil {

    private ChannelUtil() {
    }

    public static EmbeddedChannel wsSockJsChannel(final String prefix, final ChannelHandler handler) {
        return wsSockJsChannel(prefix, handler, sockJsChannelConfig());
    }

    public static EmbeddedChannel wsSockJsChannel(final String prefix,
                                                  final ChannelHandler handler,
                                                  final SockJsSocketChannelConfig config) {
        final TestEmbeddedChannel ch = newTestEmbeddedChannel(prefix, handler, config);
        addDefaultSockJsHandlers(ch.pipeline());

        // just add a mock to simulate a ServerBootstrap childhandler
        ch.pipeline().addLast("ServerBootstrap$ServerBootstrapAcceptor#0", mock(ChannelHandler.class));

        ch.pipeline().addLast(new WsCodecRemover());
        ch.pipeline().remove(HttpObjectAggregator.class);
        setDefaultSockJsChannelOptions(ch, prefix);
        return ch;
    }

    public static TestEmbeddedChannel sockJsChannel(final String prefix, final ChannelHandler handler) {
        return sockJsChannel(prefix, handler, sockJsChannelConfig());
    }

    public static TestEmbeddedChannel sockJsChannel(final String prefix,
                                                    final ChannelHandler handler,
                                                    final SockJsSocketChannelConfig config) {
        final TestEmbeddedChannel ch = newTestEmbeddedChannel(prefix, handler, config);
        addDefaultSockJsHandlers(ch.pipeline());

        // just add a mock to simulate a ServerBootstrap childhandler
        ch.pipeline().addLast("ServerBootstrap$ServerBootstrapAcceptor#0", mock(ChannelHandler.class));
        // remove the HttpResponseEncoder so that we can check the plain HttpResponses.
        ch.pipeline().remove(HttpResponseEncoder.class);
        ch.pipeline().remove(HttpObjectAggregator.class);
        setDefaultSockJsChannelOptions(ch, prefix);
        return ch;
    }
    private static TestEmbeddedChannel newTestEmbeddedChannel(final String prefix,
                                                       final ChannelHandler handler,
                                                       final SockJsSocketChannelConfig config) {
        config.setPrefix(prefix);
        return new TestEmbeddedChannel(sockJsServerChannel(handler, config), config);
    }

    public static SockJsSocketChannelConfig sockJsChannelConfig() {
        return new DefaultSockJsSocketChannelConfig(mock(SocketChannel.class), mock(Socket.class));
    }

    public static SockJsSocketChannelConfig sockJsChannelConfig(final CorsConfig corsConfig) {
        final SockJsSocketChannelConfig channelConfig = sockJsChannelConfig();
        channelConfig.setCorsConfig(corsConfig);
        return channelConfig;
    }

    private static SockJsServerSocketChannelAdapter sockJsServerChannel(final ChannelHandler handler,
                                                           final SockJsSocketChannelConfig config) {

        final SockJsService sockJsService = new SockJsService(config.getPrefix(), handler);

        final SockJsServerConfig sockJsServerConfig = new DefaultSockJsServerConfig(config.getPrefix());
        final SockJsServerChannelConfig sockJsServerChannelConfig =
                new DefaultSockJsServerChannelConfig(mock(Channel.class), sockJsServerConfig);

        final SockJsServerChannel sockJsServerChannel = mock(SockJsServerChannel.class);
        when(sockJsServerChannel.config()).thenReturn(sockJsServerChannelConfig);
        when(sockJsServerChannel.serviceFor(any(String.class))).thenReturn(sockJsService);

        final ServerSocketChannel serverSocketChannel = mock(ServerSocketChannel.class);
        final ServerSocketChannelConfig serverSocketChannelConfig = mock(ServerSocketChannelConfig.class);
        when(serverSocketChannelConfig.isAutoRead()).thenReturn(Boolean.TRUE);
        when(serverSocketChannelConfig.getMessageSizeEstimator()).thenReturn(DefaultMessageSizeEstimator.DEFAULT);
        when(serverSocketChannelConfig.getAllocator()).thenReturn(ByteBufAllocator.DEFAULT);
        when(serverSocketChannel.config()).thenReturn(serverSocketChannelConfig);

        return new SockJsServerSocketChannelAdapter(sockJsServerChannel, serverSocketChannel);
    }

    public static void setDefaultSockJsChannelOptions(final EmbeddedChannel ch, final String prefix) {
        ch.config().setOption(SockJsChannelOption.PREFIX, prefix);
        ch.config().setOption(SockJsChannelOption.COOKIES_NEEDED, true);
    }

    public static EmbeddedChannel echoChannel() {
        return sockJsChannel("/echo", new SockJsEchoHandler());
    }

    public static EmbeddedChannel echoChannel(final String prefix) {
        return sockJsChannel(prefix, new SockJsEchoHandler());
    }

    public static EmbeddedChannel webSocketEchoChannel() {
        return wsSockJsChannel("/echo", new SockJsEchoHandler());
    }

    public static EmbeddedChannel webSocketEchoChannel(final SockJsSocketChannelConfig config) {
        return wsSockJsChannel("/echo", new SockJsEchoHandler(), config);
    }

    public static EmbeddedChannel echoChannel(final SockJsSocketChannelConfig config) {
        return sockJsChannel("/echo", new SockJsEchoHandler(), config);
    }

    public static EmbeddedChannel closeChannel() {
        return sockJsChannel("/close", new SockJsCloseHandler());
    }

    public static EmbeddedChannel closeChannel(final SockJsSocketChannelConfig config) {
        return sockJsChannel("/close", new SockJsCloseHandler(), config);
    }

    public static EmbeddedChannel webSocketCloseChannel() {
        return wsSockJsChannel("/close", new SockJsCloseHandler());
    }

    public static EmbeddedChannel webSocketCloseChannel(final SockJsSocketChannelConfig config) {
        return wsSockJsChannel("/close", new SockJsCloseHandler(), config);
    }

    /**
     * This ChannelHandlers sole purpose is to remove WebSocket frame encoders
     * so that WebSocketFrame's are not encoded and can be asserted as is.
     */
    private static class WsCodecRemover extends ChannelHandlerAdapter {

        @Override
        public void write(final ChannelHandlerContext ctx, final Object msg, final ChannelPromise channelPromise)
                throws Exception {
            if (ctx.pipeline().get(WebSocket00FrameEncoder.class) != null) {
                ctx.pipeline().remove(WebSocket00FrameEncoder.class);
            }
            if (ctx.pipeline().get(WebSocket08FrameEncoder.class) != null) {
                ctx.pipeline().remove(WebSocket08FrameEncoder.class);
            }
            if (ctx.pipeline().get(WebSocket07FrameEncoder.class) != null) {
                ctx.pipeline().remove(WebSocket07FrameEncoder.class);
            }
            if (ctx.pipeline().get(WebSocket13FrameEncoder.class) != null) {
                ctx.pipeline().remove(WebSocket13FrameEncoder.class);
            }
            ctx.writeAndFlush(msg, channelPromise);
        }
    }

}
