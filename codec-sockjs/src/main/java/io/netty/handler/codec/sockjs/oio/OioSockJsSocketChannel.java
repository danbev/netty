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
import io.netty.channel.EventLoop;
import io.netty.channel.socket.oio.OioSocketChannel;
import io.netty.handler.codec.http.HttpObjectAggregator;
import io.netty.handler.codec.http.HttpRequestDecoder;
import io.netty.handler.codec.http.HttpResponseEncoder;
import io.netty.handler.codec.sockjs.handler.SockJsMultiplexer;

import java.net.Socket;

public class OioSockJsSocketChannel extends OioSocketChannel {

    private boolean registered;

    public OioSockJsSocketChannel(Channel parent, EventLoop eventLoop, Socket socket) {
        super(parent, eventLoop, socket);
    }

    @Override
    protected void doBeginRead() throws Exception {
        super.doBeginRead();
    }

    @Override
    protected void doRegister() throws Exception {
        if (!registered) {
            super.doRegister();
            pipeline().addLast("decoder", new HttpRequestDecoder());
            pipeline().addLast("encoder", new HttpResponseEncoder());
            pipeline().addLast("chucked", new HttpObjectAggregator(1048576));
            pipeline().addLast("mux", new SockJsMultiplexer());
            registered = true;
        }
    }

}

