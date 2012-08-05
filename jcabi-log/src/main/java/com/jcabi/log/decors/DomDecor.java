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

import com.jcabi.log.DecorException;
import java.io.StringWriter;
import java.util.Formattable;
import java.util.Formatter;
import javax.xml.transform.OutputKeys;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.stream.StreamResult;
import org.w3c.dom.Node;

/**
 * Decorates XML Document.
 *
 * @author Yegor Bugayenko (yegor@jcabi.com)
 * @version $Id$
 * @since 0.1
 */
public final class DomDecor implements Formattable {

    /**
     * DOM transformer factory, DOM.
     */
    private static final TransformerFactory FACTORY =
        TransformerFactory.newInstance();

    /**
     * The document.
     */
    private final transient Node node;

    /**
     * Public ctor.
     * @param doc The document
     * @throws DecorException If some problem with it
     */
    public DomDecor(final Object doc) throws DecorException {
        if (doc != null && !(doc instanceof Node)) {
            throw new DecorException(
                String.format(
                    "Instance of org.w3c.dom.Node required, while %s provided",
                    doc.getClass().getName()
                )
            );
        }
        this.node = (Node) doc;
    }

    /**
     * {@inheritDoc}
     * @checkstyle ParameterNumber (4 lines)
     */
    @Override
    public void formatTo(final Formatter formatter, final int flags,
        final int width, final int precision) {
        final StringWriter writer = new StringWriter();
        if (this.node == null) {
            writer.write("NULL");
        } else {
            try {
                final Transformer trans = DomDecor.FACTORY.newTransformer();
                trans.setOutputProperty(OutputKeys.INDENT, "yes");
                trans.transform(
                    new DOMSource(this.node),
                    new StreamResult(writer)
                );
            } catch (javax.xml.transform.TransformerConfigurationException ex) {
                throw new IllegalStateException(ex);
            } catch (javax.xml.transform.TransformerException ex) {
                throw new IllegalStateException(ex);
            }
        }
        formatter.format("%s", writer.toString());
    }

}
