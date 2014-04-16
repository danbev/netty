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

import static io.netty.handler.codec.http.HttpHeaders.Names.ALLOW;
import static io.netty.handler.codec.http.HttpHeaders.Names.CONNECTION;
import static io.netty.handler.codec.http.HttpHeaders.Names.CONTENT_LENGTH;
import static io.netty.handler.codec.http.HttpHeaders.Names.CONTENT_TYPE;
import static io.netty.handler.codec.http.HttpHeaders.Names.UPGRADE;
import static io.netty.handler.codec.http.HttpMethod.GET;
import static io.netty.handler.codec.http.HttpMethod.POST;
import static io.netty.handler.codec.http.HttpResponseStatus.BAD_REQUEST;
import static io.netty.handler.codec.http.HttpResponseStatus.METHOD_NOT_ALLOWED;
import static io.netty.handler.codec.http.HttpVersion.HTTP_1_1;
import static io.netty.handler.codec.sockjs.transport.HttpResponseBuilder.CONTENT_TYPE_PLAIN;
import static io.netty.handler.codec.sockjs.util.ChannelUtil.*;
import static io.netty.handler.codec.sockjs.util.HttpUtil.decode;
import static io.netty.handler.codec.sockjs.util.HttpUtil.decodeFullHttpResponse;
import static io.netty.handler.codec.sockjs.util.HttpUtil.webSocketUpgradeRequest;
import static org.hamcrest.CoreMatchers.nullValue;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.is;

import io.netty.channel.ChannelHandler;
import io.netty.channel.embedded.EmbeddedChannel;
import io.netty.handler.codec.http.DefaultFullHttpRequest;
import io.netty.handler.codec.http.FullHttpRequest;
import io.netty.handler.codec.http.FullHttpResponse;
import io.netty.handler.codec.http.HttpResponse;
import io.netty.handler.codec.http.HttpResponseStatus;
import io.netty.handler.codec.http.websocketx.TextWebSocketFrame;
import io.netty.handler.codec.http.websocketx.WebSocketVersion;
import io.netty.handler.codec.sockjs.channel.SockJsChannelOption;
import io.netty.handler.codec.sockjs.SockJsEchoHandler;
import io.netty.util.CharsetUtil;

import org.junit.Test;

public class WebSocketTransportTest {

    @Test
    public void upgradeRequest() throws Exception {
        final ChannelHandler echoHandler = new SockJsEchoHandler();
        final EmbeddedChannel ch = wsSockJsChannel("/echo", echoHandler);
        assertUpgradeRequest(ch);
    }

    @Test
    public void invalidHttpMethod() throws Exception {
        final ChannelHandler echoHandler = new SockJsEchoHandler();
        final EmbeddedChannel ch = wsSockJsChannel("/echo", echoHandler);
        final FullHttpRequest request = new DefaultFullHttpRequest(HTTP_1_1, POST, "/echo/websocket");
        request.retain();
        ch.writeInbound(request);
        final HttpResponse response = decode(ch);
        assertThat(response.getStatus(), is(METHOD_NOT_ALLOWED));
        assertThat(response.headers().get(ALLOW), is(GET.toString()));
    }

    @Test
    public void nonUpgradeRequest() throws Exception {
        final ChannelHandler echoHandler = new SockJsEchoHandler();
        final EmbeddedChannel ch = wsSockJsChannel("/echo", echoHandler);
        final FullHttpRequest request = new DefaultFullHttpRequest(HTTP_1_1, GET, "/echo/123/123/websocket");
        ch.writeInbound(request);

        final FullHttpResponse response = decodeFullHttpResponse(ch);
        assertThat(response.getStatus(), is(BAD_REQUEST));
        assertThat(response.headers().get(CONTENT_TYPE), is(CONTENT_TYPE_PLAIN));
        assertThat(response.content().toString(CharsetUtil.UTF_8), equalTo("Can \"Upgrade\" only to \"WebSocket\"."));
        response.release();
    }

    @Test
    public void invalidConnectionHeader() throws Exception {
        final ChannelHandler echoHandler = new SockJsEchoHandler();
        final EmbeddedChannel ch = wsSockJsChannel("/echo", echoHandler);
        final FullHttpRequest request = webSocketUpgradeRequest("/echo/123/123/websocket", WebSocketVersion.V13);
        request.headers().set(UPGRADE, "WebSocket");
        request.headers().set(CONNECTION, "close");
        ch.writeInbound(request);

        final FullHttpResponse response = decodeFullHttpResponse(ch);
        assertThat(response.getStatus(), is(BAD_REQUEST));
        assertThat(response.content().toString(CharsetUtil.UTF_8), equalTo("\"Connection\" must be \"Upgrade\"."));
        response.release();
    }

    @Test
    public void invalidJsonInWebSocketFrame() throws Exception {
        final ChannelHandler echoHandler = new SockJsEchoHandler();
        final EmbeddedChannel ch = wsSockJsChannel("/echo", echoHandler);
        assertUpgradeRequest(ch);

        ch.writeInbound(new TextWebSocketFrame("[invalidJson"));
        assertThat(ch.isOpen(), is(false));
    }

    @Test
    public void writeJsonArray() throws Exception {
        final ChannelHandler echoHandler = new SockJsEchoHandler();
        final EmbeddedChannel ch = wsSockJsChannel("/echo", echoHandler);
        assertUpgradeRequest(ch);

        ch.writeInbound(new TextWebSocketFrame("[\"x\",\"y\"]"));
        // Discard of the HttpResponse
        final TextWebSocketFrame open = ch.readOutbound();
        assertThat(open.text(), equalTo("o"));
        open.release();

        final TextWebSocketFrame x = ch.readOutbound();
        assertThat(x.text(), equalTo("a[\"x\"]"));
        x.release();

        final TextWebSocketFrame y = ch.readOutbound();
        assertThat(y.text(), equalTo("a[\"y\"]"));
        y.release();
    }

    @Test
    public void writeJsonString() throws Exception {
        final ChannelHandler echoHandler = new SockJsEchoHandler();
        final EmbeddedChannel ch = wsSockJsChannel("/echo", echoHandler);
        assertUpgradeRequest(ch);

        final TextWebSocketFrame open = ch.readOutbound();
        assertThat(open.text(), equalTo("o"));
        open.release();

        ch.writeInbound(new TextWebSocketFrame("\"x\""));
        final TextWebSocketFrame x = ch.readOutbound();
        assertThat(x.text(), equalTo("a[\"x\"]"));
        x.release();
        ch.finish();
    }

    @Test
    public void firefox602ConnectionHeader() throws Exception {
        final ChannelHandler echoHandler = new SockJsEchoHandler();
        final EmbeddedChannel ch = wsSockJsChannel("/echo", echoHandler);
        final FullHttpRequest request = webSocketUpgradeRequest("/echo/123/123/websocket", WebSocketVersion.V08);
        request.headers().set(CONNECTION, "keep-alive, Upgrade");
        ch.writeInbound(request);

        final HttpResponse response = decode(ch);
        assertThat(response.getStatus(), is(HttpResponseStatus.SWITCHING_PROTOCOLS));
        assertThat(response.headers().get(CONNECTION), equalTo("Upgrade"));
        ch.finish();
    }

    @Test
    public void headersSanity() throws Exception {
        verifyHeaders(WebSocketVersion.V07);
        verifyHeaders(WebSocketVersion.V08);
        verifyHeaders(WebSocketVersion.V13);
    }

    private static void verifyHeaders(final WebSocketVersion version) throws Exception {
        final ChannelHandler echoHandler = new SockJsEchoHandler();
        final EmbeddedChannel ch = wsSockJsChannel("/echo", echoHandler);
        final FullHttpRequest request = webSocketUpgradeRequest("/echo/123/123/websocket", version);
        ch.writeInbound(request);
        final HttpResponse response = decode(ch);
        assertThat(response.getStatus(), is(HttpResponseStatus.SWITCHING_PROTOCOLS));
        assertThat(response.headers().get(CONNECTION), equalTo("Upgrade"));
        assertThat(response.headers().get(UPGRADE), equalTo("websocket"));
        assertThat(response.headers().get(CONTENT_LENGTH), is(nullValue()));
    }

    private static void assertUpgradeRequest(final EmbeddedChannel ch) throws Exception {
        final FullHttpRequest request = webSocketUpgradeRequest(ch.config().getOption(SockJsChannelOption.PREFIX) +
                "/123/123/websocket", WebSocketVersion.V13);
        ch.writeInbound(request);
        final HttpResponse response = decode(ch);
        assertThat(response.getStatus(), is(HttpResponseStatus.SWITCHING_PROTOCOLS));
        assertThat(response.headers().get(UPGRADE), equalTo("websocket"));
    }

}
