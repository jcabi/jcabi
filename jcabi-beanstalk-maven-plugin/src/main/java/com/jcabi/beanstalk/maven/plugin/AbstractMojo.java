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
import org.apache.maven.plugin.MojoFailureException;
import org.apache.maven.settings.Settings;
import org.jfrog.maven.annomojo.annotations.MojoParameter;
import org.slf4j.impl.StaticLoggerBinder;

/**
 * Deploys WAR artifact to Amazon Elastic Beanstalk.
 *
 * @author Yegor Bugayenko (yegor@tpc2.com)
 * @version $Id$
 * @since 0.7.1
 */
abstract class AbstractMojo
    extends org.apache.maven.plugin.AbstractMojo {

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
        final long start = System.currentTimeMillis();
        try {
            this.exec(
                new Application(ebt, this.name),
                new OverridingVersion(
                    ebt,
                    this.name,
                    new OverridingBundle(
                        new AmazonS3Client(creds),
                        this.bucket,
                        this.key,
                        this.war
                    )
                ),
                this.template
            );
        } catch (DeploymentException ex) {
            throw new MojoFailureException("failed to deploy", ex);
        } finally {
            ebt.shutdown();
            Logger.info(
                this,
                "Deployment took %[ms]s",
                System.currentTimeMillis() - start
            );
        }
    }

    /**
     * Deploy using this EBT client.
     * @param app Application to deploy to
     * @param version Version to deploy
     * @param template Template to use
     * @throws DeploymentException If failed to deploy
     */
    protected abstract void exec(final Application app,
        final Version version, final String template)
        throws DeploymentException;

    /**
     * Report when environment is failed.
     * @param env The environment
     */
    protected void postMortem(final Environment env) {
        Logger.error(this, "Failed to deploy to '%s'", env);
        if (!env.terminated()) {
            Logger.error(
                this,
                "TAIL report should explain the cause of failure:"
            );
            this.log(env.tail().split("\n"));
        }
        Logger.error(this, "Latest EBT events (in reverse order):");
        this.log(env.events());
        env.terminate();
    }

    /**
     * Log all lines from the collection.
     * @param lines All lines to log
     */
    private void log(final String[] lines) {
        for (String line : lines) {
            Logger.info(this, ">> %s", line);
        }
    }

}
