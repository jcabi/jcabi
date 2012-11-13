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

import java.util.Formattable;
import java.util.FormattableFlags;
import java.util.Formatter;
import org.hamcrest.MatcherAssert;
import org.hamcrest.Matchers;
import org.junit.Test;
import org.mockito.Mockito;

/**
 * Abstract test case for all decors in the package.
 * @author Marina Kosenko (marina.kosenko@gmail.com)
 * @author Yegor Bugayenko (yegor@tpc2.com)
 * @version $Id$
 */
public abstract class AbstractDecorTest {

    /**
     * Object/system under test.
     */
    private final transient Object sut;

    /**
     * The text to expect as an output.
     */
    private final transient String text;

    /**
     * Formatting flas.
     */
    private final transient int flags;

    /**
     * Formatting width.
     */
    private final transient int width;

    /**
     * Formatting precision.
     */
    private final transient int precision;

    /**
     * Public ctor.
     * @param obj The object
     * @param txt Expected text
     * @param flgs Flags
     * @param wdt Width
     * @param prcs Precission
     * @checkstyle ParameterNumber (3 lines)
     */
    public AbstractDecorTest(final Object obj, final String txt,
        final int flgs, final int wdt, final int prcs) {
        this.sut = obj;
        this.text = txt;
        this.flags = flgs;
        this.width = wdt;
        this.precision = prcs;
    }

    /**
     * AbstractDecor can convert object to text.
     * @throws Exception If some problem inside
     */
    @Test
    public final void convertsDifferentFormats() throws Exception {
        final Formattable decor = this.decor();
        final Appendable dest = Mockito.mock(Appendable.class);
        final Formatter fmt = new Formatter(dest);
        decor.formatTo(fmt, this.flags, this.width, this.precision);
        Mockito.verify(dest).append(this.text);
    }

    /**
     * AbstractDecor can convert object to text, via Logger.
     * @throws Exception If some problem inside
     */
    @Test
    public final void convertsDifferentFormatsViaLogger() throws Exception {
        final StringBuilder format = new StringBuilder();
        format.append('%');
        if ((this.flags & FormattableFlags.LEFT_JUSTIFY) == FormattableFlags
            .LEFT_JUSTIFY) {
            format.append('-');
        }
        if (this.width > 0) {
            format.append(Integer.toString(this.width));
        }
        if (this.precision > 0) {
            format.append('.').append(Integer.toString(this.precision));
        }
        if ((this.flags & FormattableFlags.UPPERCASE) == FormattableFlags
            .UPPERCASE) {
            format.append('S');
        } else {
            format.append('s');
        }
        MatcherAssert.assertThat(
            Logger.format(format.toString(), this.decor()),
            Matchers.equalTo(this.text)
        );
    }

    /**
     * Get decor with the object.
     * @return The decor to test
     * @throws Exception If some problem
     */
    protected abstract Formattable decor() throws Exception;

    /**
     * Get object under test.
     * @return The object
     */
    protected final Object object() {
        return this.sut;
    }

}
