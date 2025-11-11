/*
 * Copyright MapStruct Authors.
 *
 * Licensed under the Apache License version 2.0, available at http://www.apache.org/licenses/LICENSE-2.0
 */
package org.mapstruct.ap.test.bugs._3089;

import java.util.Collection;

import javax.lang.model.element.Element;
import javax.lang.model.element.ElementKind;
import javax.lang.model.element.Modifier;
import javax.lang.model.element.Name;
import javax.lang.model.element.TypeElement;
import javax.lang.model.type.DeclaredType;
import javax.lang.model.type.TypeMirror;

import org.mapstruct.ap.descriptor.ExecutableDescriptor;
import org.mapstruct.ap.descriptor.LangModifier;
import org.mapstruct.ap.descriptor.TypeDescriptor;
import org.mapstruct.ap.descriptor.TypeElementDescriptor;
import org.mapstruct.ap.spi.BuilderInfo;
import org.mapstruct.ap.spi.BuilderProvider;
import org.mapstruct.ap.spi.ImmutablesBuilderProvider;

/**
 * Regression test for <a href="https://github.com/mapstruct/mapstruct/issues/3089">#3089</a>.
 */
public class Issue3089BuilderProvider extends ImmutablesBuilderProvider implements BuilderProvider {

    @Override
    public BuilderInfo findBuilderInfo(TypeDescriptor type) {
        TypeElementDescriptor descriptor = toTypeElement( type );
        if ( descriptor == null ) {
            return null;
        }
        return findBuilderInfo( descriptor );
    }

    @Override
    protected BuilderInfo findBuilderInfo(TypeElementDescriptor typeElement) {
        Element nativeElement = unwrapTypeElement( typeElement );
        if ( nativeElement instanceof TypeElement ) {
            TypeElement typeElementHandle = (TypeElement) nativeElement;
            if ( typeElementHandle.getQualifiedName().toString().endsWith( ".Item" ) ) {
                BuilderInfo info = findBuilderInfoFromInnerBuilderClass( typeElement, typeElementHandle );
                if ( info != null ) {
                    return info;
                }
            }
        }
        return super.findBuilderInfo( typeElement );
    }

    private BuilderInfo findBuilderInfoFromInnerBuilderClass(TypeElementDescriptor descriptor,
                                                            TypeElement typeElement) {
        if ( shouldIgnore( descriptor ) ) {
            return null;
        }

        for ( Element enclosed : typeElement.getEnclosedElements() ) {
            if ( enclosed.getKind() != ElementKind.CLASS ) {
                continue;
            }
            TypeElement innerType = (TypeElement) enclosed;
            if ( isBuilderCandidate( innerType ) ) {
                TypeElementDescriptor builderDescriptor = elements.typeElement(
                    innerType.getQualifiedName().toString()
                );
                if ( builderDescriptor == null ) {
                    continue;
                }
                ExecutableDescriptor defaultConstructor = elements.constructors( builderDescriptor ).stream()
                    .filter( c -> c.parameters().isEmpty() )
                    .filter( c -> c.modifiers().contains( LangModifier.PUBLIC ) )
                    .findFirst()
                    .orElse( null );
                if ( defaultConstructor != null ) {
                    Collection<ExecutableDescriptor> buildMethods = findBuildMethods(
                        builderDescriptor,
                        builderDescriptor,
                        descriptor
                    );
                    if ( !buildMethods.isEmpty() ) {
                        return new BuilderInfo.Builder()
                            .builderCreationMethod( defaultConstructor )
                            .buildMethod( buildMethods )
                            .build();
                    }
                }
            }
        }
        return null;
    }

    private boolean isBuilderCandidate(TypeElement innerType) {
        TypeElement outerType = (TypeElement) innerType.getEnclosingElement();
        String outerQualifiedName = outerType.getQualifiedName().toString();
        int idx = outerQualifiedName.lastIndexOf( '.' );
        String packageName = idx < 0 ? "" : outerQualifiedName.substring( 0, idx );
        Name outerSimpleName = outerType.getSimpleName();
        String builderClassName = packageName + ".Immutable" + outerSimpleName + ".Builder";
        TypeElement superType = resolveSuperType( innerType );
        return innerType.getSimpleName().contentEquals( "Builder" )
            && superType != null
            && superType.getQualifiedName().contentEquals( builderClassName )
            && innerType.getModifiers().contains( Modifier.PUBLIC );
    }

    private TypeElement resolveSuperType(TypeElement innerType) {
        TypeMirror mirror = innerType.getSuperclass();
        if ( !( mirror instanceof DeclaredType ) ) {
            return null;
        }
        Element element = ( (DeclaredType) mirror ).asElement();
        return element instanceof TypeElement ? (TypeElement) element : null;
    }
}
