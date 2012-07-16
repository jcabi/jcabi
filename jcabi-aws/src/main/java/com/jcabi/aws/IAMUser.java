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
package com.jcabi.aws;

import com.amazonaws.auth.AWSCredentials;
import com.amazonaws.auth.BasicAWSCredentials;
import java.io.IOException;
import java.util.concurrent.atomic.AtomicReference;

/**
 * IAM User identifier.
 *
 * <p>The class is thread-safe.
 *
 * @author Yegor Bugayenko (yegor@jcabi.com)
 * @version $Id$
 * @since 0.1.10
 */
public class IAMUser implements Resource {

    /**
     * The cloud.
     */
    private final transient Cloud cloud;

    /**
     * AWS IAM key.
     */
    private final transient AtomicReference<String> iamKey =
        new AtomicReference<String>();

    /**
     * AWS IAM secret key.
     */
    private final transient AtomicReference<String> iamSecret =
        new AtomicReference<String>();

    /**
     * Public ctor.
     * @param cld The cloud
     */
    public IAMUser(final Cloud cld) {
        this.cloud = cld;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public Cloud back() {
        return this.cloud;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void acquire() throws IOException {
        if (this.iamKey.get() == null) {
            throw new IllegalStateException(
                "IAM user key is not set with #key()"
            );
        }
        if (this.iamSecret.get() == null) {
            throw new IllegalStateException(
                "IAM user secret is not set with #secret()"
            );
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void close() throws IOException {
        // do nothing
    }

    /**
     * Set key.
     * @param key AWS key to set
     * @return This object
     */
    @Resource.Before
    public IAMUser key(final String key) {
        this.iamKey.set(key);
        return this;
    }

    /**
     * Set secret key.
     * @param secret AWS secret to set
     * @return This object
     */
    @Resource.Before
    public IAMUser secret(final String secret) {
        this.iamSecret.set(secret);
        return this;
    }

    /**
     * Get AWS credentials (throws exception if it's not live).
     * @return The credentials
     * @see #isLive()
     */
    @Resource.After
    public AWSCredentials credentials() {
        if (!this.isLive()) {
            throw new IllegalStateException(
                "IAM user is not live, use #isLive() to check first"
            );
        }
        return new BasicAWSCredentials(this.iamKey.get(), this.iamSecret.get());
    }

    /**
     * Is is a live configuration/access or we're in a unit testing mode?
     * @return TRUE if credentials are good for live deployment
     * @see #credentials()
     */
    @Resource.After
    public boolean isLive() {
        return this.iamKey.get().matches("[A-Z]{20}");
    }

}
