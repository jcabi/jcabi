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

import org.slf4j.LoggerFactory;

/**
 * Universal logger, and adapter between API
 * and a third-party logging facility.
 *
 * <p>Instead of relying
 * on some logging engine you can use this class, which transforms all
 * messages to SLF4J. This approach gives you a perfect decoupling of business
 * logic and logging mechanism. All methods in the class are called
 * statically, without the necessity to instantiate the class.
 *
 * <p>Use it like this in any class, and in any package:
 *
 * <pre>
 * package com.example.XXX;
 * import com.jcabi.log.Logger;
 * public class MyClass {
 *   public void foo(Integer num) {
 *     Logger.info(this, "foo(%d) just called", num);
 *   }
 * }
 * </pre>
 *
 * <p>Or statically (pay attention to {@code MyClass.class}):
 *
 * <pre>
 * public class MyClass {
 *   public static void foo(Integer num) {
 *     Logger.info(MyClass.class, "foo(%d) just called", num);
 *   }
 * }
 * </pre>
 *
 * <p>Exact binding between SLF4J and logging facility has to be
 * specified in {@code pom.xml} of your project.
 *
 * <p>For performance reasons in most cases before sending a
 * {@code TRACE} or {@code DEBUG} log message you should check whether this
 * logging level is enabled in the project, e.g.:
 *
 * <pre>
 * //...
 * if (Logger.isTraceEnabled(this)) {
 *   Logger.trace(this, "#foo() called");
 * }
 * //...
 * </pre>
 *
 * @author Yegor Bugayenko (yegor@jcabi.com)
 * @version $Id$
 * @since 0.1
 */
public final class Logger {

    /**
     * Private ctor, to avoid class instantiation.
     *
     * <p>This is utility class and you can't instantiate it directly.
     */
    private Logger() {
        // intentionally empty
    }

    /**
     * Format one string.
     * @param fmt The format
     * @param args List of arbitrary arguments
     * @return Formatted string
     */
    public static String format(final String fmt, final Object... args) {
        String result;
        if (args.length == 0) {
            result = fmt;
        } else {
            final PreFormatter pre = new PreFormatter(fmt, args);
            result = String.format(pre.getFormat(), pre.getArguments());
        }
        return result;
    }

    /**
     * Protocol one message, with {@code TRACE} priority level.
     * @param source The source of the logging operation
     * @param msg The text message to be logged, with meta-tags
     * @param args List of arguments
     */
    public static void trace(
        final Object source,
        final String msg, final Object... args
    ) {
        if (Logger.isTraceEnabled(source)) {
            Logger.logger(source).trace(Logger.format(msg, args));
        }
    }

    /**
     * Protocol one message, with {@code DEBUG} priority level.
     * @param source The source of the logging operation
     * @param msg The text message to be logged, with meta-tags
     * @param args List of arguments
     */
    public static void debug(
        final Object source,
        final String msg, final Object... args
    ) {
        if (Logger.isDebugEnabled(source)) {
            Logger.logger(source).debug(Logger.format(msg, args));
        }
    }

    /**
     * Protocol one message, with {@code INFO} priority level.
     * @param source The source of the logging operation
     * @param msg The text message to be logged, with meta-tags
     * @param args List of arguments
     */
    public static void info(
        final Object source,
        final String msg, final Object... args
    ) {
        if (Logger.isInfoEnabled(source)) {
            Logger.logger(source).info(Logger.format(msg, args));
        }
    }

    /**
     * Protocol one message, with {@code WARN} priority level.
     * @param source The source of the logging operation
     * @param msg The text message to be logged, with meta-tags
     * @param args List of arguments
     */
    public static void warn(
        final Object source,
        final String msg, final Object... args
    ) {
        if (Logger.isWarnEnabled(source)) {
            Logger.logger(source).warn(Logger.format(msg, args));
        }
    }

    /**
     * Protocol one message, with {@code ERROR} priority level.
     * @param source The source of the logging operation
     * @param msg The text message to be logged, with meta-tags
     * @param args List of arguments
     */
    public static void error(final Object source,
        final String msg, final Object... args) {
        Logger.logger(source).error(Logger.format(msg, args));
    }

    /**
     * Validates whether {@code TRACE} priority level is enabled for
     * this particular logger.
     * @param source The source of the logging operation
     * @return Is it enabled?
     */
    public static boolean isTraceEnabled(final Object source) {
        return Logger.logger(source).isTraceEnabled();
    }

    /**
     * Validates whether {@code DEBUG} priority level is enabled for
     * this particular logger.
     * @param source The source of the logging operation
     * @return Is it enabled?
     */
    public static boolean isDebugEnabled(final Object source) {
        return Logger.logger(source).isDebugEnabled();
    }

    /**
     * Validates whether {@code INFO} priority level is enabled for
     * this particular logger.
     * @param source The source of the logging operation
     * @return Is it enabled?
     * @since 1.0.5
     */
    public static boolean isInfoEnabled(final Object source) {
        return Logger.logger(source).isInfoEnabled();
    }

    /**
     * Validates whether {@code INFO} priority level is enabled for
     * this particular logger.
     * @param source The source of the logging operation
     * @return Is it enabled?
     * @since 1.0.5
     */
    public static boolean isWarnEnabled(final Object source) {
        return Logger.logger(source).isWarnEnabled();
    }

    /**
     * Get the instance of the logger for this particular caller.
     * @param source Source of the logging operation
     * @return The instance of {@link Logger} class
     */
    private static org.slf4j.Logger logger(final Object source) {
        org.slf4j.Logger logger;
        if (source instanceof Class) {
            logger = LoggerFactory.getLogger((Class) source);
        } else {
            logger = LoggerFactory.getLogger(source.getClass());
        }
        return logger;
    }

}
