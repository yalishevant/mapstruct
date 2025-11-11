/*
 * Copyright MapStruct Authors.
 *
 * Licensed under the Apache License version 2.0, available at http://www.apache.org/licenses/LICENSE-2.0
 */
package org.mapstruct.ap.internal.model.source.selector;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Objects;
import java.util.Set;

import org.mapstruct.ap.internal.model.common.Type;
import org.mapstruct.ap.internal.model.source.Method;
import org.mapstruct.ap.internal.model.source.SourceMethod;
import org.mapstruct.ap.internal.util.AnnotationDescriptorUtils;
import org.mapstruct.ap.internal.util.AnnotationValueUtils;
import org.mapstruct.ap.descriptor.AnnotationDescriptor;
import org.mapstruct.ap.descriptor.AnnotationValueDescriptor;
import org.mapstruct.ap.descriptor.ElementDescriptor;
import org.mapstruct.ap.descriptor.ExecutableDescriptor;
import org.mapstruct.ap.descriptor.FieldDescriptor;
import org.mapstruct.ap.descriptor.LangElementKind;
import org.mapstruct.ap.langmodel.api.LangElements;
import org.mapstruct.ap.langmodel.api.LangTypes;
import org.mapstruct.ap.descriptor.TypeDescriptor;
import org.mapstruct.ap.descriptor.TypeElementDescriptor;

/**
 * Finds the {@code XmlElementRef} annotation on a field (of the mapping result type or its super types) matching the
 * target property name. Then selects those methods with matching {@code name} and {@code scope} attributes of the
 * {@code XmlElementDecl} annotation, if that is present. Matching happens in the following order:
 * <ol>
 * <li>Name and Scope matches</li>
 * <li>Scope matches</li>
 * <li>Name matches</li>
 * </ol>
 * If there are name and scope matches, only those will be returned, otherwise the next in line (scope matches), etc. If
 * the given method is not annotated with {@code XmlElementDecl} it will be considered as matching.
 *
 * @author Sjaak Derksen
 * @see JavaxXmlElementDeclSelector
 * @see JakartaXmlElementDeclSelector
 */
abstract class XmlElementDeclSelector implements MethodSelector {

    private final LangElements langElements;
    private final LangTypes langTypes;

    XmlElementDeclSelector(LangElements langElements, LangTypes langTypes) {
        this.langElements = Objects.requireNonNull( langElements );
        this.langTypes = Objects.requireNonNull( langTypes );
    }

    @Override
    public <T extends Method> List<SelectedMethod<T>> getMatchingMethods(List<SelectedMethod<T>> methods,
                                                                         SelectionContext context) {
        Type resultType = context.getMappingMethod().getResultType();
        String targetPropertyName = context.getSelectionCriteria().getTargetPropertyName();

        List<SelectedMethod<T>> nameMatches = new ArrayList<>();
        List<SelectedMethod<T>> scopeMatches = new ArrayList<>();
        List<SelectedMethod<T>> nameAndScopeMatches = new ArrayList<>();
        XmlElementRefInfo xmlElementRefInfo = findXmlElementRef( resultType, targetPropertyName );

        for ( SelectedMethod<T> candidate : methods ) {
            if ( !( candidate.getMethod() instanceof SourceMethod ) ) {
                continue;
            }

            SourceMethod candidateMethod = (SourceMethod) candidate.getMethod();
            ExecutableDescriptor executable = candidateMethod.getExecutableDescriptor();
            XmlElementDeclInfo xmlElementDeclInfo = getXmlElementDeclInfo( executable );

            if ( xmlElementDeclInfo == null ) {
                continue;
            }

            String name = xmlElementDeclInfo.nameValue();
            TypeDescriptor scope = xmlElementDeclInfo.scopeType();

            boolean nameIsSetAndMatches = name != null && name.equals( xmlElementRefInfo.nameValue() );
            boolean scopeIsSetAndMatches =
                scope != null
                    && xmlElementRefInfo.sourceType() != null
                    && langTypes.isSameType( scope, xmlElementRefInfo.sourceType() );

            if ( nameIsSetAndMatches ) {
                if ( scopeIsSetAndMatches ) {
                    nameAndScopeMatches.add( candidate );
                }
                else {
                    nameMatches.add( candidate );
                }
            }
            else if ( scopeIsSetAndMatches ) {
                scopeMatches.add( candidate );
            }
        }

        if ( !nameAndScopeMatches.isEmpty() ) {
            return nameAndScopeMatches;
        }
        else if ( !scopeMatches.isEmpty() ) {
            return scopeMatches;
        }
        else if ( !nameMatches.isEmpty() ) {
            return nameMatches;
        }
        else {
            return methods;
        }
    }

    private XmlElementRefInfo findXmlElementRef(Type resultType, String targetPropertyName) {
        TypeDescriptor startingDescriptor = resultType != null ? resultType.getTypeDescriptor() : null;
        XmlElementRefInfo defaultInfo = new XmlElementRefInfo( targetPropertyName, startingDescriptor );

        if ( targetPropertyName == null || startingDescriptor == null ) {
            return defaultInfo;
        }

        Set<String> visited = new HashSet<>();
        TypeDescriptor current = startingDescriptor;

        while ( current != null && visited.add( current.id() ) ) {
            TypeElementDescriptor currentElement = langTypes.asElement( current );
            if ( currentElement == null ) {
                break;
            }

            for ( FieldDescriptor field : langElements.enclosedFields( currentElement ) ) {
                ElementDescriptor enclosing = field.enclosingElement().orElse( null );
                if ( !( enclosing instanceof TypeElementDescriptor ) ) {
                    continue;
                }
                if ( !currentElement.id().equals( ( (TypeElementDescriptor) enclosing ).id() ) ) {
                    continue;
                }
                if ( targetPropertyName.equals( field.simpleName().content() ) ) {
                    String name = extractXmlElementRefName( field );
                    if ( name != null ) {
                        return new XmlElementRefInfo( name, current );
                    }
                }
            }

            current = directSuperClass( current );
        }

        return defaultInfo;
    }

    private String extractXmlElementRefName(FieldDescriptor field) {
        AnnotationDescriptor annotation = AnnotationDescriptorUtils
            .findAnnotation( langElements, field, xmlElementRefAnnotation() )
            .orElse( null );
        if ( annotation == null ) {
            return null;
        }

        AnnotationValueDescriptor value = AnnotationDescriptorUtils.getValue( annotation, "name" );
        return AnnotationValueUtils.asString( value );
    }

    private TypeDescriptor directSuperClass(TypeDescriptor type) {
        if ( type == null ) {
            return null;
        }
        for ( TypeDescriptor candidate : langTypes.directSupertypes( type ) ) {
            if ( candidate == null ) {
                continue;
            }
            TypeElementDescriptor element = langTypes.asElement( candidate );
            if ( element != null && element.kind() == LangElementKind.CLASS ) {
                return candidate;
            }
        }
        return null;
    }

    private XmlElementDeclInfo getXmlElementDeclInfo(ExecutableDescriptor executable) {
        AnnotationDescriptor annotation = AnnotationDescriptorUtils
            .findAnnotation( langElements, executable, xmlElementDeclAnnotation() )
            .orElse( null );
        if ( annotation == null ) {
            return null;
        }

        String name = AnnotationValueUtils.asString(
            AnnotationDescriptorUtils.getValue( annotation, "name" )
        );
        TypeDescriptor scope = AnnotationValueUtils.asType(
            AnnotationDescriptorUtils.getValue( annotation, "scope" )
        );
        return new XmlElementDeclInfo( name, scope );
    }

    protected abstract String xmlElementDeclAnnotation();

    protected abstract String xmlElementRefAnnotation();

    static class XmlElementRefInfo {

        private final String nameValue;
        private final TypeDescriptor sourceType;

        XmlElementRefInfo(String nameValue, TypeDescriptor sourceType) {
            this.nameValue = nameValue;
            this.sourceType = sourceType;
        }

        String nameValue() {
            return nameValue;
        }

        TypeDescriptor sourceType() {
            return sourceType;
        }
    }

    static class XmlElementDeclInfo {

        private final String nameValue;
        private final TypeDescriptor scopeType;

        XmlElementDeclInfo(String nameValue, TypeDescriptor scopeType) {
            this.nameValue = nameValue;
            this.scopeType = scopeType;
        }

        String nameValue() {
            return nameValue;
        }

        TypeDescriptor scopeType() {
            return scopeType;
        }
    }
}
