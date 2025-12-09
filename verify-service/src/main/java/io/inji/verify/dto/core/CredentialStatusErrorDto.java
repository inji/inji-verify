package io.inji.verify.dto.core;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.Setter;

@AllArgsConstructor
@Getter
@Setter
public class CredentialStatusErrorDto {
    String timestamp;
    int status;
    String path;
    String error;
}
