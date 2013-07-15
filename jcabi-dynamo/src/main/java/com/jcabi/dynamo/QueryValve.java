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
import com.jcabi.aspects.Immutable;
import com.jcabi.aspects.Loggable;
import com.jcabi.aspects.Tv;
import com.jcabi.log.Logger;
import java.util.Collection;
import java.util.List;
import java.util.Map;
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
@EqualsAndHashCode
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
     * Public ctor.
     */
    public QueryValve() {
        this(Tv.TWENTY, true);
    }

    /**
     * Public ctor.
     * @param lmt Limit
     * @param fwd Forward
     */
    public QueryValve(final int lmt, final boolean fwd) {
        this.limit = lmt;
        this.forward = fwd;
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
            final QueryRequest request = new QueryRequest()
                .withTableName(table)
                .withAttributesToGet(keys)
                .withReturnConsumedCapacity(ReturnConsumedCapacity.TOTAL)
                .withKeyConditions(conditions)
                .withConsistentRead(true)
                .withScanIndexForward(QueryValve.this.forward)
                .withLimit(QueryValve.this.limit);
            final QueryResult result = aws.query(request);
            Logger.debug(
                this,
                "#items(): loaded %d item(s) from '%s', %.2f units",
                result.getCount(),
                table,
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
        return new QueryValve(lmt, this.forward);
    }

    /**
     * With scan index forward flag.
     * @param fwd Forward flag
     * @return New query valve
     */
    public QueryValve withScanIndexForward(final boolean fwd) {
        return new QueryValve(this.limit, fwd);
    }

    /**
     * Next dosage.
     */
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
        public Dosage next() {
            final Dosage dosage;
            if (this.result.getLastEvaluatedKey() == null) {
                dosage = new Dosage.Empty();
            } else {
                final AmazonDynamoDB aws = this.credentials.aws();
                try {
                    final QueryRequest rqst =
                        this.request.withExclusiveStartKey(
                            this.result.getLastEvaluatedKey()
                        );
                    final QueryResult rslt = aws.query(rqst);
                    Logger.debug(
                        this,
                        "#next(): loaded %d item(s) from '%s', %.2f units",
                        rslt.getCount(),
                        rqst.getTableName(),
                        rslt.getConsumedCapacity().getCapacityUnits()
                    );
                    dosage = new QueryValve.NextDosage(
                        this.credentials, rqst, rslt
                    );
                } finally {
                    aws.shutdown();
                }
            }
            return dosage;
        }
    }

}
