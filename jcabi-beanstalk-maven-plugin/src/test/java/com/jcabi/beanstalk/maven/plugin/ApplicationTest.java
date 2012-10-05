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
import org.apache.maven.plugin.logging.SystemStreamLog;
import org.hamcrest.MatcherAssert;
import org.hamcrest.Matchers;
import org.junit.Assume;
import org.junit.BeforeClass;
import org.junit.Test;
import org.mockito.Mockito;
import org.slf4j.impl.StaticLoggerBinder;

/**
 * Test case for {@link Application}.
 *
 * @author Yegor Bugayenko (yegor@jcabi.com)
 * @version $Id$
 */
public final class ApplicationTest {

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
        final Application app = new Application(aws, name);
        MatcherAssert.assertThat(
            app.candidate("old-name"),
            Matchers.notNullValue()
        );
    }

    /**
     * Application can create a new env name.
     * @throws Exception If something is wrong
     */
    @Test
    public void createsNewEnvironmentNameInLiveServer() throws Exception {
        Assume.assumeThat(
            System.getProperty("aws.key"),
            Matchers.notNullValue()
        );
        final AWSCredentials creds = new BasicAWSCredentials(
            System.getProperty("aws.key"),
            System.getProperty("aws.secret")
        );
        final AWSElasticBeanstalk aws = new AWSElasticBeanstalkClient(creds);
        final Application app = new Application(aws, "netbout");
        MatcherAssert.assertThat(
            app.candidate("netbout"),
            Matchers.notNullValue()
        );
    }

}
