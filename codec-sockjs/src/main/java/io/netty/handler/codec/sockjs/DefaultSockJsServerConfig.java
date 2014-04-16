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

import io.netty.util.internal.StringUtil;

/**
 * Represents a configuration options for a SockJS ServerChannel.
 */
public class DefaultSockJsServerConfig implements SockJsServerConfig {

    private String prefix;
    private boolean tls;
    private String keyStore;
    private String keystorePassword;

    public DefaultSockJsServerConfig() {
    }

    public DefaultSockJsServerConfig(final String prefix) {
        this.prefix = prefix;
    }

    @Override
    public String getPrefix() {
        return prefix;
    }

    @Override
    public SockJsServerConfig setPrefix(final String prefix) {
        this.prefix = prefix;
        return this;
    }

    @Override
    public boolean isTls() {
        return tls;
    }

    @Override
    public SockJsServerConfig setTls(boolean tls) {
        this.tls = tls;
        return this;
    }

    @Override
    public String keyStore() {
        return keyStore;
    }

    @Override
    public SockJsServerConfig setKeyStore(String keyStore) {
        this.keyStore = keyStore;
        return this;
    }

    @Override
    public String keyStorePassword() {
        return keystorePassword;
    }

    @Override
    public SockJsServerConfig setKeyStorePassword(String password) {
        keystorePassword = password;
        return this;
    }

    public String toString() {
        return StringUtil.simpleClassName(this) + "[tls=" + tls +
                ", keyStore=" + keyStore +
                ']';
    }

}
