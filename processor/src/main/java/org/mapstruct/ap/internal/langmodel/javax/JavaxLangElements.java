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
import java.util.Objects;

import javax.lang.model.element.Element;
import javax.lang.model.element.ExecutableElement;
import javax.lang.model.element.PackageElement;
import javax.lang.model.element.TypeElement;
import javax.lang.model.element.VariableElement;
import javax.lang.model.util.ElementFilter;

import org.mapstruct.ap.internal.langmodel.descriptor.AnnotationDescriptor;
import org.mapstruct.ap.internal.langmodel.descriptor.ElementDescriptor;
import org.mapstruct.ap.internal.langmodel.descriptor.ExecutableDescriptor;
import org.mapstruct.ap.internal.langmodel.descriptor.FieldDescriptor;
import org.mapstruct.ap.internal.langmodel.api.LangElements;
import org.mapstruct.ap.internal.langmodel.api.PackageDescriptor;
import org.mapstruct.ap.internal.langmodel.descriptor.RecordComponentDescriptor;
import org.mapstruct.ap.internal.langmodel.descriptor.TypeElementDescriptor;

final class JavaxLangElements implements LangElements {

    private final JavaxLangModelContext context;
    private final JavaxDescriptorFactory factory;

    private static final boolean RECORD_COMPONENTS_AVAILABLE;
    private static final Method RECORD_COMPONENTS_IN;

    static {
        Method recordComponentsIn;
        boolean recordComponentsAvailable;
        try {
            recordComponentsIn = ElementFilter.class.getMethod( "recordComponentsIn", Iterable.class );
            recordComponentsAvailable = true;
        }
        catch ( NoSuchMethodException ex ) {
            recordComponentsIn = null;
            recordComponentsAvailable = false;
        }
        RECORD_COMPONENTS_IN = recordComponentsIn;
        RECORD_COMPONENTS_AVAILABLE = recordComponentsAvailable;
    }

    JavaxLangElements(JavaxLangModelContext context,
                      JavaxDescriptorFactory factory) {
        this.context = Objects.requireNonNull( context );
        this.factory = Objects.requireNonNull( factory );
    }

    @Override
    public TypeElementDescriptor typeElement(String canonicalName) {
        TypeElement element = context.delegateElementUtils().getTypeElement( canonicalName );
        if ( element == null ) {
            return null;
        }
        return factory.typeElementDescriptor( element );
    }

    @Override
    public PackageDescriptor packageOf(ElementDescriptor element) {
        PackageElement packageElement =
            context.delegateElementUtils().getPackageOf( JavaxElementUnwrapper.unwrapElement( element ) );
        return (PackageDescriptor) factory.elementDescriptor( packageElement );
    }

    @Override
    public boolean overrides(ExecutableDescriptor overrider, ExecutableDescriptor overridden,
                             TypeElementDescriptor type) {
        ExecutableElement overriderElement = ( (JavaxExecutableDescriptor) overrider ).element();
        ExecutableElement overriddenElement = ( (JavaxExecutableDescriptor) overridden ).element();
        TypeElement typeElement = ( (JavaxTypeElementDescriptor) type ).element();
        return context.delegateElementUtils().overrides( overriderElement, overriddenElement, typeElement );
    }

    @Override
    public List<AnnotationDescriptor> annotationMirrors(ElementDescriptor element) {
        return JavaxAnnotationFactory.annotations(
            context,
            factory,
            JavaxElementUnwrapper.unwrapElement( element )
        );
    }

    @Override
    public List<ExecutableDescriptor> enclosedExecutables(TypeElementDescriptor type) {
        TypeElement typeElement = ( (JavaxTypeElementDescriptor) type ).element();
        List<ExecutableElement> methods =
            context.delegateElementUtils().getAllEnclosedExecutableElements( typeElement );

        if ( methods.isEmpty() ) {
            return Collections.emptyList();
        }
        List<ExecutableDescriptor> descriptors = new ArrayList<>( methods.size() );

        for ( ExecutableElement method : methods ) {
            descriptors.add( (ExecutableDescriptor) factory.elementDescriptor( method ) );
        }
        return Collections.unmodifiableList( descriptors );
    }

    @Override
    public List<ExecutableDescriptor> constructors(TypeElementDescriptor type) {
        TypeElement typeElement = ( (JavaxTypeElementDescriptor) type ).element();
        List<ExecutableElement> constructors = ElementFilter.constructorsIn( typeElement.getEnclosedElements() );
        if ( constructors.isEmpty() ) {
            return Collections.emptyList();
        }
        List<ExecutableDescriptor> descriptors = new ArrayList<>( constructors.size() );
        for ( ExecutableElement constructor : constructors ) {
            descriptors.add( (ExecutableDescriptor) factory.elementDescriptor( constructor ) );
        }
        return Collections.unmodifiableList( descriptors );
    }

    @Override
    public List<FieldDescriptor> enclosedFields(TypeElementDescriptor type) {
        TypeElement typeElement = ( (JavaxTypeElementDescriptor) type ).element();
        List<VariableElement> fields = context.delegateElementUtils().getAllEnclosedFields( typeElement );
        if ( fields.isEmpty() ) {
            return Collections.emptyList();
        }
        List<FieldDescriptor> descriptors = new ArrayList<>( fields.size() );
        for ( VariableElement field : fields ) {
            descriptors.add( (FieldDescriptor) factory.elementDescriptor( field ) );
        }
        return Collections.unmodifiableList( descriptors );
    }

    @Override
    public List<RecordComponentDescriptor> recordComponents(TypeElementDescriptor type) {
        if ( !RECORD_COMPONENTS_AVAILABLE || !type.isRecord() ) {
            return Collections.emptyList();
        }
        TypeElement typeElement = ( (JavaxTypeElementDescriptor) type ).element();
        try {
            @SuppressWarnings("unchecked")
            List<Element> components =
                (List<Element>) RECORD_COMPONENTS_IN.invoke( null, typeElement.getEnclosedElements() );
            if ( components == null || components.isEmpty() ) {
                return Collections.emptyList();
            }
            List<RecordComponentDescriptor> result = new ArrayList<>( components.size() );
            for ( Element component : components ) {
                result.add( (RecordComponentDescriptor) factory.elementDescriptor( component ) );
            }
            return Collections.unmodifiableList( result );
        }
        catch ( IllegalAccessException | InvocationTargetException ex ) {
            return Collections.emptyList();
        }
    }
}
