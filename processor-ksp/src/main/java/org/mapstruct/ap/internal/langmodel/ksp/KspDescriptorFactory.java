/*
 * Copyright MapStruct Authors.
 *
 * Licensed under the Apache License version 2.0, available at http://www.apache.org/licenses/LICENSE-2.0
 */
package org.mapstruct.ap.internal.langmodel.ksp;

import com.google.devtools.ksp.processing.Resolver;
import com.google.devtools.ksp.symbol.KSAnnotation;
import com.google.devtools.ksp.symbol.KSClassDeclaration;
import com.google.devtools.ksp.symbol.KSDeclaration;
import com.google.devtools.ksp.symbol.KSFunctionDeclaration;
import com.google.devtools.ksp.symbol.KSPropertyDeclaration;
import com.google.devtools.ksp.symbol.KSType;
import com.google.devtools.ksp.symbol.KSTypeArgument;
import com.google.devtools.ksp.symbol.KSValueParameter;

import org.mapstruct.ap.internal.langmodel.LangDescriptorFactory;
import org.mapstruct.ap.internal.langmodel.descriptor.AnnotationDescriptor;
import org.mapstruct.ap.internal.langmodel.descriptor.AnnotationValueDescriptor;
import org.mapstruct.ap.internal.langmodel.descriptor.ElementDescriptor;
import org.mapstruct.ap.internal.langmodel.descriptor.TypeDescriptor;
import org.mapstruct.ap.internal.langmodel.descriptor.TypeElementDescriptor;

import java.util.IdentityHashMap;
import java.util.Map;
import java.util.Objects;

/**
 * KSP implementation of {@link LangDescriptorFactory}.
 * Provides caching for descriptor instances to ensure identity-based lookups work correctly.
 */
final class KspDescriptorFactory implements LangDescriptorFactory {

    private final KspLangModelContext context;
    private final Resolver resolver;
    private final Map<KSType, KspTypeDescriptor> typeCache = new IdentityHashMap<>();
    private final Map<KSTypeArgument, KspTypeArgumentDescriptor> typeArgumentCache = new IdentityHashMap<>();
    private final Map<KSDeclaration, ElementDescriptor> elementCache = new IdentityHashMap<>();
    private final Map<KSAnnotation, KspAnnotationDescriptor> annotationCache = new IdentityHashMap<>();
    private final Map<String, KspPackageDescriptor> packageCache = new IdentityHashMap<>();

    KspDescriptorFactory(KspLangModelContext context, Resolver resolver) {
        this.context = Objects.requireNonNull( context, "context" );
        this.resolver = Objects.requireNonNull( resolver, "resolver" );
    }

    Resolver resolver() {
        return resolver;
    }

    KspLangModelContext context() {
        return context;
    }

    @Override
    public TypeDescriptor typeDescriptor(Object nativeType) {
        if ( nativeType == null ) {
            return null;
        }

        if ( nativeType instanceof KSType ) {
            KSType type = (KSType) nativeType;
            return typeCache.computeIfAbsent( type, t -> new KspTypeDescriptor( this, t ) );
        }

        if ( nativeType instanceof KSClassDeclaration ) {
            KSClassDeclaration decl = (KSClassDeclaration) nativeType;
            return typeDescriptor( decl.asStarProjectedType() );
        }

        throw unsupportedHandle( "type", nativeType );
    }

    /**
     * Creates a descriptor for a type argument (which may be a wildcard).
     */
    TypeDescriptor typeArgumentDescriptor(KSTypeArgument argument) {
        if ( argument == null ) {
            return null;
        }
        return typeArgumentCache.computeIfAbsent( argument, arg -> new KspTypeArgumentDescriptor( this, arg ) );
    }

    @Override
    public TypeElementDescriptor typeElementDescriptor(Object nativeTypeElement) {
        if ( nativeTypeElement == null ) {
            return null;
        }

        if ( nativeTypeElement instanceof KSClassDeclaration ) {
            KSClassDeclaration decl = (KSClassDeclaration) nativeTypeElement;
            ElementDescriptor cached = elementCache.get( decl );
            if ( cached instanceof TypeElementDescriptor ) {
                return (TypeElementDescriptor) cached;
            }
            KspTypeElementDescriptor descriptor = new KspTypeElementDescriptor( this, decl );
            elementCache.put( decl, descriptor );
            return descriptor;
        }

        throw unsupportedHandle( "type element", nativeTypeElement );
    }

    @Override
    public ElementDescriptor elementDescriptor(Object nativeElement) {
        if ( nativeElement == null ) {
            return null;
        }

        if ( nativeElement instanceof KSDeclaration ) {
            KSDeclaration decl = (KSDeclaration) nativeElement;
            ElementDescriptor cached = elementCache.get( decl );
            if ( cached != null ) {
                return cached;
            }

            ElementDescriptor descriptor = createElementDescriptor( decl );
            if ( descriptor != null ) {
                elementCache.put( decl, descriptor );
            }
            return descriptor;
        }

        if ( nativeElement instanceof KSValueParameter ) {
            // Parameters are not cached by declaration since they don't implement KSDeclaration
            return new KspParameterDescriptor( this, (KSValueParameter) nativeElement );
        }

        throw unsupportedHandle( "element", nativeElement );
    }

    @Override
    public AnnotationDescriptor annotationDescriptor(Object nativeAnnotation) {
        if ( nativeAnnotation == null ) {
            return null;
        }

        if ( nativeAnnotation instanceof KSAnnotation ) {
            KSAnnotation annotation = (KSAnnotation) nativeAnnotation;
            return annotationCache.computeIfAbsent( annotation, a -> new KspAnnotationDescriptor( this, a ) );
        }

        throw unsupportedHandle( "annotation", nativeAnnotation );
    }

    @Override
    public AnnotationValueDescriptor annotationValueDescriptor(Object nativeAnnotationValue) {
        if ( nativeAnnotationValue == null ) {
            return null;
        }

        // KSP doesn't have a separate annotation value type like javax.lang.model
        // Values are retrieved directly from KSValueArgument
        throw new UnsupportedOperationException(
            "Direct annotation value wrapping not supported in KSP. Use KspAnnotationDescriptor.elementValues() instead."
        );
    }

    /**
     * Creates a package descriptor for the given declaration.
     */
    KspPackageDescriptor packageDescriptor(KSDeclaration declaration) {
        String packageName = declaration != null && declaration.getPackageName() != null
            ? declaration.getPackageName().asString()
            : "";
        return packageCache.computeIfAbsent( packageName, KspPackageDescriptor::new );
    }

    private ElementDescriptor createElementDescriptor(KSDeclaration declaration) {
        if ( declaration instanceof KSClassDeclaration ) {
            return new KspTypeElementDescriptor( this, (KSClassDeclaration) declaration );
        }

        if ( declaration instanceof KSFunctionDeclaration ) {
            return new KspExecutableDescriptor( this, (KSFunctionDeclaration) declaration );
        }

        if ( declaration instanceof KSPropertyDeclaration ) {
            return new KspFieldDescriptor( this, (KSPropertyDeclaration) declaration );
        }

        // For other declaration types, return null
        return null;
    }

    private IllegalArgumentException unsupportedHandle(String kind, Object value) {
        String typeName = value == null ? "null" : value.getClass().getName();
        return new IllegalArgumentException( "Unsupported " + kind + " handle for KSP backend: " + typeName );
    }
}
