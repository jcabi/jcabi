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
import org.apache.commons.io.IOUtils;
import org.apache.maven.artifact.Artifact;
import org.apache.maven.artifact.DefaultArtifact;
import org.apache.maven.plugin.AbstractMojo;
import org.apache.maven.plugin.MojoFailureException;
import org.apache.maven.project.MavenProject;
import org.jfrog.maven.annomojo.annotations.MojoGoal;
import org.jfrog.maven.annomojo.annotations.MojoParameter;
import org.jfrog.maven.annomojo.annotations.MojoPhase;
import org.slf4j.impl.StaticLoggerBinder;

/**
 * Keystore abstract.
 *
 * @author Yegor Bugayenko (yegor@jcabi.com)
 * @version $Id$
 * @since 0.5
 */
final class Keystore {

    /**
     * Unique password of it.
     */
    private final transient String password;

    /**
     * The password that identifies its uniqueness.
     * @param pwd The password
     */
    public Keystore(final String pwd) {
        this.password = pwd;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public String toString() {
        final String[] names = new String[] {
            "javax.net.ssl.keyStore",
            "javax.net.ssl.keyStorePassword",
            "javax.net.ssl.trustStore",
            "javax.net.ssl.trustStorePassword",
        };
        final StringBuilder text = new StringBuilder();
        text.append('[');
        for (String name : names) {
            if (text.length() > 1) {
                text.append(", ");
            }
            text.append(name).append("=");
            final String value = System.getProperty(name);
            if (name == null) {
                text.append("NULL");
            } else {
                text.append(value);
            }
        }
        text.append(']');
        return text.toString();
    }

    /**
     * Is it active now in the JVM?
     * @return TRUE if JVM is using our keystore
     */
    public boolean isActive() {
        final String pwd = System.getProperty("javax.net.ssl.keyStorePassword");
        return pwd != null && pwd.equals(this.password);
    }

    /**
     * Activate it.
     * @throws IOException If fails
     */
    public void activate() throws IOException {
        final File jks = this.jks();
        System.setProperty("javax.net.ssl.keyStore", jks.getAbsolutePath());
        System.setProperty("javax.net.ssl.keyStorePassword", this.password);
        System.setProperty("javax.net.ssl.trustStore", jks.getAbsolutePath());
        System.setProperty("javax.net.ssl.trustStorePassword", this.password);
    }

    /**
     * Create and return JKS keystore in a file.
     * @return The file with keystore
     * @throws IOException If fails
     */
    private File jks() throws IOException {
        final long start = System.currentTimeMillis();
        final File file = File.createTempFile(
            String.format("%s-", this.getClass().getName()),
            ".jks"
        );
        file.delete();
        final ProcessBuilder builder = new ProcessBuilder(
            String.format(
                "%s/bin/keytool",
                System.getProperty("java.home")
            ),
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
            file.getAbsolutePath()
        );
        builder.redirectErrorStream(true);
        final Process process = builder.start();
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
        try {
            process.waitFor();
        } catch (InterruptedException ex) {
            Thread.currentThread().interrupt();
            throw new IllegalStateException(ex);
        }
        final int code = process.exitValue();
        if (code != 0) {
            throw new IllegalStateException(
                Logger.format(
                    "Non-zero exit code #%d: %s",
                    code,
                    IOUtils.toString(process.getInputStream())
                )
            );
        }
        Logger.info(
            this,
            "Keystore created in '%s' in %[ms]s",
            file,
            System.currentTimeMillis() - start
        );
        return file;
    }

}
