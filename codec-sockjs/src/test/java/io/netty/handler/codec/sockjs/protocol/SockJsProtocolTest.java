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
import io.netty.handler.codec.sockjs.SockJsChannelOption;
import io.netty.handler.codec.sockjs.SockJsSocketChannelConfig;
import io.netty.handler.codec.sockjs.transport.HttpResponseBuilder;
import io.netty.handler.codec.sockjs.transport.TransportType;
import io.netty.handler.codec.sockjs.util.HttpUtil;
import io.netty.handler.codec.sockjs.util.JsonUtil;
import io.netty.util.ReferenceCountUtil;
import org.junit.Test;

import java.util.Arrays;
import java.util.UUID;
import java.util.regex.Pattern;

import static io.netty.handler.codec.http.HttpHeaders.Names.*;
import static io.netty.handler.codec.http.HttpHeaders.Values.WEBSOCKET;
import static io.netty.handler.codec.http.HttpHeaders.Values.KEEP_ALIVE;
import static io.netty.handler.codec.http.HttpMethod.*;
import static io.netty.handler.codec.http.HttpResponseStatus.BAD_REQUEST;
import static io.netty.handler.codec.http.HttpResponseStatus.INTERNAL_SERVER_ERROR;
import static io.netty.handler.codec.http.HttpResponseStatus.METHOD_NOT_ALLOWED;
import static io.netty.handler.codec.http.HttpResponseStatus.NOT_FOUND;
import static io.netty.handler.codec.http.HttpResponseStatus.NO_CONTENT;
import static io.netty.handler.codec.http.HttpResponseStatus.OK;
import static io.netty.handler.codec.http.HttpResponseStatus.SWITCHING_PROTOCOLS;
import static io.netty.handler.codec.http.HttpVersion.*;
import static io.netty.handler.codec.http.websocketx.WebSocketVersion.*;
import static io.netty.handler.codec.sockjs.DefaultSockJsSocketChannelConfig.defaultCorsConfig;
import static io.netty.handler.codec.sockjs.SockJsChannelOption.SOCKJS_URL;
import static io.netty.handler.codec.sockjs.SockJsTestUtil.*;
import static io.netty.handler.codec.sockjs.transport.EventSourceTransport.CONTENT_TYPE_EVENT_STREAM;
import static io.netty.handler.codec.sockjs.transport.HttpResponseBuilder.CONTENT_TYPE_HTML;
import static io.netty.handler.codec.sockjs.transport.HttpResponseBuilder.CONTENT_TYPE_JAVASCRIPT;
import static io.netty.handler.codec.sockjs.transport.HttpResponseBuilder.CONTENT_TYPE_PLAIN;
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
        final EmbeddedChannel ch = echoChannel();
        ch.writeInbound(httpRequest(ch.config().getOption(SockJsChannelOption.PREFIX)));

        final FullHttpResponse response = ch.readOutbound();
        assertThat(response.getStatus().code(), is(OK.code()));
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
        EmbeddedChannel ch = echoChannel();
        ch.writeInbound(httpRequest("/echo" + "/iframe.html"));

        final FullHttpResponse response1 = ch.readOutbound();
        final String etag1 = getEtag(response1);
        response1.release();
        ch.close();

        ch = echoChannel();
        ch.writeInbound(httpRequest("/echo/iframe.html"));

        final FullHttpResponse response2 = ch.readOutbound();
        final String etag2 = getEtag(response2);
        assertThat(etag1, equalTo(etag2));
        response2.release();
        ch.close();

        final HttpRequest requestWithEtag = httpRequest("/echo/iframe.html");
        requestWithEtag.headers().set(IF_NONE_MATCH, etag1);
        ch = echoChannel();
        ch.writeInbound(requestWithEtag);

        final HttpResponse response = ch.readOutbound();
        assertThat(response.getStatus(), equalTo(HttpResponseStatus.NOT_MODIFIED));
        assertThat(response.headers().get(CONTENT_TYPE), is(nullValue()));
        ch.close();
    }

    /*
     * Equivalent to InfoTest.test_basic in sockjs-protocol-0.3.3.py.
     */
    @Test
    public void infoTestBasic() throws Exception {
        final FullHttpResponse response = sendInfoRequest();
        assertThat(response.getStatus(), is(OK));
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
        final EmbeddedChannel ch = echoChannel(sockJsChannelConfig(defaultCorsConfig()
                .allowedRequestHeaders(Names.CONTENT_TYPE.toString())
                .preflightResponseHeader(Names.CONTENT_TYPE, "text/plain; charset=UTF-8")
                .build()));
        final HttpRequest request = httpRequest("/echo", OPTIONS);
        request.headers().set(ACCESS_CONTROL_REQUEST_HEADERS, CONTENT_TYPE);
        ch.writeInbound(request);

        final HttpResponse response = ch.readOutbound();
        assertThat(response.getStatus(), is(OK));
        assertCORSPreflightResponseHeaders(response);
        assertCORSHeaders(response, "*");
    }

    /*
    * Equivalent to InfoTest.test_options in sockjs-protocol-0.3.3.py.
    */
    @Test
    public void corsConfigOverride() throws Exception {
        final String origin = "http://localhost";
        final EmbeddedChannel ch = echoChannel(sockJsChannelConfig(CorsConfig.withOrigin(origin).build()));
        ch.writeInbound(httpRequest("/echo", OPTIONS));

        final HttpResponse response = ch.readOutbound();
        assertThat(response.headers().get(ACCESS_CONTROL_ALLOW_ORIGIN), equalTo(origin));
    }

    /*
     * Equivalent to InfoTest.test_options_null_origin in sockjs-protocol-0.3.3.py.
     */
    @Test
    public void infoTestOptionsNullOrigin() throws Exception {
        final SockJsSocketChannelConfig config = sockJsChannelConfig(defaultCorsConfig()
                .allowedRequestHeaders(Names.CONTENT_TYPE.toString())
                .preflightResponseHeader(Names.CONTENT_TYPE, "text/plain; charset=UTF-8")
                .build());
        final EmbeddedChannel ch = echoChannel(config);
        final FullHttpRequest request = httpRequest("/echo/info", OPTIONS);
        request.headers().set(ACCESS_CONTROL_REQUEST_HEADERS, CONTENT_TYPE);
        request.headers().set(ORIGIN, "null");
        ch.writeInbound(request);

        final HttpResponse response = ch.readOutbound();
        assertThat(response.getStatus(), is(OK));
        assertCORSPreflightResponseHeaders(response);
        assertCORSHeaders(response, "*");
    }

    /*
     * Equivalent to InfoTest.test_disabled_websocket in sockjs-protocol-0.3.3.py.
     */
    @Test
    public void infoTestDisabledWebsocket() throws Exception {
        final EmbeddedChannel ch = echoChannel();
        ch.config().setOption(SockJsChannelOption.WEBSOCKET_ENABLED, false);
        ch.writeInbound(httpRequest("/echo/info"));

        final FullHttpResponse response = ch.readOutbound();
        assertThat(response.getStatus(), is(OK));
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
        final String sessionId = UUID.randomUUID().toString();
        final String sessionUrl = "/echo/000/" + sessionId;

        final FullHttpResponse openSessionResponse = xhrRequest(sessionUrl, echoChannel());
        assertOpenFrameResponse(openSessionResponse);

        final FullHttpResponse sendResponse = xhrSendRequest(sessionUrl, "[\"a\"]", echoChannel());
        assertNoContent(sendResponse);
        sendResponse.release();

        final FullHttpResponse pollResponse = xhrRequest("/echo/999/" + sessionId, echoChannel());
        assertMessageFrameContent(pollResponse, "a");
        pollResponse.release();
    }

    /*
     * Equivalent to Protocol.test_simpleSession in sockjs-protocol-0.3.3.py.
     */
    @Test
    public void protocolTestSimpleSession() throws Exception {
        final String sessionUrl = "/echo/111/" + UUID.randomUUID();

        final FullHttpResponse openSessionResponse = xhrRequest(sessionUrl, echoChannel());
        assertOpenFrameResponse(openSessionResponse);

        final FullHttpResponse sendResponse = xhrSendRequest(sessionUrl, "[\"a\"]", echoChannel());
        assertNoContent(sendResponse);
        sendResponse.release();

        final FullHttpResponse pollResponse = xhrRequest(sessionUrl, echoChannel());
        assertMessageFrameContent(pollResponse, "a");
        pollResponse.release();

        final FullHttpResponse badSessionResponse = xhrSendRequest("/echo/111/badsession", "[\"a\"]", echoChannel());
        assertThat(badSessionResponse.getStatus(), is(NOT_FOUND));
        badSessionResponse.release();
    }

    /*
     * Equivalent to Protocol.test_closeSession in sockjs-protocol-0.3.3.py.
     */
    @Test
    public void protocolTestCloseSession() throws Exception {
        final String sessionUrl = "/close/222/" + UUID.randomUUID();
        final FullHttpResponse openSessionResponse = xhrRequest(sessionUrl, closeChannel());
        assertOpenFrameResponse(openSessionResponse);
        assertGoAwayResponse(xhrRequest(sessionUrl, closeChannel()));
        assertGoAwayResponse(xhrRequest(sessionUrl, closeChannel()));
    }

    /*
     * Equivalent to WebSocketHttpErrors.test_httpMethod in sockjs-protocol-0.3.3.py.
     */
    @Test
    public void webSocketHttpErrorsTestHttpMethod() throws Exception {
        final String sessionUrl = "/echo/222/" + UUID.randomUUID();
        final EmbeddedChannel ch = echoChannel();

        ch.writeInbound(httpRequest(sessionUrl + "/websocket"));

        final FullHttpResponse response = ch.readOutbound();
        assertThat(response.getStatus(), equalTo(BAD_REQUEST));
        assertThat(response.content().toString(UTF_8), equalTo("Can \"Upgrade\" only to \"WebSocket\"."));
        response.release();
    }

    /*
     * Equivalent to WebSocketHttpErrors.test_invalidConnectionHeader in sockjs-protocol-0.3.3.py.
     */
    @Test
    public void webSocketHttpErrorsTestInvalidConnectionHeader() throws Exception {
        final String sessionUrl = "/echo/222/" + UUID.randomUUID();
        final EmbeddedChannel ch = echoChannel();

        final FullHttpRequest request = webSocketUpgradeRequest(sessionUrl + "/websocket");
        request.headers().set(UPGRADE, "websocket");
        request.headers().set(CONNECTION, "close");
        ch.writeInbound(request);

        final FullHttpResponse response = ch.readOutbound();
        assertThat(response.getStatus(), equalTo(BAD_REQUEST));
        assertThat(response.content().toString(UTF_8), equalTo("\"Connection\" must be \"Upgrade\"."));
        response.release();
    }

    /*
     * Equivalent to WebsocketHttpErrors.test_invalidMethod in sockjs-protocol-0.3.3.py.
     */
    @Test
    public void webSocketHttpErrorsTestInvalidMethod() throws Exception {
        final String sessionUrl = "/echo/222/" + UUID.randomUUID();
        final EmbeddedChannel ch = echoChannel();

        final FullHttpRequest request = webSocketUpgradeRequest(sessionUrl + "/websocket");
        request.setMethod(POST);
        ch.writeInbound(request);

        final HttpResponse response = ch.readOutbound();
        assertThat(response.getStatus(), equalTo(METHOD_NOT_ALLOWED));
    }

    /*
     * Equivalent to WebsocketHixie76.test_transport in sockjs-protocol-0.3.3.py.
     */
    @Test
    public void webSocketHixie76TestTransport() throws Exception {
        final String sessionUrl = "/echo/222/" + UUID.randomUUID();
        final EmbeddedChannel ch = webSocketEchoChannel();

        ch.writeInbound(webSocketUpgradeRequest(sessionUrl + "/websocket", V00));

        final FullHttpResponse upgradeResponse = HttpUtil.decodeFullHttpResponse(ch);
        assertThat(upgradeResponse.content().toString(UTF_8), equalTo("8jKS'y:G*Co,Wxa-"));
        upgradeResponse.release();
        ch.readOutbound();
        ch.readOutbound();

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
        final String sessionUrl = "/close/222/" + UUID.randomUUID();
        final EmbeddedChannel ch = webSocketCloseChannel();

        final FullHttpRequest request = webSocketUpgradeRequest(sessionUrl + "/websocket", V00);
        ch.writeInbound(request);

        final FullHttpResponse upgradeResponse = HttpUtil.decodeFullHttpResponse(ch);
        assertThat(upgradeResponse.content().toString(UTF_8), equalTo("8jKS'y:G*Co,Wxa-"));
        upgradeResponse.release();

        ch.readOutbound();
        ch.readOutbound();
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
        final String sessionUrl = "/echo/222/" + UUID.randomUUID();
        final EmbeddedChannel ch = webSocketEchoChannel();

        final FullHttpRequest request = webSocketUpgradeRequest(sessionUrl + "/websocket", V00);
        ch.writeInbound(request);

        final FullHttpResponse upgradeResponse = HttpUtil.decodeFullHttpResponse(ch);
        assertThat(upgradeResponse.content().toString(UTF_8), equalTo("8jKS'y:G*Co,Wxa-"));
        upgradeResponse.release();
        ch.readOutbound();
        ch.readOutbound();

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
        final String sessionUrl = "/echo/222/" + UUID.randomUUID();
        final EmbeddedChannel ch1 = webSocketEchoChannel();
        final EmbeddedChannel ch2 = webSocketEchoChannel();
        final EmbeddedChannel ch3 = webSocketEchoChannel();

        ch1.writeInbound(webSocketUpgradeRequest(sessionUrl + "/websocket", V00));

        final FullHttpResponse upgradeResponse = HttpUtil.decodeFullHttpResponse(ch1);
        assertThat(upgradeResponse.getStatus(), equalTo(SWITCHING_PROTOCOLS));
        assertThat(upgradeResponse.content().toString(UTF_8), equalTo("8jKS'y:G*Co,Wxa-"));
        upgradeResponse.release();

        ch2.writeInbound(webSocketUpgradeRequest(sessionUrl + "/websocket", V00));

        final FullHttpResponse upgradeResponse2 = HttpUtil.decodeFullHttpResponse(ch2);
        assertThat(upgradeResponse2.getStatus(), equalTo(SWITCHING_PROTOCOLS));
        assertThat(upgradeResponse2.content().toString(UTF_8), equalTo("8jKS'y:G*Co,Wxa-"));
        upgradeResponse2.release();

        ch1.readOutbound();
        ch1.readOutbound();
        final ByteBufHolder open1 = ch1.readOutbound();
        assertThat(open1.content().toString(UTF_8), equalTo("o"));
        open1.release();

        ch2.readOutbound();
        ch2.readOutbound();
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

        ch3.writeInbound(webSocketUpgradeRequest(sessionUrl + "/websocket"));

        final HttpResponse upgradeResponse3 = HttpUtil.decode(ch3);
        assertThat(upgradeResponse3.getStatus(), equalTo(SWITCHING_PROTOCOLS));

        final ByteBufHolder open3 = ch3.readOutbound();
        assertThat(open3.content().toString(UTF_8), equalTo("o"));
        open3.release();

        ch3.writeInbound(new TextWebSocketFrame("\"a\""));
        final ByteBufHolder msg3 = ch3.readOutbound();
        assertThat(msg3.content().toString(UTF_8), equalTo("a[\"a\"]"));
        msg3.release();

        ch3.writeInbound(new TextWebSocketFrame("\"b\""));
        final ByteBufHolder msg4 = ch3.readOutbound();
        assertThat(msg4.content().toString(UTF_8), equalTo("a[\"b\"]"));
        msg4.release();
        ch3.close();
    }

    /*
     * Equivalent to WebsocketHixie76.test_haproxy in sockjs-protocol-0.3.3.py.
     */
    @Test
    public void webSocketHixie76TestHAProxy() throws Exception {
        final String sessionUrl = "/echo/222/" + UUID.randomUUID();
        final EmbeddedChannel ch = webSocketEchoChannel();

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
        assertThat(upgradeResponse.getStatus(), equalTo(SWITCHING_PROTOCOLS));

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
        final String serviceName = "/echo";
        final String sessionUrl = serviceName + "/222/" + UUID.randomUUID();
        final EmbeddedChannel ch = webSocketEchoChannel();

        final FullHttpRequest request = webSocketUpgradeRequest(sessionUrl + "/websocket", V00);
        ch.writeInbound(request);

        final FullHttpResponse upgradeResponse = HttpUtil.decodeFullHttpResponse(ch);
        assertThat(upgradeResponse.getStatus(), equalTo(SWITCHING_PROTOCOLS));
        assertThat(upgradeResponse.content().toString(UTF_8), equalTo("8jKS'y:G*Co,Wxa-"));
        ch.readOutbound();
        ch.readOutbound();

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
        final EmbeddedChannel ch = webSocketEchoChannel();
        final FullHttpRequest request = HttpUtil.webSocketUpgradeRequest("/echo/123/123/websocket", V08);
        request.headers().set(CONNECTION, "keep-alive, Upgrade");
        ch.writeInbound(request);

        final HttpResponse response = HttpUtil.decode(ch);
        assertThat(response.getStatus(), is(SWITCHING_PROTOCOLS));
        assertThat(response.headers().get(CONNECTION), equalTo("Upgrade"));
    }

    /*
     * Equivalent to XhrPolling.test_options in sockjs-protocol-0.3.3.py.
     */
    @Test
    public void xhrPollingTestOptions() throws Exception {
        final SockJsSocketChannelConfig config = sockJsChannelConfig(defaultCorsConfig()
                .allowedRequestHeaders(Names.CONTENT_TYPE.toString())
                .preflightResponseHeader(Names.CONTENT_TYPE, "text/plain; charset=UTF-8")
                .build());
        final String sessionUrl = "/echo/abc/" + UUID.randomUUID();
        final FullHttpRequest xhrRequest = httpRequest(sessionUrl + "/xhr", OPTIONS);
        xhrRequest.headers().set(ACCESS_CONTROL_REQUEST_HEADERS, CONTENT_TYPE);

        final HttpResponse xhrOptionsResponse = xhrRequest(xhrRequest, echoChannel(config));
        assertCORSPreflightResponseHeaders(xhrOptionsResponse);

        final FullHttpRequest xhrSendRequest = httpRequest(sessionUrl + "/xhr_send", OPTIONS);
        xhrSendRequest.headers().set(ACCESS_CONTROL_REQUEST_HEADERS, CONTENT_TYPE);
        final HttpResponse xhrSendOptionsResponse = xhrRequest(xhrSendRequest, echoChannel(config));
        assertCORSPreflightResponseHeaders(xhrSendOptionsResponse);
    }

    /*
     * Equivalent to XhrPolling.test_transport in sockjs-protocol-0.3.3.py.
     */
    @Test
    public void xhrPollingTestTransport() throws Exception {
        final String sessionUrl = "/echo/abc/" + UUID.randomUUID();

        final FullHttpResponse response = xhrRequest(sessionUrl, echoChannel());
        assertOpenFrameResponse(response);
        assertThat(response.headers().get(CONTENT_TYPE), equalTo(CONTENT_TYPE_JAVASCRIPT));
        assertCORSHeaders(response, "*");
        verifyNotCached(response);

        final FullHttpResponse xhrSendResponse = xhrSendRequest(sessionUrl, "[\"x\"]", echoChannel());
        assertNoContent(xhrSendResponse);
        assertThat(xhrSendResponse.headers().get(CONTENT_TYPE), equalTo(CONTENT_TYPE_PLAIN));
        assertCORSHeaders(response, "*");
        verifyNotCached(xhrSendResponse);
        xhrSendResponse.release();

        final FullHttpResponse pollResponse = xhrRequest(sessionUrl, echoChannel());
        assertMessageFrameContent(pollResponse, "x");
        pollResponse.release();
    }

    @Test
    public void xhrPollingSessionReuse() throws Exception {
        final String sessionUrl = "/echo/abc/" + UUID.randomUUID();
        assertOpenFrameResponse(xhrRequest(sessionUrl, echoChannel()));
        assertNoContent(xhrSendRequest(sessionUrl, "[\"x\"]", echoChannel()));
        assertMessageFrameContent(xhrRequest(sessionUrl, echoChannel()), "x");
        xhrRequest(sessionUrl, echoChannel());
        assertNoContent(xhrSendRequest(sessionUrl, "[\"x\"]", echoChannel()));
        assertMessageFrameContent(xhrRequest(sessionUrl, echoChannel()), "x");
    }

    /*
     * Equivalent to XhrPolling.test_invalid_session in sockjs-protocol-0.3.3.py.
     */
    @Test
    public void xhrPollingTestInvalidSession() throws Exception {
        final String sessionUrl = "/echo/abc/" + UUID.randomUUID();
        final FullHttpResponse xhrSendResponse = xhrSendRequest(sessionUrl, "[\"x\"]", echoChannel());
        assertThat(xhrSendResponse.getStatus(), is(NOT_FOUND));
        xhrSendResponse.release();
    }

    /*
     * Equivalent to XhrPolling.test_invalid_json sockjs-protocol-0.3.3.py.
     */
    @Test
    public void xhrPollingTestInvalidJson() throws Exception {
        final String sessionUrl = "/echo/abc/" + UUID.randomUUID();

        assertOpenFrameResponse(xhrRequest(sessionUrl, echoChannel()));

        final FullHttpResponse xhrSendResponse = xhrSendRequest(sessionUrl, "[\"x\"", echoChannel());
        assertThat(xhrSendResponse.getStatus(), is(INTERNAL_SERVER_ERROR));
        assertThat(xhrSendResponse.content().toString(UTF_8), equalTo("Broken JSON encoding."));
        xhrSendResponse.release();

        final FullHttpResponse noPayloadResponse = xhrSendRequest(sessionUrl, "", echoChannel());
        assertThat(noPayloadResponse.getStatus(), is(INTERNAL_SERVER_ERROR));
        assertThat(noPayloadResponse.content().toString(UTF_8), equalTo("Payload expected."));
        noPayloadResponse.release();

        final FullHttpResponse validSendResponse = xhrSendRequest(sessionUrl, "[\"a\"]", echoChannel());
        assertThat(validSendResponse.getStatus(), is(NO_CONTENT));
        validSendResponse.release();

        final FullHttpResponse pollResponse = xhrRequest(sessionUrl, echoChannel());
        assertMessageFrameContent(pollResponse, "a");
        pollResponse.release();
    }

    /*
     * Equivalent to XhrPolling.test_content_types sockjs-protocol-0.3.3.py.
     */
    @Test
    public void xhrPollingTestContentTypes() throws Exception {
        final String sessionUrl = "/echo/abc/" + UUID.randomUUID();

        final FullHttpResponse response = xhrRequest(sessionUrl, echoChannel());
        assertThat(response.getStatus(), is(OK));
        assertThat(response.content().toString(UTF_8), equalTo("o\n"));
        response.release();

        final FullHttpResponse textPlain = xhrSendRequest(sessionUrl, "[\"a\"]", "text/plain", echoChannel());
        assertThat(textPlain.getStatus(), is(NO_CONTENT));
        textPlain.release();

        final FullHttpResponse json = xhrSendRequest(sessionUrl, "[\"b\"]", "application/json", echoChannel());
        assertThat(json.getStatus(), is(NO_CONTENT));
        json.release();

        final FullHttpResponse json2 = xhrSendRequest(sessionUrl, "[\"c\"]", "application/json;charset=utf-8",
                echoChannel());
        assertThat(json2.getStatus(), is(NO_CONTENT));
        json2.release();
        final FullHttpResponse xml = xhrSendRequest(sessionUrl, "[\"d\"]", "application/xml", echoChannel());
        assertThat(xml.getStatus(), is(NO_CONTENT));
        xml.release();
        final FullHttpResponse xml2 = xhrSendRequest(sessionUrl, "[\"e\"]", "text/xml", echoChannel());
        assertThat(xml2.getStatus(), is(NO_CONTENT));
        xml2.release();
        final FullHttpResponse xml3 = xhrSendRequest(sessionUrl, "[\"f\"]", "text/xml; charset=utf-8", echoChannel());
        assertThat(xml3.getStatus(), is(NO_CONTENT));
        xml3.release();
        final FullHttpResponse empty = xhrSendRequest(sessionUrl, "[\"g\"]", "", echoChannel());
        assertThat(empty.getStatus(), is(NO_CONTENT));
        empty.release();

        final FullHttpResponse pollRequest = xhrRequest(sessionUrl, echoChannel());
        assertThat(pollRequest.getStatus(), is(OK));
        assertThat(pollRequest.content().toString(UTF_8), equalTo("a[\"a\",\"b\",\"c\",\"d\",\"e\",\"f\",\"g\"]\n"));
        pollRequest.release();
    }

    /*
     * Equivalent to XhrPolling.test_request_headers_cors sockjs-protocol-0.3.3.py.
     */
    @Test
    public void xhrPollingTestRequestHeadersCors() throws Exception {
        final SockJsSocketChannelConfig config = sockJsChannelConfig(defaultCorsConfig()
                .allowedRequestHeaders(Names.CONTENT_TYPE.toString(), "a", "b", "c")
                .build());
        final String sessionUrl = "/echo/abc/" + UUID.randomUUID();

        final FullHttpRequest okRequest = httpRequest(sessionUrl + "/xhr", POST);
        okRequest.headers().set(ORIGIN, "https://localhost:8081");
        okRequest.headers().set(ACCESS_CONTROL_REQUEST_HEADERS, Arrays.asList("a", "b", "c", CONTENT_TYPE));
        final HttpResponse response = xhrRequest(okRequest, echoChannel(config));
        assertThat(response.getStatus(), is(OK));
        assertCORSHeaders(response, "*");
        assertThat(response.headers().getAll(ACCESS_CONTROL_ALLOW_HEADERS), hasItems("a", "b", "c"));

        final String emptySessionUrl = "/echo/abc/" + UUID.randomUUID();
        final FullHttpRequest emptyHeaderRequest = httpRequest(emptySessionUrl + "/xhr", POST);
        emptyHeaderRequest.headers().set(ACCESS_CONTROL_REQUEST_HEADERS, "");
        config.setCorsConfig(defaultCorsConfig().allowedRequestHeaders("").build());
        final HttpResponse emptyHeaderResponse = xhrRequest(emptyHeaderRequest, echoChannel(config));
        assertThat(emptyHeaderResponse.getStatus(), is(OK));
        assertCORSHeaders(response, "*");
        assertThat(emptyHeaderResponse.headers().get(ACCESS_CONTROL_ALLOW_HEADERS), equalTo(""));

        final String noHeaderSessionUrl = "/echo/abc/" + UUID.randomUUID();
        final FullHttpRequest noHeaderRequest = httpRequest(noHeaderSessionUrl + "/xhr", POST);
        final HttpResponse noHeaderResponse = xhrRequest(noHeaderRequest, echoChannel(config));
        assertThat(noHeaderResponse.getStatus(), is(OK));
        assertCORSHeaders(response, "*");
        assertThat(noHeaderResponse.headers().get(ACCESS_CONTROL_ALLOW_HEADERS), is(nullValue()));
    }

    /*
     * Equivalent to XhrStreaming.test_options in sockjs-protocol-0.3.3.py.
     */
    @Test
    public void xhrStreamingTestOptions() throws Exception {
        final String sessionUrl = "/echo/222/" + UUID.randomUUID();
        final EmbeddedChannel ch = echoChannel();

        final FullHttpRequest request = httpRequest(sessionUrl + TransportType.XHR_STREAMING.path(), GET);
        ch.writeInbound(request);

        final HttpResponse response = ch.readOutbound();
        assertThat(response.getStatus(), equalTo(OK));
        assertThat(response.headers().get(CONTENT_TYPE), equalTo(CONTENT_TYPE_JAVASCRIPT));
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

        final FullHttpResponse validSendResponse = xhrSendRequest(sessionUrl, "[\"x\"]", echoChannel());
        assertThat(validSendResponse.getStatus(), is(NO_CONTENT));
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
        final String sessionUrl = "/echo/222/" + UUID.randomUUID();
        final EmbeddedChannel ch = echoChannel();
        ch.config().setOption(SockJsChannelOption.MAX_STREAMING_BYTES_SIZE, 4096);

        final FullHttpRequest request = httpRequest(sessionUrl + TransportType.XHR_STREAMING.path(), GET);
        ch.writeInbound(request);
        final HttpResponse response = ch.readOutbound();
        assertThat(response.getStatus(), equalTo(OK));
        assertThat(response.headers().get(CONTENT_TYPE), equalTo(CONTENT_TYPE_JAVASCRIPT));
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
            final FullHttpResponse validSendResponse = xhrSendRequest(sessionUrl, "[\"" + msg + "\"]", echoChannel());
            assertThat(validSendResponse.getStatus(), is(NO_CONTENT));
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
        final String sessionUrl = "/echo/222/" + UUID.randomUUID();
        final EmbeddedChannel ch = echoChannel();
        ch.config().setOption(SockJsChannelOption.MAX_STREAMING_BYTES_SIZE, 4096);

        final FullHttpRequest request = httpRequest(sessionUrl + TransportType.EVENTSOURCE.path(), GET);
        ch.writeInbound(request);
        final HttpResponse response =  ch.readOutbound();
        assertThat(response.getStatus(), equalTo(OK));
        assertThat(response.headers().get(CONTENT_TYPE), equalTo(CONTENT_TYPE_EVENT_STREAM));

        final DefaultHttpContent newLinePrelude = ch.readOutbound();
        assertThat(newLinePrelude.content().toString(UTF_8), equalTo("\r\n"));
        newLinePrelude.release();

        final DefaultHttpContent data = ch.readOutbound();
        assertThat(data.content().toString(UTF_8), equalTo("data: o\r\n\r\n"));
        data.release();

        final String msg = generateMessage(4096);
        final FullHttpResponse validSendResponse = xhrSendRequest(sessionUrl, "[\"" + msg + "\"]", echoChannel());
        assertThat(validSendResponse.getStatus(), is(NO_CONTENT));
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
        final String sessionUrl = "/echo/222/" + UUID.randomUUID();
        final EmbeddedChannel ch = echoChannel();
        ch.config().setOption(SockJsChannelOption.MAX_STREAMING_BYTES_SIZE, 4096);

        final FullHttpRequest request = httpRequest(sessionUrl + TransportType.EVENTSOURCE.path(), GET);
        ch.writeInbound(request);
        final HttpResponse response =  ch.readOutbound();
        assertThat(response.getStatus(), equalTo(OK));
        assertThat(response.headers().get(CONTENT_TYPE), equalTo(CONTENT_TYPE_EVENT_STREAM));

        final DefaultHttpContent newLinePrelude = ch.readOutbound();
        assertThat(newLinePrelude.content().toString(UTF_8), equalTo("\r\n"));
        newLinePrelude.release();

        final DefaultHttpContent data = ch.readOutbound();
        assertThat(data.content().toString(UTF_8), equalTo("data: o\r\n\r\n"));
        data.release();

        final String msg = "[\"  \\u0000\\n\\r \"]";
        final FullHttpResponse validSendResponse = xhrSendRequest(sessionUrl, msg, echoChannel());
        assertThat(validSendResponse.getStatus(), is(NO_CONTENT));
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
        final String serviceName = "/echo";
        final String sessionUrl = serviceName + "/222/" + UUID.randomUUID();
        final EmbeddedChannel ch = echoChannel();
        ch.config().setOption(SockJsChannelOption.MAX_STREAMING_BYTES_SIZE, 4096);

        final FullHttpRequest request = httpRequest(sessionUrl + TransportType.HTMLFILE.path() + "?c=callback", GET);
        ch.writeInbound(request);
        final HttpResponse response =  ch.readOutbound();
        assertThat(response.getStatus(), equalTo(OK));
        assertThat(response.headers().get(CONTENT_TYPE), equalTo(CONTENT_TYPE_HTML));

        final HttpContent headerChunk = ch.readOutbound();
        assertThat(headerChunk.content().readableBytes(), is(greaterThan(1024)));
        final String header = headerChunk.content().toString(UTF_8);
        assertThat(header, containsString("var c = parent.callback"));
        headerChunk.release();

        final HttpContent openChunk = ch.readOutbound();
        assertThat(openChunk.content().toString(UTF_8), equalTo("<script>\np(\"o\");\n</script>\r\n"));
        openChunk.release();

        final FullHttpResponse validSendResponse = xhrSendRequest(sessionUrl, "[\"x\"]", echoChannel());
        assertThat(validSendResponse.getStatus(), is(NO_CONTENT));
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
        final String serviceName = "/echo";
        final String sessionUrl = serviceName + "/222/" + UUID.randomUUID();
        final EmbeddedChannel ch = echoChannel();
        ch.config().setOption(SockJsChannelOption.MAX_STREAMING_BYTES_SIZE, 4096);

        final FullHttpRequest request = httpRequest(sessionUrl + TransportType.HTMLFILE.path() + "?c=", GET);
        ch.writeInbound(request);
        final FullHttpResponse response =  ch.readOutbound();
        assertThat(response.getStatus(), equalTo(INTERNAL_SERVER_ERROR));
        assertThat(response.content().toString(UTF_8), equalTo("\"callback\" parameter required"));
        response.release();
    }

    /*
     * Equivalent to HtmlFile.test_response_limit in sockjs-protocol-0.3.3.py.
     */
    @Test
    public void htmlFileTestResponseLimit() throws Exception {
        final String serviceName = "/echo";
        final String sessionUrl = serviceName + "/222/" + UUID.randomUUID();
        final EmbeddedChannel ch = echoChannel();
        ch.config().setOption(SockJsChannelOption.MAX_STREAMING_BYTES_SIZE, 4096);

        final FullHttpRequest request = httpRequest(sessionUrl + TransportType.HTMLFILE.path() + "?c=callback", GET);
        ch.writeInbound(request);
        final HttpResponse response =  ch.readOutbound();
        assertThat(response.getStatus(), equalTo(OK));

        // read and discard header chunk
        ReferenceCountUtil.release(ch.readOutbound());
        // read and discard open frame
        ReferenceCountUtil.release(ch.readOutbound());

        final String msg = generateMessage(4096);
        final FullHttpResponse validSendResponse = xhrSendRequest(sessionUrl, "[\"" + msg + "\"]", echoChannel());
        assertThat(validSendResponse.getStatus(), is(NO_CONTENT));
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
        final String serviceName = "/echo";
        final String sessionUrl = serviceName + "/222/" + UUID.randomUUID();

        final EmbeddedChannel ch = echoChannel();
        final FullHttpResponse response = (FullHttpResponse) jsonpRequest(sessionUrl + "/jsonp?c=%63allback", ch);
        assertThat(response.getStatus(), is(OK));
        assertThat(response.content().toString(UTF_8), equalTo("callback(\"o\");\r\n"));
        assertThat(response.headers().get(CONTENT_TYPE), equalTo(CONTENT_TYPE_JAVASCRIPT));
        verifyNotCached(response);
        response.release();

        final String data = "d=%5B%22x%22%5D";
        final FullHttpResponse sendResponse = jsonpSend(sessionUrl + "/jsonp_send", data, echoChannel());
        assertThat(sendResponse.getStatus(), is(OK));
        assertThat(sendResponse.content().toString(UTF_8), equalTo("ok"));
        assertThat(sendResponse.headers().get(CONTENT_TYPE), equalTo(CONTENT_TYPE_PLAIN));
        verifyNotCached(response);
        sendResponse.release();

        final FullHttpResponse pollResponse = (FullHttpResponse) jsonpRequest(sessionUrl + "/jsonp?c=callback",
                echoChannel());
        assertThat(pollResponse.getStatus(), is(OK));
        assertThat(pollResponse.headers().get(CONTENT_TYPE), equalTo(CONTENT_TYPE_JAVASCRIPT));
        assertThat(pollResponse.content().toString(UTF_8), equalTo("callback(\"a[\\\"x\\\"]\");\r\n"));
        verifyNotCached(pollResponse);
        pollResponse.release();
    }

    /*
     * Equivalent to JsonPolling.test_no_callback in sockjs-protocol-0.3.3.py.
     */
    @Test
    public void jsonpPollingTestNoCallback() throws Exception {
        final EmbeddedChannel ch = echoChannel();
        final FullHttpResponse response = (FullHttpResponse) jsonpRequest("/echo/a/a/jsonp", ch);
        assertThat(response.getStatus(), is(INTERNAL_SERVER_ERROR));
        assertThat(response.content().toString(UTF_8), equalTo("\"callback\" parameter required"));
        response.release();
    }

    /*
     * Equivalent to JsonPolling.test_invalid_json in sockjs-protocol-0.3.3.py.
     */
    @Test
    public void jsonpPollingTestInvalidJson() throws Exception {
        final String serviceName = "/echo";
        final String sessionUrl = serviceName + "/222/" + UUID.randomUUID();

        final FullHttpResponse response = (FullHttpResponse) jsonpRequest(sessionUrl + "/jsonp?c=x", echoChannel());
        assertThat(response.getStatus(), is(OK));
        assertThat(response.content().toString(UTF_8), equalTo("x(\"o\");\r\n"));
        response.release();

        assertBrokenJSONEncoding(jsonpSend(sessionUrl + "/jsonp_send", "d=%5B%22x", echoChannel()));
        assertPayloadExpected(jsonpSend(sessionUrl + "/jsonp_send", "", echoChannel()));
        assertPayloadExpected(jsonpSend(sessionUrl + "/jsonp_send", "d=", echoChannel()));
        assertPayloadExpected(jsonpSend(sessionUrl + "/jsonp_send", "p=p", echoChannel()));

        final FullHttpResponse sendResponse = jsonpSend(sessionUrl + "/jsonp_send", "d=%5B%22b%22%5D", echoChannel());
        assertThat(sendResponse.getStatus(), is(OK));
        sendResponse.release();

        final FullHttpResponse pollResponse = (FullHttpResponse) jsonpRequest(sessionUrl + "/jsonp?c=x", echoChannel());
        assertThat(pollResponse.getStatus(), is(OK));
        assertThat(pollResponse.content().toString(UTF_8), equalTo("x(\"a[\\\"b\\\"]\");\r\n"));
        pollResponse.release();
    }

    /*
     * Equivalent to JsonPolling.test_content_types in sockjs-protocol-0.3.3.py.
     */
    @Test
    public void jsonpPollingTestContentTypes() throws Exception {
        final String serviceName = "/echo";
        final String sessionUrl = serviceName + "/222/" + UUID.randomUUID();

        final FullHttpResponse response = (FullHttpResponse) jsonpRequest(sessionUrl + "/jsonp?c=x", echoChannel());
        assertThat(response.content().toString(UTF_8), equalTo("x(\"o\");\r\n"));
        response.release();

        final String data = "d=%5B%22abc%22%5D";
        final FullHttpResponse sendResponse = jsonpSend(sessionUrl + "/jsonp_send", data, echoChannel());
        assertThat(sendResponse.getStatus(), is(OK));
        sendResponse.release();

        final FullHttpRequest plainRequest = httpRequest(sessionUrl + "/jsonp_send", POST);
        plainRequest.headers().set(CONTENT_TYPE, "text/plain");
        final ByteBuf byteBuf = Unpooled.copiedBuffer("[\"%61bc\"]", UTF_8);
        plainRequest.content().writeBytes(byteBuf);
        byteBuf.release();

        final FullHttpResponse plainResponse = jsonpSend(plainRequest, echoChannel());
        assertThat(plainResponse.getStatus(), is(OK));
        plainResponse.release();

        final FullHttpResponse pollResponse = (FullHttpResponse) jsonpRequest(sessionUrl + "/jsonp?c=x", echoChannel());
        assertThat(pollResponse.getStatus(), is(OK));
        assertThat(pollResponse.content().toString(UTF_8), equalTo("x(\"a[\\\"abc\\\",\\\"%61bc\\\"]\");\r\n"));
        pollResponse.release();
    }

    /*
     * Equivalent to JsonPolling.test_close in sockjs-protocol-0.3.3.py.
     */
    @Test
    public void jsonpPollingTestClose() throws Exception {
        final String serviceName = "/close";
        final String sessionUrl = serviceName + "/222/" + UUID.randomUUID();
        final FullHttpResponse response = (FullHttpResponse) jsonpRequest(sessionUrl + "/jsonp?c=x", closeChannel());
        assertThat(response.content().toString(UTF_8), equalTo("x(\"o\");\r\n"));
        response.release();

        final FullHttpResponse firstResponse = (FullHttpResponse) jsonpRequest(sessionUrl + "/jsonp?c=x",
                closeChannel());
        assertThat(firstResponse.content().toString(UTF_8), equalTo("x(\"c[3000,\\\"Go away!\\\"]\");\r\n"));
        firstResponse.release();

        final FullHttpResponse secondResponse = (FullHttpResponse) jsonpRequest(sessionUrl + "/jsonp?c=x",
                closeChannel());
        assertThat(secondResponse.content().toString(UTF_8), equalTo("x(\"c[3000,\\\"Go away!\\\"]\");\r\n"));
        secondResponse.release();
    }

    /*
     * Equivalent to JsessionIdCookie.test_basic in sockjs-protocol-0.3.3.py.
     */
    @Test
    public void jsessionIdCookieTestBasic() throws Exception {
        final EmbeddedChannel ch = echoChannel();
        ch.config().setOption(SockJsChannelOption.COOKIES_NEEDED, true);
        final FullHttpResponse response = infoRequest(ch, "/echo");
        assertThat(response.getStatus(), is(OK));
        verifyNoSET_COOKIE(response);
        assertThat(infoAsJson(response).get("cookie_needed").asBoolean(), is(true));
        response.release();
    }

    /*
     * Equivalent to JsessionIdCookie.test_xhr in sockjs-protocol-0.3.3.py.
     */
    @Test
    public void jsessionIdCookieTestXhr() throws Exception {
        final EmbeddedChannel ch = echoChannel();
        ch.config().setOption(SockJsChannelOption.COOKIES_NEEDED, true);
        assertSetCookie(TransportType.XHR.path());

        final String serviceName = "/echo";
        final String sessionUrl = serviceName + "/abc/" + UUID.randomUUID();
        final FullHttpRequest request = httpRequest(sessionUrl + TransportType.XHR.path(), GET);
        request.headers().set("Cookie", ClientCookieEncoder.encode("JSESSIONID", "abcdef"));
        ch.writeInbound(request);
        final FullHttpResponse response = ch.readOutbound();
        assertThat(response.getStatus(), is(OK));
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
        final String serviceName = "/echo";
        final EmbeddedChannel ch = webSocketEchoChannel();

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
        final EmbeddedChannel ch = webSocketCloseChannel();
        ch.writeInbound(webSocketUpgradeRequest("/close/websocket"));
        ReferenceCountUtil.release(ch.readOutbound());
        assertThat(ch.isActive(), is(false));
        ch.finish();
    }

    @Test
    public void webSocketCloseSession() throws Exception {
        final String serviceName = "/echo";
        final String sessionUrl = serviceName + "/222/" + UUID.randomUUID();
        final EmbeddedChannel ch = webSocketEchoChannel();

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
        final String sessionUrl = "/echo/abc/" + UUID.randomUUID();

        final FullHttpResponse response = xhrRequest(sessionUrl, echoChannel());
        assertOpenFrameResponse(response);

        final String content = serverKillerStringEsc();
        final FullHttpResponse xhrSendResponse = xhrSendRequest(sessionUrl, "[\"" + content + "\"]", echoChannel());
        assertThat(xhrSendResponse.getStatus(), is(NO_CONTENT));
        xhrSendResponse.release();

        final FullHttpResponse pollResponse = xhrRequest(sessionUrl, echoChannel());
        assertThat(pollResponse.getStatus(), is(OK));
        assertThat(pollResponse.content().toString(UTF_8), equalTo("a[\"" + content + "\"]\n"));
        pollResponse.release();
    }

    /*
     * Equivalent to JSONEncoding.test_xhr_server_decodes in sockjs-protocol-0.3.3.py.
     */
    @Test
    public void jsonEncodingTestXhrServerDecodes() throws Exception {
        final String sessionUrl = "/echo/abc/" + UUID.randomUUID();

        final FullHttpResponse response = xhrRequest(sessionUrl, echoChannel());
        assertThat(response.getStatus(), is(OK));
        assertThat(response.content().toString(UTF_8), equalTo("o\n"));
        response.release();

        final String content = "[\"" + generateUnicodeValues(0x0000, 0xFFFF) + "\"]";
        final FullHttpResponse xhrSendResponse = xhrSendRequest(sessionUrl, content, echoChannel());
        assertThat(xhrSendResponse.getStatus(), is(NO_CONTENT));
        xhrSendResponse.release();

        final FullHttpResponse pollResponse = xhrRequest(sessionUrl, echoChannel());
        assertThat(pollResponse.getStatus(), is(OK));

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
        final String sessionUrl = "/close/222/" + UUID.randomUUID();
        final EmbeddedChannel ch = closeChannel();
        final FullHttpRequest request = httpRequest(sessionUrl + TransportType.XHR_STREAMING.path(), GET);
        ch.writeInbound(request);

        final HttpResponse response =  ch.readOutbound();
        assertThat(response.getStatus(), equalTo(OK));
        //Read and discard prelude
        ReferenceCountUtil.release(ch.readOutbound());
        // Read and discard of the open frame
        ReferenceCountUtil.release(ch.readOutbound());

        final DefaultHttpContent closeResponse = ch.readOutbound();
        assertThat(closeResponse.content().toString(UTF_8), equalTo("c[3000,\"Go away!\"]\n"));
        closeResponse.release();

        final EmbeddedChannel ch2 = closeChannel();
        final FullHttpRequest request2 = httpRequest(sessionUrl + TransportType.XHR_STREAMING.path(), GET);
        ch2.writeInbound(request2);

        final HttpResponse response2 =  ch2.readOutbound();
        assertThat(response2.getStatus(), equalTo(OK));

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
        final String sessionUrl = "/echo/222/" + UUID.randomUUID();
        final EmbeddedChannel ch = echoChannel();
        final FullHttpRequest request = httpRequest(sessionUrl + TransportType.XHR_STREAMING.path(), POST);
        ch.writeInbound(request);

        final HttpResponse response = ch.readOutbound();
        assertThat(response.getStatus(), equalTo(OK));
        //Read and discard prelude
        ReferenceCountUtil.release(ch.readOutbound());
        final DefaultHttpContent openResponse = ch.readOutbound();
        assertThat(openResponse.content().toString(UTF_8), equalTo("o\n"));
        openResponse.release();

        final EmbeddedChannel ch2 = echoChannel();
        final FullHttpRequest request2 = httpRequest(sessionUrl + TransportType.XHR_STREAMING.path(), POST);
        ch2.writeInbound(request2);

        final HttpResponse response2 =  ch2.readOutbound();
        assertThat(response2.getStatus(), equalTo(OK));
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
        final String sessionUrl = "/echo/222/" + UUID.randomUUID();
        final EmbeddedChannel ch = echoChannel();
        final FullHttpRequest request = httpRequest(sessionUrl + TransportType.XHR_STREAMING.path(), GET);
        ch.writeInbound(request);

        final HttpResponse response =  ch.readOutbound();
        assertThat(response.getStatus(), equalTo(OK));
        //Read and discard prelude
        ReferenceCountUtil.release(ch.readOutbound());
        final DefaultHttpContent openResponse = ch.readOutbound();
        assertThat(openResponse.content().toString(UTF_8), equalTo("o\n"));
        openResponse.release();

        final EmbeddedChannel ch2 = echoChannel();
        final FullHttpRequest request2 = httpRequest(sessionUrl + TransportType.XHR_STREAMING.path(), GET);
        ch2.writeInbound(request2);

        final HttpResponse response2 =  ch2.readOutbound();
        assertThat(response2.getStatus(), equalTo(OK));
        //Read and discard prelude
        ReferenceCountUtil.release(ch2.readOutbound());

        final DefaultHttpContent closeResponse2 = ch2.readOutbound();
        assertThat(closeResponse2.content().toString(UTF_8), equalTo("c[2010,\"Another connection still open\"]\n"));
        closeResponse2.release();

        assertThat(ch2.isActive(), is(false));
        ch.close();

        final EmbeddedChannel ch3 = echoChannel();
        final FullHttpRequest request3 = httpRequest(sessionUrl + TransportType.XHR_STREAMING.path(), POST);
        ch3.writeInbound(request3);

        final HttpResponse response3 =  ch3.readOutbound();
        assertThat(response3.getStatus(), equalTo(OK));
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
        final String sessionUrl = "/echo/000/" + UUID.randomUUID();

        final EmbeddedChannel ch = echoChannel();
        ch.writeInbound(httpRequest(sessionUrl + TransportType.XHR.path(), GET));

        final FullHttpResponse response = ch.readOutbound();
        assertThat(response.getStatus(), is(OK));
        assertThat(response.content().toString(UTF_8), equalTo("o\n"));
        response.release();

        final EmbeddedChannel ch2 = echoChannel();
        ch2.writeInbound(httpRequest(sessionUrl + TransportType.XHR.path(), GET));
        final FullHttpResponse response2 = ch2.readOutbound();
        assertThat(response2.content().toString(UTF_8), equalTo("c[2010,\"Another connection still open\"]\n"));
        response2.release();

        final EmbeddedChannel ch3 = echoChannel();
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
        final EmbeddedChannel ch = echoChannel();
        final FullHttpRequest request = httpGetRequest("/echo", HTTP_1_0);
        request.headers().set(CONNECTION, KEEP_ALIVE);
        ch.writeInbound(request);

        final FullHttpResponse response = ch.readOutbound();
        assertThat(response.getStatus(), is(OK));
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
                final EmbeddedChannel ch2 = echoChannel();
                ch2.writeInbound(httpGetRequest("/echo", HTTP_1_0));
                final HttpResponse newResponse = ch2.readOutbound();
                assertThat(newResponse.getStatus(), is(OK));
            }
        }
        response.release();
    }

    /*
     * Equivalent to Http10.test_streaming in sockjs-protocol-0.3.3.py.
     */
    @Test
    public void http10TestStreaming() throws Exception {
        final String sessionUrl = "/close/222/" + UUID.randomUUID();
        final EmbeddedChannel ch = closeChannel();
        final FullHttpRequest request = httpPostRequest(sessionUrl + TransportType.XHR_STREAMING.path(), HTTP_1_0);
        request.headers().set(CONNECTION, KEEP_ALIVE);
        ch.writeInbound(request);

        final HttpResponse response =  ch.readOutbound();
        assertThat(response.getStatus(), equalTo(OK));
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
        final EmbeddedChannel ch = echoChannel();
        final FullHttpRequest request = httpGetRequest("/echo", HTTP_1_1);
        request.headers().set(CONNECTION, KEEP_ALIVE);
        ch.writeInbound(request);

        final FullHttpResponse response = ch.readOutbound();
        assertThat(response.getStatus(), is(OK));
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
        ch.close();

        final EmbeddedChannel ch2 = echoChannel();
        ch2.writeInbound(httpGetRequest("/echo", HTTP_1_0));
        final FullHttpResponse newResponse = ch2.readOutbound();
        assertThat(newResponse.getStatus(), is(OK));
        newResponse.release();
    }

    /*
     * Equivalent to Http11.test_streaming in sockjs-protocol-0.3.3.py.
     */
    @Test
    public void http11TestStreaming() throws Exception {
        final String sessionUrl = "/close/222/" + UUID.randomUUID();
        final EmbeddedChannel ch = closeChannel();
        final FullHttpRequest request = httpPostRequest(sessionUrl + TransportType.XHR_STREAMING.path(), HTTP_1_1);
        request.headers().set(CONNECTION, KEEP_ALIVE);
        ch.writeInbound(request);

        final HttpResponse response =  ch.readOutbound();
        assertThat(response.getStatus(), equalTo(OK));
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
        final EmbeddedChannel ch = echoChannel();
        ch.config().setOption(SockJsChannelOption.COOKIES_NEEDED, true);
        ch.writeInbound(httpRequest("/missing"));
        final FullHttpResponse response = ch.readOutbound();
        assertThat(response.getStatus().code(), is(NOT_FOUND.code()));
        response.release();
    }

    private static void assertGoAwayResponse(final FullHttpResponse response) {
        assertThat(response.getStatus(), is(OK));
        assertThat(response.content().toString(UTF_8), equalTo("c[3000,\"Go away!\"]\n"));
    }

    private static void assertNoContent(final FullHttpResponse response) {
        assertThat(response.getStatus(), is(NO_CONTENT));
        assertThat(response.content().isReadable(), is(false));
    }

    private static void assertMessageFrameContent(final FullHttpResponse response, final String expected) {
        assertThat(response.getStatus(), is(OK));
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

    private static String serverKillerStringEsc() {
        return "\\u200c\\u200d\\u200e\\u200f\\u2028\\u2029\\u202a\\u202b\\u202c\\u202d\\u202e\\u202f\\u2060\\u2061" +
               "\\u2062\\u2063\\u2064\\u2065\\u2066\\u2067\\u2068\\u2069\\u206a\\u206b\\u206c\\u206d\\u206e\\u206f" +
               "\\ufff0\\ufff1\\ufff2\\ufff3\\ufff4\\ufff5\\ufff6\\ufff7\\ufff8\\ufff9\\ufffa\\ufffb\\ufffc\\ufffd" +
               "\\ufffe\\uffff";
    }

    private static String generateUnicodeValues(final int start, final int end) {
        final StringBuilder sb = new StringBuilder();
        for (int i = start ; i <= end; i++) {
            final String hex = Integer.toHexString(i);
            sb.append("\\u");
            switch (hex.length()) {
                case 1: {
                    sb.append("000");
                    break;
                }
                case 2: {
                    sb.append("00");
                    break;
                }
                case 3: {
                    sb.append('0');
                    break;
                }
            }
            sb.append(JsonUtil.escapeCharacters(hex.toCharArray()));
        }
        return sb.toString();
    }

    private static void assertSetCookie(final String transportPath) {
        final String serviceName = "/echo";
        final String sessionUrl = serviceName + "/abc/" + UUID.randomUUID();
        final EmbeddedChannel ch = echoChannel();
        ch.config().setOption(SockJsChannelOption.COOKIES_NEEDED, true);
        final FullHttpRequest request = httpRequest(sessionUrl + transportPath, GET);
        ch.writeInbound(request);

        final HttpResponse response = ch.readOutbound();
        assertThat(response.getStatus(), is(OK));
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
        assertThat(response.getStatus(), is(OK));
        assertThat(response.content().toString(UTF_8), equalTo("o\n"));
        response.release();
    }

    private static void verifyHeaders(final WebSocketVersion version) throws Exception {
        final EmbeddedChannel ch = webSocketEchoChannel();
        final FullHttpRequest request = HttpUtil.webSocketUpgradeRequest("/echo/websocket", version);
        ch.writeInbound(request);
        final HttpResponse response = HttpUtil.decode(ch);
        assertThat(response.getStatus(), is(SWITCHING_PROTOCOLS));
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
        final String sessionUrl = "/echo/222/" + UUID.randomUUID();
        final EmbeddedChannel ch = webSocketEchoChannel();

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
        final String sessionUrl = "/close/222/" + UUID.randomUUID();
        final EmbeddedChannel ch = webSocketCloseChannel();

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
        final String sessionUrl = "/close/222/" + UUID.randomUUID();
        final EmbeddedChannel ch = webSocketCloseChannel();

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
        assertThat(response.getStatus(), is(INTERNAL_SERVER_ERROR));
        assertThat(response.content().toString(UTF_8), equalTo("Broken JSON encoding."));
        response.release();
    }

    private static void assertPayloadExpected(final FullHttpResponse response) {
        assertThat(response.getStatus(), is(INTERNAL_SERVER_ERROR));
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
        final EmbeddedChannel ch = echoChannel();
        ch.config().setOption(SockJsChannelOption.COOKIES_NEEDED, true);

        ch.writeInbound(httpRequest("/echo" + sessionPart + TransportType.XHR.path()));
        final FullHttpResponse response = ch.readOutbound();
        assertThat(response.getStatus(), is(OK));
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
        final EmbeddedChannel ch = echoChannel();
        ch.config().setOption(SockJsChannelOption.PREFIX, "/echo");
        ch.config().setOption(SockJsChannelOption.COOKIES_NEEDED, true);
        ch.writeInbound(httpRequest("/echo" + path));

        final FullHttpResponse response = ch.readOutbound();
        assertThat(response.getStatus().code(), is(OK.code()));
        assertThat(response.headers().get(CONTENT_TYPE), equalTo("text/html; charset=UTF-8"));
        assertThat(response.headers().get(CACHE_CONTROL), equalTo("max-age=31536000, public"));
        assertThat(response.headers().get(EXPIRES), is(notNullValue()));
        verifyNoSET_COOKIE(response);
        assertThat(response.headers().get(ETAG), is(notNullValue()));
        assertThat(response.content().toString(UTF_8), equalTo(iframeHtml(ch.config().getOption(SOCKJS_URL))));
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
        final EmbeddedChannel ch = echoChannel(prefix);
        ch.config().setOption(SockJsChannelOption.COOKIES_NEEDED, true);
        ch.writeInbound(httpRequest('/' + prefix + path));
        final FullHttpResponse response = ch.readOutbound();
        assertThat(response.getStatus(), is(NOT_FOUND));
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
        final EmbeddedChannel ch = echoChannel();
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
