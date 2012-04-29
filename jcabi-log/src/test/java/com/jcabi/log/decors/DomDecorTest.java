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

import java.util.Formattable;
import java.util.Formatter;
import javax.xml.parsers.DocumentBuilderFactory;
import org.junit.Test;
import org.mockito.Mockito;
import org.w3c.dom.Document;

/**
 * Test case for {@link DomDecor}.
 * @author Yegor Bugayenko (yegor@jcabi.com)
 * @version $Id: DomDecorTest.java 324 2012-02-26 22:31:04Z guard $
 */
public final class DomDecorTest {

    /**
     * DocumentDecor can transform Document to text.
     * @throws Exception If some problem
     */
    @Test
    public void convertsDocumentToText() throws Exception {
        final Document doc = DocumentBuilderFactory.newInstance()
            .newDocumentBuilder().newDocument();
        doc.appendChild(doc.createElement("root"));
        final Formattable decor = new DomDecor(doc);
        final Appendable dest = Mockito.mock(Appendable.class);
        final Formatter fmt = new Formatter(dest);
        decor.formatTo(fmt, 0, 0, 0);
        Mockito.verify(dest).append(
            // @checkstyle LineLength (1 line)
            "<?xml version=\"1.0\" encoding=\"UTF-8\" standalone=\"no\"?>\n<root/>\n"
        );
    }

    /**
     * DocumentDecor can handle NULL properly.
     * @throws Exception If some problem
     */
    @Test
    public void convertsNullToText() throws Exception {
        final Formattable decor = new DomDecor(null);
        final Appendable dest = Mockito.mock(Appendable.class);
        final Formatter fmt = new Formatter(dest);
        decor.formatTo(fmt, 0, 0, 0);
        Mockito.verify(dest).append("NULL");
    }

}
