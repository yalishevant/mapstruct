package org.mapstruct.ap.test.descriptor.regression;

import java.util.Arrays;

import org.mapstruct.ap.testutil.ProcessorTest;
import org.mapstruct.ap.testutil.WithClasses;

import static org.assertj.core.api.Assertions.assertThat;

@WithClasses({
    DescriptorCollectionMapper.class,
    DescriptorForgedMapper.class,
    DescriptorBuilderMapper.class,
    DescriptorQualifierMapper.class,
    DescriptorEnumMapper.class
})
public class DescriptorRegressionTest {

    @ProcessorTest
    public void shouldCopyCollectionsWithoutSharingInstances() {
        DescriptorCollectionSource source = new DescriptorCollectionSource();
        source.getTags().addAll( Arrays.asList( "first", "second" ) );

        DescriptorCollectionTarget target = DescriptorCollectionMapper.INSTANCE.map( source );

        assertThat( target.getTags() ).containsExactly( "first", "second" );
        target.getTags().add( "third" );
        assertThat( source.getTags() ).containsExactly( "first", "second" );
    }

    @ProcessorTest
    public void shouldGenerateForgedChildMappings() {
        DescriptorForgedParentDto dto = new DescriptorForgedParentDto();
        dto.getChildren().add( new DescriptorForgedChildDto( "kid" ) );

        DescriptorForgedParent parent = DescriptorForgedMapper.INSTANCE.map( dto );

        assertThat( parent.getChildren() )
            .hasSize( 1 )
            .extracting( DescriptorForgedChild::getLabel )
            .containsExactly( "kid" );
    }

    @ProcessorTest
    public void shouldDiscoverBuilderTargets() {
        DescriptorBuilderAddressSource addressSource = new DescriptorBuilderAddressSource( "Amsterdam" );
        DescriptorBuilderSource source = new DescriptorBuilderSource();
        source.setName( "Unit" );
        source.setAddress( addressSource );

        DescriptorBuilderTarget target = DescriptorBuilderMapper.INSTANCE.map( source );

        assertThat( target.getName() ).isEqualTo( "Unit" );
        assertThat( target.getAddress() ).isNotNull();
        assertThat( target.getAddress().getCity() ).isEqualTo( "Amsterdam" );
    }

    @ProcessorTest
    public void shouldRespectQualifiers() {
        DescriptorQualifierSource source = new DescriptorQualifierSource( "mapper" );

        DescriptorQualifierTarget target = DescriptorQualifierMapper.INSTANCE.map( source );

        assertThat( target.getNickname() ).isEqualTo( "MAPPER" );
    }

    @ProcessorTest
    public void shouldMapEnums() {
        assertThat( DescriptorEnumMapper.INSTANCE.map( DescriptorSourceStatus.UNSPECIFIED ) )
            .isEqualTo( DescriptorTargetStatus.DEFAULTED );
        assertThat( DescriptorEnumMapper.INSTANCE.map( DescriptorSourceStatus.BLOCKED ) )
            .isEqualTo( DescriptorTargetStatus.ACTIVE );
    }
}
