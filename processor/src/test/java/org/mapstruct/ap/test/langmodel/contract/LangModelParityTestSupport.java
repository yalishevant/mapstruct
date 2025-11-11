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

import javax.tools.Diagnostic;
import javax.tools.DiagnosticCollector;
import javax.tools.JavaCompiler;
import javax.tools.JavaFileObject;
import javax.tools.StandardJavaFileManager;
import javax.tools.ToolProvider;

import static org.assertj.core.api.Assertions.assertThat;

final class LangModelParityTestSupport {

    private LangModelParityTestSupport() {
    }

    static void runParityCheck(String focus) throws Exception {
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
            options.add( "org.mapstruct.ap.test.langmodel.contract.LangModelParityProcessor" );
            if ( focus != null && !focus.isBlank() ) {
                options.add( "-Amapstruct.test.langModelParity=" + focus );
            }

            Boolean success = compiler.getTask( null, fileManager, diagnostics, options, null, sources ).call();

            assertThat( diagnostics.getDiagnostics() )
                .filteredOn( diagnostic -> diagnostic.getKind() == Diagnostic.Kind.ERROR )
                .isEmpty();
            assertThat( success ).isTrue();
        }
    }
}
