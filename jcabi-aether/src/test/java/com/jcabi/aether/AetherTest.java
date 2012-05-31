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
package com.jcabi.aether;

import java.io.File;
import java.util.Arrays;
import java.util.List;
import org.apache.maven.project.MavenProject;
import org.hamcrest.MatcherAssert;
import org.hamcrest.Matchers;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;
import org.mockito.Mockito;
import org.sonatype.aether.artifact.Artifact;
import org.sonatype.aether.repository.RemoteRepository;
import org.sonatype.aether.util.artifact.DefaultArtifact;
import org.sonatype.aether.util.artifact.JavaScopes;

/**
 * Test case for {@link Aether}.
 * @author Yegor Bugayenko (yegor@jcabi.com)
 * @version $Id$
 */
public final class AetherTest {

    /**
     * Temp dir.
     * @checkstyle VisibilityModifier (3 lines)
     */
    @Rule
    public final transient TemporaryFolder temp = new TemporaryFolder();

    /**
     * Aether can find and load artifacts.
     * @throws Exception If there is some problem inside
     */
    @Test
    @SuppressWarnings("unchecked")
    public void findsAndLoadsArtifacts() throws Exception {
        final MavenProject project = Mockito.mock(MavenProject.class);
        Mockito.doReturn(
            Arrays.asList(
                new RemoteRepository(
                    "id",
                    "default",
                    "http://repo1.maven.org/maven2/"
                )
            )
        ).when(project).getRemoteProjectRepositories();
        final File local = this.temp.newFolder("local-repository");
        final Aether aether = new Aether(project, local.getPath());
        final List<Artifact> deps = aether.resolve(
            new DefaultArtifact(
                "com.jcabi",
                "jcabi-log",
                "",
                "jar",
                "0.1.5"
            ),
            JavaScopes.RUNTIME
        );
        MatcherAssert.assertThat(
            deps,
            Matchers.allOf(
                Matchers.<Artifact>iterableWithSize(Matchers.greaterThan(1)),
                Matchers.<Artifact>hasItems(
                    Matchers.<Artifact>hasProperty(
                        "file",
                        Matchers.hasToString(
                            Matchers.endsWith("/jcabi-log-0.1.5.jar")
                        )
                    )
                )
            )
        );
    }

}
