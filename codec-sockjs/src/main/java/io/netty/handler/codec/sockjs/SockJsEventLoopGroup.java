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

import io.netty.channel.EventLoop;
import io.netty.channel.nio.NioEventLoop;
import io.netty.channel.nio.NioEventLoopGroup;

import java.nio.channels.spi.SelectorProvider;
import java.util.concurrent.Executor;
import java.util.concurrent.ThreadFactory;

/**
 * This EventLoopGroup implementation will create SockJSEventLoop instances
 * which are referred to as children.
 *
 * It is from this EventLoopGroup that channels will get their EventLoop.
 *
 */
public class SockJsEventLoopGroup extends NioEventLoopGroup {

    public SockJsEventLoopGroup() {
    }

    public SockJsEventLoopGroup(final int nThreads) {
        super(nThreads);
    }

    public SockJsEventLoopGroup(final int nThreads, final ThreadFactory threadFactory) {
        super(nThreads, threadFactory);
    }

    public SockJsEventLoopGroup(final int nThreads, final Executor executor) {
        super(nThreads, executor);
    }

    public SockJsEventLoopGroup(final int nThreads, final ThreadFactory threadFactory,
                                final SelectorProvider selectorProvider) {
        super(nThreads, threadFactory, selectorProvider);
    }

    public SockJsEventLoopGroup(final int nThreads, final Executor executor, final SelectorProvider selectorProvider) {
        super(nThreads, executor, selectorProvider);
    }

    @Override
    protected SockJsEventLoop newChild(final Executor executor, final Object... args) throws Exception {
        final NioEventLoop eventLoop = (NioEventLoop) super.newChild(executor, args);
        return new SockJsEventLoop(eventLoop);
    }
}
