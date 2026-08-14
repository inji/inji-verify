package io.inji.verify.serialization;


import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;

public class ListConverterTest {

    private ListConverter<TestObjectA> converterA;
    private ListConverter<TestObjectB> converterB;

    @BeforeEach
    void setUp() {
        converterA = new ListConverter<>(TestObjectA.class);
        converterB = new ListConverter<>(TestObjectB.class);
    }

    @Test
    void testConvertToDatabaseColumn_TestObjectA() {
        List<TestObjectA> list = Arrays.asList(
                new TestObjectA("Item1", 10),
                new TestObjectA("Item2", 20)
        );

        String json = converterA.convertToDatabaseColumn(list);
        assertNotNull(json);
        assertTrue(json.contains("\"name\":\"Item1\""));
        assertTrue(json.contains("\"value\":10"));
        assertTrue(json.contains("\"name\":\"Item2\""));
        assertTrue(json.contains("\"value\":20"));
        assertTrue(json.startsWith("[") && json.endsWith("]"));
    }

    @Test
    void testConvertToEntityAttribute_TestObjectA() {
        String json = "[{\"name\":\"ItemA\",\"value\":1},{\"name\":\"ItemB\",\"value\":2}]";
        List<TestObjectA> list = converterA.convertToEntityAttribute(json);

        assertNotNull(list);
        assertEquals(2, list.size());
        assertEquals(new TestObjectA("ItemA", 1), list.get(0));
        assertEquals(new TestObjectA("ItemB", 2), list.get(1));
    }

    @Test
    void testConvertToDatabaseColumn_TestObjectB_Immutable() {
        List<TestObjectB> list = Arrays.asList(
                new TestObjectB("ID_X", "Description One"),
                new TestObjectB("ID_Y", "Description Two")
        );

        String json = converterB.convertToDatabaseColumn(list);
        assertNotNull(json);
        assertTrue(json.contains("\"id\":\"ID_X\""));
        assertTrue(json.contains("\"description\":\"Description One\""));
        assertTrue(json.contains("\"id\":\"ID_Y\""));
        assertTrue(json.contains("\"description\":\"Description Two\""));
        assertTrue(json.startsWith("[") && json.endsWith("]"));
    }

    @Test
    void testConvertToEntityAttribute_TestObjectB_Immutable() {
        String json = "[{\"id\":\"ID_1\",\"description\":\"First\"},{\"id\":\"ID_2\",\"description\":\"Second\"}]";
        List<TestObjectB> list = converterB.convertToEntityAttribute(json);

        assertNotNull(list);
        assertEquals(2, list.size());
        assertEquals(new TestObjectB("ID_1", "First"), list.get(0));
        assertEquals(new TestObjectB("ID_2", "Second"), list.get(1));
    }

    @Test
    void testConvertToDatabaseColumn_NullInput() {
        String json = converterA.convertToDatabaseColumn(null);
        assertNull(json);
    }

    @Test
    void testConvertToEntityAttribute_NullInput() {
        List<TestObjectA> list = converterA.convertToEntityAttribute(null);
        assertNull(list);
    }

    @Test
    void testConvertToEntityAttribute_EmptyStringInput() {
        List<TestObjectA> list = converterA.convertToEntityAttribute("   ");
        assertNull(list);
    }

    @Test
    void testSerializationErrorHandling() {
        List<Object> malformedList = Collections.singletonList(new Object() {
            public Object getSelf() { return this; }
        });

        RuntimeException thrown = assertThrows(RuntimeException.class, () -> {
            new ListConverter<>(Object.class).convertToDatabaseColumn(malformedList);
        });
        assertTrue(thrown.getMessage().contains("Error converting"));
    }

    @Test
    void testDeserializationErrorHandling() {
        String malformedJson = "[{\"name\":\"ItemA\",\"value\":\"not_an_int\"}]"; // Value type mismatch

        RuntimeException thrown = assertThrows(RuntimeException.class, () -> {
            converterA.convertToEntityAttribute(malformedJson);
        });
        assertTrue(thrown.getMessage().contains("Error converting"));
    }
}