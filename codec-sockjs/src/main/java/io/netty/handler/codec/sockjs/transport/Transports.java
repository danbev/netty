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

import io.netty.buffer.ByteBuf;
import io.netty.buffer.ByteBufAllocator;
import io.netty.buffer.ByteBufUtil;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelPromise;
import io.netty.handler.codec.http.FullHttpResponse;
import io.netty.handler.codec.http.HttpRequest;
import io.netty.handler.codec.http.HttpResponse;
import io.netty.handler.codec.http.HttpResponseStatus;
import io.netty.util.CharsetUtil;

import java.nio.CharBuffer;

import static io.netty.handler.codec.http.HttpHeaders.Names.*;
import static io.netty.handler.codec.http.HttpMethod.*;
import static io.netty.handler.codec.http.HttpResponseStatus.*;
import static io.netty.handler.codec.sockjs.transport.HttpResponseBuilder.*;

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

        public String path() {
            return '/' + name().toLowerCase();
        }
    }

    private Transports() {
    }

    /**
     * Escapes unicode characters in the passed in char array to a Java string with
     * Java style escaped charaters.
     *
     * @param value the char[] for which unicode characters should be escaped
     * @return {@code String} Java style escaped unicode characters.
     */
    public static String escapeCharacters(final char[] value) {
        final StringBuilder buffer = new StringBuilder();
        for (char ch : value) {
            if (ch >= '\u0000' && ch <= '\u001F' ||
                    ch >= '\uD800' && ch <= '\uDFFF' ||
                    ch >= '\u200C' && ch <= '\u200F' ||
                    ch >= '\u2028' && ch <= '\u202F' ||
                    ch >= '\u2060' && ch <= '\u206F' ||
                    ch >= '\uFFF0' && ch <= '\uFFFF') {
                final String ss = Integer.toHexString(ch);
                buffer.append('\\').append('u');
                for (int k = 0; k < 4 - ss.length(); k++) {
                    buffer.append('0');
                }
                buffer.append(ss.toLowerCase());
            } else {
                buffer.append(ch);
            }
        }
        return buffer.toString();
    }

    /**
     * Creates a {@code FullHttpResponse} with the {@code METHOD_NOT_ALLOWED} status.
     *
     * @param request the {@link HttpRequest} for which the buildResponse should be generated.
     * @return {@link FullHttpResponse} with the {@link HttpResponseStatus#METHOD_NOT_ALLOWED}.
     */
    public static HttpResponse methodNotAllowedResponse(final HttpRequest request) {
        return responseFor(request)
                .methodNotAllowed()
                .header(CONTENT_LENGTH, 0)
                .header(ALLOW, GET)
                .buildResponse();
    }

    /**
     * Creates a {@code FullHttpResponse} with the {@code BAD_REQUEST} status and a body.
     *
     * @param request the {@link HttpRequest} for which a buildResponse should be created.
     * @param content the content that will become the buildResponse body.
     * @param alloc the {@link io.netty.buffer.ByteBufAllocator} to use.
     * @return {@link FullHttpResponse} with the {@link HttpResponseStatus#BAD_REQUEST}.
     */
    public static FullHttpResponse badRequestResponse(final HttpRequest request,
                                                      final String content,
                                                      final ByteBufAllocator alloc) {
        return responseWithContent(request, BAD_REQUEST, CONTENT_TYPE_PLAIN, content, alloc);
    }

    /**
     * Creates a {@code FullHttpResponse} with the {@code INTERNAL_SERVER_ERROR} status and a body.
     *
     * @param request the {@link HttpRequest} for which a buildResponse should be created.
     * @param content the content that will become the buildResponse body.
     * @param alloc the {@link io.netty.buffer.ByteBufAllocator} to use.
     * @return {@link FullHttpResponse} with the {@link HttpResponseStatus#INTERNAL_SERVER_ERROR}.
     */
    public static FullHttpResponse internalServerErrorResponse(final HttpRequest request,
                                                               final String content,
                                                               final ByteBufAllocator alloc) {
        return responseWithContent(request, INTERNAL_SERVER_ERROR, CONTENT_TYPE_PLAIN, content, alloc);
    }

    /**
     * Creates FullHttpResponse with a buildResponse body.
     *
     * @param request the {@link HttpRequest} for which a buildResponse should be created.
     * @param status the status of the HTTP buildResponse
     * @param contentType the value for the 'Content-Type' HTTP buildResponse header.
     * @param content the content that will become the body of the HTTP buildResponse.
     * @param alloc the {@link io.netty.buffer.ByteBufAllocator} to use.
     */
    public static FullHttpResponse responseWithContent(final HttpRequest request,
                                                       final HttpResponseStatus status,
                                                       final String contentType,
                                                       final String content,
                                                       final ByteBufAllocator alloc) {
        final ByteBuf byteBuf = ByteBufUtil.encodeString(alloc, CharBuffer.wrap(content), CharsetUtil.UTF_8);
        return responseWithContent(request, status, contentType, byteBuf);
    }

    /**
     * Creates FullHttpResponse with a buildResponse body.
     *
     * @param request the {@link HttpRequest} for which a buildResponse should be created.
     * @param status the status of the HTTP buildResponse
     * @param contentType the value for the 'Content-Type' HTTP buildResponse header.
     * @param content the {@link ByteBuf} content that will become the body of the HTTP buildResponse.
     */
    public static FullHttpResponse responseWithContent(final HttpRequest request,
                                                       final HttpResponseStatus status,
                                                       final String contentType,
                                                       final ByteBuf content) {
        return responseFor(request).status(status).content(content).contentType(contentType).buildFullResponse();
    }

    /**
     * Writes the passed in respone to the {@link ChannelHandlerContext} if it is active.
     *
     * @param ctx the {@link ChannelHandlerContext} to write the buildResponse to.
     * @param response the {@link HttpResponseStatus} to be written.
     */
    public static void writeResponse(final ChannelHandlerContext ctx, final HttpResponse response) {
        if (ctx.channel().isActive() && ctx.channel().isRegistered()) {
            ctx.writeAndFlush(response);
        }
    }

    /**
     * Writes the passed in respone to the {@link ChannelHandlerContext} if it is active.
     *
     * @param ctx the {@link ChannelHandlerContext} to write the buildResponse to.
     * @param promise the {@link ChannelPromise}
     * @param response the {@link HttpResponseStatus} to be written.
     */
    public static void writeResponse(final ChannelHandlerContext ctx, final ChannelPromise promise,
            final HttpResponse response) {
        if (ctx.channel().isActive() && ctx.channel().isRegistered()) {
            ctx.writeAndFlush(response, promise);
        }
    }
}
