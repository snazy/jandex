/*
 * JBoss, Home of Professional Open Source.
 * Copyright 2014 Red Hat, Inc., and individual contributors
 * as indicated by the @author tags.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.jboss.jandex;

import java.io.BufferedInputStream;
import java.io.DataInputStream;
import java.io.InputStream;
import java.lang.reflect.Array;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

/**
 * Common utilities
 *
 * @author Jason T. Greene
 */
class Utils {
    static final byte[] INIT_METHOD_NAME = Utils.toUTF8("<init>");

    static final byte[] CLINIT_METHOD_NAME = Utils.toUTF8("<clinit>");

    static final Comparator<byte[]> BYTE_ARRAY_COMPARATOR = (arr1, arr2) -> {
        int l1 = arr1.length;
        int l2 = arr2.length;
        int l = Math.min(l1, l2);
        for (int i = 0; i < l; i++) {
            int b1 = arr1[i] & 0xff;
            int b2 = arr2[i] & 0xff;
            if (b1 != b2) {
                return b1 - b2;
            }
        }
        return l1 - l2;
    };

    static <E extends Comparable<E>> int compareNullSafe(E o1, E o2) {
        if (o1 != null && o2 != null) {
            return o1.compareTo(o2);
        }
        if (o1 == null && o2 == null) {
            return 0;
        }
        if (o1 == null) {
            return -1;
        }
        return 1;
    }

    static <E> int compareNullSafe(E o1, E o2, Comparator<E> comparator) {
        if (o1 != null && o2 != null) {
            return comparator.compare(o1, o2);
        }
        if (o1 == null && o2 == null) {
            return 0;
        }
        if (o1 == null) {
            return -1;
        }
        return 1;
    }

    // Replace with `Arrays.compare(O[], O)` (since Java 9)
    static <E extends Comparable<E>> int compareArrays(E[] arr1, E[] arr2) {
        if (arr1 == null && arr2 == null) {
            return 0;
        }
        if (arr1 == null) {
            return -1;
        }
        if (arr2 == null) {
            return 1;
        }
        int l1 = arr1.length;
        int l2 = arr2.length;
        int l = Math.min(l1, l2);
        for (int i = 0; i < l; i++) {
            int r = arr1[i].compareTo(arr2[i]);
            if (r != 0) {
                return r;
            }
        }
        return 0;
    }

    // Replace with `Arrays.compare(O[], O[], Comparator)` (since Java 9)
    static <E> int compareArrays(E[] arr1, E[] arr2, Comparator<E> comparator) {
        if (arr1 == null && arr2 == null) {
            return 0;
        }
        if (arr1 == null) {
            return -1;
        }
        if (arr2 == null) {
            return 1;
        }
        int l1 = arr1.length;
        int l2 = arr2.length;
        int l = Math.min(l1, l2);
        for (int i = 0; i < l; i++) {
            int r = comparator.compare(arr1[i], arr2[i]);
            if (r != 0) {
                return r;
            }
        }
        return 0;
    }

    static byte[] toUTF8(String string) {
        return string.getBytes(StandardCharsets.UTF_8);
    }

    static String fromUTF8(byte[] bytes) {
        return new String(bytes, StandardCharsets.UTF_8);
    }

    static <K, V> Map<K, V[]> unfold(Map<K, List<V>> map, Class<V> listElementType) {
        if (map.isEmpty()) {
            return Collections.emptyMap();
        }

        Map<K, V[]> result = new HashMap<>();
        map.forEach((key, value) -> {
            V[] array = (V[]) Array.newInstance(listElementType, value.size());
            result.put(key, value.toArray(array));
        });
        return result;
    }

    static <K, V> Map<K, V> minimize(Map<K, V> map) {
        if (map.isEmpty()) {
            return Collections.emptyMap();
        } else if (map.size() == 1) {
            Map.Entry<K, V> entry = map.entrySet().iterator().next();
            return Collections.singletonMap(entry.getKey(), entry.getValue());
        } else {
            return map;
        }
    }

    static <T> List<T> listOfCapacity(int capacity) {
        return capacity > 0 ? new ArrayList<>(capacity) : Collections.emptyList();
    }

    static final class ReusableBufferedDataInputStream extends DataInputStream {
        private ReusableBufferedInputStream reusableBuffered = null;

        ReusableBufferedDataInputStream() {
            super(null);
        }

        void setInputStream(InputStream in) {
            Objects.requireNonNull(in);
            // this is already buffered: let's use it directly
            if (in instanceof BufferedInputStream) {
                assert !(in instanceof ReusableBufferedInputStream);
                this.in = in;
            } else {
                if (this.in == null) {
                    if (reusableBuffered == null) {
                        reusableBuffered = new ReusableBufferedInputStream();
                    }
                    this.in = reusableBuffered;
                }
                reusableBuffered.setInputStream(in);
            }
        }

        @Override
        public void close() {
            if (in == reusableBuffered) {
                reusableBuffered.close();
            } else {
                in = null;
            }
        }

        @Override
        public boolean markSupported() {
            return false;
        }

        @Override
        @Deprecated
        public synchronized void mark(int readlimit) {
            throw new UnsupportedOperationException("mark/reset not supported");
        }

        @Override
        @Deprecated
        public synchronized void reset() {
            throw new UnsupportedOperationException("mark/reset not supported");
        }
    }

    private static final class ReusableBufferedInputStream extends BufferedInputStream {
        private ReusableBufferedInputStream() {
            super(null);
        }

        void setInputStream(InputStream in) {
            Objects.requireNonNull(in);
            if (pos != 0 && this.in != null) {
                throw new IllegalStateException("the stream cannot be reused");
            }
            this.in = in;
        }

        @Override
        public void close() {
            in = null;
            count = 0;
            pos = 0;
        }

        @Override
        public boolean markSupported() {
            return false;
        }

        @Override
        @Deprecated
        public synchronized void mark(int readlimit) {
            throw new UnsupportedOperationException("mark/reset not supported");
        }

        @Override
        @Deprecated
        public synchronized void reset() {
            throw new UnsupportedOperationException("mark/reset not supported");
        }
    }
}
