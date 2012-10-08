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
import com.amazonaws.services.elasticbeanstalk.model.CreateEnvironmentRequest;
import com.amazonaws.services.elasticbeanstalk.model.CreateEnvironmentResult;
import com.amazonaws.services.elasticbeanstalk.model.DescribeEnvironmentsRequest;
import com.amazonaws.services.elasticbeanstalk.model.DescribeEnvironmentsResult;
import com.amazonaws.services.elasticbeanstalk.model.EnvironmentDescription;
import com.amazonaws.services.elasticbeanstalk.model.S3Location;
import com.amazonaws.services.elasticbeanstalk.model.TerminateEnvironmentRequest;
import com.amazonaws.services.elasticbeanstalk.model.TerminateEnvironmentResult;
import com.jcabi.log.Logger;
import java.io.File;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;
import java.util.concurrent.TimeUnit;
import java.util.regex.Pattern;

/**
 * EBT application.
 *
 * @author Yegor Bugayenko (yegor@jcabi.com)
 * @version $Id$
 * @since 0.3
 */
final class Application {

    /**
     * AWS beanstalk client.
     */
    private final transient AWSElasticBeanstalk client;

    /**
     * Application name.
     */
    private final transient String name;

    /**
     * Public ctor.
     * @param clnt The client
     * @param app Application name
     */
    public Application(final AWSElasticBeanstalk clnt, final String app) {
        this.client = clnt;
        this.name = app;
    }

    /**
     * Activate candidate environment by swap of CNAMEs, or leave current
     * environment as available, if it's alone.
     */
    public void swap() {
        // this.client.swapEnvironmentCNAMEs();
        // Logger.info(this, "Environments swapped by CNAME update");
    }

    /**
     * Create candidate environment.
     * @param version Version to deploy
     * @param template Configuration template
     * @return The environment
     */
    public Environment candidate(final Version version, final String template) {
        final String available = this.available();
        Logger.info(
            this,
            "Candidate environment name in '%s' app is '%s'",
            this.name,
            available
        );
        final CreateEnvironmentResult res = this.client.createEnvironment(
            new CreateEnvironmentRequest(this.name, available)
                .withDescription(available)
                .withVersionLabel(version.label())
                .withTemplateName(template)
                .withCNAMEPrefix(available)
        );
        Logger.info(
            this,
            // @checkstyle LineLength (1 line)
            "Candidate environment '%s/%s' created at CNAME '%s' (status:%s, health:%s)",
            res.getApplicationName(),
            res.getEnvironmentName(),
            res.getCNAME(),
            res.getStatus(),
            res.getHealth()
        );
        return new Environment(
            this.client,
            res.getApplicationName(),
            res.getEnvironmentName()
        );
    }

    /**
     * Get available candidate name.
     * @return The name
     */
    private String available() {
        final DescribeEnvironmentsResult res = this.client.describeEnvironments(
            new DescribeEnvironmentsRequest()
                .withApplicationName(this.name)
        );
        final Collection<EnvironmentDescription> envs = res.getEnvironments();
        final Pattern pattern = Pattern.compile(
            String.format("%s.elasticbeanstalk.com", Pattern.quote(this.name))
        );
        Logger.info(
            this,
            "Found %d environment(s) in '%s' app:",
            envs.size(),
            this.name
        );
        String available = null;
        final Collection<String> occupied = new HashSet<String>();
        for (EnvironmentDescription env : envs) {
            occupied.add(env.getEnvironmentName());
            if ("Terminated".equals(env.getStatus())) {
                Logger.info(
                    this,
                    "  environment '%s/%s' is terminated",
                    env.getApplicationName(),
                    env.getEnvironmentName()
                );
            } else if (pattern.matcher(env.getCNAME()).matches()) {
                available = env.getEnvironmentName();
                Logger.info(
                    this,
                    // @checkstyle LineLength (1 line)
                    "  environment '%s/%s' is available, CNAME=%s (status=%s, health=%s)",
                    env.getApplicationName(),
                    env.getEnvironmentName(),
                    env.getCNAME(),
                    env.getStatus(),
                    env.getHealth()
                );
            } else {
                Logger.warn(
                    this,
                    // @checkstyle LineLength (1 line)
                    "  environment '%s/%s' has incorrect CNAME '%s' (doesn't start with '%s.'), should be terminated (status=%s, health=%s)",
                    env.getApplicationName(),
                    env.getEnvironmentName(),
                    env.getCNAME(),
                    this.name,
                    env.getStatus(),
                    env.getHealth()
                );
                new Environment(
                    this.client,
                    env.getApplicationName(),
                    env.getEnvironmentName()
                ).terminate();
            }
        }
        if (available == null) {
            available = this.name;
            Logger.info(
                this,
                // @checkstyle LineLength (1 line)
                "No active environments in '%s' with CNAME prefix '%s' (among %d envs)",
                this.name,
                this.name,
                envs.size()
            );
        } else {
            Logger.info(
                this,
                // @checkstyle LineLength (1 line)
                "Environment '%s/%s' considered active, among %d environment(s)",
                this.name,
                available,
                envs.size()
            );
            int suffix = 0;
            do {
                available = String.format("%s-%d", this.name, ++suffix);
            } while (occupied.contains(available));
        }
        return available;
    }

}
