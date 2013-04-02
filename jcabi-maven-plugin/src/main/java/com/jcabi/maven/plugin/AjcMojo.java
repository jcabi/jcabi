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
package com.jcabi.maven.plugin;

import com.jcabi.log.Logger;
import java.io.File;
import java.io.FileFilter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Date;
import javax.validation.constraints.NotNull;
import lombok.EqualsAndHashCode;
import lombok.ToString;
import org.apache.commons.io.FileUtils;
import org.apache.commons.io.filefilter.DirectoryFileFilter;
import org.apache.commons.io.filefilter.NotFileFilter;
import org.apache.commons.io.filefilter.TrueFileFilter;
import org.apache.commons.io.filefilter.WildcardFileFilter;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.time.DateFormatUtils;
import org.apache.maven.plugin.AbstractMojo;
import org.apache.maven.plugin.MojoFailureException;
import org.apache.maven.project.MavenProject;
import org.jfrog.maven.annomojo.annotations.MojoGoal;
import org.jfrog.maven.annomojo.annotations.MojoParameter;
import org.jfrog.maven.annomojo.annotations.MojoPhase;
import org.slf4j.impl.StaticLoggerBinder;

/**
 * AspectJ compile CLASS files.
 *
 * @author Yegor Bugayenko (yegor@tpc2.com)
 * @version $Id$
 * @since 0.7.16
 */
@MojoGoal("ajc")
@MojoPhase("process-classes")
@ToString
@EqualsAndHashCode(callSuper = false)
public final class AjcMojo extends AbstractMojo {

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
     * Source directory.
     */
    @MojoParameter(
        required = true,
        readonly = false,
        description = "Source directory with .java files"
    )
    private transient String sourceDirectory;

    /**
     * Directory with compiled files (they should be there already).
     */
    @MojoParameter(
        required = true,
        readonly = false,
        description = "Destination directory with .class files"
    )
    private transient String outputDirectory;

    /**
     * {@inheritDoc}
     */
    @Override
    public void execute() throws MojoFailureException {
        StaticLoggerBinder.getSingleton().setMavenLog(this.getLog());
        final File src = new File(this.project.getBuild().getSourceDirectory());
                                                <target name="main">
                                                    <iajc sourceroots="${aspectj.src}"
                                                        inpath="${aspectj.bin}"
                                                        destdir="${aspectj.temp}"
                                                        classpathref="classpath"
                                                        aspectPath="${aspectj.libs}"
                                                        warn="constructorName,packageDefaultMethod,deprecation,maskedCatchBlocks,unusedLocals,unusedArguments,unusedImports,syntheticAccess,assertIdentifier"
                                                        debug="false"
                                                        time="true"
                                                        verbose="false"
                                                        source="1.6"
                                                        target="1.6"
                                                        showWeaveInfo="true"
                                                        failonerror="true" />
    }

    /**
     * Create and return a text of the version file.
     * @param dir The destination directory
     * @return The text
     */
    private String text(@NotNull final File dir) {
        return String.format(
            "Project Version: %s\nBuild Number: %s\nBuild Date: %s\n\n%s",
            this.project.getVersion(),
            this.buildNumber,
            DateFormatUtils.ISO_DATETIME_FORMAT.format(new Date()),
            StringUtils.join(VersionalizeMojo.files(dir, "*"), "\n")
        );
    }

    /**
     * Versionalize packages from source to dest.
     * @param src Source directory
     * @param dest Destination
     * @throws IOException If some IO problem
     */
    @SuppressWarnings("PMD.AvoidInstantiatingObjectsInLoops")
    private void versionalize(@NotNull final File src, @NotNull final File dest)
        throws IOException {
        final Collection<File> dirs = FileUtils.listFilesAndDirs(
            src,
            new NotFileFilter(TrueFileFilter.INSTANCE),
            DirectoryFileFilter.DIRECTORY
        );
        final String name = String.format(
            "%s-%s-%s.txt",
            VersionalizeMojo.cleanup(this.project.getGroupId()),
            VersionalizeMojo.cleanup(this.project.getArtifactId()),
            VersionalizeMojo.cleanup(this.project.getPackaging())
        );
        for (File dir : dirs) {
            if (VersionalizeMojo.files(dir, "*.java").isEmpty()) {
                continue;
            }
            final File ddir = new File(
                dest,
                dir.getCanonicalPath().substring(
                    src.getCanonicalPath().length() + 1
                )
            );
            final File version = new File(ddir, name);
            version.getParentFile().mkdirs();
            FileUtils.write(version, this.text(ddir));
            Logger.info(this, "File %s added", version);
        }
    }

    /**
     * Clean the text.
     * @param text The text
     * @return Clean version of it
     */
    private static String cleanup(final String text) {
        return text.replaceAll("[^_a-z0-9\\-]", "-");
    }

    /**
     * All Java files in the directory.
     * @param dir The directory
     * @param mask Mask to use
     * @return List of Java file names
     */
    private static Collection<String> files(final File dir, final String mask) {
        final FileFilter filter = new WildcardFileFilter(mask);
        final File[] files = dir.listFiles(filter);
        final Collection<String> names = new ArrayList<String>(files.length);
        for (File file : files) {
            names.add(file.getName());
        }
        return names;
    }

}
