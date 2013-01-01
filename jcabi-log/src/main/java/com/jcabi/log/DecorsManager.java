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
package com.jcabi.log;

import java.lang.reflect.Constructor;
import java.util.Formattable;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

/**
 * Manager of all decors.
 *
 * @author Marina Kosenko (marina.kosenko@gmail.com)
 * @author Yegor Bugayenko (yegor@tpc2.com)
 * @version $Id$
 * @since 0.1
 */
final class DecorsManager {

    /**
     * Storage of all found decors.
     * @checkstyle LineLength (2 lines)
     */
    private static final ConcurrentMap<String, Class<? extends Formattable>> DECORS =
        new ConcurrentHashMap<String, Class<? extends Formattable>>();

    static {
        DecorsManager.DECORS.put("dom", DomDecor.class);
        DecorsManager.DECORS.put("exception", ExceptionDecor.class);
        DecorsManager.DECORS.put("list", ListDecor.class);
        DecorsManager.DECORS.put("ms", MsDecor.class);
        DecorsManager.DECORS.put("nano", NanoDecor.class);
        DecorsManager.DECORS.put("object", ObjectDecor.class);
        DecorsManager.DECORS.put("size", SizeDecor.class);
        DecorsManager.DECORS.put("secret", SecretDecor.class);
        DecorsManager.DECORS.put("text", TextDecor.class);
        DecorsManager.DECORS.put("type", TypeDecor.class);
    }

    /**
     * Private ctor.
     */
    private DecorsManager() {
        // empty
    }

    /**
     * Get decor by key.
     * @param key Key for the formatter to be used to fmt the arguments
     * @param arg The arbument to supply
     * @return The decor
     * @throws DecorException If some problem
     */
    public static Formattable decor(final String key, final Object arg)
        throws DecorException {
        final Class<? extends Formattable> type = DecorsManager.find(key);
        Formattable decor;
        try {
            decor = (Formattable) DecorsManager.ctor(type).newInstance(arg);
        } catch (InstantiationException ex) {
            throw new DecorException(
                ex,
                "Can't instantiate %s(%s)",
                type.getName(),
                arg.getClass().getName()
            );
        } catch (IllegalAccessException ex) {
            throw new DecorException(
                ex,
                "Can't access %s(%s)",
                type.getName(),
                arg.getClass().getName()
            );
        } catch (java.lang.reflect.InvocationTargetException ex) {
            throw new DecorException(
                ex,
                "Can't invoke %s(%s)",
                type.getName(),
                arg.getClass().getName()
            );
        }
        return decor;
    }

    /**
     * Find decor.
     * @param key Key for the formatter to be used to fmt the arguments
     * @return The type of decor found
     * @throws DecorException If some problem
     */
    @SuppressWarnings("unchecked")
    private static Class<? extends Formattable> find(final String key)
        throws DecorException {
        Class<? extends Formattable> type;
        if (DecorsManager.DECORS.containsKey(key)) {
            type = DecorsManager.DECORS.get(key);
        } else {
            try {
                type = (Class) Class.forName(key);
            } catch (ClassNotFoundException ex) {
                throw new DecorException(
                    ex,
                    "Decor '%s' not found and class can't be instantiated",
                    key
                );
            }
        }
        return type;
    }

    /**
     * Get ctor of the type.
     * @param type The type
     * @return The ctor
     * @throws DecorException If some problem
     */
    private static Constructor<?> ctor(final Class<? extends Formattable> type)
        throws DecorException {
        final Constructor[] ctors = type.getConstructors();
        if (ctors.length != 1) {
            throw new DecorException(
                "%s should have just one public one-arg ctor, but there are %d",
                type.getName(),
                ctors.length
            );
        }
        final Constructor<?> ctor = ctors[0];
        if (ctor.getParameterTypes().length != 1) {
            throw new DecorException(
                "%s public ctor should have just once parameter",
                type.getName()
            );
        }
        return ctor;
    }

}
