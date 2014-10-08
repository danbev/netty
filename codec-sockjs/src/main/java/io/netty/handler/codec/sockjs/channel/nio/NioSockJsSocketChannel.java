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

import io.netty.channel.Channel;
import io.netty.channel.socket.nio.NioSocketChannel;
import io.netty.handler.codec.sockjs.channel.DefaultSockJsSocketChannelConfig;
import io.netty.handler.codec.sockjs.channel.SockJsServerSocketChannelConfig;
import io.netty.handler.codec.sockjs.channel.SockJsSocketChannelConfig;

import java.nio.channels.SocketChannel;

public class NioSockJsSocketChannel extends NioSocketChannel {

    private final SockJsSocketChannelConfig config;

    public NioSockJsSocketChannel(final Channel parent, final SocketChannel socketChannel) {
        super(parent, socketChannel);
        config = new DefaultSockJsSocketChannelConfig(this, socketChannel.socket());
        // Some SockJS transports need to know if TLS is in use, we set this pass this setting along
        // to the SocketChannel config.
        final SockJsServerSocketChannelConfig parentConfig = (SockJsServerSocketChannelConfig) parent.config();
        config.setTls(parentConfig.isTls());
    }

    @Override
    public SockJsSocketChannelConfig config() {
        return config;
    }

}
