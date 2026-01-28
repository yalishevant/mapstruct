/*
 * Copyright MapStruct Authors.
 *
 * Licensed under the Apache License version 2.0, available at http://www.apache.org/licenses/LICENSE-2.0
 */
package org.mapstruct.ap.internal.langmodel.ksp;

import com.google.devtools.ksp.symbol.FunctionKind;
import com.google.devtools.ksp.symbol.KSFunctionDeclaration;
import com.google.devtools.ksp.symbol.KSType;
import com.google.devtools.ksp.symbol.KSTypeParameter;
import com.google.devtools.ksp.symbol.KSTypeReference;
import com.google.devtools.ksp.symbol.KSValueParameter;

import javax.lang.model.element.AnnotationValue;
import javax.lang.model.element.Element;
import javax.lang.model.element.ElementKind;
import javax.lang.model.element.ElementVisitor;
import javax.lang.model.element.ExecutableElement;
import javax.lang.model.element.TypeParameterElement;
import javax.lang.model.element.VariableElement;
import javax.lang.model.type.TypeMirror;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * Adapter that wraps a KSP {@link KSFunctionDeclaration} as a javax {@link ExecutableElement}.
 */
final class KspExecutableElementAdapter extends KspElementAdapter implements ExecutableElement {

    private final KSFunctionDeclaration function;

    KspExecutableElementAdapter(KSFunctionDeclaration function, KspTypeAdapterFactory adapterFactory) {
        super( function, adapterFactory );
        this.function = function;
    }

    @Override
    public ElementKind getKind() {
        FunctionKind kind = function.getFunctionKind();
        if ( "<init>".equals( function.getSimpleName().asString() ) ) {
            return ElementKind.CONSTRUCTOR;
        }
        return ElementKind.METHOD;
    }

    @Override
    public TypeMirror asType() {
        // Return the executable type
        return new KspExecutableTypeAdapter( function, adapterFactory );
    }

    @Override
    public List<? extends TypeParameterElement> getTypeParameters() {
        List<KSTypeParameter> typeParams = function.getTypeParameters();
        if ( typeParams.isEmpty() ) {
            return Collections.emptyList();
        }
        List<TypeParameterElement> result = new ArrayList<>( typeParams.size() );
        for ( KSTypeParameter param : typeParams ) {
            result.add( new KspTypeParameterElementAdapter( param, adapterFactory ) );
        }
        return result;
    }

    @Override
    public TypeMirror getReturnType() {
        KSTypeReference returnTypeRef = function.getReturnType();
        if ( returnTypeRef == null ) {
            return new KspNoTypeAdapter( javax.lang.model.type.TypeKind.VOID );
        }
        return adapterFactory.typeMirror( returnTypeRef.resolve() );
    }

    @Override
    public List<? extends VariableElement> getParameters() {
        List<KSValueParameter> params = function.getParameters();
        if ( params.isEmpty() ) {
            return Collections.emptyList();
        }
        List<VariableElement> result = new ArrayList<>( params.size() );
        for ( KSValueParameter param : params ) {
            result.add( new KspParameterElementAdapter( param, adapterFactory ) );
        }
        return result;
    }

    @Override
    public TypeMirror getReceiverType() {
        KSTypeReference receiverType = function.getExtensionReceiver();
        if ( receiverType == null ) {
            return new KspNoTypeAdapter( javax.lang.model.type.TypeKind.NONE );
        }
        return adapterFactory.typeMirror( receiverType.resolve() );
    }

    @Override
    public boolean isVarArgs() {
        List<KSValueParameter> params = function.getParameters();
        if ( params.isEmpty() ) {
            return false;
        }
        return params.get( params.size() - 1 ).isVararg();
    }

    @Override
    public boolean isDefault() {
        // In Kotlin, interface methods can have default implementations (non-abstract)
        return !function.isAbstract();
    }

    @Override
    public List<? extends TypeMirror> getThrownTypes() {
        // Kotlin doesn't have checked exceptions
        return Collections.emptyList();
    }

    @Override
    public AnnotationValue getDefaultValue() {
        // Default values for annotation methods - not commonly used in mappers
        return null;
    }

    @Override
    public <R, P> R accept(ElementVisitor<R, P> v, P p) {
        return v.visitExecutable( this, p );
    }

    KSFunctionDeclaration unwrapFunction() {
        return function;
    }
}
