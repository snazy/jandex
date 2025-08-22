/*
 * JBoss, Home of Professional Open Source.
 * Copyright 2021 Red Hat, Inc., and individual contributors
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

import static org.jboss.jandex.Utils.BYTE_ARRAY_COMPARATOR;

import java.util.Arrays;
import java.util.Comparator;
import java.util.List;

/**
 * The shared internal representation for RecordComponentInfo objects.
 */
final class RecordComponentInternal implements Comparable<RecordComponentInternal> {
    static final RecordComponentInternal[] EMPTY_ARRAY = new RecordComponentInternal[0];
    private final byte[] name;
    private Type type;
    private AnnotationInstance[] annotations;

    @Override
    public int compareTo(RecordComponentInternal o) {
        int r = BYTE_ARRAY_COMPARATOR.compare(name, o.name);
        if (r != 0) {
            return r;
        }
        r = Type.TYPE_WRITE_COMPARATOR.compare(type, o.type);
        if (r != 0) {
            return r;
        }

        int l1 = annotations == null ? 0 : annotations.length;
        int l2 = o.annotations == null ? 0 : o.annotations.length;
        int l = Math.min(l1, l2);
        for (int i = 0; i < l; i++) {
            r = annotations[i].compareTo(o.annotations[i]);
            if (r != 0) {
                return r;
            }
        }
        return l1 - l2;
    }

    static final NameComparator NAME_COMPARATOR = new NameComparator();

    static class NameComparator implements Comparator<RecordComponentInternal> {
        public int compare(RecordComponentInternal instance, RecordComponentInternal instance2) {
            return BYTE_ARRAY_COMPARATOR.compare(instance.name, instance2.name);
        }
    }

    RecordComponentInternal(byte[] name, Type type) {
        this(name, type, AnnotationInstance.EMPTY_ARRAY);
    }

    RecordComponentInternal(byte[] name, Type type, AnnotationInstance[] annotations) {
        this.name = name;
        this.type = type;
        this.annotations = annotations;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }

        RecordComponentInternal that = (RecordComponentInternal) o;

        if (!Arrays.equals(annotations, that.annotations)) {
            return false;
        }
        if (!Arrays.equals(name, that.name)) {
            return false;
        }
        if (!type.equals(that.type)) {
            return false;
        }

        return true;
    }

    @Override
    public int hashCode() {
        int result = Arrays.hashCode(name);
        result = 31 * result + type.hashCode();
        result = 31 * result + Arrays.hashCode(annotations);
        return result;
    }

    boolean internEquals(Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }

        RecordComponentInternal that = (RecordComponentInternal) o;

        if (!Arrays.equals(annotations, that.annotations)) {
            return false;
        }
        if (!Arrays.equals(name, that.name)) {
            return false;
        }
        if (!type.internEquals(that.type)) {
            return false;
        }

        return true;
    }

    int internHashCode() {
        int result = Arrays.hashCode(name);
        result = 31 * result + type.internHashCode();
        result = 31 * result + Arrays.hashCode(annotations);
        return result;
    }

    final String name() {
        return Utils.fromUTF8(name);
    }

    final byte[] nameBytes() {
        return name;
    }

    final Type type() {
        return type;
    }

    final List<AnnotationInstance> annotations() {
        return new ImmutableArrayList<>(annotations);
    }

    final AnnotationInstance[] annotationArray() {
        return annotations;
    }

    final AnnotationInstance annotation(DotName name) {
        return AnnotationInstance.binarySearch(annotations, name);
    }

    final boolean hasAnnotation(DotName name) {
        return annotation(name) != null;
    }

    @Override
    public String toString() {
        return type.toString(true) + " " + name();
    }

    public String toString(ClassInfo clazz) {
        return type.toString(true) + " " + clazz.name() + "." + name();
    }

    void setType(Type type) {
        this.type = type;
    }

    void setAnnotations(List<AnnotationInstance> annotations) {
        if (annotations.size() > 0) {
            this.annotations = annotations.toArray(new AnnotationInstance[annotations.size()]);
            Arrays.sort(this.annotations, AnnotationInstance.NAME_COMPARATOR);
        }
    }
}
