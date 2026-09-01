package io.inji.verify.dto.submission;

import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.core.JsonToken;
import com.fasterxml.jackson.databind.DeserializationContext;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.JsonDeserializer;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

import java.io.IOException;

/**
 * Binds a JSON value to {@link JsonNode} but rejects duplicate object keys
 * ({@link DeserializationFeature#FAIL_ON_READING_DUP_TREE_KEY}) so they are not silently collapsed.
 */
public class FailOnDuplicateKeyJsonNodeDeserializer extends JsonDeserializer<JsonNode> {

    @Override
    public JsonNode deserialize(JsonParser p, DeserializationContext ctxt) throws IOException {
        if (p.currentToken() == JsonToken.VALUE_NULL) {
            return null;
        }
        ObjectMapper mapper = p.getCodec() instanceof ObjectMapper objectMapper
                ? objectMapper.copy()
                : new ObjectMapper();
        mapper.enable(DeserializationFeature.FAIL_ON_READING_DUP_TREE_KEY);
        return mapper.readTree(p);
    }
}
