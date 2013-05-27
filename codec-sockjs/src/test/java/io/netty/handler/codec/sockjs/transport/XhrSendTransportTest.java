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

import static io.netty.handler.codec.sockjs.util.SockJsAsserts.assertDefaultResponseHeaders;
import static io.netty.handler.codec.sockjs.util.SockJsAsserts.assertContentType;
import static io.netty.handler.codec.sockjs.transport.HttpResponseBuilder.*;
import static org.hamcrest.CoreMatchers.equalTo;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;
import static io.netty.util.CharsetUtil.UTF_8;

import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;
import io.netty.channel.embedded.EmbeddedChannel;
import io.netty.handler.codec.http.DefaultFullHttpRequest;
import io.netty.handler.codec.http.FullHttpRequest;
import io.netty.handler.codec.http.FullHttpResponse;
import io.netty.handler.codec.http.HttpHeaderNames;
import io.netty.handler.codec.http.HttpMethod;
import io.netty.handler.codec.http.HttpResponseStatus;
import io.netty.handler.codec.http.HttpVersion;
import io.netty.handler.codec.sockjs.SockJsConfig;

import org.junit.Test;

public class XhrSendTransportTest {

    @Test (expected = NullPointerException.class)
    public void constructWithoutConfig() {
        new XhrSendTransport(null);
    }

    @Test
    public void messageReceivedNoPayload() {
        final FullHttpResponse response = processHttpRequest(requestWithBody(null));
        assertThat(response.status(), equalTo(HttpResponseStatus.INTERNAL_SERVER_ERROR));
        assertThat(response.protocolVersion(), equalTo(HttpVersion.HTTP_1_1));
        assertThat(response.content().toString(UTF_8), equalTo("Payload expected."));
        response.release();
    }

    @Test
    public void messageReceivedNoPayloadHttpVersion1_0() {
        final FullHttpResponse response = processHttpRequest(requestWithBody(null, HttpVersion.HTTP_1_0));
        assertThat(response.status(), equalTo(HttpResponseStatus.INTERNAL_SERVER_ERROR));
        assertThat(response.protocolVersion(), equalTo(HttpVersion.HTTP_1_0));
        assertThat(response.content().toString(UTF_8), equalTo("Payload expected."));
        response.release();
    }

    @Test
    public void messageReceivedSimpleString() {
        final String body = "[\"some message\"]";
        final FullHttpResponse response = processHttpRequest(requestWithBody(body));
        assertThat(response.status(), equalTo(HttpResponseStatus.NO_CONTENT));
        assertThat(response.protocolVersion(), equalTo(HttpVersion.HTTP_1_1));
        assertThat(response.content().capacity(), is(0));
        assertDefaultResponseHeaders(response, CONTENT_TYPE_PLAIN);
        response.release();
    }

    @Test
    public void messageReceivedJsonObject() {
        final String body = "[{\"firstName\": \"Fletch\"}]";
        final FullHttpResponse response = processHttpRequest(requestWithBody(body));
        assertThat(response.status(), equalTo(HttpResponseStatus.NO_CONTENT));
        assertThat(response.protocolVersion(), equalTo(HttpVersion.HTTP_1_1));
        assertThat(response.content().capacity(), is(0));
        assertDefaultResponseHeaders(response, CONTENT_TYPE_PLAIN);
        response.release();
    }

    @Test
    public void messageReceivedNoFormDataParameter() {
        final FullHttpResponse response = processHttpRequest(requestWithFormData(null));
        assertThat(response.status(), equalTo(HttpResponseStatus.INTERNAL_SERVER_ERROR));
        assertThat(response.protocolVersion(), equalTo(HttpVersion.HTTP_1_1));
        assertThat(response.content().toString(UTF_8), equalTo("Payload expected."));
        response.release();
    }

    @Test
    public void messageReceivedFormDataParameter() {
        final String data = "[\"some message\"]";
        final FullHttpResponse response = processHttpRequest(requestWithFormData(data));
        assertThat(response.status(), equalTo(HttpResponseStatus.NO_CONTENT));
        assertThat(response.protocolVersion(), equalTo(HttpVersion.HTTP_1_1));
        assertThat(response.content().capacity(), is(0));
        assertDefaultResponseHeaders(response, CONTENT_TYPE_PLAIN);
        response.release();
    }

    @Test
    public void messageReceivedInvalidJson() {
        final String data = "[\"some message";
        final FullHttpResponse response = processHttpRequest(requestWithFormData(data));
        assertThat(response.status(), equalTo(HttpResponseStatus.INTERNAL_SERVER_ERROR));
        assertThat(response.protocolVersion(), equalTo(HttpVersion.HTTP_1_1));
        assertContentType(response, CONTENT_TYPE_PLAIN);
        assertThat(response.content().toString(UTF_8), equalTo("Broken JSON encoding."));
        response.release();
    }

    private static FullHttpResponse processHttpRequest(final FullHttpRequest request) {
        final XhrSendTransport transport = new XhrSendTransport(SockJsConfig.withPrefix("/test")
                .cookiesNeeded().build());
        final EmbeddedChannel channel = new EmbeddedChannel(transport);
        channel.writeInbound(request);
        final FullHttpResponse response = channel.readOutbound();
        channel.finish();
        return response;
    }

    private static FullHttpRequest requestWithBody(final String body) {
        return requestWithBody(body, HttpVersion.HTTP_1_1);
    }

    private static FullHttpRequest requestWithBody(final String body, HttpVersion httpVersion) {
        final DefaultFullHttpRequest r = new DefaultFullHttpRequest(httpVersion, HttpMethod.GET, "/test");
        if (body != null) {
            final ByteBuf byteBuf = Unpooled.copiedBuffer(body, UTF_8);
            r.content().writeBytes(byteBuf);
            byteBuf.release();
        }
        return r;
    }

    private static FullHttpRequest requestWithFormData(final String data) {
        final DefaultFullHttpRequest r = new DefaultFullHttpRequest(HttpVersion.HTTP_1_1, HttpMethod.GET, "/test");
        r.headers().set(HttpHeaderNames.CONTENT_TYPE, CONTENT_TYPE_FORM);
        if (data == null) {
            final ByteBuf byteBuf = Unpooled.copiedBuffer("d=", UTF_8);
            r.content().writeBytes(byteBuf);
            byteBuf.release();
        } else {
            final ByteBuf byteBuf = Unpooled.copiedBuffer("d=" + data, UTF_8);
            r.content().writeBytes(byteBuf);
            byteBuf.release();
        }
        return r;
    }

}