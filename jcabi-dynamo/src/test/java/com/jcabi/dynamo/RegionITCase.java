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

import com.amazonaws.services.dynamodbv2.model.AttributeDefinition;
import com.amazonaws.services.dynamodbv2.model.AttributeValue;
import com.amazonaws.services.dynamodbv2.model.CreateTableRequest;
import com.amazonaws.services.dynamodbv2.model.KeySchemaElement;
import com.amazonaws.services.dynamodbv2.model.KeyType;
import com.amazonaws.services.dynamodbv2.model.ProvisionedThroughput;
import com.amazonaws.services.dynamodbv2.model.ScalarAttributeType;
import com.jcabi.aspects.Tv;
import java.util.Iterator;
import org.apache.commons.lang3.RandomStringUtils;
import org.hamcrest.MatcherAssert;
import org.hamcrest.Matchers;
import org.junit.AfterClass;
import org.junit.Assume;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;

/**
 * Integration case for {@link Region}.
 * @author Yegor Bugayenko (yegor@tpc2.com)
 * @version $Id$
 * @checkstyle ClassDataAbstractionCoupling (500 lines)
 */
public final class RegionITCase {

    /**
     * AWS key.
     */
    private static final String KEY =
        System.getProperty("failsafe.dynamo.key");

    /**
     * AWS secret.
     */
    private static final String SECRET =
        System.getProperty("failsafe.dynamo.secret");

    /**
     * AWS table name.
     */
    private static final String TABLE =
        System.getProperty("failsafe.dynamo.table");

    /**
     * Dynamo table hash key.
     */
    private static final String HASH = "hash-key";

    /**
     * Dynamo table range key.
     */
    private static final String RANGE = "range-key";

    /**
     * Region.
     */
    private static Region region;

    /**
     * Table mocker.
     */
    private static TableMocker table;

    /**
     * Before the test.
     * @throws Exception If fails
     */
    @Before
    public void skip() throws Exception {
        Assume.assumeThat(RegionITCase.KEY, Matchers.notNullValue());
    }

    /**
     * Before the test.
     * @throws Exception If fails
     */
    @BeforeClass
    public static void before() throws Exception {
        if (RegionITCase.KEY == null) {
            return;
        }
        RegionITCase.region = new Region.Simple(
            new Credentials.Simple(
                RegionITCase.KEY, RegionITCase.SECRET
            )
        );
        RegionITCase.table = new TableMocker(
            RegionITCase.region,
            new CreateTableRequest()
                .withTableName(RegionITCase.TABLE)
                .withProvisionedThroughput(
                    new ProvisionedThroughput()
                        .withReadCapacityUnits(1L)
                        .withWriteCapacityUnits(1L)
                )
                .withAttributeDefinitions(
                    new AttributeDefinition()
                        .withAttributeName(RegionITCase.HASH)
                        .withAttributeType(ScalarAttributeType.S),
                    new AttributeDefinition()
                        .withAttributeName(RegionITCase.RANGE)
                        .withAttributeType(ScalarAttributeType.S)
                )
                .withKeySchema(
                    new KeySchemaElement()
                        .withAttributeName(RegionITCase.HASH)
                        .withKeyType(KeyType.HASH),
                    new KeySchemaElement()
                        .withAttributeName(RegionITCase.RANGE)
                        .withKeyType(KeyType.RANGE)
                )
        );
        RegionITCase.table.create();
    }

    /**
     * After the test.
     * @throws Exception If fails
     */
    @AfterClass
    public static void after() throws Exception {
        if (RegionITCase.KEY == null) {
            return;
        }
        RegionITCase.table.drop();
    }

    /**
     * Region.Simple can work with AWS.
     * @throws Exception If some problem inside
     */
    @Test
    @SuppressWarnings("PMD.AvoidInstantiatingObjectsInLoops")
    public void worksWithAmazon() throws Exception {
        final Table tbl = RegionITCase.region.table(RegionITCase.TABLE);
        final String attr = RandomStringUtils.randomAlphabetic(Tv.EIGHT);
        final String value = RandomStringUtils.randomAlphanumeric(Tv.TEN);
        final String hash = RandomStringUtils.randomAlphanumeric(Tv.TEN);
        for (int idx = 0; idx < Tv.FIVE; ++idx) {
            tbl.put(
                new Attributes()
                    .with(RegionITCase.HASH, hash)
                    .with(RegionITCase.RANGE, idx)
                    .with(attr, value)
            );
        }
        MatcherAssert.assertThat(
            tbl.frame()
                .where(RegionITCase.HASH, Conditions.equalTo(hash))
                .through(new QueryValve().withLimit(1)),
            Matchers.hasSize(Tv.FIVE)
        );
        final Frame frame = tbl.frame()
            .where(attr, Conditions.equalTo(value))
            .through(
                new ScanValve()
                    .withLimit(1)
                    .withAttributeToGet(attr)
            );
        MatcherAssert.assertThat(frame, Matchers.hasSize(Tv.FIVE));
        final Iterator<Item> items = frame.iterator();
        final Item item = items.next();
        final String range = item.get(RegionITCase.RANGE).getS();
        MatcherAssert.assertThat(
            item.get(attr).getS(),
            Matchers.equalTo(value)
        );
        item.put(attr, new AttributeValue("empty"));
        MatcherAssert.assertThat(
            tbl.frame()
                .where(RegionITCase.HASH, hash)
                .where(RegionITCase.RANGE, range)
                .through(new ScanValve())
                .iterator().next()
                .get(attr).getS(),
            Matchers.not(Matchers.equalTo(value))
        );
        items.remove();
    }

}
