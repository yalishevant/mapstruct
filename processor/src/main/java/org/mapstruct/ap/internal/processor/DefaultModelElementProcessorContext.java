/*
 * Copyright MapStruct Authors.
 *
 * Licensed under the Apache License version 2.0, available at http://www.apache.org/licenses/LICENSE-2.0
 */
package org.mapstruct.ap.internal.processor;

import java.util.Map;
import java.util.Objects;
import org.mapstruct.ap.internal.codegen.CodeGenerationContext;
import org.mapstruct.ap.internal.codegen.CodeGenerator;
import org.mapstruct.ap.internal.codegen.DefaultCodeGenerationContext;
import org.mapstruct.ap.internal.codegen.freemarker.FreemarkerCodeGenerator;
import org.mapstruct.ap.internal.codegen.freemarker.FreemarkerTemplateRenderer;
import org.mapstruct.ap.internal.codegen.template.TemplateRenderer;
import org.mapstruct.ap.internal.option.Options;
import org.mapstruct.ap.internal.processor.ModelElementProcessor.ProcessorContext;
import org.mapstruct.ap.internal.util.AccessorNamingUtils;
import org.mapstruct.ap.internal.util.AnnotationProcessorContextView;
import org.mapstruct.ap.internal.util.FormattingMessager;
import org.mapstruct.ap.internal.util.RoundContext;
import org.mapstruct.ap.internal.version.VersionInformation;
import org.mapstruct.ap.spi.EnumMappingStrategy;
import org.mapstruct.ap.spi.EnumTransformationStrategy;
import org.mapstruct.ap.internal.langmodel.api.DescriptorUnwrapper;
import org.mapstruct.ap.internal.langmodel.LangModelContext;
import org.mapstruct.ap.internal.langmodel.codegen.GeneratedFileSink;
import org.mapstruct.ap.internal.model.common.TypeFactory;

/**
 * Default implementation of the processor context.
 *
 * @author Gunnar Morling
 */
public class DefaultModelElementProcessorContext implements ProcessorContext {

    private final FormattingMessager messager;
    private final Options options;
    private final TypeFactory typeFactory;
    private final VersionInformation versionInformation;
    private final LangModelContext langModelContext;
    private final DescriptorUnwrapper descriptorUnwrapper;
    private final AccessorNamingUtils accessorNaming;
    private final RoundContext roundContext;
    private final CodeGenerationContext codeGenerationContext;
    private final CodeGenerator codeGenerator;

    public DefaultModelElementProcessorContext(Options options,
            RoundContext roundContext,
            Map<String, String> notToBeImported,
            LangModelContext langModelContext,
            DescriptorUnwrapper descriptorUnwrapper,
            FormattingMessager messager,
            GeneratedFileSink generatedFileSink,
            VersionInformation versionInformation) {

        this.options = Objects.requireNonNull( options, "options" );
        this.roundContext = Objects.requireNonNull( roundContext, "roundContext" );
        this.langModelContext = Objects.requireNonNull( langModelContext, "langModelContext" );
        this.descriptorUnwrapper = Objects.requireNonNull( descriptorUnwrapper, "descriptorUnwrapper" );
        this.messager = Objects.requireNonNull( messager, "messager" );
        this.versionInformation = Objects.requireNonNull( versionInformation, "versionInformation" );
        Objects.requireNonNull( generatedFileSink, "generatedFileSink" );

        AnnotationProcessorContextView annotationProcessorContext =
            roundContext.getAnnotationProcessorContext();
        this.accessorNaming = annotationProcessorContext.getAccessorNaming();
        this.typeFactory = new TypeFactory(
            langModelContext,
            messager,
            roundContext,
            notToBeImported,
            options.isVerbose(),
            versionInformation
        );

        TemplateRenderer templateRenderer = new FreemarkerTemplateRenderer();
        this.codeGenerationContext = new DefaultCodeGenerationContext(
            generatedFileSink,
            templateRenderer,
            descriptorUnwrapper
        );
        this.codeGenerator = new FreemarkerCodeGenerator();
    }

    @Override
    public LangModelContext getLangModelContext() {
        return langModelContext;
    }

    @Override
    public TypeFactory getTypeFactory() {
        return typeFactory;
    }

    @Override
    public FormattingMessager getMessager() {
        return messager;
    }

    @Override
    public AccessorNamingUtils getAccessorNaming() {
        return accessorNaming;
    }

    @Override
    public CodeGenerator getCodeGenerator() {
        return codeGenerator;
    }

    @Override
    public CodeGenerationContext getCodeGenerationContext() {
        return codeGenerationContext;
    }

    @Override
    public Map<String, EnumTransformationStrategy> getEnumTransformationStrategies() {
        return roundContext.getAnnotationProcessorContext().getEnumTransformationStrategies();
    }

    @Override
    public EnumMappingStrategy getEnumMappingStrategy() {
        return roundContext.getAnnotationProcessorContext().getEnumMappingStrategy();
    }

    @Override
    public Options getOptions() {
        return options;
    }

    @Override
    public VersionInformation getVersionInformation() {
        return versionInformation;
    }

    @Override
    public boolean isErroneous() {
        return messager.isErroneous();
    }

    @Override
    public DescriptorUnwrapper getDescriptorUnwrapper() {
        return descriptorUnwrapper;
    }

}
