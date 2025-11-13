/*
 * Copyright MapStruct Authors.
 *
 * Licensed under the Apache License version 2.0, available at http://www.apache.org/licenses/LICENSE-2.0
 */
package org.mapstruct.ap.test.langmodel.parity;

import static org.assertj.core.api.Assertions.assertThat;

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

import org.junit.jupiter.api.Test;

class LangModelDescriptorParityTest {

    @Test
    void descriptorSnapshotMatchesGolden() throws Exception {
        JavaCompiler compiler = ToolProvider.getSystemJavaCompiler();
        assertThat( compiler ).as( "JavaCompiler must be available" ).isNotNull();

        Path fixture = Paths.get(
            "src",
            "test",
            "java",
            "org",
            "mapstruct",
            "ap",
            "test",
            "langmodel",
            "parity",
            "DescriptorParityFixtures.java"
        );

        Path golden = Paths.get(
            "src",
            "test",
            "resources",
            "org",
            "mapstruct",
            "ap",
            "test",
            "langmodel",
            "parity",
            "descriptor-parity-snapshot.txt"
        ).toAbsolutePath().normalize();

        DiagnosticCollector<JavaFileObject> diagnostics = new DiagnosticCollector<>();
        try ( StandardJavaFileManager fileManager = compiler.getStandardFileManager(
            diagnostics,
            null,
            StandardCharsets.UTF_8
        ) ) {
            Iterable<? extends JavaFileObject> sources =
                fileManager.getJavaFileObjectsFromFiles( List.of( fixture.toFile() ) );

            List<String> options = new ArrayList<>();
            options.add( "-proc:only" );
            options.add( "-classpath" );
            options.add( System.getProperty( "java.class.path" ) );
            options.add( "-processor" );
            options.add( "org.mapstruct.ap.test.langmodel.parity.LangModelDescriptorParityProcessor" );
            options.add( "-Amapstruct.test.descriptorParityGolden=" + golden );

            Boolean success = compiler.getTask( null, fileManager, diagnostics, options, null, sources ).call();

            assertThat( diagnostics.getDiagnostics() )
                .filteredOn( diagnostic -> diagnostic.getKind() == Diagnostic.Kind.ERROR )
                .isEmpty();
            assertThat( success ).isTrue();
        }
    }
}

