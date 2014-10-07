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
package io.netty.handler.codec.sockjs.util;

import io.netty.channel.Channel;
import io.netty.channel.ChannelConfig;
import io.netty.channel.ChannelHandler;
import io.netty.handler.codec.sockjs.util.StubEmbeddedEventLoop.SchedulerExecutor;

public class TestEmbeddedChannel extends AbstractTestEmbeddedChannel {

    private final ChannelConfig config;

    public TestEmbeddedChannel(final Channel parent, ChannelConfig config) {
        super(parent);
        this.config = config;
    }

    public TestEmbeddedChannel(ChannelConfig config, ChannelHandler... handlers) {
        super(handlers);
        this.config = config;
    }

    @Override
    protected AbstractTestUnsafe createTestUnsafe(AbstractUnsafe delegate) {
        return new AbstractTestUnsafe(delegate) {
            @Override
            public SchedulerExecutor createSchedulerExecutor() {
                return new SuccessSchedulerExecutor();
            }
        };
    }

    @Override
    public ChannelConfig config() {
        if (config == null) {
            return super.config();
        }
        return config;
    }

    public void removeLastInboundHandler() {
        pipeline().remove("EmbeddedChannel$LastInboundHandler#0");
    }

}

    /*
    public TestEmbeddedChannel(final Channel parent, final ChannelConfig config) {
        this.parent = parent;
        this.config = config;
        // remove EmbeddedChannels LastInboundHandler channel handler or it will simply store all messages written.
        pipeline().remove("EmbeddedChannel$LastInboundHandler#0");
    }

    @Override
    public ChannelConfig config() {
        return parent == null ? super.config() : config;
    }

    @Override
    public Channel parent() {
        return parent;
    }

}
    */
