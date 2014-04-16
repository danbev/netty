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
package io.netty.handler.codec.sockjs.channel;

import io.netty.channel.ServerChannel;
import io.netty.handler.codec.sockjs.SockJsService;

/**
 * A {@link ServerChannel} for a SockJS server.
 */
public interface SockJsServerChannel extends ServerChannel {

    /**
     * Returns the {@link SockJsService} for the passed-in SockJS service
     * prefix.
     * The 'prefix' is the part of the URL used to access the service.
     *
     * @param prefix the SockJS prefix
     * @return {@link SockJsService} the service matching the passed-in prefix or null if no match was found.
     */
    SockJsService serviceFor(String prefix);

    @Override
    SockJsServerChannelConfig config();

}
