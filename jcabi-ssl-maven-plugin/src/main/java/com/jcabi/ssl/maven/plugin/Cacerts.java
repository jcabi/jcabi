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
import org.apache.commons.io.FileUtils;

/**
 * Abstraction of {@code java.home/lib/security/cacerts} file.
 *
 * @author Yegor Bugayenko (yegor@jcabi.com)
 * @version $Id$
 * @since 0.5
 */
final class Cacerts {

    /**
     * Constant {@code javax.net.ssl.trustStore}.
     */
    private static final String TRUST = "javax.net.ssl.trustStore";

    /**
     * Constant {@code javax.net.ssl.trustStorePassword}.
     */
    private static final String TRUST_PWD = "javax.net.ssl.trustStorePassword";

    /**
     * New location of the trust store.
     */
    private final transient File store;

    /**
     * Public ctor.
     * @param file New location
     * @throws IOException If fails
     */
    public Cacerts(final File file) throws IOException {
        this.store = file;
        final File prev = new File(
            String.format(
                "%s/lib/security/cacerts",
                System.getProperty("java.home")
            )
        );
        FileUtils.copyFile(prev, this.store);
        Logger.info(
            this,
            "Existing cacerts '%s' copied to '%s' (%s)",
            prev,
            this.store,
            FileUtils.byteCountToDisplaySize(this.store.length())
        );
    }

    /**
     * Import existing keystore content into this trust store.
     * @throws IOException If fails
     */
    public void imprt() throws IOException {
        final File keystore = new File(System.getProperty(Keystore.KEY));
        final String pwd = System.getProperty(Keystore.KEY_PWD);
        new Keytool(this.store, "changeit").imprt(keystore, pwd);
        System.setProperty(Cacerts.TRUST, this.store.getAbsolutePath());
        System.setProperty(Cacerts.TRUST_PWD, "changeit");
        Logger.info(
            this,
            "keyStore '%s' imported into trustStore '%s'",
            keystore,
            this.store
        );
    }

}
