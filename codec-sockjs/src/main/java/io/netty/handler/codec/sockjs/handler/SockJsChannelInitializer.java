/*
 * Copyright 2014 The Netty Project
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

import io.netty.channel.ChannelInitializer;
import io.netty.channel.ChannelPipeline;
import io.netty.channel.socket.SocketChannel;
import io.netty.handler.codec.http.HttpObjectAggregator;
import io.netty.handler.codec.http.HttpRequestDecoder;
import io.netty.handler.codec.http.HttpResponseEncoder;
import io.netty.handler.codec.sockjs.SockJsServerConfig;
import io.netty.handler.codec.sockjs.channel.SockJsChannelOption;
import io.netty.handler.ssl.SslHandler;

import javax.net.ssl.SSLEngine;

/**
 * A default {@link ChannelInitializer} for SockJS that sets up HTTP/HTTPS.
 *
 * This is only a default initializer can users can override it by configuring
 * this using {@link SockJsChannelOption#CHANNEL_INITIALIZER}.
 */
public class SockJsChannelInitializer extends ChannelInitializer<SocketChannel> {

    private final SockJsServerConfig sockjsServerConfig;

    public SockJsChannelInitializer(final SockJsServerConfig sockjsServerConfig) {
        this.sockjsServerConfig = sockjsServerConfig;
    }

    @Override
    protected void initChannel(final SocketChannel ch) throws Exception {
        final ChannelPipeline pipeline = ch.pipeline();
        if (sockjsServerConfig.isTls()) {
            final SSLEngine sslEngine = sockjsServerConfig.getSslContext().createSSLEngine();
            sslEngine.setUseClientMode(false);
            pipeline.addLast("ssl", new SslHandler(sslEngine));
        }
        pipeline.addLast("decoder", new HttpRequestDecoder());
        pipeline.addLast("encoder", new HttpResponseEncoder());
        pipeline.addLast("chucked", new HttpObjectAggregator(1048576));
        pipeline.addLast("mux", new SockJsMultiplexer());
    }

}
