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
package io.netty.handler.codec.sockjs;

import io.netty.channel.ChannelInitializer;
import io.netty.handler.codec.http.cors.CorsConfig;
import io.netty.util.internal.StringUtil;

import java.util.HashSet;
import java.util.Iterator;
import java.util.Set;

/**
 * Represents a configuration options for a SockJs Channel.
 *
 */
public class DefaultSockJsConfig implements SockJsConfig {

    private String prefix;
    private boolean webSocketEnabled = true;
    private long webSocketHeartbeatInterval = -1;
    private final Set<String> webSocketProtocols = new HashSet<String>();
    private boolean cookiesNeeded;
    private String sockjsUrl = "http://cdn.sockjs.org/sockjs-0.3.4.min.js";
    private long sessionTimeout = 5000;
    private long heartbeatInterval = 25000;
    private int maxStreamingBytesSize = 128 * 1024;
    private boolean tls;
    private String keyStore;
    private String keystorePassword;
    private CorsConfig corsConfig = CorsConfig.withAnyOrigin().allowCredentials().maxAge(31536000).build();
    private ChannelInitializer<SockJsChannel> channelInitializer;

    public DefaultSockJsConfig() {
    }

    public DefaultSockJsConfig(final String prefix) {
        this.prefix = prefix;
    }

    @Override
    public String getPrefix() {
        return prefix;
    }

    @Override
    public SockJsConfig setPrefix(final String prefix) {
        this.prefix = prefix;
        return this;
    }

    @Override
    public boolean isWebSocketEnabled() {
        return webSocketEnabled;
    }

    @Override
    public SockJsConfig setWebSocketEnabled(boolean enable) {
        webSocketEnabled = enable;
        return this;
    }

    @Override
    public long webSocketHeartbeatInterval() {
        return webSocketHeartbeatInterval;
    }

    @Override
    public SockJsConfig setWebSocketHeartbeatInterval(long ms) {
        webSocketHeartbeatInterval = ms;
        return this;
    }

    @Override
    public Set<String> webSocketProtocol() {
        return webSocketProtocols;
    }

    @Override
    public SockJsConfig setWebSocketProtocol(Set<String> protocols) {
        webSocketProtocols.addAll(protocols);
        return this;
    }

    @Override
    public String webSocketProtocolCSV() {
        if (webSocketProtocols.isEmpty()) {
            return null;
        }
        final StringBuilder sb = new StringBuilder();
        final Iterator<String> iterator = webSocketProtocols.iterator();
        if (iterator.hasNext()) {
            sb.append(iterator.next());
            while (iterator.hasNext()) {
                sb.append(',').append(iterator.next());
            }
        }
        return sb.toString();
    }

    @Override
    public boolean areCookiesNeeded() {
        return cookiesNeeded;
    }

    @Override
    public SockJsConfig setCookiesNeeded(boolean needed) {
        cookiesNeeded = needed;
        return this;
    }

    @Override
    public String sockJsUrl() {
        return sockjsUrl;
    }

    @Override
    public SockJsConfig setSockJsUrl(String url) {
        sockjsUrl = url;
        return this;
    }

    @Override
    public long sessionTimeout() {
        return sessionTimeout;
    }

    @Override
    public SockJsConfig setSessionTimeout(long ms) {
        sessionTimeout = ms;
        return this;
    }

    @Override
    public long heartbeatInterval() {
        return heartbeatInterval;
    }

    @Override
    public SockJsConfig setHeartbeatInterval(long ms) {
        heartbeatInterval = ms;
        return this;
    }

    @Override
    public int maxStreamingBytesSize() {
        return maxStreamingBytesSize;
    }

    @Override
    public SockJsConfig setMaxStreamingBytesSize(int max) {
        maxStreamingBytesSize = max;
        return this;
    }

    @Override
    public boolean isTls() {
        return tls;
    }

    @Override
    public SockJsConfig setTls(boolean tls) {
        this.tls = tls;
        return this;
    }

    @Override
    public String keyStore() {
        return keyStore;
    }

    @Override
    public SockJsConfig setKeyStore(String keyStore) {
        this.keyStore = keyStore;
        return this;
    }

    @Override
    public String keyStorePassword() {
        return keystorePassword;
    }

    @Override
    public SockJsConfig setKeyStorePassword(String password) {
        keystorePassword = password;
        return this;
    }

    @Override
    public CorsConfig corsConfig() {
        return corsConfig;
    }

    @Override
    public SockJsConfig setCorsConfig(final CorsConfig corsConfig) {
        this.corsConfig = corsConfig;
        return this;
    }

    @Override
    public ChannelInitializer<SockJsChannel> channelInitializer() {
        return channelInitializer;
    }

    @Override
    public SockJsConfig setChannelInitializer(ChannelInitializer<SockJsChannel> init) {
        channelInitializer = init;
        return this;
    }

    public String toString() {
        return StringUtil.simpleClassName(this) + "[getPrefix=" + prefix +
                ", webSocketEnabled=" + webSocketEnabled +
                ", webSocketProtocols=" + webSocketProtocols +
                ", webSocketHeartbeatInterval=" + webSocketHeartbeatInterval +
                ", cookiesNeeded=" + cookiesNeeded +
                ", sockJsUrl=" + sockjsUrl +
                ", sessionTimeout=" + sessionTimeout +
                ", heartbeatInterval=" + heartbeatInterval +
                ", maxStreamingBytesSize=" + maxStreamingBytesSize +
                ", tls=" + tls +
                ", keyStore=" + keyStore +
                ", corsConfig=" + corsConfig +
                ", channelInitializer=" + channelInitializer +
                ']';
    }

}
