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
    private final transient String app;

    /**
     * Public ctor.
     * @param clnt The client
     * @param name Application name
     */
    public Application(final AWSElasticBeanstalk clnt, final String name) {
        this.client = clnt;
        this.app = name;
    }

    /**
     * Get its name.
     * @return The name
     */
    public String name() {
        return this.app;
    }

    /**
     * Get candidate environment name.
     * @param env Environment name
     * @return The name
     */
    public String candidate(final String env) {
        final String active = this.active(env);
        String candidate;
        if (active.equals(env)) {
            candidate = String.format("%s-1", env);
        } else {
            candidate = env;
        }
        Logger.info(
            this,
            "Candidate environment name in '%s' app is '%s'",
            this.app,
            candidate
        );
        return candidate;
    }

    /**
     * Get name of active environment, for this CNAME.
     * @param prefix The CNAME prefix
     * @return The name
     */
    private String active(final String prefix) {
        final DescribeEnvironmentsResult res = this.client.describeEnvironments(
            new DescribeEnvironmentsRequest().withApplicationName(this.app)
        );
        final Collection<EnvironmentDescription> envs = res.getEnvironments();
        String active = "";
        final Pattern pattern = Pattern.compile(
            String.format("%s.elasticbeanstalk.com", Pattern.quote(prefix))
        );
        for (EnvironmentDescription env : envs) {
            if (pattern.matcher(env.getCNAME()).matches()) {
                active = env.getEnvironmentName();
            } else if ("Terminated".equals(env.getStatus())) {
                Logger.info(
                    this,
                    "Environment '%s' in '%s' app is terminated",
                    env.getEnvironmentName(),
                    env.getApplicationName()
                );
            } else {
                Logger.warn(
                    this,
                    // @checkstyle LineLength (1 line)
                    "Environment '%s' in '%s' app has incorrect CNAME '%s' (doesn't start with '%s.'), should be terminated (status=%s, health=%s)",
                    env.getEnvironmentName(),
                    env.getApplicationName(),
                    env.getCNAME(),
                    prefix,
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
        if (active.isEmpty()) {
            Logger.info(
                this,
                "No active environments in '%s' app with CNAME prefix '%s'",
                this.app,
                prefix
            );
        } else {
            Logger.info(
                this,
                "Environment '%s' is active in '%s' app with CNAME prefix '%s'",
                active,
                this.app,
                prefix
            );
        }
        return active;
    }

}
