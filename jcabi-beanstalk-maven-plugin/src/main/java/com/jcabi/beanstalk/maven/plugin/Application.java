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

import com.amazonaws.services.elasticbeanstalk.AWSElasticBeanstalk;
import com.amazonaws.services.elasticbeanstalk.model.CheckDNSAvailabilityRequest;
import com.amazonaws.services.elasticbeanstalk.model.CreateEnvironmentRequest;
import com.amazonaws.services.elasticbeanstalk.model.CreateEnvironmentResult;
import com.amazonaws.services.elasticbeanstalk.model.DescribeEnvironmentsRequest;
import com.amazonaws.services.elasticbeanstalk.model.DescribeEnvironmentsResult;
import com.amazonaws.services.elasticbeanstalk.model.EnvironmentDescription;
import com.amazonaws.services.elasticbeanstalk.model.SwapEnvironmentCNAMEsRequest;
import com.jcabi.log.Logger;
import java.util.Collection;
import java.util.LinkedList;
import org.apache.commons.lang.RandomStringUtils;

/**
 * EBT application.
 *
 * @author Yegor Bugayenko (yegor@tpc2.com)
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
        assert clnt != null;
        assert app != null;
        this.client = clnt;
        this.name = app;
    }

    /**
     * Clean it up beforehand.
     * @param wipe Kill all existing environments no matter what?
     */
    public void clean(final boolean wipe) {
        for (Environment env : this.environments()) {
            if (env.primary() && env.green() && !wipe) {
                Logger.info(
                    this,
                    "Environment '%s' is primary and green",
                    env
                );
                continue;
            }
            if (env.terminated()) {
                continue;
            }
            if (wipe) {
                Logger.info(
                    this,
                    "Wiping out environment '%s'...",
                    env
                );
            } else {
                Logger.info(
                    this,
                    "Environment '%s' is not primary+green, terminating...",
                    env
                );
            }
            env.terminate();
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public String toString() {
        return this.name;
    }

    /**
     * Activate candidate environment by swap of CNAMEs.
     * @param candidate The candidate to make a primary environment
     */
    public void swap(final Environment candidate) {
        Environment primary = null;
        for (Environment env : this.environments()) {
            if (env.primary()) {
                primary = env;
                break;
            }
        }
        if (primary == null) {
            throw new DeploymentException(
                String.format(
                    "Application '%s' doesn't have a primary env, can't merge",
                    this.name
                )
            );
        }
        this.client.swapEnvironmentCNAMEs(
            new SwapEnvironmentCNAMEsRequest()
                .withDestinationEnvironmentName(primary.name())
                .withSourceEnvironmentName(candidate.name())
        );
        Logger.info(
            this,
            "Environment '%s' swapped CNAME with '%s'",
            candidate.name(),
            primary.name()
        );
        if (candidate.stable() && !candidate.primary()) {
            throw new DeploymentException(
                String.format(
                    "Failed to swap, '%s' didn't become a primary env",
                    candidate
                )
            );
        }
        if (primary.stable() && primary.primary()) {
            throw new DeploymentException(
                String.format(
                    "Failed to swap, '%s' is still a primary env",
                    primary
                )
            );
        }
        primary.terminate();
    }

    /**
     * Create candidate environment.
     * @param version Version to deploy
     * @param template EBT configuration template
     * @return The environment
     */
    public Environment candidate(final Version version, final String template) {
        final CreateEnvironmentRequest request = this.suggest();
        Logger.info(
            this,
            "Suggested candidate environment name is '%s' with '%s' CNAME",
            request.getEnvironmentName(),
            request.getCNAMEPrefix()
        );
        final CreateEnvironmentResult res = this.client.createEnvironment(
            request
                .withApplicationName(this.name)
                .withVersionLabel(version.label())
                .withTemplateName(template)
        );
        Logger.info(
            this,
            // @checkstyle LineLength (1 line)
            "Candidate environment '%s/%s/%s' created at CNAME '%s' (status:%s, health:%s)",
            res.getApplicationName(),
            res.getEnvironmentName(),
            res.getEnvironmentId(),
            res.getCNAME(),
            res.getStatus(),
            res.getHealth()
        );
        return new Environment(this.client, res.getEnvironmentId());
    }

    /**
     * Get all environments in this app.
     * @return Collection of envs
     */
    @SuppressWarnings("PMD.AvoidInstantiatingObjectsInLoops")
    private Collection<Environment> environments() {
        final DescribeEnvironmentsResult res = this.client.describeEnvironments(
            new DescribeEnvironmentsRequest().withApplicationName(this.name)
        );
        final Collection<Environment> envs = new LinkedList<Environment>();
        for (EnvironmentDescription desc : res.getEnvironments()) {
            envs.add(new Environment(this.client, desc.getEnvironmentId()));
        }
        return envs;
    }

    /**
     * Suggest new candidate environment CNAME (and at the same time it will
     * be used as a name of environment).
     * @return The environment create request with data inside
     */
    private CreateEnvironmentRequest suggest() {
        final CreateEnvironmentRequest request = new CreateEnvironmentRequest();
        if (this.occupied(this.name)) {
            request.withCNAMEPrefix(this.makeup(this.name));
        } else {
            request.withCNAMEPrefix(this.name);
        }
        while (true) {
            final String ename = String.format(
                "%s-%s",
                this.name,
                // @checkstyle MagicNumber (1 line)
                RandomStringUtils.randomAlphabetic(4)
            );
            boolean exists = false;
            for (Environment env : this.environments()) {
                if (env.name().equals(ename)) {
                    exists = true;
                    break;
                }
            }
            if (!exists) {
                request.withEnvironmentName(ename).withDescription(ename);
                break;
            }
        }
        return request;
    }

    /**
     * Make up a nice CNAME, using provided one as a base.
     * @param base Base name, which will get a suffix to become unique
     * @return The CNAME, suggested and not occupied
     */
    private String makeup(final String base) {
        String cname;
        int suffix = 0;
        do {
            cname = String.format("%s-%d", base, ++suffix);
        } while (this.occupied(cname));
        return cname;
    }

    /**
     * This CNAME is occupied?
     * @param cname The CNAME to check
     * @return TRUE if it's occupied
     */
    private boolean occupied(final String cname) {
        return !this.client.checkDNSAvailability(
            new CheckDNSAvailabilityRequest(cname)
        ).getAvailable();
    }

}
