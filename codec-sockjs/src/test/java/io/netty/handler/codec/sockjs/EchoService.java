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


import io.netty.util.internal.logging.InternalLogger;
import io.netty.util.internal.logging.InternalLoggerFactory;

import static io.netty.handler.codec.sockjs.util.Arguments.checkNotNull;

/**
 * Test service required by
 * <a href="http://sockjs.github.io/sockjs-protocol/sockjs-protocol-0.3.3.html">sockjs-protocol</a>
 * which will send back message it receives.
 */
public class EchoService implements SockJsService {

    private static final InternalLogger LOGGER = InternalLoggerFactory.getInstance(CloseService.class);
    private final SockJsConfig config;
    private SockJsSessionContext session;

    public EchoService(final SockJsConfig config) {
        checkNotNull(config, "config");
        this.config = config;
    }

    @Override
    public SockJsConfig config() {
        return config;
    }

    @Override
    public void onMessage(final String message) throws Exception {
        session.send(message);
    }

    @Override
    public void onOpen(final SockJsSessionContext session) {
        LOGGER.debug("onOpen");
        this.session = session;
    }

    @Override
    public void onClose() {
        LOGGER.debug("onClose sessionId: " + session.sessionId());
    }

}