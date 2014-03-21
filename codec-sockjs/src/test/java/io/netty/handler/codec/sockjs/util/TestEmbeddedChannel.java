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
package io.netty.handler.codec.sockjs.util;

import io.netty.channel.ChannelConfig;
import io.netty.channel.ChannelHandlerInvoker;
import io.netty.channel.ChannelOutboundBuffer;
import io.netty.channel.ChannelPromise;
import io.netty.channel.embedded.EmbeddedChannel;
import io.netty.channel.socket.SocketChannel;
import io.netty.handler.codec.sockjs.DefaultSockJsChannelConfig;
import io.netty.handler.codec.sockjs.SockJsChannelConfig;

import java.net.Socket;
import java.net.SocketAddress;

import static org.mockito.Mockito.mock;

public class TestEmbeddedChannel extends EmbeddedChannel {

    private final SockJsChannelConfig config  = new DefaultSockJsChannelConfig(mock(SocketChannel.class),
            mock(Socket.class));

    public TestEmbeddedChannel() {
        // remove EmbeddedChannels LastInboundHandler channel handler or it will simply
        // store all messages written.
        pipeline().remove("EmbeddedChannel$LastInboundHandler#0");
    }

    @Override
    public Unsafe unsafe() {
        final AbstractUnsafe delegate = newUnsafe();
        return new TestUnsafe(delegate, new StubEmbeddedEventLoop(eventLoop()));
    }

    @Override
    public ChannelConfig config() {
        return config == null ? super.config() : config;
    }

    private static final class TestUnsafe implements Unsafe {

        private final Unsafe delegate;
        private final ChannelHandlerInvoker invoker;

        private TestUnsafe(final Unsafe delegate, final ChannelHandlerInvoker invoker) {
            this.delegate = delegate;
            this.invoker = invoker;
        }

        @Override
        public ChannelHandlerInvoker invoker() {
            return invoker;
        }

        @Override
        public SocketAddress localAddress() {
            return delegate.localAddress();
        }

        @Override
        public SocketAddress remoteAddress() {
            return delegate.remoteAddress();
        }

        @Override
        public void register(ChannelPromise promise) {
            delegate.register(promise);
        }

        @Override
        public void bind(SocketAddress localAddress, ChannelPromise promise) {
            delegate.bind(localAddress, promise);
        }

        @Override
        public void connect(SocketAddress remoteAddress, SocketAddress localAddress, ChannelPromise promise) {
            delegate.connect(remoteAddress, localAddress, promise);
        }

        @Override
        public void disconnect(ChannelPromise promise) {
            delegate.disconnect(promise);
        }

        @Override
        public void close(ChannelPromise promise) {
            delegate.close(promise);
        }

        @Override
        public void closeForcibly() {
            delegate.closeForcibly();
        }

        @Override
        public void beginRead() {
            delegate.beginRead();
        }

        @Override
        public void write(Object msg, ChannelPromise promise) {
            delegate.write(msg, promise);
        }

        @Override
        public void flush() {
            delegate.flush();
        }

        @Override
        public ChannelPromise voidPromise() {
            return delegate.voidPromise();
        }

        @Override
        public ChannelOutboundBuffer outboundBuffer() {
            return delegate.outboundBuffer();
        }
    }
}

