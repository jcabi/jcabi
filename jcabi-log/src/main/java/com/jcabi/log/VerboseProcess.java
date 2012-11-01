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

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.util.concurrent.Callable;
import java.util.concurrent.CountDownLatch;

/**
 * Git engine.
 *
 * @author Yegor Bugayenko (yegor@jcabi.com)
 * @version $Id$
 * @since 0.5
 */
public final class VerboseProcess {

    /**
     * The process we're working with.
     */
    private final transient Process process;

    /**
     * Public ctor.
     * @param prc The process to work with
     */
    public VerboseProcess(final Process prc) {
        this.process = prc;
    }

    /**
     * Public ctor.
     * @param builder Process builder to work with
     */
    public VerboseProcess(final ProcessBuilder builder) {
        builder.redirectErrorStream(true);
        try {
            this.process = builder.start();
            this.process.getOutputStream().close();
        } catch (java.io.IOException ex) {
            throw new IllegalStateException(ex);
        }
    }

    /**
     * Get stdout from the process.
     * @return Stdout
     */
    public String stdout() {
        String stdout;
        try {
            stdout = this.waitFor();
        } catch (InterruptedException ex) {
            Thread.currentThread().interrupt();
            throw new IllegalStateException(ex);
        }
        final int code = this.process.exitValue();
        if (code != 0) {
            throw new IllegalArgumentException(
                Logger.format(
                    "Non-zero exit code %d: %[text]s",
                    code,
                    stdout
                )
            );
        }
        return stdout;
    }

    /**
     * Wait for the process to stop, logging its output in parallel.
     * @return Stdout produced by the process
     * @throws InterruptedException If interrupted in between
     */
    @SuppressWarnings("PMD.DoNotUseThreads")
    private String waitFor() throws InterruptedException {
        final BufferedReader reader = new BufferedReader(
            new InputStreamReader(this.process.getInputStream())
        );
        final CountDownLatch done = new CountDownLatch(1);
        final StringBuffer stdout = new StringBuffer();
        new Thread(
            new VerboseRunnable(
                new Callable<Void>() {
                    @Override
                    public Void call() throws Exception {
                        while (true) {
                            final String line = reader.readLine();
                            if (line == null) {
                                break;
                            }
                            Logger.info(VerboseProcess.class, ">> %s", line);
                            stdout.append(line);
                        }
                        done.countDown();
                        return null;
                    }
                },
                false
            )
        ).start();
        try {
            this.process.waitFor();
        } finally {
            done.await();
            try {
                reader.close();
            } catch (java.io.IOException ex) {
                Logger.error(this, "failed to close reader: %[exception]s", ex);
            }
        }
        return stdout.toString();
    }

}
