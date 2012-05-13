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
package com.jcabi.velocity;

import java.io.PrintWriter;
import java.io.StringWriter;
import org.apache.velocity.Template;
import org.apache.velocity.VelocityContext;
import org.apache.velocity.app.VelocityEngine;
import org.apache.velocity.runtime.RuntimeConstants;
import org.apache.velocity.runtime.resource.loader.ClasspathResourceLoader;

/**
 * Velocity page, builder/wrapper around Apache Velocity template.
 *
 * <p>The template should be in classpath:
 *
 * <pre>
 * String text = new VelocityPage("com/foo/my-template.vm")
 *   .set("name", "John Doe")
 *   .toString();
 * </pre>
 *
 * <p>At the moment all logging is forwarded to LOG4J. In Velocity 2.0 there
 * will be an adapter for SLF4J and we'll use it: {@code Slf4jLogChute}.
 *
 * <p>The class is mutable and thread-safe.
 *
 * @author Yegor Bugayenko (yegor@jcabi.com)
 * @version $Id$
 * @link <a href="http://velocity.apache.org/engine/releases/velocity-1.7/user-guide.html">Velocity User Guide</a>
 * @link <a href="http://velocity.apache.org/engine/releases/velocity-1.7/developer-guide.html">Velocity Developer Guide</a>
 * @link <a href="http://velocity.apache.org/engine/devel/apidocs/org/apache/velocity/slf4j/Slf4jLogChute.html">Slf4jLogChute</a>
 */
public final class VelocityPage {

    /**
     * The engine to use.
     */
    private static final VelocityEngine ENGINE = VelocityPage.init();

    /**
     * Name of resource.
     */
    private final transient String name;

    /**
     * The context.
     */
    private final transient VelocityContext context = new VelocityContext();

    /**
     * Public ctor.
     * @param res Name of resource with template
     */
    public VelocityPage(final String res) {
        this.name = res;
    }

    /**
     * Set the name to the value specified.
     * @param prop Name of the property to set
     * @param value The value to use
     * @return This object
     */
    public VelocityPage set(final String prop, final Object value) {
        synchronized (this.context) {
            this.context.put(prop, value);
        }
        return this;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public String toString() {
        final Template template = VelocityPage.ENGINE.getTemplate(this.name);
        final StringWriter writer = new StringWriter();
        template.merge(this.context, new PrintWriter(writer));
        return writer.toString();
    }

    /**
     * Create and initialize Velocity engine.
     * @return The engine to use
     */
    private static VelocityEngine init() {
        final VelocityEngine engine = new VelocityEngine();
        engine.setProperty("resource.loader", "cp");
        engine.setProperty(
            "cp.resource.loader.class",
            ClasspathResourceLoader.class.getName()
        );
        engine.setProperty(
            RuntimeConstants.RUNTIME_LOG_LOGSYSTEM_CLASS,
            "org.apache.velocity.runtime.log.Log4JLogChute"
        );
        engine.setProperty(
            "runtime.log.logsystem.log4j.logger",
            "org.apache.velocity"
        );
        engine.init();
        return engine;
    }

}
