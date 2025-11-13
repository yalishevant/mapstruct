/*
 * Copyright MapStruct Authors.
 *
 * Licensed under the Apache License version 2.0, available at http://www.apache.org/licenses/LICENSE-2.0
 */
package org.mapstruct.ap.internal.langmodel.shared;

import java.util.Collections;
import java.util.SortedMap;
import java.util.TreeMap;

/**
 * Immutable snapshot capturing descriptor metadata for a collection of types.
 */
public final class DescriptorSnapshot {

    private final SortedMap<String, TypeSnapshot> types = new TreeMap<>();

    void addType(TypeSnapshot snapshot) {
        types.put( snapshot.qualifiedName(), snapshot );
    }

    public SortedMap<String, TypeSnapshot> types() {
        return Collections.unmodifiableSortedMap( types );
    }
}

