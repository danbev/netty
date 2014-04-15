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

import io.netty.handler.codec.http.cors.CorsConfig;

import java.util.Set;

public interface SockJsConfig {

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
    SockJsConfig setPrefix(String prefix);

    /**
     * Determines whether WebSocket support will not be enabled.
     *
     * @return {@code true} if WebSocket support is enabled.
     */
    boolean isWebSocketEnabled();

    /**
     * Enables/disables WebSocket support.
     *
     * @param enable if true then WebSocket support will be enabled.
     * @return SockJsConfig to support method chaining.
     */
    SockJsConfig setWebSocketEnabled(boolean enable);

    /**
     * The WebSocket heartbeat interval.
     *
     * This might be required in certain environments where idle connections
     * are closed by a proxy. It is a separate value from the hearbeat that
     * the streaming protocols use as it is often desirable to have a much
     * larger value for it.
     *
     * @return {@code long} how often, in ms, that a WebSocket heartbeat should be sent
     */
    long webSocketHeartbeatInterval();

    /**
     * Sets the WebSocket heartbeat interval
     *
     * @param ms the interval in milliseconds.
     * @return SockJsConfig to support method chaining.
     */
    SockJsConfig setWebSocketHeartbeatInterval(long ms);

    /**
     * If WebSockets are in use the this give the oppertunity to specify
     * what 'WebSocket-Protocols' should be returned and supported by this
     * SockJS session.
     *
     * @return {@code Set<String>} of WebSocket protocols supported.
     */
    Set<String> webSocketProtocol();

    /**
     * Sets the WebSocket protocols that this configuration supports.
     *
     * @param protocols the WebSocket protocols that are supported.
     * @return SockJsConfig to support method chaining.
     */
    SockJsConfig setWebSocketProtocol(Set<String> protocols);

    /**
     * If WebSockets are in use the this give the oppertunity to specify
     * what 'WebSocket-Protocols' should be returned and supported by this
     * SockJS session.
     *
     * @return {@code String} A comma separated value String with the WebSocket protocols supported
     */
    String webSocketProtocolCSV();

    /**
     * Determines if a {@code JSESSIONID} cookie will be set. This is used by some
     * load balancers to enable session stickyness.
     *
     * @return {@code true} if a {@code JSESSIONID} cookie should be set.
     */
    boolean areCookiesNeeded();

    /**
     * Configures if a {@code JSESSIONID} cookie will be set.
     *
     * @param needed if true then a JSESSIONID will be set.
     * @return SockJsConfig to support method chaining.
     */
    SockJsConfig setCookiesNeeded(boolean needed);

    /**
     * The url to the sock-js-<version>.json. This is used by the 'getPrefix/iframe' protocol and
     * the url is replaced in the script returned to the client. This allows for configuring
     * the version of sockjs used. By default it is 'http://cdn.sockjs.org/sockjs-0.3.4.min.js'.
     *
     * @return {@code String} the url to the sockjs version to be used.
     */
    String sockJsUrl();

    /**
     * Sets the SockJS url to be used with the iframe protocol.
     *
     * @param url the url.
     * @return SockJsConfig to support method chaining.
     */
    SockJsConfig setSockJsUrl(String url);

    /**
     * A time out for inactive sessions.
     *
     * @return {@code long} the timeout in ms. The default is 5000ms.
     */
    long sessionTimeout();

    /**
     * Sets the session timeout
     *
     * @param ms the timeout in milliseconds.
     * @return SockJsConfig to support method chaining.
     */
    SockJsConfig setSessionTimeout(long ms);

    /**
     * A heartbeat interval.
     *
     * @return {@code long} how often, in ms, that a heartbeat should be sent
     */
    long heartbeatInterval();

    /**
     * Sets the heartbeat interval.
     *
     * @param ms the heartbeat interval in milliseconds.
     * @return SockJsConfig to support method chaining.
     */
    SockJsConfig setHeartbeatInterval(long ms);

    /**
     * The max number of types that a streaming transport protocol should allow to be returned
     * before closing the connection, forcing the client to reconnect. This is done so that the
     * responseText in the XHR Object will not grow and be come an issue for the client. Instead,
     * by forcing a reconnect the client will create a new XHR object and this can be see as a
     * form of garbage collection.
     *
     * @return {@code int} the max number of bytes that can be written. Default is 131072.
     */
    int maxStreamingBytesSize();

    /**
     * Sets the max streaming bytes size.
     *
     * @param max the maximum size for streaming bytes, after which a new connection will be foreced.
     * @return SockJsConfig to support method chaining.
     */
    SockJsConfig setMaxStreamingBytesSize(int max);

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
     * @return SockJsConfig to support method chaining.
     */
    SockJsConfig setTls(boolean tls);

    /**
     * Returns the keystore to be used if transport layer security is enabled.
     * TODO: This should probably be moved into the SockJsServerChannel and not be per service.
     *
     * @return {@code String} the path to the keystore to be used
     */
    String keyStore();

    /**
     * Sets the keystore to be used.
     * TODO: This should probably be moved into the SockJsServerChannel and not be per service.
     *
     * @param keyStore the keystore to be used.
     * @return SockJsConfig to support method chaining.
     */
    SockJsConfig setKeyStore(String keyStore);

    /**
     * Returns the keystore password to be used if transport layer security is enabled.
     * TODO: This should probably be moved into the SockJsServerChannel and not be per service.
     *
     * @return {@code String} the password to the configured keystore
     */
    String keyStorePassword();

    /**
     * Sets the keystore password.
     * TODO: This should probably be moved into the SockJsServerChannel and not be per service.
     *
     * @param password the password for the keystore.
     * @return SockJsConfig to support method chaining.
     */
    SockJsConfig setKeyStorePassword(String password);

    /**
     * Returns the CORS configuration for this SockJS configuration
     *
     * @return CorsConfiguration the CORS configuration for this SockJS config.
     */
    CorsConfig corsConfig();

    /**
     * Sets the CORS configuration for this SockJS configuration
     *
     * @return SockJsConfig to support method chaining.
     */
    SockJsConfig setCorsConfig(CorsConfig corsConfig);

}
