/*
 * Copyright MapStruct Authors.
 *
 * Licensed under the Apache License version 2.0, available at http://www.apache.org/licenses/LICENSE-2.0
 */
package org.mapstruct.ap.internal.codegen;

import java.util.HashMap;
import java.util.Map;

import org.junit.jupiter.api.Test;
import org.mapstruct.ap.internal.codegen.template.TemplateRenderable;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class GeneratedFileBuilderTest {

    @Test
    void copiesAttributesDefensively() {
        Map<String, Object> attributes = new HashMap<>();
        attributes.put( "foo", "bar" );

        GeneratedFile.Builder builder = GeneratedFile
            .javaSource( "org.example", "TestMapper", new StubRenderable() )
            .withAttributes( attributes );

        attributes.put( "foo", "mutated" );

        GeneratedFile generatedFile = builder.build();

        assertThat( generatedFile.getAttributes() )
            .containsEntry( "foo", "bar" );
        assertThatThrownBy( () -> generatedFile.getAttributes().put( "foo", "baz" ) )
            .isInstanceOf( UnsupportedOperationException.class );
    }

    private static final class StubRenderable extends TemplateRenderable {
    }
}
