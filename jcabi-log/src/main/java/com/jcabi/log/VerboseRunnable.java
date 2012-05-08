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

/**
 * Wrapper of {@link Runnable}, that logs all uncaught runtime exceptions.
 *
 * <p>You can use it with scheduled executor, for example:
 *
 * <pre>
 * Executors.newScheduledThreadPool(2).scheduleAtFixedRate(
 *   new VerboseRunnable(runnable), 1L, 1L, TimeUnit.SECONDS
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
 * @since 0.1.3
 */
@SuppressWarnings("PMD.DoNotUseThreads")
public final class VerboseRunnable implements Runnable {

    /**
     * Original runnable.
     */
    private final transient Runnable origin;

    /**
     * Default constructor.
     * @param runnable Runnable to wrap
     */
    public VerboseRunnable(final Runnable runnable) {
        this.origin = runnable;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    @SuppressWarnings("PMD.AvoidCatchingGenericException")
    public void run() {
        try {
            this.origin.run();
        // @checkstyle IllegalCatch (1 line)
        } catch (RuntimeException ex) {
            Logger.warn(
                this,
                "%[exception]s",
                ex
            );
            throw ex;
        // @checkstyle IllegalCatch (1 line)
        } catch (Error error) {
            Logger.error(
                this,
                "error: %[exception]s",
                error
            );
            throw error;
        }
    }

}
