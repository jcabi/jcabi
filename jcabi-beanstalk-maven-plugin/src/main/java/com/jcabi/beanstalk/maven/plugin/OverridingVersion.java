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
import com.amazonaws.services.elasticbeanstalk.model.ApplicationVersionDescription;
import com.amazonaws.services.elasticbeanstalk.model.CreateApplicationVersionRequest;
import com.amazonaws.services.elasticbeanstalk.model.CreateApplicationVersionResult;
import com.amazonaws.services.elasticbeanstalk.model.DeleteApplicationVersionRequest;
import com.amazonaws.services.elasticbeanstalk.model.DescribeApplicationVersionsRequest;
import com.amazonaws.services.elasticbeanstalk.model.DescribeApplicationVersionsResult;
import com.amazonaws.services.elasticbeanstalk.model.S3Location;
import com.jcabi.log.Logger;

/**
 * EBT application version.
 *
 * @author Yegor Bugayenko (yegor@jcabi.com)
 * @version $Id$
 * @since 0.3
 */
final class OverridingVersion implements Version {

    /**
     * AWS beanstalk client.
     */
    private final transient AWSElasticBeanstalk client;

    /**
     * Application name.
     */
    private final transient String application;

    /**
     * Version name.
     */
    private final transient String name;

    /**
     * S3 location of the bundle.
     */
    private final transient S3Location location;

    /**
     * Public ctor.
     * @param clnt Client
     * @param app Application name
     * @param bundle Bundle
     */
    public OverridingVersion(final AWSElasticBeanstalk clnt, final String app,
        final Bundle bundle) {
        this.client = clnt;
        this.application = app;
        this.name = bundle.name();
        this.location = bundle.location();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public String toString() {
        return this.name;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public String label() {
        if (this.exists()) {
            this.remove();
        }
        final CreateApplicationVersionResult res =
            this.client.createApplicationVersion(
                new CreateApplicationVersionRequest()
                    .withApplicationName(this.application)
                    .withVersionLabel(this.name)
                    .withSourceBundle(this.location)
                    .withDescription(this.name)
            );
        final ApplicationVersionDescription desc = res.getApplicationVersion();
        Logger.info(
            this,
            "Version '%s' created for '%s' (%s): '%s'",
            desc.getVersionLabel(),
            desc.getApplicationName(),
            this.location,
            desc.getDescription()
        );
        return desc.getVersionLabel();
    }

    /**
     * This label exists already?
     * @return Yes or no
     */
    private boolean exists() {
        final DescribeApplicationVersionsResult res =
            this.client.describeApplicationVersions(
                new DescribeApplicationVersionsRequest()
                    .withApplicationName(this.application)
                    .withVersionLabels(this.name)
            );
        boolean exists = false;
        if (!res.getApplicationVersions().isEmpty()) {
            final ApplicationVersionDescription ver =
                res.getApplicationVersions().get(0);
            Logger.info(
                this,
                "Version '%s' already exists for '%s' app: '%s'",
                ver.getVersionLabel(),
                ver.getApplicationName(),
                ver.getDescription()
            );
            exists = true;
        }
        return exists;
    }

    /**
     * Remove this label.
     */
    private void remove() {
        this.client.deleteApplicationVersion(
            new DeleteApplicationVersionRequest()
                .withApplicationName(this.application)
                .withVersionLabel(this.name)
        );
        Logger.info(
            this,
            "Version '%s' deleted for '%s' app",
            this.name,
            this.application
        );
    }

}