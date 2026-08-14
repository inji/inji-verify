package io.inji.verify.serialization;


import io.inji.verify.exception.SerializationException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

public class GenericConverterTest {

    private GenericConverter<TestObjectA> converterA;
    private GenericConverter<TestObjectB> converterB;

    @BeforeEach
    void setUp() {
        converterA = new GenericConverter<>(TestObjectA.class);
        converterB = new GenericConverter<>(TestObjectB.class);
    }


    @Test
    void testConvertToDatabaseColumn_TestObjectA() {
        TestObjectA obj = new TestObjectA("MyItem", 123);
        String json = converterA.convertToDatabaseColumn(obj);

        assertNotNull(json);
        assertTrue(json.contains("\"name\":\"MyItem\""));
        assertTrue(json.contains("\"value\":123"));
        assertTrue(json.startsWith("{") && json.endsWith("}"));
    }

    @Test
    void testConvertToEntityAttribute_TestObjectA() {
        String json = "{\"name\":\"AnotherItem\",\"value\":456}";
        TestObjectA obj = converterA.convertToEntityAttribute(json);

        assertNotNull(obj);
        assertEquals(new TestObjectA("AnotherItem", 456), obj);
    }


    @Test
    void testConvertToDatabaseColumn_TestObjectB_Immutable() {
        TestObjectB obj = new TestObjectB("UUID-123", "Some description text");
        String json = converterB.convertToDatabaseColumn(obj);

        assertNotNull(json);
        assertTrue(json.contains("\"id\":\"UUID-123\""));
        assertTrue(json.contains("\"description\":\"Some description text\""));
        assertTrue(json.startsWith("{") && json.endsWith("}"));
    }

    @Test
    void testConvertToEntityAttribute_TestObjectB_Immutable() {
        String json = "{\"id\":\"UUID-456\",\"description\":\"Different description\"}";
        TestObjectB obj = converterB.convertToEntityAttribute(json);

        assertNotNull(obj);
        assertEquals(new TestObjectB("UUID-456", "Different description"), obj);
    }


    @Test
    void testConvertToDatabaseColumn_NullInput() {
        String json = converterA.convertToDatabaseColumn(null);
        assertNull(json);
    }

    @Test
    void testConvertToEntityAttribute_NullInput() {
        TestObjectA obj = converterA.convertToEntityAttribute(null);
        assertNull(obj);
    }

    @Test
    void testConvertToEntityAttribute_EmptyStringInput() {
        TestObjectA obj = converterA.convertToEntityAttribute("   ");
        assertNull(obj);
    }

    @Test
    void testSerializationErrorHandling() {
        Object malformedObject = new Object() {
            public Object getSelf() { return this; }
        };

        RuntimeException thrown = assertThrows(RuntimeException.class, () -> {
            new GenericConverter<>(Object.class).convertToDatabaseColumn(malformedObject);
        });
        assertTrue(thrown.getMessage().contains("Error converting"));
    }

    @Test
    void testDeserializationErrorHandling() {
        String malformedJson = "{\"name\":\"ItemA\",\"value\":\"not_an_int\"}";

        RuntimeException thrown = assertThrows(RuntimeException.class, () -> {
            converterA.convertToEntityAttribute(malformedJson);
        });
        assertTrue(thrown.getMessage().contains("Error converting"));
    }
}