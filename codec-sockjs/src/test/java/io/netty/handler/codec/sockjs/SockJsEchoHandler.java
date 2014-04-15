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
import io.netty.channel.ChannelPromise;
import io.netty.channel.SimpleChannelInboundHandler;
import io.netty.handler.codec.sockjs.handler.SessionHandler.Event;
import io.netty.util.internal.logging.InternalLogger;
import io.netty.util.internal.logging.InternalLoggerFactory;

@ChannelHandler.Sharable
public class SockJsEchoHandler extends SimpleChannelInboundHandler<String> {

    private static final InternalLogger logger = InternalLoggerFactory.getInstance(SockJsEchoHandler.class);

    @Override
    public void userEventTriggered(final ChannelHandlerContext ctx, final Object evt) throws Exception {
        if (evt == Event.ON_SESSION_OPEN) {
            //logger.info(ctx.channel().parent().config().getOption(SockJsChannelOption.PREFIX) + " Connected");
        } else {
            super.userEventTriggered(ctx, evt);
        }
    }

    @Override
    protected void messageReceived(final ChannelHandlerContext ctx, final String msg) throws Exception {
        //logger.info(ctx.channel().parent().config().getOption(SockJsChannelOption.PREFIX) + " Echoing: " + msg);
        ctx.writeAndFlush(msg);
    }

    @Override
    public void close(final ChannelHandlerContext ctx, final ChannelPromise promise) throws Exception {
        //logger.info(ctx.channel().parent().config().getOption(SockJsChannelOption.PREFIX) + " closing");
        super.close(ctx, promise);
    }
}
