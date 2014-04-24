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
import io.netty.handler.codec.sockjs.SockJsServiceConfig;
import io.netty.handler.codec.sockjs.channel.SockJsServerSocketChannelAdapter;
import io.netty.handler.codec.sockjs.SockJsService;
import io.netty.util.internal.logging.InternalLogger;
import io.netty.util.internal.logging.InternalLoggerFactory;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static io.netty.handler.codec.sockjs.util.TransportUtil.writeNotFoundResponse;

/**
 * A ChannelHandler responsible for adding the ChannelHandlers to the pipeline
 * for the requested SockJS service.
 * <p>
 */
public class SockJsMultiplexer extends ChannelHandlerAdapter {

    private static final InternalLogger logger = InternalLoggerFactory.getInstance(SockJsMultiplexer.class);
    private static final Pattern PREFIX = Pattern.compile("/?([^/]*)");

    @Override
    public void channelRead(final ChannelHandlerContext ctx, final Object msg) throws Exception {
        if (msg instanceof HttpRequest) {
            final HttpRequest request = (HttpRequest) msg;
            final SockJsService sockJsService = sockJsServiceFor(request, ctx);
            if (sockJsService == null) {
                writeNotFoundResponse(request, ctx);
                return;
            }
            addHandlerForSockJsService(sockJsService, ctx);
            addCorsAndSockJsHandler(sockJsService, ctx);
            removeSockJsMultiplexer(ctx);
        }
        ctx.fireChannelRead(msg);
    }

    private static void addCorsAndSockJsHandler(final SockJsService sockJsService, final ChannelHandlerContext ctx) {
        // The 'prefix' of a SockJS service is configured using the 'option' method of ServerBootstrap in contrast
        // to the rest of the options that use the 'childOption' method call. The SockJsHandler
        // needs to access the prefix so we set it here.
        final SockJsServiceConfig sockJsConfig = (SockJsServiceConfig) ctx.channel().config();
        sockJsConfig.setPrefix(sockJsService.prefix());

        ctx.pipeline().addAfter(ctx.name(), "sockjs", new SockJsHandler(sockJsConfig));
        ctx.pipeline().addAfter(ctx.name(), "cors", new CorsHandler(sockJsConfig.corsConfig()));
    }

    private static void addHandlerForSockJsService(SockJsService sockJsService, ChannelHandlerContext ctx) {
        ctx.pipeline().addAfter(ctx.name(), "childHandler", sockJsService.childChannelInitializer());
        ctx.fireChannelRegistered();
        ctx.fireChannelRead(ctx.channel());
        // The childChannelInitializer added above is an instance of ChannelInitializer added by the ServerBootstrap
        // While the childHandler is removed by the normal ChannelInitialier's init method, the
        // ServerBootstrap.BootstrapAcceptor does not remove itself from the pipeline so we explicitely remove
        // it here.
        ctx.pipeline().remove("ServerBootstrap$ServerBootstrapAcceptor#0");
    }

    private void removeSockJsMultiplexer(final ChannelHandlerContext ctx) {
        ctx.pipeline().remove(this);
    }

    private static SockJsService sockJsServiceFor(final HttpRequest request, final ChannelHandlerContext ctx) {
        final SockJsServerSocketChannelAdapter sockJsServerChannel =
                (SockJsServerSocketChannelAdapter) ctx.channel().parent();
        return sockJsServerChannel.serviceFor(requestPrefix(request));
    }

    static String requestPrefix(final HttpRequest request) {
        final Matcher m = PREFIX.matcher(new QueryStringDecoder(request.getUri()).path());
        return m.find() ? '/' + m.group(1) : "not found";
    }

    @Override
    public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause) throws Exception {
        logger.error("Exception caught: ", cause);
        super.exceptionCaught(ctx, cause);
    }
}
