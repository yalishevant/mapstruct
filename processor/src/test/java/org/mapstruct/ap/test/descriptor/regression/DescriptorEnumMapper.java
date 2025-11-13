package org.mapstruct.ap.test.descriptor.regression;

import org.mapstruct.Mapper;
import org.mapstruct.MappingConstants;
import org.mapstruct.ValueMapping;
import org.mapstruct.ValueMappings;
import org.mapstruct.factory.Mappers;

@Mapper
public interface DescriptorEnumMapper {

    DescriptorEnumMapper INSTANCE = Mappers.getMapper( DescriptorEnumMapper.class );

    @ValueMappings({
        @ValueMapping(source = "UNSPECIFIED", target = "DEFAULTED"),
        @ValueMapping(source = MappingConstants.ANY_REMAINING, target = "ACTIVE")
    })
    DescriptorTargetStatus map(DescriptorSourceStatus status);
}

enum DescriptorSourceStatus {
    UNSPECIFIED,
    ACTIVE,
    BLOCKED
}

enum DescriptorTargetStatus {
    DEFAULTED,
    ACTIVE
}
