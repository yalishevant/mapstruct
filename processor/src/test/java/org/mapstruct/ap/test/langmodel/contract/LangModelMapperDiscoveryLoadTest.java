/*
 * Copyright MapStruct Authors.
 *
 * Licensed under the Apache License version 2.0, available at http://www.apache.org/licenses/LICENSE-2.0
 */
package org.mapstruct.ap.test.langmodel.contract;

import org.junit.jupiter.api.AfterEach;
import org.mapstruct.ap.test.langmodel.contract.discovery.DiscoveryMapper;
import org.mapstruct.ap.test.langmodel.contract.discovery.DiscoveryMapperConfig;
import org.mapstruct.ap.test.langmodel.contract.discovery.DiscoverySource;
import org.mapstruct.ap.test.langmodel.contract.discovery.DiscoveryTarget;
import org.mapstruct.ap.test.langmodel.contract.discovery.SecondaryDiscoveryMapper;
import org.mapstruct.ap.testutil.ProcessorTest;
import org.mapstruct.ap.testutil.WithClasses;
import org.mapstruct.ap.testutil.WithServiceImplementation;
import org.mapstruct.ap.testutil.compilation.annotation.ProcessorOption;
import org.mapstruct.ap.testutil.langmodel.CountingLangModelContextFactory;

import static org.assertj.core.api.Assertions.assertThat;

@WithClasses({
    DiscoveryMapper.class,
    DiscoveryMapperConfig.class,
    SecondaryDiscoveryMapper.class,
    DiscoverySource.class,
    DiscoveryTarget.class
})
@WithServiceImplementation( CountingLangModelContextFactory.class )
class LangModelMapperDiscoveryLoadTest {

    @AfterEach
    void resetCounter() {
        CountingLangModelContextFactory.reset();
    }

    @ProcessorTest
    @ProcessorOption(name = "mapstruct.langModelBackend", value = "counting")
    void createsLangModelContextOnlyForProcessedMappers() {
        DiscoverySource source = new DiscoverySource();
        source.setValue( "value" );

        DiscoveryTarget primary = DiscoveryMapper.INSTANCE.map( source );
        DiscoveryTarget secondary = SecondaryDiscoveryMapper.INSTANCE.map( source );

        assertThat( primary.getValue() ).isEqualTo( "value" );
        assertThat( secondary.getValue() ).isEqualTo( "value" );

        assertThat( CountingLangModelContextFactory.createdContexts() )
            .as( "LangModelContextFactory#create should run once per mapper" )
            .isEqualTo( 2 );
    }
}
