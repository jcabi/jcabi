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
package com.jcabi.manifests;

import com.jcabi.log.Logger;
import java.io.File;
import org.apache.commons.io.FileUtils;
import org.hamcrest.MatcherAssert;
import org.hamcrest.Matchers;
import org.junit.Test;

/**
 * Test case for {@link Manifests}.
 * @author Yegor Bugayenko (yegor@tpc2.com)
 * @version $Id$
 * @since 0.7
 */
public final class ManifestsTest {

    /**
     * Manifests can read a single attribute, which always exist in MANIFEST.MF.
     * @throws Exception If something goes wrong
     */
    @Test
    public void readsSingleExistingAttribute() throws Exception {
        MatcherAssert.assertThat(
            Manifests.read("Class-Path"),
            Matchers.notNullValue()
        );
    }

    /**
     * Manifests can read an injected attribute.
     * @throws Exception If something goes wrong
     */
    @Test
    public void readsInjectedAttribute() throws Exception {
        final String name = "Foo-Attribute";
        final String value = "some special value";
        MatcherAssert.assertThat(
            Manifests.exists(name),
            Matchers.equalTo(false)
        );
        Manifests.inject(name, value);
        MatcherAssert.assertThat(
            Manifests.exists(name),
            Matchers.equalTo(true)
        );
        MatcherAssert.assertThat(
            Manifests.read(name),
            Matchers.equalTo(value)
        );
    }

    /**
     * Manifests can throw an exception if attribute name is NULL.
     * @throws Exception If something goes wrong
     */
    @Test(expected = javax.validation.ConstraintViolationException.class)
    public void throwsExceptionWhenAttributeNameIsNull() throws Exception {
        Manifests.read(null);
    }

    /**
     * Manifests can throw an exception if trying to inject NULL.
     * @throws Exception If something goes wrong
     */
    @Test(expected = javax.validation.ConstraintViolationException.class)
    public void throwsExceptionWhenInjectingNull() throws Exception {
        Manifests.inject("attr-foo", null);
    }

    /**
     * Manifests can throw an exception if an attribute is empty.
     * @throws Exception If something goes wrong
     */
    @Test(expected = IllegalArgumentException.class)
    public void throwsExceptionWhenAttributeIsEmpty() throws Exception {
        Manifests.read("Jcabi-Test-Empty-Attribute");
    }

    /**
     * Manifests can throw an exception when attribute is absent.
     * @throws Exception If something goes wrong
     */
    @Test(expected = IllegalArgumentException.class)
    public void throwsExceptionIfAttributeIsMissed() throws Exception {
        Manifests.read("absent-property");
    }

    /**
     * Manifests can throw an exception loading file with empty attribute.
     * @throws Exception If something goes wrong
     */
    @Test(expected = java.io.IOException.class)
    public void throwsExceptionWhenNoAttributes() throws Exception {
        final File file = new File(
            Thread.currentThread().getContextClassLoader()
                .getResource("META-INF/MANIFEST_INVALID.MF").getFile()
        );
        Manifests.append(file);
    }

    /**
     * Manifests can make a snapshot and restore it back.
     * @throws Exception If something goes wrong
     */
    @Test
    public void makesSnapshotAndRestoresBack() throws Exception {
        final String name = "Test-Foo-Attribute";
        final byte[] snapshot = Manifests.snapshot();
        MatcherAssert.assertThat("is absent", !Manifests.exists(name));
        Manifests.inject(name, "some value to inject");
        MatcherAssert.assertThat("should be", Manifests.exists(name));
        Manifests.revert(snapshot);
        MatcherAssert.assertThat("reverted", !Manifests.exists(name));
    }

    /**
     * Manifests can append attributes from file.
     * @throws Exception If something goes wrong
     */
    @Test
    public void appendsAttributesFromFile() throws Exception {
        final String name = "Test-Attribute-From-File";
        final String value = "some text value of attribute";
        final File file = File.createTempFile("test-", ".MF");
        FileUtils.writeStringToFile(
            file,
            Logger.format("%s: %s\n", name, value)
        );
        Manifests.append(file);
        MatcherAssert.assertThat(
            "loaded from file",
            Manifests.exists(name) && Manifests.read(name).equals(value)
        );
        file.delete();
    }

}
