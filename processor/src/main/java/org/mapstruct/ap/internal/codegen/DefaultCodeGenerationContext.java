/*
 * Copyright MapStruct Authors.
 *
 * Licensed under the Apache License version 2.0, available at http://www.apache.org/licenses/LICENSE-2.0
 */
package org.mapstruct.ap.internal.codegen;

import org.mapstruct.ap.internal.codegen.template.TemplateRenderer;
import org.mapstruct.ap.internal.langmodel.api.DescriptorUnwrapper;
import org.mapstruct.ap.internal.langmodel.codegen.GeneratedFileSink;

/**
 * Default immutable implementation of {@link CodeGenerationContext}.
 */
public final class DefaultCodeGenerationContext implements CodeGenerationContext {

    private final GeneratedFileSink generatedFileSink;
    private final TemplateRenderer templateRenderer;
    private final DescriptorUnwrapper descriptorUnwrapper;

    public DefaultCodeGenerationContext(GeneratedFileSink generatedFileSink, TemplateRenderer templateRenderer,
                                        DescriptorUnwrapper descriptorUnwrapper) {
        this.generatedFileSink = generatedFileSink;
        this.templateRenderer = templateRenderer;
        this.descriptorUnwrapper = descriptorUnwrapper;
    }

    @Override
    public GeneratedFileSink getGeneratedFileSink() {
        return generatedFileSink;
    }

    @Override
    public TemplateRenderer getTemplateRenderer() {
        return templateRenderer;
    }

    @Override
    public DescriptorUnwrapper getDescriptorUnwrapper() {
        return descriptorUnwrapper;
    }
}
