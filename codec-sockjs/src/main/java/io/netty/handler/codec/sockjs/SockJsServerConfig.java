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

import io.netty.channel.ChannelInitializer;

/**
 * The configuration properties for the SockJS server.
 */
public interface SockJsServerConfig {

    /**
     * The getPrefix/name, of the SockJS service.
     * For example, in the url "http://localhost/echo/111/12345/xhr", 'echo' is the getPrefix.
     *
     * @return {@code String} the getPrefix/name of the SockJS service.
     */
    String getPrefix();

    /**
     * Sets the SockJS service prefix.
     *
     * @param prefix the prefix for the SockJS service.
     * @return SockJsConfig to support method chaining.
     */
    SockJsServerConfig setPrefix(String prefix);

    /**
     * Determines whether transport layer security (TLS) should be used.
     *
     * @return {@code true} if transport layer security should be used.
     */
    boolean isTls();

    /**
     * Enables/disables Transport Layer Security (TLS)
     *
     * @param tls if true TLS will be enabled.
     * @return SockJsServerConfig to support method chaining.
     */
    SockJsServerConfig setTls(boolean tls);

    /**
     * Returns the keystore to be used if transport layer security is enabled.
     *
     * @return {@code String} the path to the keystore to be used
     */
    String keyStore();

    /**
     * Sets the keystore to be used.
     *
     * @param keyStore the keystore to be used.
     * @return SockJsServerConfig to support method chaining.
     */
    SockJsServerConfig setKeyStore(String keyStore);

    /**
     * Returns the keystore password to be used if transport layer security is enabled.
     *
     * @return {@code String} the password to the configured keystore
     */
    String keyStorePassword();

    /**
     * Sets the keystore password.
     *
     * @param password the password for the keystore.
     * @return SockJsServerConfig to support method chaining.
     */
    SockJsServerConfig setKeyStorePassword(String password);

    /**
     * Gets the {@link ChannelInitializer} used to set up HTTP/HTTP handlers for SockJS.
     *
     * @return channelInitilizer the {@link ChannelInitializer} used to set up HTTP/HTTPS for SockJS.
     */
    ChannelInitializer<?> getChannelInitializer();

    /**
     * Sets the {@link ChannelInitializer} used to set up HTTP/HTTP handlers for SockJS.
     *
     * @param channelInitilizer the {@link ChannelInitializer} used to set up HTTP/HTTPS for SockJS.
     * @return SockJsServerConfig to support method chaining.
     */
    SockJsServerConfig setChannelInitilizer(ChannelInitializer<?> channelInitilizer);

}
