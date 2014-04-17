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
package io.netty.handler.codec.sockjs.channel;

import io.netty.buffer.ByteBufAllocator;
import io.netty.channel.ChannelConfig;
import io.netty.channel.ChannelInitializer;
import io.netty.channel.ChannelOption;
import io.netty.channel.MessageSizeEstimator;
import io.netty.channel.RecvByteBufAllocator;
import io.netty.channel.socket.ServerSocketChannelConfig;
import io.netty.handler.codec.sockjs.SockJsServerConfig;

import java.util.Map;

public class DefaultSockJsServerSocketChannelConfig implements SockJsServerSocketChannelConfig {

    private final ServerSocketChannelConfig serverSocketChannelConfig;
    private final SockJsServerChannelConfig sockJsConfig;

    public DefaultSockJsServerSocketChannelConfig(final SockJsServerChannelConfig sockJsConfig,
                                                  final ServerSocketChannelConfig serverSocketChannelConfig) {
        this.sockJsConfig = sockJsConfig;
        this.serverSocketChannelConfig = serverSocketChannelConfig;
    }

    @Override
    public int getBacklog() {
        return serverSocketChannelConfig.getBacklog();
    }

    @Override
    public ServerSocketChannelConfig setBacklog(int backlog) {
        return serverSocketChannelConfig.setBacklog(backlog);
    }

    @Override
    public boolean isReuseAddress() {
        return serverSocketChannelConfig.isReuseAddress();
    }

    @Override
    public ServerSocketChannelConfig setReuseAddress(boolean reuseAddress) {
        return serverSocketChannelConfig.setReuseAddress(reuseAddress);
    }

    @Override
    public int getReceiveBufferSize() {
        return serverSocketChannelConfig.getReceiveBufferSize();
    }

    @Override
    public ServerSocketChannelConfig setReceiveBufferSize(int receiveBufferSize) {
        return serverSocketChannelConfig.setReceiveBufferSize(receiveBufferSize);
    }

    @Override
    public ServerSocketChannelConfig setPerformancePreferences(int connectionTime, int latency, int bandwidth) {
        return serverSocketChannelConfig.setPerformancePreferences(connectionTime, latency, bandwidth);
    }

    @Override
    public Map<ChannelOption<?>, Object> getOptions() {
        final Map<ChannelOption<?>, Object> options = serverSocketChannelConfig.getOptions();
        options.putAll(sockJsConfig.getOptions());
        return options;
    }

    @Override
    public boolean setOptions(Map<ChannelOption<?>, ?> options) {
        if (sockJsConfig.setOptions(options)) {
            return true;
        }
        return serverSocketChannelConfig.setOptions(options);
    }

    @Override
    public <T> T getOption(ChannelOption<T> option) {
        T t = sockJsConfig.getOption(option);
        if (t == null) {
            t = serverSocketChannelConfig.getOption(option);
        }
        return t;
    }

    @Override
    public <T> boolean setOption(ChannelOption<T> option, T value) {
        if (sockJsConfig.setOption(option, value)) {
            return true;
        }
        return serverSocketChannelConfig.setOption(option, value);
    }

    @Override
    public int getConnectTimeoutMillis() {
        return serverSocketChannelConfig.getConnectTimeoutMillis();
    }

    @Override
    public ServerSocketChannelConfig setConnectTimeoutMillis(int connectTimeoutMillis) {
        return serverSocketChannelConfig.setConnectTimeoutMillis(connectTimeoutMillis);
    }

    @Override
    public int getMaxMessagesPerRead() {
        return serverSocketChannelConfig.getMaxMessagesPerRead();
    }

    @Override
    public ServerSocketChannelConfig setMaxMessagesPerRead(int maxMessagesPerRead) {
        return serverSocketChannelConfig.setMaxMessagesPerRead(maxMessagesPerRead);
    }

    @Override
    public int getWriteSpinCount() {
        return serverSocketChannelConfig.getWriteSpinCount();
    }

    @Override
    public ServerSocketChannelConfig setWriteSpinCount(int writeSpinCount) {
        return serverSocketChannelConfig.setWriteSpinCount(writeSpinCount);
    }

    @Override
    public ByteBufAllocator getAllocator() {
        return serverSocketChannelConfig.getAllocator();
    }

    @Override
    public ServerSocketChannelConfig setAllocator(ByteBufAllocator allocator) {
        return serverSocketChannelConfig.setAllocator(allocator);
    }

    @Override
    public RecvByteBufAllocator getRecvByteBufAllocator() {
        return serverSocketChannelConfig.getRecvByteBufAllocator();
    }

    @Override
    public ServerSocketChannelConfig setRecvByteBufAllocator(RecvByteBufAllocator allocator) {
        return serverSocketChannelConfig.setRecvByteBufAllocator(allocator);
    }

    @Override
    public boolean isAutoRead() {
        return serverSocketChannelConfig.isAutoRead();
    }

    @Override
    public ServerSocketChannelConfig setAutoRead(boolean autoRead) {
        return serverSocketChannelConfig.setAutoRead(autoRead);
    }

    @Override
    public int getWriteBufferHighWaterMark() {
        return serverSocketChannelConfig.getWriteBufferHighWaterMark();
    }

    @Override
    public ChannelConfig setWriteBufferHighWaterMark(int writeBufferHighWaterMark) {
        return serverSocketChannelConfig.setWriteBufferHighWaterMark(writeBufferHighWaterMark);
    }

    @Override
    public int getWriteBufferLowWaterMark() {
        return serverSocketChannelConfig.getWriteBufferLowWaterMark();
    }

    @Override
    public ChannelConfig setWriteBufferLowWaterMark(int writeBufferLowWaterMark) {
        return serverSocketChannelConfig.setWriteBufferLowWaterMark(writeBufferLowWaterMark);
    }

    @Override
    public MessageSizeEstimator getMessageSizeEstimator() {
        return serverSocketChannelConfig.getMessageSizeEstimator();
    }

    @Override
    public ServerSocketChannelConfig setMessageSizeEstimator(MessageSizeEstimator estimator) {
        return serverSocketChannelConfig.setMessageSizeEstimator(estimator);
    }

    @Override
    public String getPrefix() {
        return sockJsConfig.getPrefix();
    }

    @Override
    public SockJsServerConfig setPrefix(String prefix) {
        return sockJsConfig.setPrefix(prefix);
    }

    @Override
    public boolean isTls() {
        return sockJsConfig.isTls();
    }

    @Override
    public SockJsServerConfig setTls(boolean tls) {
        return sockJsConfig.setTls(tls);
    }

    @Override
    public String keyStore() {
        return sockJsConfig.keyStore();
    }

    @Override
    public SockJsServerConfig setKeyStore(String keyStore) {
        return sockJsConfig.setKeyStore(keyStore);
    }

    @Override
    public String keyStorePassword() {
        return sockJsConfig.keyStorePassword();
    }

    @Override
    public SockJsServerConfig setKeyStorePassword(String password) {
        return sockJsConfig.setKeyStorePassword(password);
    }

    @Override
    public ChannelInitializer<?> getChannelInitializer() {
        return sockJsConfig.getChannelInitializer();
    }

    @Override
    public SockJsServerConfig setChannelInitilizer(ChannelInitializer<?> channelInitilizer) {
        return sockJsConfig.setChannelInitilizer(channelInitilizer);
    }

}
