package org.mapstruct.ap.test.descriptor.regression;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;
import java.util.Locale;

import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.Qualifier;
import org.mapstruct.factory.Mappers;

@Mapper
public interface DescriptorQualifierMapper {

    DescriptorQualifierMapper INSTANCE = Mappers.getMapper( DescriptorQualifierMapper.class );

    @Mapping(target = "nickname", qualifiedBy = DescriptorUpperCaseQualifier.class)
    DescriptorQualifierTarget map(DescriptorQualifierSource source);

    @DescriptorUpperCaseQualifier
    default String uppercase(String value) {
        return value == null ? null : value.toUpperCase( Locale.ROOT );
    }
}

@Qualifier
@Target(ElementType.METHOD)
@Retention(RetentionPolicy.CLASS)
@interface DescriptorUpperCaseQualifier {
}

class DescriptorQualifierSource {

    private String nickname;

    DescriptorQualifierSource() {
    }

    DescriptorQualifierSource(String nickname) {
        this.nickname = nickname;
    }

    public String getNickname() {
        return nickname;
    }

    public void setNickname(String nickname) {
        this.nickname = nickname;
    }
}

class DescriptorQualifierTarget {

    private String nickname;

    public String getNickname() {
        return nickname;
    }

    public void setNickname(String nickname) {
        this.nickname = nickname;
    }
}
