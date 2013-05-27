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
package io.netty.handler.codec.sockjs.util;

import io.netty.handler.codec.sockjs.AbstractSockJsServiceFactory;
import io.netty.handler.codec.sockjs.EchoService;
import io.netty.handler.codec.sockjs.SockJsConfig;
import io.netty.handler.codec.sockjs.SockJsService;
import io.netty.handler.codec.sockjs.SockJsServiceFactory;

public final class SockJsTestServices {

    private SockJsTestServices() {
    }

    public static SockJsServiceFactory closeServiceFactory(final SockJsService closeService) {
        return new AbstractSockJsServiceFactory(closeService.config()) {
            @Override
            public SockJsService newService() {
                return closeService;
            }
        };
    }

    public static SockJsServiceFactory echoServiceFactory(final SockJsService service) throws Exception {
        return new AbstractSockJsServiceFactory(service.config()) {
            @Override
            public SockJsService newService() {
                return service;
            }
        };
    }

    public static final EchoService singletonEchoServiceFactory =
            new EchoService(SockJsConfig.withPrefix("/echo").cookiesNeeded().build());

    public static SockJsServiceFactory singletonEchoServiceFactory() {
        return new AbstractSockJsServiceFactory(singletonEchoServiceFactory.config()) {
            @Override
            public SockJsService newService() {
                return singletonEchoServiceFactory;
            }
        };
    }

}