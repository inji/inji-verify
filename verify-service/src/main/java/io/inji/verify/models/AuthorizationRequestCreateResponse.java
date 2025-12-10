package io.inji.verify.models;

import io.inji.verify.dto.authorizationrequest.AuthorizationRequestResponseDto;
import io.inji.verify.serialization.impl.AuthorizationRequestResponseDtoConverter;
import jakarta.persistence.*;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.io.Serializable;

@Getter
@Entity
@AllArgsConstructor
@NoArgsConstructor(force = true)
@Table(name = "authorization_request_details")
public class AuthorizationRequestCreateResponse implements Serializable {
    @Id
    private final String requestId;

    @NotNull
    private final String transactionId;

    @NotNull
    @Convert(converter = AuthorizationRequestResponseDtoConverter.class)
    @Column(columnDefinition = "TEXT")
    private final AuthorizationRequestResponseDto authorizationDetails;

    @NotNull
    private final long expiresAt;
}