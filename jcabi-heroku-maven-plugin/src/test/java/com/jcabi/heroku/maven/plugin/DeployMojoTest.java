/**
 * Copyright (c) 2012-2013, JCabi.com
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
package com.jcabi.heroku.maven.plugin;

import com.jcabi.velocity.VelocityPage;
import com.rexsl.test.XhtmlMatchers;
import java.io.StringReader;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;
import java.util.Properties;
import org.apache.maven.artifact.DefaultArtifact;
import org.apache.maven.model.Build;
import org.apache.maven.model.Extension;
import org.apache.maven.project.MavenProject;
import org.apache.maven.settings.Server;
import org.apache.maven.settings.Settings;
import org.hamcrest.MatcherAssert;
import org.hamcrest.Matchers;
import org.junit.Assert;
import org.junit.Test;

/**
 * Test case for {@link DeployMojo} (more detailed test is in maven invoker).
 * @author Yegor Bugayenko (yegor@tpc2.com)
 * @version $Id$
 * @checkstyle ClassDataAbstractionCoupling (500 lines)
 */
public final class DeployMojoTest {

    /**
     * DeployMojo can skip execution when flag is set.
     * @throws Exception If something is wrong
     */
    @Test
    public void skipsExecutionWhenRequired() throws Exception {
        final DeployMojo mojo = new DeployMojo();
        mojo.setSkip(true);
        mojo.execute();
    }

    /**
     * DeployMojo can generate correct system.properties file.
     * @throws Exception If something is wrong
     */
    @Test
    @SuppressWarnings("PMD.UseConcurrentHashMap")
    public void velocityTemplateCorrectlyBuildsSysProps() throws Exception {
        final String expectedKey = "java.runtime.version";
        final String expectedVal = "1.7";
        final Map<String, String> sysProps = new HashMap<String, String>();
        sysProps.put(
            expectedKey,
            expectedVal
        );
        final StringReader stringReader = new StringReader(
            new VelocityPage(
                "com/jcabi/heroku/maven/plugin/system.properties.vm")
                .set("systemProperties", sysProps).toString());
        final Properties properties = new Properties();
        properties.load(stringReader);
        Assert.assertEquals(
            expectedVal,
            properties.getProperty(expectedKey)
        );
    }

    /**
     * DeployMojo can generate correct settings.xml file.
     * @throws Exception If something is wrong
     */
    @Test
    public void velocityTemplateCorrectlyBuildsSettingsXml() throws Exception {
        final Server server = new Server();
        server.setUsername("john");
        server.setPassword("xxx");
        final Settings settings = new Settings();
        settings.addServer(server);
        final String nspace = "http://maven.apache.org/SETTINGS/1.0.0";
        MatcherAssert.assertThat(
            new VelocityPage(
                "com/jcabi/heroku/maven/plugin/settings.xml.vm"
            ).set("settings", settings).toString(),
            Matchers.allOf(
                XhtmlMatchers.hasXPath(
                    "//ns1:server[ns1:username='john' and ns1:password='xxx']",
                    nspace
                ),
                XhtmlMatchers.hasXPath(
                    "//ns1:server[ns1:username='john' and not(ns1:privateKey)]",
                    nspace
                )
            )
        );
    }

    /**
     * DeployMojo can generate correct settings.xml file.
     * @throws Exception If something is wrong
     */
    @Test
    public void velocityTemplateCorrectlyBuildsPomXml() throws Exception {
        final Build build = new Build();
        final Extension ext = new Extension();
        ext.setArtifactId("test-foo");
        build.addExtension(ext);
        final MavenProject project = new MavenProject();
        project.setBuild(build);
        final String nspace = "http://maven.apache.org/POM/4.0.0";
        MatcherAssert.assertThat(
            new VelocityPage(
                "com/jcabi/heroku/maven/plugin/pom.xml.vm"
            ).set("project", project)
                .set("timestamp", "332211")
                .set(
                    "deps",
                    Arrays.asList(
                        new DefaultArtifact("fooo", "", "", "", "", "", null)
                    )
                )
                .toString(),
            Matchers.allOf(
                XhtmlMatchers.hasXPath(
                    "//ns1:name[.='332211']",
                    nspace
                ),
                XhtmlMatchers.hasXPath(
                    "//ns1:extension[ns1:artifactId='test-foo']",
                    nspace
                ),
                XhtmlMatchers.hasXPath(
                    "//ns1:dependency[ns1:groupId='fooo']",
                    nspace
                ),
                XhtmlMatchers.hasXPath(
                    "//ns1:configuration[ns1:outputDirectory='${basedir}']",
                    nspace
                )
            )
        );
    }

}
