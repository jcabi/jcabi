/**
 * Copyright (c) 2012, jcabi.com
 * All rights reserved.
 *
 * Redistribution and use in source and binary forms, with or without
 * modification, are permitted provided that the following conditions
 * are met: 1) Redistributions of source code must retain the above
 * copyright notice, this list of conditions and the following
 * disclaimer. 2) Redistributions in binary form must reproduce the above
 * copyright notice, this list of conditions and the following
 * disclaimer in the documentation and/or other materials provided
 * with the distribution. 3) Neither the name of the jcabi.com nor
 * the names of its contributors may be used to endorse or promote
 * products derived from this software without specific prior written
 * permission.
 *
 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS
 * "AS IS" AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT
 * NOT LIMITED TO, THE IMPLIED WARRANTIES OF MERCHANTABILITY AND
 * FITNESS FOR A PARTICULAR PURPOSE ARE DISCLAIMED. IN NO EVENT SHALL
 * THE COPYRIGHT HOLDER OR CONTRIBUTORS BE LIABLE FOR ANY DIRECT,
 * INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR CONSEQUENTIAL DAMAGES
 * (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR
 * SERVICES; LOSS OF USE, DATA, OR PROFITS; OR BUSINESS INTERRUPTION)
 * HOWEVER CAUSED AND ON ANY THEORY OF LIABILITY, WHETHER IN CONTRACT,
 * STRICT LIABILITY, OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE)
 * ARISING IN ANY WAY OUT OF THE USE OF THIS SOFTWARE, EVEN IF ADVISED
 * OF THE POSSIBILITY OF SUCH DAMAGE.
 */
package com.jcabi.log;

import java.util.concurrent.ThreadFactory;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * Convenient {@link ThreadFactory}, that logs all uncaught exceptions.
 *
 * <p>The factory should be used together
 * with executor services from {@code java.util.concurrent} package. Without
 * these "verbose" threads your runnable tasks will not report anything to
 * console once they die because of a runtime exception, for example:
 *
 * <pre>
 * Executors.newScheduledThreadPool(2).scheduleAtFixedRate(
 *   new Runnable() {
 *     &#64;Override
 *     public void run() {
 *       // some sensitive operation that may throw
 *       // a runtime exception
 *     },
 *     1L, 1L, TimeUnit.SECONDS
 *   }
 * );
 * </pre>
 *
 * <p>The exception in this example will never be caught by nobody. It will
 * just terminate current execution of the {@link Runnable} task. Moreover,
 * it won't reach any {@link java.lang.Thread.UncaughtExceptionHandler},
 * because this
 * is how {@link java.util.concurrent.ScheduledExecutorService}
 * is behaving. This is how we solve
 * the problem with {@link VerboseThreads}:
 *
 * <pre>
 * ThreadFactory factory = new VerboseThreads();
 * Executors.newScheduledThreadPool(2, factory).scheduleAtFixedRate(
 *   new Runnable() {
 *     &#64;Override
 *     public void run() {
 *       // the same sensitive operation that may throw
 *       // a runtime exception
 *     },
 *     1L, 1L, TimeUnit.SECONDS
 *   }
 * );
 * </pre>
 *
 * <p>Now, every runtime exception that is not caught inside your
 * {@link Runnable} will be reported to log (using {@link Logger}).
 *
 * <p>This class is thread-safe.
 *
 * @author Yegor Bugayenko (yegor@jcabi.com)
 * @version $Id$
 * @since 0.1.2
 * @see VerboseRunnable
 */
@SuppressWarnings("PMD.DoNotUseThreads")
public final class VerboseThreads implements ThreadFactory {

    /**
     * Thread group.
     */
    private final transient ThreadGroup group;

    /**
     * Prefix to use.
     */
    private final transient String prefix;

    /**
     * Number of the next thread to create.
     */
    private final transient AtomicInteger number = new AtomicInteger(1);

    /**
     * Create threads as daemons?
     */
    private final transient boolean daemon;

    /**
     * Default thread priority.
     */
    private final transient int priority;

    /**
     * Default constructor ({@code "verbose"} as a prefix, threads are daemons,
     * default thread priority is {@code 1}).
     */
    public VerboseThreads() {
        this("verbose", true, 1);
    }

    /**
     * Detailed constructor, with a prefix of thread names (threads are daemons,
     * default thread priority is {@code 1}).
     * @param pfx Prefix for thread names
     */
    public VerboseThreads(final String pfx) {
        this(pfx, true, 1);
    }

    /**
     * Detailed constructor, with a prefix of thread names (threads are daemons,
     * default thread priority is {@code 1}).
     * @param type Prefix will be build from this type name
     */
    public VerboseThreads(final Object type) {
        this(type.getClass().getSimpleName(), true, 1);
    }

    /**
     * Detailed constructor, with a prefix of thread names (threads are daemons,
     * default thread priority is {@code 1}).
     * @param type Prefix will be build from this type name
     */
    public VerboseThreads(final Class<?> type) {
        this(type.getSimpleName(), true, 1);
    }

    /**
     * Detailed constructor.
     * @param pfx Prefix for thread names
     * @param dmn Threads should be daemons?
     * @param prt Default priority for all threads
     */
    public VerboseThreads(final String pfx, final boolean dmn, final int prt) {
        this.prefix = pfx;
        this.daemon = dmn;
        this.priority = prt;
        this.group = new ThreadGroup(pfx) {
            @Override
            public void uncaughtException(final Thread thread,
                final Throwable throwable) {
                Logger.warn(this, "%[exception]s", throwable);
            }
        };
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public Thread newThread(final Runnable runnable) {
        final Thread thread = new Thread(
            this.group,
            // @checkstyle AnonInnerLength (50 lines)
            new Runnable() {
                @Override
                @SuppressWarnings("PMD.AvoidCatchingGenericException")
                public void run() {
                    try {
                        runnable.run();
                    // @checkstyle IllegalCatch (1 line)
                    } catch (RuntimeException ex) {
                        Logger.warn(
                            this,
                            "%s: %[exception]s",
                            Thread.currentThread().getName(),
                            ex
                        );
                        throw ex;
                    // @checkstyle IllegalCatch (1 line)
                    } catch (Error error) {
                        Logger.error(
                            this,
                            "%s (error): %[exception]s",
                            Thread.currentThread().getName(),
                            error
                        );
                        throw error;
                    }
                }
            }
        );
        thread.setName(
            String.format(
                "%s-%d",
                this.prefix,
                this.number.getAndIncrement()
            )
        );
        thread.setDaemon(this.daemon);
        thread.setPriority(this.priority);
        return thread;
    }

}
