/*
 * Copyright MapStruct Authors.
 *
 * Licensed under the Apache License version 2.0, available at http://www.apache.org/licenses/LICENSE-2.0
 */
package org.mapstruct.ap.test.langmodel.contract.discovery;

import org.mapstruct.Mapper;
import org.mapstruct.factory.Mappers;

@Mapper(config = DiscoveryMapperConfig.class)
public interface DiscoveryMapper {

    DiscoveryMapper INSTANCE = Mappers.getMapper( DiscoveryMapper.class );

    DiscoveryTarget map(DiscoverySource source);
}
