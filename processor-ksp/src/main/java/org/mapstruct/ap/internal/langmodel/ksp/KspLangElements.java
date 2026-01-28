/*
 * Copyright MapStruct Authors.
 *
 * Licensed under the Apache License version 2.0, available at http://www.apache.org/licenses/LICENSE-2.0
 */
package org.mapstruct.ap.internal.langmodel.ksp;

import com.google.devtools.ksp.processing.Resolver;
import com.google.devtools.ksp.symbol.ClassKind;
import com.google.devtools.ksp.symbol.FunctionKind;
import com.google.devtools.ksp.symbol.KSAnnotation;
import com.google.devtools.ksp.symbol.KSClassDeclaration;
import com.google.devtools.ksp.symbol.KSDeclaration;
import com.google.devtools.ksp.symbol.KSFunctionDeclaration;
import com.google.devtools.ksp.symbol.KSPropertyDeclaration;
import com.google.devtools.ksp.symbol.KSType;
import com.google.devtools.ksp.symbol.KSTypeReference;
import com.google.devtools.ksp.symbol.KSValueParameter;
import com.google.devtools.ksp.symbol.Modifier;

import org.mapstruct.ap.internal.langmodel.api.LangElements;
import org.mapstruct.ap.internal.langmodel.api.PackageDescriptor;
import org.mapstruct.ap.internal.langmodel.descriptor.AnnotationDescriptor;
import org.mapstruct.ap.internal.langmodel.descriptor.ElementDescriptor;
import org.mapstruct.ap.internal.langmodel.descriptor.ExecutableDescriptor;
import org.mapstruct.ap.internal.langmodel.descriptor.FieldDescriptor;
import org.mapstruct.ap.internal.langmodel.descriptor.RecordComponentDescriptor;
import org.mapstruct.ap.internal.langmodel.descriptor.TypeElementDescriptor;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Objects;
import java.util.Set;

/**
 * KSP implementation of {@link LangElements}.
 */
final class KspLangElements implements LangElements {

    private static final Set<String> OBJECT_METHODS = Set.of(
        "equals", "hashCode", "toString", "getClass", "notify", "notifyAll", "wait", "clone", "finalize"
    );

    private final KspLangModelContext context;
    private final KspDescriptorFactory factory;
    private final Resolver resolver;

    KspLangElements(KspLangModelContext context, KspDescriptorFactory factory, Resolver resolver) {
        this.context = Objects.requireNonNull( context, "context" );
        this.factory = Objects.requireNonNull( factory, "factory" );
        this.resolver = Objects.requireNonNull( resolver, "resolver" );
    }

    @Override
    public TypeElementDescriptor typeElement(String canonicalName) {
        if ( canonicalName == null || canonicalName.isEmpty() ) {
            return null;
        }

        KSClassDeclaration classDecl = resolver.getClassDeclarationByName(
            resolver.getKSNameFromString( canonicalName )
        );

        if ( classDecl == null ) {
            // Try Kotlin-style name mapping for Java primitives
            String kotlinName = mapJavaToKotlinType( canonicalName );
            if ( kotlinName != null && !kotlinName.equals( canonicalName ) ) {
                classDecl = resolver.getClassDeclarationByName(
                    resolver.getKSNameFromString( kotlinName )
                );
            }
        }

        return classDecl != null ? factory.typeElementDescriptor( classDecl ) : null;
    }

    @Override
    public PackageDescriptor packageOf(ElementDescriptor element) {
        if ( element == null ) {
            return new KspPackageDescriptor( "" );
        }

        Object unwrapped = element.unwrap();
        if ( unwrapped instanceof KSDeclaration ) {
            return factory.packageDescriptor( (KSDeclaration) unwrapped );
        }

        return new KspPackageDescriptor( "" );
    }

    @Override
    public boolean overrides(ExecutableDescriptor overrider, ExecutableDescriptor overridden,
                             TypeElementDescriptor type) {
        if ( overrider == null || overridden == null ) {
            return false;
        }

        Object overriderUnwrapped = overrider.unwrap();
        Object overriddenUnwrapped = overridden.unwrap();

        if ( !( overriderUnwrapped instanceof KSFunctionDeclaration ) ||
             !( overriddenUnwrapped instanceof KSFunctionDeclaration ) ) {
            return false;
        }

        KSFunctionDeclaration overriderFunc = (KSFunctionDeclaration) overriderUnwrapped;
        KSFunctionDeclaration overriddenFunc = (KSFunctionDeclaration) overriddenUnwrapped;

        // Check if overriderFunc overrides overriddenFunc
        KSDeclaration overridee = overriderFunc.findOverridee();
        if ( overridee instanceof KSFunctionDeclaration ) {
            if ( overridee.equals( overriddenFunc ) ) {
                return true;
            }
        }

        // Fallback: check by name and parameter types
        if ( !overriderFunc.getSimpleName().asString().equals( overriddenFunc.getSimpleName().asString() ) ) {
            return false;
        }

        List<KSValueParameter> overriderParams = overriderFunc.getParameters();
        List<KSValueParameter> overriddenParams = overriddenFunc.getParameters();

        if ( overriderParams.size() != overriddenParams.size() ) {
            return false;
        }

        for ( int i = 0; i < overriderParams.size(); i++ ) {
            KSTypeReference overriderTypeRef = overriderParams.get( i ).getType();
            KSTypeReference overriddenTypeRef = overriddenParams.get( i ).getType();

            if ( overriderTypeRef == null || overriddenTypeRef == null ) {
                return false;
            }

            KSType overriderType = overriderTypeRef.resolve();
            KSType overriddenType = overriddenTypeRef.resolve();

            // Compare erased types
            KSDeclaration overriderDecl = overriderType.getDeclaration();
            KSDeclaration overriddenDecl = overriddenType.getDeclaration();

            if ( overriderDecl == null || overriddenDecl == null ) {
                return false;
            }

            String overriderName = overriderDecl.getQualifiedName() != null
                ? overriderDecl.getQualifiedName().asString()
                : overriderDecl.getSimpleName().asString();
            String overriddenName = overriddenDecl.getQualifiedName() != null
                ? overriddenDecl.getQualifiedName().asString()
                : overriddenDecl.getSimpleName().asString();

            if ( !overriderName.equals( overriddenName ) ) {
                return false;
            }
        }

        return true;
    }

    @Override
    public List<AnnotationDescriptor> annotationMirrors(ElementDescriptor element) {
        if ( element == null ) {
            return Collections.emptyList();
        }

        Object unwrapped = element.unwrap();
        Iterable<KSAnnotation> annotations;

        if ( unwrapped instanceof KSDeclaration ) {
            annotations = KspSequenceUtils.toIterable( ( (KSDeclaration) unwrapped ).getAnnotations() );
        }
        else if ( unwrapped instanceof KSValueParameter ) {
            annotations = KspSequenceUtils.toIterable( ( (KSValueParameter) unwrapped ).getAnnotations() );
        }
        else {
            return Collections.emptyList();
        }

        List<AnnotationDescriptor> result = new ArrayList<>();
        for ( KSAnnotation annotation : annotations ) {
            AnnotationDescriptor descriptor = factory.annotationDescriptor( annotation );
            if ( descriptor != null ) {
                result.add( descriptor );
            }
        }

        return result.isEmpty() ? Collections.emptyList() : Collections.unmodifiableList( result );
    }

    @Override
    public List<ExecutableDescriptor> enclosedExecutables(TypeElementDescriptor type) {
        KSClassDeclaration classDecl = unwrapTypeElement( type );
        if ( classDecl == null ) {
            return Collections.emptyList();
        }

        Set<String> seenSignatures = new HashSet<>();
        List<ExecutableDescriptor> result = new ArrayList<>();

        // Collect methods from this class and all supertypes
        collectExecutables( classDecl, result, seenSignatures, new HashSet<>() );

        return result.isEmpty() ? Collections.emptyList() : Collections.unmodifiableList( result );
    }

    @Override
    public List<ExecutableDescriptor> constructors(TypeElementDescriptor type) {
        KSClassDeclaration classDecl = unwrapTypeElement( type );
        if ( classDecl == null ) {
            return Collections.emptyList();
        }

        List<ExecutableDescriptor> result = new ArrayList<>();

        // Primary constructor
        KSFunctionDeclaration primaryConstructor = classDecl.getPrimaryConstructor();
        if ( primaryConstructor != null ) {
            ExecutableDescriptor descriptor = (ExecutableDescriptor) factory.elementDescriptor( primaryConstructor );
            if ( descriptor != null ) {
                result.add( descriptor );
            }
        }

        // Secondary constructors
        for ( KSDeclaration declaration : KspSequenceUtils.toIterable( classDecl.getDeclarations() ) ) {
            if ( declaration instanceof KSFunctionDeclaration ) {
                KSFunctionDeclaration func = (KSFunctionDeclaration) declaration;
                if ( func.getFunctionKind() == FunctionKind.MEMBER &&
                     "<init>".equals( func.getSimpleName().asString() ) &&
                     func != primaryConstructor ) {
                    ExecutableDescriptor descriptor = (ExecutableDescriptor) factory.elementDescriptor( func );
                    if ( descriptor != null ) {
                        result.add( descriptor );
                    }
                }
            }
        }

        return result.isEmpty() ? Collections.emptyList() : Collections.unmodifiableList( result );
    }

    @Override
    public List<FieldDescriptor> enclosedFields(TypeElementDescriptor type) {
        KSClassDeclaration classDecl = unwrapTypeElement( type );
        if ( classDecl == null ) {
            return Collections.emptyList();
        }

        Set<String> seenNames = new HashSet<>();
        List<FieldDescriptor> result = new ArrayList<>();

        // Collect properties from this class and all supertypes
        collectFields( classDecl, result, seenNames, new HashSet<>() );

        return result.isEmpty() ? Collections.emptyList() : Collections.unmodifiableList( result );
    }

    @Override
    public List<RecordComponentDescriptor> recordComponents(TypeElementDescriptor type) {
        KSClassDeclaration classDecl = unwrapTypeElement( type );
        if ( classDecl == null ) {
            return Collections.emptyList();
        }

        // In Kotlin, data class primary constructor parameters are similar to record components
        if ( !classDecl.getModifiers().contains( Modifier.DATA ) ) {
            return Collections.emptyList();
        }

        KSFunctionDeclaration primaryConstructor = classDecl.getPrimaryConstructor();
        if ( primaryConstructor == null ) {
            return Collections.emptyList();
        }

        List<KSValueParameter> parameters = primaryConstructor.getParameters();
        if ( parameters.isEmpty() ) {
            return Collections.emptyList();
        }

        List<RecordComponentDescriptor> result = new ArrayList<>( parameters.size() );
        for ( KSValueParameter param : parameters ) {
            result.add( new KspRecordComponentDescriptor( factory, param ) );
        }

        return Collections.unmodifiableList( result );
    }

    private void collectExecutables(KSClassDeclaration classDecl,
                                    List<ExecutableDescriptor> result,
                                    Set<String> seenSignatures,
                                    Set<String> visitedTypes) {
        String qualifiedName = classDecl.getQualifiedName() != null
            ? classDecl.getQualifiedName().asString()
            : classDecl.getSimpleName().asString();

        if ( visitedTypes.contains( qualifiedName ) ) {
            return;
        }
        visitedTypes.add( qualifiedName );

        // Use getAllFunctions() to get all functions including inherited ones
        // This is important because getDeclarations() may not return abstract interface methods
        for ( KSFunctionDeclaration func : KspSequenceUtils.toIterable( classDecl.getAllFunctions() ) ) {
            // Skip constructors
            if ( "<init>".equals( func.getSimpleName().asString() ) ) {
                continue;
            }

            // Skip private methods
            if ( func.getModifiers().contains( Modifier.PRIVATE ) ) {
                continue;
            }

            // Skip Object methods
            if ( OBJECT_METHODS.contains( func.getSimpleName().asString() ) ) {
                continue;
            }

            String signature = computeMethodSignature( func );
            if ( seenSignatures.add( signature ) ) {
                ExecutableDescriptor descriptor = (ExecutableDescriptor) factory.elementDescriptor( func );
                if ( descriptor != null ) {
                    result.add( descriptor );
                }
            }
        }
    }

    private void collectFields(KSClassDeclaration classDecl,
                               List<FieldDescriptor> result,
                               Set<String> seenNames,
                               Set<String> visitedTypes) {
        String qualifiedName = classDecl.getQualifiedName() != null
            ? classDecl.getQualifiedName().asString()
            : classDecl.getSimpleName().asString();

        if ( visitedTypes.contains( qualifiedName ) ) {
            return;
        }
        visitedTypes.add( qualifiedName );

        // Collect properties from this class
        for ( KSDeclaration declaration : KspSequenceUtils.toIterable( classDecl.getDeclarations() ) ) {
            if ( declaration instanceof KSPropertyDeclaration ) {
                KSPropertyDeclaration property = (KSPropertyDeclaration) declaration;

                // Skip private properties
                if ( property.getModifiers().contains( Modifier.PRIVATE ) ) {
                    continue;
                }

                String name = property.getSimpleName().asString();
                if ( seenNames.add( name ) ) {
                    FieldDescriptor descriptor = (FieldDescriptor) factory.elementDescriptor( property );
                    if ( descriptor != null ) {
                        result.add( descriptor );
                    }
                }
            }
        }

        // Collect from supertypes
        for ( KSTypeReference superTypeRef : KspSequenceUtils.toIterable( classDecl.getSuperTypes() ) ) {
            KSType superType = superTypeRef.resolve();
            KSDeclaration superDecl = superType.getDeclaration();
            if ( superDecl instanceof KSClassDeclaration ) {
                collectFields( (KSClassDeclaration) superDecl, result, seenNames, visitedTypes );
            }
        }
    }

    private String computeMethodSignature(KSFunctionDeclaration func) {
        StringBuilder builder = new StringBuilder();
        builder.append( func.getSimpleName().asString() ).append( "(" );

        List<KSValueParameter> params = func.getParameters();
        for ( int i = 0; i < params.size(); i++ ) {
            if ( i > 0 ) {
                builder.append( "," );
            }
            KSTypeReference typeRef = params.get( i ).getType();
            if ( typeRef != null ) {
                KSType type = typeRef.resolve();
                KSDeclaration decl = type.getDeclaration();
                if ( decl != null && decl.getQualifiedName() != null ) {
                    builder.append( decl.getQualifiedName().asString() );
                }
                else {
                    builder.append( type.toString() );
                }
            }
        }

        builder.append( ")" );
        return builder.toString();
    }

    private KSClassDeclaration unwrapTypeElement(TypeElementDescriptor descriptor) {
        if ( descriptor == null ) {
            return null;
        }

        Object unwrapped = descriptor.unwrap();
        if ( unwrapped instanceof KSClassDeclaration ) {
            return (KSClassDeclaration) unwrapped;
        }

        return null;
    }

    private String mapJavaToKotlinType(String javaName) {
        switch ( javaName ) {
            case "java.lang.Object":
                return "kotlin.Any";
            case "java.lang.String":
                return "kotlin.String";
            case "java.lang.Integer":
            case "int":
                return "kotlin.Int";
            case "java.lang.Long":
            case "long":
                return "kotlin.Long";
            case "java.lang.Short":
            case "short":
                return "kotlin.Short";
            case "java.lang.Byte":
            case "byte":
                return "kotlin.Byte";
            case "java.lang.Float":
            case "float":
                return "kotlin.Float";
            case "java.lang.Double":
            case "double":
                return "kotlin.Double";
            case "java.lang.Boolean":
            case "boolean":
                return "kotlin.Boolean";
            case "java.lang.Character":
            case "char":
                return "kotlin.Char";
            case "void":
                return "kotlin.Unit";
            default:
                return javaName;
        }
    }
}
