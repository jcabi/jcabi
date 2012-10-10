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

import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import org.apache.log4j.Level;
import org.apache.log4j.PatternLayout;
import org.apache.log4j.spi.LoggingEvent;

/**
 * Multi-color layout for LOG4J.
 *
 * <p>Use it in your LOG4J configuration:
 *
 * <pre> log4j.rootLogger=INFO, CONSOLE
 * log4j.appender.CONSOLE=org.apache.log4j.ConsoleAppender
 * log4j.appender.CONSOLE.layout=com.jcabi.log.MulticolorLayout
 * log4j.appender.CONSOLE.layout.ConversionPattern=[%color{%-5p}] %c: %m%n</pre>
 *
 * <p>The part of the message wrapped with {@code %color{...}}
 * will change its color according to the logging level of the event. Without
 * this highlighting the behavior of the layout is identical to
 * {@link PatternLayout}. You can use {@code %color-red{...}} if you
 * want to use specifically red color for the wrapped piece of text. Supported
 * colors are: {@code red}, {@code blue}, {@code yellow}, {@code cyan},
 * {@code black}, and {@code white}.
 *
 * Besides that you can specify any ANSI color you like with
 * {@code %color-<attr>;<bg>;<fg>{...}}, where
 * {@code <attr>} is a binary mask of attributes,
 * {@code <bg>} is a background color, and
 * {@code <fg>} is a foreground color. Read more about
 * <a href="http://en.wikipedia.org/wiki/ANSI_escape_code">ANSI escape code</a>.
 *
 * <p>The class is immutable and thread-safe.
 *
 * @author Yegor Bugayenko (yegor@jcabi.com)
 * @version $Id$
 * @since 0.1.10
 * @see <a href="http://en.wikipedia.org/wiki/ANSI_escape_code">ANSI escape code</a>
 * @see <a href="http://logging.apache.org/log4j/1.2/apidocs/org/apache/log4j/PatternLayout.html">PatternLayout from LOG4J</a>
 * @see <a href="http://www.jcabi.com/jcabi-log/multicolor.html">How to use with Maven</a>
 */
@SuppressWarnings("PMD.NonStaticInitializer")
public final class MulticolorLayout extends PatternLayout {

    /**
     * Control sequence indicator.
     */
    private static final String CSI = "\u001b[";

    /**
     * Colors with names.
     */
    private static final ConcurrentMap<String, String> COLORS =
        new ConcurrentHashMap<String, String>() {
            private static final long serialVersionUID = 0x7526EF78EEDFE465L;
            {
                this.put("black", "30");
                this.put("blue", "34");
                this.put("cyan", "36");
                this.put("green", "32");
                this.put("magenta", "35");
                this.put("red", "31");
                this.put("yellow", "33");
                this.put("white", "37");
            }
        };

    /**
     * Colors of levels.
     */
    private static final ConcurrentMap<Level, String> LEVELS =
        new ConcurrentHashMap<Level, String>() {
            private static final long serialVersionUID = 0x7526FF78EEDFC465L;
            {
                this.put(Level.TRACE, "2;33");
                this.put(Level.DEBUG, "2;37");
                this.put(Level.INFO, "0;37");
                this.put(Level.WARN, "0;33");
                this.put(Level.ERROR, "0;31");
                this.put(Level.FATAL, "0;35");
            }
        };

    /**
     * Regular expression for all matches.
     */
    private static final Pattern METAS = Pattern.compile(
        "%color(?:-([a-z]+|[0-9]{1,3};[0-9]{1,3};[0-9]{1,3}))?\\{(.*?)\\}"
    );

    /**
     * {@inheritDoc}
     */
    @Override
    public void setConversionPattern(final String pattern) {
        final Matcher matcher = MulticolorLayout.METAS.matcher(pattern);
        final StringBuffer buf = new StringBuffer();
        while (matcher.find()) {
            matcher.appendReplacement(buf, "");
            buf.append(MulticolorLayout.CSI)
                .append(this.ansi(matcher.group(1)))
                .append('m')
                .append(matcher.group(2))
                .append(MulticolorLayout.CSI)
                .append('m');
        }
        matcher.appendTail(buf);
        super.setConversionPattern(buf.toString());
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public String format(final LoggingEvent event) {
        return super.format(event).replace(
            String.format("%s?m", MulticolorLayout.CSI),
            String.format(
                "%s%sm",
                MulticolorLayout.CSI,
                MulticolorLayout.LEVELS.get(event.getLevel())
            )
        );
    }

    /**
     * Convert our text to ANSI color.
     * @param meta Meta text
     * @return ANSI color
     */
    private String ansi(final String meta) {
        String ansi;
        if (meta == null) {
            ansi = "?";
        } else if (meta.matches("[a-z]+")) {
            ansi = MulticolorLayout.COLORS.get(meta);
            if (ansi == null) {
                throw new IllegalArgumentException(
                    String.format("unknown color '%s'", meta)
                );
            }
        } else {
            ansi = meta;
        }
        return ansi;
    }

}
