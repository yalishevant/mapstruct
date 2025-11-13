/*
 * Copyright MapStruct Authors.
 *
 * Licensed under the Apache License version 2.0, available at http://www.apache.org/licenses/LICENSE-2.0
 */
package org.mapstruct.ap.internal.langmodel.javax;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Locale;
import java.util.Objects;
import java.util.Optional;
import java.util.StringJoiner;

import javax.lang.model.element.Element;
import javax.lang.model.element.ElementKind;
import javax.lang.model.element.ExecutableElement;
import javax.lang.model.element.TypeElement;
import javax.lang.model.element.TypeParameterElement;
import javax.lang.model.element.VariableElement;
import javax.lang.model.type.ArrayType;
import javax.lang.model.type.DeclaredType;
import javax.lang.model.type.IntersectionType;
import javax.lang.model.type.TypeKind;
import javax.lang.model.type.TypeMirror;
import javax.lang.model.type.TypeVariable;
import javax.lang.model.type.UnionType;
import javax.lang.model.type.WildcardType;

import org.mapstruct.ap.internal.langmodel.descriptor.LangTypeKind;
import org.mapstruct.ap.internal.langmodel.descriptor.TypeDescriptor;
import org.mapstruct.ap.internal.langmodel.descriptor.TypeElementDescriptor;
import org.mapstruct.ap.internal.langmodel.descriptor.util.TypeDescriptorDisplay;

final class JavaxTypeDescriptor implements TypeDescriptor {
    private static final Class<?> ANNOTATED_TYPE_CLASS;
    private static final Method ANNOTATED_TYPE_UNDERLYING_METHOD;

    private final JavaxDescriptorFactory factory;
    private final TypeMirror mirror;
    private final TypeMirror underlyingMirror;
    private final String id;
    private final LangTypeKind kind;

    JavaxTypeDescriptor(JavaxDescriptorFactory factory,
                        TypeMirror mirror) {
        this.factory = Objects.requireNonNull( factory );
        this.mirror = Objects.requireNonNull( mirror );
        this.underlyingMirror = unwrapAnnotatedType( mirror );
        this.kind = JavaxKindMapper.map( underlyingMirror.getKind() );
        this.id = computeStableId( underlyingMirror );
    }

    TypeMirror mirror() {
        return mirror;
    }

    @Override
    public String id() {
        return id;
    }

    @Override
    public LangTypeKind kind() {
        return kind;
    }

    @Override
    public Optional<String> qualifiedName() {
        DeclaredType declared = asDeclaredType();
        if ( declared == null ) {
            return Optional.empty();
        }
        TypeElement element = (TypeElement) declared.asElement();
        return Optional.of( element.getQualifiedName().toString() );
    }

    @Override
    public Optional<TypeElementDescriptor> typeElement() {
        DeclaredType declared = asDeclaredType();
        if ( declared == null ) {
            return Optional.empty();
        }
        return Optional.ofNullable(
            factory.typeElementDescriptor( (TypeElement) declared.asElement() )
        );
    }

    @Override
    public Optional<TypeDescriptor> componentType() {
        if ( underlyingMirror.getKind() == TypeKind.ARRAY ) {
            ArrayType arrayType = (ArrayType) underlyingMirror;
            return Optional.ofNullable( factory.typeDescriptor( arrayType.getComponentType() ) );
        }
        return Optional.empty();
    }

    @Override
    public List<TypeDescriptor> typeArguments() {
        DeclaredType declared = asDeclaredType();
        if ( declared == null ) {
            return Collections.emptyList();
        }
        List<TypeDescriptor> result = new ArrayList<>();
        for ( TypeMirror argument : declared.getTypeArguments() ) {
            TypeDescriptor descriptor = factory.typeDescriptor( argument );
            if ( descriptor != null ) {
                result.add( descriptor );
            }
        }
        return result.isEmpty() ? Collections.emptyList() : Collections.unmodifiableList( result );
    }

    @Override
    public boolean isPrimitive() {
        return underlyingMirror.getKind().isPrimitive();
    }

    @Override
    public boolean isVoid() {
        return underlyingMirror.getKind() == TypeKind.VOID;
    }

    @Override
    public Optional<TypeDescriptor> wildcardExtendsBound() {
        if ( underlyingMirror.getKind() == TypeKind.WILDCARD ) {
            WildcardType wildcard = (WildcardType) underlyingMirror;
            return Optional.ofNullable( factory.typeDescriptor( wildcard.getExtendsBound() ) );
        }
        return Optional.empty();
    }

    @Override
    public Optional<TypeDescriptor> wildcardSuperBound() {
        if ( underlyingMirror.getKind() == TypeKind.WILDCARD ) {
            WildcardType wildcard = (WildcardType) underlyingMirror;
            return Optional.ofNullable( factory.typeDescriptor( wildcard.getSuperBound() ) );
        }
        return Optional.empty();
    }

    @Override
    public Optional<String> typeVariableName() {
        if ( underlyingMirror.getKind() == TypeKind.TYPEVAR ) {
            TypeVariable typeVariable = (TypeVariable) underlyingMirror;
            return Optional.of( typeVariable.asElement().getSimpleName().toString() );
        }
        return Optional.empty();
    }

    @Override
    public List<TypeDescriptor> typeVariableBounds() {
        if ( underlyingMirror.getKind() == TypeKind.TYPEVAR ) {
            List<TypeDescriptor> result = new ArrayList<>();
            TypeVariable typeVariable = (TypeVariable) underlyingMirror;
            TypeMirror upper = typeVariable.getUpperBound();
            if ( upper != null && upper.getKind() != TypeKind.NONE ) {
                TypeDescriptor descriptor = factory.typeDescriptor( upper );
                if ( descriptor != null ) {
                    result.add( descriptor );
                }
            }

            TypeMirror lower = typeVariable.getLowerBound();
            if ( lower != null && lower.getKind() != TypeKind.NONE && lower.getKind() != TypeKind.NULL ) {
                TypeDescriptor descriptor = factory.typeDescriptor( lower );
                if ( descriptor != null ) {
                    result.add( descriptor );
                }
            }

            return result.isEmpty() ? Collections.emptyList() : Collections.unmodifiableList( result );
        }
        else if ( underlyingMirror.getKind() == TypeKind.INTERSECTION ) {
            IntersectionType intersectionType = (IntersectionType) underlyingMirror;
            List<TypeDescriptor> result = new ArrayList<>();
            for ( TypeMirror bound : intersectionType.getBounds() ) {
                TypeDescriptor descriptor = factory.typeDescriptor( bound );
                if ( descriptor != null ) {
                    result.add( descriptor );
                }
            }
            return result.isEmpty() ? Collections.emptyList() : Collections.unmodifiableList( result );
        }
        return Collections.emptyList();
    }

    @Override
    public int compareTo(TypeDescriptor other) {
        return TypeDescriptorDisplay.displayName( this )
            .compareTo( TypeDescriptorDisplay.displayName( other ) );
    }

    @Override
    public boolean equals(Object obj) {
        if ( this == obj ) {
            return true;
        }
        if ( !( obj instanceof JavaxTypeDescriptor ) ) {
            return false;
        }
        JavaxTypeDescriptor other = (JavaxTypeDescriptor) obj;
        return Objects.equals( id, other.id );
    }

    @Override
    public int hashCode() {
        return id.hashCode();
    }

    private String computeStableId(TypeMirror type) {
        TypeKind typeKind = type.getKind();
        switch ( typeKind ) {
            case BOOLEAN:
            case BYTE:
            case SHORT:
            case INT:
            case LONG:
            case CHAR:
            case FLOAT:
            case DOUBLE:
                return "primitive:" + typeKind.name().toLowerCase( Locale.ROOT );
            case VOID:
                return "void";
            case NULL:
                return "null";
            case ARRAY:
                return "array:" + computeStableId( ( (ArrayType) type ).getComponentType() );
            case DECLARED:
            case ERROR:
                return declaredTypeId( (DeclaredType) type );
            case TYPEVAR:
                return typeVariableId( (TypeVariable) type );
            case WILDCARD:
                return wildcardId( (WildcardType) type );
            case INTERSECTION:
                return intersectionId( (IntersectionType) type );
            case UNION:
                return unionId( (UnionType) type );
            case PACKAGE:
                return "package:" + type.toString();
            case NONE:
                return "none";
            default:
                return type.toString() + "|" + typeKind;
        }
    }

    private String declaredTypeId(DeclaredType declared) {
        Element element = declared.asElement();
        String qualifiedName;
        if ( element instanceof TypeElement ) {
            qualifiedName = ( (TypeElement) element ).getQualifiedName().toString();
        }
        else {
            qualifiedName = element != null ? element.toString() : declared.toString();
        }
        StringBuilder builder = new StringBuilder( "declared:" ).append( qualifiedName );
        List<? extends TypeMirror> arguments = declared.getTypeArguments();
        if ( !arguments.isEmpty() ) {
            builder.append( '<' );
            for ( int i = 0; i < arguments.size(); i++ ) {
                if ( i > 0 ) {
                    builder.append( ',' );
                }
                builder.append( computeStableId( arguments.get( i ) ) );
            }
            builder.append( '>' );
        }
        return builder.toString();
    }

    private String typeVariableId(TypeVariable typeVariable) {
        Element parameterElement = typeVariable.asElement();
        if ( !( parameterElement instanceof TypeParameterElement ) ) {
            return "typevar:unknown:" + parameterElement + ":" + typeVariable;
        }

        TypeParameterElement typeParameter = (TypeParameterElement) parameterElement;
        Element genericElement = typeParameter.getGenericElement();
        String owner = describeGenericElement( genericElement );
        int index = resolveTypeParameterIndex( typeParameter, genericElement );
        return new StringBuilder()
            .append( "typevar:" )
            .append( owner )
            .append( ':' )
            .append( index )
            .append( ':' )
            .append( typeParameter.getSimpleName() )
            .toString();
    }

    private String describeGenericElement(Element element) {
        if ( element instanceof TypeElement ) {
            return "type:" + ( (TypeElement) element ).getQualifiedName();
        }
        if ( element instanceof ExecutableElement ) {
            return "method:" + describeExecutableElement( (ExecutableElement) element );
        }
        return element.getKind() + ":" + element.toString();
    }

    private String describeExecutableElement(ExecutableElement executable) {
        Element enclosing = executable.getEnclosingElement();
        String enclosingName = enclosing instanceof TypeElement
            ? ( (TypeElement) enclosing ).getQualifiedName().toString()
            : enclosing.toString();

        StringBuilder builder = new StringBuilder();
        builder.append( enclosingName )
            .append( '#' )
            .append( executable.getSimpleName() )
            .append( '(' );
        List<? extends VariableElement> parameters = executable.getParameters();
        for ( int i = 0; i < parameters.size(); i++ ) {
            if ( i > 0 ) {
                builder.append( ',' );
            }
            builder.append( describeParameterType( parameters.get( i ).asType() ) );
        }
        builder.append( ')' );
        return builder.toString();
    }

    private int resolveTypeParameterIndex(TypeParameterElement parameter, Element owner) {
        List<? extends TypeParameterElement> parameters;
        if ( owner instanceof TypeElement ) {
            parameters = ( (TypeElement) owner ).getTypeParameters();
        }
        else if ( owner instanceof ExecutableElement ) {
            parameters = ( (ExecutableElement) owner ).getTypeParameters();
        }
        else {
            return -1;
        }

        for ( int i = 0; i < parameters.size(); i++ ) {
            if ( parameters.get( i ).equals( parameter ) ) {
                return i;
            }
        }
        return -1;
    }

    private String describeParameterType(TypeMirror type) {
        TypeKind kind = type.getKind();
        switch ( kind ) {
            case ARRAY:
                return describeParameterType( ( (ArrayType) type ).getComponentType() ) + "[]";
            case DECLARED:
            case ERROR:
                return describeDeclaredParameterType( (DeclaredType) type );
            case TYPEVAR:
                return ( (TypeVariable) type ).asElement().getSimpleName().toString();
            case WILDCARD:
                return describeWildcardParameterType( (WildcardType) type );
            case INTERSECTION:
                return joinBounds( ( (IntersectionType) type ).getBounds(), " & " );
            case UNION:
                return joinBounds( ( (UnionType) type ).getAlternatives(), " | " );
            case BOOLEAN:
            case BYTE:
            case SHORT:
            case INT:
            case LONG:
            case CHAR:
            case FLOAT:
            case DOUBLE:
                return kind.name().toLowerCase( Locale.ROOT );
            case VOID:
                return "void";
            case NULL:
                return "null";
            default:
                return type.toString();
        }
    }

    private String describeDeclaredParameterType(DeclaredType declared) {
        Element element = declared.asElement();
        String qualifiedName = element instanceof TypeElement
            ? ( (TypeElement) element ).getQualifiedName().toString()
            : element != null ? element.toString() : declared.toString();
        List<? extends TypeMirror> arguments = declared.getTypeArguments();
        if ( arguments.isEmpty() ) {
            return qualifiedName;
        }
        StringBuilder builder = new StringBuilder( qualifiedName ).append( '<' );
        for ( int i = 0; i < arguments.size(); i++ ) {
            if ( i > 0 ) {
                builder.append( ',' );
            }
            builder.append( describeParameterType( arguments.get( i ) ) );
        }
        builder.append( '>' );
        return builder.toString();
    }

    private String describeWildcardParameterType(WildcardType wildcard) {
        StringBuilder builder = new StringBuilder( "?" );
        TypeMirror extendsBound = wildcard.getExtendsBound();
        TypeMirror superBound = wildcard.getSuperBound();
        if ( extendsBound != null ) {
            builder.append( " extends " ).append( describeParameterType( extendsBound ) );
        }
        if ( superBound != null ) {
            builder.append( " super " ).append( describeParameterType( superBound ) );
        }
        return builder.toString();
    }

    private String joinBounds(List<? extends TypeMirror> bounds, String separator) {
        StringJoiner joiner = new StringJoiner( separator );
        for ( TypeMirror bound : bounds ) {
            joiner.add( describeParameterType( bound ) );
        }
        return joiner.toString();
    }

    private String wildcardId(WildcardType wildcard) {
        StringBuilder builder = new StringBuilder( "wildcard:" );
        TypeMirror extendsBound = wildcard.getExtendsBound();
        TypeMirror superBound = wildcard.getSuperBound();
        if ( extendsBound != null ) {
            builder.append( "extends=" ).append( computeStableId( extendsBound ) );
            if ( superBound != null ) {
                builder.append( '&' );
            }
        }
        if ( superBound != null ) {
            builder.append( "super=" ).append( computeStableId( superBound ) );
        }
        if ( extendsBound == null && superBound == null ) {
            builder.append( "unbounded" );
        }
        return builder.toString();
    }

    private String intersectionId(IntersectionType intersection) {
        StringJoiner joiner = new StringJoiner( "&" );
        for ( TypeMirror bound : intersection.getBounds() ) {
            joiner.add( computeStableId( bound ) );
        }
        return "intersection:" + joiner;
    }

    private String unionId(UnionType union) {
        StringJoiner joiner = new StringJoiner( "|" );
        for ( TypeMirror alternative : union.getAlternatives() ) {
            joiner.add( computeStableId( alternative ) );
        }
        return "union:" + joiner;
    }

    private DeclaredType asDeclaredType() {
        if ( underlyingMirror.getKind() == TypeKind.DECLARED ) {
            return (DeclaredType) underlyingMirror;
        }
        return null;
    }

    private static TypeMirror unwrapAnnotatedType(TypeMirror type) {
        if ( ANNOTATED_TYPE_CLASS == null || type == null ) {
            return type;
        }

        TypeMirror current = type;
        while ( ANNOTATED_TYPE_CLASS.isInstance( current ) ) {
            if ( ANNOTATED_TYPE_UNDERLYING_METHOD == null ) {
                break;
            }
            try {
                Object next = ANNOTATED_TYPE_UNDERLYING_METHOD.invoke( current );
                if ( !( next instanceof TypeMirror ) || next == current ) {
                    break;
                }
                current = (TypeMirror) next;
            }
            catch ( IllegalAccessException | InvocationTargetException e ) {
                break;
            }
        }
        return current;
    }

    static {
        Class<?> annotatedTypeClass;
        Method underlyingMethod;
        try {
            annotatedTypeClass = Class.forName( "javax.lang.model.type.AnnotatedType" );
            underlyingMethod = annotatedTypeClass.getMethod( "getUnderlyingType" );
        }
        catch ( ClassNotFoundException | NoSuchMethodException e ) {
            annotatedTypeClass = null;
            underlyingMethod = null;
        }

        ANNOTATED_TYPE_CLASS = annotatedTypeClass;
        ANNOTATED_TYPE_UNDERLYING_METHOD = underlyingMethod;
    }
}
