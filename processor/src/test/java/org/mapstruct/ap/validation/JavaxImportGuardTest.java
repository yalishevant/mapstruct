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
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.junit.jupiter.api.Test;

/**
 * Guard that ensures no additional classes in {@code org.mapstruct.ap.internal} start depending on
 * {@code javax.lang.model}. The allow list captures the current technical debt; any newly introduced usage must be
 * justified explicitly and added here.
 */
class JavaxImportGuardTest {

    private static final Path INTERNAL_ROOT =
        Paths.get( "src", "main", "java", "org", "mapstruct", "ap", "internal" );

    private static final Set<String> ALLOW_LIST = Collections.emptySet();

    @Test
    void noNewJavaxLangModelImportsInInternalPackage() throws IOException {
        Set<String> offending = new HashSet<>();
        try ( Stream<Path> files = Files.walk( INTERNAL_ROOT ) ) {
            files.filter( path -> path.toString().endsWith( ".java" ) )
                .filter( JavaxImportGuardTest::containsJavaxLangModel )
                .map( INTERNAL_ROOT::relativize )
                .map( JavaxImportGuardTest::normalize )
                .filter( path -> !ALLOW_LIST.contains( path ) )
                .forEach( offending::add );
        }

        assertThat( offending )
            .as( () -> formatOffending( offending ) )
            .isEmpty();
    }

    private static boolean containsJavaxLangModel(Path file) {
        try {
            return Files.readAllLines( file, StandardCharsets.UTF_8 )
                .stream()
                .anyMatch( line -> line.contains( "javax.lang.model" ) );
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
        return "Unexpected javax.lang.model usage in:\n" +
            sorted.stream().map( path -> " - " + path ).collect( Collectors.joining( "\n" ) );
    }
}
