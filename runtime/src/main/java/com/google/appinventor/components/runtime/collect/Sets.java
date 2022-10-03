// -*- mode: java; c-basic-offset: 2; -*-
// Copyright 2009-2011 Google, All Rights reserved
// Copyright 2011-2012 MIT, All rights reserved
// Released under the Apache License, Version 2.0
// http://www.apache.org/licenses/LICENSE-2.0

package com.google.appinventor.components.runtime.collect;

import java.util.*;

/**
 * Provides static methods for creating mutable {@code Set} instances easily and
 * other static methods for working with Sets.
 * <p>
 * Note: This was copied from the com.google.android.collect.Lists class
 *
 * @author markf@google.com (Mark Friedman)
 */
public class Sets {

    /**
     * Creates an empty {@code HashSet} instance.
     *
     * <p><b>Note:</b> if {@code E} is an {@link Enum} type, use {@link
     * EnumSet#noneOf} instead.
     *
     * <p><b>Note:</b> if you only need an <i>immutable</i> empty Set,
     * use {@link Collections#emptySet} instead.
     *
     * @return a newly-created, initially-empty {@code HashSet}
     */
    public static <K> HashSet<K> newHashSet() {
        return new HashSet<K>();
    }

    /**
     * Creates a {@code HashSet} instance containing the given elements.
     *
     * <p><b>Note:</b> due to a bug in javac 1.5.0_06, we cannot support the
     * following:
     *
     * <p>{@code Set<Base> set = Sets.newHashSet(sub1, sub2);}
     *
     * <p>where {@code sub1} and {@code sub2} are references to subtypes of {@code
     * Base}, not of {@code Base} itself. To get around this, you must use:
     *
     * <p>{@code Set<Base> set = Sets.<Base>newHashSet(sub1, sub2);}
     *
     * @param elements the elements that the set should contain
     * @return a newly-created {@code HashSet} containing those elements (minus
     * duplicates)
     */
    public static <E> HashSet<E> newHashSet(E... elements) {
        int capacity = elements.length * 4 / 3 + 1;
        HashSet<E> set = new HashSet<E>(capacity);
        Collections.addAll(set, elements);
        return set;
    }

    /**
     * Creates a {@code SortedSet} instance containing the given elements.
     *
     * @param elements the elements that the set should contain
     * @return a newly-created {@code SortedSet} containing those elements (minus
     * duplicates)
     */
    public static <E> SortedSet<E> newSortedSet(E... elements) {
        SortedSet<E> set = new TreeSet<E>();
        Collections.addAll(set, elements);
        return set;
    }
}