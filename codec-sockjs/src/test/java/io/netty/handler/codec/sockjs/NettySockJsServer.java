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
import io.netty.channel.socket.ServerSocketChannel;

import static io.netty.handler.codec.sockjs.SockJsChannelOption.*;

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
            sb.channel(SockJsServerChannel.class);
            sb.group(bossGroup, workerGroup);

            sb.childHandler(new ChannelInitializer<SockJsChannel>() {
                @Override
                protected void initChannel(final SockJsChannel ch) throws Exception {
                    ch.pipeline().addLast("echo", new SockJsEchoHandler());
                }
            });
            sb.childOption(PREFIX, "/echo");
            sb.childOption(MAX_STREAMING_BYTES_SIZE, 4096);
            sb.childOption(WEBSOCKET_ENABLED, true);
            sb.childOption(SESSION_TIMEOUT, Long.MAX_VALUE);

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
