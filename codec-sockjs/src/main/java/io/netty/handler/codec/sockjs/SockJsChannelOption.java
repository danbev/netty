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

import io.netty.channel.ChannelOption;
import io.netty.handler.codec.http.cors.CorsConfig;

import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static io.netty.channel.ChannelOption.valueOf;

public final class SockJsChannelOption {

    private static final Class<SockJsChannelOption> T = SockJsChannelOption.class;
    public static final ChannelOption<String> PREFIX = valueOf(T, "PREFIX");
    public static final ChannelOption<Boolean> WEBSOCKET_ENABLED = valueOf(T, "WEBSOCKET_ENABLED");
    public static final ChannelOption<Long> WEBSOCKET_HEARTBEAT_INTERVAL = valueOf(T, "WEBSOCKET_HEARTBEAT_INTERVAL");
    public static final ChannelOption<Set<String>> WEBSOCKET_PROTOCOLS = valueOf(T, "WEBSOCKET_PROTOCOLS");
    public static final ChannelOption<Boolean> COOKIES_NEEDED = valueOf(T, "COOKIES_NEEDED");
    public static final ChannelOption<String> SOCKJS_URL = valueOf(T, "SOCKJS_URL");
    public static final ChannelOption<Long> SESSION_TIMEOUT = valueOf(T, "SESSION_TIMEOUT");
    public static final ChannelOption<Long> HEARTBEAT_INTERVAL = valueOf(T, "HEARTBEAT_INTERVAL");
    public static final ChannelOption<Integer> MAX_STREAMING_BYTES_SIZE = valueOf(T, "MAX_STREAMING_BYTES_SIZE");
    public static final ChannelOption<Boolean> TLS = valueOf(T, "TLS");
    public static final ChannelOption<String> KEYSTORE = valueOf(T, "KEYSTORE");
    public static final ChannelOption<String> KEYSTORE_PASSWORD = valueOf(T, "KEYSTORE_PASSWORD");
    public static final ChannelOption<CorsConfig> CORS_CONFIG = valueOf(T, "CORS_CONFIG");

    private SockJsChannelOption() {
    }

    /**
     * Enables the creation of ChannelOption for a specific SockJs service.
     * A SockJs service is identified by its 'getPrefix'.
     *
     * @param prefix the getPrefix for the SockJs service
     * @param option the option to be set for the SockJs service.
     * @return ChannelOption for the passed original option plus a SockJs service prefix.
     */
    public static ChannelOption forPrefix(final String prefix, ChannelOption<?> option) {
        return valueOf(option.name() + '#' + prefix);
    }

    private static final Pattern PREFIX_PATTERN = Pattern.compile('(' + SockJsChannelOption.class.getName() +
            "#.+)#(.+)$");

    public static PrefixOptionMetadata extractPrefix(final ChannelOption<?> option) {
        final Matcher m = PREFIX_PATTERN.matcher(option.name());
        if (m.find()) {
            final String optionName = m.group(1);
            final ChannelOption<Object> baseOption = valueOf(optionName);
            return new PrefixOptionMetadata(m.group(2), baseOption);
        }
        return null;
    }

    public static class PrefixOptionMetadata {

        private final String prefix;
        private final ChannelOption<?> option;

        public PrefixOptionMetadata(final String prefix, final ChannelOption<?> option) {
            this.prefix = prefix;
            this.option = option;
        }

        public String prefix() {
            return prefix;
        }

        public ChannelOption<?> option() {
            return option;
        }
    }
}