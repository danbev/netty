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

import static org.hamcrest.CoreMatchers.containsString;
import static org.hamcrest.CoreMatchers.equalTo;
import static org.hamcrest.CoreMatchers.nullValue;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.hasItems;
import static org.hamcrest.Matchers.is;

import org.junit.Test;

import java.util.Arrays;
import java.util.HashSet;

public class ConfigTest {

    @Test
    public void webSocketProtocols() {
        final SockJsConfig config = new DefaultSockJsConfig("/echo")
                .setWebSocketProtocol(new HashSet<String>(Arrays.asList("one", "two")));
        assertThat(config.webSocketProtocol().size(), is(2));
        assertThat(config.webSocketProtocol(), hasItems("one", "two"));
    }

    @Test
    public void webSocketProtocolsAsCSV() {
        final SockJsConfig config = new DefaultSockJsConfig("/echo")
                .setWebSocketProtocol(new HashSet<String>(Arrays.asList("one", "two")));
        assertThat(config.webSocketProtocol().size(), is(2));
        assertThat(config.webSocketProtocolCSV(), containsString("one"));
        assertThat(config.webSocketProtocolCSV(), containsString("two"));
    }

    @Test
    public void webSocketProtocolsAsCSVNoProtocols() {
        final SockJsConfig config = new DefaultSockJsConfig("/echo");
        assertThat(config.webSocketProtocol().size(), is(0));
        assertThat(config.webSocketProtocolCSV(), is(nullValue()));
    }

    @Test
    public void keystore() {
        final String keystore = "/somepath/keystore.jks";
        final String keystorePassword = "changme";
        final SockJsConfig config = new DefaultSockJsConfig("/echo")
                .setTls(true)
                .setKeyStore(keystore)
                .setKeyStorePassword(keystorePassword);
        assertThat(config.keyStore(), equalTo(keystore));
        assertThat(config.keyStorePassword(), equalTo(keystorePassword));
    }

}
