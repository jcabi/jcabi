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
package com.jcabi.log;

import org.hamcrest.MatcherAssert;
import org.hamcrest.Matchers;
import org.junit.Assert;
import org.junit.Test;

/**
 * Test case for {@link VerboseProcess}.
 * @author Yegor Bugayenko (yegor@tpc2.com)
 * @version $Id$
 */
public final class VerboseProcessTest {

    /**
     * VerboseProcess can run a command line script.
     * @throws Exception If something goes wrong
     */
    @Test
    public void runsACommandLineScript() throws Exception {
        final VerboseProcess process = new VerboseProcess(
            new ProcessBuilder("echo", "hello, world!")
        );
        MatcherAssert.assertThat(
            process.stdout(),
            Matchers.containsString("hello")
        );
    }

    /**
     * VerboseProcess can run a command line script with exception.
     * @throws Exception If something goes wrong
     */
    @Test
    public void runsACommandLineScriptWithException() throws Exception {
        final VerboseProcess process = new VerboseProcess(
            new ProcessBuilder("cat", "/non-existing-file.txt")
        );
        try {
            process.stdout();
            Assert.fail("exception expected");
        } catch (IllegalArgumentException ex) {
            MatcherAssert.assertThat(
                ex.getMessage(),
                Matchers.containsString("No such file or directory")
            );
        }
    }

    /**
     * VerboseProcess can handle a long running command.
     * @throws Exception If something goes wrong
     */
    @Test
    public void handlesLongRunningCommand() throws Exception {
        final VerboseProcess process = new VerboseProcess(
            new ProcessBuilder("sleep", "2")
        );
        MatcherAssert.assertThat(
            process.stdout(),
            Matchers.equalTo("")
        );
    }

    /**
     * VerboseProcess can reject NULL.
     * @throws Exception If something goes wrong
     */
    @Test(expected = javax.validation.ConstraintViolationException.class)
    public void rejectsNullProcesses() throws Exception {
        final ProcessBuilder builder = null;
        new VerboseProcess(builder);
    }

}
