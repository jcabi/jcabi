/**
 * Copyright (c) 2012-2013, JCabi.com
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
import org.apache.commons.codec.digest.DigestUtils;
import org.apache.maven.plugin.AbstractMojo;
import org.apache.maven.plugin.MojoFailureException;
import org.apache.maven.project.MavenProject;
import org.jfrog.maven.annomojo.annotations.MojoGoal;
import org.jfrog.maven.annomojo.annotations.MojoParameter;
import org.jfrog.maven.annomojo.annotations.MojoPhase;
import org.slf4j.impl.StaticLoggerBinder;

/**
 * Generate SSL keystore and configure in JVM.
 *
 * @author Yegor Bugayenko (yegor@tpc2.com)
 * @version $Id$
 * @since 0.5
 */
@MojoGoal("keygen")
@MojoPhase("initialize")
public final class KeygenMojo extends AbstractMojo {

    /**
     * Maven project.
     */
    @MojoParameter(
        expression = "${project}",
        required = true,
        readonly = true,
        description = "Maven project"
    )
    private transient MavenProject project;

    /**
     * Shall we skip execution?
     */
    @MojoParameter(
        defaultValue = "false",
        required = false,
        description = "Skips execution"
    )
    private transient boolean skip;

    /**
     * Name of keystore.jks file.
     */
    @MojoParameter(
        defaultValue = "${project.build.directory}/keystore.jks",
        required = false,
        description = "Name of keystore.jks file"
    )
    private transient File keystore;

    /**
     * Name of cacerts.jks file.
     */
    @MojoParameter(
        defaultValue = "${project.build.directory}/cacerts.jks",
        required = false,
        description = "Name of cacerts.jks file"
    )
    private transient File cacerts;

    /**
     * Set skip option.
     * @param skp Shall we skip execution?
     */
    public void setSkip(final boolean skp) {
        this.skip = skp;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void execute() throws MojoFailureException {
        StaticLoggerBinder.getSingleton().setMavenLog(this.getLog());
        if (this.skip) {
            Logger.info(this, "execution skipped because of 'skip' option");
            return;
        }
        final Keystore store = new Keystore(
            DigestUtils.md5Hex(this.getClass().getName())
        );
        if (!store.isActive()) {
            try {
                store.activate(this.keystore);
                new Cacerts(this.cacerts).imprt();
            } catch (java.io.IOException ex) {
                throw new IllegalStateException(ex);
            }
        }
        store.populate(this.project.getProperties());
        Logger.info(this, "Keystore is active: %s", store);
    }

}
