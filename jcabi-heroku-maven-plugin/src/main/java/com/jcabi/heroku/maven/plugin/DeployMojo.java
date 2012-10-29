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

import com.jcabi.log.Logger;
import com.jcabi.velocity.VelocityPage;
import java.io.File;
import java.util.ArrayList;
import java.util.List;
import org.apache.maven.artifact.Artifact;
import org.apache.maven.artifact.DefaultArtifact;
import org.apache.maven.plugin.AbstractMojo;
import org.apache.maven.plugin.MojoFailureException;
import org.apache.maven.project.MavenProject;
import org.apache.maven.settings.Server;
import org.apache.maven.settings.Settings;
import org.jfrog.maven.annomojo.annotations.MojoGoal;
import org.jfrog.maven.annomojo.annotations.MojoParameter;
import org.jfrog.maven.annomojo.annotations.MojoPhase;
import org.slf4j.impl.StaticLoggerBinder;

/**
 * Deploys JAR/WAR artifact to Heroku.
 *
 * @author Yegor Bugayenko (yegor@jcabi.com)
 * @version $Id$
 * @since 0.4
 */
@MojoGoal("deploy")
@MojoPhase("deploy")
public final class DeployMojo extends AbstractMojo {

    /**
     * Maven project.
     */
    @MojoParameter(
        expression = "${project}",
        required = true,
        readonly = true,
        description = "Maven project"
    )
    private transient MavenProject project;

    /**
     * Setting.xml.
     */
    @MojoParameter(
        expression = "${settings}",
        required = true,
        readonly = true,
        description = "Maven settings.xml reference"
    )
    private transient Settings settings;

    /**
     * Shall we skip execution?
     */
    @MojoParameter(
        defaultValue = "false",
        required = false,
        description = "Skips execution"
    )
    private transient boolean skip;

    /**
     * Server ID from settings.xml.
     */
    @MojoParameter(
        defaultValue = "heroku.com",
        required = false,
        description = "Server ID from settings.xml"
    )
    private transient String server;

    /**
     * Application name.
     */
    @MojoParameter(
        required = true,
        description = "Heroku application name"
    )
    private transient String name;

    /**
     * Content of {@code Procfile}.
     */
    @MojoParameter(
        required = true,
        description = "Content of Procfile"
    )
    private transient String procfile;

    /**
     * List of artifacts to download.
     */
    @MojoParameter(
        required = true,
        description = "Artifacts to download during deployment"
    )
    private transient String[] artifacts;

    /**
     * Set skip option.
     * @param skp Shall we skip execution?
     */
    public void setSkip(final boolean skp) {
        this.skip = skp;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void execute() throws MojoFailureException {
        StaticLoggerBinder.getSingleton().setMavenLog(this.getLog());
        if (this.skip) {
            Logger.info(this, "execution skipped because of 'skip' option");
            return;
        }
        final long start = System.currentTimeMillis();
        final Heroku heroku = new Heroku(this.git(), this.name);
        final Repo repo = heroku.clone(
            new File(new File(this.project.getBuild().getDirectory()), "heroku")
        );
        try {
            repo.add(
                "settings.xml",
                new VelocityPage(
                    "com/jcabi/heroku/maven/plugin/settings.xml.vm"
                ).set("settings", this.settings).toString()
            );
            repo.add(
                "pom.xml",
                new VelocityPage(
                    "com/jcabi/heroku/maven/plugin/pom.xml.vm"
                ).set("project", this.project)
                    .set("deps", this.deps())
                    .set("timestamp", System.currentTimeMillis())
                    .toString()
            );
            repo.add("Procfile", this.procfile.trim());
        } catch (java.io.IOException ex) {
            throw new MojoFailureException("failed to save files", ex);
        }
        repo.commit();
        Logger.info(this, "Done in %[ms]s", System.currentTimeMillis() - start);
    }

    /**
     * Get git engine.
     * @return The engine
     * @throws MojoFailureException If somethings goes wrong
     */
    private Git git() throws MojoFailureException {
        final Server srv = this.settings.getServer(this.server);
        if (srv == null) {
            throw new MojoFailureException(
                String.format(
                    "Server '%s' not found in settings.xml",
                    this.server
                )
            );
        }
        final String location = srv.getPrivateKey();
        if (location == null || location.isEmpty()) {
            throw new MojoFailureException(
                String.format(
                    "privateKey is not defined for '%s' server in settings.xml",
                    srv.getId()
                )
            );
        }
        final File file = new File(location);
        if (!file.exists()) {
            throw new MojoFailureException(
                String.format("SSH key file '%s' doesn't exist", file)
            );
        }
        try {
            return new Git(
                file,
                new File(this.project.getBuild().getDirectory())
            );
        } catch (java.io.IOException ex) {
            throw new MojoFailureException("failed to initialize git", ex);
        }
    }

    /**
     * Create a collection of artifacts.
     *
     * <p>Coordinates should be formatted as
     * {@code groupId:artifactId:packaging:classifier:version}.
     *
     * @return List of them
     * @throws MojoFailureException If somethings goes wrong
     * @see <a href="http://maven.apache.org/pom.html#Maven_Coordinates">Maven coordinates</a>
     */
    @SuppressWarnings("PMD.AvoidInstantiatingObjectsInLoops")
    private List<Artifact> deps() throws MojoFailureException {
        if (this.artifacts.length == 0) {
            throw new MojoFailureException(
                "At least one artifact should be configured"
            );
        }
        final List<Artifact> deps = new ArrayList<Artifact>(
            this.artifacts.length
        );
        for (String coordinates : this.artifacts) {
            final String[] parts = coordinates.split(":");
            // @checkstyle MagicNumber (1 line)
            if (parts.length != 5) {
                throw new MojoFailureException(
                    String.format(
                        "Maven artifact coordinates '%s' is not absolute",
                        coordinates
                    )
                );
            }
            deps.add(
                // @checkstyle MagicNumber (10 lines)
                new DefaultArtifact(
                    parts[0],
                    parts[1],
                    parts[4],
                    "runtime",
                    parts[2],
                    parts[3],
                    null
                )
            );
        }
        return deps;
    }

}
