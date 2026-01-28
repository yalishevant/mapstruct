/*
 * Copyright MapStruct Authors.
 *
 * Licensed under the Apache License version 2.0, available at http://www.apache.org/licenses/LICENSE-2.0
 */
package org.mapstruct.ap.internal.langmodel.ksp;

import javax.lang.model.element.Element;
import javax.lang.model.element.TypeElement;
import javax.lang.model.type.ArrayType;
import javax.lang.model.type.DeclaredType;
import javax.lang.model.type.ExecutableType;
import javax.lang.model.type.NoType;
import javax.lang.model.type.NullType;
import javax.lang.model.type.PrimitiveType;
import javax.lang.model.type.TypeKind;
import javax.lang.model.type.TypeMirror;
import javax.lang.model.type.WildcardType;
import javax.lang.model.util.Types;
import java.util.List;

/**
 * Stub implementation of {@link Types} for KSP.
 * <p>
 * All methods throw {@link UnsupportedOperationException} as KSP does not use
 * the javax.lang.model API. This implementation exists only to satisfy the
 * {@link org.mapstruct.ap.spi.MapStructProcessingEnvironment} contract for SPIs
 * that may not require actual type utilities.
 */
final class KspStubTypes implements Types {

    private static final String NOT_SUPPORTED_MSG =
        "javax.lang.model Types API is not supported in KSP. Use KSP's native API instead.";

    @Override
    public Element asElement(TypeMirror t) {
        throw new UnsupportedOperationException( NOT_SUPPORTED_MSG );
    }

    @Override
    public boolean isSameType(TypeMirror t1, TypeMirror t2) {
        throw new UnsupportedOperationException( NOT_SUPPORTED_MSG );
    }

    @Override
    public boolean isSubtype(TypeMirror t1, TypeMirror t2) {
        throw new UnsupportedOperationException( NOT_SUPPORTED_MSG );
    }

    @Override
    public boolean isAssignable(TypeMirror t1, TypeMirror t2) {
        throw new UnsupportedOperationException( NOT_SUPPORTED_MSG );
    }

    @Override
    public boolean contains(TypeMirror t1, TypeMirror t2) {
        throw new UnsupportedOperationException( NOT_SUPPORTED_MSG );
    }

    @Override
    public boolean isSubsignature(ExecutableType m1, ExecutableType m2) {
        throw new UnsupportedOperationException( NOT_SUPPORTED_MSG );
    }

    @Override
    public List<? extends TypeMirror> directSupertypes(TypeMirror t) {
        throw new UnsupportedOperationException( NOT_SUPPORTED_MSG );
    }

    @Override
    public TypeMirror erasure(TypeMirror t) {
        throw new UnsupportedOperationException( NOT_SUPPORTED_MSG );
    }

    @Override
    public TypeElement boxedClass(PrimitiveType p) {
        throw new UnsupportedOperationException( NOT_SUPPORTED_MSG );
    }

    @Override
    public PrimitiveType unboxedType(TypeMirror t) {
        throw new UnsupportedOperationException( NOT_SUPPORTED_MSG );
    }

    @Override
    public TypeMirror capture(TypeMirror t) {
        throw new UnsupportedOperationException( NOT_SUPPORTED_MSG );
    }

    @Override
    public PrimitiveType getPrimitiveType(TypeKind kind) {
        throw new UnsupportedOperationException( NOT_SUPPORTED_MSG );
    }

    @Override
    public NullType getNullType() {
        throw new UnsupportedOperationException( NOT_SUPPORTED_MSG );
    }

    @Override
    public NoType getNoType(TypeKind kind) {
        throw new UnsupportedOperationException( NOT_SUPPORTED_MSG );
    }

    @Override
    public ArrayType getArrayType(TypeMirror componentType) {
        throw new UnsupportedOperationException( NOT_SUPPORTED_MSG );
    }

    @Override
    public WildcardType getWildcardType(TypeMirror extendsBound, TypeMirror superBound) {
        throw new UnsupportedOperationException( NOT_SUPPORTED_MSG );
    }

    @Override
    public DeclaredType getDeclaredType(TypeElement typeElem, TypeMirror... typeArgs) {
        throw new UnsupportedOperationException( NOT_SUPPORTED_MSG );
    }

    @Override
    public DeclaredType getDeclaredType(DeclaredType containing, TypeElement typeElem, TypeMirror... typeArgs) {
        throw new UnsupportedOperationException( NOT_SUPPORTED_MSG );
    }

    @Override
    public TypeMirror asMemberOf(DeclaredType containing, Element element) {
        throw new UnsupportedOperationException( NOT_SUPPORTED_MSG );
    }
}
