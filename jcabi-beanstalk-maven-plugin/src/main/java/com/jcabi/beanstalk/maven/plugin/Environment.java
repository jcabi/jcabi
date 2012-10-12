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
import com.amazonaws.services.elasticbeanstalk.model.ConfigurationOptionSetting;
import com.amazonaws.services.elasticbeanstalk.model.ConfigurationSettingsDescription;
import com.amazonaws.services.elasticbeanstalk.model.DescribeConfigurationSettingsRequest;
import com.amazonaws.services.elasticbeanstalk.model.DescribeConfigurationSettingsResult;
import com.amazonaws.services.elasticbeanstalk.model.DescribeEnvironmentsRequest;
import com.amazonaws.services.elasticbeanstalk.model.DescribeEnvironmentsResult;
import com.amazonaws.services.elasticbeanstalk.model.DescribeEventsRequest;
import com.amazonaws.services.elasticbeanstalk.model.DescribeEventsResult;
import com.amazonaws.services.elasticbeanstalk.model.EnvironmentDescription;
import com.amazonaws.services.elasticbeanstalk.model.EnvironmentInfoDescription;
import com.amazonaws.services.elasticbeanstalk.model.EnvironmentInfoType;
import com.amazonaws.services.elasticbeanstalk.model.EventDescription;
import com.amazonaws.services.elasticbeanstalk.model.RequestEnvironmentInfoRequest;
import com.amazonaws.services.elasticbeanstalk.model.RetrieveEnvironmentInfoRequest;
import com.amazonaws.services.elasticbeanstalk.model.TerminateEnvironmentRequest;
import com.amazonaws.services.elasticbeanstalk.model.TerminateEnvironmentResult;
import com.jcabi.log.Logger;
import java.net.URL;
import java.util.Collection;
import java.util.LinkedList;
import java.util.List;
import java.util.concurrent.TimeUnit;
import org.apache.commons.io.IOUtils;

/**
 * EBT environment.
 *
 * @author Yegor Bugayenko (yegor@jcabi.com)
 * @version $Id$
 * @since 0.3
 * @checkstyle ClassDataAbstractionCoupling (500 lines)
 */
@SuppressWarnings("PMD.TooManyMethods")
final class Environment {

    /**
     * For how long we can wait until env reaches certain status.
     */
    private static final int MAX_DELAY_MS = 30 * 60 * 1000;

    /**
     * AWS beanstalk client.
     */
    private final transient AWSElasticBeanstalk client;

    /**
     * Environment ID.
     */
    private final transient String eid;

    /**
     * Public ctor.
     * @param clnt The client
     * @param idnt Environment ID
     */
    public Environment(final AWSElasticBeanstalk clnt, final String idnt) {
        this.client = clnt;
        this.eid = idnt;
        final EnvironmentDescription desc = this.description();
        final String template = desc.getTemplateName();
        if (template != null) {
            final DescribeConfigurationSettingsResult res =
                this.client.describeConfigurationSettings(
                    new DescribeConfigurationSettingsRequest()
                        .withApplicationName(desc.getApplicationName())
                        .withTemplateName(template)
                );
            for (ConfigurationSettingsDescription config
                : res.getConfigurationSettings()) {
                Logger.debug(
                    this,
                    "Environment '%s/%s/%s' settings:",
                    config.getApplicationName(),
                    config.getEnvironmentName()
                );
                for (ConfigurationOptionSetting opt
                    : config.getOptionSettings()) {
                    Logger.debug(
                        this,
                        "  %s/%s: %s",
                        opt.getNamespace(),
                        opt.getOptionName(),
                        opt.getValue()
                    );
                }
            }
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public String toString() {
        final EnvironmentDescription desc = this.description();
        return String.format(
            "%s/%s/%s/%s",
            desc.getApplicationName(),
            desc.getEnvironmentName(),
            desc.getEnvironmentId(),
            desc.getCNAME()
        );
    }

    /**
     * Is it primary environment in the application?
     * @return TRUE if this environment is attached to the main CNAME
     */
    public boolean primary() {
        final EnvironmentDescription desc = this.description();
        final boolean primary = desc.getCNAME().startsWith(
            String.format("%s.", desc.getApplicationName())
        );
        Logger.info(
            this,
            "Environment '%s' considered primary: %B",
            this,
            primary
        );
        return primary;
    }

    /**
     * Get environment name.
     * @return Name of it
     */
    public String name() {
        return this.description().getEnvironmentName();
    }

    /**
     * Environment is in Green health?
     * @return TRUE if environment is in Green health
     */
    public boolean green() {
        return this.stable() && "Green".equals(this.description().getHealth());
    }

    /**
     * Wait for stable state, and return TRUE if achieved or FALSE if not.
     * @return TRUE if environment is stable
     */
    public boolean stable() {
        return this.until(
            new Environment.Barrier() {
                @Override
                public String message() {
                    return "stable state";
                }
                @Override
                public boolean allow(final EnvironmentDescription desc) {
                    return !desc.getStatus().matches(".*ing$");
                }
            }
        );
    }

    /**
     * Is it terminated?
     * @return Yes or no
     */
    public boolean terminated() {
        return this.stable()
            && "Terminated".equals(this.description().getStatus());
    }

    /**
     * Terminate environment.
     */
    public void terminate() {
        if (!this.stable()) {
            throw new DeploymentException(
                String.format(
                    "env '%s' is not stable, can't terminate",
                    this.eid
                )
            );
        }
        if (!this.terminated()) {
            final TerminateEnvironmentResult res =
                this.client.terminateEnvironment(
                    new TerminateEnvironmentRequest()
                        .withEnvironmentId(this.eid)
                        .withTerminateResources(true)
                );
            Logger.info(
                this,
                "Environment '%s/%s/%s' is terminated (label:'%s', status:%s)",
                res.getApplicationName(),
                res.getEnvironmentName(),
                res.getEnvironmentId(),
                res.getCNAME(),
                res.getVersionLabel(),
                res.getStatus()
            );
        }
    }

    /**
     * Get latest events.
     * @return Collection of events
     */
    public Collection<String> events() {
        if (!this.stable()) {
            throw new DeploymentException(
                String.format(
                    "env '%s' is not stable, can't get list of events",
                    this.eid
                )
            );
        }
        final DescribeEventsResult res = this.client.describeEvents(
            new DescribeEventsRequest().withEnvironmentId(this.eid)
        );
        final Collection<String> events = new LinkedList<String>();
        for (EventDescription desc : res.getEvents()) {
            events.add(
                String.format(
                    "[%s]: %s",
                    desc.getSeverity(),
                    desc.getMessage()
                )
            );
        }
        return events;
    }

    /**
     * Tail log.
     * @return Full text of tail log from the environment
     */
    public String tail() {
        if (!this.stable()) {
            throw new DeploymentException(
                String.format(
                    "env '%s' is not stable, can't get TAIL report",
                    this.eid
                )
            );
        }
        if (this.terminated()) {
            throw new DeploymentException(
                String.format(
                    "env '%s' is terminated, can't get TAIL report",
                    this.eid
                )
            );
        }
        this.client.requestEnvironmentInfo(
            new RequestEnvironmentInfoRequest()
                .withEnvironmentId(this.eid)
                .withInfoType(EnvironmentInfoType.Tail)
        );
        final RetrieveEnvironmentInfoRequest req =
            new RetrieveEnvironmentInfoRequest()
                .withEnvironmentId(this.eid)
                .withInfoType(EnvironmentInfoType.Tail);
        List<EnvironmentInfoDescription> infos;
        final long start = System.currentTimeMillis();
        do {
            if (System.currentTimeMillis() - start > Environment.MAX_DELAY_MS) {
                throw new DeploymentException(
                    String.format(
                        "env '%s' doesn't report its TAIL, time out",
                        this.eid
                    )
                );
            }
            Logger.info(
                this,
                "Waiting for TAIL report of %s",
                this.eid
            );
            infos = this.client
                .retrieveEnvironmentInfo(req)
                .getEnvironmentInfo();
        } while (infos.isEmpty());
        final EnvironmentInfoDescription desc = infos.get(0);
        try {
            return IOUtils.toString(new URL(desc.getMessage()).openStream());
        } catch (java.io.IOException ex) {
            throw new IllegalStateException(ex);
        }
    }

    /**
     * Get environment description of this.
     * @return The description
     */
    private EnvironmentDescription description() {
        final DescribeEnvironmentsResult res = this.client.describeEnvironments(
            new DescribeEnvironmentsRequest()
                .withEnvironmentIds(this.eid)
        );
        if (res.getEnvironments().isEmpty()) {
            throw new DeploymentException(
                String.format(
                    "environment '%s' not found",
                    this.eid
                )
            );
        }
        final EnvironmentDescription desc = res.getEnvironments().get(0);
        Logger.debug(
            this,
            // @checkstyle LineLength (1 line)
            "ID=%s, env=%s, app=%s, CNAME=%s, label=%s, template=%s, status=%s, health=%s",
            desc.getEnvironmentId(),
            desc.getEnvironmentName(),
            desc.getApplicationName(),
            desc.getCNAME(),
            desc.getVersionLabel(),
            desc.getTemplateName(),
            desc.getStatus(),
            desc.getHealth()
        );
        return desc;
    }

    /**
     * Wait for the barrier to pass.
     * @param barrier The barrier
     * @return TRUE if passed, FALSE if timeout
     */
    private boolean until(final Environment.Barrier barrier) {
        boolean passed = false;
        final long start = System.currentTimeMillis();
        while (true) {
            final EnvironmentDescription desc = this.description();
            Logger.info(
                this,
                // @checkstyle LineLength (1 line)
                "Environment '%s/%s/%s': health=%s, status=%s (waiting for %s, %[ms]s)",
                desc.getApplicationName(),
                desc.getEnvironmentName(),
                desc.getEnvironmentId(),
                desc.getHealth(),
                desc.getStatus(),
                barrier.message(),
                System.currentTimeMillis() - start
            );
            if (barrier.allow(desc)) {
                passed = true;
                break;
            }
            if (System.currentTimeMillis() - start > Environment.MAX_DELAY_MS) {
                Logger.warn(
                    this,
                    "Environment failed to reach '%s' after %[ms]s",
                    barrier.message(),
                    System.currentTimeMillis() - start
                );
                break;
            }
            try {
                TimeUnit.MINUTES.sleep(1);
            } catch (InterruptedException ex) {
                Thread.currentThread().interrupt();
                throw new DeploymentException(ex);
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
         * @return TRUE if we can continue, FALSE if extra cycle of waiting
         *  is required
         */
        boolean allow(EnvironmentDescription desc);
        /**
         * What are we waiting for?
         * @return Message to show in log
         */
        String message();
    }

}
