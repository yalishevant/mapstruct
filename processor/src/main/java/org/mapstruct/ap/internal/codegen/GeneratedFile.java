/*
 * Copyright MapStruct Authors.
 *
 * Licensed under the Apache License version 2.0, available at http://www.apache.org/licenses/LICENSE-2.0
 */
package org.mapstruct.ap.internal.codegen;

import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

import org.mapstruct.ap.internal.codegen.template.TemplateRenderable;

/**
 * Descriptor of a file that should be produced by the {@link CodeGenerator}.
 */
public final class GeneratedFile {

    public enum Kind {
        JAVA_SOURCE,
        RESOURCE
    }

    private final Kind kind;
    private final String packageName;
    private final String simpleName;
    private final String resourceName;
    private final TemplateRenderable template;
    private final List<Object> originatingElements;
    private final Map<String, Object> attributes;

    private GeneratedFile(Builder builder) {
        this.kind = builder.kind;
        this.packageName = builder.packageName;
        this.simpleName = builder.simpleName;
        this.resourceName = builder.resourceName;
        this.template = builder.template;
        this.originatingElements = Collections.unmodifiableList( new ArrayList<>( builder.originatingElements ) );
        this.attributes = Collections.unmodifiableMap( new LinkedHashMap<>( builder.attributes ) );
    }

    public Kind getKind() {
        return kind;
    }

    public String getPackageName() {
        return packageName;
    }

    public String getSimpleName() {
        return simpleName;
    }

    public String getResourceName() {
        return resourceName;
    }

    public TemplateRenderable getTemplate() {
        return template;
    }

    public List<Object> getOriginatingElements() {
        return originatingElements;
    }

    public Map<String, Object> getAttributes() {
        return attributes;
    }

    public static Builder javaSource(String packageName, String simpleName, TemplateRenderable template) {
        Objects.requireNonNull( simpleName, "simpleName" );
        Objects.requireNonNull( template, "template" );
        return new Builder( Kind.JAVA_SOURCE, template )
            .withPackageName( packageName )
            .withSimpleName( simpleName );
    }

    public static Builder resource(String resourceName, TemplateRenderable template) {
        Objects.requireNonNull( resourceName, "resourceName" );
        Objects.requireNonNull( template, "template" );
        return new Builder( Kind.RESOURCE, template )
            .withResourceName( resourceName );
    }

    public static final class Builder {
        private final Kind kind;
        private final TemplateRenderable template;
        private String packageName;
        private String simpleName;
        private String resourceName;
        private final List<Object> originatingElements = new ArrayList<>();
        private Map<String, Object> attributes = Collections.emptyMap();

        private Builder(Kind kind, TemplateRenderable template) {
            this.kind = kind;
            this.template = template;
        }

        public Builder withPackageName(String packageName) {
            this.packageName = packageName;
            return this;
        }

        public Builder withSimpleName(String simpleName) {
            this.simpleName = simpleName;
            return this;
        }

        public Builder withResourceName(String resourceName) {
            this.resourceName = resourceName;
            return this;
        }

        public Builder addOriginatingElement(Object element) {
            if ( element != null ) {
                originatingElements.add( element );
            }
            return this;
        }

        public Builder withAttributes(Map<String, Object> attributes) {
            if ( attributes == null || attributes.isEmpty() ) {
                this.attributes = Collections.emptyMap();
            }
            else {
                this.attributes = new LinkedHashMap<>( attributes );
            }
            return this;
        }

        public GeneratedFile build() {
            if ( kind == Kind.JAVA_SOURCE && simpleName == null ) {
                throw new IllegalStateException( "Java source file requires a simple name" );
            }
            if ( kind == Kind.RESOURCE && resourceName == null ) {
                throw new IllegalStateException( "Resource file requires a resource name" );
            }
            return new GeneratedFile( this );
        }
    }
}
