package io.inji.verify.serialization;

import com.fasterxml.jackson.annotation.JsonCreator;
import lombok.Value;
import lombok.AllArgsConstructor;

@Value
@AllArgsConstructor(onConstructor_ = @JsonCreator)
public class TestObjectB {
    String id;
    String description;
}