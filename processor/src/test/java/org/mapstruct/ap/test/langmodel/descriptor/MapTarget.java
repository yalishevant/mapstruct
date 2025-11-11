/*
 * Copyright MapStruct Authors.
 *
 * Licensed under the Apache License version 2.0, available at http://www.apache.org/licenses/LICENSE-2.0
 */
package org.mapstruct.ap.test.langmodel.descriptor;

import java.util.Map;

public class MapTarget {

    private Map<String, PersonDto> registry;

    public Map<String, PersonDto> getRegistry() {
        return registry;
    }

    public void setRegistry(Map<String, PersonDto> registry) {
        this.registry = registry;
    }
}
