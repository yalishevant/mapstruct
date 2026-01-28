/*
 * Copyright MapStruct Authors.
 *
 * Licensed under the Apache License version 2.0, available at http://www.apache.org/licenses/LICENSE-2.0
 */
package org.mapstruct.ap.internal.langmodel.ksp;

import com.google.devtools.ksp.symbol.KSFunctionDeclaration;
import com.google.devtools.ksp.symbol.KSTypeReference;
import com.google.devtools.ksp.symbol.KSValueParameter;

import javax.lang.model.element.AnnotationMirror;
import javax.lang.model.type.ExecutableType;
import javax.lang.model.type.TypeKind;
import javax.lang.model.type.TypeMirror;
import javax.lang.model.type.TypeVariable;
import javax.lang.model.type.TypeVisitor;
import java.lang.annotation.Annotation;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * Adapter that wraps a KSP {@link KSFunctionDeclaration} as a javax {@link ExecutableType}.
 */
final class KspExecutableTypeAdapter implements ExecutableType {

    private final KSFunctionDeclaration function;
    private final KspTypeAdapterFactory adapterFactory;

    KspExecutableTypeAdapter(KSFunctionDeclaration function, KspTypeAdapterFactory adapterFactory) {
        this.function = function;
        this.adapterFactory = adapterFactory;
    }

    @Override
    public TypeKind getKind() {
        return TypeKind.EXECUTABLE;
    }

    @Override
    public List<? extends TypeVariable> getTypeVariables() {
        return Collections.emptyList(); // Simplified
    }

    @Override
    public TypeMirror getReturnType() {
        KSTypeReference returnTypeRef = function.getReturnType();
        if ( returnTypeRef == null ) {
            return new KspNoTypeAdapter( TypeKind.VOID );
        }
        return adapterFactory.typeMirror( returnTypeRef.resolve() );
    }

    @Override
    public List<? extends TypeMirror> getParameterTypes() {
        List<KSValueParameter> params = function.getParameters();
        if ( params.isEmpty() ) {
            return Collections.emptyList();
        }
        List<TypeMirror> result = new ArrayList<>( params.size() );
        for ( KSValueParameter param : params ) {
            KSTypeReference typeRef = param.getType();
            if ( typeRef != null ) {
                result.add( adapterFactory.typeMirror( typeRef.resolve() ) );
            }
        }
        return result;
    }

    @Override
    public TypeMirror getReceiverType() {
        KSTypeReference receiverType = function.getExtensionReceiver();
        if ( receiverType == null ) {
            return new KspNoTypeAdapter( TypeKind.NONE );
        }
        return adapterFactory.typeMirror( receiverType.resolve() );
    }

    @Override
    public List<? extends TypeMirror> getThrownTypes() {
        return Collections.emptyList();
    }

    @Override
    public <R, P> R accept(TypeVisitor<R, P> v, P p) {
        return v.visitExecutable( this, p );
    }

    @Override
    public List<? extends AnnotationMirror> getAnnotationMirrors() {
        return Collections.emptyList();
    }

    @Override
    public <A extends Annotation> A getAnnotation(Class<A> annotationType) {
        return null;
    }

    @Override
    @SuppressWarnings( "unchecked" )
    public <A extends Annotation> A[] getAnnotationsByType(Class<A> annotationType) {
        return (A[]) java.lang.reflect.Array.newInstance( annotationType, 0 );
    }
}
