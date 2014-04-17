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

import javax.net.ssl.SSLContext;
import javax.net.ssl.SSLEngine;

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
     * Returns the {@link SSLContext} to be used if transport layer security is enabled.
     *
     * @return {@code SSLContext} to be used with the {@link SSLEngine}.
     */
    SSLContext getSslContext();

    /**
     * Sets the {@link SSLContext} to be used if transport layer security is enabled.
     *
     * @param sslContext the {@link SSLContext} to be used with the {@link SSLEngine}.
     * @return SockJsServerConfig to support method chaining.
     */
    SockJsServerConfig setSslContext(SSLContext sslContext);

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
