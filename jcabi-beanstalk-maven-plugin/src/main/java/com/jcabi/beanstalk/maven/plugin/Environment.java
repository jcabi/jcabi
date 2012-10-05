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

/**
 * EBT environment.
 *
 * @author Yegor Bugayenko (yegor@jcabi.com)
 * @version $Id$
 * @since 0.3
 */
final class Environment {

    /**
     * How many retries to do.
     */
    private static final int MAX_ATTEMPTS = 10;

    /**
     * AWS beanstalk client.
     */
    private final transient AWSElasticBeanstalk client;

    /**
     * Application name.
     */
    private final transient Application app;

    /**
     * Environment name.
     */
    private final transient String name;

    /**
     * Public ctor.
     * @param clnt The client
     * @param application Application name
     * @param env Environment name
     * @param tmpl Configuration template name
     */
    public Environment(final AWSElasticBeanstalk clnt, final String application,
        final String env) {
        this.client = clnt;
        this.app = new Application(clnt, application);
        this.name = env;
    }

    /**
     * Environment is in READY state?
     * @return TRUE if environment is in Ready state, FALSE otherwise
     */
    public boolean ready() {
        return this.until(
            new Environment.Barrier() {
                @Override
                public boolean allow(final EnvironmentDescription desc) {
                    return "Green".equals(desc.getHealth())
                        && "Ready".equals(desc.getStatus());
                }
            }
        );
    }

    /**
     * Create a new candidate environment, deploying a new bundle into it.
     * @param bundle The bundle to deploy there
     * @param template Configuration template
     */
    public Environment candidate(final Bundle bundle, final String template) {
        final String child = this.app.candidate(this.name);
        final CreateEnvironmentResult res = this.client.createEnvironment(
            new CreateEnvironmentRequest(this.app.name(), child)
                .withDescription("swapped...")
                .withVersionLabel(
                    new Version(this.client, this.app.name(), bundle).label()
                )
                .withTemplateName(template)
                .withCNAMEPrefix(child)
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
     * Activate this environment by swap of CNAMEs.
     */
    public void activate() {
        this.client.swapEnvironmentCNAMEs();
        Logger.info(this, "Environments swapped by CNAME update");
    }

    /**
     * Terminate environment.
     */
    public void terminate() {
        final boolean ready = this.until(
            new Environment.Barrier() {
                @Override
                public boolean allow(final EnvironmentDescription desc) {
                    return !"Terminated".equals(desc.getStatus())
                        && !"Launching".equals(desc.getStatus());
                }
            }
        );
        if (!ready) {
            throw new IllegalStateException(
                Logger.format(
                    "environment '%s/%s' can't be terminated",
                    this.app.name(),
                    this.name
                )
            );
        }
        final TerminateEnvironmentResult res =
            this.client.terminateEnvironment(
                new TerminateEnvironmentRequest()
                    .withEnvironmentName(this.name)
                    .withTerminateResources(true)
            );
        Logger.info(
            this,
            "Environment '%s/%s' will be terminated (label:'%s', status:%s)",
            res.getApplicationName(),
            res.getEnvironmentName(),
            res.getCNAME(),
            res.getVersionLabel(),
            res.getStatus()
        );
    }

    /**
     * Get environment description of this.
     * @return The description
     */
    private EnvironmentDescription description() {
        final DescribeEnvironmentsResult res = this.client.describeEnvironments(
            new DescribeEnvironmentsRequest()
                .withApplicationName(this.app.name())
        );
        EnvironmentDescription desc = null;
        final Collection<EnvironmentDescription> envs = res.getEnvironments();
        final Collection<String> names = new ArrayList<String>(envs.size());
        for (EnvironmentDescription env : envs) {
            names.add(
                String.format(
                    "%s/%s",
                    env.getApplicationName(),
                    env.getEnvironmentName()
                )
            );
            if (!env.getEnvironmentName().equals(this.name)) {
                continue;
            }
            desc = env;
            break;
        }
        if (desc == null) {
            throw new IllegalStateException(
                Logger.format(
                    "environment %s/%s not found amount %d others %[list]s",
                    this.app.name(),
                    this.name,
                    res.getEnvironments().size(),
                    names
                )
            );
        }
        return desc;
    }

    /**
     * Wait for the barrier to pass.
     * @param barrier The barrier
     * @return TRUE if passed, FALSE if timeout
     */
    private boolean until(final Environment.Barrier barrier) {
        boolean passed = false;
        int retry = 0;
        while (++retry < Environment.MAX_ATTEMPTS) {
            final EnvironmentDescription desc = this.description();
            Logger.info(
                this,
                "Environment '%s/%s': health=%s, status=%s, retry=%d of %d",
                desc.getApplicationName(),
                desc.getEnvironmentName(),
                desc.getHealth(),
                desc.getStatus(),
                retry,
                Environment.MAX_ATTEMPTS
            );
            if (barrier.allow(desc)) {
                passed = true;
                break;
            }
            try {
                TimeUnit.MINUTES.sleep(1);
            } catch (InterruptedException ex) {
                Thread.currentThread().interrupt();
                throw new IllegalStateException(ex);
            }
        }
        return passed;
    }

    /**
     * Barrier before the next operation.
     */
    private interface Barrier {
        /**
         * Can we continue?
         * @param desc Description of environment
         */
        boolean allow(EnvironmentDescription desc);
    }

}
