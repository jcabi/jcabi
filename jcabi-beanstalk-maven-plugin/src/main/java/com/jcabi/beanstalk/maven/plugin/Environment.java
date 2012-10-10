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
import com.amazonaws.services.elasticbeanstalk.model.EnvironmentDescription;
import com.amazonaws.services.elasticbeanstalk.model.TerminateEnvironmentRequest;
import com.amazonaws.services.elasticbeanstalk.model.TerminateEnvironmentResult;
import com.jcabi.log.Logger;
import java.util.concurrent.TimeUnit;

/**
 * EBT environment.
 *
 * @author Yegor Bugayenko (yegor@jcabi.com)
 * @version $Id$
 * @since 0.3
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
        final DescribeConfigurationSettingsResult res =
            this.client.describeConfigurationSettings(
                new DescribeConfigurationSettingsRequest()
                    .withTemplateName(desc.getTemplateName())
            );
        for (ConfigurationSettingsDescription config
            : res.getConfigurationSettings()) {
            Logger.debug(
                this,
                "Environment '%s/%s/%s' settings:",
                config.getApplicationName(),
                config.getEnvironmentName()
            );
            for (ConfigurationOptionSetting opt : config.getOptionSettings()) {
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

    /**
     * Is it primary environment in the application?
     * @return TRUE if this environment is attached to the main CNAME
     */
    public boolean primary() {
        final EnvironmentDescription desc = this.description();
        return desc.getCNAME().startsWith(
            String.format("%s.", desc.getEnvironmentName())
        );
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
        return this.until(
            new Environment.Barrier() {
                @Override
                public String message() {
                    return "Ready state";
                }
                @Override
                public boolean allow(final EnvironmentDescription desc) {
                    return "Ready".equals(desc.getStatus());
                }
            }
        ) && "Green".equals(this.description().getHealth());
    }

    /**
     * Terminate environment.
     */
    public void terminate() {
        final boolean ready = this.until(
            new Environment.Barrier() {
                @Override
                public String message() {
                    return "not Terminated/Launching";
                }
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
                    "environment '%s' can't be terminated (time out)",
                    this.eid
                )
            );
        }
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
            throw new IllegalStateException(
                String.format(
                    "environment '%s' not found",
                    this.eid
                )
            );
        }
        return res.getEnvironments().get(0);
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
