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
import io.netty.channel.ChannelFutureListener;
import io.netty.channel.ChannelHandlerAdapter;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelPromise;
import io.netty.handler.codec.http.DefaultHttpContent;
import io.netty.handler.codec.http.HttpHeaders;
import io.netty.handler.codec.http.HttpRequest;
import io.netty.handler.codec.http.HttpResponse;
import io.netty.handler.codec.http.LastHttpContent;
import io.netty.handler.codec.http.QueryStringDecoder;
import io.netty.handler.codec.sockjs.SockJsConfig;
import io.netty.handler.codec.sockjs.handler.SessionHandler.Event;
import io.netty.handler.codec.sockjs.protocol.Frame;
import io.netty.handler.codec.sockjs.util.JsonUtil;
import io.netty.util.ReferenceCountUtil;
import io.netty.util.internal.logging.InternalLogger;
import io.netty.util.internal.logging.InternalLoggerFactory;

import java.util.List;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;

import static io.netty.buffer.Unpooled.*;
import static io.netty.handler.codec.http.HttpConstants.*;
import static io.netty.handler.codec.http.HttpHeaders.Names.*;
import static io.netty.handler.codec.http.HttpHeaders.Values.*;
import static io.netty.handler.codec.sockjs.transport.HttpResponseBuilder.*;
import static io.netty.util.CharsetUtil.*;

/**
 * A streaming transport for SockJS.
 *
 * This transport is intended to be used in an iframe, where the src of
 * the iframe will have the an url looking something like this:
 *
 * http://server/echo/serverId/sessionId/htmlfile?c=callback
 * The server will respond with a html snipped containing a html header
 * and a script element. When data is available on the server this classes
 * write method will write a script to the connection that will invoke the
 * callback.
 */
public class HtmlFileTransport extends ChannelHandlerAdapter {

    private static final InternalLogger logger = InternalLoggerFactory.getInstance(HtmlFileTransport.class);
    private static final ByteBuf HEADER_PART1 = unreleasableBuffer(copiedBuffer(
            "<!doctype html>\n" +
            "<html><head>\n" +
            "  <meta http-equiv=\"X-UA-Compatible\" content=\"IE=edge\" />\n" +
            "  <meta http-equiv=\"Content-Type\" content=\"text/html; charset=UTF-8\" />\n" +
            "</head><body><h2>Don't panic!</h2>\n" +
            "  <script>\n" +
            "    document.domain = document.domain;\n" +
            "    var c = parent.", UTF_8));
    private static final ByteBuf HEADER_PART2 = unreleasableBuffer(copiedBuffer(
            ";\n" +
            "    c.start();\n" +
            "    function p(d) {c.message(d);};\n" +
            "    window.onload = function() {c.stop();};\n" +
            "  </script>", UTF_8));
    private static final ByteBuf PREFIX = unreleasableBuffer(copiedBuffer("<script>\np(\"", UTF_8));
    private static final ByteBuf POSTFIX = unreleasableBuffer(copiedBuffer("\");\n</script>\r\n", UTF_8));
    private static final ByteBuf END_HEADER = unreleasableBuffer(copiedBuffer(new byte[] {CR, LF, CR, LF}));

    private final SockJsConfig config;
    private final HttpRequest request;
    private final AtomicBoolean headerSent = new AtomicBoolean(false);
    private final AtomicInteger bytesSent = new AtomicInteger(0);
    private String callback;

    public HtmlFileTransport(final SockJsConfig config, final HttpRequest request) {
        this.config = config;
        this.request = request;
    }

    @Override
    public void channelRead(final ChannelHandlerContext ctx, final Object msg) throws Exception {
        if (msg instanceof HttpRequest) {
            final String c = getCallbackFromRequest((HttpRequest) msg);
            if (c.isEmpty()) {
                ReferenceCountUtil.release(msg);
                respondCallbackRequired(ctx);
                ctx.fireUserEventTriggered(Event.CLOSE_CONTEXT);
                return;
            } else {
                callback = c;
            }
        }
        ctx.fireChannelRead(msg);
    }

    private static String getCallbackFromRequest(final HttpRequest request) {
        final QueryStringDecoder qsd = new QueryStringDecoder(request.getUri());
        final List<String> c = qsd.parameters().get("c");
        return c == null || c.isEmpty() ? "" : c.get(0);
    }

    @Override
    public void write(final ChannelHandlerContext ctx, final Object msg, final ChannelPromise promise)
            throws Exception {
        if (msg instanceof Frame) {
            final Frame frame = (Frame) msg;
            if (headerSent.compareAndSet(false, true)) {
                final HttpResponse response = createResponse(CONTENT_TYPE_HTML);
                final ByteBuf header = ctx.alloc().buffer();
                header.writeBytes(HEADER_PART1.duplicate());
                final ByteBuf content = copiedBuffer(callback, UTF_8);
                header.writeBytes(content);
                content.release();
                header.writeBytes(HEADER_PART2.duplicate());
                final int spaces = 1024 * header.readableBytes();
                final ByteBuf paddedBuffer = ctx.alloc().buffer(1024 + 50);
                paddedBuffer.writeBytes(header);
                header.release();
                for (int s = 0; s < spaces + 20; s++) {
                    paddedBuffer.writeByte(' ');
                }
                paddedBuffer.writeBytes(END_HEADER.duplicate());
                ctx.write(response, promise);
                ctx.writeAndFlush(new DefaultHttpContent(paddedBuffer));
            }

            final ByteBuf data = ctx.alloc().buffer();
            data.writeBytes(PREFIX.duplicate());
            data.writeBytes(JsonUtil.escapeJson(frame.content(), data));
            frame.content().release();
            data.writeBytes(POSTFIX.duplicate());
            final int dataSize = data.readableBytes();
            ctx.writeAndFlush(new DefaultHttpContent(data));

            if (maxBytesLimit(dataSize)) {
                if (logger.isDebugEnabled()) {
                    logger.debug("max bytesSize limit reached [{}]", config.maxStreamingBytesSize());
                }
                ctx.writeAndFlush(LastHttpContent.EMPTY_LAST_CONTENT).addListener(ChannelFutureListener.CLOSE);
            }
        } else {
            ctx.write(ReferenceCountUtil.retain(msg), promise);
        }
    }

    private void respondCallbackRequired(final ChannelHandlerContext ctx) {
        ctx.writeAndFlush(responseFor(request)
                .internalServerError()
                .content("\"callback\" parameter required")
                .contentType(CONTENT_TYPE_PLAIN)
                .header(HttpHeaders.Names.CACHE_CONTROL, NO_CACHE_HEADER)
                .buildFullResponse(ctx.alloc()));
    }

    private boolean maxBytesLimit(final int bytesWritten) {
        bytesSent.addAndGet(bytesWritten);
        return bytesSent.get() >= config.maxStreamingBytesSize();
    }

    protected HttpResponse createResponse(final String contentType) {
        return responseFor(request)
                .ok()
                .chunked()
                .contentType(contentType)
                .setCookie(config)
                .header(ACCESS_CONTROL_ALLOW_ORIGIN, "*")
                .header(ACCESS_CONTROL_ALLOW_CREDENTIALS, "true")
                .header(CONNECTION, CLOSE)
                .header(CACHE_CONTROL, NO_CACHE_HEADER)
                .buildResponse();
    }

}
