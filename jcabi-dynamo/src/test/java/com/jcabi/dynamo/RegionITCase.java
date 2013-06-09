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
import com.amazonaws.services.dynamodbv2.model.AttributeDefinition;
import com.amazonaws.services.dynamodbv2.model.AttributeValue;
import com.amazonaws.services.dynamodbv2.model.CreateTableRequest;
import com.amazonaws.services.dynamodbv2.model.DeleteTableRequest;
import com.amazonaws.services.dynamodbv2.model.DescribeTableRequest;
import com.amazonaws.services.dynamodbv2.model.DescribeTableResult;
import com.amazonaws.services.dynamodbv2.model.KeySchemaElement;
import com.amazonaws.services.dynamodbv2.model.KeyType;
import com.amazonaws.services.dynamodbv2.model.ProvisionedThroughput;
import com.amazonaws.services.dynamodbv2.model.ScalarAttributeType;
import com.jcabi.aspects.Tv;
import com.jcabi.log.Logger;
import java.util.Iterator;
import java.util.concurrent.TimeUnit;
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
    private static final String HASH = "id";

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
        final AmazonDynamoDB aws = RegionITCase.aws();
        aws.createTable(
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
                        .withAttributeType(ScalarAttributeType.S)
                )
                .withKeySchema(
                    new KeySchemaElement()
                        .withAttributeName(RegionITCase.HASH)
                        .withKeyType(KeyType.HASH)
                )
        );
        final DescribeTableRequest request = new DescribeTableRequest()
            .withTableName(RegionITCase.TABLE);
        while (true) {
            final DescribeTableResult result = aws.describeTable(request);
            if ("ACTIVE".equals(result.getTable().getTableStatus())) {
                Logger.info(RegionITCase.class, "Dynamo table is ready");
                break;
            }
            TimeUnit.SECONDS.sleep(Tv.FIVE);
            Logger.info(
                RegionITCase.class,
                "waiting for Dynamo: %s",
                result.getTable().getTableStatus()
            );
        }
    }

    /**
     * After the test.
     */
    @AfterClass
    public static void after() {
        if (RegionITCase.KEY == null) {
            return;
        }
        final AmazonDynamoDB aws = RegionITCase.aws();
        aws.deleteTable(
            new DeleteTableRequest()
                .withTableName(RegionITCase.TABLE)
        );
        Logger.info(RegionITCase.class, "Dynamo table deleted");
    }

    /**
     * Region.Simple can work with AWS.
     * @throws Exception If some problem inside
     */
    @Test
    public void worksWithAmazon() throws Exception {
        final Credentials creds = new Credentials.Simple(
            RegionITCase.KEY, RegionITCase.SECRET
        );
        final Region region = new Region.Simple(creds);
        final Table table = region.table(RegionITCase.TABLE);
        final String attr = RandomStringUtils.randomAlphabetic(Tv.EIGHT);
        final String value = RandomStringUtils.randomAlphanumeric(Tv.TEN);
        table.put(
            new Attributes()
                .with(RegionITCase.HASH, "first-hash")
                .with(attr, value)
        );
        final Frame frame = table.frame().where(
            attr, Conditions.equalTo(value)
        );
        MatcherAssert.assertThat(frame.size(), Matchers.equalTo(1));
        final Iterator<Item> items = frame.iterator();
        final Item item = items.next();
        MatcherAssert.assertThat(
            item.get(attr).getS(),
            Matchers.equalTo(value)
        );
        item.put(attr, new AttributeValue("empty"));
        MatcherAssert.assertThat(
            item.get(attr).getS(),
            Matchers.not(Matchers.equalTo(value))
        );
        items.remove();
    }

    /**
     * Make AWS client.
     * @return The client
     */
    private static AmazonDynamoDB aws() {
        final Credentials creds = new Credentials.Simple(
            RegionITCase.KEY, RegionITCase.SECRET
        );
        return creds.aws();
    }

}
