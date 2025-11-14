/*
 * Copyright MapStruct Authors.
 *
 * Licensed under the Apache License version 2.0, available at http://www.apache.org/licenses/LICENSE-2.0
 */
package org.mapstruct.ap.internal.model.source;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;

import org.junit.jupiter.api.Test;
import org.mapstruct.ap.internal.langmodel.descriptor.LangTypeKind;
import org.mapstruct.ap.internal.langmodel.descriptor.TypeDescriptor;
import org.mapstruct.ap.internal.langmodel.descriptor.TypeElementDescriptor;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Unit tests for {@link SelectionParameters}.
 */
class SelectionParametersTest {

    @Test
    void gettersExposeConstructorValues() {
        SelectionParameters parameters = parameters(
            Arrays.asList( "org.mapstruct.test.SomeQualifier", "org.mapstruct.test.OtherQualifier" ),
            Arrays.asList( "language", "german" ),
            "resultType"
        );

        assertThat( parameters.getQualifyingNames() ).containsExactly( "language", "german" );
        assertThat( descriptorsToIds( parameters.getQualifiers() ) )
            .containsExactly( "org.mapstruct.test.SomeQualifier", "org.mapstruct.test.OtherQualifier" );
        assertThat( parameters.getResultType().id() ).isEqualTo( "resultType" );
    }

    @Test
    void hashCodeIncludesNamesAndResultType() {
        SelectionParameters parameters = parameters(
            Collections.singletonList( "Qualifier" ),
            Collections.singletonList( "german" ),
            "resultType"
        );

        int expectedHash = 3;
        expectedHash = 97 * expectedHash + Collections.singletonList( "german" ).hashCode();
        expectedHash = 97 * expectedHash + "resultType".hashCode();

        assertThat( parameters.hashCode() ).isEqualTo( expectedHash );
    }

    @Test
    void equalsRespectsQualifiersAndNames() {
        SelectionParameters left = parameters(
            Arrays.asList( "QualifierA", "QualifierB" ),
            Arrays.asList( "foo", "bar" ),
            "result"
        );

        SelectionParameters identical = parameters(
            Arrays.asList( "QualifierA", "QualifierB" ),
            Arrays.asList( "foo", "bar" ),
            "result"
        );

        SelectionParameters differentQualifiers = parameters(
            Arrays.asList( "QualifierA" ),
            Arrays.asList( "foo", "bar" ),
            "result"
        );

        SelectionParameters differentNames = parameters(
            Arrays.asList( "QualifierA", "QualifierB" ),
            Collections.singletonList( "foo" ),
            "result"
        );

        SelectionParameters differentResult = parameters(
            Arrays.asList( "QualifierA", "QualifierB" ),
            Arrays.asList( "foo", "bar" ),
            "otherResult"
        );

        assertThat( left ).isEqualTo( identical );
        assertThat( left ).isNotEqualTo( differentQualifiers );
        assertThat( left ).isNotEqualTo( differentNames );
        assertThat( left ).isNotEqualTo( differentResult );
        assertThat( left ).isNotEqualTo( null );
        assertThat( left ).isNotEqualTo( "not-selection-parameters" );
    }

    private static SelectionParameters parameters(List<String> qualifierIds,
                                                  List<String> qualifierNames,
                                                  String resultId) {
        List<TypeDescriptor> qualifiers = qualifierIds != null
            ? qualifierIds.stream().map( SelectionParametersTest::descriptor ).collect( Collectors.toList() )
            : null;

        TypeDescriptor resultType = resultId != null ? descriptor( resultId ) : null;

        return new SelectionParameters(
            qualifiers,
            qualifierNames,
            Collections.emptyList(),
            Collections.emptyList(),
            resultType,
            null
        );
    }

    private static List<String> descriptorsToIds(List<TypeDescriptor> descriptors) {
        return descriptors.stream().map( TypeDescriptor::id ).collect( Collectors.toList() );
    }

    private static TypeDescriptor descriptor(String id) {
        return new TestTypeDescriptor( id );
    }

    /**
     * Minimal {@link TypeDescriptor} implementation for exercising {@link SelectionParameters}.
     */
    private static final class TestTypeDescriptor implements TypeDescriptor {

        private final String id;

        private TestTypeDescriptor(String id) {
            this.id = Objects.requireNonNull( id );
        }

        @Override
        public String id() {
            return id;
        }

        @Override
        public LangTypeKind kind() {
            return LangTypeKind.DECLARED;
        }

        @Override
        public String displayName() {
            return id;
        }

        @Override
        public java.util.Optional<String> qualifiedName() {
            return java.util.Optional.of( id );
        }

        @Override
        public java.util.Optional<String> packageName() {
            return java.util.Optional.empty();
        }

        @Override
        public java.util.Optional<TypeElementDescriptor> typeElement() {
            return java.util.Optional.empty();
        }

        @Override
        public java.util.Optional<TypeDescriptor> componentType() {
            return java.util.Optional.empty();
        }

        @Override
        public List<TypeDescriptor> typeArguments() {
            return Collections.emptyList();
        }

        @Override
        public boolean isPrimitive() {
            return false;
        }

        @Override
        public boolean isVoid() {
            return false;
        }

        @Override
        public boolean isEnum() {
            return false;
        }

        @Override
        public boolean isInterface() {
            return false;
        }

        @Override
        public java.util.Optional<TypeDescriptor> wildcardExtendsBound() {
            return java.util.Optional.empty();
        }

        @Override
        public java.util.Optional<TypeDescriptor> wildcardSuperBound() {
            return java.util.Optional.empty();
        }

        @Override
        public java.util.Optional<String> typeVariableName() {
            return java.util.Optional.empty();
        }

        @Override
        public List<TypeDescriptor> typeVariableBounds() {
            return Collections.emptyList();
        }

        @Override
        public TypeDescriptor erasure() {
            return this;
        }

        @Override
        public Object unwrap() {
            return null;
        }

        @Override
        public int compareTo(TypeDescriptor other) {
            return id.compareTo( other.id() );
        }
    }
}
