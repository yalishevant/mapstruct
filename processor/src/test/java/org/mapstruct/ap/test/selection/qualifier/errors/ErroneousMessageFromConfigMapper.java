/*
 * Copyright MapStruct Authors.
 *
 * Licensed under the Apache License version 2.0, available at http://www.apache.org/licenses/LICENSE-2.0
 */
package org.mapstruct.ap.test.selection.qualifier.errors;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;

import org.mapstruct.InheritConfiguration;
import org.mapstruct.Mapper;
import org.mapstruct.MapperConfig;
import org.mapstruct.Mapping;
import org.mapstruct.Qualifier;

@Mapper( config = ErroneousMessageFromConfigMapper.Config.class )
public interface ErroneousMessageFromConfigMapper {

    @InheritConfiguration( name = "mapWithQualifier" )
    Target map(Source source);

    default Nested map(String in) {
        return null;
    }

    @MapperConfig
    interface Config {

        @Mapping(target = "nested", source = "value", qualifiedBy = SelectMe.class)
        Target mapWithQualifier(Source source);
    }

    class Source {
        private String value;

        public String getValue() {
            return value;
        }

        public void setValue(String value) {
            this.value = value;
        }
    }

    class Target {
        private Nested nested;

        public Nested getNested() {
            return nested;
        }

        public void setNested(Nested nested) {
            this.nested = nested;
        }
    }

    class Nested {
        private String value;

        public String getValue() {
            return value;
        }

        public void setValue(String value) {
            this.value = value;
        }
    }

    @Qualifier
    @java.lang.annotation.Target(ElementType.METHOD)
    @Retention(RetentionPolicy.CLASS)
    @interface SelectMe {
    }
}
