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

import com.jcabi.aether.Aether;
import com.jcabi.log.Logger;
import java.util.Collection;
import java.util.List;
import org.apache.maven.plugin.AbstractMojo;
import org.apache.maven.plugin.MojoFailureException;
import org.apache.maven.project.MavenProject;
import org.slf4j.impl.StaticLoggerBinder;
import org.sonatype.aether.RepositorySystemSession;
import org.sonatype.aether.artifact.Artifact;
import org.sonatype.aether.resolution.DependencyResolutionException;
import org.sonatype.aether.util.artifact.DefaultArtifact;
import org.sonatype.aether.util.artifact.JavaScopes;

/**
 * Finds all artifacts by names in the current project.
 *
 * @author Yegor Bugayenko (yegor@tpc2.com)
 * @version $Id$
 * @goal run
 * @phase test
 * @threadSafe
 */
public class RunMojo extends AbstractMojo {

    /**
     * Maven project, to be injected by Maven itself.
     * @parameter expression="${project}"
     * @required
     * @readonly
     */
    private transient MavenProject project;

    /**
     * The current repository/network configuration of Maven.
     * @parameter default-value="${repositorySystemSession}"
     * @readonly
     */
    private transient RepositorySystemSession session;

    /**
     * List of coordinates to resolve.
     * @parameter expression="${jcabi.coordinates}"
     */
    private transient List<String> coordinates;

    /**
     * {@inheritDoc}
     */
    @Override
    public final void execute() throws MojoFailureException {
        StaticLoggerBinder.getSingleton().setMavenLog(this.getLog());
        final Aether aether = new Aether(
            this.project,
            this.session.getLocalRepository().getBasedir().getPath()
        );
        for (String coord : this.coordinates) {
            Logger.info(this, "%s:", coord);
            try {
                final Collection<Artifact> deps = aether.resolve(
                    new DefaultArtifact(coord),
                    JavaScopes.RUNTIME
                );
                for (Artifact dep : deps) {
                    Logger.info(this, "    %s", dep);
                }
            } catch (DependencyResolutionException ex) {
                throw new MojoFailureException(
                    String.format("failed to resolve '%s'", coord),
                    ex
                );
            }
        }
    }

}
