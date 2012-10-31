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
package com.jcabi.ssl.maven.plugin;

import com.jcabi.log.Logger;
import java.io.File;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.List;
import org.apache.commons.io.IOUtils;

/**
 * Keytool abstraction.
 *
 * @author Yegor Bugayenko (yegor@jcabi.com)
 * @version $Id$
 * @since 0.5
 */
final class Keytool {

    /**
     * Keystore location.
     */
    private final transient File keystore;

    /**
     * Keystore password.
     */
    private final transient String password;

    /**
     * Public ctor.
     * @param store The location of keystore
     * @param pwd The password
     */
    public Keytool(final File store, final String pwd) {
        this.keystore = store;
        this.password = pwd;
    }

    /**
     * List content of the keystore.
     * @return The content of it
     * @throws IOException If fails
     */
    public String list() throws IOException {
        final Process process = Keytool.process(
            "-list",
            // @checkstyle MultipleStringLiterals (1 line)
            "-storepass",
            this.password,
            // @checkstyle MultipleStringLiterals (1 line)
            "-keystore",
            this.keystore.getAbsolutePath()
        );
        return this.waitFor(process);
    }

    /**
     * Generate key.
     * @throws IOException If fails
     */
    public void genkey() throws IOException {
        final long start = System.currentTimeMillis();
        final Process process = Keytool.process(
            "-genkey",
            "-alias",
            "localhost",
            "-keyalg",
            "RSA",
            "-storepass",
            this.password,
            "-keypass",
            this.password,
            "-keystore",
            this.keystore.getAbsolutePath()
        );
        final PrintWriter writer = new PrintWriter(
            new OutputStreamWriter(process.getOutputStream())
        );
        writer.print("localhost\n");
        writer.print("ACME Co.\n");
        writer.print("software developers\n");
        writer.print("San Francisco\n");
        writer.print("California\n");
        writer.print("US\n");
        writer.print("yes\n");
        writer.close();
        this.waitFor(process);
        Logger.info(
            this,
            "Keystore created in '%s' in %[ms]s",
            this.keystore,
            System.currentTimeMillis() - start
        );
    }

    /**
     * Wait for this process to finish and validate its output.
     * @param process The process to wait for
     * @return Stdout of the process
     * @throws IOException If fails
     */
    private String waitFor(final Process process) throws IOException {
        try {
            process.waitFor();
        } catch (InterruptedException ex) {
            Thread.currentThread().interrupt();
            throw new IllegalStateException(ex);
        }
        final int code = process.exitValue();
        final String stdout = IOUtils.toString(process.getInputStream());
        if (code != 0) {
            throw new IllegalStateException(
                Logger.format(
                    "Non-zero exit code #%d: %s",
                    code,
                    stdout
                )
            );
        }
        return stdout;
    }

    /**
     * Create process.
     * @param args Arguments
     * @return Process just created and started
     * @throws IOException If fails
     */
    private static Process process(final String... args) throws IOException {
        final List<String> cmds = new ArrayList<String>(args.length + 1);
        cmds.add(
            String.format(
                "%s/bin/keytool",
                System.getProperty("java.home")
            )
        );
        for (String arg : args) {
            cmds.add(arg);
        }
        final ProcessBuilder builder = new ProcessBuilder(cmds);
        builder.redirectErrorStream(true);
        return builder.start();
    }

}
