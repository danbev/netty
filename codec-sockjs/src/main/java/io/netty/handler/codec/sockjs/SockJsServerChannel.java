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

import io.netty.channel.EventLoop;
import io.netty.channel.EventLoopGroup;
import io.netty.channel.socket.nio.NioServerSocketChannel;
import io.netty.util.internal.logging.InternalLogger;
import io.netty.util.internal.logging.InternalLoggerFactory;

import java.nio.channels.SocketChannel;
import java.util.List;

/**
 * A server socket channel that extends NioServerSocketChannel to
 * return DefaultSockJsChannel instead of NioSocketChannels.
 */
public class SockJsServerChannel extends NioServerSocketChannel {

    private static final InternalLogger logger = InternalLoggerFactory.getInstance(SockJsServerChannel.class);

    public SockJsServerChannel(final EventLoop eventLoop, final EventLoopGroup childGroup) {
        super(eventLoop, childGroup);
    }

    @Override
    protected int doReadMessages(final List<Object> buf) throws Exception {
        final SocketChannel ch = javaChannel().accept();
        try {
            if (ch != null) {
                buf.add(new DefaultSockJsChannel(this, childEventLoopGroup().next(), ch));
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

}
