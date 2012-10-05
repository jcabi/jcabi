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
import com.amazonaws.auth.BasicAWSCredentials;
import com.amazonaws.services.elasticbeanstalk.AWSElasticBeanstalk;
import com.amazonaws.services.elasticbeanstalk.AWSElasticBeanstalkClient;
import com.amazonaws.services.s3.AmazonS3;
import com.amazonaws.services.s3.AmazonS3Client;
import com.jcabi.log.Logger;
import java.io.File;
import org.apache.maven.plugin.AbstractMojo;
import org.apache.maven.plugin.MojoFailureException;
import org.apache.maven.project.MavenProject;
import org.apache.maven.settings.Server;
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
     * Maven project, to be injected by Maven itself.
     */
    @MojoParameter(
        expression = "${project}",
        required = true,
        readonly = true,
        description = "Maven project reference"
    )
    private transient MavenProject project;

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
        required = true,
        description = "Skips execution"
    )
    private transient boolean skip;

    /**
     * Server ID to deploy to.
     */
    @MojoParameter(
        defaultValue = "aws.amazon.com",
        required = true,
        description = "ID of the server to deploy to, from settings.xml"
    )
    private transient String server;

    /**
     * Application name.
     */
    @MojoParameter(
        required = true,
        description = "Amazon Elastic Beanstalk application name"
    )
    private transient String application;

    /**
     * Environment name.
     */
    @MojoParameter(
        required = true,
        description = "Amazon Elastic Beanstalk environment name"
    )
    private transient String environment;

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
        defaultValue = "${project.build.directory}/${project.build.finalName}.war",
        required = true,
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
        final AWSCredentials creds = this.credentials();
        final Bundle bundle = new OverridingBundle(
            new AmazonS3Client(creds),
            this.bucket,
            this.key,
            this.war
        );
        final AWSElasticBeanstalk ebt = new AWSElasticBeanstalkClient(creds);
        final Environment env = new Environment(
            ebt,
            this.application,
            this.environment
        );
        final Environment candidate = env.candidate(bundle, this.template);
        if (candidate.ready()) {
            candidate.activate();
            env.terminate();
        } else {
            candidate.terminate();
        }
        ebt.shutdown();
    }

    /**
     * Create AWS credentials.
     * @return The credentials
     * @throws MojoFailureException If some error
     */
    private AWSCredentials credentials() throws MojoFailureException {
        final Server srv = this.settings.getServer(this.server);
        if (srv == null) {
            throw new MojoFailureException(
                String.format(
                    "Server '%s' is absent in settings.xml",
                    this.server
                )
            );
        }
        final String key = srv.getUsername().trim();
        if (!key.matches("[A-F0-9]{20}")) {
            throw new MojoFailureException(
                String.format(
                    "Key '%s' for server '%s' is not a valid AWS key",
                    key,
                    this.server
                )
            );
        }
        final String secret = srv.getPassword().trim();
        if (!secret.matches("[a-zA-Z0-9\\+/]{40}")) {
            throw new MojoFailureException(
                String.format(
                    "Secret '%s' for server '%s' is not a valid AWS secret",
                    secret,
                    this.server
                )
            );
        }
        Logger.info(
            this,
            "Using server '%s' with AWS key '%s'",
            this.server,
            key
        );
        return new BasicAWSCredentials(key, secret);
    }

}
