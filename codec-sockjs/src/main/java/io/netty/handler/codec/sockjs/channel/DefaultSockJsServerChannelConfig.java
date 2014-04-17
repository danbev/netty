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

import io.netty.channel.Channel;
import io.netty.channel.ChannelInitializer;
import io.netty.channel.ChannelOption;
import io.netty.channel.DefaultChannelConfig;
import io.netty.handler.codec.sockjs.DefaultSockJsServerConfig;
import io.netty.handler.codec.sockjs.SockJsServerConfig;

import javax.net.ssl.SSLContext;
import java.util.Map;

import static io.netty.handler.codec.sockjs.channel.SockJsChannelOption.CHANNEL_INITIALIZER;
import static io.netty.handler.codec.sockjs.channel.SockJsChannelOption.PREFIX;
import static io.netty.handler.codec.sockjs.channel.SockJsChannelOption.SSL_CONTEXT;
import static io.netty.handler.codec.sockjs.channel.SockJsChannelOption.TLS;

public class DefaultSockJsServerChannelConfig extends DefaultChannelConfig implements SockJsServerChannelConfig {

    private final SockJsServerConfig config;

    public DefaultSockJsServerChannelConfig(final Channel channel) {
        this(channel, new DefaultSockJsServerConfig());
    }

    public DefaultSockJsServerChannelConfig(final Channel channel, final SockJsServerConfig sockJsConfig) {
        super(channel);
        config = sockJsConfig;
    }

    @Override
    public <T> boolean setOption(ChannelOption<T> option, T value) {
        if (option == PREFIX) {
            config.setPrefix((String) value);
        } else if (option == SSL_CONTEXT) {
            config.setSslContext((SSLContext) value);
            config.setTls(true);
        } else if (option == CHANNEL_INITIALIZER) {
            config.setChannelInitilizer((ChannelInitializer<?>) value);
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
        if (option == TLS) {
            return (T) Boolean.valueOf(isTls());
        }
        if (option == SSL_CONTEXT) {
            return (T) config.getSslContext();
        }
        if (option == CHANNEL_INITIALIZER) {
            return (T) config.getChannelInitializer();
        }
        return super.getOption(option);
    }

    @Override
    public Map<ChannelOption<?>, Object> getOptions() {
        return getOptions(super.getOptions(),
                PREFIX,
                TLS,
                SSL_CONTEXT,
                CHANNEL_INITIALIZER);
    }

    @Override
    public String getPrefix() {
        return config.getPrefix();
    }

    @Override
    public SockJsServerConfig setPrefix(final String prefix) {
        return config.setPrefix(prefix);
    }

    @Override
    public boolean isTls() {
        return config.isTls();
    }

    @Override
    public SockJsServerConfig setTls(final boolean tls) {
        return config.setTls(tls);
    }

    @Override
    public SSLContext getSslContext() {
        return config.getSslContext();
    }

    @Override
    public SockJsServerConfig setSslContext(final SSLContext sslContext) {
        return config.setSslContext(sslContext);
    }

    @Override
    public ChannelInitializer<?> getChannelInitializer() {
        return config.getChannelInitializer();
    }

    @Override
    public SockJsServerConfig setChannelInitilizer(final ChannelInitializer<?> channelInitilizer) {
        return config.setChannelInitilizer(channelInitilizer);
    }
}
