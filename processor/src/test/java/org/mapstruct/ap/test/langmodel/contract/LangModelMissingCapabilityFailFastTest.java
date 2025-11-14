/*
 * Copyright MapStruct Authors.
 *
 * Licensed under the Apache License version 2.0, available at http://www.apache.org/licenses/LICENSE-2.0
 */
package org.mapstruct.ap.test.langmodel.contract;

import javax.tools.Diagnostic.Kind;

import org.mapstruct.ap.internal.langmodel.LangModelContextFactory;
import org.mapstruct.ap.testutil.ProcessorTest;
import org.mapstruct.ap.testutil.WithClasses;
import org.mapstruct.ap.testutil.WithServiceImplementation;
import org.mapstruct.ap.testutil.compilation.annotation.CompilationResult;
import org.mapstruct.ap.testutil.compilation.annotation.Diagnostic;
import org.mapstruct.ap.testutil.compilation.annotation.ExpectedCompilationOutcome;
import org.mapstruct.ap.testutil.compilation.annotation.ProcessorOption;

/**
 * Verifies that missing mandatory lang model capabilities fail fast with descriptive diagnostics.
 */
@WithClasses(LangModelMissingCapabilityMapper.class)
class LangModelMissingCapabilityFailFastTest {

    private static final String EXPECTED_GEMS_MESSAGE =
        "AnnotationGemsCapability is required by MapStruct Thin API";
    private static final String EXPECTED_ENUM_MESSAGE =
        "EnumMappingCapability is required by MapStruct Thin API";
    private static final String EXPECTED_SPI_MESSAGE =
        "SpiBridgeCapability is required by MapStruct Thin API";

    @ProcessorTest
    @WithServiceImplementation(value = MissingAnnotationGemsLangModelContextFactory.class,
        provides = LangModelContextFactory.class)
    @ProcessorOption(name = "mapstruct.langModelBackend", value = "missing-annotation-gems")
    @ExpectedCompilationOutcome(
        value = CompilationResult.FAILED,
        diagnostics = @Diagnostic(
            kind = Kind.ERROR,
            type = LangModelMissingCapabilityMapper.class,
            messageRegExp = EXPECTED_GEMS_MESSAGE
        )
    )
    void failsWhenAnnotationGemsCapabilityMissing() {
    }

    @ProcessorTest
    @WithServiceImplementation(value = MissingEnumMappingLangModelContextFactory.class,
        provides = LangModelContextFactory.class)
    @ProcessorOption(name = "mapstruct.langModelBackend", value = "missing-enum-mapping")
    @ExpectedCompilationOutcome(
        value = CompilationResult.FAILED,
        diagnostics = @Diagnostic(
            kind = Kind.ERROR,
            type = LangModelMissingCapabilityMapper.class,
            messageRegExp = EXPECTED_ENUM_MESSAGE
        )
    )
    void failsWhenEnumMappingCapabilityMissing() {
    }

    @ProcessorTest
    @WithServiceImplementation(value = MissingSpiBridgeLangModelContextFactory.class,
        provides = LangModelContextFactory.class)
    @ProcessorOption(name = "mapstruct.langModelBackend", value = "missing-spi-bridge")
    @ExpectedCompilationOutcome(
        value = CompilationResult.FAILED,
        diagnostics = @Diagnostic(
            kind = Kind.ERROR,
            type = LangModelMissingCapabilityMapper.class,
            messageRegExp = EXPECTED_SPI_MESSAGE
        )
    )
    void failsWhenSpiBridgeCapabilityMissing() {
    }
}
