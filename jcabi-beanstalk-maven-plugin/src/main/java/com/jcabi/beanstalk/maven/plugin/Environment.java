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
    private final transient String app;

    /**
     * Environment name.
     */
    private final transient String name;

    /**
     * Public ctor.
     * @param clnt The client
     * @param application Application name
     * @param env Environment name
     */
    public Environment(final AWSElasticBeanstalk clnt, final String application,
        final String env) {
        this.client = clnt;
        this.app = application;
        this.name = env;
        final DescribeConfigurationSettingsResult res =
            this.client.describeConfigurationSettings(
                new DescribeConfigurationSettingsRequest()
                    .withApplicationName(this.app)
                    .withEnvironmentName(this.name)
            );
        for (ConfigurationSettingsDescription config
            : res.getConfigurationSettings()) {
            Logger.debug(
                this,
                "Environment '%s/%s' settings:",
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
                    "environment '%s/%s' can't be terminated (time out)",
                    this.app,
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
            "Environment '%s/%s' is terminated (label:'%s', status:%s)",
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
                .withApplicationName(this.app)
                .withEnvironmentNames(this.name)
        );
        if (res.getEnvironments().isEmpty()) {
            throw new IllegalStateException(
                String.format(
                    "environment '%s/%s' not found",
                    this.app,
                    this.name
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
        int retry = 0;
        while (++retry < Environment.MAX_ATTEMPTS) {
            final EnvironmentDescription desc = this.description();
            Logger.info(
                this,
                // @checkstyle LineLength (1 line)
                "Environment '%s/%s': health=%s, status=%s, retry=%dmin of %d (waiting for %s)",
                desc.getApplicationName(),
                desc.getEnvironmentName(),
                desc.getHealth(),
                desc.getStatus(),
                retry,
                Environment.MAX_ATTEMPTS,
                barrier.message()
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
