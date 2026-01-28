/*
 * Copyright MapStruct Authors.
 *
 * Licensed under the Apache License version 2.0, available at http://www.apache.org/licenses/LICENSE-2.0
 */
package org.mapstruct.ap.internal.langmodel.ksp;

import com.google.devtools.ksp.processing.Resolver;
import com.google.devtools.ksp.symbol.KSAnnotation;

import org.mapstruct.ap.internal.gem.AnnotateWithGem;
import org.mapstruct.ap.internal.gem.AnnotateWithsGem;
import org.mapstruct.ap.internal.gem.BeanMappingGem;
import org.mapstruct.ap.internal.gem.BuilderGem;
import org.mapstruct.ap.internal.gem.ConditionGem;
import org.mapstruct.ap.internal.gem.DecoratedWithGem;
import org.mapstruct.ap.internal.gem.EnumMappingGem;
import org.mapstruct.ap.internal.gem.IgnoredGem;
import org.mapstruct.ap.internal.gem.IgnoredListGem;
import org.mapstruct.ap.internal.gem.InheritConfigurationGem;
import org.mapstruct.ap.internal.gem.InheritInverseConfigurationGem;
import org.mapstruct.ap.internal.gem.IterableMappingGem;
import org.mapstruct.ap.internal.gem.JavadocGem;
import org.mapstruct.ap.internal.gem.MapMappingGem;
import org.mapstruct.ap.internal.gem.MappingGem;
import org.mapstruct.ap.internal.gem.MappingsGem;
import org.mapstruct.ap.internal.gem.SubclassMappingGem;
import org.mapstruct.ap.internal.gem.SubclassMappingsGem;
import org.mapstruct.ap.internal.gem.ValueMappingGem;
import org.mapstruct.ap.internal.gem.ValueMappingsGem;
import org.mapstruct.ap.internal.langmodel.AnnotationGemFactory;
import org.mapstruct.ap.internal.langmodel.descriptor.AnnotationDescriptor;

import javax.lang.model.element.AnnotationMirror;
import java.util.function.Function;

/**
 * KSP implementation of {@link AnnotationGemFactory}.
 * Uses adapters to bridge KSP annotations to javax.lang.model.AnnotationMirror.
 */
public final class KspAnnotationGemFactory implements AnnotationGemFactory {

    private final KspTypeAdapterFactory adapterFactory;

    public KspAnnotationGemFactory(Resolver resolver) {
        this.adapterFactory = new KspTypeAdapterFactory( resolver );
    }

    @Override
    public BeanMappingGem beanMapping(AnnotationDescriptor annotation) {
        return instantiate( annotation, BeanMappingGem::instanceOn );
    }

    @Override
    public AnnotateWithGem annotateWith(AnnotationDescriptor annotation) {
        return instantiate( annotation, AnnotateWithGem::instanceOn );
    }

    @Override
    public AnnotateWithsGem annotateWiths(AnnotationDescriptor annotation) {
        return instantiate( annotation, AnnotateWithsGem::instanceOn );
    }

    @Override
    public IterableMappingGem iterableMapping(AnnotationDescriptor annotation) {
        return instantiate( annotation, IterableMappingGem::instanceOn );
    }

    @Override
    public MapMappingGem mapMapping(AnnotationDescriptor annotation) {
        return instantiate( annotation, MapMappingGem::instanceOn );
    }

    @Override
    public EnumMappingGem enumMapping(AnnotationDescriptor annotation) {
        return instantiate( annotation, EnumMappingGem::instanceOn );
    }

    @Override
    public MappingGem mapping(AnnotationDescriptor annotation) {
        return instantiate( annotation, MappingGem::instanceOn );
    }

    @Override
    public MappingsGem mappings(AnnotationDescriptor annotation) {
        return instantiate( annotation, MappingsGem::instanceOn );
    }

    @Override
    public SubclassMappingGem subclassMapping(AnnotationDescriptor annotation) {
        return instantiate( annotation, SubclassMappingGem::instanceOn );
    }

    @Override
    public SubclassMappingsGem subclassMappings(AnnotationDescriptor annotation) {
        return instantiate( annotation, SubclassMappingsGem::instanceOn );
    }

    @Override
    public ValueMappingGem valueMapping(AnnotationDescriptor annotation) {
        return instantiate( annotation, ValueMappingGem::instanceOn );
    }

    @Override
    public ValueMappingsGem valueMappings(AnnotationDescriptor annotation) {
        return instantiate( annotation, ValueMappingsGem::instanceOn );
    }

    @Override
    public ConditionGem condition(AnnotationDescriptor annotation) {
        return instantiate( annotation, ConditionGem::instanceOn );
    }

    @Override
    public IgnoredGem ignored(AnnotationDescriptor annotation) {
        return instantiate( annotation, IgnoredGem::instanceOn );
    }

    @Override
    public IgnoredListGem ignoredList(AnnotationDescriptor annotation) {
        return instantiate( annotation, IgnoredListGem::instanceOn );
    }

    @Override
    public BuilderGem builder(AnnotationDescriptor annotation) {
        return instantiate( annotation, BuilderGem::instanceOn );
    }

    @Override
    public DecoratedWithGem decoratedWith(AnnotationDescriptor annotation) {
        return instantiate( annotation, DecoratedWithGem::instanceOn );
    }

    @Override
    public InheritConfigurationGem inheritConfiguration(AnnotationDescriptor annotation) {
        return instantiate( annotation, InheritConfigurationGem::instanceOn );
    }

    @Override
    public InheritInverseConfigurationGem inheritInverseConfiguration(AnnotationDescriptor annotation) {
        return instantiate( annotation, InheritInverseConfigurationGem::instanceOn );
    }

    @Override
    public JavadocGem javadoc(AnnotationDescriptor annotation) {
        return instantiate( annotation, JavadocGem::instanceOn );
    }

    private <G> G instantiate(AnnotationDescriptor annotation, Function<AnnotationMirror, G> factory) {
        if ( annotation == null ) {
            return null;
        }

        // If it's already a KspAnnotationDescriptor, unwrap and adapt
        if ( annotation instanceof KspAnnotationDescriptor ) {
            KSAnnotation ksAnnotation = ( (KspAnnotationDescriptor) annotation ).annotation();
            AnnotationMirror mirror = adapterFactory.annotationMirror( ksAnnotation );
            return factory.apply( mirror );
        }

        return null;
    }
}
