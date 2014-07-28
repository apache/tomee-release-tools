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


import java.lang.reflect.Method;
import java.util.*;
import java.util.regex.Pattern;

/**
 * @version $Revision$ $Date$
 */
public class ObjectList<E> extends ArrayList<E> {

    public ObjectList() {
    }

    public ObjectList(final Collection collection) {
        super(collection);
    }

    public ObjectList(final int i) {
        super(i);
    }

    private static int compare(final Object a, final Object b) {
        if (a instanceof Comparable) {
            return ((Comparable) a).compareTo(b);
        } else {
            return a.toString().compareTo(b.toString());
        }
    }

    public Object min(final String field) {
        return Collections.min(this, getComparator(field));
    }

    public Object max(final String field) {
        return Collections.max(this, getComparator(field));
    }

    // public List values(String field) {
    // if (size() == 0) return this;
    // Accessor accessor = new Accessor(field, this);
    //
    // boolean ObjectData = true;
    // List uniqueList = new ArrayList();
    // for (int i = 0; i < this.size(); i++) {
    // Object Object = (Object) this.get(i);
    // Object value = accessor.getValue(Object);
    // if (!uniqueList.contains(value)){
    // uniqueList.add(value);
    // ObjectData = ObjectData && value instanceof Object;
    // }
    // }
    //
    // if (ObjectData){
    // return new ObjectList(uniqueList);
    // } else {
    // return uniqueList;
    // }
    // }

    public List collect(final String field) {
        if (size() == 0) return this;
        final Accessor accessor = new Accessor(field, this);

        boolean ObjectData = true;
        final List collection = new ArrayList();
        for (int i = 0; i < this.size(); i++) {
            final Object Object = (Object) this.get(i);
            final Object value = accessor.getValue(Object);
            if (value instanceof List) {
                final List list = (List) value;
                for (int j = 0; j < list.size(); j++) {
                    final Object object = list.get(j);
                    collection.add(object);
                    ObjectData = ObjectData && object instanceof Object;
                }
            } else {
                collection.add(value);
                ObjectData = ObjectData && value instanceof Object;
            }
        }

        if (ObjectData) {
            return new ObjectList(collection);
        } else {
            return collection;
        }
    }

    public ObjectList<E> unique(final String field) {
        if (size() == 0) return this;
        final Accessor accessor = new Accessor(field, this);
        final ObjectList subset = new ObjectList();
        final List uniqueList = new ArrayList();
        for (int i = 0; i < this.size(); i++) {
            final Object Object = (Object) this.get(i);
            final Object value = accessor.getValue(Object);
            if (!uniqueList.contains(value)) {
                uniqueList.add(value);
                subset.add(Object);
            }
        }

        return subset;
    }

    /**
     * Returns returns a new list with both lists added together.
     * <p/>
     * http://en.wikipedia.org/wiki/Set#Unions
     *
     * @param list
     * @return new list c = a + b
     */
    public ObjectList<E> union(final List list) {
        final ObjectList difference = new ObjectList(this);
        for (int i = 0; i < list.size(); i++) {
            final Object object = list.get(i);
            if (!this.contains(object)) {
                difference.add(object);
            }
        }
        return difference;
    }

    /**
     * Returns a new list containing the common items from this list and the specified list.
     * <p/>
     * Put another way, returns only the items in a and b that overlap.
     * <p/>
     * http://en.wikipedia.org/wiki/Set#Intersections
     *
     * @param list
     * @return new list c = a && b
     */
    public ObjectList<E> intersection(final List list) {
        final ObjectList common = new ObjectList();
        for (int i = 0; i < this.size(); i++) {
            final Object object = this.get(i);
            if (list.contains(object)) {
                common.add(object);
            }
        }
        return common;
    }

    /**
     * Synonym for intersection
     *
     * @param list
     * @return new list c = a && b
     */
    public ObjectList<E> common(final List list) {
        return intersection(list);
    }

    /**
     * Returns a new list containing only the items from list a not present in list b.
     * <p/>
     * http://en.wikipedia.org/wiki/Set#Complements
     *
     * @param list
     * @return new list c = a - b
     */
    public ObjectList<E> subtract(final List list) {
        final ObjectList subtract = new ObjectList(this);
        for (int i = 0; i < list.size(); i++) {
            final Object object = list.get(i);
            subtract.remove(object);
        }
        return subtract;
    }

    /**
     * Returns a new list containing only the items from list a not present in list b.
     * <p/>
     * This is logically equivalent to:
     * <p/>
     * a.union(b).subtract(a.common(b))
     * <p/>
     * Though this method is more efficient.
     * <p/>
     * http://en.wikipedia.org/wiki/Symmetric_difference
     *
     * @param list
     * @return new list c = a XOR b
     */
    public ObjectList<E> difference(final List list) {
        final ObjectList difference = new ObjectList(this);
        for (int i = 0; i < list.size(); i++) {
            final Object object = list.get(i);
            if (this.contains(object)) {
                difference.remove(object);
            } else {
                difference.add(object);
            }
        }
        return difference;
    }

    public int sum(final String field) {
        if (size() == 0) return 0;
        int sum = 0;
        final Accessor accessor = new Accessor(field, this);

        for (int i = 0; i < this.size(); i++) {
            try {
                final Object Object = (Object) this.get(i);
                sum += accessor.intValue(Object);
            } catch (final NumberFormatException e) {
            }
        }
        return sum;
    }

    public int average(final String field) {
        if (size() == 0) return 0;
        int sum = 0;
        final Accessor accessor = new Accessor(field, this);
        int count = 0;
        for (int i = 0; i < this.size(); i++) {
            try {
                final Object Object = (Object) this.get(i);
                sum += accessor.intValue(Object);
                count++;
            } catch (final NumberFormatException e) {
            }
        }
        return (sum == 0) ? sum : sum / count;
    }

    public ObjectList<E> contains(final String field, final String string) {
        if (size() == 0) return this;
        final Accessor accessor = new Accessor(field, this);
        final ObjectList subset = new ObjectList();
        for (int i = 0; i < this.size(); i++) {
            final Object Object = (Object) this.get(i);
            final String value = accessor.stringValue(Object);
            if (value != null && value.indexOf(string) != -1) {
                subset.add(Object);
            }
        }
        return subset;
    }

    public ObjectList<E> matches(final String field, final String string) {
        if (size() == 0) return this;
        final Pattern pattern = Pattern.compile(string);
        final Accessor accessor = new Accessor(field, this);
        final ObjectList subset = new ObjectList();
        for (int i = 0; i < this.size(); i++) {
            final Object Object = (Object) this.get(i);
            final String value = accessor.stringValue(Object);
            if (value != null && pattern.matcher(value).matches()) {
                subset.add(Object);
            }
        }
        return subset;
    }

    public ObjectList<E> equals(final String field, final String string) {
        if (size() == 0) return this;
        final Accessor accessor = new Accessor(field, this);
        final ObjectList subset = new ObjectList();
        for (int i = 0; i < this.size(); i++) {
            final Object Object = (Object) this.get(i);
            final String value = accessor.stringValue(Object);
            if (value != null && value.equals(string)) {
                subset.add(Object);
            }
        }
        return subset;
    }

    public ObjectList<E> greater(final String field, final String string) {
        return compareAndCollect(field, string, 1);
    }

    public ObjectList<E> less(final String field, final String string) {
        return compareAndCollect(field, string, -1);
    }

    public ObjectList<E> greater(final String field, final Object object) {
        return compareAndCollect(field, object, 1);
    }

    public ObjectList<E> less(final String field, final Object object) {
        return compareAndCollect(field, object, -1);
    }

    /**
     * Synonym for sort(field, false);
     *
     * @param field
     */
    public ObjectList<E> ascending(final String field) {
        return sort(field);
    }

    /**
     * Synonym for sort(field, true);
     *
     * @param field
     */
    public ObjectList<E> descending(final String field) {
        return sort(field, true);
    }

    public ObjectList<E> sort(final String field) {
        return sort(field, false);
    }

    public ObjectList<E> sort(final String field, final boolean reverse) {
        if (size() == 0) return this;
        Comparator comparator = getComparator(field);

        comparator = reverse ? new ReverseComparator(comparator) : comparator;
        final ObjectList list = new ObjectList(this);
        Collections.sort(list, comparator);

        return list;
    }

    private ObjectList compareAndCollect(final String field, final Object valueB, final int condition) {
        if (size() == 0) return this;
        try {

            final Accessor accessor = new Accessor(field, this);
            final ObjectList subset = new ObjectList();

            for (int i = 0; i < size(); i++) {
                try {
                    final Object object = get(i);
                    final Object valueA = accessor.getValue(object);

                    final int result = ObjectList.compare(valueA, valueB);

                    if (result / condition > 0) {
                        subset.add(object);
                    }
                } catch (final Exception e) {
                }
            }
            return subset;
        } catch (final Exception e) {
            return new ObjectList();
        }
    }

    // private ObjectList compareAndCollect(String field, Object base, int
    // condition) {
    // Comparator comparator = getComparator(field);
    //
    // ObjectList subset = new ObjectList();
    // for (int i = 0; i < this.size(); i++) {
    // Object object = this.get(i);
    // int value = comparator.compare(object, base);
    // if (value / condition > 0) {
    // subset.add(object);
    // }
    // }
    // return subset;
    // }

    private Comparator getComparator(final String field) {
        return new FieldComparator(new Accessor(field, this));
    }

    private static class ReverseComparator implements Comparator {
        private final Comparator comparator;

        public ReverseComparator(final Comparator comparator) {
            this.comparator = comparator;
        }

        public int compare(final Object a, final Object b) {
            return -1 * comparator.compare(a, b);
        }
    }

    private static class FieldComparator implements Comparator {
        private final Accessor accessor;

        public FieldComparator(final Accessor accessor) {
            this.accessor = accessor;
        }

        public int compare(final Object objectA, final Object objectB) {
            try {
                final Object a = accessor.getValue((Object) objectA);
                final Object b = accessor.getValue((Object) objectB);
                return ObjectList.compare(a, b);
            } catch (final Exception e) {
                return 0;
            }
        }
    }

    public static class Accessor {

        private final Method method;

        public Accessor(final String field, final List list) {
            this.method = method(list, field);
        }

        private Method method(final List list, final String field) {
            try {
                final Object first = (Object) list.get(0);
                final StringBuilder sb = new StringBuilder(field);
                sb.setCharAt(0, Character.toUpperCase(sb.charAt(0)));
                return first.getClass().getMethod("get" + sb);
            } catch (final NoSuchMethodException e) {
            }

            return null;
        }

        public Object getValue(final Object Object) {
            try {
                return method.invoke(Object);
            } catch (final Exception e) {
                return null;
            }
        }

        public int intValue(final Object Object) throws java.lang.NumberFormatException {
            final Object value = getValue(Object);
            if (value instanceof Number) {
                final Number number = (Number) value;
                return number.intValue();
            }
            return new Integer(value.toString()).intValue();
        }

        public String stringValue(final Object Object) {
            final Object value = getValue(Object);
            return (value == null) ? null : value.toString();
        }

    }
}
