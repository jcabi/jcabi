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
package com.jcabi.urn;

import java.net.URI;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import org.apache.commons.lang.SerializationUtils;
import org.hamcrest.MatcherAssert;
import org.hamcrest.Matchers;
import org.junit.Test;

/**
 * Uniform Resource Name (URN), tests.
 *
 * @author Yegor Bugayenko (yegor@tpc2.com)
 * @version $Id$
 * @since 0.6
 */
@SuppressWarnings("PMD.TooManyMethods")
public final class URNTest {

    /**
     * URN can be instantiated from plain text.
     * @throws Exception If there is some problem inside
     */
    @Test
    public void instantiatesFromText() throws Exception {
        final URN urn = new URN("urn:jcabi:jeff%20lebowski%2540");
        MatcherAssert.assertThat(urn.nid(), Matchers.equalTo("jcabi"));
        MatcherAssert.assertThat(
            urn.nss(),
            Matchers.equalTo("jeff lebowski%40")
        );
    }

    /**
     * URN can encode NSS properly.
     * @throws Exception If there is some problem inside
     */
    @Test
    public void encodesNssAsRequiredByUrlSyntax() throws Exception {
        final URN urn = new URN("test", "walter sobchak!");
        MatcherAssert.assertThat(
            urn.toString(),
            Matchers.equalTo("urn:test:walter%20sobchak%21")
        );
    }

    /**
     * URN can throw exception when text is NULL.
     * @throws Exception If there is some problem inside
     */
    @Test(expected = javax.validation.ConstraintViolationException.class)
    public void throwsExceptionWhenTextIsNull() throws Exception {
        new URN(null);
    }

    /**
     * URN can be instantiated from components.
     * @throws Exception If there is some problem inside
     */
    @Test
    public void instantiatesFromComponents() throws Exception {
        final String nid = "foo";
        final String nss = "\u8416 & \u8415 *&^%$#@!-~`\"'";
        final URN urn = new URN(nid, nss);
        MatcherAssert.assertThat(urn.nid(), Matchers.equalTo(nid));
        MatcherAssert.assertThat(urn.nss(), Matchers.equalTo(nss));
        MatcherAssert.assertThat(urn.toURI(), Matchers.instanceOf(URI.class));
    }

    /**
     * URN can throw exception when NID is NULL.
     * @throws Exception If there is some problem inside
     */
    @Test(expected = javax.validation.ConstraintViolationException.class)
    public void throwsExceptionWhenNidIsNull() throws Exception {
        new URN(null, "some-test-nss");
    }

    /**
     * URN can throw exception when NSS is NULL.
     * @throws Exception If there is some problem inside
     */
    @Test(expected = javax.validation.ConstraintViolationException.class)
    public void throwsExceptionWhenNssIsNull() throws Exception {
        new URN("namespace1", null);
    }

    /**
     * URN can be tested for equivalence of another URN.
     * @throws Exception If there is some problem inside
     */
    @Test
    public void comparesForEquivalence() throws Exception {
        final String text = "urn:foo:some-other-specific-string";
        final URN first = new URN(text);
        final URN second = new URN(text);
        MatcherAssert.assertThat(first, Matchers.equalTo(second));
    }

    /**
     * URN can be tested for equivalence with another URI.
     * @throws Exception If there is some problem inside
     */
    @Test
    public void comparesForEquivalenceWithUri() throws Exception {
        final String text = "urn:foo:somespecificstring";
        final URN first = new URN(text);
        final URI second = new URI(text);
        MatcherAssert.assertThat(first.equals(second), Matchers.is(true));
    }

    /**
     * URN can be tested for equivalence with string.
     * @throws Exception If there is some problem inside
     */
    @Test
    public void comparesForEquivalenceWithString() throws Exception {
        final String text = "urn:foo:sometextastext";
        final URN first = new URN(text);
        MatcherAssert.assertThat(first.equals(text), Matchers.is(true));
    }

    /**
     * URN can be converted to string.
     * @throws Exception If there is some problem inside
     */
    @Test
    public void convertsToString() throws Exception {
        final String text = "urn:foo:textofurn";
        final URN urn = new URN(text);
        MatcherAssert.assertThat(urn.toString(), Matchers.equalTo(text));
    }

    /**
     * URN can catch incorrect syntax.
     * @throws Exception If there is some problem inside
     */
    @Test(expected = java.net.URISyntaxException.class)
    public void catchesIncorrectURNSyntax() throws Exception {
        new URN("some incorrect name");
    }

    /**
     * URN can pass correct syntax.
     * @throws Exception If there is some problem inside
     */
    @Test
    public void passesCorrectURNSyntax() throws Exception {
        final String[] texts = new String[] {
            "urn:foo:some%20text%20with%20spaces",
            "urn:a:",
            "urn:a:?alpha=50",
            "urn:a:?boom",
            "urn:a:test?123",
            "urn:a:test?1a2b3c",
            "urn:a:test?1A2B3C",
            "urn:a:?alpha=abccde%20%45%4Fme",
            "urn:woquo:ns:pa/procure/BalanceRecord?name=*",
            "urn:a:?alpha=50&beta=u%20worksfine",
            "urn:verylongnamespaceid:",
            "urn:a:?alpha=50*",
            "urn:a:b/c/d",
        };
        for (String text : texts) {
            final URN urn = URN.create(text);
            MatcherAssert.assertThat(
                URN.create(urn.toString()),
                Matchers.equalTo(urn)
            );
            MatcherAssert.assertThat("is valid", URN.isValid(urn.toString()));
        }
    }

    /**
     * URN can throw exception for incorrect syntax.
     * @throws Exception If there is some problem inside
     */
    @Test
    public void throwsExceptionForIncorrectURNSyntax() throws Exception {
        final String[] texts = new String[] {
            "abc",
            "",
            "urn::",
            "urn:incorrect namespace name with spaces:test",
            "urn:abc+foo:test-me",
            "urn:test:?abc?",
            "urn:test:?abc=incorrect*value",
            "urn:test:?abc=invalid-symbols:^%$#&@*()!-in-argument-value",
            "urn:incorrect%20namespace:",
            "urn:verylongnameofanamespaceverylongnameofanamespace:",
            "urn:test:spaces are not allowed here",
            "urn:test:unicode-has-to-be-encoded:\u8514",
        };
        for (String text : texts) {
            try {
                URN.create(text);
                MatcherAssert.assertThat(text, Matchers.nullValue());
            } catch (IllegalArgumentException ex) {
                assert ex != null;
            }
        }
    }

    /**
     * URN can be "empty".
     * @throws Exception If there is some problem inside
     */
    @Test
    public void emptyURNIsAFirstClassCitizen() throws Exception {
        final URN urn = new URN();
        MatcherAssert.assertThat(urn.isEmpty(), Matchers.equalTo(true));
    }

    /**
     * URN can be "empty" only in one form.
     * @throws Exception If there is some problem inside
     */
    @Test(expected = IllegalArgumentException.class)
    public void emptyURNHasOnlyOneVariant() throws Exception {
        new URN("void", "it-is-impossible-to-have-any-NSS-here");
    }

    /**
     * URN can be "empty" only in one form, with from-text ctor.
     * @throws Exception If there is some problem inside
     */
    @Test(expected = java.net.URISyntaxException.class)
    public void emptyURNHasOnlyOneVariantWithTextCtor() throws Exception {
        new URN("urn:void:it-is-impossible-to-have-any-NSS-here");
    }

    /**
     * URN can match a pattern.
     * @throws Exception If there is some problem inside
     */
    @Test
    public void matchesPatternWithAnotherURN() throws Exception {
        MatcherAssert.assertThat(
            "matches",
            new URN("urn:test:file").matches("urn:test:*")
        );
    }

    /**
     * URN can add and retrieve params.
     * @throws Exception If there is some problem inside
     */
    @Test
    public void addAndRetrievesParamsByName() throws Exception {
        final String name = "crap";
        final String value = "@!$#^\u0433iu**76\u0945";
        final URN urn = new URN("urn:test:x?bb")
            .param("bar", "\u8514 value?")
            .param(name, value);
        MatcherAssert.assertThat(
            urn.toString(),
            Matchers.containsString("bar=%E8%94%94%20value%3F")
        );
        MatcherAssert.assertThat(urn.param("bb"), Matchers.equalTo(""));
        MatcherAssert.assertThat(urn.param(name), Matchers.equalTo(value));
    }

    /**
     * URN can fetch a pure part (without params) from itself.
     * @throws Exception If there is some problem inside
     */
    @Test
    public void fetchesBodyWithoutParams() throws Exception {
        MatcherAssert.assertThat(
            new URN("urn:test:something?a=9&b=4").pure(),
            Matchers.equalTo(new URN("urn:test:something"))
        );
    }

    /**
     * URN can be serialized.
     * @throws Exception If there is some problem inside
     */
    @Test
    public void serializesToBytes() throws Exception {
        final URN urn = new URN("urn:test:some-data-to-serialize");
        final byte[] bytes = SerializationUtils.serialize(urn);
        MatcherAssert.assertThat(
            ((URN) SerializationUtils.deserialize(bytes)).toString(),
            Matchers.equalTo(urn.toString())
        );
    }

    /**
     * URN can be persistent in params ordering.
     * @throws Exception If there is some problem inside
     */
    @Test
    public void persistsOrderingOfParams() throws Exception {
        final List<String> params = Arrays.asList(
            new String[] {"ft", "sec", "9", "123", "a1b2c3", "A", "B", "C"}
        );
        URN first = new URN("urn:test:x");
        URN second = first;
        for (String param : params) {
            first = first.param(param, "");
        }
        Collections.shuffle(params);
        for (String param : params) {
            second = second.param(param, "");
        }
        MatcherAssert.assertThat(first, Matchers.equalTo(second));
    }

    /**
     * URN can be mocked.
     * @throws Exception If there is some problem inside
     */
    @Test
    public void mocksUrnWithAMocker() throws Exception {
        MatcherAssert.assertThat(
            new URNMocker().mock(),
            Matchers.not(Matchers.equalTo(new URNMocker().mock()))
        );
    }

}
