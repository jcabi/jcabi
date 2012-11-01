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

import java.util.concurrent.Callable;

/**
 * Wrapper of {@link Runnable}, that logs all uncaught runtime exceptions.
 *
 * <p>You can use it with scheduled executor, for example:
 *
 * <pre> Executors.newScheduledThreadPool(2).scheduleAtFixedRate(
 *   new VerboseRunnable(runnable, true), 1L, 1L, TimeUnit.SECONDS
 * );</pre>
 *
 * <p>Now, every runtime exception that is not caught inside your
 * {@link Runnable} will be reported to log (using {@link Logger}).
 * Two-arguments constructor can be used when you need to instruct the class
 * about what to do with the exception: either swallow it or escalate.
 * Sometimes it's very important to swallow exceptions. Otherwise an entire
 * thread may get stuck (like in the example above).
 *
 * <p>This class is thread-safe.
 *
 * @author Yegor Bugayenko (yegor@jcabi.com)
 * @version $Id$
 * @since 0.1.3
 * @see VerboseThreads
 */
@SuppressWarnings("PMD.DoNotUseThreads")
public final class VerboseRunnable implements Runnable {

    /**
     * Original runnable.
     */
    private final transient Runnable origin;

    /**
     * Swallow exceptions?
     */
    private final transient boolean swallow;

    /**
     * Default constructor, doesn't swallow exceptions.
     * @param runnable Runnable to wrap
     */
    public VerboseRunnable(final Runnable runnable) {
        this(runnable, false);
    }

    /**
     * Default constructor, with configurable behavior for exceptions.
     * @param runnable Runnable to wrap
     * @param swlw Shall we swallow exceptions ({@code TRUE}) or re-throw
     *  ({@code FALSE})? Exception swallowing means that {@link #run()}
     *  will never throw any exceptions (in any case all exceptions are logged
     *  using {@link Logger}.
     * @since 0.1.4
     */
    public VerboseRunnable(final Runnable runnable, final boolean swlw) {
        this.origin = runnable;
        this.swallow = swlw;
    }

    /**
     * Default constructor.
     * @param callable Callable to wrap
     * @param swlw Shall we swallow exceptions ({@code TRUE}) or re-throw
     *  ({@code FALSE})? Exception swallowing means that {@link #run()}
     *  will never throw any exceptions (in any case all exceptions are logged
     *  using {@link Logger}.
     * @since 0.1.10
     */
    @SuppressWarnings("PMD.AvoidCatchingGenericException")
    public VerboseRunnable(final Callable<?> callable, final boolean swlw) {
        this.origin = new Runnable() {
            @Override
            public void run() {
                try {
                    callable.call();
                // @checkstyle IllegalCatch (1 line)
                } catch (Exception ex) {
                    throw new IllegalArgumentException(ex);
                }
            }
            @Override
            public String toString() {
                return callable.toString();
            }
        };
        this.swallow = swlw;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public String toString() {
        return this.origin.toString();
    }

    /**
     * {@inheritDoc}
     *
     * <p>We catch {@link RuntimeException} and {@link Error} here. All other
     * types of exceptions are "checked exceptions" and won't be thrown out
     * of {@link Runnable#run()} method.
     */
    @Override
    @SuppressWarnings("PMD.AvoidCatchingGenericException")
    public void run() {
        try {
            this.origin.run();
        // @checkstyle IllegalCatch (1 line)
        } catch (RuntimeException ex) {
            if (this.swallow) {
                Logger.warn(
                    this,
                    "swallowed exception: %[exception]s",
                    ex
                );
            } else {
                Logger.warn(
                    this,
                    "escalated exception: %[exception]s",
                    ex
                );
                throw ex;
            }
        // @checkstyle IllegalCatch (1 line)
        } catch (Error error) {
            if (this.swallow) {
                Logger.error(
                    this,
                    "swallowed error: %[exception]s",
                    error
                );
            } else {
                Logger.error(
                    this,
                    "escalated error: %[exception]s",
                    error
                );
                throw error;
            }
        }
    }

}
