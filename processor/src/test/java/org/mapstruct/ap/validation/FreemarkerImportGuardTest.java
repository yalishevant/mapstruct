/*
 * Copyright MapStruct Authors.
 *
 * Licensed under the Apache License version 2.0, available at http://www.apache.org/licenses/LICENSE-2.0
 */
package org.mapstruct.ap.validation;

import static org.assertj.core.api.Assertions.assertThat;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.junit.jupiter.api.Test;

/**
 * Guard that ensures Freemarker stays encapsulated within the code generation package. All new usages must go through
 * {@link org.mapstruct.ap.internal.codegen.TemplateRenderer}.
 */
class FreemarkerImportGuardTest {

    private static final Path INTERNAL_ROOT =
        Paths.get( "src", "main", "java", "org", "mapstruct", "ap", "internal" );

    private static final String ALLOWED_PREFIX = "codegen/freemarker/";

    @Test
    void freemarkerIsNotImportedOutsideCodegenPackage() throws IOException {
        try ( Stream<Path> files = Files.walk( INTERNAL_ROOT ) ) {
            Set<String> offending = files.filter( path -> path.toString().endsWith( ".java" ) )
                .filter( FreemarkerImportGuardTest::containsFreemarkerImport )
                .map( INTERNAL_ROOT::relativize )
                .map( FreemarkerImportGuardTest::normalize )
                .filter( relative -> !relative.startsWith( ALLOWED_PREFIX ) )
                .collect( Collectors.toSet() );

            assertThat( offending )
                .as( () -> formatOffending( offending ) )
                .isEmpty();
        }
    }

    private static boolean containsFreemarkerImport(Path file) {
        try {
            return Files.readAllLines( file, StandardCharsets.UTF_8 )
                .stream()
                .map( String::trim )
                .anyMatch( line -> line.startsWith( "import freemarker." ) );
        }
        catch ( IOException e ) {
            throw new IllegalStateException( "Failed to read " + file, e );
        }
    }

    private static String normalize(Path relativePath) {
        return relativePath.toString().replace( '\\', '/' );
    }

    private static String formatOffending(Set<String> offending) {
        if ( offending.isEmpty() ) {
            return "";
        }
        List<String> sorted = new ArrayList<>( offending );
        sorted.sort( String::compareTo );
        return "Unexpected freemarker usage in:\n" +
            sorted.stream().map( path -> " - " + path ).collect( Collectors.joining( "\n" ) );
    }
}
