/*
 * Copyright MapStruct Authors.
 *
 * Licensed under the Apache License version 2.0, available at http://www.apache.org/licenses/LICENSE-2.0
 */
package org.mapstruct.ap.internal.langmodel.ksp;

import com.google.devtools.ksp.symbol.KSDeclaration;

import org.mapstruct.ap.internal.langmodel.api.PackageDescriptor;
import org.mapstruct.ap.internal.langmodel.descriptor.ElementDescriptor;
import org.mapstruct.ap.internal.langmodel.descriptor.LangElementKind;
import org.mapstruct.ap.internal.langmodel.descriptor.LangModifier;
import org.mapstruct.ap.internal.langmodel.descriptor.NameDescriptor;
import org.mapstruct.ap.internal.langmodel.descriptor.TypeDescriptor;

import java.util.Collections;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;

/**
 * KSP implementation of {@link PackageDescriptor}.
 */
final class KspPackageDescriptor implements PackageDescriptor {

    private final String packageName;
    private final String id;

    KspPackageDescriptor(String packageName) {
        this.packageName = packageName != null ? packageName : "";
        this.id = "package:" + this.packageName;
    }

    KspPackageDescriptor(KSDeclaration declaration) {
        this( declaration != null && declaration.getPackageName() != null
            ? declaration.getPackageName().asString()
            : "" );
    }

    @Override
    public String id() {
        return id;
    }

    @Override
    public LangElementKind kind() {
        return LangElementKind.PACKAGE;
    }

    @Override
    public NameDescriptor simpleName() {
        // Simple name is the last segment of the package
        int lastDot = packageName.lastIndexOf( '.' );
        String simple = lastDot >= 0 ? packageName.substring( lastDot + 1 ) : packageName;
        return new KspNameDescriptor( simple );
    }

    @Override
    public Optional<ElementDescriptor> enclosingElement() {
        return Optional.empty();
    }

    @Override
    public Set<LangModifier> modifiers() {
        return Collections.emptySet();
    }

    @Override
    public TypeDescriptor asType() {
        return null;
    }

    @Override
    public Object unwrap() {
        return packageName;
    }

    @Override
    public String qualifiedName() {
        return packageName;
    }

    public boolean isUnnamed() {
        return packageName.isEmpty();
    }

    @Override
    public int compareTo(ElementDescriptor other) {
        return simpleName().content().compareTo( other.simpleName().content() );
    }

    @Override
    public boolean equals(Object obj) {
        if ( this == obj ) {
            return true;
        }
        if ( !( obj instanceof KspPackageDescriptor ) ) {
            return false;
        }
        KspPackageDescriptor other = (KspPackageDescriptor) obj;
        return Objects.equals( packageName, other.packageName );
    }

    @Override
    public int hashCode() {
        return packageName.hashCode();
    }

    @Override
    public String toString() {
        return "KspPackageDescriptor[" + packageName + "]";
    }
}
