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

import com.jcabi.log.VerboseRunnable;
import com.jcabi.log.VerboseThreads;
import java.io.File;
import java.util.Arrays;
import java.util.concurrent.Callable;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import org.apache.maven.project.MavenProject;
import org.hamcrest.CustomMatcher;
import org.hamcrest.Matcher;
import org.hamcrest.MatcherAssert;
import org.hamcrest.Matchers;
import org.junit.Assert;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;
import org.mockito.Mockito;
import org.sonatype.aether.artifact.Artifact;
import org.sonatype.aether.repository.Authentication;
import org.sonatype.aether.repository.RemoteRepository;
import org.sonatype.aether.resolution.DependencyResolutionException;
import org.sonatype.aether.util.artifact.DefaultArtifact;
import org.sonatype.aether.util.artifact.JavaScopes;

/**
 * Test case for {@link Aether}.
 * @author Yegor Bugayenko (yegor@jcabi.com)
 * @version $Id$
 * @checkstyle ClassDataAbstractionCoupling (500 lines)
 */
@SuppressWarnings({ "PMD.DoNotUseThreads", "PMD.TooManyMethods" })
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
    public void findsAndLoadsArtifacts() throws Exception {
        final File local = this.temp.newFolder("local-repository");
        final Aether aether = new Aether(this.project(), local.getPath());
        final Artifact[] artifacts = new Artifact[] {
            // @checkstyle LineLength (1 line)
            new DefaultArtifact("com.jcabi.aether-test:parent:pom:1.0-SNAPSHOT"),
            new DefaultArtifact("com.jcabi:jcabi-log:pom:1.0-SNAPSHOT"),
            new DefaultArtifact("log4j:log4j:jar:1.2.16"),
        };
        final Matcher<?> matcher = new CustomMatcher<String>("file exists") {
            @Override
            public boolean matches(final Object file) {
                return File.class.cast(file).exists();
            }
        };
        for (Artifact artifact : artifacts) {
            MatcherAssert.assertThat(
                aether.resolve(artifact, JavaScopes.RUNTIME),
                Matchers.<Artifact>everyItem(
                    Matchers.<Artifact>hasProperty("file", matcher)
                )
            );
        }
    }

    /**
     * Aether can resolve in parallel threads.
     * @throws Exception If there is some problem inside
     */
    @Test
    public void resolvesArtifactsInParallelThreads() throws Exception {
        final File local = this.temp.newFolder("local-repo-2");
        final Aether aether = new Aether(this.project(), local.getPath());
        final int threads = Runtime.getRuntime().availableProcessors() * 5;
        final Artifact artifact = new DefaultArtifact(
            "com.jcabi:jcabi-assembly:pom:0.1.10"
        );
        final CountDownLatch start = new CountDownLatch(1);
        final CountDownLatch latch = new CountDownLatch(threads);
        final Runnable task = new VerboseRunnable(
            new Callable<Void>() {
                @Override
                public Void call() throws Exception {
                    start.await();
                    MatcherAssert.assertThat(
                        aether.resolve(artifact, JavaScopes.RUNTIME),
                        Matchers.not(Matchers.<Artifact>empty())
                    );
                    latch.countDown();
                    return null;
                }
            },
            true
        );
        final ExecutorService svc =
            Executors.newFixedThreadPool(threads, new VerboseThreads());
        for (int thread = 0; thread < threads; ++thread) {
            svc.submit(task);
        }
        start.countDown();
        MatcherAssert.assertThat(
            latch.await(2, TimeUnit.MINUTES),
            Matchers.is(true)
        );
        svc.shutdown();
    }

    /**
     * Aether can reject NULL maven project.
     * @throws Exception If there is some problem inside
     */
    @Test(expected = IllegalArgumentException.class)
    public void rejectsNullMavenProject() throws Exception {
        new Aether(null, this.temp.newFolder("local-repository-77").getPath());
    }

    /**
     * Aether can reject NULL repo path.
     * @throws Exception If there is some problem inside
     */
    @Test(expected = IllegalArgumentException.class)
    public void rejectsNullRepoPath() throws Exception {
        new Aether(this.project(), null);
    }

    /**
     * Aether can reject NULL artifact.
     * @throws Exception If there is some problem inside
     */
    @Test(expected = IllegalArgumentException.class)
    public void rejectsNullArtifact() throws Exception {
        new Aether(this.project(), this.temp.newFolder("repo-1").getPath())
            .resolve(null, JavaScopes.RUNTIME);
    }

    /**
     * Aether can reject NULL scope.
     * @throws Exception If there is some problem inside
     */
    @Test(expected = IllegalArgumentException.class)
    public void rejectsNullScope() throws Exception {
        new Aether(this.project(), this.temp.newFolder("repo-2").getPath())
            .resolve(new DefaultArtifact("junit:junit:4.10"), null);
    }

    /**
     * Aether can throw on non-found artifact.
     * @throws Exception If there is some problem inside
     */
    @Test(expected = DependencyResolutionException.class)
    public void throwsWhenArtifactNotFound() throws Exception {
        new Aether(
            this.project(),
            this.temp.newFolder("local-repo-97").getPath()
        ).resolve(
            new DefaultArtifact("com.jcabi:jcabi-log:jar:0.0.0"),
            JavaScopes.COMPILE
        );
    }

    /**
     * Aether can recover after failure.
     * @throws Exception If there is some problem inside
     */
    @Test
    public void recoversAfterFailure() throws Exception {
        final Aether aether = new Aether(
            this.project(),
            this.temp.newFolder("local-repo-99").getPath()
        );
        try {
            aether.resolve(
                new DefaultArtifact("com.jcabi:jcabi-x:jar:0.0.0"),
                JavaScopes.TEST
            );
            Assert.fail("expection expected here");
        } catch (DependencyResolutionException ex) {
            assert ex != null;
        }
        MatcherAssert.assertThat(
            aether.resolve(
                new DefaultArtifact("com.jcabi:jcabi-log:jar:0.1.8"),
                JavaScopes.RUNTIME
            ),
            Matchers.not(Matchers.<Artifact>empty())
        );
    }

    /**
     * Make mock maven project.
     * @return The project
     * @throws Exception If there is some problem inside
     */
    private MavenProject project() throws Exception {
        final MavenProject project = Mockito.mock(MavenProject.class);
        final String type = "default";
        final RemoteRepository aws = new RemoteRepository(
            "aether-test",
            type,
            "s3://aether-test.jcabi.com/snapshot"
        );
        aws.setAuthentication(
            new Authentication(
                System.getProperty("aws.key"),
                System.getProperty("aws.secret")
            )
        );
        Mockito.doReturn(
            Arrays.asList(
                new RemoteRepository[] {
                    aws,
                    new RemoteRepository(
                        "sonatype",
                        type,
                        "https://oss.sonatype.org/content/groups/public"
                    ),
                    new RemoteRepository(
                        "maven-central",
                        type,
                        "http://repo1.maven.org/maven2/"
                    ),
                }
            )
        ).when(project).getRemoteProjectRepositories();
        return project;
    }

}
