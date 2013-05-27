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

import io.netty.channel.Channel;
import io.netty.channel.ChannelFuture;
import io.netty.channel.ChannelFutureListener;
import io.netty.channel.ChannelHandlerContext;
import io.netty.handler.codec.sockjs.protocol.HeartbeatFrame;
import io.netty.handler.codec.sockjs.protocol.MessageFrame;
import io.netty.util.internal.StringUtil;
import io.netty.util.internal.logging.InternalLogger;
import io.netty.util.internal.logging.InternalLoggerFactory;

import java.util.List;
import java.util.concurrent.ConcurrentMap;

/**
 * A streaming session state handles session interactions which has a persistent
 * connection to the client.
 *
 * This connection/channel is created when the first request from the client is made. Upon
 * opening the session any messages will be flushed. This could happen if the client connection
 * is dropped but the session still active and the SockJS service has generated messages but
 * has no where to send them. In this case the messages will be queue up and it is these
 * message that get flushed.
 */
final class StreamingSessionState extends AbstractTimersSessionState {

    private static final InternalLogger logger = InternalLoggerFactory.getInstance(StreamingSessionState.class);
    private static final HeartbeatFrame HEARTBEAT_FRAME = new HeartbeatFrame();

    StreamingSessionState(final ConcurrentMap<String, SockJsSession> sessions, final SockJsSession session) {
        super(sessions, session);
    }

    @Override
    public void onOpen(final ChannelHandlerContext ctx) {
        super.onOpen(ctx);
        flushMessages();
    }

    @Override
    public ChannelHandlerContext getSendingContext() {
        return sockJsSession().connectionContext();
    }

    private void flushMessages() {
        final Channel channel = sockJsSession().connectionContext().channel();
        if (channel.isActive() && channel.isRegistered()) {
            final List<String> allMessages = sockJsSession().getAllMessages();
            if (allMessages.isEmpty()) {
                return;
            }

            final MessageFrame messageFrame = new MessageFrame(allMessages);
            logger.debug("flushing [{}]", messageFrame);
            channel.writeAndFlush(messageFrame).addListener(new ChannelFutureListener() {
                @Override
                public void operationComplete(final ChannelFuture future) throws Exception {
                    if (!future.isSuccess()) {
                        final SockJsSession sockJsSession = sockJsSession();
                        for (String msg : allMessages) {
                            sockJsSession.addMessage(msg);
                        }
                    }
                }
            });
        }
    }

    @Override
    public void onSockJSServerInitiatedClose() {
        super.onSockJSServerInitiatedClose();
        final ChannelHandlerContext context = sockJsSession().connectionContext();
        if (context != null) { //could be null if the request is aborted, for example due to missing callback.
            logger.debug("Will close session connectionContext " + sockJsSession().connectionContext());
            context.close();
        }
    }

    @Override
    public boolean isInUse() {
        return sockJsSession().connectionContext().channel().isActive();
    }

    @Override
    Runnable createHeartbeater(final ChannelHandlerContext ctx) {
        return new StreamingHeartbeater(ctx);
    }

    @Override
    public String toString() {
        return StringUtil.simpleClassName(this);
    }

    private class StreamingHeartbeater implements Runnable {

        private final ChannelHandlerContext ctx;

        StreamingHeartbeater(final ChannelHandlerContext ctx) {
            this.ctx = ctx;
        }

        @Override
        public void run() {
            if (ctx.channel().isActive() && ctx.channel().isRegistered()) {
                if (logger.isDebugEnabled()) {
                    logger.debug("Sending heartbeat for {}", sockJsSession());
                }
                ctx.channel().writeAndFlush(HEARTBEAT_FRAME.duplicate());
            } else {
                logger.error("Could not run heartbeat as channel {} is not active", ctx);
            }
        }
    }

}
