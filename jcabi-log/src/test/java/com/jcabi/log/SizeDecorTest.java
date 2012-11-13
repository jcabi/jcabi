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

import java.util.Arrays;
import java.util.Collection;
import java.util.Formattable;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;
import org.junit.runners.Parameterized.Parameters;

/**
 * Test case for {@link SizeDecor}.
 * @author Marina Kosenko (marina.kosenko@gmail.com)
 * @author Yegor Bugayenko (yegor@tpc2.com)
 * @version $Id$
 */
@RunWith(Parameterized.class)
@SuppressWarnings("PMD.TestClassWithoutTestCases")
public final class SizeDecorTest extends AbstractDecorTest {

    /**
     * Public ctor.
     * @param size The size
     * @param text Expected text
     * @param flags Flags
     * @param width Width
     * @param precision Precission
     * @checkstyle ParameterNumber (3 lines)
     */
    public SizeDecorTest(final Long size, final String text,
        final int flags, final int width, final int precision) {
        super(size, text, flags, width, precision);
    }

    /**
     * Params for this parametrized test.
     * @return Array of arrays of params for ctor
     * @todo #26 The SizeDecor class is not implemented yet, that's why
     *  the test is not enabled at the moment. You should uncomment the
     *  lines below and make sure the test passes.
     */
    @Parameters
    public static Collection<Object[]> params() {
        return Arrays.asList(
            new Object[][] {
                // @checkstyle MethodBodyComments (12 lines)
                // { null, "NULL", 0, 0, 0 },
                // { 1L, "1b", 0, 0, 0 },
                // { 123L, "  123b", 0, 6, 0 },
                // { 1024L, "1Kb", 0, 0, 3 },
                // { 5120L, "5Kb", 0, 0, 0 },
                // { 12345L, "12.056Kb", 0, 0, 3 },
                // { 12345L, "12.1Kb  ", FormattableFlags.LEFT_JUSTIFY, 8, 1 },
                // { 98765432L, "94.190MB", FormattableFlags.UPPERCASE, 0, 3 },
                // { 98765432L, "94.190Mb", 0, 0, 3 },
                // { 90L * 1024 * 1024 * 1024, "90Gb", 0, 0, 0 },
                // { 13L * 1024 * 1024 * 1024 * 1024, "13Tb", 0, 0, 0 },
            }
        );
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected Formattable decor() {
        return new SizeDecor((Long) this.object());
    }

}
