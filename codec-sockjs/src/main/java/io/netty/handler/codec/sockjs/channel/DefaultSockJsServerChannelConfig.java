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
import io.netty.channel.ChannelOption;
import io.netty.channel.DefaultChannelConfig;
import io.netty.handler.codec.sockjs.DefaultSockJsServerConfig;
import io.netty.handler.codec.sockjs.SockJsServerConfig;

import java.util.Map;

import static io.netty.handler.codec.sockjs.channel.SockJsChannelOption.PREFIX;
import static io.netty.handler.codec.sockjs.channel.SockJsChannelOption.KEYSTORE;
import static io.netty.handler.codec.sockjs.channel.SockJsChannelOption.KEYSTORE_PASSWORD;
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
        } else if (option == TLS) {
            config.setTls((Boolean) value);
        } else if (option == KEYSTORE) {
            config.setKeyStore((String) value);
        } else if (option == KEYSTORE_PASSWORD) {
            config.setKeyStorePassword((String) value);
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
        if (option == KEYSTORE) {
            return (T) config.keyStore();
        }
        if (option == KEYSTORE_PASSWORD) {
            return (T) config.keyStorePassword();
        }
        return super.getOption(option);
    }

    @Override
    public Map<ChannelOption<?>, Object> getOptions() {
        return getOptions(super.getOptions(),
                PREFIX,
                TLS,
                KEYSTORE,
                KEYSTORE_PASSWORD);
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
    public SockJsServerConfig setTls(boolean tls) {
        return config.setTls(tls);
    }

    @Override
    public String keyStore() {
        return config.keyStore();
    }

    @Override
    public SockJsServerConfig setKeyStore(String keyStore) {
        return config.setKeyStore(keyStore);
    }

    @Override
    public String keyStorePassword() {
        return config.keyStorePassword();
    }

    @Override
    public SockJsServerConfig setKeyStorePassword(String password) {
        return config.setKeyStorePassword(password);
    }
}
