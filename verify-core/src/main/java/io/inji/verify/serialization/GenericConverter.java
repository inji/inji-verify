package io.inji.verify.serialization;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.module.paramnames.ParameterNamesModule;
import io.inji.verify.exception.SerializationException;
import jakarta.persistence.AttributeConverter;
import jakarta.persistence.Converter;

@Converter
public class GenericConverter<T> implements AttributeConverter<T, String> {

    protected static final ObjectMapper objectMapper = new ObjectMapper();

    static {
        objectMapper.registerModule(new ParameterNamesModule());
    }

    private final Class<T> targetType;

    public GenericConverter(Class<T> targetType) {
        this.targetType = targetType;
    }

    @Override
    public String convertToDatabaseColumn(T attribute) {
        if (attribute == null) {
            return null;
        }
        try {
            return objectMapper.writeValueAsString(attribute);
        } catch (JsonProcessingException e) {
            throw new SerializationException("Error converting " + targetType.getSimpleName() + " to JSON string.");
        }
    }

    @Override
    public T convertToEntityAttribute(String dbData) {
        if (dbData == null || dbData.trim().isEmpty()) {
            return null;
        }
        try {
            return objectMapper.readValue(dbData, targetType);
        } catch (JsonProcessingException e) {
            throw new SerializationException("Error converting JSON string to " + targetType.getSimpleName() + ".");
        }
    }
}