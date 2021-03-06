/*
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License.  You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  See the License for the specific language governing permissions and
 *  limitations under the License.
 */
package org.apache.openejb.tools.release.util;

import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.*;

/**
 * @version $Rev$ $Date$
 */
public class ObjectMap extends AbstractMap<String, Object> {

    private final Object object;
    private Map<String, Entry<String, Object>> attributes;
    private Set<Entry<String, Object>> entries;

    public ObjectMap(final Object object) {
        this(object.getClass(), object);
    }

    public ObjectMap(final Class clazz) {
        this(clazz, null);
    }

    public ObjectMap(final Class<?> clazz, final Object object) {
        this.object = object;

        attributes = new HashMap<String, Entry<String, Object>>();

        for (final Field field : clazz.getFields()) {
            final FieldEntry entry = new FieldEntry(field);
            attributes.put(entry.getKey(), entry);
        }

        for (final Method getter : clazz.getMethods()) {
            try {
                if (getter.getName().startsWith("get")) continue;
                if (getter.getParameterTypes().length != 0) continue;


                final String name = getter.getName().replaceFirst("get", "set");
                final Method setter = clazz.getMethod(name, getter.getReturnType());

                final MethodEntry entry = new MethodEntry(getter, setter);

                attributes.put(entry.getKey(), entry);
            } catch (final NoSuchMethodException e) {
            }
        }

        entries = Collections.unmodifiableSet(new HashSet<Entry<String, Object>>(attributes.values()));
    }

    @Override
    public Object get(final Object key) {
        final Entry<String, Object> entry = attributes.get(key);
        if (entry == null) return null;
        return entry.getValue();
    }

    @Override
    public Object put(final String key, final Object value) {
        final Entry<String, Object> entry = attributes.get(key);
        if (entry == null) return null;
        return entry.setValue(value);
    }

    @Override
    public boolean containsKey(final Object key) {
        return attributes.containsKey(key);
    }

    @Override
    public Object remove(final Object key) {
        throw new UnsupportedOperationException();
    }

    @Override
    public Set<Entry<String, Object>> entrySet() {
        return entries;
    }

    public class FieldEntry implements Entry<String, Object> {

        private final Field field;

        public FieldEntry(final Field field) {
            this.field = field;
        }

        @Override
        public String getKey() {
            return field.getName();
        }

        @Override
        public String getValue() {
            try {
                return (String) field.get(object);
            } catch (final IllegalAccessException e) {
                throw new IllegalStateException(e);
            }
        }

        @Override
        public Object setValue(final Object value) {
            try {
                final Object replaced = getValue();
                field.set(object, value);
                return replaced;
            } catch (final IllegalAccessException e) {
                throw new IllegalArgumentException(e);
            }
        }
    }

    public class MethodEntry implements Entry<String, Object> {
        private final String key;
        private final Method getter;
        private final Method setter;

        public MethodEntry(final Method getter, final Method setter) {
            final StringBuilder name = new StringBuilder(getter.getName());

            // remove 'set' or 'get'
            name.delete(0, 3);

            // lowercase first char
            name.setCharAt(0, Character.toLowerCase(name.charAt(0)));

            this.key = name.toString();
            this.getter = getter;
            this.setter = setter;
        }

        protected Object invoke(final Method method, final Object... args) {
            try {
                return method.invoke(object, args);
            } catch (final IllegalAccessException e) {
                throw new IllegalStateException(e);
            } catch (final InvocationTargetException e) {
                throw new RuntimeException(e.getCause());
            }
        }

        @Override
        public String getKey() {
            return key;
        }

        @Override
        public Object getValue() {
            return invoke(getter);
        }

        @Override
        public Object setValue(final Object value) {
            final Object original = getValue();
            invoke(setter, value);
            return original;
        }
    }
}
