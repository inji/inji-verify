package io.inji.verify.dto.authorizationrequest;

import io.inji.verify.dto.presentation.VPDefinitionResponseDto;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Getter;

@AllArgsConstructor
@Getter
@NotNull
public class VPRequestCreateDto {
    @NotNull(message = "Client Id must not be null")
    @NotBlank(message = "Client Id must not be empty")
    String clientId;
    String transactionId;
    String presentationDefinitionId;
    String nonce;
    @Valid
    VPDefinitionResponseDto presentationDefinition;
    boolean acceptVPWithoutHolderProof;
    boolean responseCodeValidationRequired;
}