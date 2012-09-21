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

import com.jcabi.log.Logger;
import java.util.ArrayList;
import java.util.Collection;
import java.util.LinkedList;
import java.util.List;
import org.apache.maven.project.MavenProject;
import org.apache.maven.repository.internal.MavenRepositorySystemSession;
import org.sonatype.aether.RepositorySystem;
import org.sonatype.aether.RepositorySystemSession;
import org.sonatype.aether.artifact.Artifact;
import org.sonatype.aether.collection.CollectRequest;
import org.sonatype.aether.graph.Dependency;
import org.sonatype.aether.graph.DependencyFilter;
import org.sonatype.aether.repository.Authentication;
import org.sonatype.aether.repository.LocalRepository;
import org.sonatype.aether.repository.RemoteRepository;
import org.sonatype.aether.resolution.ArtifactResult;
import org.sonatype.aether.resolution.DependencyRequest;
import org.sonatype.aether.resolution.DependencyResolutionException;
import org.sonatype.aether.resolution.DependencyResult;
import org.sonatype.aether.util.filter.DependencyFilterUtils;

/**
 * Resolver of dependencies for one artifact.
 *
 * <p>You need the following dependencies to have in classpath in order to
 * to work with this class:
 *
 * <pre>
 * org.sonatype.aether:aether-api:1.13.1
 * org.apache.maven:maven-core:3.0.3
 * </pre>
 *
 * <p>The class is immutable and thread-safe.
 *
 * @author Yegor Bugayenko (yegor@jcabi.com)
 * @version $Id$
 * @since 0.1.6
 * @checkstyle ClassDataAbstractionCoupling (500 lines)
 * @see <a href="http://sonatype.github.com/sonatype-aether/apidocs/overview-tree.html">Aether 1.13.1 JavaDoc</a>
 */
public final class Aether {

    /**
     * The project.
     */
    private final transient MavenProject project;

    /**
     * Location of local repo.
     */
    private final transient String localRepo;

    /**
     * Public ctor, requires information about all remote repos and one
     * local.
     * @param prj The Maven project
     * @param repo Local repository location (file path)
     */
    public Aether(final MavenProject prj, final String repo) {
        if (prj == null) {
            throw new IllegalArgumentException("maven project can't be NULL");
        }
        if (repo == null) {
            throw new IllegalArgumentException("repo path can't be NULL");
        }
        this.project = prj;
        this.localRepo = repo;
    }

    /**
     * List of transitive deps of the artifact.
     * @param root The artifact to work with
     * @param scope The scope to work with ("runtime", "test", etc.)
     * @return The list of dependencies
     * @throws DependencyResolutionException If can't fetch it
     * @todo #51 This "filter IF NOT NULL" validation is a workaround,
     *  since I don't
     *  know what the actual problem is. Looks like sometimes (for some unknown
     *  reason) #classpathFilter() returns NULL. When exactly this may happen
     *  I have no idea. That's why this workaround. Sometime later we should
     *  do a proper testing and reproduce this defect in a test.
     */
    public List<Artifact> resolve(final Artifact root, final String scope)
        throws DependencyResolutionException {
        if (root == null) {
            throw new IllegalArgumentException("root artifact can't be NULL");
        }
        if (scope == null) {
            throw new IllegalArgumentException("scope can't be NULL");
        }
        final Dependency rdep = new Dependency(root, scope);
        final CollectRequest crq = this.request(rdep);
        final RepositorySystem system = new RepositorySystemBuilder().build();
        final List<Artifact> deps = new LinkedList<Artifact>();
        final DependencyFilter filter =
            DependencyFilterUtils.classpathFilter(scope);
        if (filter != null) {
            deps.addAll(
                this.fetch(
                    system,
                    this.session(system),
                    new DependencyRequest(crq, filter)
                )
            );
        }
        return deps;
    }

    /**
     * Fetch dependencies.
     * @param system The system to read from
     * @param session The session
     * @param dreq Dependency request
     * @return The list of dependencies
     * @throws DependencyResolutionException If can't fetch it
     * @todo #51 This catch of NPE is a temprorary measure. I don't know why
     *  Aether throws NPE in case of unresolveable artifact. This is the best
     *  I can do at the moment in order to protect clients of the class.
     */
    @SuppressWarnings("PMD.AvoidCatchingGenericException")
    private List<Artifact> fetch(final RepositorySystem system,
        final RepositorySystemSession session,
        final DependencyRequest dreq) throws DependencyResolutionException {
        final List<Artifact> deps = new LinkedList<Artifact>();
        try {
            Collection<ArtifactResult> results;
            synchronized (this.localRepo) {
                results = system
                    .resolveDependencies(session, dreq)
                    .getArtifactResults();
            }
            for (ArtifactResult res : results) {
                deps.add(res.getArtifact());
            }
        // @checkstyle IllegalCatch (1 line)
        } catch (Exception ex) {
            throw new DependencyResolutionException(
                new DependencyResult(dreq),
                new IllegalArgumentException(
                    Logger.format(
                        "failed to load '%s' from %[list]s into %s",
                        dreq.getCollectRequest().getRoot(),
                        Aether.reps(dreq.getCollectRequest().getRepositories()),
                        session.getLocalRepositoryManager()
                            .getRepository()
                            .getBasedir()
                    ),
                    ex
                )
            );
        }
        return deps;
    }

    /**
     * Create collect request.
     * @param root The root to start with
     * @return The request
     */
    private CollectRequest request(final Dependency root) {
        final CollectRequest request = new CollectRequest();
        request.setRoot(root);
        for (RemoteRepository repo
            : this.project.getRemoteProjectRepositories()) {
            if (!repo.getProtocol().matches("https?|file|s3")) {
                Logger.warn(
                    this,
                    "%s ignored (only S3, HTTP/S, and FILE are supported)",
                    repo
                );
                continue;
            }
            request.addRepository(repo);
        }
        return request;
    }

    /**
     * Convert a list of repositories into a list of strings.
     * @param repos The list of them
     * @return The list of texts
     */
    private static Collection<String> reps(
        final Collection<RemoteRepository> repos) {
        final Collection<String> texts = new ArrayList<String>(repos.size());
        final StringBuilder text = new StringBuilder();
        for (RemoteRepository repo : repos) {
            final Authentication auth = repo.getAuthentication();
            text.setLength(0);
            text.append(repo.toString());
            if (auth == null) {
                text.append(" without authentication");
            } else {
                text.append(" with ").append(auth.toString());
            }
            texts.add(text.toString());
        }
        return texts;
    }

    /**
     * Create RepositorySystemSession.
     * @param system The repository system to work with
     * @return The session
     */
    private RepositorySystemSession session(final RepositorySystem system) {
        final LocalRepository local = new LocalRepository(this.localRepo);
        final MavenRepositorySystemSession session =
            new MavenRepositorySystemSession();
        session.setLocalRepositoryManager(
            system.newLocalRepositoryManager(local)
        );
        session.setTransferListener(new LogTransferListener());
        return session;
    }

}
