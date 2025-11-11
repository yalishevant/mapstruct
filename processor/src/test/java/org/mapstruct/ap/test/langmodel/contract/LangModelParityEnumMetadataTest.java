/*
 * Copyright MapStruct Authors.
 *
 * Licensed under the Apache License version 2.0, available at http://www.apache.org/licenses/LICENSE-2.0
 */
package org.mapstruct.ap.test.langmodel.contract;

import org.junit.jupiter.api.Test;

class LangModelParityEnumMetadataTest {

    @Test
    void enumMetadataParityMatchesJavaxBehaviour() throws Exception {
        LangModelParityTestSupport.runParityCheck( "enum" );
    }
}
