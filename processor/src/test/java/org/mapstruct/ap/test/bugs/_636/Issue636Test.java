/*
 * Copyright MapStruct Authors.
 *
 * Licensed under the Apache License version 2.0, available at http://www.apache.org/licenses/LICENSE-2.0
 */
package org.mapstruct.ap.test.bugs._636;

import org.mapstruct.ap.testutil.IssueKey;
import org.mapstruct.ap.testutil.ProcessorTest;
import org.mapstruct.ap.testutil.WithClasses;

import static org.assertj.core.api.Assertions.assertThat;

@IssueKey("636")
@WithClasses({
    SourceTargetMapper.class,
    SourceTargetMapperWithUses.class,
    SourceTargetBaseMapper.class,
    Source.class,
    Target.class,
    Foo.class,
    Bar.class
})
class Issue636Test {

    @ProcessorTest
    void mapsUsingStaticFactoryMethodFromParentInterface() {
        Source source = new Source( 7L, "bar-7" );

        Target target = SourceTargetMapper.INSTANCE.mapSourceToTarget( source );

        assertThat( target.getFoo() ).extracting( Foo::getId ).isEqualTo( 7L );
        assertThat( target.getBar() ).extracting( Bar::getId ).isEqualTo( "bar-7" );
    }

    @ProcessorTest
    void mapsUsingStaticFactoryViaUsesReference() {
        Source source = new Source( 11L, "bar-11" );

        Target target = SourceTargetMapperWithUses.INSTANCE.mapSourceToTarget( source );

        assertThat( target.getFoo() ).extracting( Foo::getId ).isEqualTo( 11L );
        assertThat( target.getBar() ).extracting( Bar::getId ).isEqualTo( "bar-11" );
    }
}
