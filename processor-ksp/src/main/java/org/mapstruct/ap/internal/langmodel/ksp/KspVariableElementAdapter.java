/*
 * Copyright MapStruct Authors.
 *
 * Licensed under the Apache License version 2.0, available at http://www.apache.org/licenses/LICENSE-2.0
 */
package org.mapstruct.ap.internal.langmodel.ksp;

import com.google.devtools.ksp.symbol.KSPropertyDeclaration;
import com.google.devtools.ksp.symbol.KSTypeReference;

import javax.lang.model.element.ElementKind;
import javax.lang.model.element.ElementVisitor;
import javax.lang.model.element.VariableElement;
import javax.lang.model.type.TypeMirror;

/**
 * Adapter that wraps a KSP {@link KSPropertyDeclaration} as a javax {@link VariableElement}.
 */
final class KspVariableElementAdapter extends KspElementAdapter implements VariableElement {

    private final KSPropertyDeclaration property;

    KspVariableElementAdapter(KSPropertyDeclaration property, KspTypeAdapterFactory adapterFactory) {
        super( property, adapterFactory );
        this.property = property;
    }

    @Override
    public ElementKind getKind() {
        return ElementKind.FIELD;
    }

    @Override
    public TypeMirror asType() {
        KSTypeReference typeRef = property.getType();
        if ( typeRef == null ) {
            return new KspNoTypeAdapter( javax.lang.model.type.TypeKind.NONE );
        }
        return adapterFactory.typeMirror( typeRef.resolve() );
    }

    @Override
    public Object getConstantValue() {
        // Constant values for compile-time constants - simplified implementation
        return null;
    }

    @Override
    public <R, P> R accept(ElementVisitor<R, P> v, P p) {
        return v.visitVariable( this, p );
    }

    KSPropertyDeclaration unwrapProperty() {
        return property;
    }
}
