/*
 * Copyright MapStruct Authors.
 *
 * Licensed under the Apache License version 2.0, available at http://www.apache.org/licenses/LICENSE-2.0
 */
package org.mapstruct.ap.test.langmodel.descriptor;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Stream;

import org.junit.jupiter.api.extension.RegisterExtension;
import org.mapstruct.ap.testutil.ProcessorTest;
import org.mapstruct.ap.testutil.WithClasses;
import org.mapstruct.ap.testutil.runner.GeneratedSource;
import org.mapstruct.factory.Mappers;

import static org.assertj.core.api.Assertions.assertThat;

/**
     * Regression coverage for descriptor-first type handling in complex scenarios (wildcards, collections, builders).
     */
class DescriptorInteropTest {

    @RegisterExtension
    final GeneratedSource generatedSource = new GeneratedSource();

    @ProcessorTest
    @WithClasses({
        WildcardDescriptorMapper.class,
        WildcardSource.class,
        WildcardTarget.class,
        Person.class,
        Employee.class,
        PersonDto.class
    })
    void mapsWildcardCollections() {
        WildcardSource source = new WildcardSource();
        source.setPeople( Collections.singletonList( new Employee( "Alice" ) ) );

        WildcardDescriptorMapper mapper = Mappers.getMapper( WildcardDescriptorMapper.class );

        WildcardTarget target = mapper.map( source );

        assertThat( target.getPeople() )
            .extracting( PersonDto::getName )
            .containsExactly( "Alice" );
    }

    @ProcessorTest
    @WithClasses({
        MapDescriptorMapper.class,
        MapSource.class,
        MapTarget.class,
        Person.class,
        Employee.class,
        PersonDto.class
    })
    void mapsWildcardValuesInMap() {
        MapSource source = new MapSource();
        Map<String, Employee> registry = new HashMap<>();
        registry.put( "id-1", new Employee( "Bob" ) );
        source.setRegistry( registry );

        MapDescriptorMapper mapper = Mappers.getMapper( MapDescriptorMapper.class );

        MapTarget target = mapper.map( source );

        assertThat( target.getRegistry() ).hasSize( 1 );
        PersonDto mapped = target.getRegistry().get( "id-1" );
        assertThat( mapped ).isNotNull();
        assertThat( mapped.getName() ).isEqualTo( "Bob" );
    }

    @ProcessorTest
    @WithClasses({
        BuilderDescriptorMapper.class,
        Report.class,
        ReportSource.class,
        Person.class,
        Employee.class,
        PersonDto.class
    })
    void mapsIntoBuilderTarget() {
        ReportSource source = new ReportSource();
        source.setAttendees( Arrays.asList( new Person( "Carol" ), new Employee( "Dave" ) ) );

        BuilderDescriptorMapper mapper = Mappers.getMapper( BuilderDescriptorMapper.class );

        Report report = mapper.map( source );

        assertThat( report.getAttendees() )
            .extracting( PersonDto::getName )
            .containsExactly( "Carol", "Dave" );
    }

    @ProcessorTest
    @WithClasses({
        NestedWildcardCollectionsMapper.class,
        NestedWildcardCollectionsSource.class,
        NestedWildcardCollectionsTarget.class,
        Person.class,
        Employee.class,
        PersonDto.class
    })
    void mapsNestedWildcardCollections() {
        NestedWildcardCollectionsSource source = new NestedWildcardCollectionsSource();
        List<List<? extends Person>> groups = new ArrayList<>();
        groups.add( Arrays.asList( new Employee( "Alice" ), new Person( "Bob" ) ) );
        groups.add( Collections.singletonList( new Employee( "Carol" ) ) );
        source.setGroups( groups );

        NestedWildcardCollectionsMapper mapper = Mappers.getMapper( NestedWildcardCollectionsMapper.class );

        NestedWildcardCollectionsTarget target = mapper.map( source );

        List<List<PersonDto>> mappedGroups = target.getGroups();
        assertThat( mappedGroups ).hasSize( 2 );
        assertThat( mappedGroups.get( 0 ) )
            .extracting( PersonDto::getName )
            .containsExactly( "Alice", "Bob" );
        assertThat( mappedGroups.get( 1 ) )
            .extracting( PersonDto::getName )
            .containsExactly( "Carol" );
    }

    @ProcessorTest
    @WithClasses({
        NestedArrayCollectionsMapper.class,
        NestedArrayCollectionsSource.class,
        NestedArrayCollectionsTarget.class,
        Person.class,
        Employee.class,
        PersonDto.class
    })
    void mapsNestedArrayCollections() {
        NestedArrayCollectionsSource source = new NestedArrayCollectionsSource();
        List<Person[]> matrix = new ArrayList<>();
        matrix.add( new Person[] { new Person( "Alice" ), new Employee( "Bob" ) } );
        matrix.add( new Person[] { new Employee( "Carol" ), new Person( "Dave" ) } );
        source.setMatrix( matrix );

        NestedArrayCollectionsMapper mapper = Mappers.getMapper( NestedArrayCollectionsMapper.class );

        NestedArrayCollectionsTarget target = mapper.map( source );

        List<PersonDto[]> mappedMatrix = target.getMatrix();
        assertThat( mappedMatrix ).hasSize( 2 );
        assertThat( mappedMatrix.get( 0 ) )
            .extracting( PersonDto::getName )
            .containsExactly( "Alice", "Bob" );
        assertThat( mappedMatrix.get( 1 ) )
            .extracting( PersonDto::getName )
            .containsExactly( "Carol", "Dave" );
    }

    @ProcessorTest
    @WithClasses({
        DecoratorDescriptorMapper.class,
        DescriptorMapperDecorator.class,
        Person.class,
        PersonDto.class
    })
    void appliesMapperDecorator() {
        DecoratorDescriptorMapper mapper = Mappers.getMapper( DecoratorDescriptorMapper.class );

        PersonDto dto = mapper.map( new Person( "Alice" ) );

        assertThat( dto ).isNotNull();
        assertThat( dto.getName() ).isEqualTo( "Alice (decorated)" );
    }

    @ProcessorTest
    @WithClasses({
        StreamDescriptorMapper.class,
        Employee.class,
        Person.class,
        PersonDto.class
    })
    void mapsJavaStreams() {
        StreamDescriptorMapper mapper = Mappers.getMapper( StreamDescriptorMapper.class );

        List<PersonDto> result = mapper.map(
            Stream.of( new Person( "Alice" ), new Employee( "Bob" ), new Person( "Carol" ) )
        );

        assertThat( result )
            .extracting( PersonDto::getName )
            .containsExactly( "Alice", "Bob", "Carol" );
    }

    @ProcessorTest
    @WithClasses({
        SealedDescriptorMapper.class,
        Shape.class,
        ShapeDto.class
    })
    void mapsSealedHierarchies() {
        SealedDescriptorMapper mapper = Mappers.getMapper( SealedDescriptorMapper.class );

        ShapeDto circle = mapper.map( new Circle( 2.5 ) );
        ShapeDto rectangle = mapper.map( new Rectangle( 3.0, 4.0 ) );

        assertThat( circle ).isInstanceOf( CircleDto.class );
        assertThat( ( (CircleDto) circle ).getRadius() ).isEqualTo( 2.5 );
        assertThat( rectangle ).isInstanceOf( RectangleDto.class );
        assertThat( ( (RectangleDto) rectangle ).getWidth() ).isEqualTo( 3.0 );
        assertThat( ( (RectangleDto) rectangle ).getHeight() ).isEqualTo( 4.0 );
    }
}
