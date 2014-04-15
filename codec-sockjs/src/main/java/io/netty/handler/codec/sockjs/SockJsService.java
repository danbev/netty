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

import io.netty.channel.ChannelHandler;
import io.netty.handler.codec.http.cors.CorsHandler;
import io.netty.handler.codec.sockjs.handler.SockJsHandler;
import io.netty.util.internal.StringUtil;

public class SockJsService {

    private final SockJsChannelConfig config;
    private final ChannelHandler childInitializer;

    public SockJsService(final SockJsChannelConfig config, final ChannelHandler childInitializer) {
        this.config = config;
        this.childInitializer = childInitializer;
    }

    public SockJsChannelConfig config() {
        return config;
    }

    public ChannelHandler childChannelInitializer() {
        return childInitializer;
    }

    public CorsHandler corsHandler() {
        return new CorsHandler(config.corsConfig());
    }

    public SockJsHandler sockJsHandler() {
        return new SockJsHandler(config);
    }

    @Override
    public String toString() {
        return StringUtil.simpleClassName(SockJsService.class) +
                "[config=" + config +
                ", corsHandler=" + corsHandler() +
                ", sockJsHandler=" + sockJsHandler() +
                ", childInitializer=" + childInitializer + "]";
    }

}
