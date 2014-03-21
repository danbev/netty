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

import io.netty.channel.ChannelHandlerInvoker;
import io.netty.channel.EventLoop;
import io.netty.channel.EventLoopGroup;
import io.netty.channel.nio.NioEventLoop;
import io.netty.util.concurrent.EventExecutor;
import io.netty.util.concurrent.Future;
import io.netty.util.concurrent.ProgressivePromise;
import io.netty.util.concurrent.Promise;
import io.netty.util.concurrent.ScheduledFuture;

import java.util.Collection;
import java.util.List;
import java.util.Set;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

public class SockJsEventLoop implements EventLoop {

    private final NioEventLoop delegate;

    public SockJsEventLoop(final NioEventLoop delegate) {
        this.delegate = delegate;
    }

    protected NioEventLoop delegate() {
        return delegate;
    }

    @Override
    public EventLoopGroup parent() {
        return delegate.parent();
    }

    @Override
    public boolean inEventLoop() {
        return delegate.inEventLoop();
    }

    @Override
    public boolean inEventLoop(final Thread thread) {
        return delegate.inEventLoop(thread);
    }

    @Override
    public <V> Promise<V> newPromise() {
        return delegate.newPromise();
    }

    @Override
    public <V> ProgressivePromise<V> newProgressivePromise() {
        return delegate.newProgressivePromise();
    }

    @Override
    public <V> Future<V> newSucceededFuture(final V result) {
        return delegate.newSucceededFuture(result);
    }

    @Override
    public <V> Future<V> newFailedFuture(final Throwable cause) {
        return delegate.newFailedFuture(cause);
    }

    @Override
    public Future<?> submit(final Runnable task) {
        return delegate.submit(task);
    }

    @Override
    public <T> List<java.util.concurrent.Future<T>> invokeAll(final Collection<? extends Callable<T>> tasks)
            throws InterruptedException {
        return delegate.invokeAll(tasks);
    }

    @Override
    public <T> List<java.util.concurrent.Future<T>> invokeAll(final Collection<? extends Callable<T>> tasks,
                                                              final long timeout, final TimeUnit unit)
            throws InterruptedException {
        return delegate.invokeAll(tasks, timeout, unit);
    }

    @Override
    public <T> T invokeAny(final Collection<? extends Callable<T>> tasks) throws InterruptedException,
            ExecutionException {
        return delegate.invokeAny(tasks);
    }

    @Override
    public <T> T invokeAny(final Collection<? extends Callable<T>> tasks, final long timeout, final TimeUnit unit)
            throws InterruptedException, ExecutionException, TimeoutException {
        return delegate.invokeAny(tasks, timeout, unit);
    }

    @Override
    public <T> Future<T> submit(final Runnable task, final T result) {
        return delegate.submit(task, result);
    }

    @Override
    public <T> Future<T> submit(final Callable<T> task) {
        return delegate.submit(task);
    }

    @Override
    public ScheduledFuture<?> schedule(final Runnable command, final long delay, final TimeUnit unit) {
        return delegate.schedule(command, delay, unit);
    }

    @Override
    public <V> ScheduledFuture<V> schedule(final Callable<V> callable, final long delay, final TimeUnit unit) {
        return delegate.schedule(callable, delay, unit);
    }

    @Override
    public ScheduledFuture<?> scheduleAtFixedRate(final Runnable command, final long initialDelay, final long period,
                                                  final TimeUnit unit) {
        return delegate.scheduleAtFixedRate(command, initialDelay, period, unit);
    }

    @Override
    public ScheduledFuture<?> scheduleWithFixedDelay(final Runnable command, final long initialDelay, final long delay,
                                                     final TimeUnit unit) {
        return delegate.scheduleWithFixedDelay(command, initialDelay, delay, unit);
    }

    @Override
    public boolean isShuttingDown() {
        return delegate.isShuttingDown();
    }

    @Override
    public Future<?> shutdownGracefully() {
        return delegate.shutdownGracefully();
    }

    @Override
    public Future<?> shutdownGracefully(final long quietPeriod, final long timeout, final TimeUnit unit) {
        return delegate.shutdownGracefully(quietPeriod, timeout, unit);
    }

    @Override
    public Future<?> terminationFuture() {
        return delegate.terminationFuture();
    }

    @Override
    @Deprecated
    public void shutdown() {
        delegate.shutdown();
    }

    @Override
    @Deprecated
    public List<Runnable> shutdownNow() {
        return delegate.shutdownNow();
    }

    @Override
    public boolean isShutdown() {
        return delegate.isShutdown();
    }

    @Override
    public boolean isTerminated() {
        return delegate.isTerminated();
    }

    @Override
    public boolean awaitTermination(final long timeout, final TimeUnit unit) throws InterruptedException {
        return delegate.awaitTermination(timeout, unit);
    }

    @Override
    public EventLoop next() {
        return delegate.next();
    }

    @Override
    public <E extends EventExecutor> Set<E> children() {
        return delegate.children();
    }

    @Override
    public ChannelHandlerInvoker asInvoker() {
        return delegate.asInvoker();
    }

    @Override
    public void execute(final Runnable command) {
        delegate.execute(command);
    }
}
