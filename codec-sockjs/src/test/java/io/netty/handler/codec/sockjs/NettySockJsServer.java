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

import io.netty.bootstrap.ServerBootstrap;
import io.netty.channel.Channel;
import io.netty.channel.ChannelInitializer;
import io.netty.channel.EventLoopGroup;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.oio.OioEventLoopGroup;
import io.netty.channel.socket.SocketChannel;
import io.netty.handler.codec.http.cors.CorsConfig;
import io.netty.handler.codec.sockjs.channel.nio.NioSockJsServerChannel;
import io.netty.handler.codec.sockjs.channel.oio.OioSockJsServerChannel;

import static io.netty.handler.codec.http.HttpMethod.*;
import static io.netty.handler.codec.sockjs.channel.SockJsChannelOption.*;

/**
 * A SockJS server that will start the services required for the
 * <a href="http://sockjs.github.io/sockjs-protocol/sockjs-protocol-0.3.3.html">sockjs-protocol</a> test suite,
 * enabling the python test suite to be run against Netty's SockJS implementation.
 */
public class NettySockJsServer {

    private final int port;

    public NettySockJsServer(final int port) {
        this.port = port;
    }

    public void run() throws Exception {
        final EventLoopGroup bossGroup = new NioEventLoopGroup();
        final EventLoopGroup workerGroup = new NioEventLoopGroup();
        try {
            final ServerBootstrap sb = new ServerBootstrap();
            sb.channel(NioSockJsServerChannel.class);
            sb.group(bossGroup, workerGroup);

            //sb.option(TLS, true);
            //sb.option(KEYSTORE, "path");
            //sb.option(KEYSTORE_PASSWORD, "path");

            final CorsConfig corsConfig = DefaultSockJsServiceConfig.defaultCorsConfig("test", "*", "localhost:8081")
                    .allowedRequestHeaders("a", "b", "c")
                    .allowNullOrigin()
                    .allowedRequestMethods(POST, GET, OPTIONS)
                    .build();

            sb.childHandler(new ChannelInitializer<SocketChannel>() {
                @Override
                protected void initChannel(final SocketChannel ch) throws Exception {
                    ch.pipeline().addLast(new SockJsEchoHandler());
                }
            });
            sb.option(PREFIX, "/echo");
            sb.childOption(MAX_STREAMING_BYTES_SIZE, 4096);
            sb.childOption(CORS_CONFIG, corsConfig);
            sb.childOption(HEARTBEAT_INTERVAL, 60000L);
            sb.register();

            sb.childHandler(new ChannelInitializer<SocketChannel>() {
                @Override
                protected void initChannel(final SocketChannel ch) throws Exception {
                    ch.pipeline().addLast(new SockJsCloseHandler());
                }
            });
            sb.option(PREFIX, "/close");
            sb.childOption(CORS_CONFIG, corsConfig);
            sb.childOption(HEARTBEAT_INTERVAL, 60000L);
            sb.register();

            sb.childHandler(new ChannelInitializer<SocketChannel>() {
                @Override
                protected void initChannel(final SocketChannel ch) throws Exception {
                    ch.pipeline().addLast(new SockJsEchoHandler());
                }
            });
            sb.option(PREFIX, "/cookie_needed_echo");
            sb.childOption(COOKIES_NEEDED, true);
            sb.childOption(CORS_CONFIG, corsConfig);
            sb.childOption(HEARTBEAT_INTERVAL, 60000L);
            sb.register();

            sb.childHandler(new ChannelInitializer<SocketChannel>() {
                @Override
                protected void initChannel(final SocketChannel ch) throws Exception {
                    ch.pipeline().addLast(new SockJsWSDisabledHandler());
                }
            });
            sb.option(PREFIX, "/disabled_websocket_echo");
            sb.childOption(WEBSOCKET_ENABLED, false);
            sb.childOption(CORS_CONFIG, corsConfig);
            sb.childOption(HEARTBEAT_INTERVAL, 60000L);
            sb.register();

            final Channel ch = sb.bind(port).sync().channel();
            System.out.println("Web socket server started on port [" + port + "], ");
            ch.closeFuture().sync();
        } finally {
            bossGroup.shutdownGracefully();
            workerGroup.shutdownGracefully();
        }
    }

    public static void main(final String[] args) throws Exception {
        final int port = args.length > 0 ? Integer.parseInt(args[0]) : 8081;
        new NettySockJsServer(port).run();
    }

}
