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
        this.clean();
    }

    /**
     * Activate candidate environment by swap of CNAMEs.
     * @param candidate The candidate to make a primary environment
     */
    public void swap(final Environment candidate) {
        this.client.swapEnvironmentCNAMEs(
            new SwapEnvironmentCNAMEsRequest()
                .withDestinationEnvironmentName(this.name)
                .withSourceEnvironmentName(candidate.name())
        );
        Logger.info(
            this,
            "Environment '%s' swapped CNAME with '%s'",
            candidate.name(),
            this.name
        );
    }

    /**
     * Create candidate environment.
     * @param version Version to deploy
     * @param template EBT configuration template
     * @return The environment
     */
    public Environment candidate(final Version version, final String template) {
        final String cname = this.suggest();
        Logger.info(
            this,
            "Suggested candidate environment name is '%s' in '%s' app",
            cname,
            this.name
        );
        final CreateEnvironmentResult res = this.client.createEnvironment(
            new CreateEnvironmentRequest(this.name, cname)
                .withDescription(cname)
                .withVersionLabel(version.label())
                .withTemplateName(template)
                .withCNAMEPrefix(cname)
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
     * Remove all abandoned environments.
     */
    @SuppressWarnings("PMD.AvoidInstantiatingObjectsInLoops")
    private void clean() {
        final DescribeEnvironmentsResult res = this.client.describeEnvironments(
            new DescribeEnvironmentsRequest().withApplicationName(this.name)
        );
        final String prefix = String.format("%s.", this.name);
        for (EnvironmentDescription env : res.getEnvironments()) {
            if (!env.getCNAME().startsWith(prefix)
                && !"Terminated".equals(env.getStatus())) {
                Logger.info(
                    this,
                    // @checkstyle LineLength (1 line)
                    "Environment '%s/%s' doesn't belong to CNAME '%s' (CNAME=%s, health=%s, status=%s), terminating...",
                    env.getApplicationName(),
                    env.getEnvironmentName(),
                    this.name,
                    env.getCNAME(),
                    env.getHealth(),
                    env.getStatus()
                );
                new Environment(
                    this.client,
                    this.name,
                    env.getEnvironmentName()
                ).terminate();
            }
        }
    }

    /**
     * Suggest new candidate environment CNAME.
     * @return The CNAME, suggested and not occupied
     */
    private String suggest() {
        String cname;
        if (this.occupied(this.name)) {
            cname = this.makeup(this.name);
        } else {
            cname = this.name;
        }
        return cname;
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
