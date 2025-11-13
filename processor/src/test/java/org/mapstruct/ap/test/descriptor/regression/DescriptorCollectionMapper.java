package org.mapstruct.ap.test.descriptor.regression;

import java.util.ArrayList;
import java.util.List;

import org.mapstruct.Mapper;
import org.mapstruct.factory.Mappers;

@Mapper
public interface DescriptorCollectionMapper {

    DescriptorCollectionMapper INSTANCE = Mappers.getMapper( DescriptorCollectionMapper.class );

    DescriptorCollectionTarget map(DescriptorCollectionSource source);
}

class DescriptorCollectionSource {

    private List<String> tags = new ArrayList<>();

    public List<String> getTags() {
        return tags;
    }

    public void setTags(List<String> tags) {
        this.tags = tags;
    }
}

class DescriptorCollectionTarget {

    private List<String> tags;

    public List<String> getTags() {
        return tags;
    }

    public void setTags(List<String> tags) {
        this.tags = tags;
    }
}
