/*
 * Copyright MapStruct Authors.
 *
 * Licensed under the Apache License version 2.0, available at http://www.apache.org/licenses/LICENSE-2.0
 */
package org.mapstruct.ap.internal.langmodel;

import org.mapstruct.ap.internal.langmodel.descriptor.AnnotationDescriptor;
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

/**
 * Factory for instantiating gem wrappers from language-neutral {@link AnnotationDescriptor descriptors}.
 *
 * <p>
 * Implementations are backend-specific and re-use the descriptor abstraction to bridge into MapStruct's
 * generated gem classes without leaking {@code javax.lang.model} types into the neutral layer.
 * </p>
 */
public interface AnnotationGemFactory {

    BeanMappingGem beanMapping(AnnotationDescriptor annotation);

    AnnotateWithGem annotateWith(AnnotationDescriptor annotation);

    AnnotateWithsGem annotateWiths(AnnotationDescriptor annotation);

    IterableMappingGem iterableMapping(AnnotationDescriptor annotation);

    MapMappingGem mapMapping(AnnotationDescriptor annotation);

    EnumMappingGem enumMapping(AnnotationDescriptor annotation);

    MappingGem mapping(AnnotationDescriptor annotation);

    MappingsGem mappings(AnnotationDescriptor annotation);

    SubclassMappingGem subclassMapping(AnnotationDescriptor annotation);

    SubclassMappingsGem subclassMappings(AnnotationDescriptor annotation);

    ValueMappingGem valueMapping(AnnotationDescriptor annotation);

    ValueMappingsGem valueMappings(AnnotationDescriptor annotation);

    ConditionGem condition(AnnotationDescriptor annotation);

    IgnoredGem ignored(AnnotationDescriptor annotation);

    IgnoredListGem ignoredList(AnnotationDescriptor annotation);

    BuilderGem builder(AnnotationDescriptor annotation);

    DecoratedWithGem decoratedWith(AnnotationDescriptor annotation);

    InheritConfigurationGem inheritConfiguration(AnnotationDescriptor annotation);

    InheritInverseConfigurationGem inheritInverseConfiguration(AnnotationDescriptor annotation);

    JavadocGem javadoc(AnnotationDescriptor annotation);
}
