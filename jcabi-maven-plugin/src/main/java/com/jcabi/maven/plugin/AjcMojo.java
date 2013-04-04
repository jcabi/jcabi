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
import com.jcabi.aether.Classpath;
import com.jcabi.aspects.Cacheable;
import com.jcabi.aspects.Loggable;
import com.jcabi.log.Logger;
import java.io.File;
import java.io.IOException;
import java.util.Arrays;
import java.util.Collection;
import java.util.LinkedList;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.TimeUnit;
import lombok.EqualsAndHashCode;
import lombok.ToString;
import org.apache.commons.io.FileUtils;
import org.apache.commons.io.filefilter.TrueFileFilter;
import org.apache.commons.lang3.StringUtils;
import org.apache.maven.plugin.AbstractMojo;
import org.apache.maven.plugin.MojoFailureException;
import org.apache.maven.project.MavenProject;
import org.aspectj.bridge.IMessage;
import org.aspectj.bridge.IMessageHolder;
import org.aspectj.tools.ajc.Main;
import org.jfrog.maven.annomojo.annotations.MojoGoal;
import org.jfrog.maven.annomojo.annotations.MojoParameter;
import org.jfrog.maven.annomojo.annotations.MojoPhase;
import org.slf4j.impl.StaticLoggerBinder;
import org.sonatype.aether.RepositorySystemSession;
import org.sonatype.aether.util.artifact.JavaScopes;

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
@Loggable(Loggable.DEBUG)
@SuppressWarnings({ "PMD.TooManyMethods", "PMD.ExcessiveImports" })
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
        readonly = true
    )
    private transient MavenProject project;

    /**
     * Maven project.
     */
    @MojoParameter(
        expression = "${repositorySystemSession}",
        required = true,
        readonly = true
    )
    private transient RepositorySystemSession session;

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
    @Loggable(value = Loggable.DEBUG, limit = 1, unit = TimeUnit.MINUTES)
    public void execute() throws MojoFailureException {
        StaticLoggerBinder.getSingleton().setMavenLog(this.getLog());
        this.classesDirectory.mkdirs();
        this.tempDirectory.mkdirs();
        final Main main = new Main();
        final IMessageHolder mholder = new AjcMojo.MsgHolder();
        final String jdk = "1.6";
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
                jdk,
                "-target",
                jdk,
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
            mholder
        );
        try {
            FileUtils.copyDirectory(this.tempDirectory, this.classesDirectory);
        } catch (IOException ex) {
            throw new MojoFailureException("failed to copy files back", ex);
        }
        Logger.info(
            this,
            "ajc result: %d file(s), %d error(s), %d warning(s)",
            AjcMojo.files(this.tempDirectory).size(),
            mholder.numMessages(IMessage.ERROR, true),
            mholder.numMessages(IMessage.WARNING, false)
        );
        if (mholder.hasAnyMessage(IMessage.ERROR, true)) {
            throw new MojoFailureException("AJC failed, see log above");
        }
    }

    /**
     * Get classpath for AJC.
     * @return Classpath
     */
    @Loggable(value = Loggable.DEBUG, limit = 1, unit = TimeUnit.MINUTES)
    @Cacheable(forever = true)
    private String classpath() {
        return StringUtils.join(
            new Classpath(
                this.project,
                this.session.getLocalRepository().getBasedir(),
                Arrays.asList(
                    JavaScopes.COMPILE,
                    JavaScopes.PROVIDED,
                    JavaScopes.SYSTEM
                )
            ),
            AjcMojo.SEP
        );
    }

    /**
     * Get locations of all aspect libraries for AJC.
     * @return Classpath
     */
    @Cacheable(forever = true)
    private String aspectpath() {
        return new StringBuilder()
            .append(this.classpath())
            .append(AjcMojo.SEP)
            .append(System.getProperty("java.class.path"))
            .toString();
    }

    /**
     * Get locations of all source roots (with aspects in source form).
     * @return Directories separated
     */
    @Cacheable(forever = true)
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

    /**
     * Find all files in the directory.
     * @param dir The directory
     * @return List of them
     */
    private static Collection<File> files(final File dir) {
        final Collection<File> files = new LinkedList<File>();
        final Collection<File> all = FileUtils.listFiles(
            dir, TrueFileFilter.INSTANCE, TrueFileFilter.INSTANCE
        );
        for (File file : all) {
            if (file.isFile()) {
                files.add(file);
            }
        }
        return files;
    }

    /**
     * Message holder.
     */
    private static final class MsgHolder implements IMessageHolder {
        /**
         * All messages seen so far.
         */
        private final transient Collection<IMessage> messages =
            new CopyOnWriteArrayList<IMessage>();
        /**
         * {@inheritDoc}
         */
        @Override
        public boolean hasAnyMessage(final IMessage.Kind kind,
            final boolean greater) {
            boolean has = false;
            for (IMessage msg : this.messages) {
                has = msg.getKind().equals(kind) || greater
                    && IMessage.Kind.COMPARATOR
                    .compare(msg.getKind(), kind) > 0;
                if (has) {
                    break;
                }
            }
            return has;
        }
        /**
         * {@inheritDoc}
         */
        @Override
        public int numMessages(final IMessage.Kind kind,
            final boolean greater) {
            int num = 0;
            for (IMessage msg : this.messages) {
                final boolean has = msg.getKind().equals(kind) || greater
                    && IMessage.Kind.COMPARATOR
                    .compare(msg.getKind(), kind) > 0;
                if (has) {
                    ++num;
                }
            }
            return num;
        }
        /**
         * {@inheritDoc}
         */
        @Override
        public IMessage[] getMessages(final IMessage.Kind kind,
            final boolean greater) {
            throw new UnsupportedOperationException();
        }
        /**
         * {@inheritDoc}
         */
        @Override
        public List<IMessage> getUnmodifiableListView() {
            throw new UnsupportedOperationException();
        }
        /**
         * {@inheritDoc}
         */
        @Override
        public void clearMessages() {
            throw new UnsupportedOperationException();
        }
        /**
         * {@inheritDoc}
         */
        @Override
        public boolean handleMessage(final IMessage msg) {
            if (msg.getKind().equals(IMessage.ERROR)
                || msg.getKind().equals(IMessage.FAIL)
                || msg.getKind().equals(IMessage.ABORT)) {
                Logger.error(AjcMojo.class, msg.getMessage());
            } else if (msg.getKind().equals(IMessage.WARNING)) {
                Logger.warn(AjcMojo.class, msg.getMessage());
            } else if (msg.getKind().equals(IMessage.WEAVEINFO)
                || msg.getKind().equals(IMessage.INFO)) {
                Logger.info(AjcMojo.class, msg.getMessage());
            } else {
                Logger.debug(AjcMojo.class, msg.getMessage());
            }
            this.messages.add(msg);
            return true;
        }
        /**
         * {@inheritDoc}
         */
        @Override
        public boolean isIgnoring(final IMessage.Kind kind) {
            return false;
        }
        /**
         * {@inheritDoc}
         */
        @Override
        public void dontIgnore(final IMessage.Kind kind) {
            assert kind != null;
        }
        /**
         * {@inheritDoc}
         */
        @Override
        public void ignore(final IMessage.Kind kind) {
            assert kind != null;
        }
    }

}
