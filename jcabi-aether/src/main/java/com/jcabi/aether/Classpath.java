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

import com.jcabi.aspects.Cacheable;
import com.jcabi.aspects.Loggable;
import java.io.File;
import java.util.AbstractSet;
import java.util.Collection;
import java.util.Iterator;
import java.util.LinkedHashSet;
import java.util.Set;
import javax.validation.constraints.NotNull;
import lombok.EqualsAndHashCode;
import lombok.ToString;
import org.apache.maven.artifact.DependencyResolutionRequiredException;
import org.apache.maven.model.Dependency;
import org.apache.maven.project.MavenProject;
import org.sonatype.aether.artifact.Artifact;
import org.sonatype.aether.resolution.DependencyResolutionException;
import org.sonatype.aether.util.artifact.DefaultArtifact;
import org.sonatype.aether.util.artifact.JavaScopes;

/**
 * A classpath of a Maven Project.
 *
 * <p>It is a convenient wrapper around {@link Aether} class, that allows you
 * to fetch all dependencies of a Maven Project by their scope. The class
 * implements a {@link Set} of {@link File}s and can be used like this:
 *
 * <pre> String classpath = StringUtils.join(
 *   new Classpath(project, localRepo, "runtime")
 *   System.getProperty("path.separator")
 * );</pre>
 *
 * @author Yegor Bugayenko (yegor@tpc2.com)
 * @version $Id$
 * @since 0.7.16
 */
@ToString
@EqualsAndHashCode(callSuper = false)
@Loggable(Loggable.DEBUG)
public final class Classpath extends AbstractSet<File> implements Set<File> {

    /**
     * Maven Project.
     */
    private final transient MavenProject project;

    /**
     * Aether to work with.
     */
    private final transient Aether aether;

    /**
     * Artifacts scope.
     */
    private final transient String scope;

    /**
     * Public ctor.
     * @param prj The Maven project
     * @param repo Local repository location (directory path)
     * @param scp The scope to use, e.g. "runtime" or "compile"
     */
    public Classpath(@NotNull final MavenProject prj,
        @NotNull final String repo, @NotNull final String scp) {
        super();
        this.project = prj;
        this.aether = new Aether(prj, repo);
        this.scope = scp;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public Iterator<File> iterator() {
        return this.fetch().iterator();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public int size() {
        return this.fetch().size();
    }

    /**
     * Fetch all files found (JAR, ZIP, directories, etc).
     * @return Set of files
     */
    @SuppressWarnings("PMD.AvoidInstantiatingObjectsInLoops")
    @Cacheable(forever = true)
    public Set<File> fetch() {
        final Set<String> paths = new LinkedHashSet<String>();
        paths.addAll(this.elements());
        for (Artifact artifact : this.artifacts()) {
            paths.add(artifact.getFile().getPath());
        }
        final Set<File> files = new LinkedHashSet<File>();
        for (String path : paths) {
            files.add(new File(path));
        }
        return files;
    }

    /**
     * Get Maven Project elements.
     * @return Collection of them
     */
    private Collection<String> elements() {
        Collection<String> elements;
        try {
            if (this.scope.equals(JavaScopes.TEST)) {
                elements = this.project.getTestClasspathElements();
            } else if (this.scope.equals(JavaScopes.RUNTIME)) {
                elements = this.project.getRuntimeClasspathElements();
            } else if (this.scope.equals(JavaScopes.SYSTEM)) {
                elements = this.project.getSystemClasspathElements();
            } else {
                elements = this.project.getCompileClasspathElements();
            }
        } catch (DependencyResolutionRequiredException ex) {
            throw new IllegalStateException("Failed to read classpath", ex);
        }
        return elements;
    }

    /**
     * Set of unique artifacts, which should be available in classpath.
     *
     * <p>This method gets a full list of artifacts of the project,
     * including their transitive dependencies.
     *
     * @return The set of artifacts
     */
    private Set<Artifact> artifacts() {
        final Set<Artifact> artifacts = new LinkedHashSet<Artifact>();
        for (RootArtifact root : this.roots()) {
            for (Artifact dep : this.deps(root)) {
                boolean found = false;
                for (Artifact exists : artifacts) {
                    if (dep.getArtifactId().equals(exists.getArtifactId())
                        && dep.getGroupId().equals(exists.getGroupId())
                        && dep.getClassifier().equals(exists.getClassifier())) {
                        found = true;
                        break;
                    }
                }
                if (found) {
                    continue;
                }
                if (root.excluded(dep)) {
                    continue;
                }
                artifacts.add(dep);
            }
        }
        return artifacts;
    }

    /**
     * Set of unique root artifacts.
     *
     * <p>The method is getting a list of artifacts from Maven Project, without
     * their transitive dependencies (that's why they are called "root"
     * artifacts).
     *
     * @return The set of root artifacts
     * @see #artifacts()
     */
    @SuppressWarnings("PMD.AvoidInstantiatingObjectsInLoops")
    private Set<RootArtifact> roots() {
        final Set<RootArtifact> roots = new LinkedHashSet<RootArtifact>();
        for (Dependency dep : this.project.getDependencies()) {
            if (!this.scope.equals(dep.getScope())) {
                continue;
            }
            roots.add(
                new RootArtifact(
                    new DefaultArtifact(
                        dep.getGroupId(),
                        dep.getArtifactId(),
                        dep.getClassifier(),
                        dep.getType(),
                        dep.getVersion()
                    ),
                    dep.getExclusions()
                )
            );
        }
        return roots;
    }

    /**
     * Get all deps of a root artifact.
     * @param root The root
     * @return The list of artifacts
     * @see #artifacts()
     */
    private Collection<Artifact> deps(final RootArtifact root) {
        Collection<Artifact> deps;
        try {
            deps = this.aether.resolve(root.artifact(), JavaScopes.RUNTIME);
        } catch (DependencyResolutionException ex) {
            throw new IllegalStateException(
                String.format("Failed to resolve '%s'", root),
                ex
            );
        }
        return deps;
    }

}

