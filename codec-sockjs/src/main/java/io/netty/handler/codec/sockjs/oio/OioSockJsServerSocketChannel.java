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
package io.netty.handler.codec.sockjs.oio;

import io.netty.channel.EventLoop;
import io.netty.channel.EventLoopGroup;
import io.netty.channel.socket.oio.OioServerSocketChannel;
import io.netty.util.internal.logging.InternalLogger;
import io.netty.util.internal.logging.InternalLoggerFactory;

import java.net.Socket;
import java.net.SocketTimeoutException;
import java.util.List;

public class OioSockJsServerSocketChannel extends OioServerSocketChannel {

    private static final InternalLogger logger = InternalLoggerFactory.getInstance(OioSockJsServerSocketChannel.class);

    public OioSockJsServerSocketChannel(final EventLoop eventLoop, final EventLoopGroup childGroup) {
        super(eventLoop, childGroup);
    }

    @Override
    protected int doReadMessages(final List<Object> buf) throws Exception {
        if (serverSocket().isClosed()) {
            return -1;
        }

        try {
            final Socket s = serverSocket().accept();
            try {
                if (s != null) {
                    buf.add(new OioSockJsSocketChannel(this, childEventLoopGroup().next(), s));
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

}
