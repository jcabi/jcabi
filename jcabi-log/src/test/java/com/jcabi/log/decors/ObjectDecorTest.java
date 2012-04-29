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

import java.util.Arrays;
import java.util.Collection;
import java.util.Formattable;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;
import org.junit.runners.Parameterized.Parameters;

/**
 * Test case for {@link ObjectDecor}.
 * @author Marina Kosenko (marina.kosenko@gmail.com)
 * @author Yegor Bugayenko (yegor@jcabi.com)
 * @version $Id: ObjectDecorTest.java 341 2012-04-25 21:00:53Z guard $
 */
@RunWith(Parameterized.class)
@SuppressWarnings("PMD.TestClassWithoutTestCases")
public final class ObjectDecorTest extends AbstractDecorTest {

    /**
     * Public ctor.
     * @param object The object
     * @param text Expected text
     * @param flags Flags
     * @param width Width
     * @param precision Precission
     * @checkstyle ParameterNumber (3 lines)
     */
    public ObjectDecorTest(final Object object, final String text,
        final int flags, final int width, final int precision) {
        super(object, text, flags, width, precision);
    }

    /**
     * Params for this parametrized test.
     * @return Array of arrays of params for ctor
     * @todo #26 The ObjectDecor class is not implemented yet, that's why
     *  the test is not enabled at the moment. You should uncomment the
     *  lines below and make sure the test passes.
     */
    @Parameters
    public static Collection<Object[]> params() {
        return Arrays.asList(
            new Object[][] {
                // @checkstyle MethodBodyComments (2 lines)
                // { null, "NULL", 0, 0, 0 },
                // { new SecretDecor("x"), "{secret: \"x\"}", 0, 0, 0 }
            }
        );
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected Formattable decor() {
        return new ObjectDecor(this.object());
    }

}
