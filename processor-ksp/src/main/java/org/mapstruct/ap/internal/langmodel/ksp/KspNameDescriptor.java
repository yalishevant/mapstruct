/*
 * Copyright MapStruct Authors.
 *
 * Licensed under the Apache License version 2.0, available at http://www.apache.org/licenses/LICENSE-2.0
 */
package org.mapstruct.ap.internal.langmodel.ksp;

import com.google.devtools.ksp.symbol.KSName;

import org.mapstruct.ap.internal.langmodel.descriptor.NameDescriptor;

import java.util.Objects;

/**
 * KSP implementation of {@link NameDescriptor}.
 */
final class KspNameDescriptor implements NameDescriptor {

    private final String content;

    KspNameDescriptor(KSName name) {
        this.content = name != null ? name.asString() : "";
    }

    KspNameDescriptor(String name) {
        this.content = name != null ? name : "";
    }

    @Override
    public String content() {
        return content;
    }

    @Override
    public boolean contentEquals(CharSequence cs) {
        return content.contentEquals( cs );
    }

    @Override
    public boolean equals(Object obj) {
        if ( this == obj ) {
            return true;
        }
        if ( !( obj instanceof NameDescriptor ) ) {
            return false;
        }
        NameDescriptor other = (NameDescriptor) obj;
        return content.equals( other.content() );
    }

    @Override
    public int hashCode() {
        return content.hashCode();
    }

    @Override
    public String toString() {
        return content;
    }
}
