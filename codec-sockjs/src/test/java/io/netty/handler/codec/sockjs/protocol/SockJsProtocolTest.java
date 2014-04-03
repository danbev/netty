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
package io.netty.handler.codec.sockjs.protocol;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.netty.buffer.ByteBuf;
import io.netty.buffer.ByteBufHolder;
import io.netty.buffer.Unpooled;
import io.netty.channel.ChannelHandler;
import io.netty.channel.embedded.EmbeddedChannel;
import io.netty.handler.codec.http.ClientCookieEncoder;
import io.netty.handler.codec.http.DefaultFullHttpRequest;
import io.netty.handler.codec.http.DefaultHttpContent;
import io.netty.handler.codec.http.FullHttpRequest;
import io.netty.handler.codec.http.FullHttpResponse;
import io.netty.handler.codec.http.HttpContent;
import io.netty.handler.codec.http.HttpHeaders;
import io.netty.handler.codec.http.HttpHeaders.Names;
import io.netty.handler.codec.http.HttpMethod;
import io.netty.handler.codec.http.HttpRequest;
import io.netty.handler.codec.http.HttpResponse;
import io.netty.handler.codec.http.HttpResponseStatus;
import io.netty.handler.codec.http.HttpVersion;
import io.netty.handler.codec.http.LastHttpContent;
import io.netty.handler.codec.http.cors.CorsConfig;
import io.netty.handler.codec.http.websocketx.CloseWebSocketFrame;
import io.netty.handler.codec.http.websocketx.TextWebSocketFrame;
import io.netty.handler.codec.http.websocketx.WebSocketVersion;
import io.netty.handler.codec.sockjs.SockJsChannelConfig;
import io.netty.handler.codec.sockjs.SockJsChannelInitializer;
import io.netty.handler.codec.sockjs.SockJsChannelOption;
import io.netty.handler.codec.sockjs.SockJsCloseHandler;
import io.netty.handler.codec.sockjs.SockJsEchoHandler;
import io.netty.handler.codec.sockjs.transport.EventSourceTransport;
import io.netty.handler.codec.sockjs.transport.HttpResponseBuilder;
import io.netty.handler.codec.sockjs.transport.TransportType;
import io.netty.handler.codec.sockjs.util.HttpUtil;
import io.netty.handler.codec.sockjs.util.JsonUtil;
import io.netty.handler.codec.sockjs.util.TestEmbeddedChannel;
import io.netty.util.ReferenceCountUtil;
import org.junit.Test;

import java.util.UUID;
import java.util.regex.Pattern;

import static io.netty.handler.codec.http.HttpHeaders.Names.*;
import static io.netty.handler.codec.http.HttpHeaders.Values.WEBSOCKET;
import static io.netty.handler.codec.http.HttpHeaders.Values.KEEP_ALIVE;
import static io.netty.handler.codec.http.HttpMethod.*;
import static io.netty.handler.codec.http.HttpVersion.*;
import static io.netty.handler.codec.http.websocketx.WebSocketVersion.*;
import static io.netty.handler.codec.sockjs.SockJsTestUtil.*;
import static io.netty.handler.codec.sockjs.util.ChannelUtil.*;
import static io.netty.util.CharsetUtil.*;
import static org.hamcrest.CoreMatchers.containsString;
import static org.hamcrest.CoreMatchers.equalTo;
import static org.hamcrest.CoreMatchers.notNullValue;
import static org.hamcrest.CoreMatchers.nullValue;
import static org.hamcrest.Matchers.*;
import static org.hamcrest.Matchers.is;
import static org.junit.Assert.*;

public class SockJsProtocolTest {

    private static final Pattern SEMICOLON = Pattern.compile(";");

    /*
     * Equivalent to BaseUrlGreeting.test_greeting in sockjs-protocol-0.3.3.py.
     */
    @Test
    public void baseUrlGreetingTestGreeting() throws Exception {
        final EmbeddedChannel ch = sockJsPipeline("/echo", new SockJsEchoHandler());
        ch.writeInbound(httpRequest(ch.config().getOption(SockJsChannelOption.PREFIX)));

        final FullHttpResponse response = ch.readOutbound();
        assertThat(response.getStatus().code(), is(HttpResponseStatus.OK.code()));
        assertThat(response.headers().get(CONTENT_TYPE), equalTo("text/plain; charset=UTF-8"));
        assertThat(response.content().toString(UTF_8), equalTo("Welcome to SockJS!\n"));
        verifyNoSET_COOKIE(response);
        response.release();
    }

    /*
     * Equivalent to BaseUrlGreeting.test_notFound in sockjs-protocol-0.3.3.py.
     */
    @Test
    public void baseUrlGreetingTestNotFound() throws Exception {
        assertNotFoundResponse("/echo", "/a");
        assertNotFoundResponse("/echo", "/a.html");
        assertNotFoundResponse("/echo", "//");
        assertNotFoundResponse("/echo", "///");
        assertNotFoundResponse("/echo", "//a");
        assertNotFoundResponse("/echo", "/a/a/");
        assertNotFoundResponse("/echo", "/a/");
    }

    /*
     * Equivalent to IframePage.test_simpleUrl in sockjs-protocol-0.3.3.py.
     */
    @Test
    public void iframePageSimpleUrl() throws Exception {
        verifyIframe("/iframe.html");
    }

    /*
     * Equivalent to IframePage.test_versionedUrl in sockjs-protocol-0.3.3.py.
     */
    @Test
    public void iframePageTestVersionedUrl() {
        verifyIframe("/iframe-a.html");
        verifyIframe("/iframe-.html");
        verifyIframe("/iframe-0.1.2.html");
        verifyIframe("/iframe-0.1.2.abc-dirty.2144.html");
    }

    /*
     * Equivalent to IframePage.test_queriedUrl in sockjs-protocol-0.3.3.py.
     */
    @Test
    public void iframePageTestQueriedUrl() {
        verifyIframe("/iframe-a.html?t=1234");
        verifyIframe("/iframe-0.1.2.html?t=123414");
        verifyIframe("/iframe-0.1.2abc-dirty.2144.html?t=qweqweq123");
    }

    /*
     * Equivalent to IframePage.test_invalidUrl in sockjs-protocol-0.3.3.py.
     */
    @Test
    public void iframePageTestInvalidUrl() {
        assertNotFoundResponse("/echo", "/iframe.htm");
        assertNotFoundResponse("/echo", "/iframe");
        assertNotFoundResponse("/echo", "/IFRAME.HTML");
        assertNotFoundResponse("/echo", "/IFRAME");
        assertNotFoundResponse("/echo", "/iframe.HTML");
        assertNotFoundResponse("/echo", "/iframe.xml");
        assertNotFoundResponse("/echo", "/iframe-/.html");
    }

    /*
     * Equivalent to IframePage.test_cacheability in sockjs-protocol-0.3.3.py.
     */
    @Test
    public void iframeCachability() throws Exception {
        EmbeddedChannel ch = sockJsPipeline("/echo", new SockJsEchoHandler());

        ch.writeInbound(httpRequest("/echo" + "/iframe.html"));
        final FullHttpResponse response1 = ch.readOutbound();
        final String etag1 = getEtag(response1);
        response1.release();

        ch = sockJsPipeline("/echo", new SockJsEchoHandler());
        ch.writeInbound(httpRequest("/echo/iframe.html"));

        final FullHttpResponse response2 = ch.readOutbound();
        final String etag2 = getEtag(response2);
        assertThat(etag1, equalTo(etag2));
        response2.release();

        final HttpRequest requestWithEtag = httpRequest("/echo/iframe.html");
        requestWithEtag.headers().set(IF_NONE_MATCH, etag1);
        ch = sockJsPipeline("/echo", new SockJsEchoHandler());
        ch.writeInbound(requestWithEtag);

        final HttpResponse response = ch.readOutbound();
        assertThat(response.getStatus(), equalTo(HttpResponseStatus.NOT_MODIFIED));
        assertThat(response.headers().get(CONTENT_TYPE), is(nullValue()));
    }

    /*
     * Equivalent to InfoTest.test_basic in sockjs-protocol-0.3.3.py.
     */
    @Test
    public void infoTestBasic() throws Exception {
        final FullHttpResponse response = sendInfoRequest();
        assertThat(response.getStatus(), is(HttpResponseStatus.OK));
        verifyContentType(response, "application/json; charset=UTF-8");
        verifyNoSET_COOKIE(response);
        verifyNotCached(response);
        assertCORSHeaders(response, "*");

        final JsonNode json = contentAsJson(response);
        assertThat(json.get("websocket").asBoolean(), is(true));
        assertThat(json.get("cookie_needed").asBoolean(), is(true));
        assertThat(json.get("origins").get(0).asText(), is("*:*"));
        assertThat(json.get("entropy").asLong(), is(notNullValue()));
        response.release();
    }

    /*
     * Equivalent to InfoTest.test_entropy in sockjs-protocol-0.3.3.py.
     */
    @Test
    public void infoTestEntropy() throws Exception {
        final FullHttpResponse response1 = sendInfoRequest();
        final FullHttpResponse response2 = sendInfoRequest();
        assertThat(getEntropy(response1) != getEntropy(response2), is(true));
        response1.release();
        response2.release();
    }

    /*
     * Equivalent to InfoTest.test_options in sockjs-protocol-0.3.3.py.
     */
    @Test
    public void infoTestOptions() throws Exception {
        final CorsConfig corsConfig = SockJsChannelInitializer.defaultCorsConfig()
                .preflightResponseHeader(Names.CONTENT_TYPE, "text/plain; charset=UTF-8")
                .build();
        final EmbeddedChannel ch = sockJsPipeline("/echo", new SockJsEchoHandler(), corsConfig);
        ch.writeInbound(httpRequest("/echo", OPTIONS));

        final HttpResponse response = ch.readOutbound();
        // sockjs-protocol expects a 204 "No Content" while CORS "should" return 200 "OK".
        // perhaps this should be configurable. Check the spec!
        // assertThat(buildResponse.getStatus(), is(HttpResponseStatus.NO_CONTENT));
        assertThat(response.getStatus(), is(HttpResponseStatus.OK));
        assertCORSPreflightResponseHeaders(response);
        assertCORSHeaders(response, "*");
    }

    /*
     * Equivalent to InfoTest.test_options_null_origin in sockjs-protocol-0.3.3.py.
     */
    @Test
    public void infoTestOptionsNullOrigin() throws Exception {
        final CorsConfig corsConfig = SockJsChannelInitializer.defaultCorsConfig()
                .preflightResponseHeader(Names.CONTENT_TYPE, "text/plain; charset=UTF-8")
                .build();
        final EmbeddedChannel ch = sockJsPipeline("/echo", new SockJsEchoHandler(), corsConfig);
        final FullHttpRequest request = httpRequest("/echo/info", OPTIONS);
        request.headers().set(ORIGIN, "null");
        ch.writeInbound(request);

        final HttpResponse response = ch.readOutbound();
        // sockjs-protocol expects a 204 "No Content" while CORS "should" return 200 "OK".
        // assertThat(buildResponse.getStatus(), is(HttpResponseStatus.NO_CONTENT));
        assertThat(response.getStatus(), is(HttpResponseStatus.OK));
        assertCORSPreflightResponseHeaders(response);
        assertCORSHeaders(response, "*");
    }

    /*
     * Equivalent to InfoTest.test_disabled_websocket in sockjs-protocol-0.3.3.py.
     */
    @Test
    public void infoTestDisabledWebsocket() throws Exception {
        final EmbeddedChannel ch = sockJsPipeline("/echo", new SockJsEchoHandler());
        ch.config().setOption(SockJsChannelOption.WEBSOCKET_ENABLED, false);
        ch.writeInbound(httpRequest("/echo/info"));

        final FullHttpResponse response = ch.readOutbound();
        assertThat(response.getStatus(), is(HttpResponseStatus.OK));
        assertThat(contentAsJson(response).get("websocket").asBoolean(), is(false));
        response.release();
    }

    /*
     * Equivalent to SessionURLs.test_anyValue in sockjs-protocol-0.3.3.py.
     */
    @Test
    public void sessionUrlsTestAnyValue() throws Exception {
        assertOKResponse("/a/a");
        assertOKResponse("/_/_");
        assertOKResponse("/1/1");
        assertOKResponse("/abcdefgh_i-j%20/abcdefg_i-j%20");
    }

    /*
     * Equivalent to SessionURLs.test_invalidPaths in sockjs-protocol-0.3.3.py.
     */
    @Test
    public void sessionUrlsTestInvalidPaths() throws Exception {
        assertNotFoundResponse("echo", "//");
        assertNotFoundResponse("echo", "/a./a");
        assertNotFoundResponse("echo", "/a/a.");
        assertNotFoundResponse("echo", "/./.");
        assertNotFoundResponse("echo", "/");
        assertNotFoundResponse("echo", "///");
    }

    /*
     * Equivalent to SessionURLs.test_ignoringServerId in sockjs-protocol-0.3.3.py.
     */
    @Test
    public void sessionUrlsTestIgnoringServerId() throws Exception {
        final SockJsEchoHandler echoHandler = new SockJsEchoHandler();
        final String sessionId = UUID.randomUUID().toString();
        final String sessionUrl = "/echo/000/" + sessionId;

        final FullHttpResponse openSessionResponse = xhrRequest(sessionUrl, sockJsPipeline("/echo", echoHandler));
        assertOpenFrameResponse(openSessionResponse);

        final FullHttpResponse sendResponse = xhrSendRequest(sessionUrl, "[\"a\"]",
                sockJsPipeline("/echo", echoHandler));
        assertNoContent(sendResponse);
        sendResponse.release();

        final FullHttpResponse pollResponse = xhrRequest("/echo/999/" + sessionId,
                sockJsPipeline("/echo", echoHandler));
        assertMessageFrameContent(pollResponse, "a");
        pollResponse.release();
    }

    /*
     * Equivalent to Protocol.test_simpleSession in sockjs-protocol-0.3.3.py.
     */
    @Test
    public void protocolTestSimpleSession() throws Exception {
        final SockJsEchoHandler echoHandler = new SockJsEchoHandler();
        final String sessionUrl = "/echo/111/" + UUID.randomUUID();

        final FullHttpResponse openSessionResponse = xhrRequest(sessionUrl, sockJsPipeline("/echo", echoHandler));
        assertOpenFrameResponse(openSessionResponse);

        final FullHttpResponse sendResponse = xhrSendRequest(sessionUrl, "[\"a\"]",
                sockJsPipeline("/echo", echoHandler));
        assertNoContent(sendResponse);
        sendResponse.release();

        final FullHttpResponse pollResponse = xhrRequest(sessionUrl, sockJsPipeline("/echo", echoHandler));
        assertMessageFrameContent(pollResponse, "a");
        pollResponse.release();

        final FullHttpResponse badSessionResponse = xhrSendRequest("/echo/111/badsession", "[\"a\"]",
                sockJsPipeline("/echo", echoHandler));
        assertThat(badSessionResponse.getStatus(), is(HttpResponseStatus.NOT_FOUND));
        badSessionResponse.release();
    }

    /*
     * Equivalent to Protocol.test_closeSession in sockjs-protocol-0.3.3.py.
     */
    @Test
    public void protocolTestCloseSession() throws Exception {
        final ChannelHandler closeHandler = new SockJsCloseHandler();
        final String sessionUrl = "/close/222/" + UUID.randomUUID();

        final FullHttpResponse openSessionResponse = xhrRequest(sessionUrl, sockJsPipeline("/close", closeHandler));
        assertOpenFrameResponse(openSessionResponse);
        assertGoAwayResponse(xhrRequest(sessionUrl, sockJsPipeline("/close", closeHandler)));
        assertGoAwayResponse(xhrRequest(sessionUrl, sockJsPipeline("/close", closeHandler)));
    }

    /*
     * Equivalent to WebSocketHttpErrors.test_httpMethod in sockjs-protocol-0.3.3.py.
     */
    @Test
    public void webSocketHttpErrorsTestHttpMethod() throws Exception {
        final ChannelHandler echoHandler = new SockJsEchoHandler();
        final String sessionUrl = "/echo/222/" + UUID.randomUUID();
        final EmbeddedChannel ch = sockJsPipeline("/echo", echoHandler);

        final HttpRequest request = httpRequest(sessionUrl + "/websocket");
        ch.writeInbound(request);

        final FullHttpResponse response = ch.readOutbound();
        assertThat(response.getStatus(), equalTo(HttpResponseStatus.BAD_REQUEST));
        assertThat(response.content().toString(UTF_8), equalTo("Can \"Upgrade\" only to \"WebSocket\"."));
        response.release();
    }

    /*
     * Equivalent to WebSocketHttpErrors.test_invalidConnectionHeader in sockjs-protocol-0.3.3.py.
     */
    @Test
    public void webSocketHttpErrorsTestInvalidConnectionHeader() throws Exception {
        final ChannelHandler echoHandler = new SockJsEchoHandler();
        final String sessionUrl = "/echo/222/" + UUID.randomUUID();
        final EmbeddedChannel ch = sockJsPipeline("/echo", echoHandler);

        final FullHttpRequest request = webSocketUpgradeRequest(sessionUrl + "/websocket");
        request.headers().set(UPGRADE, "websocket");
        request.headers().set(CONNECTION, "close");
        ch.writeInbound(request);

        final FullHttpResponse response = ch.readOutbound();
        assertThat(response.getStatus(), equalTo(HttpResponseStatus.BAD_REQUEST));
        assertThat(response.content().toString(UTF_8), equalTo("\"Connection\" must be \"Upgrade\"."));
        response.release();
    }

    /*
     * Equivalent to WebsocketHttpErrors.test_invalidMethod in sockjs-protocol-0.3.3.py.
     */
    @Test
    public void webSocketHttpErrorsTestInvalidMethod() throws Exception {
        final ChannelHandler echoHandler = new SockJsEchoHandler();
        final String sessionUrl = "/echo/222/" + UUID.randomUUID();
        final EmbeddedChannel ch = sockJsPipeline("/echo", echoHandler);

        final FullHttpRequest request = webSocketUpgradeRequest(sessionUrl + "/websocket");
        request.setMethod(POST);
        ch.writeInbound(request);

        final HttpResponse response = ch.readOutbound();
        assertThat(response.getStatus(), equalTo(HttpResponseStatus.METHOD_NOT_ALLOWED));
    }

    /*
     * Equivalent to WebsocketHixie76.test_transport in sockjs-protocol-0.3.3.py.
     */
    @Test
    public void webSocketHixie76TestTransport() throws Exception {
        final ChannelHandler echoHandler = new SockJsEchoHandler();
        final String sessionUrl = "/echo/222/" + UUID.randomUUID();
        final EmbeddedChannel ch = wsSockJsPipeline("/echo", echoHandler);

        final FullHttpRequest request = webSocketUpgradeRequest(sessionUrl + "/websocket", V00);
        ch.writeInbound(request);

        final FullHttpResponse upgradeResponse = HttpUtil.decodeFullHttpResponse(ch);
        assertThat(upgradeResponse.content().toString(UTF_8), equalTo("8jKS'y:G*Co,Wxa-"));
        upgradeResponse.release();

        final TextWebSocketFrame openFrame = ch.readOutbound();
        assertThat(openFrame.content().toString(UTF_8), equalTo("o"));
        openFrame.release();
        ReferenceCountUtil.release(ch.readOutbound());

        final TextWebSocketFrame textWebSocketFrame = new TextWebSocketFrame("\"a\"");
        ch.writeInbound(textWebSocketFrame);

        final TextWebSocketFrame textFrame = ch.readOutbound();
        assertThat(textFrame.content().toString(UTF_8), equalTo("a[\"a\"]"));
        textFrame.release();
    }

    /*
     * Equivalent to WebsocketHixie76.test_close in sockjs-protocol-0.3.3.py.
     */
    @Test
    public void webSocketHixie76TestClose() throws Exception {
        final ChannelHandler closeHandler = new SockJsCloseHandler();
        final String sessionUrl = "/close/222/" + UUID.randomUUID();
        final EmbeddedChannel ch = wsSockJsPipeline("/close", closeHandler);

        final FullHttpRequest request = webSocketUpgradeRequest(sessionUrl + "/websocket", V00);
        ch.writeInbound(request);

        final FullHttpResponse upgradeResponse = HttpUtil.decodeFullHttpResponse(ch);
        assertThat(upgradeResponse.content().toString(UTF_8), equalTo("8jKS'y:G*Co,Wxa-"));
        upgradeResponse.release();

        final TextWebSocketFrame openFrame = ch.readOutbound();
        assertThat(openFrame.content().toString(UTF_8), equalTo("o"));
        openFrame.release();

        final TextWebSocketFrame closeFrame = ch.readOutbound();
        assertThat(closeFrame.content().toString(UTF_8), equalTo("c[3000,\"Go away!\"]"));
        assertThat(ch.isActive(), is(false));
        closeFrame.release();
        webSocketTestClose(V13);
    }

    /*
     * Equivalent to WebsocketHixie76.test_empty_frame in sockjs-protocol-0.3.3.py.
     */
    @Test
    public void webSocketHixie76TestEmptyFrame() throws Exception {
        final ChannelHandler echoHandler = new SockJsEchoHandler();
        final String sessionUrl = "/echo/222/" + UUID.randomUUID();
        final EmbeddedChannel ch = wsSockJsPipeline("/echo", echoHandler);

        final FullHttpRequest request = webSocketUpgradeRequest(sessionUrl + "/websocket", V00);
        ch.writeInbound(request);

        final FullHttpResponse upgradeResponse = HttpUtil.decodeFullHttpResponse(ch);
        assertThat(upgradeResponse.content().toString(UTF_8), equalTo("8jKS'y:G*Co,Wxa-"));
        upgradeResponse.release();

        final TextWebSocketFrame openFrame = ch.readOutbound();
        assertThat(openFrame.content().toString(UTF_8), equalTo("o"));
        openFrame.release();

        final TextWebSocketFrame emptyWebSocketFrame = new TextWebSocketFrame("");
        ch.writeInbound(emptyWebSocketFrame);

        final TextWebSocketFrame webSocketFrame = new TextWebSocketFrame("\"a\"");
        ch.writeInbound(webSocketFrame);

        final TextWebSocketFrame textFrame = ch.readOutbound();
        assertThat(textFrame.content().toString(UTF_8), equalTo("a[\"a\"]"));
        textFrame.release();
    }

    /*
     * Equivalent to WebsocketHixie76.test_reuseSessionId in sockjs-protocol-0.3.3.py.
     */
    @Test
    public void webSocketHixie76TestReuseSessionId() throws Exception {
        final ChannelHandler echoHandler = new SockJsEchoHandler();
        final String sessionUrl = "/echo/222/" + UUID.randomUUID();
        final EmbeddedChannel ch1 = wsSockJsPipeline("/echo", echoHandler);
        final EmbeddedChannel ch2 = wsSockJsPipeline("/echo", echoHandler);

        ch1.writeInbound(webSocketUpgradeRequest(sessionUrl + "/websocket", V00));
        final FullHttpResponse upgradeResponse = HttpUtil.decodeFullHttpResponse(ch1);
        assertThat(upgradeResponse.content().toString(UTF_8), equalTo("8jKS'y:G*Co,Wxa-"));
        upgradeResponse.release();

        ch2.writeInbound(webSocketUpgradeRequest(sessionUrl + "/websocket", V00));
        final FullHttpResponse upgradeResponse2 = HttpUtil.decodeFullHttpResponse(ch2);
        assertThat(upgradeResponse2.content().toString(UTF_8), equalTo("8jKS'y:G*Co,Wxa-"));
        upgradeResponse2.release();

        final ByteBufHolder open1 = ch1.readOutbound();
        assertThat(open1.content().toString(UTF_8), equalTo("o"));
        open1.release();

        final ByteBufHolder open2 = ch2.readOutbound();
        assertThat(open2.content().toString(UTF_8), equalTo("o"));
        open2.release();

        ch1.writeInbound(new TextWebSocketFrame("\"a\""));
        final ByteBufHolder msg1 = ch1.readOutbound();
        assertThat(msg1.content().toString(UTF_8), equalTo("a[\"a\"]"));
        msg1.release();

        ch2.writeInbound(new TextWebSocketFrame("\"b\""));
        final ByteBufHolder msg2 = ch2.readOutbound();
        assertThat(msg2.content().toString(UTF_8), equalTo("a[\"b\"]"));
        msg2.release();

        ch1.close();
        ch2.close();

        final EmbeddedChannel newCh = wsSockJsPipeline("/echo", echoHandler);

        newCh.writeInbound(webSocketUpgradeRequest(sessionUrl + "/websocket"));
        final FullHttpResponse upgradeResponse3 = HttpUtil.decodeFullHttpResponse(newCh);
        assertThat(upgradeResponse3.getStatus(), equalTo(HttpResponseStatus.SWITCHING_PROTOCOLS));
        upgradeResponse3.release();

        final ByteBufHolder open = (ByteBufHolder) readOutboundDiscardEmpty(newCh);
        assertThat(open.content().toString(UTF_8), equalTo("o"));
        open.release();

        newCh.writeInbound(new TextWebSocketFrame("\"a\""));
        final ByteBufHolder msg = (ByteBufHolder) readOutboundDiscardEmpty(newCh);
        assertThat(msg.content().toString(UTF_8), equalTo("a[\"a\"]"));
        msg.release();
        newCh.close();
    }

    /*
     * Equivalent to WebsocketHixie76.test_haproxy in sockjs-protocol-0.3.3.py.
     */
    @Test
    public void webSocketHixie76TestHAProxy() throws Exception {
        final ChannelHandler echoHandler = new SockJsEchoHandler();
        final String sessionUrl = "/echo/222/" + UUID.randomUUID();
        final EmbeddedChannel ch = wsSockJsPipeline("/echo", echoHandler);

        final FullHttpRequest request = new DefaultFullHttpRequest(HTTP_1_1, GET, sessionUrl + "/websocket");
        request.headers().set(HOST, "server.test.com");
        request.headers().set(UPGRADE, WEBSOCKET.toString());
        request.headers().set(CONNECTION, "Upgrade");
        request.headers().set(CONNECTION, "Upgrade");
        request.headers().set(SEC_WEBSOCKET_KEY1, "4 @1  46546xW%0l 1 5");
        request.headers().set(SEC_WEBSOCKET_KEY2, "12998 5 Y3 1  .P00");
        request.headers().set(ORIGIN, "http://example.com");
        ch.writeInbound(request);

        final HttpResponse upgradeResponse = HttpUtil.decode(ch);
        assertThat(upgradeResponse.getStatus(), equalTo(HttpResponseStatus.SWITCHING_PROTOCOLS));

        ch.writeInbound(Unpooled.copiedBuffer("^n:ds[4U", US_ASCII));

        final ByteBuf key = (ByteBuf) readOutboundDiscardEmpty(ch);
        assertThat(key.toString(US_ASCII), equalTo("8jKS'y:G*Co,Wxa-"));
        key.release();

        final TextWebSocketFrame openFrame = ch.readOutbound();
        assertThat(openFrame.content().toString(UTF_8), equalTo("o"));
        openFrame.release();

        final TextWebSocketFrame textWebSocketFrame = new TextWebSocketFrame("\"a\"");
        ch.writeInbound(textWebSocketFrame);

        final TextWebSocketFrame textFrame = ch.readOutbound();
        assertThat(textFrame.content().toString(UTF_8), equalTo("a[\"a\"]"));
        textFrame.release();
    }

    /*
     * Equivalent to WebsocketHixie76.test_broken_json in sockjs-protocol-0.3.3.py.
     */
    @Test
    public void webSocketHixie76TestBrokenJSON() throws Exception {
        final ChannelHandler echoHandler = new SockJsEchoHandler();
        final String serviceName = "/echo"; //this was close??
        final String sessionUrl = serviceName + "/222/" + UUID.randomUUID();
        final EmbeddedChannel ch = wsSockJsPipeline("/echo", echoHandler);

        final FullHttpRequest request = webSocketUpgradeRequest(sessionUrl + "/websocket", V00);
        ch.writeInbound(request);

        final FullHttpResponse upgradeResponse = HttpUtil.decodeFullHttpResponse(ch);
        assertThat(upgradeResponse.getStatus(), equalTo(HttpResponseStatus.SWITCHING_PROTOCOLS));
        assertThat(upgradeResponse.content().toString(UTF_8), equalTo("8jKS'y:G*Co,Wxa-"));
        final ByteBufHolder open = ch.readOutbound();
        assertThat(open.content().toString(UTF_8), equalTo("o"));
        open.release();
        upgradeResponse.release();
        webSocketTestBrokenJSON(V13);
    }

    /*
     * Equivalent to WebsocketHybi10.test_transport in sockjs-protocol-0.3.3.py.
     */
    @Test
    public void webSocketHybi10TestTransport() throws Exception {
        webSocketTestTransport(V08);
    }

    /*
     * Equivalent to WebsocketHybi10.test_close in sockjs-protocol-0.3.3.py.
     */
    @Test
    public void webSocketHybi10TestClose() throws Exception {
        webSocketTestClose(V08);
    }

    /*
     * Equivalent to WebsocketHybi10.test_headersSantity in sockjs-protocol-0.3.3.py.
     */
    @Test
    public void webSocketHybi10TestHeadersSanity() throws Exception {
        verifyHeaders(V07);
        verifyHeaders(V08);
        verifyHeaders(V13);
    }

    /*
     * Equivalent to WebsocketHybi10.test_broken_json in sockjs-protocol-0.3.3.py.
     */
    @Test
    public void webSocketHybi10TestBrokenJSON() throws Exception {
        webSocketTestBrokenJSON(V08);
    }

    /*
     * Equivalent to WebsocketHybi10.test_transport, but for Hybi17, in sockjs-protocol-0.3.3.py.
     */
    @Test
    public void webSocketHybi17TestTransport() throws Exception {
        webSocketTestTransport(V13);
    }

    /*
     * Equivalent to WebsocketHybi10.test_close, but for Hybi17, in sockjs-protocol-0.3.3.py.
     */
    @Test
    public void webSocketHybi17TestClose() throws Exception {
        webSocketTestClose(V13);
    }

    /*
     * Equivalent to WebsocketHybi10.test_broken_json, but for Hybi17, in sockjs-protocol-0.3.3.py.
     */
    @Test
    public void webSocketHybi17TestBrokenJSON() throws Exception {
        webSocketTestBrokenJSON(V13);
    }

    /*
     * Equivalent to WebsocketHybi10.test_firefox_602_connection_header in sockjs-protocol-0.3.3.py.
     */
    @Test
    public void webSocketHybi10Firefox602ConnectionHeader() throws Exception {
        final ChannelHandler echoHandler = new SockJsEchoHandler();
        final EmbeddedChannel ch = wsSockJsPipeline("/echo", echoHandler);
        final FullHttpRequest request = HttpUtil.webSocketUpgradeRequest("/echo/123/123/websocket", V08);
        request.headers().set(CONNECTION, "keep-alive, Upgrade");
        ch.writeInbound(request);

        final HttpResponse response = HttpUtil.decode(ch);
        assertThat(response.getStatus(), is(HttpResponseStatus.SWITCHING_PROTOCOLS));
        assertThat(response.headers().get(CONNECTION), equalTo("Upgrade"));
    }

    /*
     * Equivalent to XhrPolling.test_options in sockjs-protocol-0.3.3.py.
     */
    @Test
    public void xhrPollingTestOptions() throws Exception {
        final CorsConfig corsConfig = SockJsChannelInitializer.defaultCorsConfig()
                .preflightResponseHeader(Names.CONTENT_TYPE, "text/plain; charset=UTF-8")
                .build();
        final ChannelHandler echoHandler = new SockJsEchoHandler();
        final String sessionUrl = "/echo/abc/" + UUID.randomUUID();
        final FullHttpRequest xhrRequest = httpRequest(sessionUrl + "/xhr", OPTIONS);

        final HttpResponse xhrOptionsResponse = xhrRequest(xhrRequest,
                sockJsPipeline("/echo", echoHandler, corsConfig));
        assertCORSPreflightResponseHeaders(xhrOptionsResponse);

        final FullHttpRequest xhrSendRequest = httpRequest(sessionUrl + "/xhr_send", OPTIONS);
        final HttpResponse xhrSendOptionsResponse = xhrRequest(xhrSendRequest,
                sockJsPipeline("echo", echoHandler, corsConfig));
        assertCORSPreflightResponseHeaders(xhrSendOptionsResponse);
    }

    /*
     * Equivalent to XhrPolling.test_transport in sockjs-protocol-0.3.3.py.
     */
    @Test
    public void xhrPollingTestTransport() throws Exception {
        final ChannelHandler echoHandler = new SockJsEchoHandler();
        final String sessionUrl = "/echo/abc/" + UUID.randomUUID();

        final FullHttpResponse response = xhrRequest(sessionUrl, sockJsPipeline("/echo", echoHandler));
        assertOpenFrameResponse(response);
        assertThat(response.headers().get(CONTENT_TYPE), equalTo(HttpResponseBuilder.CONTENT_TYPE_JAVASCRIPT));
        assertCORSHeaders(response, "*");
        verifyNotCached(response);

        final FullHttpResponse xhrSendResponse = xhrSendRequest(sessionUrl, "[\"x\"]",
                sockJsPipeline("/echo", echoHandler));
        assertNoContent(xhrSendResponse);
        assertThat(xhrSendResponse.headers().get(CONTENT_TYPE), equalTo(HttpResponseBuilder.CONTENT_TYPE_PLAIN));
        assertCORSHeaders(response, "*");
        verifyNotCached(xhrSendResponse);
        xhrSendResponse.release();

        final FullHttpResponse pollResponse = xhrRequest(sessionUrl, sockJsPipeline("/echo", echoHandler));
        assertMessageFrameContent(pollResponse, "x");
        pollResponse.release();
    }

    @Test
    public void xhrPollingSessionReuse() throws Exception {
        final ChannelHandler echoHandler = new SockJsEchoHandler();
        final String sessionUrl = "/echo/abc/" + UUID.randomUUID();
        assertOpenFrameResponse(xhrRequest(sessionUrl, sockJsPipeline("/echo", echoHandler)));
        assertNoContent(xhrSendRequest(sessionUrl, "[\"x\"]", sockJsPipeline("/echo", echoHandler)));
        assertMessageFrameContent(xhrRequest(sessionUrl, sockJsPipeline("/echo", echoHandler)), "x");
        xhrRequest(sessionUrl, sockJsPipeline("/echo", echoHandler));
        assertNoContent(xhrSendRequest(sessionUrl, "[\"x\"]", sockJsPipeline("/echo", echoHandler)));
        assertMessageFrameContent(xhrRequest(sessionUrl, sockJsPipeline("/echo", echoHandler)), "x");
    }

    /*
     * Equivalent to XhrPolling.test_invalid_session in sockjs-protocol-0.3.3.py.
     */
    @Test
    public void xhrPollingTestInvalidSession() throws Exception {
        final ChannelHandler echoHandler = new SockJsEchoHandler();
        final String sessionUrl = "/echo/abc/" + UUID.randomUUID();
        final FullHttpResponse xhrSendResponse = xhrSendRequest(sessionUrl, "[\"x\"]",
                sockJsPipeline("/echo", echoHandler));
        assertThat(xhrSendResponse.getStatus(), is(HttpResponseStatus.NOT_FOUND));
        xhrSendResponse.release();
    }

    /*
     * Equivalent to XhrPolling.test_invalid_json sockjs-protocol-0.3.3.py.
     */
    @Test
    public void xhrPollingTestInvalidJson() throws Exception {
        final ChannelHandler echoHandler = new SockJsEchoHandler();
        final String sessionUrl = "/echo/abc/" + UUID.randomUUID();

        assertOpenFrameResponse(xhrRequest(sessionUrl, sockJsPipeline("/echo", echoHandler)));

        final FullHttpResponse xhrSendResponse = xhrSendRequest(sessionUrl, "[\"x\"",
                sockJsPipeline("/echo", echoHandler));
        assertThat(xhrSendResponse.getStatus(), is(HttpResponseStatus.INTERNAL_SERVER_ERROR));
        assertThat(xhrSendResponse.content().toString(UTF_8), equalTo("Broken JSON encoding."));
        xhrSendResponse.release();

        final FullHttpResponse noPayloadResponse = xhrSendRequest(sessionUrl, "", sockJsPipeline("/echo", echoHandler));
        assertThat(noPayloadResponse.getStatus(), is(HttpResponseStatus.INTERNAL_SERVER_ERROR));
        assertThat(noPayloadResponse.content().toString(UTF_8), equalTo("Payload expected."));
        noPayloadResponse.release();

        final FullHttpResponse validSendResponse = xhrSendRequest(sessionUrl, "[\"a\"]",
                sockJsPipeline("/echo", echoHandler));
        assertThat(validSendResponse.getStatus(), is(HttpResponseStatus.NO_CONTENT));
        validSendResponse.release();

        final FullHttpResponse pollResponse = xhrRequest(sessionUrl, sockJsPipeline("/echo", echoHandler));
        assertMessageFrameContent(pollResponse, "a");
        pollResponse.release();
    }

    /*
     * Equivalent to XhrPolling.test_content_types sockjs-protocol-0.3.3.py.
     */
    @Test
    public void xhrPollingTestContentTypes() throws Exception {
        final ChannelHandler echoHandler = new SockJsEchoHandler();
        final String sessionUrl = "/echo/abc/" + UUID.randomUUID();

        final FullHttpResponse response = xhrRequest(sessionUrl, sockJsPipeline("/echo", echoHandler));
        assertThat(response.getStatus(), is(HttpResponseStatus.OK));
        assertThat(response.content().toString(UTF_8), equalTo("o\n"));
        response.release();

        final FullHttpResponse textPlain = xhrSendRequest(sessionUrl, "[\"a\"]", "text/plain",
                sockJsPipeline("/echo", echoHandler));
        assertThat(textPlain.getStatus(), is(HttpResponseStatus.NO_CONTENT));
        textPlain.release();

        final FullHttpResponse json = xhrSendRequest(sessionUrl, "[\"b\"]", "application/json",
                sockJsPipeline("/echo", echoHandler));
        assertThat(json.getStatus(), is(HttpResponseStatus.NO_CONTENT));
        json.release();

        final FullHttpResponse json2 = xhrSendRequest(sessionUrl, "[\"c\"]", "application/json;charset=utf-8",
                sockJsPipeline("/echo", echoHandler));
        assertThat(json2.getStatus(), is(HttpResponseStatus.NO_CONTENT));
        json2.release();
        final FullHttpResponse xml = xhrSendRequest(sessionUrl, "[\"d\"]", "application/xml",
                sockJsPipeline("/echo", echoHandler));
        assertThat(xml.getStatus(), is(HttpResponseStatus.NO_CONTENT));
        xml.release();
        final FullHttpResponse xml2 = xhrSendRequest(sessionUrl, "[\"e\"]", "text/xml",
                sockJsPipeline("/echo", echoHandler));
        assertThat(xml2.getStatus(), is(HttpResponseStatus.NO_CONTENT));
        xml2.release();
        final FullHttpResponse xml3 = xhrSendRequest(sessionUrl, "[\"f\"]", "text/xml; charset=utf-8",
                sockJsPipeline("/echo", echoHandler));
        assertThat(xml3.getStatus(), is(HttpResponseStatus.NO_CONTENT));
        xml3.release();
        final FullHttpResponse empty = xhrSendRequest(sessionUrl, "[\"g\"]", "", sockJsPipeline("/echo", echoHandler));
        assertThat(empty.getStatus(), is(HttpResponseStatus.NO_CONTENT));
        empty.release();

        final FullHttpResponse pollRequest = xhrRequest(sessionUrl, sockJsPipeline("/echo", echoHandler));
        assertThat(pollRequest.getStatus(), is(HttpResponseStatus.OK));
        assertThat(pollRequest.content().toString(UTF_8), equalTo("a[\"a\",\"b\",\"c\",\"d\",\"e\",\"f\",\"g\"]\n"));
        pollRequest.release();
    }

    /*
     * Equivalent to XhrPolling.test_request_headers_cors sockjs-protocol-0.3.3.py.
     */
    @Test
    public void xhrPollingTestRequestHeadersCors() throws Exception {
        final ChannelHandler echoHandler = new SockJsEchoHandler();
        final String sessionUrl = "/echo/abc/" + UUID.randomUUID();

        final FullHttpRequest okRequest = httpRequest(sessionUrl + "/xhr", POST);
        okRequest.headers().set(ACCESS_CONTROL_REQUEST_HEADERS, "a, b, c");
        CorsConfig corsConfig = SockJsChannelInitializer.defaultCorsConfig().allowedRequestHeaders("a, b, c").build();
        final HttpResponse response = xhrRequest(okRequest, sockJsPipeline("/echo", echoHandler, corsConfig));
        assertThat(response.getStatus(), is(HttpResponseStatus.OK));
        assertCORSHeaders(response, "*");
        assertThat(response.headers().get(ACCESS_CONTROL_ALLOW_HEADERS), equalTo("a, b, c"));

        final String emptySessionUrl = "/echo/abc/" + UUID.randomUUID();
        final FullHttpRequest emptyHeaderRequest = httpRequest(emptySessionUrl + "/xhr", POST);
        emptyHeaderRequest.headers().set(ACCESS_CONTROL_REQUEST_HEADERS, "");
        corsConfig = SockJsChannelInitializer.defaultCorsConfig().allowedRequestHeaders("").build();
        final HttpResponse emptyHeaderResponse = xhrRequest(emptyHeaderRequest,
                sockJsPipeline("/echo", echoHandler, corsConfig));
        assertThat(emptyHeaderResponse.getStatus(), is(HttpResponseStatus.OK));
        assertCORSHeaders(response, "*");
        //TODO: sort out if this should be possible. We added an request header of '""' above. Should that be ignored?
        assertThat(emptyHeaderResponse.headers().get(ACCESS_CONTROL_ALLOW_HEADERS), equalTo(""));

        final String noHeaderSessionUrl = "/echo/abc/" + UUID.randomUUID();
        final FullHttpRequest noHeaderRequest = httpRequest(noHeaderSessionUrl + "/xhr", POST);
        final HttpResponse noHeaderResponse = xhrRequest(noHeaderRequest, sockJsPipeline("/echo", echoHandler));
        assertThat(noHeaderResponse.getStatus(), is(HttpResponseStatus.OK));
        assertCORSHeaders(response, "*");
        //TODO: sort out why this should be null and not contains a Content-Type header?
        //assertThat(noHeaderResponse.headers().get(ACCESS_CONTROL_ALLOW_HEADERS), is(nullValue()));
    }

    /*
     * Equivalent to XhrStreaming.test_options in sockjs-protocol-0.3.3.py.
     */
    @Test
    public void xhrStreamingTestOptions() throws Exception {
        final ChannelHandler echoHandler = new SockJsEchoHandler();
        final String sessionUrl = "/echo/222/" + UUID.randomUUID();
        final EmbeddedChannel ch = sockJsPipeline("/echo", echoHandler);

        final FullHttpRequest request = httpRequest(sessionUrl + TransportType.XHR_STREAMING.path(), GET);
        ch.writeInbound(request);

        final HttpResponse response = ch.readOutbound();
        assertThat(response.getStatus(), equalTo(HttpResponseStatus.OK));
        assertThat(response.headers().get(CONTENT_TYPE), equalTo(HttpResponseBuilder.CONTENT_TYPE_JAVASCRIPT));
        assertCORSHeaders(response, "*");
        verifyNoCacheHeaders(response);

        final DefaultHttpContent prelude = ch.readOutbound();
        assertThat(prelude.content().readableBytes(), is(PreludeFrame.CONTENT_SIZE + 1));
        final ByteBuf buffer = Unpooled.buffer(PreludeFrame.CONTENT_SIZE + 1);
        prelude.content().readBytes(buffer);
        buffer.release();
        prelude.release();

        final DefaultHttpContent openResponse = ch.readOutbound();
        assertThat(openResponse.content().toString(UTF_8), equalTo("o\n"));
        openResponse.release();

        final FullHttpResponse validSendResponse = xhrSendRequest(sessionUrl, "[\"x\"]",
                sockJsPipeline("/echo", echoHandler));
        assertThat(validSendResponse.getStatus(), is(HttpResponseStatus.NO_CONTENT));
        validSendResponse.release();

        final DefaultHttpContent chunk = ch.readOutbound();
        assertThat(chunk.content().toString(UTF_8), equalTo("a[\"x\"]\n"));
        chunk.release();
    }

    /*
     * Equivalent to XhrStreaming.test_response_limit in sockjs-protocol-0.3.3.py.
     */
    @Test
    public void xhrStreamingTestResponseLimit() throws Exception {
        final ChannelHandler echoHandler = new SockJsEchoHandler();
        final String sessionUrl = "/echo/222/" + UUID.randomUUID();
        final EmbeddedChannel ch = sockJsPipeline("/echo", echoHandler);
        ch.config().setOption(SockJsChannelOption.MAX_STREAMING_BYTES_SIZE, 4096);

        final FullHttpRequest request = httpRequest(sessionUrl + TransportType.XHR_STREAMING.path(), GET);
        ch.writeInbound(request);
        final HttpResponse response = ch.readOutbound();
        assertThat(response.getStatus(), equalTo(HttpResponseStatus.OK));
        assertThat(response.headers().get(CONTENT_TYPE), equalTo(HttpResponseBuilder.CONTENT_TYPE_JAVASCRIPT));
        assertCORSHeaders(response, "*");
        verifyNoCacheHeaders(response);

        final DefaultHttpContent prelude = ch.readOutbound();
        assertThat(prelude.content().readableBytes(), is(PreludeFrame.CONTENT_SIZE + 1));
        final ByteBuf buf = Unpooled.buffer(PreludeFrame.CONTENT_SIZE + 1);
        prelude.content().readBytes(buf);
        buf.release();
        prelude.release();

        final DefaultHttpContent openResponse = ch.readOutbound();
        assertThat(openResponse.content().toString(UTF_8), equalTo("o\n"));
        openResponse.release();

        final String msg = generateMessage(128);
        for (int i = 0; i < 31; i++) {
            final FullHttpResponse validSendResponse = xhrSendRequest(sessionUrl, "[\"" + msg + "\"]",
                    sockJsPipeline("/echo", echoHandler));
            assertThat(validSendResponse.getStatus(), is(HttpResponseStatus.NO_CONTENT));
            validSendResponse.release();
            final DefaultHttpContent chunk = ch.readOutbound();
            assertThat(chunk.content().toString(UTF_8), equalTo("a[\"" + msg + "\"]\n"));
            chunk.release();
        }
        final LastHttpContent lastChunk = ch.readOutbound();
        assertThat(lastChunk.content().readableBytes(), is(0));
        lastChunk.release();
        assertThat(ch.isOpen(), is(false));
    }

    /*
     * Equivalent to EventSource.test_response_limit in sockjs-protocol-0.3.3.py.
     */
    @Test
    public void eventSourceTestResponseLimit() throws Exception {
        final ChannelHandler echoHandler = new SockJsEchoHandler();
        final String sessionUrl = "/echo/222/" + UUID.randomUUID();
        final EmbeddedChannel ch = sockJsPipeline("/echo", echoHandler);
        ch.config().setOption(SockJsChannelOption.MAX_STREAMING_BYTES_SIZE, 4096);

        final FullHttpRequest request = httpRequest(sessionUrl + TransportType.EVENTSOURCE.path(), GET);
        ch.writeInbound(request);
        final HttpResponse response =  ch.readOutbound();
        assertThat(response.getStatus(), equalTo(HttpResponseStatus.OK));
        assertThat(response.headers().get(CONTENT_TYPE), equalTo(EventSourceTransport.CONTENT_TYPE_EVENT_STREAM));

        final DefaultHttpContent newLinePrelude = ch.readOutbound();
        assertThat(newLinePrelude.content().toString(UTF_8), equalTo("\r\n"));
        newLinePrelude.release();

        final DefaultHttpContent data = ch.readOutbound();
        assertThat(data.content().toString(UTF_8), equalTo("data: o\r\n\r\n"));
        data.release();

        final String msg = generateMessage(4096);
        final FullHttpResponse validSendResponse = xhrSendRequest(sessionUrl, "[\"" + msg + "\"]",
                sockJsPipeline("/echo", echoHandler));
        assertThat(validSendResponse.getStatus(), is(HttpResponseStatus.NO_CONTENT));
        validSendResponse.release();

        final DefaultHttpContent chunk = ch.readOutbound();
        assertThat(chunk.content().toString(UTF_8), equalTo("data: a[\"" + msg + "\"]\r\n\r\n"));
        chunk.release();
        assertThat(ch.isOpen(), is(false));
    }

    /*
     * Equivalent to EventSource.test_transport in sockjs-protocol-0.3.3.py.
     */
    @Test
    public void eventSourceTestTransport() throws Exception {
        final ChannelHandler echoHandler = new SockJsEchoHandler();
        final String sessionUrl = "/echo/222/" + UUID.randomUUID();
        final EmbeddedChannel ch = sockJsPipeline("/echo", echoHandler);
        ch.config().setOption(SockJsChannelOption.MAX_STREAMING_BYTES_SIZE, 4096);

        final FullHttpRequest request = httpRequest(sessionUrl + TransportType.EVENTSOURCE.path(), GET);
        ch.writeInbound(request);
        final HttpResponse response =  ch.readOutbound();
        assertThat(response.getStatus(), equalTo(HttpResponseStatus.OK));
        assertThat(response.headers().get(CONTENT_TYPE), equalTo(EventSourceTransport.CONTENT_TYPE_EVENT_STREAM));

        final DefaultHttpContent newLinePrelude = ch.readOutbound();
        assertThat(newLinePrelude.content().toString(UTF_8), equalTo("\r\n"));
        newLinePrelude.release();

        final DefaultHttpContent data = ch.readOutbound();
        assertThat(data.content().toString(UTF_8), equalTo("data: o\r\n\r\n"));
        data.release();

        final String msg = "[\"  \\u0000\\n\\r \"]";
        final FullHttpResponse validSendResponse = xhrSendRequest(sessionUrl, msg,
                sockJsPipeline("/echo", echoHandler));
        assertThat(validSendResponse.getStatus(), is(HttpResponseStatus.NO_CONTENT));
        validSendResponse.release();

        final DefaultHttpContent chunk = ch.readOutbound();
        assertThat(chunk.content().toString(UTF_8), equalTo("data: a[\"  \\u0000\\n\\r \"]\r\n\r\n"));
        chunk.release();
    }

    /*
     * Equivalent to HtmlFile.test_transport in sockjs-protocol-0.3.3.py.
     */
    @Test
    public void htmlFileTestTransport() throws Exception {
        final ChannelHandler echoHandler = new SockJsEchoHandler();
        final String serviceName = "/echo";
        final String sessionUrl = serviceName + "/222/" + UUID.randomUUID();
        final EmbeddedChannel ch = sockJsPipeline("/echo", echoHandler);
        ch.config().setOption(SockJsChannelOption.MAX_STREAMING_BYTES_SIZE, 4096);

        final FullHttpRequest request = httpRequest(sessionUrl + TransportType.HTMLFILE.path() + "?c=callback", GET);
        ch.writeInbound(request);
        final HttpResponse response =  ch.readOutbound();
        assertThat(response.getStatus(), equalTo(HttpResponseStatus.OK));
        assertThat(response.headers().get(CONTENT_TYPE), equalTo(HttpResponseBuilder.CONTENT_TYPE_HTML));

        final HttpContent headerChunk = ch.readOutbound();
        assertThat(headerChunk.content().readableBytes(), is(greaterThan(1024)));
        final String header = headerChunk.content().toString(UTF_8);
        assertThat(header, containsString("var c = parent.callback"));
        headerChunk.release();

        final HttpContent openChunk = ch.readOutbound();
        assertThat(openChunk.content().toString(UTF_8), equalTo("<script>\np(\"o\");\n</script>\r\n"));
        openChunk.release();

        final FullHttpResponse validSendResponse = xhrSendRequest(sessionUrl, "[\"x\"]",
                sockJsPipeline("/echo", echoHandler));
        assertThat(validSendResponse.getStatus(), is(HttpResponseStatus.NO_CONTENT));
        validSendResponse.release();

        final DefaultHttpContent messageChunk = ch.readOutbound();
        assertThat(messageChunk.content().toString(UTF_8), equalTo("<script>\np(\"a[\\\"x\\\"]\");\n</script>\r\n"));
        messageChunk.release();
        ch.finish();
    }

    /*
     * Equivalent to HtmlFile.test_no_callback in sockjs-protocol-0.3.3.py.
     */
    @Test
    public void htmlFileTestNoCallback() throws Exception {
        final ChannelHandler echoHandler = new SockJsEchoHandler();
        final String serviceName = "/echo";
        final String sessionUrl = serviceName + "/222/" + UUID.randomUUID();
        final EmbeddedChannel ch = sockJsPipeline("/echo", echoHandler);
        ch.config().setOption(SockJsChannelOption.MAX_STREAMING_BYTES_SIZE, 4096);

        final FullHttpRequest request = httpRequest(sessionUrl + TransportType.HTMLFILE.path() + "?c=", GET);
        ch.writeInbound(request);
        final FullHttpResponse response =  ch.readOutbound();
        assertThat(response.getStatus(), equalTo(HttpResponseStatus.INTERNAL_SERVER_ERROR));
        assertThat(response.content().toString(UTF_8), equalTo("\"callback\" parameter required"));
        response.release();
    }

    /*
     * Equivalent to HtmlFile.test_response_limit in sockjs-protocol-0.3.3.py.
     */
    @Test
    public void htmlFileTestResponseLimit() throws Exception {
        final ChannelHandler echoHandler = new SockJsEchoHandler();
        final String serviceName = "/echo";
        final String sessionUrl = serviceName + "/222/" + UUID.randomUUID();
        final EmbeddedChannel ch = sockJsPipeline("/echo", echoHandler);
        ch.config().setOption(SockJsChannelOption.MAX_STREAMING_BYTES_SIZE, 4096);

        final FullHttpRequest request = httpRequest(sessionUrl + TransportType.HTMLFILE.path() + "?c=callback", GET);
        ch.writeInbound(request);
        final HttpResponse response =  ch.readOutbound();
        assertThat(response.getStatus(), equalTo(HttpResponseStatus.OK));

        // read and discard header chunk
        ReferenceCountUtil.release(ch.readOutbound());
        // read and discard open frame
        ReferenceCountUtil.release(ch.readOutbound());

        final String msg = generateMessage(4096);
        final FullHttpResponse validSendResponse = xhrSendRequest(sessionUrl, "[\"" + msg + "\"]",
                sockJsPipeline("/echo", echoHandler));
        assertThat(validSendResponse.getStatus(), is(HttpResponseStatus.NO_CONTENT));
        validSendResponse.release();

        final DefaultHttpContent chunk = ch.readOutbound();
        assertThat(chunk.content().toString(UTF_8), equalTo("<script>\np(\"a[\\\"" + msg + "\\\"]\");\n</script>\r\n"));
        assertThat(ch.isOpen(), is(false));
        chunk.release();
    }

    /*
     * Equivalent to JsonPolling.test_transport in sockjs-protocol-0.3.3.py.
     */
    @Test
    public void jsonpPollingTestTransport() throws Exception {
        final ChannelHandler echoHandler = new SockJsEchoHandler();
        final String serviceName = "/echo";
        final String sessionUrl = serviceName + "/222/" + UUID.randomUUID();

        final TestEmbeddedChannel ch = sockJsPipeline("/echo", echoHandler);
        final FullHttpResponse response = (FullHttpResponse) jsonpRequest(sessionUrl + "/jsonp?c=%63allback", ch);
        assertThat(response.getStatus(), is(HttpResponseStatus.OK));
        assertThat(response.content().toString(UTF_8), equalTo("callback(\"o\");\r\n"));
        assertThat(response.headers().get(CONTENT_TYPE), equalTo(HttpResponseBuilder.CONTENT_TYPE_JAVASCRIPT));
        verifyNotCached(response);
        response.release();

        final String data = "d=%5B%22x%22%5D";
        final FullHttpResponse sendResponse = jsonpSend(sessionUrl + "/jsonp_send", data,
                sockJsPipeline("/echo", echoHandler));
        assertThat(sendResponse.getStatus(), is(HttpResponseStatus.OK));
        assertThat(sendResponse.content().toString(UTF_8), equalTo("ok"));
        assertThat(sendResponse.headers().get(CONTENT_TYPE), equalTo(HttpResponseBuilder.CONTENT_TYPE_PLAIN));
        verifyNotCached(response);
        sendResponse.release();

        final FullHttpResponse pollResponse = (FullHttpResponse) jsonpRequest(sessionUrl + "/jsonp?c=callback",
                sockJsPipeline("/echo", echoHandler));
        assertThat(pollResponse.getStatus(), is(HttpResponseStatus.OK));
        assertThat(pollResponse.headers().get(CONTENT_TYPE), equalTo(HttpResponseBuilder.CONTENT_TYPE_JAVASCRIPT));
        assertThat(pollResponse.content().toString(UTF_8), equalTo("callback(\"a[\\\"x\\\"]\");\r\n"));
        verifyNotCached(pollResponse);
        pollResponse.release();
    }

    /*
     * Equivalent to JsonPolling.test_no_callback in sockjs-protocol-0.3.3.py.
     */
    @Test
    public void jsonpPollingTestNoCallback() throws Exception {
        final ChannelHandler echoHandler = new SockJsEchoHandler();
        final TestEmbeddedChannel ch = sockJsPipeline("/echo", echoHandler);
        final FullHttpResponse response = (FullHttpResponse) jsonpRequest("/echo/a/a/jsonp", ch);
        assertThat(response.getStatus(), is(HttpResponseStatus.INTERNAL_SERVER_ERROR));
        assertThat(response.content().toString(UTF_8), equalTo("\"callback\" parameter required"));
        response.release();
    }

    /*
     * Equivalent to JsonPolling.test_invalid_json in sockjs-protocol-0.3.3.py.
     */
    @Test
    public void jsonpPollingTestInvalidJson() throws Exception {
        final ChannelHandler echoHandler = new SockJsEchoHandler();
        final String serviceName = "/echo";
        final String sessionUrl = serviceName + "/222/" + UUID.randomUUID();

        final FullHttpResponse response = (FullHttpResponse) jsonpRequest(sessionUrl + "/jsonp?c=x",
                sockJsPipeline("/echo", echoHandler));
        assertThat(response.getStatus(), is(HttpResponseStatus.OK));
        assertThat(response.content().toString(UTF_8), equalTo("x(\"o\");\r\n"));
        response.release();

        assertBrokenJSONEncoding(jsonpSend(sessionUrl + "/jsonp_send", "d=%5B%22x",
                sockJsPipeline("/echo", echoHandler)));
        assertPayloadExpected(jsonpSend(sessionUrl + "/jsonp_send", "", sockJsPipeline("/echo", echoHandler)));
        assertPayloadExpected(jsonpSend(sessionUrl + "/jsonp_send", "d=", sockJsPipeline("/echo", echoHandler)));
        assertPayloadExpected(jsonpSend(sessionUrl + "/jsonp_send", "p=p", sockJsPipeline("/echo", echoHandler)));

        final FullHttpResponse sendResponse = jsonpSend(sessionUrl + "/jsonp_send", "d=%5B%22b%22%5D",
                sockJsPipeline("/echo", echoHandler));
        assertThat(sendResponse.getStatus(), is(HttpResponseStatus.OK));
        sendResponse.release();

        final FullHttpResponse pollResponse = (FullHttpResponse) jsonpRequest(sessionUrl + "/jsonp?c=x",
                sockJsPipeline("/echo", echoHandler));
        assertThat(pollResponse.getStatus(), is(HttpResponseStatus.OK));
        assertThat(pollResponse.content().toString(UTF_8), equalTo("x(\"a[\\\"b\\\"]\");\r\n"));
        pollResponse.release();
    }

    /*
     * Equivalent to JsonPolling.test_content_types in sockjs-protocol-0.3.3.py.
     */
    @Test
    public void jsonpPollingTestContentTypes() throws Exception {
        final ChannelHandler echoHandler = new SockJsEchoHandler();
        final String serviceName = "/echo";
        final String sessionUrl = serviceName + "/222/" + UUID.randomUUID();

        final FullHttpResponse response = (FullHttpResponse) jsonpRequest(sessionUrl + "/jsonp?c=x",
                sockJsPipeline("/echo", echoHandler));
        assertThat(response.content().toString(UTF_8), equalTo("x(\"o\");\r\n"));
        response.release();

        final String data = "d=%5B%22abc%22%5D";
        final FullHttpResponse sendResponse = jsonpSend(sessionUrl + "/jsonp_send", data,
                sockJsPipeline("/echo", echoHandler));
        assertThat(sendResponse.getStatus(), is(HttpResponseStatus.OK));
        sendResponse.release();

        final FullHttpRequest plainRequest = httpRequest(sessionUrl + "/jsonp_send", POST);
        plainRequest.headers().set(CONTENT_TYPE, "text/plain");
        final ByteBuf byteBuf = Unpooled.copiedBuffer("[\"%61bc\"]", UTF_8);
        plainRequest.content().writeBytes(byteBuf);
        byteBuf.release();

        final FullHttpResponse plainResponse = jsonpSend(plainRequest, sockJsPipeline("/echo", echoHandler));
        assertThat(plainResponse.getStatus(), is(HttpResponseStatus.OK));
        plainResponse.release();

        final FullHttpResponse pollResponse = (FullHttpResponse) jsonpRequest(sessionUrl + "/jsonp?c=x",
                sockJsPipeline("/echo", echoHandler));
        assertThat(pollResponse.getStatus(), is(HttpResponseStatus.OK));
        assertThat(pollResponse.content().toString(UTF_8), equalTo("x(\"a[\\\"abc\\\",\\\"%61bc\\\"]\");\r\n"));
        pollResponse.release();
    }

    /*
     * Equivalent to JsonPolling.test_close in sockjs-protocol-0.3.3.py.
     */
    @Test
    public void jsonpPollingTestClose() throws Exception {
        final ChannelHandler closeHandler = new SockJsCloseHandler();
        final String serviceName = "/close";
        final String sessionUrl = serviceName + "/222/" + UUID.randomUUID();
        final FullHttpResponse response = (FullHttpResponse) jsonpRequest(sessionUrl + "/jsonp?c=x",
                sockJsPipeline("/close", closeHandler));
        assertThat(response.content().toString(UTF_8), equalTo("x(\"o\");\r\n"));
        response.release();

        final FullHttpResponse firstResponse = (FullHttpResponse) jsonpRequest(sessionUrl + "/jsonp?c=x",
                sockJsPipeline("/close", closeHandler));
        assertThat(firstResponse.content().toString(UTF_8), equalTo("x(\"c[3000,\\\"Go away!\\\"]\");\r\n"));
        firstResponse.release();

        final FullHttpResponse secondResponse = (FullHttpResponse) jsonpRequest(sessionUrl + "/jsonp?c=x",
                sockJsPipeline("/close", closeHandler));
        assertThat(secondResponse.content().toString(UTF_8), equalTo("x(\"c[3000,\\\"Go away!\\\"]\");\r\n"));
        secondResponse.release();
    }

    /*
     * Equivalent to JsessionIdCookie.test_basic in sockjs-protocol-0.3.3.py.
     */
    @Test
    public void jsessionIdCookieTestBasic() throws Exception {
        final ChannelHandler echoHandler = new SockJsEchoHandler();
        final EmbeddedChannel ch = sockJsPipeline("/echo", echoHandler);
        ch.config().setOption(SockJsChannelOption.COOKIES_NEEDED, true);
        final FullHttpResponse response = infoRequest(ch, "/echo");
        assertThat(response.getStatus(), is(HttpResponseStatus.OK));
        verifyNoSET_COOKIE(response);
        assertThat(infoAsJson(response).get("cookie_needed").asBoolean(), is(true));
        response.release();
    }

    /*
     * Equivalent to JsessionIdCookie.test_xhr in sockjs-protocol-0.3.3.py.
     */
    @Test
    public void jsessionIdCookieTestXhr() throws Exception {
        final ChannelHandler echoHandler = new SockJsEchoHandler();
        final EmbeddedChannel ch = sockJsPipeline("/echo", echoHandler);
        ch.config().setOption(SockJsChannelOption.COOKIES_NEEDED, true);
        assertSetCookie(TransportType.XHR.path());

        final String serviceName = "/echo";
        final String sessionUrl = serviceName + "/abc/" + UUID.randomUUID();
        final FullHttpRequest request = httpRequest(sessionUrl + TransportType.XHR.path(), GET);
        request.headers().set("Cookie", ClientCookieEncoder.encode("JSESSIONID", "abcdef"));
        ch.writeInbound(request);
        final FullHttpResponse response = ch.readOutbound();
        assertThat(response.getStatus(), is(HttpResponseStatus.OK));
        assertSetCookie("abcdef", response);
        response.release();
    }

    /*
     * Equivalent to JsessionIdCookie.test_xhr_streaming in sockjs-protocol-0.3.3.py.
     */
    @Test
    public void jsessionIdCookieTestXhrStreaming() throws Exception {
        assertSetCookie(TransportType.XHR_STREAMING.path());
    }

    /*
     * Equivalent to JsessionIdCookie.test_eventsource in sockjs-protocol-0.3.3.py.
     */
    @Test
    public void jsessionIdCookieTestEventSource() throws Exception {
        assertSetCookie(TransportType.EVENTSOURCE.path());
    }

    /*
     * Equivalent to JsessionIdCookie.test_htmlfile in sockjs-protocol-0.3.3.py.
     */
    @Test
    public void jsessionIdCookieTestHtmlFile() throws Exception {
        assertSetCookie(TransportType.HTMLFILE.path() + "?c=callback");
    }

    /*
     * Equivalent to JsessionIdCookie.test_jsonp in sockjs-protocol-0.3.3.py.
     */
    @Test
    public void jsessionIdCookieTestJsonp() throws Exception {
        assertSetCookie(TransportType.JSONP.path() + "?c=callback");
    }

    /*
     * Equivalent to RawWebsocket.test_transport in sockjs-protocol-0.3.3.py.
     */
    @Test
    public void rawWebsocketTestTransport() throws Exception {
        final ChannelHandler echoHandler = new SockJsEchoHandler();
        final String serviceName = "/echo";
        final EmbeddedChannel ch = wsSockJsPipeline("/echo", echoHandler);

        ch.writeInbound(webSocketUpgradeRequest(serviceName + "/websocket"));
        // Discard Switching Protocols buildResponse
        ReferenceCountUtil.release(ch.readOutbound());
        ch.writeInbound(new TextWebSocketFrame("Hello world!\uffff"));
        final TextWebSocketFrame textFrame = ch.readOutbound();
        assertThat(textFrame.text(), equalTo("Hello world!\uffff"));
        textFrame.release();
        ch.finish();
    }

    /*
     * Equivalent to RawWebsocket.test_close in sockjs-protocol-0.3.3.py.
     */
    @Test
    public void rawWebsocketTestClose() throws Exception {
        final ChannelHandler closeHandler = new SockJsCloseHandler();
        final EmbeddedChannel ch = wsSockJsPipeline("/close", closeHandler);
        ch.writeInbound(webSocketUpgradeRequest("/close/websocket"));
        ReferenceCountUtil.release(ch.readOutbound());
        assertThat(ch.isActive(), is(false));
        ch.finish();
    }

    @Test
    public void webSocketCloseSession() throws Exception {
        final String serviceName = "/echo";
        final String sessionUrl = serviceName + "/222/" + UUID.randomUUID();
        final ChannelHandler handler = new SockJsEchoHandler();
        final EmbeddedChannel ch = wsSockJsPipeline("/echo", handler);

        final FullHttpRequest request = webSocketUpgradeRequest(sessionUrl + "/websocket", V13.toHttpHeaderValue());
        ch.writeInbound(request);

        // read and discard the HTTP Response (this will be a ByteBuf and not an object
        // as we have a HttpEncoder in the pipeline to start with.
        ReferenceCountUtil.release(ch.readOutbound());

        final ByteBufHolder open = (ByteBufHolder) readOutboundDiscardEmpty(ch);
        assertThat(open.content().toString(UTF_8), equalTo("o"));
        open.release();

        ch.writeInbound(new CloseWebSocketFrame(1000, "Normal close"));
        final CloseWebSocketFrame closeFrame = ch.readOutbound();
        assertThat(closeFrame.statusCode(), is(1000));
        assertThat(closeFrame.reasonText(), equalTo("Normal close"));
        closeFrame.release();
    }

    /*
     * Equivalent to JSONEncoding.test_xhr_server_encodes in sockjs-protocol-0.3.3.py.
     */
    @Test
    public void jsonEncodingTestXhrServerEncodes() throws Exception {
        final ChannelHandler echoHandler = new SockJsEchoHandler();
        final String sessionUrl = "/echo/abc/" + UUID.randomUUID();

        final FullHttpResponse response = xhrRequest(sessionUrl, sockJsPipeline("/echo", echoHandler));
        assertOpenFrameResponse(response);

        final String content = escapeCharacters(serverKillerStringEsc().toCharArray());
        final FullHttpResponse xhrSendResponse = xhrSendRequest(sessionUrl, "[\"" + content + "\"]",
                sockJsPipeline("/echo", echoHandler));
        assertThat(xhrSendResponse.getStatus(), is(HttpResponseStatus.NO_CONTENT));
        xhrSendResponse.release();

        final FullHttpResponse pollResponse = xhrRequest(sessionUrl, sockJsPipeline("/echo", echoHandler));
        assertThat(pollResponse.getStatus(), is(HttpResponseStatus.OK));
        assertThat(pollResponse.content().toString(UTF_8), equalTo("a[\"" + content + "\"]\n"));
        pollResponse.release();
    }

    /*
     * Equivalent to JSONEncoding.test_xhr_server_decodes in sockjs-protocol-0.3.3.py.
     */
    @Test
    public void jsonEncodingTestXhrServerDecodes() throws Exception {
        final ChannelHandler echoHandler = new SockJsEchoHandler();
        final String sessionUrl = "/echo/abc/" + UUID.randomUUID();

        final FullHttpResponse response = xhrRequest(sessionUrl, sockJsPipeline("/echo", echoHandler));
        assertThat(response.getStatus(), is(HttpResponseStatus.OK));
        assertThat(response.content().toString(UTF_8), equalTo("o\n"));
        response.release();

        final String content = "[\"" + clientKillerStringEsc() + "\"]";
        final FullHttpResponse xhrSendResponse = xhrSendRequest(sessionUrl, content,
                sockJsPipeline("/echo", echoHandler));
        assertThat(xhrSendResponse.getStatus(), is(HttpResponseStatus.NO_CONTENT));
        xhrSendResponse.release();

        final FullHttpResponse pollResponse = xhrRequest(sessionUrl, sockJsPipeline("/echo", echoHandler));
        assertThat(pollResponse.getStatus(), is(HttpResponseStatus.OK));

        // Let the content go through the MessageFrame to match what the buildResponse will go through.
        final MessageFrame messageFrame = new MessageFrame(JsonUtil.decode(content)[0]);
        String expectedContent = JsonUtil.encode(messageFrame.content().toString(UTF_8) + '\n');
        String responseContent = JsonUtil.encode(pollResponse.content().toString(UTF_8));
        assertThat(responseContent, equalTo(expectedContent));
        pollResponse.release();
        messageFrame.release();
    }

    /*
     * Equivalent to HandlingClose.test_close_frame in sockjs-protocol-0.3.3.py.
     */
    @Test
    public void handlingCloseTestCloseFrame() throws Exception {
        final ChannelHandler closeHandler = new SockJsCloseHandler();
        final String sessionUrl = "/close/222/" + UUID.randomUUID();
        final EmbeddedChannel ch = sockJsPipeline("/close", closeHandler);
        final FullHttpRequest request = httpRequest(sessionUrl + TransportType.XHR_STREAMING.path(), GET);
        ch.writeInbound(request);

        final HttpResponse response =  ch.readOutbound();
        assertThat(response.getStatus(), equalTo(HttpResponseStatus.OK));
        //Read and discard prelude
        ReferenceCountUtil.release(ch.readOutbound());
        // Read and discard of the open frame
        ReferenceCountUtil.release(ch.readOutbound());

        final DefaultHttpContent closeResponse = ch.readOutbound();
        assertThat(closeResponse.content().toString(UTF_8), equalTo("c[3000,\"Go away!\"]\n"));
        closeResponse.release();

        final EmbeddedChannel ch2 = sockJsPipeline("/close", closeHandler);
        final FullHttpRequest request2 = httpRequest(sessionUrl + TransportType.XHR_STREAMING.path(), GET);
        ch2.writeInbound(request2);

        final HttpResponse response2 =  ch2.readOutbound();
        assertThat(response2.getStatus(), equalTo(HttpResponseStatus.OK));

        //Read and discard prelude
        ReferenceCountUtil.release(ch2.readOutbound());

        final DefaultHttpContent closeResponse2 = ch2.readOutbound();
        assertThat(closeResponse2.content().toString(UTF_8), equalTo("c[3000,\"Go away!\"]\n"));
        closeResponse2.release();

        assertThat(ch.isActive(), is(false));
        assertThat(ch2.isActive(), is(false));
    }

    /*
     * Equivalent to HandlingClose.test_close_request in sockjs-protocol-0.3.3.py.
     */
    @Test
    public void handlingCloseTestCloseRequest() throws Exception {
        final ChannelHandler echoHandler = new SockJsEchoHandler();
        final String sessionUrl = "/echo/222/" + UUID.randomUUID();
        final EmbeddedChannel ch = sockJsPipeline("/echo", echoHandler);
        final FullHttpRequest request = httpRequest(sessionUrl + TransportType.XHR_STREAMING.path(), POST);
        ch.writeInbound(request);

        final HttpResponse response = ch.readOutbound();
        assertThat(response.getStatus(), equalTo(HttpResponseStatus.OK));
        //Read and discard prelude
        ReferenceCountUtil.release(ch.readOutbound());
        final DefaultHttpContent openResponse = ch.readOutbound();
        assertThat(openResponse.content().toString(UTF_8), equalTo("o\n"));
        openResponse.release();

        final EmbeddedChannel ch2 = sockJsPipeline("/echo", echoHandler);
        final FullHttpRequest request2 = httpRequest(sessionUrl + TransportType.XHR_STREAMING.path(), POST);
        ch2.writeInbound(request2);

        final HttpResponse response2 =  ch2.readOutbound();
        assertThat(response2.getStatus(), equalTo(HttpResponseStatus.OK));
        //Read and discard prelude
        ReferenceCountUtil.release(ch2.readOutbound());

        final DefaultHttpContent closeResponse = ch2.readOutbound();
        assertThat(closeResponse.content().toString(UTF_8), equalTo("c[2010,\"Another connection still open\"]\n"));
        closeResponse.release();

        assertThat(ch2.isActive(), is(false));
    }

    /*
     * Equivalent to HandlingClose.test_abort_xhr_streaming in sockjs-protocol-0.3.3.py.
     */
    @Test
    public void handlingCloseTestAbortXhrStreaming() throws Exception {
        final ChannelHandler echoHandler = new SockJsEchoHandler();
        final String sessionUrl = "/echo/222/" + UUID.randomUUID();
        final EmbeddedChannel ch = sockJsPipeline("/echo", echoHandler);
        final FullHttpRequest request = httpRequest(sessionUrl + TransportType.XHR_STREAMING.path(), GET);
        ch.writeInbound(request);

        final HttpResponse response =  ch.readOutbound();
        assertThat(response.getStatus(), equalTo(HttpResponseStatus.OK));
        //Read and discard prelude
        ReferenceCountUtil.release(ch.readOutbound());
        final DefaultHttpContent openResponse = ch.readOutbound();
        assertThat(openResponse.content().toString(UTF_8), equalTo("o\n"));
        openResponse.release();

        final EmbeddedChannel ch2 = sockJsPipeline("/echo", echoHandler);
        final FullHttpRequest request2 = httpRequest(sessionUrl + TransportType.XHR_STREAMING.path(), GET);
        ch2.writeInbound(request2);

        final HttpResponse response2 =  ch2.readOutbound();
        assertThat(response2.getStatus(), equalTo(HttpResponseStatus.OK));
        //Read and discard prelude
        ReferenceCountUtil.release(ch2.readOutbound());

        final DefaultHttpContent closeResponse2 = ch2.readOutbound();
        assertThat(closeResponse2.content().toString(UTF_8), equalTo("c[2010,\"Another connection still open\"]\n"));
        closeResponse2.release();

        assertThat(ch2.isActive(), is(false));
        ch.close();

        final EmbeddedChannel ch3 = sockJsPipeline("/echo", echoHandler);
        final FullHttpRequest request3 = httpRequest(sessionUrl + TransportType.XHR_STREAMING.path(), POST);
        ch3.writeInbound(request3);

        final HttpResponse response3 =  ch3.readOutbound();
        assertThat(response3.getStatus(), equalTo(HttpResponseStatus.OK));
        //Read and discard prelude
        ReferenceCountUtil.release(ch3.readOutbound());

        final DefaultHttpContent closeResponse3 = ch3.readOutbound();
        assertThat(closeResponse3.content().toString(UTF_8), equalTo("c[1002,\"Connection interrupted\"]\n"));
        closeResponse3.release();

        assertThat(ch3.isActive(), is(false));
        ch.close();
    }

    /*
     * Equivalent to HandlingClose.test_abort_xhr_polling in sockjs-protocol-0.3.3.py.
     */
    @Test
    public void handlingCloseTestAbortXhrPolling() throws Exception {
        final ChannelHandler echoHandler = new SockJsEchoHandler();
        final String sessionUrl = "/echo/000/" + UUID.randomUUID();

        final EmbeddedChannel ch = sockJsPipeline("/echo", echoHandler);
        ch.writeInbound(httpRequest(sessionUrl + TransportType.XHR.path(), GET));

        final FullHttpResponse response = ch.readOutbound();
        assertThat(response.getStatus(), is(HttpResponseStatus.OK));
        assertThat(response.content().toString(UTF_8), equalTo("o\n"));
        response.release();

        final EmbeddedChannel ch2 = sockJsPipeline("/echo", echoHandler);
        ch2.writeInbound(httpRequest(sessionUrl + TransportType.XHR.path(), GET));
        final FullHttpResponse response2 = ch2.readOutbound();
        assertThat(response2.content().toString(UTF_8), equalTo("c[2010,\"Another connection still open\"]\n"));
        response2.release();

        final EmbeddedChannel ch3 = sockJsPipeline("/echo", echoHandler);
        ch3.writeInbound(httpRequest(sessionUrl + TransportType.XHR.path(), GET));

        final FullHttpResponse response3 = ch3.readOutbound();
        assertThat(response3.content().toString(UTF_8), equalTo("c[1002,\"Connection interrupted\"]\n"));
        response3.release();
    }

    /*
     * Equivalent to Http10.test_synchronous in sockjs-protocol-0.3.3.py.
     */
    @Test
    public void http10TestSynchronous() throws Exception {
        final ChannelHandler echoHandler = new SockJsEchoHandler();

        final EmbeddedChannel ch = sockJsPipeline("/echo", echoHandler);
        final FullHttpRequest request = httpGetRequest("/echo", HTTP_1_0);
        request.headers().set(CONNECTION, KEEP_ALIVE);
        ch.writeInbound(request);

        final FullHttpResponse response = ch.readOutbound();
        assertThat(response.getStatus(), is(HttpResponseStatus.OK));
        assertThat(response.getProtocolVersion(), is(HTTP_1_0));
        assertThat(response.headers().get(TRANSFER_ENCODING), is(nullValue()));
        if (response.headers().get(CONTENT_LENGTH) == null) {
            assertThat(response.headers().get(CONNECTION), equalTo("close"));
            assertThat(response.content().toString(UTF_8), equalTo("Welcome to SockJS!\n"));
            assertThat(ch.isActive(), is(false));
        } else {
            assertThat(response.headers().get(CONTENT_LENGTH), is("19"));
            assertThat(response.content().toString(UTF_8), equalTo("Welcome to SockJS!\n"));
            final String connectionHeader = response.headers().get(CONNECTION);
            if (connectionHeader.contains("close") || connectionHeader.isEmpty()) {
                assertThat(ch.isActive(), is(false));
            } else {
                assertThat(connectionHeader, equalTo("keep-alive"));
                ch.writeInbound(httpGetRequest("/echo", HTTP_1_0));
                final HttpResponse newResponse = ch.readOutbound();
                assertThat(newResponse.getStatus(), is(HttpResponseStatus.OK));
            }
        }
        response.release();
    }

    /*
     * Equivalent to Http10.test_streaming in sockjs-protocol-0.3.3.py.
     */
    @Test
    public void http10TestStreaming() throws Exception {
        final ChannelHandler closeHandler = new SockJsCloseHandler();
        final String sessionUrl = "/close/222/" + UUID.randomUUID();
        final EmbeddedChannel ch = sockJsPipeline("/close", closeHandler);
        final FullHttpRequest request = httpPostRequest(sessionUrl + TransportType.XHR_STREAMING.path(), HTTP_1_0);
        request.headers().set(CONNECTION, KEEP_ALIVE);
        ch.writeInbound(request);

        final HttpResponse response =  ch.readOutbound();
        assertThat(response.getStatus(), equalTo(HttpResponseStatus.OK));
        assertThat(response.getProtocolVersion(), is(HTTP_1_0));
        assertThat(response.headers().get(TRANSFER_ENCODING), is(nullValue()));
        assertThat(response.headers().get(CONTENT_LENGTH), is(nullValue()));

        final HttpContent httpContent = ch.readOutbound();
        assertThat(httpContent.content().readableBytes(), is(PreludeFrame.CONTENT_SIZE + 1));
        assertThat(getContent(httpContent.content()), equalTo(expectedContent(PreludeFrame.CONTENT_SIZE)));
        httpContent.release();

        final HttpContent open = ch.readOutbound();
        assertThat(open.content().toString(UTF_8), equalTo("o\n"));
        open.release();

        final HttpContent goAway = ch.readOutbound();
        assertThat(goAway.content().toString(UTF_8), equalTo("c[3000,\"Go away!\"]\n"));
        goAway.release();

        final HttpContent lastChunk = ch.readOutbound();
        assertThat(lastChunk.content().toString(UTF_8), equalTo(""));
        lastChunk.release();
    }

    /*
     * Equivalent to Http11.test_synchronous in sockjs-protocol-0.3.3.py.
     */
    @Test
    public void http11TestSynchronous() throws Exception {
        final ChannelHandler echoHandler = new SockJsEchoHandler();

        final EmbeddedChannel ch = sockJsPipeline("/echo", echoHandler);
        final FullHttpRequest request = httpGetRequest("/echo", HTTP_1_1);
        request.headers().set(CONNECTION, KEEP_ALIVE);
        ch.writeInbound(request);

        final FullHttpResponse response = ch.readOutbound();
        assertThat(response.getStatus(), is(HttpResponseStatus.OK));
        assertThat(response.getProtocolVersion(), is(HTTP_1_1));
        String connectionHeader = response.headers().get(CONNECTION);
        if (connectionHeader != null) {
            assertThat(connectionHeader, equalTo("keep-alive"));
        }

        if (response.headers().get(CONTENT_LENGTH) != null) {
            assertThat(response.headers().get(CONTENT_LENGTH), is("19"));
            assertThat(response.content().toString(UTF_8), equalTo("Welcome to SockJS!\n"));
            assertThat(response.headers().get(TRANSFER_ENCODING), is(nullValue()));
        } else {
            assertThat(response.headers().get(TRANSFER_ENCODING), is("chunked"));
            assertThat(response.content().toString(UTF_8), equalTo("Welcome to SockJS!\n"));
        }
        response.release();

        ch.writeInbound(httpGetRequest("/echo", HTTP_1_0));
        final FullHttpResponse newResponse = ch.readOutbound();
        assertThat(newResponse.getStatus(), is(HttpResponseStatus.OK));
        newResponse.release();
    }

    /*
     * Equivalent to Http11.test_streaming in sockjs-protocol-0.3.3.py.
     */
    @Test
    public void http11TestStreaming() throws Exception {
        final ChannelHandler closeHandler = new SockJsCloseHandler();
        final String sessionUrl = "/close/222/" + UUID.randomUUID();
        final EmbeddedChannel ch = sockJsPipeline("/close", closeHandler);
        final FullHttpRequest request = httpPostRequest(sessionUrl + TransportType.XHR_STREAMING.path(), HTTP_1_1);
        request.headers().set(CONNECTION, KEEP_ALIVE);
        ch.writeInbound(request);

        final HttpResponse response =  ch.readOutbound();
        assertThat(response.getStatus(), equalTo(HttpResponseStatus.OK));
        assertThat(response.getProtocolVersion(), is(HTTP_1_1));
        assertThat(response.headers().get(TRANSFER_ENCODING), equalTo("chunked"));
        assertThat(response.headers().get(CONTENT_LENGTH), is(nullValue()));

        final HttpContent httpContent = ch.readOutbound();
        assertThat(httpContent.content().readableBytes(), is(PreludeFrame.CONTENT_SIZE + 1));
        assertThat(getContent(httpContent.content()), equalTo(expectedContent(PreludeFrame.CONTENT_SIZE)));
        httpContent.release();

        final HttpContent open = ch.readOutbound();
        assertThat(open.content().toString(UTF_8), equalTo("o\n"));
        open.release();

        final HttpContent goAway = ch.readOutbound();
        assertThat(goAway.content().toString(UTF_8), equalTo("c[3000,\"Go away!\"]\n"));
        goAway.release();

        final HttpContent lastChunk = ch.readOutbound();
        assertThat(lastChunk.content().toString(UTF_8), equalTo(""));
        lastChunk.release();
    }

    @Test
    public void prefixNotFound() throws Exception {
        final ChannelHandler echoHandler = new SockJsEchoHandler();
        final EmbeddedChannel ch = sockJsPipeline("/echo", echoHandler);
        ch.config().setOption(SockJsChannelOption.COOKIES_NEEDED, true);
        ch.writeInbound(httpRequest("/missing"));
        final FullHttpResponse response = ch.readOutbound();
        assertThat(response.getStatus().code(), is(HttpResponseStatus.NOT_FOUND.code()));
        response.release();
    }

    private static void assertGoAwayResponse(final FullHttpResponse response) {
        assertThat(response.getStatus(), is(HttpResponseStatus.OK));
        assertThat(response.content().toString(UTF_8), equalTo("c[3000,\"Go away!\"]\n"));
    }

    private static void assertNoContent(final FullHttpResponse response) {
        assertThat(response.getStatus(), is(HttpResponseStatus.NO_CONTENT));
        assertThat(response.content().isReadable(), is(false));
    }

    private static void assertMessageFrameContent(final FullHttpResponse response, final String expected) {
        assertThat(response.getStatus(), is(HttpResponseStatus.OK));
        assertThat(response.content().toString(UTF_8), equalTo("a[\"" + expected + "\"]\n"));
    }

    private static byte[] getContent(final ByteBuf buf) {
        final byte[] actualContent = new byte[PreludeFrame.CONTENT_SIZE + 1];
        buf.readBytes(actualContent);
        return actualContent;
    }

    private static byte[] expectedContent(final int size) {
        final byte[] content = new byte[size + 1];
        for (int i = 0; i < content.length; i++) {
            content[i] = 'h';
        }
        content[size] = '\n';
        return content;
    }

    private static String clientKillerStringEsc() {
        return "\\u0000\\u0001\\u0002\\u0003\\u0004\\u0005\\u0006\\u0007\\u0008\\u0009\\u000a\\u000b\\u000c\\u000d" +
               "\\u000e\\u000f\\u0010\\u0011\\u0012\\u0013\\u0014\\u0015\\u0016\\u0017\\u0018\\u0019\\u001a\\u001b" +
               "\\u001c\\u001d\\u001e\\u001f\\u0022\\u007f\\u0080\\u0081\\u0082\\u0083\\u0084\\u0085\\u0086\\u0087" +
               "\\u0088\\u0089\\u008a\\u008b\\u008c\\u008d\\u008e\\u008f\\u0090\\u0091\\u0092\\u0093\\u0094\\u0095" +
               "\\u0096\\u0097\\u0098\\u0099\\u009a\\u009b\\u009c\\u009d\\u009e\\u009f\\u00ad\\u0300\\u0301\\u0302" +
               "\\u0303\\u0304\\u0305\\u0306\\u0307\\u0308\\u0309\\u030a\\u030b\\u030c\\u030d\\u030e\\u030f\\u0310" +
               "\\u0311\\u0312\\u0313\\u0314\\u0315\\u0316\\u0317\\u0318\\u0319\\u031a\\u031b\\u031c\\u031d\\u031e" +
               "\\u031f\\u0320\\u0321\\u0322\\u0323\\u0324\\u0325\\u0326\\u0327\\u0328\\u0329\\u032a\\u032b\\u032c" +
               "\\u032d\\u032e\\u032f\\u0330\\u0331\\u0332\\u0333\\u033d\\u033e\\u033f\\u0340\\u0341\\u0342\\u0343" +
               "\\u0344\\u0345\\u0346\\u034a\\u034b\\u034c\\u0350\\u0351\\u0352\\u0357\\u0358\\u035c\\u035d\\u035e" +
               "\\u035f\\u0360\\u0361\\u0362\\u0374\\u037e\\u0387\\u0591\\u0592\\u0593\\u0594\\u0595\\u0596\\u0597" +
               "\\u0598\\u0599\\u059a\\u059b\\u059c\\u059d\\u059e\\u059f\\u05a0\\u05a1\\u05a2\\u05a3\\u05a4\\u05a5" +
               "\\u05a6\\u05a7\\u05a8\\u05a9\\u05aa\\u05ab\\u05ac\\u05ad\\u05ae\\u05af\\u05c4\\u0600\\u0601\\u0602" +
               "\\u0603\\u0604\\u0610\\u0611\\u0612\\u0613\\u0614\\u0615\\u0616\\u0617\\u0653\\u0654\\u0657\\u0658" +
               "\\u0659\\u065a\\u065b\\u065d\\u065e\\u06df\\u06e0\\u06e1\\u06e2\\u06eb\\u06ec\\u070f\\u0730\\u0732" +
               "\\u0733\\u0735\\u0736\\u073a\\u073d\\u073f\\u0740\\u0741\\u0743\\u0745\\u0747\\u07eb\\u07ec\\u07ed" +
               "\\u07ee\\u07ef\\u07f0\\u07f1\\u0951\\u0958\\u0959\\u095a\\u095b\\u095c\\u095d\\u095e\\u095f\\u09dc" +
               "\\u09dd\\u09df\\u0a33\\u0a36\\u0a59\\u0a5a\\u0a5b\\u0a5e\\u0b5c\\u0b5d\\u0e38\\u0e39\\u0f43\\u0f4d" +
               "\\u0f52\\u0f57\\u0f5c\\u0f69\\u0f72\\u0f73\\u0f74\\u0f75\\u0f76\\u0f78\\u0f80\\u0f81\\u0f82\\u0f83" +
               "\\u0f93\\u0f9d\\u0fa2\\u0fa7\\u0fac\\u0fb9\\u17b4\\u17b5\\u1939\\u193a\\u1a17\\u1b6b\\u1cda\\u1cdb" +
               "\\u1dc0\\u1dc1\\u1dc2\\u1dc3\\u1dc4\\u1dc5\\u1dc6\\u1dc7\\u1dc8\\u1dc9\\u1dca\\u1dcb\\u1dcc\\u1dcd" +
               "\\u1dce\\u1dcf\\u1dfc\\u1dfe\\u1f71\\u1f73\\u1f75\\u1f77\\u1f79\\u1f7b\\u1f7d\\u1fbb\\u1fbe\\u1fc9" +
               "\\u1fcb\\u1fd3\\u1fdb\\u1fe3\\u1feb\\u1fee\\u1fef\\u1ff9\\u1ffb\\u1ffd\\u2000\\u2001\\u2002\\u2003" +
               "\\u2004\\u2005\\u2006\\u2007\\u2008\\u2009\\u200a\\u200b\\u200c\\u200d\\u200e\\u200f\\u2010\\u2011" +
               "\\u2012\\u2013\\u2014\\u2015\\u2016\\u2017\\u2018\\u2019\\u201a\\u201b\\u201c\\u201d\\u201e\\u201f" +
               "\\u2020\\u2021\\u2022\\u2023\\u2024\\u2025\\u2026\\u2027\\u2028\\u2029\\u202a\\u202b\\u202c\\u202d" +
               "\\u202e\\u202f\\u2030\\u2031\\u2032\\u2033\\u2034\\u2035\\u2036\\u2037\\u2038\\u2039\\u203a\\u203b" +
               "\\u203c\\u203d\\u203e\\u203f\\u2040\\u2041\\u2042\\u2043\\u2044\\u2045\\u2046\\u2047\\u2048\\u2049" +
               "\\u204a\\u204b\\u204c\\u204d\\u204e\\u204f\\u2050\\u2051\\u2052\\u2053\\u2054\\u2055\\u2056\\u2057" +
               "\\u2058\\u2059\\u205a\\u205b\\u205c\\u205d\\u205e\\u205f\\u2060\\u2061\\u2062\\u2063\\u2064\\u2065" +
               "\\u2066\\u2067\\u2068\\u2069\\u206a\\u206b\\u206c\\u206d\\u206e\\u206f\\u2070\\u2071\\u2072\\u2073" +
               "\\u2074\\u2075\\u2076\\u2077\\u2078\\u2079\\u207a\\u207b\\u207c\\u207d\\u207e\\u207f\\u2080\\u2081" +
               "\\u2082\\u2083\\u2084\\u2085\\u2086\\u2087\\u2088\\u2089\\u208a\\u208b\\u208c\\u208d\\u208e\\u208f" +
               "\\u2090\\u2091\\u2092\\u2093\\u2094\\u2095\\u2096\\u2097\\u2098\\u2099\\u209a\\u209b\\u209c\\u209d" +
               "\\u209e\\u209f\\u20a0\\u20a1\\u20a2\\u20a3\\u20a4\\u20a5\\u20a6\\u20a7\\u20a8\\u20a9\\u20aa\\u20ab" +
               "\\u20ac\\u20ad\\u20ae\\u20af\\u20b0\\u20b1\\u20b2\\u20b3\\u20b4\\u20b5\\u20b6\\u20b7\\u20b8\\u20b9" +
               "\\u20ba\\u20bb\\u20bc\\u20bd\\u20be\\u20bf\\u20c0\\u20c1\\u20c2\\u20c3\\u20c4\\u20c5\\u20c6\\u20c7" +
               "\\u20c8\\u20c9\\u20ca\\u20cb\\u20cc\\u20cd\\u20ce\\u20cf\\u20d0\\u20d1\\u20d2\\u20d3\\u20d4\\u20d5" +
               "\\u20d6\\u20d7\\u20d8\\u20d9\\u20da\\u20db\\u20dc\\u20dd\\u20de\\u20df\\u20e0\\u20e1\\u20e2\\u20e3" +
               "\\u20e4\\u20e5\\u20e6\\u20e7\\u20e8\\u20e9\\u20ea\\u20eb\\u20ec\\u20ed\\u20ee\\u20ef\\u20f0\\u20f1" +
               "\\u20f2\\u20f3\\u20f4\\u20f5\\u20f6\\u20f7\\u20f8\\u20f9\\u20fa\\u20fb\\u20fc\\u20fd\\u20fe\\u20ff" +
               "\\u2126\\u212a\\u212b\\u2329\\u232a\\u2adc\\u302b\\u302c\\uaab2\\uaab3\\uf900\\uf901\\uf902\\uf903" +
               "\\uf904\\uf905\\uf906\\uf907\\uf908\\uf909\\uf90a\\uf90b\\uf90c\\uf90d\\uf90e\\uf90f\\uf910\\uf911" +
               "\\uf912\\uf913\\uf914\\uf915\\uf916\\uf917\\uf918\\uf919\\uf91a\\uf91b\\uf91c\\uf91d\\uf91e\\uf91f" +
               "\\uf920\\uf921\\uf922\\uf923\\uf924\\uf925\\uf926\\uf927\\uf928\\uf929\\uf92a\\uf92b\\uf92c\\uf92d" +
               "\\uf92e\\uf92f\\uf930\\uf931\\uf932\\uf933\\uf934\\uf935\\uf936\\uf937\\uf938\\uf939\\uf93a\\uf93b" +
               "\\uf93c\\uf93d\\uf93e\\uf93f\\uf940\\uf941\\uf942\\uf943\\uf944\\uf945\\uf946\\uf947\\uf948\\uf949" +
               "\\uf94a\\uf94b\\uf94c\\uf94d\\uf94e\\uf94f\\uf950\\uf951\\uf952\\uf953\\uf954\\uf955\\uf956\\uf957" +
               "\\uf958\\uf959\\uf95a\\uf95b\\uf95c\\uf95d\\uf95e\\uf95f\\uf960\\uf961\\uf962\\uf963\\uf964\\uf965" +
               "\\uf966\\uf967\\uf968\\uf969\\uf96a\\uf96b\\uf96c\\uf96d\\uf96e\\uf96f\\uf970\\uf971\\uf972\\uf973" +
               "\\uf974\\uf975\\uf976\\uf977\\uf978\\uf979\\uf97a\\uf97b\\uf97c\\uf97d\\uf97e\\uf97f\\uf980\\uf981" +
               "\\uf982\\uf983\\uf984\\uf985\\uf986\\uf987\\uf988\\uf989\\uf98a\\uf98b\\uf98c\\uf98d\\uf98e\\uf98f" +
               "\\uf990\\uf991\\uf992\\uf993\\uf994\\uf995\\uf996\\uf997\\uf998\\uf999\\uf99a\\uf99b\\uf99c\\uf99d" +
               "\\uf99e\\uf99f\\uf9a0\\uf9a1\\uf9a2\\uf9a3\\uf9a4\\uf9a5\\uf9a6\\uf9a7\\uf9a8\\uf9a9\\uf9aa\\uf9ab" +
               "\\uf9ac\\uf9ad\\uf9ae\\uf9af\\uf9b0\\uf9b1\\uf9b2\\uf9b3\\uf9b4\\uf9b5\\uf9b6\\uf9b7\\uf9b8\\uf9b9" +
               "\\uf9ba\\uf9bb\\uf9bc\\uf9bd\\uf9be\\uf9bf\\uf9c0\\uf9c1\\uf9c2\\uf9c3\\uf9c4\\uf9c5\\uf9c6\\uf9c7" +
               "\\uf9c8\\uf9c9\\uf9ca\\uf9cb\\uf9cc\\uf9cd\\uf9ce\\uf9cf\\uf9d0\\uf9d1\\uf9d2\\uf9d3\\uf9d4\\uf9d5" +
               "\\uf9d6\\uf9d7\\uf9d8\\uf9d9\\uf9da\\uf9db\\uf9dc\\uf9dd\\uf9de\\uf9df\\uf9e0\\uf9e1\\uf9e2\\uf9e3" +
               "\\uf9e4\\uf9e5\\uf9e6\\uf9e7\\uf9e8\\uf9e9\\uf9ea\\uf9eb\\uf9ec\\uf9ed\\uf9ee\\uf9ef\\uf9f0\\uf9f1" +
               "\\uf9f2\\uf9f3\\uf9f4\\uf9f5\\uf9f6\\uf9f7\\uf9f8\\uf9f9\\uf9fa\\uf9fb\\uf9fc\\uf9fd\\uf9fe\\uf9ff" +
               "\\ufa00\\ufa01\\ufa02\\ufa03\\ufa04\\ufa05\\ufa06\\ufa07\\ufa08\\ufa09\\ufa0a\\ufa0b\\ufa0c\\ufa0d" +
               "\\ufa10\\ufa12\\ufa15\\ufa16\\ufa17\\ufa18\\ufa19\\ufa1a\\ufa1b\\ufa1c\\ufa1d\\ufa1e\\ufa20\\ufa22" +
               "\\ufa25\\ufa26\\ufa2a\\ufa2b\\ufa2c\\ufa2d\\ufa30\\ufa31\\ufa32\\ufa33\\ufa34\\ufa35\\ufa36\\ufa37" +
               "\\ufa38\\ufa39\\ufa3a\\ufa3b\\ufa3c\\ufa3d\\ufa3e\\ufa3f\\ufa40\\ufa41\\ufa42\\ufa43\\ufa44\\ufa45" +
               "\\ufa46\\ufa47\\ufa48\\ufa49\\ufa4a\\ufa4b\\ufa4c\\ufa4d\\ufa4e\\ufa4f\\ufa50\\ufa51\\ufa52\\ufa53" +
               "\\ufa54\\ufa55\\ufa56\\ufa57\\ufa58\\ufa59\\ufa5a\\ufa5b\\ufa5c\\ufa5d\\ufa5e\\ufa5f\\ufa60\\ufa61" +
               "\\ufa62\\ufa63\\ufa64\\ufa65\\ufa66\\ufa67\\ufa68\\ufa69\\ufa6a\\ufa6b\\ufa6c\\ufa6d\\ufa70\\ufa71" +
               "\\ufa72\\ufa73\\ufa74\\ufa75\\ufa76\\ufa77\\ufa78\\ufa79\\ufa7a\\ufa7b\\ufa7c\\ufa7d\\ufa7e\\ufa7f" +
               "\\ufa80\\ufa81\\ufa82\\ufa83\\ufa84\\ufa85\\ufa86\\ufa87\\ufa88\\ufa89\\ufa8a\\ufa8b\\ufa8c\\ufa8d" +
               "\\ufa8e\\ufa8f\\ufa90\\ufa91\\ufa92\\ufa93\\ufa94\\ufa95\\ufa96\\ufa97\\ufa98\\ufa99\\ufa9a\\ufa9b" +
               "\\ufa9c\\ufa9d\\ufa9e\\ufa9f\\ufaa0\\ufaa1\\ufaa2\\ufaa3\\ufaa4\\ufaa5\\ufaa6\\ufaa7\\ufaa8\\ufaa9" +
               "\\ufaaa\\ufaab\\ufaac\\ufaad\\ufaae\\ufaaf\\ufab0\\ufab1\\ufab2\\ufab3\\ufab4\\ufab5\\ufab6\\ufab7" +
               "\\ufab8\\ufab9\\ufaba\\ufabb\\ufabc\\ufabd\\ufabe\\ufabf\\ufac0\\ufac1\\ufac2\\ufac3\\ufac4\\ufac5" +
               "\\ufac6\\ufac7\\ufac8\\ufac9\\ufaca\\ufacb\\ufacc\\ufacd\\uface\\ufacf\\ufad0\\ufad1\\ufad2\\ufad3" +
               "\\ufad4\\ufad5\\ufad6\\ufad7\\ufad8\\ufad9\\ufb1d\\ufb1f\\ufb2a\\ufb2b\\ufb2c\\ufb2d\\ufb2e\\ufb2f" +
               "\\ufb30\\ufb31\\ufb32\\ufb33\\ufb34\\ufb35\\ufb36\\ufb38\\ufb39\\ufb3a\\ufb3b\\ufb3c\\ufb3e\\ufb40" +
               "\\ufb41\\ufb43\\ufb44\\ufb46\\ufb47\\ufb48\\ufb49\\ufb4a\\ufb4b\\ufb4c\\ufb4d\\ufb4e\\ufeff\\ufff0" +
               "\\ufff1\\ufff2\\ufff3\\ufff4\\ufff5\\ufff6\\ufff7\\ufff8\\ufff9\\ufffa\\ufffb\\ufffc\\ufffd\\ufffe" +
               "\\uffff";
    }

    private static String serverKillerStringEsc() {
        return "\\u200c\\u200d\\u200e\\u200f\\u2028\\u2029\\u202a\\u202b\\u202c\\u202d\\u202e\\u202f\\u2060\\u2061" +
               "\\u2062\\u2063\\u2064\\u2065\\u2066\\u2067\\u2068\\u2069\\u206a\\u206b\\u206c\\u206d\\u206e\\u206f" +
               "\\ufff0\\ufff1\\ufff2\\ufff3\\ufff4\\ufff5\\ufff6\\ufff7\\ufff8\\ufff9\\ufffa\\ufffb\\ufffc\\ufffd" +
               "\\ufffe\\uffff";
    }

    private static void assertSetCookie(final String transportPath) {
        final ChannelHandler echoHandler = new SockJsEchoHandler();
        final String serviceName = "/echo";
        final String sessionUrl = serviceName + "/abc/" + UUID.randomUUID();
        final EmbeddedChannel ch = sockJsPipeline("/echo", echoHandler);
        ch.config().setOption(SockJsChannelOption.COOKIES_NEEDED, true);
        final FullHttpRequest request = httpRequest(sessionUrl + transportPath, GET);
        ch.writeInbound(request);

        final HttpResponse response = ch.readOutbound();
        assertThat(response.getStatus(), is(HttpResponseStatus.OK));
        assertSetCookie("dummy", response);
        if (response instanceof FullHttpResponse) {
            ((FullHttpResponse) response).release();
        }
    }

    private static void assertSetCookie(final String sessionId, final HttpResponse response) {
        final String setCookie = response.headers().get(SET_COOKIE);
        final String[] split = SEMICOLON.split(setCookie);
        assertThat(split[0], equalTo("JSESSIONID=" + sessionId));
        assertThat(split[1].trim().toLowerCase(), equalTo("path=/"));
    }

    private static final ObjectMapper OBJECT_MAPPER = new ObjectMapper();

    private static JsonNode infoAsJson(final FullHttpResponse response) throws Exception {
        return OBJECT_MAPPER.readTree(response.content().toString(UTF_8));
    }

    private static void assertOpenFrameResponse(final FullHttpResponse response) {
        assertThat(response.getStatus(), is(HttpResponseStatus.OK));
        assertThat(response.content().toString(UTF_8), equalTo("o\n"));
        response.release();
    }

    private static void verifyHeaders(final WebSocketVersion version) throws Exception {
        final EmbeddedChannel ch = wsSockJsPipeline("/echo", new SockJsEchoHandler());
        final FullHttpRequest request = HttpUtil.webSocketUpgradeRequest("/echo/websocket", version);
        ch.writeInbound(request);
        final HttpResponse response = HttpUtil.decode(ch);
        assertThat(response.getStatus(), is(HttpResponseStatus.SWITCHING_PROTOCOLS));
        assertThat(response.headers().get(CONNECTION), equalTo("Upgrade"));
        assertThat(response.headers().get(UPGRADE), equalTo("websocket"));
        assertThat(response.headers().get(CONTENT_LENGTH), is(nullValue()));
    }

    private static FullHttpRequest webSocketUpgradeRequest(final String path, final WebSocketVersion version) {
        final FullHttpRequest req = new DefaultFullHttpRequest(HTTP_1_1, GET, path);
        req.headers().set(HOST, "server.test.com");
        req.headers().set(UPGRADE, WEBSOCKET.toString());
        req.headers().set(CONNECTION, "Upgrade");

        if (version == V00) {
            req.headers().set(CONNECTION, "Upgrade");
            req.headers().set(SEC_WEBSOCKET_KEY1, "4 @1  46546xW%0l 1 5");
            req.headers().set(SEC_WEBSOCKET_KEY2, "12998 5 Y3 1  .P00");
            req.headers().set(ORIGIN, "http://example.com");
            final ByteBuf byteBuf = Unpooled.copiedBuffer("^n:ds[4U", US_ASCII);
            req.content().writeBytes(byteBuf);
            byteBuf.release();
        } else {
            req.headers().set(SEC_WEBSOCKET_KEY, "dGhlIHNhbXBsZSBub25jZQ==");
            req.headers().set(SEC_WEBSOCKET_ORIGIN, "http://test.com");
            req.headers().set(SEC_WEBSOCKET_VERSION, version.toHttpHeaderValue());
        }
        return req;
    }

    private static void webSocketTestTransport(final WebSocketVersion version) {
        final ChannelHandler echoHandler = new SockJsEchoHandler();
        final String sessionUrl = "/echo/222/" + UUID.randomUUID();
        final EmbeddedChannel ch = wsSockJsPipeline("/echo", echoHandler);

        final FullHttpRequest request = webSocketUpgradeRequest(sessionUrl + "/websocket", version);
        ch.writeInbound(request);
        // Discard the HTTP Response (this will be a ByteBuf and not an object
        // as we have a HttpEncoder is in the pipeline to start with.
        ReferenceCountUtil.release(ch.readOutbound());

        final TextWebSocketFrame openFrame = ch.readOutbound();
        assertThat(openFrame.content().toString(UTF_8), equalTo("o"));
        openFrame.release();

        final TextWebSocketFrame textWebSocketFrame = new TextWebSocketFrame("\"a\"");
        ch.writeInbound(textWebSocketFrame);

        final TextWebSocketFrame textFrame = ch.readOutbound();
        assertThat(textFrame.content().toString(UTF_8), equalTo("a[\"a\"]"));
        textFrame.release();
    }

    private static void webSocketTestClose(final WebSocketVersion version) {
        final ChannelHandler closeHandler = new SockJsCloseHandler();
        final String sessionUrl = "/close/222/" + UUID.randomUUID();
        final EmbeddedChannel ch = wsSockJsPipeline("/close", closeHandler);

        final FullHttpRequest request = webSocketUpgradeRequest(sessionUrl + "/websocket", version.toHttpHeaderValue());
        ch.writeInbound(request);

        // read and discard the HTTP Response (this will be a ByteBuf and not an object
        // as we have a HttpEncoder in the pipeline to start with.
        ReferenceCountUtil.release(ch.readOutbound());

        final TextWebSocketFrame openFrame = (TextWebSocketFrame) readOutboundDiscardEmpty(ch);
        assertThat(openFrame.content().toString(UTF_8), equalTo("o"));
        openFrame.release();

        final TextWebSocketFrame closeFrame = ch.readOutbound();
        assertThat(closeFrame.content().toString(UTF_8), equalTo("c[3000,\"Go away!\"]"));
        assertThat(ch.isActive(), is(false));
        closeFrame.release();
    }

    private static void webSocketTestBrokenJSON(final WebSocketVersion version) throws Exception {
        final ChannelHandler closeHandler = new SockJsCloseHandler();
        final String sessionUrl = "/close/222/" + UUID.randomUUID();
        final EmbeddedChannel ch = wsSockJsPipeline("/close", closeHandler);

        final FullHttpRequest request = webSocketUpgradeRequest(sessionUrl + "/websocket", version.toHttpHeaderValue());
        ch.writeInbound(request);

        // read and discard the HTTP Response (this will be a ByteBuf and not an object
        // as we have a HttpEncoder in the pipeline to start with.
        ReferenceCountUtil.release(ch.readOutbound());

        final TextWebSocketFrame openFrame = ch.readOutbound();
        assertThat(openFrame.text(), equalTo("o"));
        openFrame.release();

        final TextWebSocketFrame closeFrame = ch.readOutbound();
        assertThat(closeFrame.text(), equalTo("c[3000,\"Go away!\"]"));
        closeFrame.release();
        assertThat(ch.isActive(), is(false));
    }

    private static FullHttpRequest webSocketUpgradeRequest(final String path) {
        return webSocketUpgradeRequest(path, "13");
    }

    private static FullHttpRequest webSocketUpgradeRequest(final String path, final String version) {
        final FullHttpRequest req = new DefaultFullHttpRequest(HTTP_1_1, GET, path);
        req.headers().set(HOST, "server.test.com");
        req.headers().set(UPGRADE, WEBSOCKET.toString());
        req.headers().set(CONNECTION, "Upgrade");
        req.headers().set(SEC_WEBSOCKET_KEY, "dGhlIHNhbXBsZSBub25jZQ==");
        req.headers().set(SEC_WEBSOCKET_ORIGIN, "http://test.com");
        req.headers().set(SEC_WEBSOCKET_VERSION, version);
        return req;
    }

    private static String generateMessage(final int characters) {
        final StringBuilder sb = new StringBuilder();
        for (int i = 0; i < characters; i++) {
            sb.append('x');
        }
        return sb.toString();
    }

    private static void assertBrokenJSONEncoding(final FullHttpResponse response) {
        assertThat(response.getStatus(), is(HttpResponseStatus.INTERNAL_SERVER_ERROR));
        assertThat(response.content().toString(UTF_8), equalTo("Broken JSON encoding."));
        response.release();
    }

    private static void assertPayloadExpected(final FullHttpResponse response) {
        assertThat(response.getStatus(), is(HttpResponseStatus.INTERNAL_SERVER_ERROR));
        assertThat(response.content().toString(UTF_8), equalTo("Payload expected."));
        response.release();
    }

    private static HttpResponse jsonpRequest(final String url, final EmbeddedChannel ch) {
        final HttpRequest request = httpRequest(url, GET);
        ch.writeInbound(request);
        final HttpResponse response = ch.readOutbound();
        ch.finish();
        return response;
    }

    private static FullHttpResponse jsonpSend(final FullHttpRequest request, final EmbeddedChannel ch) {
        ch.writeInbound(request);
        Object out;
        try {
            while ((out = ch.readOutbound()) != null) {
                if (out instanceof FullHttpResponse) {
                    return (FullHttpResponse) out;
                }
            }
        } finally {
            ch.finish();
        }
        throw new IllegalStateException("No outbound FullHttpResponse was written");
    }

    private static FullHttpResponse jsonpSend(final String url, final String content,
                                              final EmbeddedChannel ch) {
        final FullHttpRequest request = httpRequest(url, POST);
        request.headers().set(CONTENT_TYPE, HttpResponseBuilder.CONTENT_TYPE_FORM);
        final ByteBuf buf = Unpooled.copiedBuffer(content, UTF_8);
        request.content().writeBytes(buf);
        buf.release();
        return jsonpSend(request, ch);
    }

    private static FullHttpResponse xhrSendRequest(final String path,
                                                   final String content,
                                                   final EmbeddedChannel ch) {
        final FullHttpRequest sendRequest = httpRequest(path + "/xhr_send", POST);
        final ByteBuf byteBuf = Unpooled.copiedBuffer(content, UTF_8);
        sendRequest.content().writeBytes(byteBuf);
        byteBuf.release();
        ch.writeInbound(sendRequest);
        final FullHttpResponse response = ch.readOutbound();
        ch.finish();
        return response;
    }

    private static FullHttpResponse xhrSendRequest(final String path,
                                                   final String content,
                                                   final String contentType,
                                                   final EmbeddedChannel ch) {
        final FullHttpRequest request = httpRequest(path + TransportType.XHR_SEND.path(), POST);
        request.headers().set(CONTENT_TYPE, contentType);
        final ByteBuf byteBuf = Unpooled.copiedBuffer(content, UTF_8);
        request.content().writeBytes(byteBuf);
        byteBuf.release();
        ch.writeInbound(request);
        Object out;
        try {
            while ((out = ch.readOutbound()) != null) {
                if (out instanceof FullHttpResponse) {
                    return (FullHttpResponse) out;
                }
            }
        } finally {
            ch.finish();
        }
        throw new IllegalStateException("No outbound FullHttpResponse was written");
    }

    private static FullHttpResponse xhrRequest(final String url, EmbeddedChannel ch) {
        final FullHttpRequest request = httpRequest(url + TransportType.XHR.path(), GET);
        ch.writeInbound(request);
        final FullHttpResponse response = ch.readOutbound();
        ch.finish();
        return response;
    }

    private static FullHttpResponse infoRequest(final EmbeddedChannel ch, final String url) {
        final FullHttpRequest request = httpRequest(url + "/info", GET);
        ch.writeInbound(request);
        final FullHttpResponse response = ch.readOutbound();
        ch.finish();
        return response;
    }

    private static HttpResponse xhrRequest(final FullHttpRequest request,
                                           final EmbeddedChannel ch) {
        ch.writeInbound(request);
        final HttpResponse response = ch.readOutbound();
        ch.finish();
        return response;
    }

    private static void assertOKResponse(final String sessionPart) {
        final ChannelHandler echoHandler = new SockJsEchoHandler();
        final EmbeddedChannel ch = sockJsPipeline("/echo", echoHandler);
        ch.config().setOption(SockJsChannelOption.COOKIES_NEEDED, true);

        ch.writeInbound(httpRequest("/echo" + sessionPart + TransportType.XHR.path()));
        final FullHttpResponse response = ch.readOutbound();
        assertThat(response.getStatus(), is(HttpResponseStatus.OK));
        assertThat(response.content().toString(UTF_8), equalTo("o\n"));
        response.release();
    }

    private static void assertCORSPreflightResponseHeaders(final HttpResponse response, HttpMethod... methods) {
        final HttpHeaders headers = response.headers();
        assertThat(headers.get(CONTENT_TYPE), is("text/plain; charset=UTF-8"));
        assertThat(headers.get(CACHE_CONTROL), containsString("public"));
        assertThat(headers.get(CACHE_CONTROL), containsString("max-age=31536000"));
        assertThat(headers.get(ACCESS_CONTROL_ALLOW_CREDENTIALS), is("true"));
        assertThat(headers.get(ACCESS_CONTROL_MAX_AGE), is("31536000"));
        for (HttpMethod method : methods) {
            assertThat(headers.get(ACCESS_CONTROL_ALLOW_METHODS), containsString(method.toString()));
        }
        assertThat(headers.get(ACCESS_CONTROL_ALLOW_HEADERS), is("Content-Type"));
        assertThat(headers.get(ACCESS_CONTROL_ALLOW_CREDENTIALS), is("true"));
        // We are not using a HttpObjectEncoder hence the the Date is not onverted.
        // to the correct char sequence.
        assertThat(headers.get(EXPIRES), is(notNullValue()));
        assertThat(headers.get(SET_COOKIE), is("JSESSIONID=dummy;path=/"));
    }

    private static void verifyIframe(final String path) {
        final EmbeddedChannel ch = sockJsPipeline("/echo", new SockJsEchoHandler());
        final SockJsChannelConfig config = (SockJsChannelConfig) ch.config();
        ch.config().setOption(SockJsChannelOption.PREFIX, "/echo");
        ch.config().setOption(SockJsChannelOption.COOKIES_NEEDED, true);
        ch.writeInbound(httpRequest("/echo" + path));

        final FullHttpResponse response = ch.readOutbound();
        assertThat(response.getStatus().code(), is(HttpResponseStatus.OK.code()));
        assertThat(response.headers().get(CONTENT_TYPE), equalTo("text/html; charset=UTF-8"));
        assertThat(response.headers().get(CACHE_CONTROL), equalTo("max-age=31536000, public"));
        assertThat(response.headers().get(EXPIRES), is(notNullValue()));
        verifyNoSET_COOKIE(response);
        assertThat(response.headers().get(ETAG), is(notNullValue()));
        assertThat(response.content().toString(UTF_8), equalTo(iframeHtml(config.sockJsUrl())));
        response.release();
    }

    private static void verifyContentType(final HttpResponse response, final String contentType) {
        assertThat(response.headers().get(CONTENT_TYPE), equalTo(contentType));
    }

    private static void verifyNoSET_COOKIE(final HttpResponse response) {
        assertThat(response.headers().get(SET_COOKIE), is(nullValue()));
    }

    private static void verifyNotCached(final HttpResponse response) {
        assertThat(response.headers().get(CACHE_CONTROL), containsString("no-store"));
        assertThat(response.headers().get(CACHE_CONTROL), containsString("no-cache"));
        assertThat(response.headers().get(CACHE_CONTROL), containsString("must-revalidate"));
        assertThat(response.headers().get(CACHE_CONTROL), containsString("max-age=0"));
    }

    private static long getEntropy(final FullHttpResponse response) throws Exception {
        return contentAsJson(response).get("entropy").asLong();
    }

    private static void assertNotFoundResponse(final String prefix, final String path) {
        final TestEmbeddedChannel ch = sockJsPipeline(prefix, new SockJsEchoHandler());
        ch.config().setOption(SockJsChannelOption.PREFIX, prefix);
        ch.config().setOption(SockJsChannelOption.COOKIES_NEEDED, true);
        ch.writeInbound(httpRequest('/' + prefix + path));
        final FullHttpResponse response = ch.readOutbound();
        assertThat(response.getStatus(), is(HttpResponseStatus.NOT_FOUND));
        response.release();
    }

    private static String getEtag(final FullHttpResponse response) {
        return response.headers().get(ETAG);
    }

    private static JsonNode contentAsJson(final FullHttpResponse response) throws Exception {
        return OBJECT_MAPPER.readTree(response.content().toString(UTF_8));
    }

    private static FullHttpRequest httpRequest(final String path) {
        final DefaultFullHttpRequest request = new DefaultFullHttpRequest(HTTP_1_1, GET, path);
        request.headers().set(ORIGIN, "http://localhost");
        return request;
    }

    private static FullHttpRequest httpRequest(final String path, HttpMethod method) {
        final DefaultFullHttpRequest request = new DefaultFullHttpRequest(HTTP_1_1, method, path);
        request.headers().set(ORIGIN, "http://localhost");
        request.headers().set(ACCESS_CONTROL_REQUEST_METHOD, method.name());
        return request;
    }

    private static FullHttpRequest httpPostRequest(final String path, HttpVersion version) {
        return new DefaultFullHttpRequest(version, POST, path);
    }

    private static FullHttpRequest httpGetRequest(final String path, final HttpVersion version) {
        return new DefaultFullHttpRequest(version, GET, path);
    }

    private static FullHttpResponse sendInfoRequest() {
        final EmbeddedChannel ch = sockJsPipeline("/echo", new SockJsEchoHandler());
        ch.writeInbound(httpRequest(ch.config().getOption(SockJsChannelOption.PREFIX) + "/info"));
        final FullHttpResponse response = ch.readOutbound();
        ch.close();
        return response;
    }

    private static Object readOutboundDiscardEmpty(final EmbeddedChannel ch) {
        final Object obj = ch.readOutbound();
        if (obj instanceof ByteBuf) {
            final ByteBuf buf = (ByteBuf) obj;
            if (buf.capacity() == 0) {
                ReferenceCountUtil.release(buf);
                return readOutboundDiscardEmpty(ch);
            }
        }
        return obj;
    }

    private static String iframeHtml(final String sockJSUrl) {
        return "<!DOCTYPE html>\n" + "<html>\n" + "<head>\n"
                + "  <meta http-equiv=\"X-UA-Compatible\" content=\"IE=edge\" />\n"
                + "  <meta http-equiv=\"Content-Type\" content=\"text/html; charset=UTF-8\" />\n" + "  <script>\n"
                + "    document.domain = document.domain;\n"
                + "    _sockjs_onload = function(){SockJS.bootstrap_iframe();};\n" + "  </script>\n"
                + "  <script src=\"" + sockJSUrl + "\"></script>\n" + "</head>\n" + "<body>\n"
                + "  <h2>Don't panic!</h2>\n"
                + "  <p>This is a SockJS hidden iframe. It's used for cross domain magic.</p>\n" + "</body>\n"
                + "</html>";
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

}
