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

import static io.netty.handler.codec.http.HttpHeaders.Names.*;
import static io.netty.handler.codec.http.HttpResponseStatus.NO_CONTENT;

import io.netty.channel.ChannelFutureListener;
import io.netty.channel.ChannelHandlerContext;
import io.netty.handler.codec.http.FullHttpRequest;
import io.netty.handler.codec.http.FullHttpResponse;
import io.netty.handler.codec.http.HttpHeaders;
import io.netty.handler.codec.sockjs.SockJsConfig;
import io.netty.util.internal.StringUtil;

/**
 * XMLHttpRequest (XHR) streaming transport is a transport where a persistent
 * connection is maintained between the server and the client, over which the
 * server can send HTTP chunks.
 *
 * This handler is responsible the send part the xhr-polling transport, which is
 * sending data to the target SockJS service.
 *
 * @see XhrPollingTransport
 */
public class XhrSendTransport extends AbstractSendTransport {

    public XhrSendTransport(final SockJsConfig config) {
        super(config);
    }

    @Override
    public void respond(final ChannelHandlerContext ctx, final FullHttpRequest request) throws Exception {
        final FullHttpResponse response = HttpResponseBuilder.responseFor(request)
                .status(NO_CONTENT)
                .content("")
                .contentType(HttpResponseBuilder.CONTENT_TYPE_PLAIN)
                .setCookie(config)
                .header(CONNECTION, HttpHeaders.Values.CLOSE)
                .header(CACHE_CONTROL, HttpResponseBuilder.NO_CACHE_HEADER)
                .buildFullResponse(ctx.alloc());
        if (ctx.channel().isActive() && ctx.channel().isRegistered()) {
            ctx.writeAndFlush(response).addListener(ChannelFutureListener.CLOSE);
        }
    }

    @Override
    public String toString() {
        return StringUtil.simpleClassName(this) + "[config=" + config + ']';
    }
}
