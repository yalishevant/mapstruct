/*
 * Copyright MapStruct Authors.
 *
 * Licensed under the Apache License version 2.0, available at http://www.apache.org/licenses/LICENSE-2.0
 */
package org.mapstruct.ap.internal.langmodel.javax;

import java.util.Collections;
import java.util.EnumSet;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;

import javax.lang.model.element.Element;
import javax.lang.model.element.ElementKind;
import javax.lang.model.element.Modifier;

import org.mapstruct.ap.internal.util.IgnoreJRERequirement;
import org.mapstruct.ap.internal.langmodel.descriptor.ElementDescriptor;
import org.mapstruct.ap.internal.langmodel.descriptor.LangElementKind;
import org.mapstruct.ap.internal.langmodel.descriptor.LangModifier;
import org.mapstruct.ap.internal.langmodel.descriptor.NameDescriptor;
import org.mapstruct.ap.internal.langmodel.descriptor.TypeDescriptor;

@IgnoreJRERequirement
abstract class JavaxElementDescriptor implements ElementDescriptor {

    final JavaxLangModelContext context;
    final JavaxDescriptorFactory factory;
    final Element element;
    private final JavaxNameDescriptor name;
    private final Set<LangModifier> modifiers;

    JavaxElementDescriptor(JavaxLangModelContext context,
                           JavaxDescriptorFactory factory,
                           Element element) {
        this.context = context;
        this.factory = factory;
        this.element = element;
        this.name = new JavaxNameDescriptor( element.getSimpleName() );
        this.modifiers = convertModifiers( element.getModifiers() );
    }

    private static final boolean RECORD_KIND_SUPPORTED = isElementKindPresent( "RECORD" );
    private static final boolean RECORD_COMPONENT_KIND_SUPPORTED = isElementKindPresent( "RECORD_COMPONENT" );
    private static final boolean RECORD_COMPONENT_ELEMENT_SUPPORTED =
        isClassPresent( "javax.lang.model.element.RecordComponentElement" );

    static JavaxElementDescriptor create(JavaxLangModelContext context,
                                         JavaxDescriptorFactory factory,
                                         Element element) {
        ElementKind kind = element.getKind();
        if ( RECORD_KIND_SUPPORTED && isKind( kind, "RECORD" ) ) {
            return new JavaxTypeElementDescriptor(
                context,
                factory,
                (javax.lang.model.element.TypeElement) element
            );
        }
        if ( RECORD_COMPONENT_KIND_SUPPORTED
            && RECORD_COMPONENT_ELEMENT_SUPPORTED
            && isKind( kind, "RECORD_COMPONENT" ) ) {
            return JavaxRecordComponentElementDescriptor.create( context, factory, element );
        }

        switch ( kind ) {
            case CLASS:
            case ENUM:
            case INTERFACE:
            case ANNOTATION_TYPE:
                return new JavaxTypeElementDescriptor(
                    context,
                    factory,
                    (javax.lang.model.element.TypeElement) element
                );
            case METHOD:
            case CONSTRUCTOR:
                return new JavaxExecutableDescriptor( context, factory,
                    (javax.lang.model.element.ExecutableElement) element );
            case FIELD:
            case ENUM_CONSTANT:
                return new JavaxFieldElementDescriptor( context, factory,
                    (javax.lang.model.element.VariableElement) element );
            case PACKAGE:
                return new JavaxPackageDescriptor( context, factory,
                    (javax.lang.model.element.PackageElement) element );
            default:
                return null;
        }
    }

    @Override
    public LangElementKind kind() {
        return JavaxKindMapper.map( element.getKind() );
    }

    @Override
    public NameDescriptor simpleName() {
        return name;
    }

    @Override
    public Optional<ElementDescriptor> enclosingElement() {
        Element enclosing = element.getEnclosingElement();
        if ( enclosing == null ) {
            return Optional.empty();
        }
        return Optional.ofNullable( factory.elementDescriptor( enclosing ) );
    }

    @Override
    public Set<LangModifier> modifiers() {
        return modifiers;
    }

    @Override
    public TypeDescriptor asType() {
        return factory.typeDescriptor( element.asType() );
    }

    Element element() {
        return element;
    }

    @Override
    public boolean equals(Object obj) {
        if ( this == obj ) {
            return true;
        }
        if ( !( obj instanceof JavaxElementDescriptor ) ) {
            return false;
        }
        JavaxElementDescriptor other = (JavaxElementDescriptor) obj;
        return Objects.equals( element, other.element );
    }

    @Override
    public int hashCode() {
        return element != null ? element.hashCode() : 0;
    }

    private static Set<LangModifier> convertModifiers(Set<Modifier> modifiers) {
        if ( modifiers.isEmpty() ) {
            return Collections.emptySet();
        }
        EnumSet<LangModifier> converted = EnumSet.noneOf( LangModifier.class );
        for ( Modifier modifier : modifiers ) {
            switch ( modifier ) {
                case ABSTRACT:
                    converted.add( LangModifier.ABSTRACT );
                    break;
                case DEFAULT:
                    converted.add( LangModifier.DEFAULT );
                    break;
                case FINAL:
                    converted.add( LangModifier.FINAL );
                    break;
                case PRIVATE:
                    converted.add( LangModifier.PRIVATE );
                    break;
                case PROTECTED:
                    converted.add( LangModifier.PROTECTED );
                    break;
                case PUBLIC:
                    converted.add( LangModifier.PUBLIC );
                    break;
                case STATIC:
                    converted.add( LangModifier.STATIC );
                    break;
                default:
                    // ignored
            }
        }
        return Collections.unmodifiableSet( converted );
    }

    private static boolean isElementKindPresent(String name) {
        try {
            ElementKind.valueOf( name );
            return true;
        }
        catch (IllegalArgumentException ex) {
            return false;
        }
    }

    private static boolean isClassPresent(String name) {
        try {
            Class.forName( name );
            return true;
        }
        catch (ClassNotFoundException ex) {
            return false;
        }
    }

    private static boolean isKind(ElementKind kind, String expectedName) {
        return kind.name().equals( expectedName );
    }
}
