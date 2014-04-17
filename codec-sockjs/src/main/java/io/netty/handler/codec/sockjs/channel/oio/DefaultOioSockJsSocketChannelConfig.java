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

import io.netty.channel.ChannelOption;
import io.netty.channel.socket.SocketChannel;
import io.netty.channel.socket.oio.DefaultOioSocketChannelConfig;
import io.netty.handler.codec.http.cors.CorsConfig;
import io.netty.handler.codec.sockjs.DefaultSockJsServiceConfig;
import io.netty.handler.codec.sockjs.SockJsServiceConfig;

import java.net.Socket;
import java.util.Map;
import java.util.Set;

import static io.netty.handler.codec.sockjs.channel.SockJsChannelOption.*;

/**
 * Represents a configuration options for a SockJS Channel.
 */
public class DefaultOioSockJsSocketChannelConfig extends DefaultOioSocketChannelConfig
        implements OioSockJsSocketChannelConfig {

    private final SockJsServiceConfig config;

    public DefaultOioSockJsSocketChannelConfig(final SocketChannel channel, Socket socket) {
        super(channel, socket);
        config = new DefaultSockJsServiceConfig();
    }

    @Override
    public <T> boolean setOption(ChannelOption<T> option, T value) {
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
        } else if (option == CORS_CONFIG) {
            config.setCorsConfig((CorsConfig) value);
        } else if (option == TLS) {
            config.setTls((Boolean) value);
        } else {
            return super.setOption(option, value);
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
        if (option == CORS_CONFIG) {
            return (T) config.corsConfig();
        }
        if (option == TLS) {
            return (T) Boolean.valueOf(isTls());
        }
        return super.getOption(option);
    }

    @Override
    public Map<ChannelOption<?>, Object> getOptions() {
        return getOptions(super.getOptions(), PREFIX,
                WEBSOCKET_ENABLED,
                WEBSOCKET_HEARTBEAT_INTERVAL,
                WEBSOCKET_PROTOCOLS,
                COOKIES_NEEDED,
                SOCKJS_URL,
                SESSION_TIMEOUT,
                HEARTBEAT_INTERVAL,
                MAX_STREAMING_BYTES_SIZE,
                TLS,
                CORS_CONFIG);
    }

    @Override
    public String getPrefix() {
        return config.getPrefix();
    }

    @Override
    public SockJsServiceConfig setPrefix(String prefix) {
        return config.setPrefix(prefix);
    }

    @Override
    public boolean isWebSocketEnabled() {
        return config.isWebSocketEnabled();
    }

    @Override
    public SockJsServiceConfig setWebSocketEnabled(boolean enable) {
        return config.setWebSocketEnabled(enable);
    }

    @Override
    public long webSocketHeartbeatInterval() {
        return config.webSocketHeartbeatInterval();
    }

    @Override
    public SockJsServiceConfig setWebSocketHeartbeatInterval(long ms) {
        return config.setWebSocketHeartbeatInterval(ms);
    }

    @Override
    public Set<String> webSocketProtocol() {
        return config.webSocketProtocol();
    }

    @Override
    public SockJsServiceConfig setWebSocketProtocol(Set<String> protocols) {
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
    public SockJsServiceConfig setCookiesNeeded(boolean needed) {
        return config.setCookiesNeeded(needed);
    }

    @Override
    public String sockJsUrl() {
        return config.sockJsUrl();
    }

    @Override
    public SockJsServiceConfig setSockJsUrl(String url) {
        return config.setSockJsUrl(url);
    }

    @Override
    public long sessionTimeout() {
        return config.sessionTimeout();
    }

    @Override
    public SockJsServiceConfig setSessionTimeout(long ms) {
        return config.setSessionTimeout(ms);
    }

    @Override
    public long heartbeatInterval() {
        return config.heartbeatInterval();
    }

    @Override
    public SockJsServiceConfig setHeartbeatInterval(long ms) {
        return config.setHeartbeatInterval(ms);
    }

    @Override
    public int maxStreamingBytesSize() {
        return config.maxStreamingBytesSize();
    }

    @Override
    public SockJsServiceConfig setMaxStreamingBytesSize(int max) {
        return config.setMaxStreamingBytesSize(max);
    }

    @Override
    public CorsConfig corsConfig() {
        return config.corsConfig();
    }

    @Override
    public SockJsServiceConfig setCorsConfig(CorsConfig corsConfig) {
        return config.setCorsConfig(corsConfig);
    }

    @Override
    public boolean isTls() {
        return config.isTls();
    }

    @Override
    public SockJsServiceConfig setTls(boolean tls) {
        return config.setTls(tls);
    }

}