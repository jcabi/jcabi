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
package com.jcabi.heroku.maven.plugin;

import com.jcabi.aspects.RetryOnFailure;
import com.jcabi.log.Logger;
import com.jcabi.log.VerboseRunnable;
import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.Callable;
import java.util.concurrent.CountDownLatch;
import org.apache.commons.io.FileUtils;
import org.apache.commons.io.IOUtils;
import org.apache.commons.lang.StringUtils;

/**
 * Git engine.
 *
 * @author Yegor Bugayenko (yegor@jcabi.com)
 * @version $Id$
 * @since 0.4
 * @checkstyle ClassDataAbstractionCoupling (500 lines)
 */
final class Git {

    /**
     * Permissions to set to SSH key file.
     */
    @SuppressWarnings("PMD.AvoidUsingOctalValues")
    private static final int PERMS = 0600;

    /**
     * Default SSH location.
     */
    private static final String SSH = "/usr/bin/ssh";

    /**
     * Location of shell script.
     */
    private final transient File script;

    /**
     * Public ctor.
     * @param key Location of SSH key
     * @param temp Temp directory
     * @throws IOException If some error inside
     */
    public Git(final File key, final File temp) throws IOException {
        if (!new File(Git.SSH).exists()) {
            throw new IllegalStateException(
                String.format("SSH is not installed at '%s'", Git.SSH)
            );
        }
        final File kfile = new File(temp, "heroku.pem");
        FileUtils.copyFile(key, kfile);
        this.chmod(kfile, Git.PERMS);
        this.script = new File(temp, "git-ssh.sh");
        FileUtils.writeStringToFile(
            this.script,
            String.format(
                // @checkstyle LineLength (1 line)
                "set -x && %s -n -o UserKnownHostsFile=/dev/null -o StrictHostKeyChecking=no -i '%s' $@",
                Git.SSH,
                kfile.getAbsolutePath()
            )
        );
        this.script.setExecutable(true);
    }

    /**
     * Execute git with these arguments.
     * @param dir In which directory to run it
     * @param args Arguments to pass to it
     * @return Stdout
     */
    @RetryOnFailure
    public String exec(final File dir, final String... args) {
        final List<String> commands = new ArrayList<String>(args.length + 1);
        commands.add("git");
        for (String arg : args) {
            commands.add(arg);
        }
        Logger.info(this, "%s:...", StringUtils.join(commands, " "));
        final ProcessBuilder builder = new ProcessBuilder(commands);
        builder.directory(dir);
        builder.environment().put("GIT_SSH", this.script.getAbsolutePath());
        builder.redirectErrorStream(true);
        return this.stdout(builder);
    }

    /**
     * Get stdout from the process.
     * @param builder The process builder
     * @return Stdout
     */
    private String stdout(final ProcessBuilder builder) {
        Process process;
        String stdout;
        try {
            process = builder.start();
            process.getOutputStream().close();
            stdout = this.waitFor(process);
        } catch (java.io.IOException ex) {
            throw new IllegalStateException(ex);
        } catch (InterruptedException ex) {
            Thread.currentThread().interrupt();
            throw new IllegalStateException(ex);
        }
        final int code = process.exitValue();
        if (code != 0) {
            throw new IllegalStateException(
                Logger.format(
                    "Non-zero exit code %d: %s",
                    code,
                    stdout
                )
            );
        }
        return stdout;
    }

    /**
     * Wait for the process to stop, logging its output in parallel.
     * @param process The process to wait for
     * @return Stdout produced by the process
     * @throws InterruptedException If interrupted in between
     */
    @SuppressWarnings("PMD.DoNotUseThreads")
    private String waitFor(final Process process) throws InterruptedException {
        final BufferedReader reader = new BufferedReader(
            new InputStreamReader(process.getInputStream())
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
                            Logger.info(Git.class, ">> %s", line);
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
            process.waitFor();
        } finally {
            done.await();
            IOUtils.closeQuietly(reader);
        }
        return stdout.toString();
    }

    /**
     * Change file permissions.
     * @param file The file to change
     * @param mode Permissions to set
     * @throws IOException If some error inside
     * @see http://stackoverflow.com/questions/664432
     * @see http://stackoverflow.com/questions/1556119
     */
    private void chmod(final File file, final int mode) throws IOException {
        final ProcessBuilder builder = new ProcessBuilder(
            "chmod",
            String.format("%04o", mode),
            file.getAbsolutePath()
        );
        builder.redirectErrorStream(true);
        final Process process = builder.start();
        String stdout;
        try {
            stdout = this.waitFor(process);
        } catch (InterruptedException ex) {
            Thread.currentThread().interrupt();
            throw new IllegalStateException(ex);
        }
        final int code = process.exitValue();
        if (code != 0) {
            throw new IllegalStateException(
                Logger.format(
                    "Failed to chmod('%s', %04o)",
                    file,
                    mode,
                    stdout
                )
            );
        }
    }

}
