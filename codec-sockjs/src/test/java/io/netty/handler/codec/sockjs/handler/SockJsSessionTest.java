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
package io.netty.handler.codec.sockjs.handler;

import static org.hamcrest.CoreMatchers.equalTo;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;

import io.netty.handler.codec.sockjs.DefaultSockJsConfig;
import io.netty.handler.codec.sockjs.handler.SessionState.State;

import org.junit.Test;

import java.util.List;

public class SockJsSessionTest {

    @Test
    public void setState() throws Exception {
        final SockJsSession session = new SockJsSession("123", new DefaultSockJsConfig("/echo"));
        session.setState(State.OPEN);
        assertThat(session.getState(), is(State.OPEN));
    }

    @Test
    public void onOpen() throws Exception {
        final SockJsSession sockJSSession = new SockJsSession("123", new DefaultSockJsConfig("/echo"));
        sockJSSession.onOpen();
        assertThat(sockJSSession.getState(), is(State.OPEN));
    }

    @Test
    public void onClose() throws Exception {
        final SockJsSession sockJSSession = new SockJsSession("123", new DefaultSockJsConfig("/echo"));
        sockJSSession.onClose();
    }

    @Test
    public void addMessage() throws Exception {
        final SockJsSession sockJSSession = new SockJsSession("123", new DefaultSockJsConfig("/echo"));
        sockJSSession.addMessage("hello");
        assertThat(sockJSSession.getAllMessages().size(), is(1));
    }

    @Test
    public void addMessages() throws Exception {
        final SockJsSession sockJSSession = new SockJsSession("123", new DefaultSockJsConfig("/echo"));
        sockJSSession.addMessages(new String[]{"hello", "world"});
        final List<String> messages = sockJSSession.getAllMessages();
        assertThat(messages.size(), is(2));
        assertThat(messages.get(0), equalTo("hello"));
        assertThat(messages.get(1), equalTo("world"));
        assertThat(sockJSSession.getAllMessages().size(), is(0));
    }

}
