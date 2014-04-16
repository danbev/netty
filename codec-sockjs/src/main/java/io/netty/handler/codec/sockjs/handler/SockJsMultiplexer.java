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

import io.netty.channel.ChannelHandlerAdapter;
import io.netty.channel.ChannelHandlerContext;
import io.netty.handler.codec.http.HttpRequest;
import io.netty.handler.codec.http.QueryStringDecoder;
import io.netty.handler.codec.http.cors.CorsHandler;
import io.netty.handler.codec.sockjs.SockJsServerSocketChannelAdapter;
import io.netty.handler.codec.sockjs.SockJsService;
import io.netty.handler.codec.sockjs.SockJsSocketChannelConfig;
import io.netty.util.internal.logging.InternalLogger;
import io.netty.util.internal.logging.InternalLoggerFactory;

import static io.netty.handler.codec.sockjs.util.TransportUtil.writeNotFoundResponse;

/**
 * A ChannelHandler responsible for adding the ChannelHandlers to the pipeline
 * for the requested SockJS service.
 * <p>
 */
public class SockJsMultiplexer extends ChannelHandlerAdapter {

    private static final InternalLogger logger = InternalLoggerFactory.getInstance(SockJsMultiplexer.class);

    @Override
    public void channelRead(final ChannelHandlerContext ctx, final Object msg) throws Exception {
        if (msg instanceof HttpRequest) {
            final HttpRequest request = (HttpRequest) msg;
            final SockJsServerSocketChannelAdapter sockJsServerChannel =
                    (SockJsServerSocketChannelAdapter) ctx.channel().parent();
            final SockJsService sockJsService = sockJsServerChannel.serviceFor(requestPrefix(request));
            if (sockJsService == null) {
                writeNotFoundResponse(request, ctx);
                return;
            }

            ctx.pipeline().addAfter(ctx.name(), "acceptor", sockJsService.childChannelInitializer());
            ctx.fireChannelRegistered();
            ctx.fireChannelRead(ctx.channel());
            ctx.pipeline().remove("ServerBootstrap$ServerBootstrapAcceptor#0");
            final SockJsSocketChannelConfig sockJsConfig = (SockJsSocketChannelConfig) ctx.channel().config();
            sockJsConfig.setPrefix(sockJsService.prefix());

            if (ctx.pipeline().get("sockjs") == null) {
                ctx.pipeline().addAfter(ctx.name(), "sockjs", new SockJsHandler(sockJsConfig));
                ctx.pipeline().addAfter(ctx.name(), "cors", new CorsHandler(sockJsConfig.corsConfig()));
            } else {
                ctx.pipeline().replace("sockjs", "sockjs", new SockJsHandler(sockJsConfig));
                ctx.pipeline().replace("cors", "cors", new CorsHandler(sockJsConfig.corsConfig()));
            }
        }
        ctx.fireChannelRead(msg);
    }

    private static String requestPrefix(final HttpRequest request) {
        final String path = new QueryStringDecoder(request.getUri()).path();
        final String[] split = path.split("/");
        if (path.charAt(0) == '/') {
            return '/' + split[1];
        } else {
            return '/' + split[0];
        }
    }

    @Override
    public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause) throws Exception {
        logger.error("Exception caught: ", cause);
        super.exceptionCaught(ctx, cause);
    }
}
