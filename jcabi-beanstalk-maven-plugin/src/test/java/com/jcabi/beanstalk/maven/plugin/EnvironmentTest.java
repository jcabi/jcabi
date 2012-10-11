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
import com.amazonaws.auth.BasicAWSCredentials;
import com.amazonaws.services.elasticbeanstalk.AWSElasticBeanstalk;
import com.amazonaws.services.elasticbeanstalk.AWSElasticBeanstalkClient;
import com.amazonaws.services.elasticbeanstalk.model.ConfigurationSettingsDescription;
import com.amazonaws.services.elasticbeanstalk.model.DescribeConfigurationSettingsRequest;
import com.amazonaws.services.elasticbeanstalk.model.DescribeConfigurationSettingsResult;
import com.amazonaws.services.elasticbeanstalk.model.DescribeEnvironmentsRequest;
import com.amazonaws.services.elasticbeanstalk.model.DescribeEnvironmentsResult;
import com.amazonaws.services.elasticbeanstalk.model.EnvironmentDescription;
import com.jcabi.log.Logger;
import java.util.ArrayList;
import java.util.Arrays;
import org.apache.maven.plugin.logging.SystemStreamLog;
import org.hamcrest.MatcherAssert;
import org.hamcrest.Matchers;
import org.junit.Assume;
import org.junit.BeforeClass;
import org.junit.Test;
import org.mockito.Mockito;
import org.slf4j.impl.StaticLoggerBinder;

/**
 * Test case for {@link Environment}.
 * @author Yegor Bugayenko (yegor@jcabi.com)
 * @version $Id$
 * @checkstyle ClassDataAbstractionCoupling (500 lines)
 */
public final class EnvironmentTest {

    /**
     * AWS key, if provided in command line.
     */
    private static final String AWS_KEY = System.getProperty("aws.key");

    /**
     * AWS secret, if provided in command line.
     */
    private static final String AWS_SECRET = System.getProperty("aws.secret");

    /**
     * Configure logging.
     */
    @BeforeClass
    public static void initLog() {
        StaticLoggerBinder.getSingleton().setMavenLog(new SystemStreamLog());
    }

    /**
     * Environment can check readiness of environment.
     * @throws Exception If something is wrong
     */
    @Test
    public void checksReadinessOfEnvironment() throws Exception {
        final String eid = "some-env-id";
        final AWSElasticBeanstalk ebt = Mockito.mock(AWSElasticBeanstalk.class);
        Mockito.doReturn(
            new DescribeConfigurationSettingsResult().withConfigurationSettings(
                new ArrayList<ConfigurationSettingsDescription>()
            )
        ).when(ebt)
            .describeConfigurationSettings(
                Mockito.any(DescribeConfigurationSettingsRequest.class)
            );
        Mockito.doReturn(
            new DescribeEnvironmentsResult().withEnvironments(
                Arrays.asList(
                    new EnvironmentDescription()
                        .withStatus("Ready")
                        .withHealth("Red")
                )
            )
        ).when(ebt)
            .describeEnvironments(
                Mockito.any(DescribeEnvironmentsRequest.class)
            );
        final Environment env = new Environment(ebt, eid);
        MatcherAssert.assertThat(
            env.green(),
            Matchers.equalTo(false)
        );
    }

    /**
     * Environment can fetch TAIL report from live environment.
     * @throws Exception If something is wrong
     */
    @Test
    public void fetchesTailReportFromLiveEnvironment() throws Exception {
        Assume.assumeThat(EnvironmentTest.AWS_KEY, Matchers.notNullValue());
        final AWSCredentials creds = new BasicAWSCredentials(
            EnvironmentTest.AWS_KEY,
            EnvironmentTest.AWS_SECRET
        );
        final AWSElasticBeanstalk ebt = new AWSElasticBeanstalkClient(creds);
        final Environment env = new Environment(ebt, "e-2n2mqauqae");
        Logger.info(this, "tail report:\n%s", env.tail());
    }

    /**
     * Environment can collect events from running environment.
     * @throws Exception If something is wrong
     */
    @Test
    public void collectsEventsFromLiveEnvironment() throws Exception {
        Assume.assumeThat(EnvironmentTest.AWS_KEY, Matchers.notNullValue());
        final AWSCredentials creds = new BasicAWSCredentials(
            EnvironmentTest.AWS_KEY,
            EnvironmentTest.AWS_SECRET
        );
        final AWSElasticBeanstalk ebt = new AWSElasticBeanstalkClient(creds);
        final Environment env = new Environment(ebt, "e-nxmcbf3pvk");
        Logger.info(this, "events: %[list]s", env.events());
    }

}
