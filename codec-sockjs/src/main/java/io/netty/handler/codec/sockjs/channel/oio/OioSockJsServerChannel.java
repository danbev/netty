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
package io.netty.handler.codec.sockjs.channel.oio;

import io.netty.channel.AbstractServerChannel;
import io.netty.channel.ChannelHandlerAdapter;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.EventLoop;
import io.netty.channel.oio.OioEventLoopGroup;
import io.netty.channel.socket.oio.OioServerSocketChannel;
import io.netty.handler.codec.sockjs.channel.DefaultSockJsServerChannelConfig;
import io.netty.handler.codec.sockjs.channel.SockJsServerChannel;
import io.netty.handler.codec.sockjs.channel.SockJsServerChannelConfig;
import io.netty.handler.codec.sockjs.channel.SockJsServerSocketChannelAdapter;
import io.netty.handler.codec.sockjs.SockJsService;
import io.netty.util.internal.logging.InternalLogger;
import io.netty.util.internal.logging.InternalLoggerFactory;

import java.net.Socket;
import java.net.SocketAddress;
import java.net.SocketTimeoutException;
import java.util.List;
import java.util.concurrent.ConcurrentHashMap;

public class OioSockJsServerChannel extends AbstractServerChannel implements SockJsServerChannel {

    private static final InternalLogger logger = InternalLoggerFactory.getInstance(OioSockJsServerChannel.class);

    private static final ConcurrentHashMap<String, SockJsService> services =
            new ConcurrentHashMap<String, SockJsService>();

    private final SockJsServerChannelConfig config;
    private OioServerSocketChannel oio;

    public OioSockJsServerChannel() {
        config = new DefaultSockJsServerChannelConfig(this);
    }

    @Override
    protected void doRegister() throws Exception {
        final String prefix = config.getPrefix();
        services.putIfAbsent(prefix, new SockJsService(prefix, pipeline().removeFirst()));
    }

    @Override
    public SockJsService serviceFor(String prefix) {
        return services.get(prefix);
    }

    @Override
    public SockJsServerChannelConfig config() {
        return config;
    }

    @Override
    public boolean isOpen() {
        return oio == null || oio.isOpen();
    }

    @Override
    public boolean isActive() {
        return oio != null && oio.isActive();
    }

    @Override
    protected void doBind(SocketAddress localAddress) throws Exception {
        oio = new OioServerSocketChannel() {
            @Override
            protected int doReadMessages(List<Object> buf) throws Exception {
                if (serverSocket().isClosed()) {
                    return -1;
                }
                try {
                    final Socket s = serverSocket().accept();
                    try {
                        if (s != null) {
                            final SockJsServerSocketChannelAdapter parent =
                                    new SockJsServerSocketChannelAdapter(OioSockJsServerChannel.this, this);
                            buf.add(new OioSockJsSocketChannel(parent, s));
                            return 1;
                        }
                    } catch (final Throwable t) {
                        logger.warn("Failed to create a new channel from an accepted socket.", t);
                        if (s != null) {
                            try {
                                s.close();
                            } catch (final Throwable t2) {
                                logger.warn("Failed to close a socket.", t2);
                            }
                        }
                    }
                } catch (final SocketTimeoutException e) {
                    // Expected
                }
                return 0;
            }
        };
        oio.config().setMaxMessagesPerRead(config().getMaxMessagesPerRead());
        oio.config().setSoTimeout(50);
        oio.pipeline().addLast(new ChannelHandlerAdapter() {
            @Override
            public void channelRead(final ChannelHandlerContext ctx, final Object msg) throws Exception {
                final OioSockJsSocketChannel channel = (OioSockJsSocketChannel) msg;
                channel.pipeline().addLast(config.getChannelInitializer());
                eventLoop().parent().register(channel);
            }
        });
        eventLoop().register(oio);
        oio.unsafe().bind(localAddress, oio.newPromise());
    }

    @Override
    protected boolean isCompatible(EventLoop loop) {
        return loop.parent() instanceof OioEventLoopGroup;
    }

    @Override
    protected SocketAddress localAddress0() {
        return oio != null ? oio.localAddress() : null;
    }

    @Override
    protected void doClose() throws Exception {
        if (oio != null) {
            oio.close();
        }
    }

    @Override
    protected void doBeginRead() throws Exception {
    }
}
