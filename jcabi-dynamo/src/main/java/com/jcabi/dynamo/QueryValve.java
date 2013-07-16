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

import com.amazonaws.services.dynamodbv2.AmazonDynamoDB;
import com.amazonaws.services.dynamodbv2.model.AttributeValue;
import com.amazonaws.services.dynamodbv2.model.Condition;
import com.amazonaws.services.dynamodbv2.model.QueryRequest;
import com.amazonaws.services.dynamodbv2.model.QueryResult;
import com.amazonaws.services.dynamodbv2.model.ReturnConsumedCapacity;
import com.google.common.collect.Iterables;
import com.jcabi.aspects.Immutable;
import com.jcabi.aspects.Loggable;
import com.jcabi.aspects.Tv;
import com.jcabi.log.Logger;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import lombok.EqualsAndHashCode;
import lombok.ToString;

/**
 * Query-based valve.
 *
 * @author Yegor Bugayenko (yegor@tpc2.com)
 * @version $Id$
 */
@Immutable
@ToString
@Loggable(Loggable.DEBUG)
@EqualsAndHashCode(of = { "limit", "forward" })
public final class QueryValve implements Valve {

    /**
     * Limit to use for every query.
     */
    private final transient int limit;

    /**
     * Forward/reverse order.
     */
    private final transient boolean forward;

    /**
     * Attributes to fetch.
     */
    private final transient String[] attributes;

    /**
     * Public ctor.
     */
    public QueryValve() {
        this(Tv.TWENTY, true, new ArrayList<String>(0));
    }

    /**
     * Public ctor.
     * @param lmt Limit
     * @param fwd Forward
     * @param attrs Names of attributes to pre-fetch
     */
    private QueryValve(final int lmt, final boolean fwd,
        final Iterable<String> attrs) {
        this.limit = lmt;
        this.forward = fwd;
        this.attributes = Iterables.toArray(attrs, String.class);
    }

    /**
     * {@inheritDoc}
     * @checkstyle ParameterNumber (5 lines)
     */
    @Override
    public Dosage fetch(final Credentials credentials, final String table,
        final Map<String, Condition> conditions,
        final Collection<String> keys) {
        final AmazonDynamoDB aws = credentials.aws();
        try {
            final Set<String> attrs = new HashSet<String>(
                Arrays.asList(this.attributes)
            );
            attrs.addAll(keys);
            final QueryRequest request = new QueryRequest()
                .withTableName(table)
                .withAttributesToGet(attrs)
                .withReturnConsumedCapacity(ReturnConsumedCapacity.TOTAL)
                .withKeyConditions(conditions)
                .withConsistentRead(true)
                .withScanIndexForward(QueryValve.this.forward)
                .withLimit(QueryValve.this.limit);
            final QueryResult result = aws.query(request);
            Logger.debug(
                this,
                "#items(): loaded %d item(s) from '%s' using %s, %.2f units",
                result.getCount(),
                table,
                conditions,
                result.getConsumedCapacity().getCapacityUnits()
            );
            return new QueryValve.NextDosage(credentials, request, result);
        } finally {
            aws.shutdown();
        }
    }

    /**
     * With given limit.
     * @param lmt Limit to use
     * @return New query valve
     */
    public QueryValve withLimit(final int lmt) {
        return new QueryValve(
            lmt, this.forward,
            Arrays.asList(this.attributes)
        );
    }

    /**
     * With scan index forward flag.
     * @param fwd Forward flag
     * @return New query valve
     */
    public QueryValve withScanIndexForward(final boolean fwd) {
        return new QueryValve(
            this.limit, fwd,
            Arrays.asList(this.attributes)
        );
    }

    /**
     * With this extra attribute to pre-fetch.
     * @param name Name of attribute to pre-load
     * @return New query valve
     */
    public QueryValve withAttributeToGet(final String name) {
        return new QueryValve(
            this.limit, this.forward,
            Iterables.concat(
                Arrays.asList(this.attributes),
                Arrays.asList(name)
            )
        );
    }

    /**
     * With these extra attributes to pre-fetch.
     * @param names Name of attributes to pre-load
     * @return New query valve
     */
    public QueryValve withAttributesToGet(final String... names) {
        return new QueryValve(
            this.limit, this.forward,
            Iterables.concat(
                Arrays.asList(this.attributes),
                Arrays.asList(names)
            )
        );
    }

    /**
     * Next dosage.
     */
    @ToString
    @Loggable(Loggable.DEBUG)
    @EqualsAndHashCode(of = { "credentials", "request", "result" })
    private final class NextDosage implements Dosage {
        /**
         * AWS client.
         */
        private final transient Credentials credentials;
        /**
         * Query request.
         */
        private final transient QueryRequest request;
        /**
         * Query request.
         */
        private final transient QueryResult result;
        /**
         * Public ctor.
         * @param creds Credentials
         * @param rqst Query request
         * @param rslt Query result
         */
        protected NextDosage(final Credentials creds,
            final QueryRequest rqst, final QueryResult rslt) {
            this.credentials = creds;
            this.request = rqst;
            this.result = rslt;
        }
        /**
         * {@inheritDoc}
         */
        @Override
        public List<Map<String, AttributeValue>> items() {
            return this.result.getItems();
        }
        /**
         * {@inheritDoc}
         */
        @Override
        public boolean hasNext() {
            return this.result.getLastEvaluatedKey() != null;
        }
        /**
         * {@inheritDoc}
         */
        @Override
        public Dosage next() {
            if (!this.hasNext()) {
                throw new IllegalStateException();
            }
            final AmazonDynamoDB aws = this.credentials.aws();
            try {
                final QueryRequest rqst =
                    this.request.withExclusiveStartKey(
                        this.result.getLastEvaluatedKey()
                    );
                final QueryResult rslt = aws.query(rqst);
                Logger.debug(
                    this,
                    "#next(): loaded %d item(s) from '%s' using %s, %.2f units",
                    rslt.getCount(),
                    rqst.getTableName(),
                    rqst.getKeyConditions(),
                    rslt.getConsumedCapacity().getCapacityUnits()
                );
                return new QueryValve.NextDosage(this.credentials, rqst, rslt);
            } finally {
                aws.shutdown();
            }
        }
    }

}
