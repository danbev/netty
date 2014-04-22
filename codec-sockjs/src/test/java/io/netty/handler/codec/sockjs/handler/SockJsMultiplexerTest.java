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
package io.netty.handler.codec.sockjs.handler;

import io.netty.handler.codec.http.DefaultHttpRequest;
import io.netty.handler.codec.http.HttpRequest;
import org.junit.Test;

import static io.netty.handler.codec.http.HttpMethod.GET;
import static io.netty.handler.codec.http.HttpVersion.HTTP_1_1;
import static org.hamcrest.CoreMatchers.equalTo;
import static org.hamcrest.MatcherAssert.assertThat;

public class SockJsMultiplexerTest {

    @Test
    public void simplePrefix() {
        assertThat(SockJsMultiplexer.requestPrefix(request("/echo")), equalTo("/echo"));
    }

    @Test
    public void noPrefixSlash() {
        assertThat(SockJsMultiplexer.requestPrefix(request("echo")), equalTo("/echo"));
    }

    @Test
    public void withSubPaths() {
        assertThat(SockJsMultiplexer.requestPrefix(request("/echo/000/000/")), equalTo("/echo"));
    }

    private static HttpRequest request(final String path) {
        return new DefaultHttpRequest(HTTP_1_1, GET, path);
    }

}
