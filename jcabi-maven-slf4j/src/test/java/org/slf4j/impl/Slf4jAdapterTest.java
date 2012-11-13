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
package org.slf4j.impl;

import org.apache.maven.monitor.logging.DefaultLog;
import org.codehaus.plexus.logging.Logger;
import org.codehaus.plexus.logging.console.ConsoleLogger;
import org.hamcrest.MatcherAssert;
import org.hamcrest.Matchers;
import org.junit.Test;

/**
 * Test case for {@link Slf4jAdapter}.
 *
 * @author Yegor Bugayenko (yegor@tpc2.com)
 * @version $Id$
 */
public final class Slf4jAdapterTest {

    /**
     * Slf4jAdapter can send log messages through.
     * @throws Exception If something wrong inside
     * @checkstyle NoWhitespaceAfter (100 lines)
     * @checkstyle ExecutableStatementCount (100 lines)
     */
    @Test
    public void sendsLogMessagesThrough() throws Exception {
        final Slf4jAdapter logger = new Slf4jAdapter();
        logger.setMavenLog(
            new DefaultLog(
                new ConsoleLogger(
                    Logger.LEVEL_DEBUG,
                    Slf4jAdapterTest.class.getName()
                )
            )
        );
        logger.isTraceEnabled();
        logger.isDebugEnabled();
        logger.isInfoEnabled();
        logger.isWarnEnabled();
        logger.isErrorEnabled();
        MatcherAssert.assertThat(
            Slf4jAdapter.class.getName(),
            Matchers.equalTo(logger.getName())
        );
        logger.trace("trace-test message");
        logger.trace("trace-test-2", new IllegalArgumentException("trace-ex"));
        logger.trace("trace-test {}", "trace-message");
        logger.trace("trace-test {} {}", "trc1", "trc2");
        logger.trace("trace-test-2 {} {}", new String[] {"trace-1", "trace-2"});
        logger.debug("debug-test message");
        logger.debug("debug-test-2", new IllegalArgumentException("debug-ex"));
        logger.debug("debug-test {}", "debug-message");
        logger.debug("debug-test {} {}", "dbg1", "dbg2");
        logger.debug("debug-test-2 {} {}", new String[] {"debug-1", "debug-2"});
        logger.info("info-test message");
        logger.info("info-test-2", new IllegalArgumentException("info-ex"));
        logger.info("info-test {}", "info-message");
        logger.info("info-test {} {}", "inf1", "inf2");
        logger.info("info-test-2 {} {}", new String[] {"info-1", "info-2"});
        logger.warn("warn-test message");
        logger.warn("warn-test-2", new IllegalArgumentException("warn-ex"));
        logger.warn("warn-test {}", "warn-message");
        logger.warn("warn-test {} {}", "warn--1", "warn--2");
        logger.warn("warn-test-2 {} {}", new String[] {"warn-1", "warn-2"});
        logger.error("error-test message");
        logger.error("error-test-2", new IllegalArgumentException("error-ex"));
        logger.error("error-test {}", "error-message");
        logger.error("error-test {} {}", "error-1", "error-2");
        logger.error("error-test-2 {} {}", new String[] {"err-1", "err-2"});
    }

}
