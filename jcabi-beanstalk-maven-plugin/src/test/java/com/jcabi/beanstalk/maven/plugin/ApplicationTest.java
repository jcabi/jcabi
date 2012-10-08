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
import com.amazonaws.services.s3.AmazonS3Client;
import org.apache.maven.plugin.logging.SystemStreamLog;
import java.io.File;
import org.apache.commons.io.FileUtils;
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
 */
public final class ApplicationTest {

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
     * Application can create a new environment name.
     * @throws Exception If something is wrong
     */
    @Test
    public void createsNewEnvironmentName() throws Exception {
        final AWSElasticBeanstalk aws = Mockito.mock(AWSElasticBeanstalk.class);
        final String name = "some-app-name";
        final String template = "some-template";
        final Version version = Mockito.mock(Version.class);
        final Application app = new Application(aws, name);
        MatcherAssert.assertThat(
            app.candidate(version, template),
            Matchers.notNullValue()
        );
    }

    /**
     * Environment can deploy and reverse with a broken WAR file.
     * @throws Exception If something is wrong
     */
    @Test
    public void deploysAndReversesWithLiveAccount() throws Exception {
        Assume.assumeThat(
            System.getProperty("aws.key"),
            Matchers.notNullValue()
        );
        final AWSCredentials creds = new BasicAWSCredentials(
            System.getProperty("aws.key"),
            System.getProperty("aws.secret")
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
            "netbout"
        );
        candidate.terminate();
    }

}
