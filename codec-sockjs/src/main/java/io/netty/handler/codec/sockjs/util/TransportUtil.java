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

import io.netty.channel.Channel;
import io.netty.channel.ChannelFuture;
import io.netty.channel.ChannelFutureListener;
import io.netty.channel.ChannelHandlerContext;
import io.netty.handler.codec.AsciiString;
import io.netty.handler.codec.http.HttpHeaderUtil;
import io.netty.handler.codec.http.HttpHeaders;
import io.netty.handler.codec.http.HttpRequest;
import io.netty.handler.codec.http.HttpResponse;
import io.netty.handler.codec.sockjs.transport.HttpResponseBuilder;
import static io.netty.handler.codec.http.HttpHeaders.Names.CONNECTION;
import static io.netty.handler.codec.http.HttpHeaders.Values.KEEP_ALIVE;

public final class TransportUtil {

    private TransportUtil() {
    }

    public static void writeNotFoundResponse(final HttpRequest request, final ChannelHandlerContext ctx) {
        writeResponse(ctx.channel(), request, HttpResponseBuilder.responseFor(request)
                .notFound()
                .content("Not found").contentType(HttpResponseBuilder.CONTENT_TYPE_PLAIN)
                .buildFullResponse(ctx.alloc()));
    }

    public static void writeMethodNotAllowedResponse(final HttpRequest request, final ChannelHandlerContext ctx) {
        writeResponse(ctx.channel(), request, HttpResponseBuilder.responseFor(request)
                .methodNotAllowed()
                .content("Method not allowed").contentType(HttpResponseBuilder.CONTENT_TYPE_PLAIN)
                .buildFullResponse(ctx.alloc()));
    }

    public static void writeResponse(final Channel channel,
                                      final HttpRequest request,
                                      final HttpResponse response) {
        boolean hasKeepAliveHeader = AsciiString.equalsIgnoreCase(KEEP_ALIVE, request.headers().get(CONNECTION));
        if (!request.protocolVersion().isKeepAliveDefault() && hasKeepAliveHeader) {
            response.headers().set(CONNECTION, KEEP_ALIVE);
        }
        final ChannelFuture wf = channel.writeAndFlush(response);
        if (!HttpHeaderUtil.isKeepAlive(request)) {
            wf.addListener(ChannelFutureListener.CLOSE);
        }
    }

}
