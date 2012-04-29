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
package com.jcabi.log;

import org.hamcrest.MatcherAssert;
import org.hamcrest.Matchers;
import org.junit.Test;

/**
 * Test case for {@link PreFormatter}.
 * @author Marina Kosenko (marina.kosenko@gmail.com)
 * @author Yegor Bugayenko (yegor@jcabi.com)
 * @version $Id$
 */
public final class PreFormatterTest {

    /**
     * PreFormatter can format simple texts.
     */
    @Test
    public void decoratesArguments() {
        final PreFormatter pre = new PreFormatter(
            "%[com.jcabi.log.DecorMocker]-5.2f and %1$+.6f",
            1d,
            "some text"
        );
        MatcherAssert.assertThat(
            pre.getFormat(),
            Matchers.equalTo("%-5.2f and %1$+.6f")
        );
        MatcherAssert.assertThat(
            pre.getArguments()[0],
            Matchers.instanceOf(DecorMocker.class)
        );
        MatcherAssert.assertThat(
            pre.getArguments()[1],
            Matchers.instanceOf(String.class)
        );
    }

    /**
     * PreFormatter can handle missed decors.
     */
    @Test
    public void formatsEvenWithMissedDecors() {
        final PreFormatter pre =
            new PreFormatter("ouch: %[missed]s", "test");
        MatcherAssert.assertThat(
            pre.getFormat(),
            Matchers.equalTo("ouch: %s")
        );
        MatcherAssert.assertThat(
            pre.getArguments()[0],
            Matchers.instanceOf(String.class)
        );
    }

    /**
     * PreFormatter can handle directly provided decors.
     */
    @Test
    public void formatsWithDirectlyProvidedDecors() {
        final DecorMocker decor = new DecorMocker("a");
        final PreFormatter pre = new PreFormatter("test: %s", decor);
        MatcherAssert.assertThat(
            pre.getArguments()[0],
            Matchers.equalTo((Object) decor)
        );
    }

}
