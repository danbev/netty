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
package io.netty.handler.codec.sockjs.channel;

import io.netty.buffer.ByteBufAllocator;
import io.netty.channel.Channel;
import io.netty.channel.ChannelFuture;
import io.netty.channel.ChannelId;
import io.netty.channel.ChannelMetadata;
import io.netty.channel.ChannelPipeline;
import io.netty.channel.ChannelProgressivePromise;
import io.netty.channel.ChannelPromise;
import io.netty.channel.EventLoop;
import io.netty.channel.EventLoopGroup;
import io.netty.channel.socket.ServerSocketChannel;
import io.netty.handler.codec.sockjs.SockJsService;
import io.netty.util.Attribute;
import io.netty.util.AttributeKey;

import java.net.InetSocketAddress;
import java.net.SocketAddress;

/**
 * An adapter between a {@link ServerSocketChannel} and a {@link SockJsServerChannel} enabling
 * any type of ServerSocketChannel to become a {@link SockJsServerSocketChannel}.
 */
public class SockJsServerSocketChannelAdapter implements SockJsServerSocketChannel {

    private final SockJsServerChannel sockJsServerChannel;
    private final ServerSocketChannel delegate;
    private final SockJsServerSocketChannelConfig config;

    public SockJsServerSocketChannelAdapter(final SockJsServerChannel sockJsServerChannel,
                                            final ServerSocketChannel delegate) {
        this.sockJsServerChannel = sockJsServerChannel;
        this.delegate = delegate;
        config = new DefaultSockJsServerSocketChannelConfig(sockJsServerChannel.config(), delegate.config());
    }

    public SockJsService serviceFor(final String prefix) {
        return sockJsServerChannel.serviceFor(prefix);
    }

    @Override
    public ChannelId id() {
        return delegate.id();
    }

    @Override
    public EventLoop eventLoop() {
        return delegate.eventLoop();
    }

    @Override
    public Channel parent() {
        return delegate;
    }

    @Override
    public SockJsServerSocketChannelConfig config() {
        return config;
    }

    @Override
    public boolean isOpen() {
        return delegate.isOpen();
    }

    @Override
    public boolean isRegistered() {
        return delegate.isRegistered();
    }

    @Override
    public boolean isActive() {
        return delegate.isActive();
    }

    @Override
    public ChannelMetadata metadata() {
        return delegate.metadata();
    }

    @Override
    public InetSocketAddress localAddress() {
        return delegate.localAddress();
    }

    @Override
    public InetSocketAddress remoteAddress() {
        return delegate.remoteAddress();
    }

    @Override
    public ChannelFuture closeFuture() {
        return delegate.closeFuture();
    }

    @Override
    public boolean isWritable() {
        return delegate.isWritable();
    }

    @Override
    public Unsafe unsafe() {
        return delegate.unsafe();
    }

    @Override
    public ChannelPipeline pipeline() {
        return delegate.pipeline();
    }

    @Override
    public ByteBufAllocator alloc() {
        return delegate.alloc();
    }

    @Override
    public ChannelPromise newPromise() {
        return delegate.newPromise();
    }

    @Override
    public ChannelProgressivePromise newProgressivePromise() {
        return delegate.newProgressivePromise();
    }

    @Override
    public ChannelFuture newSucceededFuture() {
        return delegate.newSucceededFuture();
    }

    @Override
    public ChannelFuture newFailedFuture(Throwable cause) {
        return delegate.newFailedFuture(cause);
    }

    @Override
    public ChannelPromise voidPromise() {
        return delegate.voidPromise();
    }

    @Override
    public ChannelFuture bind(SocketAddress localAddress) {
        return delegate.bind(localAddress);
    }

    @Override
    public ChannelFuture connect(SocketAddress remoteAddress) {
        return delegate.connect(remoteAddress);
    }

    @Override
    public ChannelFuture connect(SocketAddress remoteAddress, SocketAddress localAddress) {
        return delegate.connect(remoteAddress, localAddress);
    }

    @Override
    public ChannelFuture disconnect() {
        return delegate.disconnect();
    }

    @Override
    public ChannelFuture close() {
        return delegate.close();
    }

    @Override
    public ChannelFuture bind(SocketAddress localAddress, ChannelPromise promise) {
        return delegate.bind(localAddress, promise);
    }

    @Override
    public ChannelFuture connect(SocketAddress remoteAddress, ChannelPromise promise) {
        return delegate.connect(remoteAddress, promise);
    }

    @Override
    public ChannelFuture connect(SocketAddress remoteAddress, SocketAddress localAddress, ChannelPromise promise) {
        return delegate.connect(remoteAddress, localAddress, promise);
    }

    @Override
    public ChannelFuture disconnect(ChannelPromise promise) {
        return delegate.disconnect(promise);
    }

    @Override
    public ChannelFuture close(ChannelPromise promise) {
        return delegate.close(promise);
    }

    @Override
    public Channel read() {
        return delegate.read();
    }

    @Override
    public ChannelFuture write(Object msg) {
        return delegate.write(msg);
    }

    @Override
    public ChannelFuture write(Object msg, ChannelPromise promise) {
        return delegate.write(msg, promise);
    }

    @Override
    public Channel flush() {
        return delegate.flush();
    }

    @Override
    public ChannelFuture writeAndFlush(Object msg, ChannelPromise promise) {
        return delegate.writeAndFlush(msg, promise);
    }

    @Override
    public ChannelFuture writeAndFlush(Object msg) {
        return delegate.writeAndFlush(msg);
    }

    @Override
    public EventLoopGroup childEventLoopGroup() {
        return delegate.childEventLoopGroup();
    }

    @Override
    public <T> Attribute<T> attr(AttributeKey<T> key) {
        return delegate.attr(key);
    }

    @Override
    public int compareTo(Channel o) {
        return delegate.compareTo(o);
    }
}
