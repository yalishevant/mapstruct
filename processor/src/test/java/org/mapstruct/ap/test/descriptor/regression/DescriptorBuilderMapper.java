package org.mapstruct.ap.test.descriptor.regression;

import org.mapstruct.Mapper;
import org.mapstruct.factory.Mappers;

@Mapper
public interface DescriptorBuilderMapper {

    DescriptorBuilderMapper INSTANCE = Mappers.getMapper( DescriptorBuilderMapper.class );

    DescriptorBuilderTarget map(DescriptorBuilderSource source);
}

class DescriptorBuilderSource {

    private String name;
    private DescriptorBuilderAddressSource address;

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public DescriptorBuilderAddressSource getAddress() {
        return address;
    }

    public void setAddress(DescriptorBuilderAddressSource address) {
        this.address = address;
    }
}

class DescriptorBuilderAddressSource {

    private String city;

    DescriptorBuilderAddressSource() {
    }

    DescriptorBuilderAddressSource(String city) {
        this.city = city;
    }

    public String getCity() {
        return city;
    }

    public void setCity(String city) {
        this.city = city;
    }
}

class DescriptorBuilderTarget {

    private final String name;
    private final DescriptorBuilderAddress address;

    DescriptorBuilderTarget(String name, DescriptorBuilderAddress address) {
        this.name = name;
        this.address = address;
    }

    public String getName() {
        return name;
    }

    public DescriptorBuilderAddress getAddress() {
        return address;
    }

    public static Builder builder() {
        return new Builder();
    }

    static class Builder {

        private String name;
        private DescriptorBuilderAddress address;

        Builder withName(String name) {
            this.name = name;
            return this;
        }

        Builder withAddress(DescriptorBuilderAddress address) {
            this.address = address;
            return this;
        }

        DescriptorBuilderTarget build() {
            return new DescriptorBuilderTarget( name, address );
        }
    }
}

class DescriptorBuilderAddress {

    private String city;

    public String getCity() {
        return city;
    }

    public void setCity(String city) {
        this.city = city;
    }
}
