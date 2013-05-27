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
package io.netty.handler.codec.sockjs;

import static org.hamcrest.CoreMatchers.equalTo;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.hasItems;
import static org.hamcrest.Matchers.is;

import org.junit.Test;

public class SockJsConfigTest {

    @Test
    public void webSocketProtocols() {
        final SockJsConfig config = SockJsConfig.withPrefix("/echo").webSocketProtocols("one", "two").build();
        assertThat(config.webSocketProtocols().size(), is(2));
        assertThat(config.webSocketProtocols(), hasItems("one", "two"));
    }

    @Test (expected = IllegalStateException.class)
    public void tlsWithoutKeystoreOrPassword() {
        SockJsConfig.withPrefix("/echo").tls(true).build();
    }

    @Test (expected = IllegalStateException.class)
    public void tlsWithoutKeystore() {
        SockJsConfig.withPrefix("/echo").tls(true).keyStorePassword("changeme").build();
    }

    @Test (expected = IllegalStateException.class)
    public void tlsWithoutKeystorePassword() {
        SockJsConfig.withPrefix("/echo").tls(true).keyStore("/test.jks").build();
    }

    @Test
    public void keystore() {
        final String keystore = "/somepath/keystore.jks";
        final String keystorePassword = "changme";
        final SockJsConfig config = SockJsConfig.withPrefix("/echo")
                .tls(true)
                .keyStore(keystore)
                .keyStorePassword(keystorePassword)
                .build();
        assertThat(config.keyStore(), equalTo(keystore));
        assertThat(config.keyStorePassword(), equalTo(keystorePassword));
    }

    @Test
    public void defaultSockJsUrlCdn() {
        final SockJsConfig config = SockJsConfig.withPrefix("/echo").build();
        assertThat(config.sockJsUrl(), equalTo("http://cdn.jsdelivr.net/sockjs/0.3.4/sockjs.min.js"));
    }

}