/**
 * Copyright (c) 2012-2013, JCabi.com
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
package com.jcabi.dynamo;

import com.amazonaws.services.dynamodbv2.model.AttributeValue;
import com.amazonaws.services.dynamodbv2.model.ExpectedAttributeValue;
import com.jcabi.aspects.Immutable;
import com.jcabi.aspects.Loggable;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import javax.validation.constraints.NotNull;
import lombok.EqualsAndHashCode;
import lombok.ToString;

/**
 * DynamoDB item attributes.
 *
 * @author Yegor Bugayenko (yegor@tpc2.com)
 * @version $Id$
 */
@Immutable
@Loggable(Loggable.DEBUG)
@ToString
@EqualsAndHashCode
@SuppressWarnings({
    "PMD.TooManyMethods", "PMD.AvoidInstantiatingObjectsInLoops"
})
public final class Attributes implements Map<String, AttributeValue> {

    /**
     * Serialization marker.
     */
    private static final long serialVersionUID = 0x3456998922348767L;

    /**
     * Pairs.
     */
    private final transient Object[][] pairs;

    /**
     * Private ctor.
     */
    public Attributes() {
        this(new HashMap<String, AttributeValue>(0));
    }

    /**
     * Private ctor.
     * @param map Map of them
     */
    public Attributes(@NotNull final Map<String, AttributeValue> map) {
        this.pairs = new Object[map.size()][];
        int pos = 0;
        for (Map.Entry<String, AttributeValue> entry : map.entrySet()) {
            this.pairs[pos] = new Object[] {entry.getKey(), entry.getValue()};
            ++pos;
        }
    }

    /**
     * With this attribute.
     * @param name Attribute name
     * @param value The value
     * @return Attributes
     */
    public Attributes with(@NotNull final String name,
        @NotNull final AttributeValue value) {
        final ConcurrentMap<String, AttributeValue> map =
            new ConcurrentHashMap<String, AttributeValue>(
                this.pairs.length + 1
            );
        map.putAll(this);
        map.put(name, value);
        return new Attributes(map);
    }

    /**
     * Convert them to a map of expected values.
     * @return Expected values
     */
    public Map<String, ExpectedAttributeValue> asKeys() {
        final ConcurrentMap<String, ExpectedAttributeValue> map =
            new ConcurrentHashMap<String, ExpectedAttributeValue>(
                this.pairs.length
            );
        for (Object[] pair : this.pairs) {
            map.put(
                pair[0].toString(),
                new ExpectedAttributeValue(AttributeValue.class.cast(pair[1]))
            );
        }
        return map;
    }

    /**
     * With this attribute.
     * @param name Attribute name
     * @param value The value
     * @return Attributes
     */
    public Attributes with(@NotNull final String name,
        @NotNull final Object value) {
        return this.with(name, new AttributeValue(value.toString()));
    }

    /**
     * Filter out all keys except provided ones.
     * @param keys Keys to leave in the map
     * @return Attributes
     */
    public Attributes only(@NotNull final Collection<String> keys) {
        final ConcurrentMap<String, AttributeValue> map =
            new ConcurrentHashMap<String, AttributeValue>(this.pairs.length);
        for (Map.Entry<String, AttributeValue> entry : this.entrySet()) {
            if (keys.contains(entry.getKey())) {
                map.put(entry.getKey(), entry.getValue());
            }
        }
        return new Attributes(map);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public int size() {
        return this.pairs.length;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public boolean isEmpty() {
        return this.pairs.length == 0;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public boolean containsKey(final Object key) {
        return this.keySet().contains(key.toString());
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public boolean containsValue(final Object value) {
        return this.values().contains(AttributeValue.class.cast(value));
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public AttributeValue get(final Object key) {
        AttributeValue value = null;
        for (Map.Entry<String, AttributeValue> entry : this.entrySet()) {
            if (entry.getKey().equals(key)) {
                value = entry.getValue();
                break;
            }
        }
        return value;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public Set<String> keySet() {
        final Set<String> keys = new HashSet<String>(this.pairs.length);
        for (Object[] pair : this.pairs) {
            keys.add(pair[0].toString());
        }
        return keys;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public Collection<AttributeValue> values() {
        final Collection<AttributeValue> values =
            new ArrayList<AttributeValue>(this.pairs.length);
        for (Object[] pair : this.pairs) {
            values.add(AttributeValue.class.cast(pair[1]));
        }
        return values;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public Set<Entry<String, AttributeValue>> entrySet() {
        final Set<Entry<String, AttributeValue>> entries =
            new HashSet<Entry<String, AttributeValue>>(this.pairs.length);
        for (Object[] pair : this.pairs) {
            entries.add(
                new HashMap.SimpleImmutableEntry<String, AttributeValue>(
                    pair[0].toString(),
                    AttributeValue.class.cast(pair[1])
                )
            );
        }
        return entries;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public AttributeValue put(final String key, final AttributeValue value) {
        throw new UnsupportedOperationException();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public AttributeValue remove(final Object key) {
        throw new UnsupportedOperationException();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void putAll(
        final Map<? extends String, ? extends AttributeValue> map) {
        throw new UnsupportedOperationException();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void clear() {
        throw new UnsupportedOperationException();
    }

}
