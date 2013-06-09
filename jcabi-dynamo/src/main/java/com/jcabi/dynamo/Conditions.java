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
import com.amazonaws.services.dynamodbv2.model.ComparisonOperator;
import com.amazonaws.services.dynamodbv2.model.Condition;
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
 * DynamoDB query conditions.
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
public final class Conditions implements Map<String, Condition> {

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
    public Conditions() {
        this(new HashMap<String, Condition>(0));
    }

    /**
     * Private ctor.
     * @param map Map of them
     */
    public Conditions(@NotNull final Map<String, Condition> map) {
        this.pairs = new Object[map.size()][];
        int pos = 0;
        for (Map.Entry<String, Condition> entry : map.entrySet()) {
            this.pairs[pos] = new Object[] {entry.getKey(), entry.getValue()};
            ++pos;
        }
    }

    /**
     * Equal to static condition builder (factory method).
     * @param value The value to equal to
     * @return The condition just created
     */
    public static Condition equalTo(@NotNull final Object value) {
        return new Condition()
            .withAttributeValueList(new AttributeValue(value.toString()))
            .withComparisonOperator(ComparisonOperator.EQ);
    }

    /**
     * With this attribute.
     * @param name Attribute name
     * @param value The value
     * @return Attributes
     */
    public Conditions with(@NotNull final String name,
        @NotNull final Condition value) {
        final ConcurrentMap<String, Condition> map =
            new ConcurrentHashMap<String, Condition>(
                this.pairs.length + 1
            );
        map.putAll(this);
        map.put(name, value);
        return new Conditions(map);
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
        return this.values().contains(Condition.class.cast(value));
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public Condition get(final Object key) {
        Condition value = null;
        for (Map.Entry<String, Condition> entry : this.entrySet()) {
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
    public Collection<Condition> values() {
        final Collection<Condition> values =
            new ArrayList<Condition>(this.pairs.length);
        for (Object[] pair : this.pairs) {
            values.add(Condition.class.cast(pair[1]));
        }
        return values;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public Set<Map.Entry<String, Condition>> entrySet() {
        final Set<Map.Entry<String, Condition>> entries =
            new HashSet<Map.Entry<String, Condition>>(this.pairs.length);
        for (Object[] pair : this.pairs) {
            entries.add(
                new HashMap.SimpleImmutableEntry<String, Condition>(
                    pair[0].toString(),
                    Condition.class.cast(pair[1])
                )
            );
        }
        return entries;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public Condition put(final String key, final Condition value) {
        throw new UnsupportedOperationException();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public Condition remove(final Object key) {
        throw new UnsupportedOperationException();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void putAll(
        final Map<? extends String, ? extends Condition> map) {
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
