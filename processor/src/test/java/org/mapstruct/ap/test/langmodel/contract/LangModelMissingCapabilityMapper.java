/*
 * Copyright MapStruct Authors.
 *
 * Licensed under the Apache License version 2.0, available at http://www.apache.org/licenses/LICENSE-2.0
 */
package org.mapstruct.ap.test.langmodel.contract;

import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

@Mapper
public interface LangModelMissingCapabilityMapper {

    @Mapping(target = "value", source = "value")
    Target map(Source source);

    TargetEnum mapEnum(SourceEnum source);

    class Source {
        public String value;
    }

    class Target {
        private String value;

        public String getValue() {
            return value;
        }

        public void setValue(String value) {
            this.value = value;
        }
    }

    enum SourceEnum {
        FIRST, SECOND
    }

    enum TargetEnum {
        FIRST, SECOND
    }
}
