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

import java.util.Formattable;
import java.util.FormattableFlags;
import java.util.Formatter;

/**
 * Decorator of a secret text.
 * @author Marina Kosenko (marina.kosenko@gmail.com)
 * @author Yegor Bugayenko (yegor@tpc2.com)
 * @version $Id$
 * @since 0.1
 */
final class SecretDecor implements Formattable {

    /**
     * The secret to work with.
     */
    private final transient String secret;

    /**
     * Public ctor.
     * @param scrt The secret
     */
    public SecretDecor(final Object scrt) {
        if (scrt == null) {
            this.secret = (String) scrt;
        } else {
            this.secret = scrt.toString();
        }
    }

    /**
     * {@inheritDoc}
     * @checkstyle ParameterNumber (4 lines)
     */
    @Override
    public void formatTo(final Formatter formatter, final int flags,
        final int width, final int precision) {
        if (this.secret == null) {
            formatter.format("NULL");
        } else {
            final StringBuilder fmt = new StringBuilder();
            fmt.append('%');
            if ((flags & FormattableFlags.LEFT_JUSTIFY) != 0) {
                fmt.append('-');
            }
            if (width != 0) {
                fmt.append(width);
            }
            if ((flags & FormattableFlags.UPPERCASE) == 0) {
                fmt.append('s');
            } else {
                fmt.append('S');
            }
            formatter.format(
                fmt.toString(),
                SecretDecor.scramble(this.secret)
            );
        }
    }

    /**
     * Scramble it and make unreadable.
     * @param text The text to scramble
     * @return The result
     */
    private static String scramble(final String text) {
        final StringBuilder out = new StringBuilder();
        if (text.isEmpty()) {
            out.append('?');
        } else {
            out.append(text.charAt(0));
        }
        out.append("***");
        if (text.isEmpty()) {
            out.append('?');
        } else {
            out.append(text.charAt(text.length() - 1));
        }
        return out.toString();
    }

}
