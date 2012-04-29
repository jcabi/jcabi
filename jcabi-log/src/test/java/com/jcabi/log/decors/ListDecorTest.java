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
package com.jcabi.log.decors;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Formattable;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;
import org.junit.runners.Parameterized.Parameters;

/**
 * Test case for {@link ListDecor}.
 * @author Yegor Bugayenko (yegor@jcabi.com)
 * @version $Id: ListDecorTest.java 324 2012-02-26 22:31:04Z guard $
 */
@RunWith(Parameterized.class)
@SuppressWarnings("PMD.TestClassWithoutTestCases")
public final class ListDecorTest extends AbstractDecorTest {

    /**
     * Public ctor.
     * @param list The list to test
     * @param text Expected text
     * @param flags Flags
     * @param width Width
     * @param precision Precission
     * @checkstyle ParameterNumber (3 lines)
     */
    public ListDecorTest(final Object list, final String text,
        final int flags, final int width, final int precision) {
        super(list, text, flags, width, precision);
    }

    /**
     * Params for this parametrized test.
     * @return Array of arrays of params for ctor
     * @todo #26 The SecretDecor class is not implemented yet, that's why
     *  the test is not enabled at the moment. You should uncomment the
     *  lines below and make sure the test passes.
     */
    @Parameters
    public static Collection<Object[]> params() {
        return Arrays.asList(
            new Object[][] {
                // @checkstyle MultipleStringLiterals (8 lines)
                {null, "[NULL]", 0, 0, 0},
                {new String[] {}, "[]", 0, 0, 0},
                {new String[] {"a"}, "[\"a\"]", 0, 0, 0},
                {new String[] {"b", "c"}, "[\"b\", \"c\"]", 0, 0, 0},
                {new ArrayList<String>(), "[]", 0, 0, 0},
                {Arrays.asList(new String[] {"x"}), "[\"x\"]", 0, 0, 0},
            }
        );
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected Formattable decor() throws Exception {
        return new ListDecor(this.object());
    }

}
