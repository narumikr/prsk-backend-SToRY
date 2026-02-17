package com.example.untitled.prskmusic.converter;

import com.example.untitled.prskmusic.enums.MusicType;
import jakarta.persistence.AttributeConverter;
import jakarta.persistence.Converter;

@Converter(autoApply = true)
public class MusicTypeConverter implements AttributeConverter<MusicType, Integer> {

    @Override
    public Integer convertToDatabaseColumn(MusicType attribute) {
        return (attribute == null) ? null : attribute.getCode();
    }

    @Override
    public MusicType convertToEntityAttribute(Integer dbData) {
        return (dbData == null) ? null : MusicType.fromCode(dbData);
    }
}
