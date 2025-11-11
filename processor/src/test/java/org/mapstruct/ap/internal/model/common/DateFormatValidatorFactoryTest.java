/*
 * Copyright MapStruct Authors.
 *
 * Licensed under the Apache License version 2.0, available at http://www.apache.org/licenses/LICENSE-2.0
 */
package org.mapstruct.ap.internal.model.common;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.ZonedDateTime;
import java.util.HashMap;

import org.junit.jupiter.api.Test;
import org.mapstruct.ap.internal.util.JodaTimeConstants;
import org.mapstruct.ap.testutil.IssueKey;

/**
 * Tests for {@link org.mapstruct.ap.internal.model.common.DateFormatValidatorFactory}.
 *
 * @author Timo Eckhardt
 */
@IssueKey( "224" )
public class DateFormatValidatorFactoryTest {

    private static final String JAVA_LANG_STRING = "java.lang.String";

    @Test
    public void testUnsupportedTypes() {
        Type sourceType = typeWithFQN( JAVA_LANG_STRING );
        Type targetType = typeWithFQN( JAVA_LANG_STRING );
        DateFormatValidator dateFormatValidator = DateFormatValidatorFactory.forTypes( sourceType, targetType );
        assertThat( dateFormatValidator.validate( "XXXX" ).isValid() ).isTrue();
    }

    @Test
    public void testJavaUtilDateValidator() {

        Type sourceType = typeWithFQN( "java.util.Date" );
        Type targetType = typeWithFQN( JAVA_LANG_STRING );

        assertInvalidDateFormat( sourceType, targetType );
        assertInvalidDateFormat( targetType, sourceType );
        assertValidDateFormat( sourceType, targetType );
        assertValidDateFormat( targetType, sourceType );
    }

    @Test
    public void testJodaTimeValidator() {

        Type targetType = typeWithFQN( JAVA_LANG_STRING );

        Type sourceType = typeWithFQN( JodaTimeConstants.DATE_TIME_FQN );
        assertInvalidDateFormat( sourceType, targetType );
        assertInvalidDateFormat( targetType, sourceType );
        assertValidDateFormat( sourceType, targetType );
        assertValidDateFormat( targetType, sourceType );

        sourceType = typeWithFQN( JodaTimeConstants.LOCAL_DATE_FQN );
        assertInvalidDateFormat( sourceType, targetType );
        assertInvalidDateFormat( targetType, sourceType );
        assertValidDateFormat( sourceType, targetType );
        assertValidDateFormat( targetType, sourceType );

        sourceType = typeWithFQN( JodaTimeConstants.LOCAL_DATE_TIME_FQN );
        assertInvalidDateFormat( sourceType, targetType );
        assertInvalidDateFormat( targetType, sourceType );
        assertValidDateFormat( sourceType, targetType );
        assertValidDateFormat( targetType, sourceType );

        sourceType = typeWithFQN( JodaTimeConstants.LOCAL_TIME_FQN );
        assertInvalidDateFormat( sourceType, targetType );
        assertInvalidDateFormat( targetType, sourceType );
        assertValidDateFormat( sourceType, targetType );
        assertValidDateFormat( targetType, sourceType );
    }

    @Test
    public void testJavaTimeValidator() {

        Type targetType = typeWithFQN( JAVA_LANG_STRING );

        Type sourceType = typeWithFQN( ZonedDateTime.class.getCanonicalName() );
        assertInvalidDateFormat( sourceType, targetType );
        assertInvalidDateFormat( targetType, sourceType );
        assertValidDateFormat( sourceType, targetType );
        assertValidDateFormat( targetType, sourceType );

        sourceType = typeWithFQN( LocalDate.class.getCanonicalName() );
        assertInvalidDateFormat( sourceType, targetType );
        assertInvalidDateFormat( targetType, sourceType );
        assertValidDateFormat( sourceType, targetType );
        assertValidDateFormat( targetType, sourceType );

        sourceType = typeWithFQN( LocalDateTime.class.getCanonicalName() );
        assertInvalidDateFormat( sourceType, targetType );
        assertInvalidDateFormat( targetType, sourceType );
        assertValidDateFormat( sourceType, targetType );
        assertValidDateFormat( targetType, sourceType );

        sourceType = typeWithFQN( LocalTime.class.getCanonicalName() );
        assertInvalidDateFormat( sourceType, targetType );
        assertInvalidDateFormat( targetType, sourceType );
        assertValidDateFormat( sourceType, targetType );
        assertValidDateFormat( targetType, sourceType );
    }

    private void assertInvalidDateFormat(Type sourceType, Type targetType) {
        DateFormatValidator dateFormatValidator = DateFormatValidatorFactory.forTypes( sourceType, targetType );
        DateFormatValidationResult result = dateFormatValidator.validate( "qwertz" );
        assertThat( result.isValid() ).isFalse();
    }

    private void assertValidDateFormat(Type sourceType, Type targetType) {
        DateFormatValidator dateFormatValidator = DateFormatValidatorFactory.forTypes( sourceType, targetType );
        DateFormatValidationResult result = dateFormatValidator.validate( "YYYY" );
        assertThat( result.isValid() ).isTrue();
    }

    private Type typeWithFQN(String fullQualifiedName) {
        String simpleName = fullQualifiedName.contains( "." )
            ? fullQualifiedName.substring( fullQualifiedName.lastIndexOf( '.' ) + 1 )
            : fullQualifiedName;
        return new Type(
            null,
            null,
            null,
            null,
            java.util.Collections.emptyList(),
            null,
            null,
            null,
            simpleName,
            fullQualifiedName,
            false,
            false,
            false,
            false,
            false,
            false,
            new HashMap<>(),
            new HashMap<>(),
            Boolean.FALSE,
            false,
            false,
            null,
            null
        );
    }

}
