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

import com.jcabi.log.Logger;
import java.io.File;
import org.apache.maven.plugin.AbstractMojo;
import org.apache.maven.plugin.MojoFailureException;
import org.apache.maven.settings.Settings;
import org.jfrog.maven.annomojo.annotations.MojoExecute;
import org.jfrog.maven.annomojo.annotations.MojoGoal;
import org.jfrog.maven.annomojo.annotations.MojoParameter;
import org.jfrog.maven.annomojo.annotations.MojoPhase;
import org.slf4j.impl.StaticLoggerBinder;

/**
 * Deploys JAR/WAR artifact to Heroku.
 *
 * @author Yegor Bugayenko (yegor@jcabi.com)
 * @version $Id$
 * @since 0.4
 */
@MojoGoal("deploy")
@MojoPhase("deploy")
@MojoExecute(phase = "deploy")
public final class DeployMojo extends AbstractMojo {

    /**
     * Setting.xml.
     */
    @MojoParameter(
        expression = "${settings}",
        required = true,
        readonly = true,
        description = "Maven settings.xml reference"
    )
    private transient Settings settings;

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
     * Location of SSH key file.
     */
    @MojoParameter(
        required = true,
        description = "SSH key file name"
    )
    private transient File key;

    /**
     * Application name.
     */
    @MojoParameter(
        required = true,
        description = "Heroku application name"
    )
    private transient String name;

    /**
     * Content of {@code Procfile}.
     */
    @MojoParameter(
        defaultValue = "",
        required = false,
        description = "Content of Procfile"
    )
    private transient String procfile;

    /**
     * List of artifacts to download.
     */
    @MojoParameter(
        required = false,
        description = "Artifacts to download during deployment"
    )
    private transient String[] artifacts;

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
        if (!this.key.exists()) {
            throw new MojoFailureException(
                String.format("SSH key file '%s' doesn't exist", this.key)
            );
        }
    }

}
