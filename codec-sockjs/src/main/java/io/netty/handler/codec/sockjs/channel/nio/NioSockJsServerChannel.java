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
package io.netty.handler.codec.sockjs.channel.nio;

import io.netty.channel.AbstractServerChannel;
import io.netty.channel.Channel;
import io.netty.channel.ChannelHandlerAdapter;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.EventLoop;
import io.netty.channel.EventLoopGroup;
import io.netty.channel.ServerChannel;
import io.netty.channel.socket.nio.NioServerSocketChannel;
import io.netty.handler.codec.sockjs.channel.DefaultSockJsServerChannelConfig;
import io.netty.handler.codec.sockjs.channel.SockJsServerChannel;
import io.netty.handler.codec.sockjs.channel.SockJsServerChannelConfig;
import io.netty.handler.codec.sockjs.channel.SockJsServerSocketChannelAdapter;
import io.netty.handler.codec.sockjs.SockJsService;
import io.netty.util.internal.logging.InternalLogger;
import io.netty.util.internal.logging.InternalLoggerFactory;

import java.net.SocketAddress;
import java.nio.channels.SocketChannel;
import java.util.List;
import java.util.concurrent.ConcurrentHashMap;

/**
 * NioSockJsServerChannel is a {@link ServerChannel} implementation which will
 * only create the actual {@link NioServerSocketChannel} when bind is called.
 *
 * This is done to allow multiple child handlers to be registered and configured
 * after which bind will be called and the ServerSocketChannel created.
 * This is done to provide multiplexing of SockJS services.
 */
public class NioSockJsServerChannel extends AbstractServerChannel implements SockJsServerChannel {

    private static final InternalLogger logger = InternalLoggerFactory.getInstance(NioSockJsServerChannel.class);

    private static final ConcurrentHashMap<String, SockJsService> services =
            new ConcurrentHashMap<String, SockJsService>();
    private final SockJsServerChannelConfig config;
    private NioServerSocketChannel socketChannel;

    public NioSockJsServerChannel(EventLoop eventLoop, EventLoopGroup childGroup) {
        super(eventLoop, childGroup);
        config = new DefaultSockJsServerChannelConfig(this);
    }

    @Override
    public SockJsService serviceFor(final String prefix) {
        return services.get(prefix);
    }

    @Override
    protected boolean isCompatible(EventLoop loop) {
        return true;
    }

    @Override
    protected void doRegister() throws Exception {
        final String prefix = config.getPrefix();
        services.putIfAbsent(prefix, new SockJsService(prefix, pipeline().removeFirst()));
    }

    @Override
    protected SocketAddress localAddress0() {
        return socketChannel.localAddress();
    }

    @Override
    protected void doBind(SocketAddress localAddress) throws Exception {
        socketChannel = new NioServerSocketChannel(eventLoop(), childEventLoopGroup().next()) {
            @Override
            protected int doReadMessages(List<Object> buf) throws Exception {
                final SocketChannel ch = javaChannel().accept();
                try {
                    if (ch != null) {
                        final SockJsServerSocketChannelAdapter parent = new SockJsServerSocketChannelAdapter(
                                NioSockJsServerChannel.this, this);
                        buf.add(new NioSockJsSocketChannel(parent, childEventLoopGroup().next(), ch));
                        return 1;
                    }
                } catch (final Throwable t) {
                    logger.warn("Failed to create a new channel from an accepted socket.", t);
                    try {
                        ch.close();
                    } catch (final Throwable t2) {
                        logger.warn("Failed to close a socket.", t2);
                    }
                }
                return 0;
            }
        };
        socketChannel.pipeline().addLast(new ChannelHandlerAdapter() {
            @Override
            public void channelRead(ChannelHandlerContext ctx, Object msg) throws Exception {
                // Since we have removed the ServerBootstrapAcceptor it will no longer add the
                // childhandler or register the channel. We will take over the responsibility to
                // register the channel.
                final Channel channel = (Channel) msg;
                channel.unsafe().register(channel.newPromise());
            }
        });

        socketChannel.unsafe().register(socketChannel.newPromise());
        socketChannel.bind(localAddress);
    }

    @Override
    protected void doClose() throws Exception {
        socketChannel.close();
    }

    @Override
    protected void doBeginRead() throws Exception {
        socketChannel.unsafe().beginRead();
    }

    @Override
    public SockJsServerChannelConfig config() {
        return config;
    }

    @Override
    public boolean isOpen() {
        return true;
    }

    @Override
    public boolean isActive() {
        return false;
    }
}
