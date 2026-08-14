package io.inji.verify.serialization;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.EqualsAndHashCode; // For good assertion practices

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@EqualsAndHashCode // Important for assertEquals in tests
public class TestObjectA {
    private String name;
    private int value;
}