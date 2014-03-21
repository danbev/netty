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

import io.netty.buffer.ByteBufAllocator;
import io.netty.channel.ChannelInitializer;
import io.netty.channel.ChannelOption;
import io.netty.channel.MessageSizeEstimator;
import io.netty.channel.RecvByteBufAllocator;
import io.netty.channel.socket.DefaultSocketChannelConfig;
import io.netty.channel.socket.SocketChannel;
import io.netty.handler.codec.http.cors.CorsConfig;

import java.net.Socket;
import java.util.Set;

import static io.netty.handler.codec.sockjs.SockJsChannelOption.*;


/**
 * Represents a configuration options for a SockJs Channel.
 */
public class DefaultSockJsChannelConfig extends DefaultSocketChannelConfig implements SockJsChannelConfig {

    private final SockJsConfig config;

    public DefaultSockJsChannelConfig(final SocketChannel channel, final Socket javaSocket) {
        super(channel, javaSocket);
        config = new DefaultSockJsConfig();
    }

    @Override
    public <T> boolean setOption(ChannelOption<T> option, T value) {
        super.setOption(option, value);
        if (option == PREFIX) {
            config.setPrefix((String) value);
        } else if (option == WEBSOCKET_ENABLED) {
            config.setWebSocketEnabled((Boolean) value);
        } else if (option == WEBSOCKET_HEARTBEAT_INTERVAL) {
            config.setWebSocketHeartbeatInterval((Long) value);
        } else if (option == WEBSOCKET_PROTOCOLS) {
            config.setWebSocketProtocol((Set<String>) value);
        } else if (option == COOKIES_NEEDED) {
            config.setCookiesNeeded((Boolean) value);
        } else if (option == SOCKJS_URL) {
            config.setSockJsUrl((String) value);
        } else if (option == SESSION_TIMEOUT) {
            config.setSessionTimeout((Long) value);
        } else if (option == HEARTBEAT_INTERVAL) {
            config.setHeartbeatInterval((Long) value);
        } else if (option == MAX_STREAMING_BYTES_SIZE) {
            config.setMaxStreamingBytesSize((Integer) value);
        } else if (option == TLS) {
            config.setTls((Boolean) value);
        } else if (option == KEYSTORE) {
            config.setKeyStore((String) value);
        } else if (option == KEYSTORE_PASSWORD) {
            config.setKeyStorePassword((String) value);
        } else if (option == CORS_CONFIG) {
            config.setCorsConfig((CorsConfig) value);
        } else {
            return false;
        }
        return true;
    }

    @Override
    @SuppressWarnings("unchecked")
    public <T> T getOption(final ChannelOption<T> option) {
        if (option == PREFIX) {
            return (T) config.getPrefix();
        }
        if (option == WEBSOCKET_ENABLED) {
            return (T) Boolean.valueOf(config.isWebSocketEnabled());
        }
        if (option == WEBSOCKET_HEARTBEAT_INTERVAL) {
            return (T) Long.valueOf(config.webSocketHeartbeatInterval());
        }
        if (option == WEBSOCKET_PROTOCOLS) {
            return (T) config.webSocketProtocol();
        }
        if (option == COOKIES_NEEDED) {
            return (T) Boolean.valueOf(config.areCookiesNeeded());
        }
        if (option == SOCKJS_URL) {
            return (T) config.sockJsUrl();
        }
        if (option == SESSION_TIMEOUT) {
            return (T) Long.valueOf(config.sessionTimeout());
        }
        if (option == HEARTBEAT_INTERVAL) {
            return (T) Long.valueOf(config.heartbeatInterval());
        }
        if (option == MAX_STREAMING_BYTES_SIZE) {
            return (T) Integer.valueOf(config.maxStreamingBytesSize());
        }
        if (option == TLS) {
            return (T) Boolean.valueOf(isTls());
        }
        if (option == KEYSTORE) {
            return (T) config.keyStore();
        }
        if (option == KEYSTORE_PASSWORD) {
            return (T) config.keyStorePassword();
        }
        if (option == CORS_CONFIG) {
            return (T) config.corsConfig();
        }
        return super.getOption(option);
    }

    @Override
    public DefaultSockJsChannelConfig setConnectTimeoutMillis(final int connectTimeoutMillis) {
        super.setConnectTimeoutMillis(connectTimeoutMillis);
        return this;
    }

    @Override
    public DefaultSockJsChannelConfig setMaxMessagesPerRead(final int maxMessagesPerRead) {
        super.setMaxMessagesPerRead(maxMessagesPerRead);
        return this;
    }

    @Override
    public DefaultSockJsChannelConfig setWriteSpinCount(final int writeSpinCount) {
        super.setWriteSpinCount(writeSpinCount);
        return this;
    }

    @Override
    public DefaultSockJsChannelConfig setAllocator(final ByteBufAllocator allocator) {
        super.setAllocator(allocator);
        return this;
    }

    @Override
    public DefaultSockJsChannelConfig setRecvByteBufAllocator(final RecvByteBufAllocator allocator) {
        super.setRecvByteBufAllocator(allocator);
        return this;
    }

    @Override
    public DefaultSockJsChannelConfig setAutoRead(final boolean autoRead) {
        super.setAutoRead(autoRead);
        return this;
    }

    @Override
    public DefaultSockJsChannelConfig setWriteBufferHighWaterMark(final int writeBufferHighWaterMark) {
        super.setWriteBufferHighWaterMark(writeBufferHighWaterMark);
        return this;
    }

    @Override
    public DefaultSockJsChannelConfig setWriteBufferLowWaterMark(final int writeBufferLowWaterMark) {
        super.setWriteBufferLowWaterMark(writeBufferLowWaterMark);
        return this;
    }

    @Override
    public DefaultSockJsChannelConfig setMessageSizeEstimator(final MessageSizeEstimator estimator) {
        super.setMessageSizeEstimator(estimator);
        return this;
    }

    @Override
    public String getPrefix() {
        return config.getPrefix();
    }

    @Override
    public SockJsConfig setPrefix(String prefix) {
        return config.setPrefix(prefix);
    }

    @Override
    public boolean isWebSocketEnabled() {
        return config.isWebSocketEnabled();
    }

    @Override
    public SockJsConfig setWebSocketEnabled(boolean enable) {
        return config.setWebSocketEnabled(enable);
    }

    @Override
    public long webSocketHeartbeatInterval() {
        return config.webSocketHeartbeatInterval();
    }

    @Override
    public SockJsConfig setWebSocketHeartbeatInterval(long ms) {
        return config.setWebSocketHeartbeatInterval(ms);
    }

    @Override
    public Set<String> webSocketProtocol() {
        return config.webSocketProtocol();
    }

    @Override
    public SockJsConfig setWebSocketProtocol(Set<String> protocols) {
        return config.setWebSocketProtocol(protocols);
    }

    @Override
    public String webSocketProtocolCSV() {
        return config.webSocketProtocolCSV();
    }

    @Override
    public boolean areCookiesNeeded() {
        return config.areCookiesNeeded();
    }

    @Override
    public SockJsConfig setCookiesNeeded(boolean needed) {
        return config.setCookiesNeeded(needed);
    }

    @Override
    public String sockJsUrl() {
        return config.sockJsUrl();
    }

    @Override
    public SockJsConfig setSockJsUrl(String url) {
        return config.setSockJsUrl(url);
    }

    @Override
    public long sessionTimeout() {
        return config.sessionTimeout();
    }

    @Override
    public SockJsConfig setSessionTimeout(long ms) {
        return config.setSessionTimeout(ms);
    }

    @Override
    public long heartbeatInterval() {
        return config.heartbeatInterval();
    }

    @Override
    public SockJsConfig setHeartbeatInterval(long ms) {
        return config.setHeartbeatInterval(ms);
    }

    @Override
    public int maxStreamingBytesSize() {
        return config.maxStreamingBytesSize();
    }

    @Override
    public SockJsConfig setMaxStreamingBytesSize(int max) {
        return config.setMaxStreamingBytesSize(max);
    }

    @Override
    public boolean isTls() {
        return config.isTls();
    }

    @Override
    public SockJsConfig setTls(boolean tls) {
        return config.setTls(tls);
    }

    @Override
    public String keyStore() {
        return config.keyStore();
    }

    @Override
    public SockJsConfig setKeyStore(String keyStore) {
        return config.setKeyStore(keyStore);
    }

    @Override
    public String keyStorePassword() {
        return config.keyStorePassword();
    }

    @Override
    public SockJsConfig setKeyStorePassword(String password) {
        return config.setKeyStorePassword(password);
    }

    @Override
    public CorsConfig corsConfig() {
        return config.corsConfig();
    }

    @Override
    public SockJsConfig setCorsConfig(CorsConfig corsConfig) {
        return config.setCorsConfig(corsConfig);
    }

    @Override
    public SockJsConfig setChannelInitializer(ChannelInitializer<SockJsChannel> init) {
        return config.setChannelInitializer(init);
    }

    @Override
    public ChannelInitializer<SockJsChannel> channelInitializer() {
        return config.channelInitializer();
    }
}
