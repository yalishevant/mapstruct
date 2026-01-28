/*
 * Copyright MapStruct Authors.
 *
 * Licensed under the Apache License version 2.0, available at http://www.apache.org/licenses/LICENSE-2.0
 */
package org.mapstruct.ap.internal.langmodel.ksp;

import com.google.devtools.ksp.symbol.KSClassDeclaration;
import com.google.devtools.ksp.symbol.KSDeclaration;
import com.google.devtools.ksp.symbol.KSType;
import com.google.devtools.ksp.symbol.KSTypeArgument;

import javax.lang.model.element.Element;
import javax.lang.model.type.DeclaredType;
import javax.lang.model.type.TypeKind;
import javax.lang.model.type.TypeMirror;
import javax.lang.model.type.TypeVisitor;
import java.util.ArrayList;
import java.util.List;

/**
 * Adapter that wraps a KSP {@link KSType} as a javax {@link DeclaredType}.
 */
final class KspDeclaredTypeAdapter extends KspTypeMirrorAdapter implements DeclaredType {

    KspDeclaredTypeAdapter(KSType type, KspTypeAdapterFactory adapterFactory) {
        super( type, adapterFactory );
    }

    @Override
    public Element asElement() {
        KSDeclaration declaration = type.getDeclaration();
        if ( declaration instanceof KSClassDeclaration ) {
            return adapterFactory.typeElement( (KSClassDeclaration) declaration );
        }
        return adapterFactory.element( declaration );
    }

    @Override
    public TypeMirror getEnclosingType() {
        // Simplified - return NONE type
        return new KspNoTypeAdapter( TypeKind.NONE );
    }

    @Override
    public List<? extends TypeMirror> getTypeArguments() {
        List<KSTypeArgument> args = type.getArguments();
        List<TypeMirror> result = new ArrayList<>( args.size() );
        for ( KSTypeArgument arg : args ) {
            KSType argType = arg.getType() != null ? arg.getType().resolve() : null;
            if ( argType != null ) {
                result.add( adapterFactory.typeMirror( argType ) );
            }
        }
        return result;
    }

    @Override
    public TypeKind getKind() {
        return TypeKind.DECLARED;
    }

    @Override
    public <R, P> R accept(TypeVisitor<R, P> v, P p) {
        return v.visitDeclared( this, p );
    }
}
