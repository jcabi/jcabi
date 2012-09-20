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
package com.jcabi.aether;

import com.jcabi.log.Logger;
import org.apache.maven.repository.internal.DefaultArtifactDescriptorReader;
import org.apache.maven.repository.internal.DefaultVersionRangeResolver;
import org.apache.maven.repository.internal.DefaultVersionResolver;
import org.sonatype.aether.RepositorySystem;
import org.sonatype.aether.connector.async.AsyncRepositoryConnectorFactory;
import org.sonatype.aether.connector.file.FileRepositoryConnectorFactory;
import org.sonatype.aether.connector.wagon.PlexusWagonConfigurator;
import org.sonatype.aether.connector.wagon.WagonConfigurator;
import org.sonatype.aether.connector.wagon.WagonProvider;
import org.sonatype.aether.connector.wagon.WagonRepositoryConnectorFactory;
import org.sonatype.aether.impl.ArtifactDescriptorReader;
import org.sonatype.aether.impl.VersionRangeResolver;
import org.sonatype.aether.impl.VersionResolver;
import org.sonatype.aether.impl.internal.DefaultRepositorySystem;
import org.sonatype.aether.impl.internal.DefaultServiceLocator;
import org.sonatype.aether.spi.connector.RepositoryConnectorFactory;

/**
 * Builder of {@link RepositorySystem} class.
 *
 * <p>The class is immutable and possibly NOT thread-safe.
 *
 * @author Yegor Bugayenko (yegor@jcabi.com)
 * @version $Id$
 * @since 0.1.6
 */
final class RepositorySystemBuilder {

    /**
     * Build it.
     * @return The repo system.
     */
    public RepositorySystem build() {
        final DefaultServiceLocator locator = new DefaultServiceLocator();
        locator.addService(
            RepositoryConnectorFactory.class,
            FileRepositoryConnectorFactory.class
        );
        locator.addService(
            RepositoryConnectorFactory.class,
            AsyncRepositoryConnectorFactory.class
        );
        locator.addService(
            WagonProvider.class,
            AmazonWagonProvider.class
        );
        locator.addService(
            WagonConfigurator.class,
            PlexusWagonConfigurator.class
        );
        locator.addService(
            RepositoryConnectorFactory.class,
            WagonRepositoryConnectorFactory.class
        );
        locator.addService(
            RepositorySystem.class,
            DefaultRepositorySystem.class
        );
        locator.addService(
            VersionResolver.class,
            DefaultVersionResolver.class
        );
        locator.addService(
            VersionRangeResolver.class,
            DefaultVersionRangeResolver.class
        );
        locator.addService(
            ArtifactDescriptorReader.class,
            DefaultArtifactDescriptorReader.class
        );
        final RepositorySystem system =
            locator.getService(RepositorySystem.class);
        if (system == null) {
            throw new IllegalStateException("failed to get service");
        }
        Logger.debug(this, "#build(): %[type]s located", system);
        return system;
    }

}
