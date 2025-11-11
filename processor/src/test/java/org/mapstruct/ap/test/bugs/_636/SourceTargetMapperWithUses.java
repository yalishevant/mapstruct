/*
 * Copyright MapStruct Authors.
 *
 * Licensed under the Apache License version 2.0, available at http://www.apache.org/licenses/LICENSE-2.0
 */
package org.mapstruct.ap.test.bugs._636;

import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.Mappings;
import org.mapstruct.factory.Mappers;

@Mapper(uses = SourceTargetBaseMapper.class)
public interface SourceTargetMapperWithUses {

    SourceTargetMapperWithUses INSTANCE = Mappers.getMapper( SourceTargetMapperWithUses.class );

    @Mappings({
        @Mapping(target = "foo", source = "idFoo"),
        @Mapping(target = "bar", source = "idBar")
    })
    Target mapSourceToTarget(Source source);
}
