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

import com.amazonaws.auth.AWSCredentials;
import com.jcabi.log.Logger;
import org.apache.maven.plugin.MojoFailureException;
import org.apache.maven.settings.Server;
import org.apache.maven.settings.Settings;

/**
 * AWS credentials from settings.xml.
 *
 * @author Yegor Bugayenko (yegor@jcabi.com)
 * @version $Id$
 * @since 0.3
 */
final class ServerCredentials implements AWSCredentials {

    /**
     * AWS key.
     */
    private final transient String key;

    /**
     * AWS secret.
     */
    private final transient String secret;

    /**
     * Public ctor.
     * @param settings Maven settings
     * @param name Name of server ID
     * @throws MojoFailureException If some error
     */
    public ServerCredentials(final Settings settings, final String name)
        throws MojoFailureException {
        final Server server = settings.getServer(name);
        if (server == null) {
            throw new MojoFailureException(
                String.format(
                    "Server '%s' is absent in settings.xml",
                    name
                )
            );
        }
        this.key = server.getUsername().trim();
        if (!this.key.matches("[A-Z0-9]{20}")) {
            throw new MojoFailureException(
                String.format(
                    "Key '%s' for server '%s' is not a valid AWS key",
                    this.key,
                    name
                )
            );
        }
        this.secret = server.getPassword().trim();
        if (!this.secret.matches("[a-zA-Z0-9\\+/]{40}")) {
            throw new MojoFailureException(
                String.format(
                    "Secret '%s' for server '%s' is not a valid AWS secret",
                    this.secret,
                    name
                )
            );
        }
        Logger.info(
            this,
            "Using server '%s' with AWS key '%s'",
            name,
            this.key
        );
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public String getAWSAccessKeyId() {
        return this.key;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public String getAWSSecretKey() {
        return this.secret;
    }

}
