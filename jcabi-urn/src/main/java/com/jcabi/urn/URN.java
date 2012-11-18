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

import java.io.Serializable;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URLDecoder;
import java.util.Map;
import java.util.TreeMap;
import org.apache.commons.lang.CharEncoding;
import org.apache.commons.lang.StringUtils;

/**
 * Uniform Resource Name (URN).
 *
 * @author Yegor Bugayenko (yegor@tpc2.com)
 * @version $Id$
 * @since 0.6
 */
@SuppressWarnings({ "PMD.TooManyMethods", "PMD.UseConcurrentHashMap" })
public final class URN implements Comparable<URN>, Serializable {

    /**
     * Serialization marker.
     */
    private static final long serialVersionUID = 0x4243AFCD9812ABDCL;

    /**
     * Marker of an empty URN.
     */
    private static final String EMPTY = "void";

    /**
     * The prefix.
     */
    private static final String PREFIX = "urn";

    /**
     * The separator.
     */
    private static final String SEP = ":";

    /**
     * Validating regular expr.
     */
    private static final String REGEX =
        // @checkstyle LineLength (1 line)
        "^urn:[a-z]{1,31}(:([\\-a-zA-Z0-9/]|%[0-9a-fA-F]{2})*)+(\\?\\w+(=([\\-a-zA-Z0-9/]|%[0-9a-fA-F]{2})*)?(&\\w+(=([\\-a-zA-Z0-9/]|%[0-9a-fA-F]{2})*)?)*)?\\*?$";

    /**
     * The URI.
     */
    @SuppressWarnings("PMD.BeanMembersShouldSerialize")
    private final URI uri;

    /**
     * Public ctor, for JAXB mostly.
     */
    public URN() {
        this(URN.EMPTY, "");
    }

    /**
     * Public ctor.
     * @param text The text of the URN
     * @throws URISyntaxException If syntax is not correct
     */
    public URN(final String text) throws URISyntaxException {
        if (text == null) {
            throw new IllegalArgumentException("Text can't be NULL");
        }
        if (!text.matches(URN.REGEX)) {
            throw new URISyntaxException(text, "Invalid format of URN");
        }
        this.uri = new URI(text);
        this.validate();
    }

    /**
     * Public ctor.
     * @param nid The namespace ID
     * @param nss The namespace specific string
     */
    public URN(final String nid, final String nss) {
        if (nid == null) {
            throw new IllegalArgumentException("NID can't be NULL");
        }
        if (nss == null) {
            throw new IllegalArgumentException("NSS can't be NULL");
        }
        this.uri = URI.create(
            String.format(
                "%s%s%s%2$s%s",
                URN.PREFIX,
                URN.SEP,
                nid,
                URN.encode(nss)
            )
        );
        try {
            this.validate();
        } catch (URISyntaxException ex) {
            throw new IllegalArgumentException(ex);
        }
    }

    /**
     * Static ctor.
     * @param text The text of the URN
     * @return The URN
     */
    public static URN create(final String text) {
        try {
            return new URN(text);
        } catch (URISyntaxException ex) {
            throw new IllegalArgumentException(ex);
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public String toString() {
        return this.uri.toString();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public int compareTo(final URN urn) {
        return this.uri.compareTo(urn.uri);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public boolean equals(final Object obj) {
        boolean equals = false;
        if (obj == this) {
            equals = true;
        } else if (obj instanceof URN) {
            equals = this.uri.equals(URN.class.cast(obj).uri);
        } else if (obj instanceof String) {
            equals = this.uri.toString().equals(String.class.cast(obj));
        } else if (obj instanceof URI) {
            equals = this.uri.equals(URI.class.cast(obj));
        }
        return equals;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public int hashCode() {
        return this.uri.hashCode();
    }

    /**
     * Is it URN?
     * @param text The text to validate
     * @return Yes of no
     */
    public static boolean isValid(final String text) {
        boolean valid = true;
        try {
            new URN(text);
        } catch (URISyntaxException ex) {
            valid = false;
        }
        return valid;
    }

    /**
     * Does it match the pattern?
     * @param pattern The pattern to match
     * @return Yes of no
     */
    public boolean matches(final URN pattern) {
        boolean matches = false;
        if (this.equals(pattern)) {
            matches = true;
        } else if (pattern.toString().endsWith("*")) {
            final String body = pattern.toString().substring(
                0,  pattern.toString().length() - 1
            );
            matches = this.uri.toString().startsWith(body);
        }
        return matches;
    }

    /**
     * Does it match the pattern?
     * @param pattern The pattern to match
     * @return Yes of no
     */
    public boolean matches(final String pattern) {
        return this.matches(URN.create(pattern));
    }

    /**
     * Is it empty?
     * @return Yes of no
     */
    public boolean isEmpty() {
        return URN.EMPTY.equals(this.nid());
    }

    /**
     * Convert it to URI.
     * @return The URI
     */
    public URI toURI() {
        return URI.create(this.uri.toString());
    }

    /**
     * Get namespace ID.
     * @return Namespace ID
     */
    public String nid() {
        return this.segment(1);
    }

    /**
     * Get namespace specific string.
     * @return Namespace specific string
     */
    public String nss() {
        try {
            return URLDecoder.decode(this.segment(2), CharEncoding.UTF_8);
        } catch (java.io.UnsupportedEncodingException ex) {
            throw new IllegalStateException(ex);
        }
    }

    /**
     * Get all params.
     * @return The params
     */
    public Map<String, String> params() {
        return URN.demap(this.toString());
    }

    /**
     * Get query param by name.
     * @param name Name of parameter
     * @return The value of it
     */
    public String param(final String name) {
        final Map<String, String> params = this.params();
        if (!params.containsKey(name)) {
            throw new IllegalArgumentException(
                String.format(
                    "Param '%s' not found in '%s', among %s",
                    name,
                    this,
                    params.keySet()
                )
            );
        }
        return params.get(name);
    }

    /**
     * Add query param and return new URN.
     * @param name Name of parameter
     * @param value The value of parameter
     * @return New URN
     */
    public URN param(final String name, final Object value) {
        final Map<String, String> params = this.params();
        params.put(name, value.toString());
        return URN.create(
            String.format(
                "%s%s",
                StringUtils.split(this.toString(), '?')[0],
                URN.enmap(params)
            )
        );
    }

    /**
     * Get just body of URN, without params.
     * @return Clean version of it
     */
    public URN pure() {
        String urn = this.toString();
        if (this.hasParams()) {
            // @checkstyle MultipleStringLiterals (1 line)
            urn = urn.substring(0, urn.indexOf('?'));
        }
        return URN.create(urn);
    }

    /**
     * Whether this URN has params?
     * @return Has them?
     */
    public boolean hasParams() {
        // @checkstyle MultipleStringLiterals (1 line)
        return this.toString().contains("?");
    }

    /**
     * Get segment by position.
     * @param pos Its position
     * @return The segment
     */
    private String segment(final int pos) {
        return StringUtils.splitPreserveAllTokens(
            this.uri.toString(),
            URN.SEP,
            // @checkstyle MagicNumber (1 line)
            3
        )[pos];
    }

    /**
     * Validate URN.
     * @throws URISyntaxException If it's not valid
     */
    private void validate() throws URISyntaxException {
        if (this.isEmpty() && !this.nss().isEmpty()) {
            throw new URISyntaxException(
                this.toString(),
                "Empty URN can't have NSS"
            );
        }
        if (!this.nid().matches("^[a-z]{1,31}$")) {
            throw new IllegalArgumentException(
                String.format(
                    "NID '%s' can contain up to 31 low case letters",
                    this.nid()
                )
            );
        }
    }

    /**
     * Decode query part of the URN into Map.
     * @param urn The URN to demap
     * @return The map of values
     */
    private static Map<String, String> demap(final String urn) {
        final Map<String, String> map = new TreeMap<String, String>();
        final String[] sectors = StringUtils.split(urn, '?');
        if (sectors.length == 2) {
            final String[] parts = StringUtils.split(sectors[1], '&');
            for (String part : parts) {
                final String[] pair = StringUtils.split(part, '=');
                String value;
                if (pair.length == 2) {
                    try {
                        value = URLDecoder.decode(pair[1], CharEncoding.UTF_8);
                    } catch (java.io.UnsupportedEncodingException ex) {
                        throw new IllegalStateException(ex);
                    }
                } else {
                    value = "";
                }
                map.put(pair[0], value);
            }
        }
        return map;
    }

    /**
     * Encode map of params into query part of URN.
     * @param params Map of params to convert to query suffix
     * @return The suffix of URN, starting with "?"
     */
    private static String enmap(final Map<String, String> params) {
        final StringBuilder query = new StringBuilder();
        if (!params.isEmpty()) {
            query.append("?");
            boolean first = true;
            for (Map.Entry<String, String> param : params.entrySet()) {
                if (!first) {
                    query.append("&");
                }
                query.append(param.getKey());
                if (!param.getValue().isEmpty()) {
                    query.append("=").append(URN.encode(param.getValue()));
                }
                first = false;
            }
        }
        return query.toString();
    }

    /**
     * Perform proper URL encoding with the text.
     * @param text The text to encode
     * @return The encoded text
     */
    private static String encode(final String text) {
        final StringBuilder encoded = new StringBuilder();
        byte[] bytes;
        try {
            bytes = text.getBytes(CharEncoding.UTF_8);
        } catch (java.io.UnsupportedEncodingException ex) {
            throw new IllegalStateException(ex);
        }
        for (byte chr : bytes) {
            if (URN.allowed(chr)) {
                encoded.append((char) chr);
            } else {
                encoded.append("%").append(String.format("%X", chr));
            }
        }
        return encoded.toString();
    }

    /**
     * This char is allowed in URN's NSS part?
     * @param chr The character
     * @return It is allowed?
     */
    private static boolean allowed(final byte chr) {
        // @checkstyle BooleanExpressionComplexity (4 lines)
        return (chr >= 'A' && chr <= 'Z')
            || (chr >= '0' && chr <= '9')
            || (chr >= 'a' && chr <= 'z')
            || (chr == '/') || (chr == '-');
    }

}
