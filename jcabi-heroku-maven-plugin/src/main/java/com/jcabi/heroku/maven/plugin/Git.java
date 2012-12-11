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

import com.jcabi.aspects.RetryOnFailure;
import com.jcabi.log.Logger;
import com.jcabi.log.VerboseProcess;
import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import javax.validation.constraints.NotNull;
import org.apache.commons.io.FileUtils;
import org.apache.commons.lang.StringUtils;

/**
 * Git engine.
 *
 * @author Yegor Bugayenko (yegor@tpc2.com)
 * @version $Id$
 * @since 0.4
 * @checkstyle ClassDataAbstractionCoupling (500 lines)
 */
final class Git {

    /**
     * Permissions to set to SSH key file.
     */
    @SuppressWarnings("PMD.AvoidUsingOctalValues")
    private static final int PERMS = 0600;

    /**
     * Default SSH location.
     */
    private static final String SSH = "/usr/bin/ssh";

    /**
     * Location of shell script.
     */
    private final transient File script;

    /**
     * Public ctor.
     * @param key Location of SSH key
     * @param temp Temp directory
     * @throws IOException If some error inside
     */
    public Git(@NotNull final File key,
        @NotNull final File temp) throws IOException {
        if (!new File(Git.SSH).exists()) {
            throw new IllegalStateException(
                String.format("SSH is not installed at '%s'", Git.SSH)
            );
        }
        final File kfile = new File(temp, "heroku.pem");
        FileUtils.copyFile(key, kfile);
        this.chmod(kfile, Git.PERMS);
        this.script = new File(temp, "git-ssh.sh");
        FileUtils.writeStringToFile(
            this.script,
            String.format(
                // @checkstyle LineLength (1 line)
                "set -x && %s -o UserKnownHostsFile=/dev/null -o StrictHostKeyChecking=no -i '%s' $@",
                Git.SSH,
                kfile.getAbsolutePath()
            )
        );
        this.script.setExecutable(true);
    }

    /**
     * Execute git with these arguments.
     * @param dir In which directory to run it
     * @param args Arguments to pass to it
     * @return Stdout
     * @checkstyle MagicNumber (2 lines)
     */
    @RetryOnFailure(delay = 3000, attempts = 2)
    public String exec(final File dir, final String... args) {
        final List<String> commands = new ArrayList<String>(args.length + 1);
        commands.add("git");
        for (String arg : args) {
            commands.add(arg);
        }
        Logger.info(this, "%s:...", StringUtils.join(commands, " "));
        final ProcessBuilder builder = new ProcessBuilder(commands);
        builder.directory(dir);
        builder.environment().put("GIT_SSH", this.script.getAbsolutePath());
        return new VerboseProcess(builder).stdout();
    }

    /**
     * Change file permissions.
     * @param file The file to change
     * @param mode Permissions to set
     * @throws IOException If some error inside
     * @see http://stackoverflow.com/questions/664432
     * @see http://stackoverflow.com/questions/1556119
     */
    private void chmod(final File file, final int mode) throws IOException {
        new VerboseProcess(
            new ProcessBuilder(
                "chmod",
                String.format("%04o", mode),
                file.getAbsolutePath()
            )
        ).stdout();
        Logger.debug(
            this,
            "chmod(%s, %3o): succeeded",
            file,
            mode
        );
    }

}
