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
import com.amazonaws.services.elasticbeanstalk.model.CheckDNSAvailabilityRequest;
import com.amazonaws.services.elasticbeanstalk.model.CheckDNSAvailabilityResult;
import com.amazonaws.services.elasticbeanstalk.model.ConfigurationSettingsDescription;
import com.amazonaws.services.elasticbeanstalk.model.CreateEnvironmentRequest;
import com.amazonaws.services.elasticbeanstalk.model.CreateEnvironmentResult;
import com.amazonaws.services.elasticbeanstalk.model.DescribeConfigurationSettingsRequest;
import com.amazonaws.services.elasticbeanstalk.model.DescribeConfigurationSettingsResult;
import com.amazonaws.services.elasticbeanstalk.model.DescribeEnvironmentsRequest;
import com.amazonaws.services.elasticbeanstalk.model.DescribeEnvironmentsResult;
import com.amazonaws.services.elasticbeanstalk.model.EnvironmentDescription;
import com.amazonaws.services.elasticbeanstalk.model.TerminateEnvironmentRequest;
import com.amazonaws.services.elasticbeanstalk.model.TerminateEnvironmentResult;
import com.amazonaws.services.s3.AmazonS3Client;
import com.jcabi.log.Logger;
import java.io.File;
import java.util.ArrayList;
import java.util.Arrays;
import org.apache.commons.io.FileUtils;
import org.apache.maven.plugin.logging.SystemStreamLog;
import org.hamcrest.MatcherAssert;
import org.hamcrest.Matchers;
import org.junit.Assume;
import org.junit.BeforeClass;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;
import org.mockito.Mockito;
import org.slf4j.impl.StaticLoggerBinder;

/**
 * Test case for {@link Application}.
 * @author Yegor Bugayenko (yegor@jcabi.com)
 * @version $Id$
 * @checkstyle ClassDataAbstractionCoupling (500 lines)
 */
@SuppressWarnings("PMD.ExcessiveImports")
public final class ApplicationTest {

    /**
     * AWS key, if provided in command line.
     */
    private static final String AWS_KEY = System.getProperty("aws.key");

    /**
     * AWS secret, if provided in command line.
     */
    private static final String AWS_SECRET = System.getProperty("aws.secret");

    /**
     * Temporary folder.
     * @checkstyle VisibilityModifier (3 lines)
     */
    @Rule
    public transient TemporaryFolder temp = new TemporaryFolder();

    /**
     * Configure logging.
     */
    @BeforeClass
    public static void initLog() {
        StaticLoggerBinder.getSingleton().setMavenLog(new SystemStreamLog());
    }

    /**
     * Application can create a new environment.
     * @throws Exception If something is wrong
     */
    @Test
    public void createsNewEnvironment() throws Exception {
        final String name = "some-app-name";
        final String template = "some-template";
        final Version version = Mockito.mock(Version.class);
        final AWSElasticBeanstalk ebt = Mockito.mock(AWSElasticBeanstalk.class);
        Mockito.doReturn(
            new CheckDNSAvailabilityResult().withAvailable(true)
        ).when(ebt)
            .checkDNSAvailability(
                Mockito.any(CheckDNSAvailabilityRequest.class)
            );
        Mockito.doReturn(
            new CreateEnvironmentResult()
                .withApplicationName(name)
                .withEnvironmentName(name)
        ).when(ebt)
            .createEnvironment(
                Mockito.any(CreateEnvironmentRequest.class)
            );
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
                        .withCNAME("")
                        .withStatus("Ready")
                )
            )
        ).when(ebt)
            .describeEnvironments(
                Mockito.any(DescribeEnvironmentsRequest.class)
            );
        Mockito.doReturn(new TerminateEnvironmentResult())
            .when(ebt)
            .terminateEnvironment(
                Mockito.any(TerminateEnvironmentRequest.class)
            );
        final Application app = new Application(ebt, name);
        app.clean();
        MatcherAssert.assertThat(
            app.candidate(version, template),
            Matchers.notNullValue()
        );
    }

    /**
     * Environment can deploy and reverse with a broken WAR file. This test
     * has to be executed only if you have full access to AWS S3 bucket, and
     * AWS EBT for deployment. The test runs full cycle of deployment and then
     * destroying of a new environment. It won't hurt anything, but will
     * consume some EBT resources. Be careful.
     *
     * @throws Exception If something is wrong
     */
    @Test
    public void deploysAndReversesWithLiveAccount() throws Exception {
        Assume.assumeThat(ApplicationTest.AWS_KEY, Matchers.notNullValue());
        final AWSCredentials creds = new BasicAWSCredentials(
            ApplicationTest.AWS_KEY,
            ApplicationTest.AWS_SECRET
        );
        final AWSElasticBeanstalk ebt = new AWSElasticBeanstalkClient(creds);
        final String name = "netbout";
        final Application app = new Application(ebt, name);
        final File war = this.temp.newFile("temp.war");
        FileUtils.writeStringToFile(war, "broken JAR file content");
        final Environment candidate = app.candidate(
            new OverridingVersion(
                ebt,
                name,
                new OverridingBundle(
                    new AmazonS3Client(creds),
                    "webapps.netbout.com",
                    war.getName(),
                    war
                )
            ),
            name
        );
        MatcherAssert.assertThat(candidate.green(), Matchers.equalTo(false));
        Logger.info(this, "tail report:\n%s", candidate.tail());
        candidate.terminate();
    }

}
