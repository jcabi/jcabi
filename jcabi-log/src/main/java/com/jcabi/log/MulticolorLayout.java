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

import org.apache.log4j.PatternLayout;
import org.apache.log4j.spi.LoggingEvent;

/**
 * Multi-color layout for LOG4J.
 *
 * <p>Use it in your LOG4J configuration:
 *
 * <pre>
 * log4j.rootLogger=INFO, CONSOLE
 * log4j.appender.CONSOLE=org.apache.log4j.ConsoleAppender
 * log4j.appender.CONSOLE.layout=com.jcabi.log.MulticolorLayout
 * log4j.appender.CONSOLE.layout.ConversionPattern=[%color{%-5p}] %c: %m%n
 * </pre>
 *
 * <p>The part of the message wrapped with {@code %color&#123;...&#125;}
 * will change its color according to the logging level of the event. Without
 * this highlighting the behavior of the layout is identical to
 * {@link PatternLayout}. You can use {@code %color-red&#123;...&#125;} if you
 * want to use specifically red color for the wrapped piece of text. Supported
 * colors are: {@code red}, {@code blue}, {@code yellow}, {@code cyan},
 * {@code black}, and {@code white}.
 *
 * Besides that you can specify any ANSI color you like with
 * {@code %color-&lt;attr&gt;-&lt;bg&gt;-&lt;fg&fg;&#123;...&#125;}, where
 * {@code &lt;attr&gt;} is a binary mask of attributes,
 * {@code &lt;bg&gt;} is a background color, and
 * {@code &lt;fg&gt;} is a foreground color. Read more about
 * <a href="http://en.wikipedia.org/wiki/ANSI_escape_code">ANSI escape code</a>.
 *
 * @author Yegor Bugayenko (yegor@jcabi.com)
 * @version $Id$
 * @since 0.1.2
 * @see <a href="http://en.wikipedia.org/wiki/ANSI_escape_code">ANSI escape code</a>
 */
public final class MulticolorLayout extends PatternLayout {

    /**
     * {@inheritDoc}
     */
    @Override
    public String format(final LoggingEvent event) {
        return super.format(event);
    }

}
