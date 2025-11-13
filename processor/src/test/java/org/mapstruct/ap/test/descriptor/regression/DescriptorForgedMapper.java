package org.mapstruct.ap.test.descriptor.regression;

import java.util.ArrayList;
import java.util.List;

import org.mapstruct.Mapper;
import org.mapstruct.factory.Mappers;

@Mapper
public interface DescriptorForgedMapper {

    DescriptorForgedMapper INSTANCE = Mappers.getMapper( DescriptorForgedMapper.class );

    DescriptorForgedParent map(DescriptorForgedParentDto source);
}

class DescriptorForgedParentDto {

    private final List<DescriptorForgedChildDto> children = new ArrayList<>();

    public List<DescriptorForgedChildDto> getChildren() {
        return children;
    }
}

class DescriptorForgedParent {

    private List<DescriptorForgedChild> children;

    public List<DescriptorForgedChild> getChildren() {
        return children;
    }

    public void setChildren(List<DescriptorForgedChild> children) {
        this.children = children;
    }
}

class DescriptorForgedChildDto {

    private String label;

    DescriptorForgedChildDto() {
    }

    DescriptorForgedChildDto(String label) {
        this.label = label;
    }

    public String getLabel() {
        return label;
    }

    public void setLabel(String label) {
        this.label = label;
    }
}

class DescriptorForgedChild {

    private String label;

    public String getLabel() {
        return label;
    }

    public void setLabel(String label) {
        this.label = label;
    }
}
