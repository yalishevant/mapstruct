/*
 * Copyright MapStruct Authors.
 *
 * Licensed under the Apache License version 2.0, available at http://www.apache.org/licenses/LICENSE-2.0
 */
package org.mapstruct.ap.internal.langmodel.ksp;

import com.google.devtools.ksp.symbol.KSFunctionDeclaration;
import com.google.devtools.ksp.symbol.KSType;
import com.google.devtools.ksp.symbol.KSTypeReference;
import com.google.devtools.ksp.symbol.KSValueParameter;

import org.mapstruct.ap.internal.langmodel.ExecutableSignature;
import org.mapstruct.ap.internal.langmodel.descriptor.TypeDescriptor;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Objects;

/**
 * KSP implementation of {@link ExecutableSignature}.
 */
final class KspExecutableSignature implements ExecutableSignature {

    private final List<TypeDescriptor> parameterTypes;
    private final TypeDescriptor returnType;
    private final List<TypeDescriptor> thrownTypes;

    private KspExecutableSignature(List<TypeDescriptor> parameterTypes,
                                   TypeDescriptor returnType,
                                   List<TypeDescriptor> thrownTypes) {
        this.parameterTypes = parameterTypes;
        this.returnType = returnType;
        this.thrownTypes = thrownTypes;
    }

    /**
     * Creates an executable signature from a KSP function declaration.
     *
     * @param factory   descriptor factory
     * @param function  KSP function declaration
     *
     * @return executable signature
     */
    static KspExecutableSignature fromFunction(KspDescriptorFactory factory, KSFunctionDeclaration function) {
        Objects.requireNonNull( factory, "factory" );
        Objects.requireNonNull( function, "function" );

        // Parameter types
        List<KSValueParameter> params = function.getParameters();
        List<TypeDescriptor> parameterTypes;
        if ( params.isEmpty() ) {
            parameterTypes = Collections.emptyList();
        }
        else {
            parameterTypes = new ArrayList<>( params.size() );
            for ( KSValueParameter param : params ) {
                KSTypeReference typeRef = param.getType();
                if ( typeRef != null ) {
                    KSType type = typeRef.resolve();
                    TypeDescriptor descriptor = factory.typeDescriptor( type );
                    if ( descriptor != null ) {
                        parameterTypes.add( descriptor );
                    }
                }
            }
            parameterTypes = Collections.unmodifiableList( parameterTypes );
        }

        // Return type
        TypeDescriptor returnType = null;
        KSTypeReference returnTypeRef = function.getReturnType();
        if ( returnTypeRef != null ) {
            returnType = factory.typeDescriptor( returnTypeRef.resolve() );
        }

        // Thrown types - Kotlin doesn't have checked exceptions
        // Could parse @Throws annotation if needed
        List<TypeDescriptor> thrownTypes = Collections.emptyList();

        return new KspExecutableSignature( parameterTypes, returnType, thrownTypes );
    }

    /**
     * Creates an executable signature from resolved types (used when resolving as member of a containing type).
     *
     * @param parameterTypes resolved parameter types
     * @param returnType     resolved return type
     * @param thrownTypes    resolved thrown types
     *
     * @return executable signature
     */
    static KspExecutableSignature of(List<TypeDescriptor> parameterTypes,
                                     TypeDescriptor returnType,
                                     List<TypeDescriptor> thrownTypes) {
        return new KspExecutableSignature(
            parameterTypes == null ? Collections.emptyList() : Collections.unmodifiableList( parameterTypes ),
            returnType,
            thrownTypes == null ? Collections.emptyList() : Collections.unmodifiableList( thrownTypes )
        );
    }

    @Override
    public List<TypeDescriptor> parameterTypes() {
        return parameterTypes;
    }

    @Override
    public TypeDescriptor returnType() {
        return returnType;
    }

    @Override
    public List<TypeDescriptor> thrownTypes() {
        return thrownTypes;
    }
}
