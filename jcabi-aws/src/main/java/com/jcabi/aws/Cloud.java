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

import com.jcabi.log.Logger;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

/**
 * Manager of cloud resources, your main entry point to the library.
 *
 * <p>Use it like this:
 *
 * <pre>MountedDirectory dir = new Cloud()
 *   .get(IAMUser.class)
 *     .key("...")
 *     .secret("...")
 *     .back()
 *   .get(Sudoer.class)
 *     .pem(this.getClass().getResource("my-key.pem"))
 *     .back()
 *   .get(AttachedVolume.class)
 *     .name("v-123456")
 *     .device("/dev/xdva5")
 *     .back()
 *   .get(FormattedVolume.class)
 *     .type("ext4")
 *     .back()
 *   .get(MountedDirectory.class)
 *     .path("/mnt/xdva5")
 *     .acquire();
 * FileUtils.touch(new File(dir.path(), "hello.txt"));
 * dir.close();
 * </pre>
 *
 * <p>This snippet will do the following:
 *
 * <ol>
 * <li>Detect your running EC2 instance name;
 * <li>Attach an EBS volume {@code v-123456} to it as {@code /dev/xdva5};
 * <li>Format the volume, if it's not yet formatted;
 * <li>Mount the volume to {@code /mnt/xdva5};
 * <li>Change ownership of {@code /mnt/xdva5} to the current user;
 * <li>Create a file {@code /mnt/xdva5/hello.txt} (if it's absent);
 * <li>Unmount the device;
 * <li>Detach the volume.
 * </ol>
 *
 * <p>The class is mutable and thread-safe.
 *
 * @author Yegor Bugayenko (yegor@jcabi.com)
 * @version $Id$
 * @since 0.1.10
 */
public final class Cloud {

    /**
     * All resources.
     */
    private final transient ConcurrentMap<Class<? extends Resource>, Resource> resources =
        new ConcurrentHashMap<Class<? extends Resource>, Resource>();

    /**
     * Get resource of the given type.
     * @param type Type of resource to get
     * @return The resource found and ready to be acquired
     * @param <T> Type of resource
     */
    public <T extends Resource> T get(final Class<T> type) {
        this.resources.putIfAbsent(type, this.instantiate(type));
        return type.cast(this.resources.get(type));
    }

    /**
     * Creare a resource of provided type.
     * @param type Type of resource to get
     * @return An instance
     */
    public Resource instantiate(final Class<? extends Resource> type) {
        try {
            return type.getConstructor(Cloud.class).newInstance(this);
        } catch (NoSuchMethodException ex) {
            throw new IllegalArgumentException(ex);
        } catch (InstantiationException ex) {
            throw new IllegalArgumentException(ex);
        } catch (IllegalAccessException ex) {
            throw new IllegalArgumentException(ex);
        } catch (java.lang.reflect.InvocationTargetException ex) {
            throw new IllegalArgumentException(ex);
        }
    }

}
