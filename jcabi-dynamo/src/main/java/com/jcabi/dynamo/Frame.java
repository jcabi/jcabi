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

import com.amazonaws.services.dynamodbv2.model.Condition;
import com.jcabi.aspects.Immutable;
import java.util.Collection;
import java.util.Map;
import javax.validation.constraints.NotNull;

/**
 * DynamoDB frame (subset of a table).
 *
 * <p>{@link Frame} is a subset of a Dynamo table, and is used to retrieve items
 * and remove them. {@link Frame} acts as an iterable immutable collection of
 * items. You can't use {@link Frame#remove(Object)} method directly. Instead,
 * find the right item using iterator and than remove it with
 * {@link Iterator#remove()}.
 *
 * <p>To fetch items from Dynamo DB, {@link Frame} uses
 * {@code Scan} operation.
 *
 * @author Yegor Bugayenko (yegor@tpc2.com)
 * @version $Id$
 * @see Item
 * @see <a href="http://docs.aws.amazon.com/amazondynamodb/latest/developerguide/QueryAndScan.html">Query and Scan</a>
 */
@Immutable
public interface Frame extends Collection<Item> {

    /**
     * Refine using this condition.
     *
     * <p>It is recommended to use a utility static method
     * {@link Conditions.equalTo(Object)}, when condition is simply an
     * equation to a plain string value.
     *
     * @param name Attribute name
     * @param condition The condition
     * @return New frame
     */
    @NotNull
    Frame where(@NotNull String name, @NotNull Condition condition);

    /**
     * Refine using these conditions.
     *
     * <p>It is recommended to use {@link Conditions} supplementary class
     * instead of a raw {@link Map}.
     *
     * @param conditions The conditions
     * @return New frame
     * @see Conditions
     */
    @NotNull
    Frame where(@NotNull Map<String, Condition> conditions);

    /**
     * Get back to the table this frame came from.
     * @return The table
     */
    @NotNull
    Table table();

}
