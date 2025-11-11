/*
 * Copyright MapStruct Authors.
 *
 * Licensed under the Apache License version 2.0, available at http://www.apache.org/licenses/LICENSE-2.0
 */
package org.mapstruct.ap.test.langmodel.contract;

import java.nio.charset.StandardCharsets;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

import javax.tools.Diagnostic;
import javax.tools.DiagnosticCollector;
import javax.tools.JavaCompiler;
import javax.tools.JavaFileObject;
import javax.tools.StandardJavaFileManager;
import javax.tools.ToolProvider;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Executes {@link LangModelSpiFallbackDiagnosticsProcessor} to confirm that SPI fallbacks emit diagnostics through the
 * language-model facade and compilation proceeds without errors.
 */
class LangModelSpiFallbackDiagnosticsTest {

    @Test
    void emitsWarningWhenSpiBridgeCapabilityMissing() throws Exception {
        JavaCompiler compiler = ToolProvider.getSystemJavaCompiler();
        assertThat( compiler ).as( "JavaCompiler must be available in test runtime" ).isNotNull();

        DiagnosticCollector<JavaFileObject> diagnostics = new DiagnosticCollector<>();
        try ( StandardJavaFileManager fileManager = compiler.getStandardFileManager(
            diagnostics,
            null,
            StandardCharsets.UTF_8
        ) ) {
            Path fixture = Paths.get(
                "src",
                "test",
                "java",
                "org",
                "mapstruct",
                "ap",
                "test",
                "langmodel",
                "contract",
                "LangModelContractTypes.java"
            );

            Iterable<? extends JavaFileObject> sources =
                fileManager.getJavaFileObjectsFromFiles( List.of( fixture.toFile() ) );

            List<String> options = new ArrayList<>();
            options.add( "-proc:only" );
            options.add( "-classpath" );
            options.add( System.getProperty( "java.class.path" ) );
            options.add( "-processor" );
            options.add( "org.mapstruct.ap.test.langmodel.contract.LangModelSpiFallbackDiagnosticsProcessor" );

            Boolean success = compiler.getTask( null, fileManager, diagnostics, options, null, sources ).call();

            assertThat( diagnostics.getDiagnostics() )
                .filteredOn( diagnostic -> diagnostic.getKind() == Diagnostic.Kind.ERROR )
                .withFailMessage( () -> "SPI fallback diagnostics test failed:\n"
                    + formatDiagnostics( diagnostics.getDiagnostics() ) )
                .isEmpty();

            assertThat( success )
                .as( () -> "Compilation failed:\n" + formatDiagnostics( diagnostics.getDiagnostics() ) )
                .isTrue();

            assertThat( diagnostics.getDiagnostics() )
                .filteredOn( diagnostic -> diagnostic.getKind() == Diagnostic.Kind.WARNING )
                .map( diagnostic -> diagnostic.getMessage( null ) )
                .anyMatch( message -> message.contains(
                    "Optional capability 'SpiBridgeCapability' is not provided by the current language model backend"
                ) );
        }
    }

    private String formatDiagnostics(List<Diagnostic<? extends JavaFileObject>> diagnostics) {
        return diagnostics.stream()
            .map( diagnostic -> diagnostic.getKind() + ": " + diagnostic.getMessage( null ) )
            .collect( Collectors.joining( System.lineSeparator() ) );
    }
}
