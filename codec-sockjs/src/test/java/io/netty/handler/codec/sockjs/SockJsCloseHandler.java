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

import io.netty.channel.ChannelHandler;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.SimpleChannelInboundHandler;
import io.netty.handler.codec.sockjs.handler.SessionHandler.Event;

/**
 * Test service required by
 * <a href="http://sockjs.github.io/sockjs-protocol/sockjs-protocol-0.3.3.html">sockjs-protocol</a>
 * which will close the session as soon as a message is received.
 */
@ChannelHandler.Sharable
public final class SockJsCloseHandler extends SimpleChannelInboundHandler<String> {

    @Override
    public void userEventTriggered(final ChannelHandlerContext ctx, final Object evt) throws Exception {
        if (evt == Event.ON_SESSION_OPEN) {
            ctx.pipeline().fireUserEventTriggered(Event.CLOSE_SESSION);
        } else {
            super.userEventTriggered(ctx, evt);
        }
    }

    @Override
    public void messageReceived(final ChannelHandlerContext ctx, final String msg) {
        // noop
    }

}
