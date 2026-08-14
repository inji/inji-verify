package io.inji.verify.serialization;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.type.CollectionType;
import com.fasterxml.jackson.module.paramnames.ParameterNamesModule;

import io.inji.verify.exception.SerializationException;
import jakarta.persistence.AttributeConverter;
import jakarta.persistence.Converter;

import java.util.List;

@Converter
public class ListConverter<T> implements AttributeConverter<List<T>, String> {

    protected static final ObjectMapper objectMapper = new ObjectMapper();

    static {
        objectMapper.registerModule(new ParameterNamesModule());
    }


    private final CollectionType TYPE_REFERENCE;


    public ListConverter(Class<T> targetType) {
        this.TYPE_REFERENCE = objectMapper.getTypeFactory().constructCollectionType(List.class, targetType);
    }


    @Override
    public String convertToDatabaseColumn(List<T> attribute) {
        if (attribute == null) {
            return null;
        }
        try {
            return objectMapper.writeValueAsString(attribute);
        } catch (JsonProcessingException e) {
            throw new SerializationException("Error converting List to JSON string.");
        }
    }

    @Override
    public List<T> convertToEntityAttribute(String dbData) {
        if (dbData == null || dbData.trim().isEmpty()) {
            return null;
        }
        try {
            return objectMapper.readValue(dbData, TYPE_REFERENCE);
        } catch (JsonProcessingException e) {
            throw new SerializationException("Error converting JSON string to List.");
        }
    }
}
