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
package com.jcabi.beanstalk.maven.plugin;

import com.amazonaws.auth.AWSCredentials;
import com.amazonaws.services.elasticbeanstalk.AWSElasticBeanstalk;
import com.amazonaws.services.elasticbeanstalk.AWSElasticBeanstalkClient;
import com.amazonaws.services.s3.AmazonS3Client;
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
 * Deploys WAR artifact to Amazon Elastic Beanstalk.
 *
 * @author Yegor Bugayenko (yegor@jcabi.com)
 * @version $Id$
 * @since 0.3
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
     * Server ID to deploy to.
     */
    @MojoParameter(
        defaultValue = "aws.amazon.com",
        required = false,
        description = "ID of the server to deploy to, from settings.xml"
    )
    private transient String server;

    /**
     * Application name (also the name of environment and CNAME).
     */
    @MojoParameter(
        required = true,
        description = "EBT application name, environment name, and CNAME"
    )
    private transient String name;

    /**
     * S3 bucket.
     */
    @MojoParameter(
        required = true,
        description = "Amazon S3 bucket name where to upload WAR file"
    )
    private transient String bucket;

    /**
     * S3 key name.
     */
    @MojoParameter(
        required = true,
        description = "Amazon S3 bucket key where to upload WAR file"
    )
    private transient String key;

    /**
     * Template name.
     */
    @MojoParameter(
        required = true,
        description = "Amazon Elastic Beanstalk configuration template name"
    )
    private transient String template;

    /**
     * WAR file to deploy.
     */
    @MojoParameter(
        // @checkstyle LineLength (1 line)
        defaultValue = "${project.build.directory}/${project.build.finalName}.war",
        required = false,
        description = "Location of .WAR file to deploy"
    )
    private transient File war;

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
        if (!this.war.exists()) {
            throw new MojoFailureException(
                String.format("WAR file '%s' doesn't exist", this.war)
            );
        }
        final AWSCredentials creds = new ServerCredentials(
            this.settings,
            this.server
        );
        final AWSElasticBeanstalk ebt = new AWSElasticBeanstalkClient(creds);
        try {
            this.deploy(
                ebt,
                new OverridingVersion(
                    ebt,
                    this.name,
                    new OverridingBundle(
                        new AmazonS3Client(creds),
                        this.bucket,
                        this.key,
                        this.war
                    )
                )
            );
        } finally {
            ebt.shutdown();
        }
    }

    /**
     * Deploy using this EBT client.
     * @param ebt EBT client
     * @param version Version to deploy
     * @throws MojoFailureException If failed to deploy
     */
    private void deploy(final AWSElasticBeanstalk ebt, final Version version)
        throws MojoFailureException {
        final Application app = new Application(ebt, this.name);
        final Environment candidate = app.candidate(version, this.template);
        if (candidate.green()) {
            if (candidate.primary()) {
                Logger.info(
                    this,
                    "Candidate env '%' is already primary, no need to swap",
                    candidate
                );
            } else {
                app.swap(candidate);
            }
        } else {
            Logger.error(
                this,
                "Failed to deploy %s to %s",
                version,
                candidate
            );
            if (!candidate.terminated()) {
                Logger.error(
                    this,
                    "TAIL report should explain the cause of failure"
                );
                for (String line : candidate.tail().split("\n")) {
                    Logger.info(this, "  %s", line);
                }
            }
            Logger.error(this, "Latest events (in reverse order):");
            for (String event : candidate.events()) {
                Logger.info(this, "   %s", event);
            }
            candidate.terminate();
            throw new MojoFailureException("failed to deploy");
        }
    }

}
