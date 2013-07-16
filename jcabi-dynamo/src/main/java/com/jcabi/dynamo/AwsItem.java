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
import com.amazonaws.services.dynamodbv2.model.GetItemRequest;
import com.amazonaws.services.dynamodbv2.model.GetItemResult;
import com.amazonaws.services.dynamodbv2.model.PutItemRequest;
import com.amazonaws.services.dynamodbv2.model.PutItemResult;
import com.amazonaws.services.dynamodbv2.model.ReturnConsumedCapacity;
import com.amazonaws.services.dynamodbv2.model.ReturnValue;
import com.jcabi.aspects.Immutable;
import com.jcabi.aspects.Loggable;
import com.jcabi.log.Logger;
import java.util.Arrays;
import java.util.NoSuchElementException;
import javax.validation.constraints.NotNull;
import lombok.EqualsAndHashCode;
import lombok.ToString;

/**
 * Single item/row in a DynamoDB table.
 *
 * @author Yegor Bugayenko (yegor@tpc2.com)
 * @version $Id$
 */
@Immutable
@Loggable(Loggable.DEBUG)
@ToString
@EqualsAndHashCode(of = { "credentials", "frm", "name", "keys" })
final class AwsItem implements Item {

    /**
     * AWS credentials.
     */
    private final transient Credentials credentials;

    /**
     * Frame.
     */
    private final transient AwsFrame frm;

    /**
     * Table name.
     */
    private final transient String name;

    /**
     * Primary keys and their values.
     */
    private final transient Attributes keys;

    /**
     * Public ctor.
     * @param creds Credentials
     * @param frame Frame
     * @param table Table name
     * @param attrs Attributes of primary keys (with values)
     * @checkstyle ParameterNumber (5 lines)
     */
    protected AwsItem(final Credentials creds, final AwsFrame frame,
        final String table, final Attributes attrs) {
        this.credentials = creds;
        this.frm = frame;
        this.name = table;
        this.keys = attrs;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public boolean has(@NotNull final String attr) {
        boolean has = this.keys.containsKey(attr);
        if (!has) {
            final AmazonDynamoDB aws = this.credentials.aws();
            try {
                final GetItemRequest request = new GetItemRequest();
                request.setTableName(this.name);
                request.setAttributesToGet(Arrays.asList(attr));
                request.setKey(this.keys);
                request.setReturnConsumedCapacity(ReturnConsumedCapacity.TOTAL);
                request.setConsistentRead(true);
                final GetItemResult result = aws.getItem(request);
                has = result.getItem().get(attr) != null;
                Logger.debug(
                    this,
                    "#has('%s'): %B from DynamoDB, %.2f units",
                    attr,
                    has,
                    result.getConsumedCapacity().getCapacityUnits()
                );
            } finally {
                aws.shutdown();
            }
        }
        return has;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    @NotNull
    public AttributeValue get(@NotNull final String attr) {
        AttributeValue value = this.keys.get(attr);
        if (value == null) {
            final AmazonDynamoDB aws = this.credentials.aws();
            try {
                final GetItemRequest request = new GetItemRequest();
                request.setTableName(this.name);
                request.setAttributesToGet(Arrays.asList(attr));
                request.setKey(this.keys);
                request.setReturnConsumedCapacity(ReturnConsumedCapacity.TOTAL);
                request.setConsistentRead(true);
                final GetItemResult result = aws.getItem(request);
                value = result.getItem().get(attr);
                if (value == null) {
                    throw new NoSuchElementException(
                        String.format("attribute %s not found", attr)
                    );
                }
                Logger.debug(
                    this,
                    "#get('%s'): loaded '%[text]s' from DynamoDB, %.2f units",
                    attr,
                    value,
                    result.getConsumedCapacity().getCapacityUnits()
                );
            } finally {
                aws.shutdown();
            }
        }
        return value;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void put(@NotNull final String attr,
        @NotNull final AttributeValue value) {
        this.put(new Attributes().with(attr, value));
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void put(@NotNull final Attributes attrs) {
        final AmazonDynamoDB aws = this.credentials.aws();
        try {
            final PutItemRequest request = new PutItemRequest();
            request.setTableName(this.name);
            request.setExpected(this.keys.asKeys());
            request.setItem(new Attributes(this.keys).with(attrs));
            request.setReturnConsumedCapacity(ReturnConsumedCapacity.TOTAL);
            request.setReturnValues(ReturnValue.NONE);
            final PutItemResult result = aws.putItem(request);
            Logger.debug(
                this,
                "#put('%s'): saved item to DynamoDB, %.2f units",
                attrs,
                result.getConsumedCapacity().getCapacityUnits()
            );
        } finally {
            aws.shutdown();
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public Frame frame() {
        return this.frm;
    }

}
