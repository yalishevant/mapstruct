/*
 * Copyright MapStruct Authors.
 *
 * Licensed under the Apache License version 2.0, available at http://www.apache.org/licenses/LICENSE-2.0
 */
package org.mapstruct.ap.test.langmodel.parity;

import java.io.IOException;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;

import javax.annotation.processing.AbstractProcessor;
import javax.annotation.processing.ProcessingEnvironment;
import javax.annotation.processing.RoundEnvironment;
import javax.annotation.processing.SupportedAnnotationTypes;
import javax.annotation.processing.SupportedSourceVersion;
import javax.lang.model.SourceVersion;
import javax.lang.model.element.TypeElement;
import javax.tools.Diagnostic;

import org.mapstruct.ap.internal.langmodel.LangModelContext;
import org.mapstruct.ap.internal.langmodel.LangModelContextFactory;
import org.mapstruct.ap.internal.langmodel.MapperEntryPoint;
import org.mapstruct.ap.internal.langmodel.shared.DescriptorSnapshot;
import org.mapstruct.ap.internal.langmodel.shared.DescriptorSnapshotBuilder;
import org.mapstruct.ap.internal.langmodel.shared.DescriptorSnapshotFormatter;
import org.mapstruct.ap.internal.langmodel.javax.DefaultVersionInformation;
import org.mapstruct.ap.internal.langmodel.javax.JavaxLangModelContextFactory;
import org.mapstruct.ap.internal.version.VersionInformation;

/**
 * Processor that captures descriptor snapshots for a fixed set of fixture types and verifies the output against a
 * golden file. Intended to run under javac / {@code javax.lang.model}.
 */
@SupportedAnnotationTypes("*")
@SupportedSourceVersion(SourceVersion.RELEASE_21)
public class LangModelDescriptorParityProcessor extends AbstractProcessor {

    private static final String GOLDEN_OPTION = "mapstruct.test.descriptorParityGolden";
    private static final String MAPPER_STUB_FQN = "org.mapstruct.ap.test.langmodel.parity.MapperStub";

    private static final List<String> TYPE_FQNS = List.of(
        "org.mapstruct.ap.test.langmodel.parity.MapperStub",
        "org.mapstruct.ap.test.langmodel.parity.Box",
        "org.mapstruct.ap.test.langmodel.parity.WildcardHolder",
        "org.mapstruct.ap.test.langmodel.parity.OverrideBase",
        "org.mapstruct.ap.test.langmodel.parity.OverrideChild",
        "org.mapstruct.ap.test.langmodel.parity.MultipleConstructors",
        "org.mapstruct.ap.test.langmodel.parity.AdderHolder",
        "org.mapstruct.ap.test.langmodel.parity.SampleEnum",
        "org.mapstruct.ap.test.langmodel.parity.Shape",
        "org.mapstruct.ap.test.langmodel.parity.Circle",
        "org.mapstruct.ap.test.langmodel.parity.Rectangle",
        "org.mapstruct.ap.test.langmodel.parity.MethodHolder"
    );

    private boolean executed;

    @Override
    public Set<String> getSupportedOptions() {
        return Set.of( GOLDEN_OPTION );
    }

    @Override
    public boolean process(Set<? extends TypeElement> annotations, RoundEnvironment roundEnv) {
        if ( executed || roundEnv.processingOver() ) {
            return false;
        }
        executed = true;

        Map<String, String> options = processingEnv.getOptions();
        String goldenPath = options.get( GOLDEN_OPTION );
        if ( goldenPath == null || goldenPath.isBlank() ) {
            processingEnv.getMessager()
                .printMessage( Diagnostic.Kind.ERROR, "Missing compiler option -" + GOLDEN_OPTION );
            return false;
        }

        try {
            Path golden = resolveGoldenPath( goldenPath );
            String expected = Files.readString( golden, StandardCharsets.UTF_8 ).replace( "\r\n", "\n" ).trim();
            String actual = buildSnapshot();

            if ( !Objects.equals( expected, actual ) ) {
                Path actualOutput = golden.resolveSibling( "descriptor-parity-actual.txt" );
                Files.writeString( actualOutput, actual, StandardCharsets.UTF_8 );
                processingEnv.getMessager().printMessage(
                    Diagnostic.Kind.ERROR,
                    "Descriptor parity snapshot mismatch. Expected snapshot: "
                        + golden.toAbsolutePath()
                        + ", actual written to: "
                        + actualOutput.toAbsolutePath()
                );
            }
        }
        catch ( Exception ex ) {
            processingEnv.getMessager().printMessage(
                Diagnostic.Kind.ERROR,
                "Failed to capture descriptor parity snapshot: " + ex
            );
        }

        return false;
    }

    private String buildSnapshot()
        throws NoSuchMethodException, InvocationTargetException, IllegalAccessException {

        LangModelContextFactory factory = new JavaxLangModelContextFactory();
        VersionInformation versionInformation = resolveVersionInformation( processingEnv );

        TypeElement mapperElement = processingEnv.getElementUtils().getTypeElement( MAPPER_STUB_FQN );
        if ( mapperElement == null ) {
            throw new IllegalStateException( "Fixture mapper not found: " + MAPPER_STUB_FQN );
        }

        MapperEntryPoint entryPoint = MapperEntryPoint.of( versionInformation, processingEnv, mapperElement );

        try ( LangModelContext context = factory.create( entryPoint ) ) {
            DescriptorSnapshotBuilder builder = new DescriptorSnapshotBuilder( context );
            DescriptorSnapshot snapshot = builder.snapshotForTypes( TYPE_FQNS );
            return DescriptorSnapshotFormatter.format( snapshot );
        }
    }

    private static VersionInformation resolveVersionInformation(ProcessingEnvironment processingEnvironment)
        throws NoSuchMethodException, InvocationTargetException, IllegalAccessException {
        Method factory = DefaultVersionInformation.class.getDeclaredMethod(
            "fromProcessingEnvironment",
            ProcessingEnvironment.class
        );
        factory.setAccessible( true );
        return (VersionInformation) factory.invoke( null, processingEnvironment );
    }

    private static Path resolveGoldenPath(String suppliedPath) {
        Path path = Paths.get( suppliedPath );
        if ( path.isAbsolute() ) {
            return path;
        }
        return path.toAbsolutePath().normalize();
    }
}
