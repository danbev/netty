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
package io.netty.handler.codec.sockjs.transport;

/**
 * Transports contains constants, enums, and utility methods that are
 * common across transport implementations.
 */
public final class Transports {

    public enum Type {
        WEBSOCKET,
        XHR,
        XHR_SEND,
        XHR_STREAMING,
        JSONP,
        JSONP_SEND,
        EVENTSOURCE,
        HTMLFILE;

        private final String path = '/' + name().toLowerCase();

        public String path() {
            return path;
        }
    }

    private Transports() {
    }

}
