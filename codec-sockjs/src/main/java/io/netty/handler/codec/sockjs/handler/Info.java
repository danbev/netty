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
package io.netty.handler.codec.sockjs.handler;

import io.netty.handler.codec.http.FullHttpResponse;
import io.netty.handler.codec.http.HttpRequest;
import io.netty.handler.codec.sockjs.SockJsConfig;

import java.util.Random;

import static io.netty.handler.codec.http.HttpHeaderNames.CACHE_CONTROL;
import static io.netty.handler.codec.sockjs.transport.HttpResponseBuilder.*;

final class Info {
    private static final Random RANDOM = new Random();

    private Info() {
    }

    public static boolean matches(final String path) {
        return path != null && path.startsWith("/info");
    }

    public static FullHttpResponse response(final SockJsConfig config, final HttpRequest request) throws Exception {
        return responseFor(request).ok()
                .content(infoContent(config))
                .contentType(CONTENT_TYPE_JSON)
                .header(CACHE_CONTROL, NO_CACHE_HEADER)
                .buildFullResponse();
    }

    private static String infoContent(final SockJsConfig config) {
        return new StringBuilder(90).append("{\"websocket\": ")
                .append(config.isWebSocketEnabled())
                .append(", \"origins\": [\"*:*\"]")
                .append(", \"cookie_needed\": ")
                .append(config.areCookiesNeeded())
                .append(", \"entropy\": ")
                .append(RANDOM.nextInt(Integer.MAX_VALUE) + 1)
                .append('}')
                .toString();
    }
}
