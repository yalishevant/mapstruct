/*
 * Copyright MapStruct Authors.
 *
 * Licensed under the Apache License version 2.0, available at http://www.apache.org/licenses/LICENSE-2.0
 */
package org.mapstruct.ap.internal.langmodel.ksp;

import com.google.devtools.ksp.symbol.KSClassDeclaration;
import com.google.devtools.ksp.symbol.KSFunctionDeclaration;
import com.google.devtools.ksp.symbol.KSType;
import com.google.devtools.ksp.symbol.KSTypeArgument;
import com.google.devtools.ksp.symbol.KSTypeParameter;
import com.google.devtools.ksp.symbol.KSTypeReference;
import com.google.devtools.ksp.symbol.KSValueParameter;

import org.mapstruct.ap.internal.langmodel.ExecutableSignature;
import org.mapstruct.ap.internal.langmodel.TypeIntrospector;
import org.mapstruct.ap.internal.langmodel.api.PackageDescriptor;
import org.mapstruct.ap.internal.langmodel.descriptor.ElementDescriptor;
import org.mapstruct.ap.internal.langmodel.descriptor.ExecutableDescriptor;
import org.mapstruct.ap.internal.langmodel.descriptor.FieldDescriptor;
import org.mapstruct.ap.internal.langmodel.descriptor.LangElementKind;
import org.mapstruct.ap.internal.langmodel.descriptor.LangModifier;
import org.mapstruct.ap.internal.langmodel.descriptor.LangTypeKind;
import org.mapstruct.ap.internal.langmodel.descriptor.RecordComponentDescriptor;
import org.mapstruct.ap.internal.langmodel.descriptor.TypeDescriptor;
import org.mapstruct.ap.internal.langmodel.descriptor.TypeElementDescriptor;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.HashMap;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;

/**
 * KSP implementation of {@link TypeIntrospector}.
 */
final class KspTypeIntrospector implements TypeIntrospector {

    private final KspLangModelContext context;
    private final KspLangTypes types;
    private final KspLangElements elements;

    KspTypeIntrospector(KspLangModelContext context, KspLangTypes types, KspLangElements elements) {
        this.context = Objects.requireNonNull( context, "context" );
        this.types = Objects.requireNonNull( types, "types" );
        this.elements = Objects.requireNonNull( elements, "elements" );
    }

    @Override
    public Metadata describe(TypeDescriptor descriptor) {
        Objects.requireNonNull( descriptor, "descriptor" );

        TypeElementDescriptor typeElement = descriptor.typeElement().orElse( null );
        TypeDescriptor componentType = descriptor.componentType().orElse( null );
        TypeDescriptor baseComponentType = baseComponentType( descriptor );

        boolean enumType = descriptor.isEnum();
        boolean interfaceType = descriptor.isInterface();
        boolean recordType = typeElement != null && typeElement.isRecord();
        boolean sealedType = typeElement != null && typeElement.isSealed();
        List<TypeDescriptor> permittedSubclasses =
            typeElement != null ? typeElement.permittedSubclasses() : Collections.emptyList();
        List<String> enumConstants = enumType ? enumConstants( typeElement ) : Collections.emptyList();

        NameResult names = computeNames( descriptor, typeElement, baseComponentType );
        TypeDescriptor topLevel = resolveTopLevelType( descriptor, typeElement, baseComponentType );
        Metadata.Names metadataNames = Metadata.Names.of(
            names.simpleName,
            names.qualifiedName,
            names.packageName
        );
        Metadata.TypeFlags metadataFlags = Metadata.TypeFlags.of(
            enumType,
            interfaceType,
            recordType,
            sealedType
        );

        return new Metadata(
            descriptor,
            typeElement,
            componentType,
            baseComponentType,
            metadataNames,
            metadataFlags,
            permittedSubclasses,
            enumConstants,
            topLevel
        );
    }

    @Override
    public ExecutableSignature resolveExecutable(TypeDescriptor containingType, ExecutableDescriptor executable) {
        if ( containingType == null || executable == null ) {
            return null;
        }

        Object containingHandle = containingType.unwrap();
        Object executableHandle = executable.unwrap();

        if ( !( containingHandle instanceof KSType ) || !( executableHandle instanceof KSFunctionDeclaration ) ) {
            return null;
        }

        KSType containingKsType = (KSType) containingHandle;
        KSFunctionDeclaration function = (KSFunctionDeclaration) executableHandle;

        // Resolve type parameters from the containing type
        Map<String, KSType> typeSubstitutions = buildTypeSubstitutions( containingKsType );

        // Resolve parameter types
        List<KSValueParameter> params = function.getParameters();
        List<TypeDescriptor> resolvedParamTypes;
        if ( params.isEmpty() ) {
            resolvedParamTypes = Collections.emptyList();
        }
        else {
            resolvedParamTypes = new ArrayList<>( params.size() );
            for ( KSValueParameter param : params ) {
                KSTypeReference typeRef = param.getType();
                if ( typeRef != null ) {
                    KSType resolvedType = resolveType( typeRef.resolve(), typeSubstitutions );
                    TypeDescriptor descriptor = context.descriptors().typeDescriptor( resolvedType );
                    if ( descriptor != null ) {
                        resolvedParamTypes.add( descriptor );
                    }
                }
            }
        }

        // Resolve return type
        TypeDescriptor resolvedReturnType = null;
        KSTypeReference returnTypeRef = function.getReturnType();
        if ( returnTypeRef != null ) {
            KSType resolvedType = resolveType( returnTypeRef.resolve(), typeSubstitutions );
            resolvedReturnType = context.descriptors().typeDescriptor( resolvedType );
        }

        return KspExecutableSignature.of( resolvedParamTypes, resolvedReturnType, Collections.emptyList() );
    }

    @Override
    public ExecutableSignature executable(ExecutableDescriptor executable) {
        if ( executable == null ) {
            return null;
        }

        Object executableHandle = executable.unwrap();
        if ( !( executableHandle instanceof KSFunctionDeclaration ) ) {
            return null;
        }

        return KspExecutableSignature.fromFunction(
            (KspDescriptorFactory) context.descriptors(),
            (KSFunctionDeclaration) executableHandle
        );
    }

    @Override
    public TypeDescriptor asMemberOf(TypeDescriptor containingType, ElementDescriptor member) {
        return types.asMemberOf( containingType, member );
    }

    @Override
    public List<ExecutableDescriptor> enclosedExecutables(TypeDescriptor descriptor) {
        TypeElementDescriptor element = descriptor.typeElement().orElse( null );
        if ( element == null ) {
            return Collections.emptyList();
        }
        return elements.enclosedExecutables( element );
    }

    @Override
    public List<ExecutableDescriptor> constructors(TypeDescriptor descriptor) {
        TypeElementDescriptor element = descriptor.typeElement().orElse( null );
        if ( element == null ) {
            return Collections.emptyList();
        }
        return elements.constructors( element );
    }

    @Override
    public List<FieldDescriptor> enclosedFields(TypeDescriptor descriptor) {
        TypeElementDescriptor element = descriptor.typeElement().orElse( null );
        if ( element == null ) {
            return Collections.emptyList();
        }
        return elements.enclosedFields( element );
    }

    @Override
    public List<RecordComponentDescriptor> recordComponents(TypeDescriptor descriptor) {
        TypeElementDescriptor element = descriptor.typeElement().orElse( null );
        if ( element == null ) {
            return Collections.emptyList();
        }
        return elements.recordComponents( element );
    }

    @Override
    public boolean overrides(ExecutableDescriptor overrider, ExecutableDescriptor overridden, TypeDescriptor type) {
        TypeElementDescriptor element = type == null ? null : type.typeElement().orElse( null );
        if ( element == null ) {
            return false;
        }
        return elements.overrides( overrider, overridden, element );
    }

    private Map<String, KSType> buildTypeSubstitutions(KSType containingType) {
        Map<String, KSType> substitutions = new HashMap<>();

        if ( containingType.getDeclaration() instanceof KSClassDeclaration ) {
            KSClassDeclaration classDecl = (KSClassDeclaration) containingType.getDeclaration();
            List<KSTypeParameter> typeParams = classDecl.getTypeParameters();
            List<KSTypeArgument> typeArgs = containingType.getArguments();

            for ( int i = 0; i < Math.min( typeParams.size(), typeArgs.size() ); i++ ) {
                KSTypeParameter param = typeParams.get( i );
                KSTypeArgument arg = typeArgs.get( i );
                KSTypeReference typeRef = arg.getType();
                if ( typeRef != null ) {
                    substitutions.put( param.getName().asString(), typeRef.resolve() );
                }
            }
        }

        return substitutions;
    }

    private KSType resolveType(KSType type, Map<String, KSType> substitutions) {
        if ( type == null || substitutions.isEmpty() ) {
            return type;
        }

        // Check if this is a type parameter that should be substituted
        if ( type.getDeclaration() instanceof KSTypeParameter ) {
            String paramName = type.getDeclaration().getSimpleName().asString();
            KSType substitution = substitutions.get( paramName );
            if ( substitution != null ) {
                return substitution;
            }
        }

        return type;
    }

    private List<String> enumConstants(TypeElementDescriptor typeElement) {
        if ( typeElement == null ) {
            return Collections.emptyList();
        }

        List<FieldDescriptor> fields = elements.enclosedFields( typeElement );
        if ( fields.isEmpty() ) {
            return Collections.emptyList();
        }

        List<String> constants = new ArrayList<>();
        for ( FieldDescriptor field : fields ) {
            if ( field.kind() == LangElementKind.ENUM_CONSTANT ) {
                Set<LangModifier> modifiers = field.modifiers();
                if ( modifiers.contains( LangModifier.PUBLIC ) ) {
                    constants.add( field.simpleName().content() );
                }
            }
        }

        return constants.isEmpty() ? Collections.emptyList() : Collections.unmodifiableList( constants );
    }

    private NameResult computeNames(TypeDescriptor descriptor,
                                    TypeElementDescriptor typeElement,
                                    TypeDescriptor baseComponentType) {
        switch ( descriptor.kind() ) {
            case DECLARED:
                return declaredNames( typeElement );
            case ARRAY:
                return arrayNames( descriptor, baseComponentType );
            case TYPE_PARAMETER:
                return typeParameterNames( descriptor );
            default:
                return simpleNames( descriptor.displayName() );
        }
    }

    private NameResult declaredNames(TypeElementDescriptor typeElement) {
        if ( typeElement == null ) {
            return new NameResult( null, null, null );
        }
        String simpleName = typeElement.simpleName().content();
        String qualifiedName = typeElement.qualifiedName();
        String packageName = null;
        PackageDescriptor packageDescriptor = elements.packageOf( typeElement );
        if ( packageDescriptor != null ) {
            packageName = packageDescriptor.qualifiedName();
        }
        return new NameResult( simpleName, qualifiedName, packageName );
    }

    private NameResult arrayNames(TypeDescriptor descriptor, TypeDescriptor baseComponentType) {
        String suffix = arraySuffix( descriptor );
        NameResult baseNames;
        if ( baseComponentType == null ) {
            baseNames = simpleNames( descriptor.displayName() );
        }
        else if ( baseComponentType.kind() == LangTypeKind.DECLARED ) {
            TypeElementDescriptor baseElement = baseComponentType.typeElement().orElse( null );
            baseNames = declaredNames( baseElement );
        }
        else if ( baseComponentType.kind() == LangTypeKind.TYPE_PARAMETER ) {
            baseNames = typeParameterNames( baseComponentType );
        }
        else {
            baseNames = simpleNames( baseComponentType.displayName() );
        }

        String simpleName = baseNames.simpleName != null ? baseNames.simpleName + suffix : null;
        String qualifiedName = baseNames.qualifiedName != null
            ? baseNames.qualifiedName + suffix
            : simpleName;
        String packageName = baseNames.packageName;

        return new NameResult( simpleName, qualifiedName, packageName );
    }

    private NameResult typeParameterNames(TypeDescriptor descriptor) {
        String name = descriptor.typeVariableName().orElse( descriptor.displayName() );
        return new NameResult( name, name, null );
    }

    private NameResult simpleNames(String displayName) {
        return new NameResult( displayName, displayName, null );
    }

    private String arraySuffix(TypeDescriptor descriptor) {
        StringBuilder suffix = new StringBuilder();
        TypeDescriptor current = descriptor;
        while ( current != null && current.kind() == LangTypeKind.ARRAY ) {
            suffix.append( "[]" );
            current = current.componentType().orElse( null );
        }
        return suffix.toString();
    }

    private TypeDescriptor baseComponentType(TypeDescriptor descriptor) {
        if ( descriptor == null || descriptor.kind() != LangTypeKind.ARRAY ) {
            return null;
        }
        TypeDescriptor current = descriptor;
        TypeDescriptor component = current.componentType().orElse( null );
        while ( component != null && component.kind() == LangTypeKind.ARRAY ) {
            current = component;
            component = component.componentType().orElse( null );
        }
        return component;
    }

    private TypeDescriptor resolveTopLevelType(TypeDescriptor descriptor,
                                               TypeElementDescriptor typeElement,
                                               TypeDescriptor baseComponentType) {
        if ( descriptor == null ) {
            return null;
        }
        if ( descriptor.kind() == LangTypeKind.ARRAY ) {
            return resolveTopLevelType( baseComponentType, baseComponentType != null
                ? baseComponentType.typeElement().orElse( null )
                : null, null );
        }
        if ( typeElement == null ) {
            return null;
        }

        Optional<ElementDescriptor> enclosing = typeElement.enclosingElement();
        if ( !enclosing.isPresent() ) {
            return null;
        }

        ElementDescriptor current = enclosing.get();
        ElementDescriptor candidate = null;
        while ( current != null ) {
            Optional<ElementDescriptor> next = current.enclosingElement();
            if ( !next.isPresent() ) {
                break;
            }
            if ( next.get().kind() == LangElementKind.PACKAGE ) {
                candidate = current;
                break;
            }
            current = next.get();
        }

        if ( candidate instanceof TypeElementDescriptor ) {
            return candidate.asType();
        }
        return null;
    }

    private static final class NameResult {
        final String simpleName;
        final String qualifiedName;
        final String packageName;

        NameResult(String simpleName, String qualifiedName, String packageName) {
            this.simpleName = simpleName;
            this.qualifiedName = qualifiedName;
            this.packageName = packageName;
        }
    }
}
