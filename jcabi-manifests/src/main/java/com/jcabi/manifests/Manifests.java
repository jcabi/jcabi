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
package com.jcabi.manifests;

import com.jcabi.log.Logger;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.Serializable;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URL;
import java.util.Enumeration;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.TreeSet;
import java.util.concurrent.ConcurrentHashMap;
import java.util.jar.Attributes;
import java.util.jar.Attributes.Name;
import java.util.jar.Manifest;
import javax.servlet.ServletContext;
import javax.validation.constraints.NotNull;
import javax.validation.constraints.Pattern;
import org.apache.commons.io.IOUtils;
import org.apache.commons.lang.SerializationUtils;

/**
 * Static reader of {@code META-INF/MANIFEST.MF} files.
 *
 * The class provides convenient methods to read
 * all {@code MANIFEST.MF} files available in classpath
 * and all attributes from them. This mechanism is very useful for transferring
 * information from continuous integration environment to the production
 * environment. For example, you want your site to show project version and
 * the date of {@code WAR} file packaging. First, you configure
 * {@code maven-war-plugin} to add this information to {@code MANIFEST.MF}:
 *
 * <pre> &lt;plugin>
 *  &lt;artifactId>maven-war-plugin&lt;/artifactId>
 *  &lt;configuration>
 *   &lt;archive>
 *    &lt;manifestEntries>
 *     &lt;Foo-Version>${project.version}&lt;/Foo-Version>
 *     &lt;Foo-Date>${maven.build.timestamp}&lt;/Foo-Date>
 *    &lt;/manifestEntries>
 *   &lt;/archive>
 *  &lt;/configuration>
 * &lt;/plugin></pre>
 *
 * <p>{@code maven-war-plugin} will add these attributes to your
 * {@code MANIFEST.MF} file and the
 * project will be deployed to the production environment. Then, you can read
 * these attributes where it's necessary (in one of your JAXB annotated objects,
 * for example) and show to users:
 *
 * <pre> import com.jcabi.manifests.Manifest;
 * import java.text.SimpleDateFormat;
 * import java.util.Date;
 * import java.util.Locale;
 * import javax.xml.bind.annotation.XmlElement;
 * import javax.xml.bind.annotation.XmlRootElement;
 * &#64;XmlRootElement
 * public final class Page {
 *   &#64;XmlElement
 *   public String version() {
 *     return Manifests.read("Foo-Version");
 *   }
 *   &#64;XmlElement
 *   public Date date() {
 *     return new SimpleDateFormat("yyyy.MM.dd", Locale.ENGLISH).parse(
 *       Manifests.read("Foo-Date");
 *     );
 *   }
 * }</pre>
 *
 * <p>In unit and integration tests you may need to inject some values
 * to {@code MANIFEST.MF} in runtime (for example, in your bootstrap Groovy
 * scripts):
 *
 * <pre> import com.jcabi.manifests.Manifests
 * Manifests.inject("Foo-URL", "http://localhost/abc");</pre>
 *
 * <p>When it is necessary to isolate such injections between different unit
 * tests "snapshots" may help, for example (it's a method in a unit test):
 *
 * <pre> &#64;Test
 * public void testSomeCode() {
 *   // save current state of all MANIFEST.MF attributes
 *   final byte[] snapshot = Manifests.snapshot();
 *   // inject new attribute required for this specific test
 *   Manifests.inject("Foo-URL", "http://localhost/abc");
 *   // restore back all attributes, as they were before the injection
 *   Manifests.revert(snapshot);
 * }</pre>
 *
 * <p>The only dependency you need (check the latest version at
 * <a href="http://www.jcabi.com/jcabi-manifests/">jcabi-manifests</a>):
 *
 * <pre> &lt;dependency>
 *  &lt;groupId>com.jcabi&lt;/groupId>
 *  &lt;artifactId>jcabi-manifests&lt;/artifactId>
 * &lt;/dependency></pre>
 *
 * <p>The class is immutable and thread-safe.
 *
 * @author Yegor Bugayenko (yegor@tpc2.com)
 * @version $Id$
 * @since 0.7
 * @see <a href="http://download.oracle.com/javase/1,5.0/docs/guide/jar/jar.html#JAR%20Manifest">JAR Manifest</a>
 * @see <a href="http://maven.apache.org/shared/maven-archiver/index.html">Maven Archiver</a>
 * @link <a href="http://www.jcabi.com/jcabi-manifests/index.html">www.jcabi.com/jcabi-manifests</a>
 */
@SuppressWarnings("PMD.UseConcurrentHashMap")
public final class Manifests {

    /**
     * Injected attributes.
     * @see #inject(String,String)
     */
    private static final Map<String, String> INJECTED =
        new ConcurrentHashMap<String, String>();

    /**
     * Attributes retrieved from all existing {@code MANIFEST.MF} files.
     * @see #load()
     */
    private static Map<String, String> attributes = Manifests.load();

    /**
     * Failures registered during loading.
     * @see #load()
     */
    private static Map<URI, String> failures;

    /**
     * It's a utility class, can't be instantiated.
     */
    private Manifests() {
        // intentionally empty
    }

    /**
     * Read one attribute available in one of {@code MANIFEST.MF} files.
     *
     * <p>If such a attribute doesn't exist {@link IllegalArgumentException}
     * will be thrown. If you're not sure whether the attribute is present or
     * not use {@link #exists(String)} beforehand.
     *
     * <p>During testing you can inject attributes into this class by means
     * of {@link #inject(String,String)}.
     *
     * <p>The method is thread-safe.
     *
     * @param name Name of the attribute
     * @return The value of the attribute retrieved
     */
    public static String read(
        @NotNull(message = "attribute name can't be NULL")
        @Pattern(regexp = ".+", message = "attribute name can't be empty")
        final String name) {
        if (Manifests.attributes == null) {
            throw new IllegalArgumentException(
                "Manifests haven't been loaded yet, internal error"
            );
        }
        if (!Manifests.exists(name)) {
            final StringBuilder bldr = new StringBuilder(
                Logger.format(
                    // @checkstyle LineLength (1 line)
                    "Atribute '%s' not found in MANIFEST.MF file(s) among %d other attribute(s) %[list]s and %d injection(s)",
                    name,
                    Manifests.attributes.size(),
                    new TreeSet<String>(Manifests.attributes.keySet()),
                    Manifests.INJECTED.size()
                )
            );
            if (!Manifests.failures.isEmpty()) {
                bldr.append("; failures: ").append(
                    Logger.format("%[list]s", Manifests.failures.keySet())
                );
            }
            throw new IllegalArgumentException(bldr.toString());
        }
        String result;
        if (Manifests.INJECTED.containsKey(name)) {
            result = Manifests.INJECTED.get(name);
        } else {
            result = Manifests.attributes.get(name);
        }
        Logger.debug(this, "#read('%s'): found '%s'", name, result);
        return result;
    }

    /**
     * Inject new attribute.
     *
     * <p>An attribute can be injected in runtime, mostly for the sake of
     * unit and integration testing. Once injected an attribute becomes
     * available with {@link #read(String)}.
     *
     * <p>The method is thread-safe.
     *
     * @param name Name of the attribute
     * @param value The value of the attribute being injected
     */
    public static void inject(
        @NotNull(message = "injected name can't be NULL")
        @Pattern(regexp = ".+", message = "name of attribute can't be empty")
        final String name,
        @NotNull(message = "inected value can't be NULL") final String value) {
        if (Manifests.INJECTED.containsKey(name)) {
            Logger.info(
                Manifests.class,
                "#inject(%s, '%s'): replaced previous injection '%s'",
                name,
                value,
                Manifests.INJECTED.get(name)
            );
        } else {
            Logger.info(
                Manifests.class,
                "#inject(%s, '%s'): injected",
                name,
                value
            );
        }
        Manifests.INJECTED.put(name, value);
    }

    /**
     * Check whether attribute exists in any of {@code MANIFEST.MF} files.
     *
     * <p>Use this method before {@link #read(String)} to check whether an
     * attribute exists, in order to avoid a runtime exception.
     *
     * <p>The method is thread-safe.
     *
     * @param name Name of the attribute to check
     * @return Returns {@code TRUE} if it exists, {@code FALSE} otherwise
     */
    public static boolean exists(
        @NotNull(message = "name of attribute can't be NULL")
        @Pattern(regexp = ".+", message = "name of attribute can't be empty")
        final String name) {
        final boolean exists = Manifests.attributes.containsKey(name)
            || Manifests.INJECTED.containsKey(name);
        Logger.debug(this, "#exists('%s'): %B", name, exists);
        return exists;
    }

    /**
     * Make a snapshot of current attributes and their values.
     *
     * <p>The method is thread-safe.
     *
     * @return The snapshot, to be used later with {@link #revert(byte[])}
     */
    public static byte[] snapshot() {
        byte[] snapshot;
        synchronized (Manifests.INJECTED) {
            snapshot = SerializationUtils.serialize(
                (Serializable) Manifests.INJECTED
            );
        }
        Logger.debug(
            Manifests.class,
            "#snapshot(): created (%d bytes)",
            snapshot.length
        );
        return snapshot;
    }

    /**
     * Revert to the state that was recorded by {@link #snapshot()}.
     *
     * <p>The method is thread-safe.
     *
     * @param snapshot The snapshot taken by {@link #snapshot()}
     */
    @SuppressWarnings("unchecked")
    public static void revert(@NotNull final byte[] snapshot) {
        synchronized (Manifests.INJECTED) {
            Manifests.INJECTED.clear();
            Manifests.INJECTED.putAll(
                (Map<String, String>) SerializationUtils.deserialize(snapshot)
            );
        }
        Logger.debug(
            Manifests.class,
            "#revert(%d bytes): reverted",
            snapshot.length
        );
    }

    /**
     * Append attributes from the web application {@code MANIFEST.MF}.
     *
     * <p>You can call this method in your own
     * {@link javax.servlet.Filter} or
     * {@link javax.servlet.ServletContextListener},
     * in order to inject {@code MANIFEST.MF} attributes to the class.
     *
     * <p>The method is thread-safe.
     *
     * @param ctx Servlet context
     * @see #Manifests()
     * @throws IOException If some I/O problem inside
     */
    public static void append(@NotNull final ServletContext ctx)
        throws IOException {
        final long start = System.currentTimeMillis();
        URL main;
        try {
            main = ctx.getResource("/META-INF/MANIFEST.MF");
        } catch (java.net.MalformedURLException ex) {
            throw new IOException(ex);
        }
        if (main == null) {
            Logger.warn(
                Manifests.class,
                "#append(%s): MANIFEST.MF not found in WAR package",
                ctx.getClass().getName()
            );
        } else {
            final Map<String, String> attrs = Manifests.loadOneFile(main);
            Manifests.attributes.putAll(attrs);
            Logger.info(
                Manifests.class,
                // @checkstyle LineLength (1 line)
                "#append(%s): %d attribs loaded from %s in %[ms]s (%d total): %[list]s",
                ctx.getClass().getName(),
                attrs.size(),
                main,
                System.currentTimeMillis() - start,
                Manifests.attributes.size(),
                new TreeSet<String>(attrs.keySet())
            );
        }
    }

    /**
     * Append attributes from the file.
     *
     * <p>The method is thread-safe.
     *
     * @param file The file to load attributes from
     * @throws IOException If some I/O problem inside
     */
    public static void append(@NotNull final File file) throws IOException {
        final long start = System.currentTimeMillis();
        Map<String, String> attrs;
        try {
            attrs = Manifests.loadOneFile(file.toURI().toURL());
        } catch (java.net.MalformedURLException ex) {
            throw new IOException(ex);
        }
        Manifests.attributes.putAll(attrs);
        Logger.info(
            Manifests.class,
            // @checkstyle LineLength (1 line)
            "#append('%s'): %d attributes loaded in %[ms]s (%d total): %[list]s",
            file, attrs.size(),
            System.currentTimeMillis() - start,
            Manifests.attributes.size(),
            new TreeSet<String>(attrs.keySet())
        );
    }

    /**
     * Load attributes from classpath.
     *
     * <p>This method doesn't throw any checked exceptions because it is called
     * from a static context above. It's just more convenient to catch all
     * exceptions here than above in a static call block.
     *
     * @return All found attributes
     */
    private static Map<String, String> load() {
        final long start = System.currentTimeMillis();
        Manifests.failures = new ConcurrentHashMap<URI, String>();
        final Map<String, String> attrs =
            new ConcurrentHashMap<String, String>();
        int count = 0;
        for (URI uri : Manifests.uris()) {
            try {
                attrs.putAll(Manifests.loadOneFile(uri.toURL()));
            } catch (IOException ex) {
                Manifests.failures.put(uri, ex.getMessage());
                Logger.error(
                    Manifests.class,
                    "#load(): '%s' failed %[exception]s",
                    uri, ex
                );
            }
            ++count;
        }
        Logger.info(
            Manifests.class,
            "#load(): %d attribs loaded from %d URL(s) in %[ms]s: %[list]s",
            attrs.size(), count,
            System.currentTimeMillis() - start,
            new TreeSet<String>(attrs.keySet())
        );
        return attrs;
    }

    /**
     * Find all URLs.
     *
     * <p>This method doesn't throw any checked exceptions just for convenience
     * of calling of it (above in {@linke #load}), although it is clear that
     * {@link IOException} is a good candidate for being thrown out of it.
     *
     * @return The list of URLs
     * @see #load()
     */
    private static Set<URI> uris() {
        Enumeration<URL> resources;
        try {
            resources = Thread.currentThread().getContextClassLoader()
                .getResources("META-INF/MANIFEST.MF");
        } catch (IOException ex) {
            throw new IllegalStateException(ex);
        }
        final Set<URI> uris = new HashSet<URI>();
        while (resources.hasMoreElements()) {
            try {
                uris.add(resources.nextElement().toURI());
            } catch (URISyntaxException ex) {
                throw new IllegalStateException(ex);
            }
        }
        return uris;
    }

    /**
     * Load attributes from one file.
     *
     * <p>Inside the method we catch {@code RuntimeException} (which may look
     * suspicious) in order to protect our execution flow from expected (!)
     * exceptions from {@link Manifest#getMainAttributes()}. For some reason,
     * this JDK method doesn't throw checked exceptions if {@code MANIFEST.MF}
     * file format is broken. Instead, it throws a runtime exception (an
     * unchecked one), which we should catch in such an inconvenient way.
     *
     * @param url The URL of it
     * @return The attributes loaded
     * @see #load()
     * @see tickets #193 and #323
     * @throws IOException If some problem happens
     */
    @SuppressWarnings("PMD.AvoidCatchingGenericException")
    private static Map<String, String> loadOneFile(final URL url)
        throws IOException {
        final Map<String, String> props =
            new ConcurrentHashMap<String, String>();
        final InputStream stream = url.openStream();
        try {
            final Manifest manifest = new Manifest(stream);
            final Attributes attrs = manifest.getMainAttributes();
            for (Object key : attrs.keySet()) {
                final String value = attrs.getValue((Name) key);
                props.put(key.toString(), value);
            }
            Logger.debug(
                Manifests.class,
                "#loadOneFile('%s'): %d attributes loaded (%[list]s)",
                url, props.size(), new TreeSet<String>(props.keySet())
            );
        // @checkstyle IllegalCatch (1 line)
        } catch (RuntimeException ex) {
            Logger.error(
                Manifests.class,
                "#getMainAttributes(): '%s' failed %[exception]s",
                url, ex
            );
        } finally {
            IOUtils.closeQuietly(stream);
        }
        return props;
    }

}
