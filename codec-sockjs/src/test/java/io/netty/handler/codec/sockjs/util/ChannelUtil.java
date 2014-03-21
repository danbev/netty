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

import io.netty.channel.ChannelHandler;
import io.netty.channel.ChannelHandlerAdapter;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelPromise;
import io.netty.channel.embedded.EmbeddedChannel;
import io.netty.handler.codec.http.HttpObjectAggregator;
import io.netty.handler.codec.http.HttpResponseEncoder;
import io.netty.handler.codec.http.cors.CorsConfig;
import io.netty.handler.codec.http.websocketx.WebSocket00FrameEncoder;
import io.netty.handler.codec.http.websocketx.WebSocket07FrameEncoder;
import io.netty.handler.codec.http.websocketx.WebSocket08FrameEncoder;
import io.netty.handler.codec.http.websocketx.WebSocket13FrameEncoder;
import io.netty.handler.codec.sockjs.DefaultSockJsChannel;
import io.netty.handler.codec.sockjs.SockJsChannelOption;

public final class ChannelUtil {

    private ChannelUtil() {
    }

    public static EmbeddedChannel wsSockJsPipeline(final String prefix, final ChannelHandler handler) {
        final TestEmbeddedChannel ch = new TestEmbeddedChannel();
        DefaultSockJsChannel.addDefaultSockJsHandlers(ch.pipeline(), DefaultSockJsChannel.defaultCorsConfig().build());
        ch.pipeline().addLast(new WsCodecRemover());
        ch.pipeline().addLast(handler);
        ch.pipeline().remove(HttpObjectAggregator.class);
        setDefaultSockJsChannelOptions(ch, prefix);
        return ch;
    }

    public static TestEmbeddedChannel sockJsPipeline(final String prefix, final ChannelHandler handler) {
        final TestEmbeddedChannel ch = new TestEmbeddedChannel();
        DefaultSockJsChannel.addDefaultSockJsHandlers(ch.pipeline(), DefaultSockJsChannel.defaultCorsConfig().build());
        ch.pipeline().addLast(handler);
        setDefaultSockJsChannelOptions(ch, prefix);

        // remove the HttpResponseEncoder so that we can check the plain HttpResponses.
        ch.pipeline().remove(HttpResponseEncoder.class);
        ch.pipeline().remove(HttpObjectAggregator.class);
        return ch;
    }

    public static TestEmbeddedChannel sockJsPipeline(final String prefix,
                                                      final ChannelHandler handler,
                                                      final CorsConfig corsConfig) {
        final TestEmbeddedChannel ch = new TestEmbeddedChannel();
        DefaultSockJsChannel.addDefaultSockJsHandlers(ch.pipeline(), corsConfig);
        ch.pipeline().addLast(handler);
        // remove the HttpResponseEncoder so that we can check the plain HttpResponses.
        ch.pipeline().remove(HttpResponseEncoder.class);
        ch.pipeline().remove(HttpObjectAggregator.class);
        setDefaultSockJsChannelOptions(ch, prefix);
        return ch;
    }

    public static void setDefaultSockJsChannelOptions(final EmbeddedChannel ch, final String prefix) {
        ch.config().setOption(SockJsChannelOption.PREFIX, prefix);
        ch.config().setOption(SockJsChannelOption.COOKIES_NEEDED, true);
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
