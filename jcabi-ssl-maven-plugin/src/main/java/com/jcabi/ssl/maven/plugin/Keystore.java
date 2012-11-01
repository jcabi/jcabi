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
import java.util.Properties;

/**
 * Keystore abstraction.
 *
 * @author Yegor Bugayenko (yegor@jcabi.com)
 * @version $Id$
 * @since 0.5
 */
final class Keystore {

    /**
     * Constant {@code javax.net.ssl.keyStore}.
     */
    public static final String KEY = "javax.net.ssl.keyStore";

    /**
     * Constant {@code javax.net.ssl.keyStorePassword}.
     */
    public static final String KEY_PWD = "javax.net.ssl.keyStorePassword";

    /**
     * Unique password of it.
     */
    private final transient String password;

    /**
     * Public ctor.
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
            Keystore.KEY,
            Keystore.KEY_PWD,
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
        final String pwd = System.getProperty(Keystore.KEY_PWD);
        return pwd != null && pwd.equals(this.password);
    }

    /**
     * Activate it, in the given file.
     * @param file The file to use
     * @throws IOException If fails
     */
    public void activate(final File file) throws IOException {
        file.getParentFile().mkdirs();
        file.delete();
        new Keytool(file, this.password).genkey();
        System.setProperty(Keystore.KEY, file.getAbsolutePath());
        System.setProperty(Keystore.KEY_PWD, this.password);
        Logger.debug(
            this,
            "Keystore: %s",
            new Keytool(file, this.password).list()
        );
    }

    /**
     * Populate given properties with data.
     * @param props The properties
     */
    public void populate(final Properties props) {
        final String[] names = new String[] {
            Keystore.KEY,
            Keystore.KEY_PWD,
        };
        for (String name : names) {
            final String value = System.getProperty(name);
            if (value == null) {
                continue;
            }
            props.put(name, value);
            Logger.info(
                this,
                "Maven property ${%s} set to '%s'",
                name,
                System.getProperty(name)
            );
        }
    }

}
