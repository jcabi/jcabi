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
package com.jcabi.beanstalk.maven.plugin;

import com.amazonaws.services.elasticbeanstalk.model.S3Location;
import com.amazonaws.services.s3.AmazonS3;
import com.amazonaws.services.s3.model.ListObjectsRequest;
import com.amazonaws.services.s3.model.ObjectListing;
import com.amazonaws.services.s3.model.ObjectMetadata;
import com.amazonaws.services.s3.model.PutObjectResult;
import com.amazonaws.services.s3.model.S3ObjectSummary;
import com.jcabi.log.Logger;
import java.io.File;
import java.io.FileInputStream;
import java.io.InputStream;
import java.util.List;
import org.apache.commons.codec.digest.DigestUtils;
import org.apache.commons.io.FileUtils;
import org.apache.commons.io.IOUtils;

/**
 * Bundle that always overrides S3 object.
 *
 * @author Yegor Bugayenko (yegor@tpc2.com)
 * @version $Id$
 * @since 0.3
 */
final class OverridingBundle implements Bundle {

    /**
     * Amazon S3 client.
     */
    private final transient AmazonS3 client;

    /**
     * S3 bucket name.
     */
    private final transient String bucket;

    /**
     * S3 key name.
     */
    private final transient String key;

    /**
     * WAR file location.
     */
    private final transient File war;

    /**
     * Public ctor.
     * @param clnt The client
     * @param bckt S3 bucket
     * @param label Location of S3 object, label name
     * @param file WAR file location
     * @checkstyle ParameterNumber (4 lines)
     */
    public OverridingBundle(final AmazonS3 clnt, final String bckt,
        final String label, final File file) {
        this.client = clnt;
        this.bucket = bckt;
        this.key = label;
        this.war = file;
        if (!this.war.exists()) {
            throw new DeploymentException(
                String.format("WAR file %s doesn't exist", this.war)
            );
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public S3Location location() {
        if (this.exists()) {
            Logger.info(
                this,
                "No need to upload %s (%s) to S3, will use existing object",
                this.war,
                FileUtils.byteCountToDisplaySize(this.war.length())
            );
        } else {
            Logger.info(
                this,
                "Uploading %s (%s) to s3://%s/%s... (may take a few minutes)",
                this.war,
                FileUtils.byteCountToDisplaySize(this.war.length()),
                this.bucket,
                this.key
            );
            final PutObjectResult res = this.client.putObject(
                this.bucket, this.key, this.war
            );
            Logger.info(
                this,
                // @checkstyle LineLength (1 line)
                "Uploaded successfully to S3, etag=%s, expires=%s, exp.rule=%s, encryption=%s, version=%s",
                res.getETag(),
                res.getExpirationTime(),
                res.getExpirationTimeRuleId(),
                res.getServerSideEncryption(),
                res.getVersionId()
            );
        }
        return new S3Location(this.bucket, this.key);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public String name() {
        return this.key;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public String etag() {
        try {
            final InputStream stream = new FileInputStream(this.war);
            final String hash = DigestUtils.md5Hex(stream);
            IOUtils.closeQuietly(stream);
            return hash;
        } catch (java.io.IOException ex) {
            throw new DeploymentException(ex);
        }
    }

    /**
     * This object already exists in the bucket.
     * @return TRUE if exists already
     */
    private boolean exists() {
        boolean exists = false;
        if (this.keyExists()) {
            final ObjectMetadata meta = this.client.getObjectMetadata(
                this.bucket, this.key
            );
            final String etag = this.etag();
            if (meta.getETag().equals(etag)) {
                Logger.info(
                    this,
                    // @checkstyle LineLength (1 line)
                    "MD5 ETag '%' of existing S3 object '%s' (%s) equals to the one of the local file (%s)",
                    meta.getETag(),
                    this.key,
                    FileUtils.byteCountToDisplaySize(meta.getContentLength()),
                    FileUtils.byteCountToDisplaySize(this.war.length())
                );
                exists = true;
            } else {
                Logger.info(
                    this,
                    // @checkstyle LineLength (1 line)
                    "MD5 ETag '%s' of S3 object '%s' (%s) differs from '%s' of the local file (%s)",
                    meta.getETag(),
                    this.key,
                    FileUtils.byteCountToDisplaySize(meta.getContentLength()),
                    etag,
                    FileUtils.byteCountToDisplaySize(this.war.length())
                );
            }
        }
        return exists;
    }

    /**
     * This key already exists in the bucket.
     * @return TRUE if key exists already
     */
    private boolean keyExists() {
        final ObjectListing listing = this.client.listObjects(
            new ListObjectsRequest()
                .withBucketName(this.bucket)
                .withDelimiter("")
                .withMaxKeys(1)
                .withPrefix(this.key)
        );
        final List<S3ObjectSummary> summaries = listing.getObjectSummaries();
        boolean exists = false;
        if (summaries.isEmpty()) {
            Logger.info(
                this,
                "S3 object '%s' not found in '%s' bucket",
                this.key,
                this.bucket
            );
        } else {
            final S3ObjectSummary summary = summaries.get(0);
            Logger.info(
                this,
                // @checkstyle LineLength (1 line)
                "S3 object '%s' found in '%s' bucket (size=%d, last-modified=%s, etag=%s)",
                summary.getKey(),
                summary.getBucketName(),
                summary.getSize(),
                summary.getLastModified(),
                summary.getETag()
            );
            exists = true;
        }
        return exists;
    }

}
