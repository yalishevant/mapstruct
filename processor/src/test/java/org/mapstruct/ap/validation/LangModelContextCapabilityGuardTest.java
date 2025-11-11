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
import java.util.List;
import java.util.regex.Pattern;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.junit.jupiter.api.Test;

/**
 * Guard tests for {@code org.mapstruct.ap.internal} to enforce thin
 * {@link org.mapstruct.ap.langmodel.LangModelContext} usage.
 * The checks mirror the shell-based controls defined in the milestone roadmap
 * and prevent regressions when Kotlin backends rely on the minimal facade set.
 */
class LangModelContextCapabilityGuardTest {

    private static final Path INTERNAL_ROOT =
        Paths.get( "src", "main", "java", "org", "mapstruct", "ap", "internal" );

    private static final Pattern OPTIONAL_CAPABILITY_PATTERN =
        Pattern.compile( "\\bOptionalCapability\\b" );

    private static final Pattern LEGACY_CONTEXT_ACCESS_PATTERN =
        Pattern.compile( "LangModelContext\\.(annotationGems|mappingExclusion|builderIntrospector|enumMapping)" );

    private static final Pattern ANNOTATION_PROCESSOR_CONTEXT_INSTANCEOF_PATTERN =
        Pattern.compile( "instanceof\\s+\\w*AnnotationProcessorContext" );

    @Test
    void noDirectOptionalCapabilityUsageInInternalPackage() throws IOException {
        List<String> offenders = findFilesMatching( OPTIONAL_CAPABILITY_PATTERN );
        assertEmpty( offenders, "OptionalCapability must not be referenced directly inside org.mapstruct.ap.internal" );
    }

    @Test
    void noLegacyLangModelContextShortcutInvocations() throws IOException {
        List<String> offenders = findFilesMatching( LEGACY_CONTEXT_ACCESS_PATTERN );
        assertEmpty(
            offenders,
            "LangModelContext optional accessors (annotationGems/mappingExclusion/...) must be requested through "
                + "LangModelContext.optional(Class)"
        );
    }

    @Test
    void noAnnotationProcessorContextInstanceofChecks() throws IOException {
        List<String> offenders = findFilesMatching( ANNOTATION_PROCESSOR_CONTEXT_INSTANCEOF_PATTERN );
        assertEmpty(
            offenders,
            "AnnotationProcessorContext downcasts are forbidden — obtain data via LangModelContext.optional(Class)"
        );
    }

    private static List<String> findFilesMatching(Pattern pattern) throws IOException {
        try ( Stream<Path> files = Files.walk( INTERNAL_ROOT ) ) {
            return files
                .filter( Files::isRegularFile )
                .filter( path -> path.toString().endsWith( ".java" ) )
                .filter( path -> fileContains( path, pattern ) )
                .map( INTERNAL_ROOT::relativize )
                .map( LangModelContextCapabilityGuardTest::normalize )
                .collect( Collectors.toList() );
        }
    }

    private static boolean fileContains(Path file, Pattern pattern) {
        try {
            for ( String line : Files.readAllLines( file, StandardCharsets.UTF_8 ) ) {
                if ( pattern.matcher( line ).find() ) {
                    return true;
                }
            }
            return false;
        }
        catch ( IOException e ) {
            throw new IllegalStateException( "Failed to read " + file, e );
        }
    }

    private static String normalize(Path relativePath) {
        return relativePath.toString().replace( '\\', '/' );
    }

    private static void assertEmpty(List<String> offenders, String message) {
        if ( offenders.isEmpty() ) {
            return;
        }
        List<String> sorted = new ArrayList<>( offenders );
        Collections.sort( sorted );
        String details = sorted.stream()
            .map( path -> " - " + path )
            .collect( Collectors.joining( System.lineSeparator() ) );
        assertThat( offenders )
            .as( () -> message + ":\n" + details )
            .isEmpty();
    }
}
