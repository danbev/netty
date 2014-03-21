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
package io.netty.handler.codec.sockjs;

import io.netty.channel.Channel;
import io.netty.channel.ChannelInitializer;
import io.netty.channel.ChannelPipeline;
import io.netty.channel.EventLoop;
import io.netty.channel.socket.nio.NioSocketChannel;
import io.netty.handler.codec.http.HttpHeaders.Names;
import io.netty.handler.codec.http.HttpObjectAggregator;
import io.netty.handler.codec.http.HttpRequestDecoder;
import io.netty.handler.codec.http.HttpResponseEncoder;
import io.netty.handler.codec.http.cors.CorsConfig;
import io.netty.handler.codec.http.cors.CorsConfig.Builder;
import io.netty.handler.codec.http.cors.CorsHandler;
import io.netty.handler.codec.sockjs.handler.SockJsHandler;

import java.nio.channels.SocketChannel;
import java.util.Date;
import java.util.concurrent.Callable;

public class DefaultSockJsChannel extends NioSocketChannel implements SockJsChannel {

    private final SockJsChannelConfig channelConfig;

    public DefaultSockJsChannel(Channel parent, EventLoop eventLoop, SocketChannel socket) {
        super(parent, ((SockJsEventLoop) eventLoop).delegate(), socket);
        channelConfig = new DefaultSockJsChannelConfig(this, socket.socket());

        pipeline().addLast(new ChannelInitializer<SockJsChannel>() {
            @Override
            protected void initChannel(final SockJsChannel ch) throws Exception {
                final SockJsChannelConfig config = ch.config();
                final ChannelInitializer<SockJsChannel> customInitializer = config.channelInitializer();
                if (customInitializer != null) {
                    ch.pipeline().addLast(customInitializer);
                } else {
                    addDefaultSockJsHandlers(ch.pipeline(), config.corsConfig());
                }
            }
        });
    }

    // TODO: This must be made configurable.
    public static Builder defaultCorsConfig() {
        return CorsConfig.anyOrigin()
                .allowCredentials()
                .preflightResponseHeader(Names.CACHE_CONTROL, "public, max-age=31536000")
                .preflightResponseHeader(Names.SET_COOKIE, "JSESSIONID=dummy;path=/")
                .preflightResponseHeader(Names.EXPIRES, new Callable<Date>() {
                    @Override
                    public Date call() throws Exception {
                        final Date date = new Date();
                        date.setTime(date.getTime() + 3600 * 1000);
                        return date;
                    }
                })
                .allowedRequestHeaders(Names.CONTENT_TYPE.toString())
                .maxAge(31536000);
    }

    @Override
    public SockJsChannelConfig config() {
        return channelConfig;
    }

    public static void addDefaultSockJsHandlers(final ChannelPipeline pipeline, final CorsConfig corsConfig) {
        pipeline.addLast("decoder", new HttpRequestDecoder());
        pipeline.addLast("encoder", new HttpResponseEncoder());
        pipeline.addLast("chucked", new HttpObjectAggregator(1048576));
        pipeline.addLast("cors", new CorsHandler(corsConfig));
        pipeline.addLast("sockjs", new SockJsHandler());
    }

}
