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
package com.jcabi.aether;

import java.io.File;
import java.util.Arrays;
import java.util.List;
import org.apache.maven.model.Dependency;
import org.apache.maven.project.MavenProject;
import org.hamcrest.MatcherAssert;
import org.hamcrest.Matchers;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;
import org.mockito.Mockito;
import org.sonatype.aether.repository.RemoteRepository;
import org.sonatype.aether.util.artifact.JavaScopes;

/**
 * Test case for {@link Classpath}.
 * @author Yegor Bugayenko (yegor@tpc2.com)
 * @version $Id$
 */
public final class ClasspathTest {

    /**
     * Temp dir.
     * @checkstyle VisibilityModifier (3 lines)
     */
    @Rule
    public final transient TemporaryFolder temp = new TemporaryFolder();

    /**
     * Classpath can build a classpath.
     * @throws Exception If there is some problem inside
     */
    @Test
    @SuppressWarnings("unchecked")
    public void buildsClasspath() throws Exception {
        final File local = this.temp.newFolder();
        final MavenProject project = Mockito.mock(MavenProject.class);
        Mockito.doReturn(Arrays.asList("/some/path/as/directory"))
            .when(project).getTestClasspathElements();
        final Dependency dep = new Dependency();
        dep.setGroupId("junit");
        dep.setArtifactId("junit");
        dep.setVersion("4.10");
        dep.setScope(JavaScopes.TEST);
        Mockito.doReturn(Arrays.asList(dep)).when(project).getDependencies();
        final List<RemoteRepository> repos = Arrays.asList(
            new RemoteRepository(
                "maven-central",
                "default",
                "http://repo1.maven.org/maven2/"
            )
        );
        Mockito.doReturn(repos).when(project).getRemoteProjectRepositories();
        MatcherAssert.assertThat(
            new Classpath(project, local.getPath(), JavaScopes.TEST),
            Matchers.<File>hasItems(
                Matchers.hasToString(Matchers.endsWith("/as/directory")),
                Matchers.hasToString(Matchers.endsWith("junit-4.10.jar")),
                Matchers.hasToString(Matchers.endsWith("hamcrest-core-1.1.jar"))
            )
        );
    }

}
