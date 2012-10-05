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
import com.amazonaws.services.elasticbeanstalk.model.ApplicationVersionDescription;
import com.amazonaws.services.elasticbeanstalk.model.CreateApplicationVersionRequest;
import com.amazonaws.services.elasticbeanstalk.model.CreateApplicationVersionResult;
import com.amazonaws.services.elasticbeanstalk.model.DescribeApplicationVersionsRequest;
import com.amazonaws.services.elasticbeanstalk.model.DescribeApplicationVersionsResult;
import com.amazonaws.services.elasticbeanstalk.model.S3Location;
import com.jcabi.log.Logger;
import java.io.File;
import java.util.Collection;

/**
 * EBT application version.
 *
 * @author Yegor Bugayenko (yegor@jcabi.com)
 * @version $Id$
 * @since 0.3
 */
final class Version {

    /**
     * AWS beanstalk client.
     */
    private final transient AWSElasticBeanstalk client;

    /**
     * Application name.
     */
    private final transient String application;

    /**
     * Bundle to use.
     */
    private final transient Bundle bundle;

    /**
     * Public ctor.
     * @param clnt Client
     * @param app Application name
     * @param bndl Bundle
     */
    public Version(final AWSElasticBeanstalk clnt, final String app,
        final Bundle bndl) {
        this.client = clnt;
        this.application = app;
        this.bundle = bndl;
    }

    /**
     * Get its label.
     * @return The label
     */
    public String label() {
        String label;
        if (this.exists()) {
            label = this.bundle.name();
        } else {
            final CreateApplicationVersionResult res =
                this.client.createApplicationVersion(
                    new CreateApplicationVersionRequest()
                        .withApplicationName(this.application)
                        .withVersionLabel(this.bundle.name())
                        .withSourceBundle(this.bundle.location())
                        .withDescription(this.bundle.name())
                );
            final ApplicationVersionDescription desc = res.getApplicationVersion();
            Logger.info(
                this,
                "Version %s is ready for %s",
                desc.getVersionLabel(),
                desc.getApplicationName()
            );
            label = desc.getVersionLabel();
        }
        return label;
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
            );
        final Collection<ApplicationVersionDescription> versions =
            res.getApplicationVersions();
        boolean exists = false;
        for (ApplicationVersionDescription ver : versions) {
            if (ver.getVersionLabel().equals(this.bundle.name())) {
                Logger.info(
                    this,
                    "Version '%s' already exists for '%s' app: '%s'",
                    ver.getVersionLabel(),
                    ver.getApplicationName(),
                    ver.getDescription()
                );
                exists = true;
                break;
            }
        }
        return exists;
    }

}
