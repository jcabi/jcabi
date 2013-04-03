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

import com.google.common.io.Files;
import com.jcabi.log.Logger;
import java.io.File;
import java.io.IOException;
import java.util.List;
import lombok.EqualsAndHashCode;
import lombok.ToString;
import org.apache.commons.io.FileUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.maven.artifact.DependencyResolutionRequiredException;
import org.apache.maven.plugin.AbstractMojo;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugin.MojoFailureException;
import org.apache.maven.project.MavenProject;
import org.aspectj.bridge.IMessage;
import org.aspectj.bridge.IMessageHolder;
import org.aspectj.tools.ajc.Main;
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
 * @link <a href="http://www.eclipse.org/aspectj/doc/next/devguide/ajc-ref.html">AJC compiler manual</a>
 */
@MojoGoal("ajc")
@MojoPhase("process-classes")
@ToString
@EqualsAndHashCode(callSuper = false)
public final class AjcMojo extends AbstractMojo {

    /**
     * Classpath separator.
     */
    private static final String SEP = System.getProperty("path.separator");

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
     * Compiled directory.
     */
    @MojoParameter(
        required = false,
        readonly = false,
        description = "Directory with compiled .class files",
        defaultValue = "${project.build.outputDirectory}"
    )
    private transient File classesDirectory;

    /**
     * Directories with aspects.
     */
    @MojoParameter(
        required = false,
        readonly = false,
        description = "Directories with aspects"
    )
    private transient File[] aspectDirectories;

    /**
     * Temporary directory.
     */
    @MojoParameter(
        defaultValue = "${project.build.directory}/jcabi-ajc",
        required = false,
        readonly = false,
        description = "Temporary directory for compiled classes"
    )
    private transient File tempDirectory;

    /**
     * {@inheritDoc}
     */
    @Override
    public void execute() throws MojoFailureException {
        StaticLoggerBinder.getSingleton().setMavenLog(this.getLog());
        this.classesDirectory.mkdirs();
        this.tempDirectory.mkdirs();
        final Main main = new Main();
        main.run(
            new String[] {
                "-inpath",
                this.classesDirectory.getAbsolutePath(),
                "-sourceroots",
                this.sourceroots(),
                "-d",
                this.tempDirectory.getAbsolutePath(),
                "-classpath",
                this.classpath(),
                "-aspectpath",
                this.aspectpath(),
                "-source",
                "1.6",
                "-target",
                "1.6",
                "-g:none",
                "-encoding",
                "UTF-8",
                "-time",
                "-showWeaveInfo",
                "-warn:constructorName",
                "-warn:packageDefaultMethod",
                "-warn:deprecation",
                "-warn:maskedCatchBlocks",
                "-warn:unusedLocals",
                "-warn:unusedArguments",
                "-warn:unusedImports",
                "-warn:syntheticAccess",
                "-warn:assertIdentifier",
            },
            new IMessageHolder() {
                @Override
                public boolean hasAnyMessage(final IMessage.Kind kind,
                    final boolean bln) {
                    return false;
                }
                @Override
                public int numMessages(final IMessage.Kind kind,
                    final boolean bln) {
                    return 0;
                }
                @Override
                public IMessage[] getMessages(final IMessage.Kind kind,
                    final boolean bln) {
                    return new IMessage[] {};
                }
                @Override
                public List<IMessage> getUnmodifiableListView() {
                    throw new UnsupportedOperationException();
                }
                @Override
                public void clearMessages() {
                    throw new UnsupportedOperationException();
                }
                @Override
                public boolean handleMessage(final IMessage msg) {
                    Logger.info(AjcMojo.class, msg.getMessage());
                    return true;
                }
                @Override
                public boolean isIgnoring(final IMessage.Kind kind) {
                    return false;
                }
                @Override
                public void dontIgnore(final IMessage.Kind kind) {
                    assert kind != null;
                }
                @Override
                public void ignore(final IMessage.Kind kind) {
                    assert kind != null;
                }
            }
        );
        try {
            FileUtils.copyDirectoryToDirectory(
                this.tempDirectory,
                this.classesDirectory
            );
        } catch (IOException ex) {
            throw new MojoFailureException("failed to copy files back", ex);
        }
    }

    /**
     * Get classpath for AJC.
     * @return Classpath
     */
    private String classpath() {
        try {
            return StringUtils.join(
                this.project.getCompileClasspathElements(),
                AjcMojo.SEP
            );
        } catch (DependencyResolutionRequiredException ex) {
            throw new IllegalStateException(ex);
        }
    }

    /**
     * Get locations of all aspect libraries for AJC.
     * @return Classpath
     */
    private String aspectpath() {
        return System.getProperty("java.class.path");
    }

    /**
     * Get locations of all source roots (with aspects in source form).
     * @return Directories separated
     */
    private String sourceroots() {
        String path;
        if (this.aspectDirectories == null
            || this.aspectDirectories.length == 0) {
            path = Files.createTempDir().getAbsolutePath();
        } else {
            for (File dir : this.aspectDirectories) {
                if (!dir.exists()) {
                    throw new IllegalStateException(
                        String.format("source directory %s is absent", dir)
                    );
                }
            }
            path = StringUtils.join(this.aspectDirectories, AjcMojo.SEP);
        }
        return path;
    }

}
