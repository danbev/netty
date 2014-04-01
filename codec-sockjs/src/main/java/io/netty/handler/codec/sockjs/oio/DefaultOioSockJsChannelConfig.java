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
package io.netty.handler.codec.sockjs.oio;

import io.netty.channel.ChannelInitializer;
import io.netty.channel.ChannelOption;
import io.netty.channel.socket.oio.DefaultOioSocketChannelConfig;
import io.netty.handler.codec.http.cors.CorsConfig;
import io.netty.handler.codec.sockjs.DefaultSockJsChannelConfig;
import io.netty.handler.codec.sockjs.SockJsChannel;
import io.netty.handler.codec.sockjs.SockJsChannelConfig;
import io.netty.handler.codec.sockjs.SockJsConfig;

import java.net.Socket;
import java.util.Set;

public class DefaultOioSockJsChannelConfig extends DefaultOioSocketChannelConfig implements OioSockJsChannelConfig {

    private final SockJsChannelConfig sockJsChannelConfig;

    public DefaultOioSockJsChannelConfig(final OioSockJsSocketChannel channel, final Socket socket) {
        super(channel, socket);
        sockJsChannelConfig = new DefaultSockJsChannelConfig(channel, socket);
    }

    @Override
    public <T> T getOption(final ChannelOption<T> option) {
        return sockJsChannelConfig.getOption(option);
    }

    @Override
    public <T> boolean setOption(final ChannelOption<T> option, final T value) {
        return sockJsChannelConfig.setOption(option, value);
    }

    @Override
    public String getPrefix() {
        return sockJsChannelConfig.getPrefix();
    }

    @Override
    public SockJsConfig setPrefix(final String prefix) {
        return sockJsChannelConfig.setPrefix(prefix);
    }

    @Override
    public boolean isWebSocketEnabled() {
        return sockJsChannelConfig.isWebSocketEnabled();
    }

    @Override
    public SockJsConfig setWebSocketEnabled(final boolean enable) {
        return sockJsChannelConfig.setWebSocketEnabled(enable);
    }

    @Override
    public long webSocketHeartbeatInterval() {
        return sockJsChannelConfig.webSocketHeartbeatInterval();
    }

    @Override
    public SockJsConfig setWebSocketHeartbeatInterval(final long ms) {
        return sockJsChannelConfig.setWebSocketHeartbeatInterval(ms);
    }

    @Override
    public Set<String> webSocketProtocol() {
        return sockJsChannelConfig.webSocketProtocol();
    }

    @Override
    public SockJsConfig setWebSocketProtocol(final Set<String> protocols) {
        return sockJsChannelConfig.setWebSocketProtocol(protocols);
    }

    @Override
    public String webSocketProtocolCSV() {
        return sockJsChannelConfig.webSocketProtocolCSV();
    }

    @Override
    public boolean areCookiesNeeded() {
        return sockJsChannelConfig.areCookiesNeeded();
    }

    @Override
    public SockJsConfig setCookiesNeeded(final boolean needed) {
        return sockJsChannelConfig.setCookiesNeeded(needed);
    }

    @Override
    public String sockJsUrl() {
        return sockJsChannelConfig.sockJsUrl();
    }

    @Override
    public SockJsConfig setSockJsUrl(final String url) {
        return sockJsChannelConfig.setSockJsUrl(url);
    }

    @Override
    public long sessionTimeout() {
        return sockJsChannelConfig.sessionTimeout();
    }

    @Override
    public SockJsConfig setSessionTimeout(final long ms) {
        return sockJsChannelConfig.setSessionTimeout(ms);
    }

    @Override
    public long heartbeatInterval() {
        return sockJsChannelConfig.heartbeatInterval();
    }

    @Override
    public SockJsConfig setHeartbeatInterval(final long ms) {
        return sockJsChannelConfig.setHeartbeatInterval(ms);
    }

    @Override
    public int maxStreamingBytesSize() {
        return sockJsChannelConfig.maxStreamingBytesSize();
    }

    @Override
    public SockJsConfig setMaxStreamingBytesSize(final int max) {
        return sockJsChannelConfig.setMaxStreamingBytesSize(max);
    }

    @Override
    public boolean isTls() {
        return sockJsChannelConfig.isTls();
    }

    @Override
    public SockJsConfig setTls(final boolean tls) {
        return sockJsChannelConfig.setTls(tls);
    }

    @Override
    public String keyStore() {
        return sockJsChannelConfig.keyStore();
    }

    @Override
    public SockJsConfig setKeyStore(final String keyStore) {
        return sockJsChannelConfig.setKeyStore(keyStore);
    }

    @Override
    public String keyStorePassword() {
        return sockJsChannelConfig.keyStorePassword();
    }

    @Override
    public SockJsConfig setKeyStorePassword(final String password) {
        return sockJsChannelConfig.setKeyStorePassword(password);
    }

    @Override
    public CorsConfig corsConfig() {
        return sockJsChannelConfig.corsConfig();
    }

    @Override
    public SockJsConfig setCorsConfig(final CorsConfig corsConfig) {
        return sockJsChannelConfig.setCorsConfig(corsConfig);
    }

    @Override
    public ChannelInitializer<SockJsChannel> channelInitializer() {
        return sockJsChannelConfig.channelInitializer();
    }

    @Override
    public SockJsConfig setChannelInitializer(final ChannelInitializer<SockJsChannel> init) {
        return sockJsChannelConfig.setChannelInitializer(init);
    }
}
