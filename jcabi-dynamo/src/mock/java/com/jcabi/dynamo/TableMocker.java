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
import com.amazonaws.services.dynamodbv2.model.CreateTableRequest;
import com.amazonaws.services.dynamodbv2.model.DeleteTableRequest;
import com.amazonaws.services.dynamodbv2.model.DescribeTableRequest;
import com.amazonaws.services.dynamodbv2.model.DescribeTableResult;
import com.amazonaws.services.dynamodbv2.model.ListTablesRequest;
import com.amazonaws.services.dynamodbv2.model.ListTablesResult;
import com.jcabi.aspects.Tv;
import com.jcabi.log.Logger;
import java.util.concurrent.TimeUnit;

/**
 * Mocker of {@link Table}.
 *
 * <p>Use it like this in your integration test:
 *
 * <pre>public class FooITCase {
 *   private Region region;
 *   private TableMocker table;
 *   &#64;Before
 *   public void prepare() {
 *     this.region = new Region.Simple(..your IT credentials..);
 *     this.table = new TableMocker(this.region, new CreateTableRequest()...);
 *     this.table.create();
 *   }
 *   &#64;After
 *   public void dropTable() {
 *     this.table.drop();
 *   }
 *   &#64;Test
 *   public void createsAndDeletesItems() {
 *     Foo foo = new Foo(this.region);
 *     foo.doSomething();
 *   }
 * }</pre>
 *
 * <p>In this example, a new DynamoDB table will be created before every
 * test method, and dropped when it's finished. This may be not the best
 * approach performance wise, since every table creation takes at least
 * ten seconds (at the time of writing). To speed things up a little, you
 * can create table before the entire test case and drop when all methods
 * are completed:
 *
 * <pre>public class FooITCase {
 *   private static Region region;
 *   private static TableMocker table;
 *   &#64;BeforeClass
 *   public static void prepare() {
 *     FooITCase.region = new Region.Simple(..your IT credentials..);
 *     FooITCase.table = new TableMocker(
 *       FooITCase.region, new CreateTableRequest()...
 *     );
 *     FooITCase.table.create();
 *   }
 *   &#64;AfterClass
 *   public static void dropTable() {
 *     FooITCase.table.drop();
 *   }
 *   &#64;Test
 *   public void createsAndDeletesItems() {
 *     Foo foo = new Foo(FooITCase.region);
 *     foo.doSomething();
 *   }
 * }</pre>
 *
 * @author Yegor Bugayenko (yegor@tpc2.com)
 * @version $Id$
 * @since 0.8
 */
public final class TableMocker {

    /**
     * Region.
     */
    private final transient Region region;

    /**
     * Table name.
     */
    private final transient CreateTableRequest request;

    /**
     * Public ctor.
     * @param reg Region
     * @param req Request
     */
    public TableMocker(final Region reg, final CreateTableRequest req) {
        this.region = reg;
        this.request = req;
    }

    /**
     * Create table.
     * @throws Exception If something fails
     */
    public void create() throws Exception {
        final AmazonDynamoDB aws = this.region.aws();
        final String name = this.request.getTableName();
        final ListTablesResult list = aws.listTables(
            new ListTablesRequest()
                .withExclusiveStartTableName(name)
                .withLimit(1)
        );
        if (list.getTableNames().isEmpty()) {
            aws.createTable(this.request);
        } else {
            Logger.info(
                this,
                "DynamoDB table '%s' already exists",
                name
            );
        }
        final DescribeTableRequest req = new DescribeTableRequest()
            .withTableName(name);
        while (true) {
            final DescribeTableResult result = aws.describeTable(req);
            if ("ACTIVE".equals(result.getTable().getTableStatus())) {
                Logger.info(
                    this,
                    "DynamoDB table '%s' is %s",
                    name,
                    result.getTable().getTableStatus()
                );
                break;
            }
            TimeUnit.SECONDS.sleep(Tv.FIVE);
            Logger.info(
                this,
                "waiting for DynamoDB table '%s': %s",
                name,
                result.getTable().getTableStatus()
            );
        }
    }

    /**
     * Create table.
     */
    public void drop() {
        final AmazonDynamoDB aws = this.region.aws();
        final String name = this.request.getTableName();
        aws.deleteTable(new DeleteTableRequest().withTableName(name));
        Logger.info(this, "DynamoDB table '%s' deleted", name);
    }

}
