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

import java.util.Arrays;
import java.util.Collection;
import java.util.Formattable;
import java.util.FormattableFlags;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;
import org.junit.runners.Parameterized.Parameters;

/**
 * Test case for {@link MsDecor}.
 * @author Yegor Bugayenko (yegor@tpc2.com)
 * @version $Id$
 */
@RunWith(Parameterized.class)
@SuppressWarnings("PMD.TestClassWithoutTestCases")
public final class MsDecorTest extends AbstractDecorTest {

    /**
     * Public ctor.
     * @param nano The amount of nanoseconds
     * @param text Expected text
     * @param flags Flags
     * @param width Width
     * @param precision Precission
     * @checkstyle ParameterNumber (3 lines)
     */
    public MsDecorTest(final Long nano, final String text,
        final int flags, final int width, final int precision) {
        super(nano, text, flags, width, precision);
    }

    /**
     * Params for this parametrized test.
     * @return Array of arrays of params for ctor
     */
    @Parameters
    public static Collection<Object[]> params() {
        return Arrays.asList(
            new Object[][] {
                // @checkstyle LineLength (20 lines)
                // @checkstyle MagicNumber (20 lines)
                {null, "NULL", 0, 0, 0},
                {13L, "13ms", 0, 0, -1},
                {13L, "13.0ms", 0, 0, 1},
                {1024L, "1s", 0, 0, 0},
                {6001L, "6.0010s", 0, 0, 4},
                {122001L, "  2MIN", FormattableFlags.UPPERCASE, 6, 0},
                {3789003L, "63min", 0, 0, 0},
            }
        );
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected Formattable decor() {
        return new MsDecor((Long) this.object());
    }

}
