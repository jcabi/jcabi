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

import org.apache.maven.plugin.logging.Log;
import org.slf4j.helpers.FormattingTuple;
import org.slf4j.helpers.MarkerIgnoringBase;
import org.slf4j.helpers.MessageFormatter;

/**
 * Implementation of {@link org.slf4j.Logger} transforming SLF4J messages
 * to Maven log messages.
 *
 * <p>The class has too many methods, but
 * we can't do anything with this since the parent class requires
 * us to implement them all.
 *
 * <p>The class is thread-safe.
 *
 * @author Yegor Bugayenko (yegor@jcabi.com)
 * @version $Id$
 * @see <a href="http://www.slf4j.org/faq.html#slf4j_compatible">SLF4J FAQ</a>
 */
@SuppressWarnings("PMD.TooManyMethods")
final class Slf4jAdapter extends MarkerIgnoringBase {

    /**
     * Serialization ID.
     */
    public static final long serialVersionUID = 0x12C0976798AB5439L;

    /**
     * The log to use.
     */
    private transient Log mlog;

    /**
     * Set Maven log.
     *
     * <p>Class variable is used as a synchronization lock, mostly because
     * only one instance of this class may exist.
     *
     * @param log The log to set
     */
    public void setMavenLog(final Log log) {
        synchronized (Slf4jAdapter.class) {
            this.mlog = log;
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public String getName() {
        return this.getClass().getName();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public boolean isTraceEnabled() {
        return false;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void trace(final String msg) {
        this.log().debug(msg);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void trace(final String format, final Object arg) {
        this.log().debug(this.format(format, arg));
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void trace(final String format, final Object first,
        final Object second) {
        this.log().debug(this.format(format, first, second));
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void trace(final String format, final Object[] array) {
        this.log().debug(this.format(format, array));
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void trace(final String msg, final Throwable thr) {
        this.log().debug(msg, thr);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public boolean isDebugEnabled() {
        return this.log().isDebugEnabled();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void debug(final String msg) {
        this.log().debug(msg);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void debug(final String format, final Object arg) {
        this.log().debug(this.format(format, arg));
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void debug(final String format, final Object first,
        final Object second) {
        this.log().debug(this.format(format, first, second));
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void debug(final String format, final Object[] array) {
        this.log().debug(this.format(format, array));
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void debug(final String msg, final Throwable thr) {
        this.log().debug(msg, thr);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public boolean isInfoEnabled() {
        return true;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void info(final String msg) {
        this.log().info(msg);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void info(final String format, final Object arg) {
        this.log().info(this.format(format, arg));
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void info(final String format, final Object first,
        final Object second) {
        this.log().info(this.format(format, first, second));
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void info(final String format, final Object[] array) {
        this.log().info(this.format(format, array));
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void info(final String msg, final Throwable thr) {
        this.log().info(msg, thr);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public boolean isWarnEnabled() {
        return true;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void warn(final String msg) {
        this.log().warn(msg);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void warn(final String format, final Object arg) {
        this.log().warn(this.format(format, arg));
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void warn(final String format, final Object[] array) {
        this.log().warn(this.format(format, array));
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void warn(final String format, final Object first,
        final Object second) {
        this.log().warn(this.format(format, first, second));
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void warn(final String msg, final Throwable thr) {
        this.log().warn(msg, thr);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public boolean isErrorEnabled() {
        return true;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void error(final String msg) {
        this.log().error(msg);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void error(final String format, final Object arg) {
        this.log().error(this.format(format, arg));
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void error(final String format, final Object first,
        final Object second) {
        this.log().error(this.format(format, first, second));
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void error(final String format, final Object[] array) {
        this.log().error(this.format(format, array));
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void error(final String msg, final Throwable thr) {
        this.log().error(msg, thr);
    }

    /**
     * Get Maven Log.
     * @return The log from Maven plugin
     */
    private Log log() {
        if (this.mlog == null) {
            throw new IllegalStateException(
                "initialize StaticLoggerBinder with #setMavenLog() first"
            );
        }
        return this.mlog;
    }

    /**
     * Format with one object.
     * @param format Format to use
     * @param arg One argument
     * @return The message
     */
    private String format(final String format, final Object arg) {
        final FormattingTuple tuple =
            MessageFormatter.format(format, arg);
        return tuple.getMessage();
    }

    /**
     * Format with two objects.
     * @param format Format to use
     * @param first First argument
     * @param second Second argument
     * @return The message
     */
    private String format(final String format, final Object first,
        final Object second) {
        final FormattingTuple tuple =
            MessageFormatter.format(format, first, second);
        return tuple.getMessage();
    }

    /**
     * Format with array.
     * @param format Format to use
     * @param array List of arguments
     * @return The message
     */
    private String format(final String format, final Object[] array) {
        final FormattingTuple tuple =
            MessageFormatter.format(format, array);
        return tuple.getMessage();
    }

}
