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
package com.jcabi.heroku.maven.plugin;

import com.jcabi.velocity.VelocityPage;
import com.rexsl.test.XhtmlMatchers;
import java.util.Arrays;
import org.apache.maven.artifact.DefaultArtifact;
import org.apache.maven.model.Build;
import org.apache.maven.model.Extension;
import org.apache.maven.project.MavenProject;
import org.apache.maven.settings.Server;
import org.apache.maven.settings.Settings;
import org.hamcrest.MatcherAssert;
import org.hamcrest.Matchers;
import org.junit.Test;

/**
 * Test case for {@link DeployMojo} (more detailed test is in maven invoker).
 * @author Yegor Bugayenko (yegor@jcabi.com)
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
     * DeployMojo can generate correct settings.xml file.
     * @throws Exception If something is wrong
     */
    @Test
    public void velocityTemplateCorrectlyBuildsSettingsXml() throws Exception {
        final Server server = new Server();
        server.setUsername("john");
        final Settings settings = new Settings();
        settings.addServer(server);
        MatcherAssert.assertThat(
            new VelocityPage(
                "com/jcabi/heroku/maven/plugin/settings.xml.vm"
            ).set("settings", settings).toString(),
            XhtmlMatchers.hasXPath(
                "//ns1:server[ns1:username='john']",
                "http://maven.apache.org/SETTINGS/1.0.0"
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
                .set(
                    "deps",
                    Arrays.asList(
                        new DefaultArtifact("fooo", "", "", "", "", "", null)
                    )
                )
                .toString(),
            Matchers.allOf(
                XhtmlMatchers.hasXPath(
                    "//ns1:extension[ns1:artifactId='test-foo']",
                    nspace
                ),
                XhtmlMatchers.hasXPath(
                    "//ns1:artifactItem[ns1:groupId='fooo']",
                    nspace
                )
            )
        );
    }

}
