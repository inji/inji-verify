package io.inji.verify.dto.presentation;

import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

public class SubmissionRequirementDtoTest {

    @Test
    public void testConstructor() {
        String name = "name";
        String rule = "rule";
        String count = "count";
        String from = "from";

        SubmissionRequirementDto dto = new SubmissionRequirementDto(name, rule, count, from);

        assertEquals(name, dto.getName());
        assertEquals(rule, dto.getRule());
        assertEquals(count, dto.getCount());
        assertEquals(from, dto.getFrom());
    }
}