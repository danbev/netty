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
package io.netty.handler.codec.sockjs.oio;

import io.netty.channel.Channel;
import io.netty.channel.ChannelHandler;
import io.netty.channel.EventLoop;
import io.netty.channel.socket.oio.OioSocketChannel;
import io.netty.handler.codec.sockjs.SockJsChannel;
import io.netty.handler.codec.sockjs.SockJsChannelInitializer;

import java.net.Socket;

public class OioSockJsSocketChannel extends OioSocketChannel implements SockJsChannel {

    private final OioSockJsChannelConfig sockJsConfig;

    public OioSockJsSocketChannel(final Channel parent, final EventLoop eventLoop, final Socket socket) {
        super(parent, eventLoop, socket);
        sockJsConfig = new DefaultOioSockJsChannelConfig(this, socket);
        addSockJsHandler(new SockJsChannelInitializer());
    }

    @Override
    public OioSockJsChannelConfig config() {
        return sockJsConfig;
    }

    @Override
    public void addSockJsHandler(final ChannelHandler handler) {
        pipeline().addLast(handler);
    }
}

