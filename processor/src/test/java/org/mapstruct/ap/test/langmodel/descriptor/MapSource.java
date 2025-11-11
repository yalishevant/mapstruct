/*
 * Copyright MapStruct Authors.
 *
 * Licensed under the Apache License version 2.0, available at http://www.apache.org/licenses/LICENSE-2.0
 */
package org.mapstruct.ap.test.langmodel.descriptor;

import java.util.Map;

public class MapSource {

    private Map<String, ? extends Person> registry;

    public Map<String, ? extends Person> getRegistry() {
        return registry;
    }

    public void setRegistry(Map<String, ? extends Person> registry) {
        this.registry = registry;
    }
}
