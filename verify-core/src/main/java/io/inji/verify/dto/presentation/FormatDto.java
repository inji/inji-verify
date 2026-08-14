package io.inji.verify.dto.presentation;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.nimbusds.jose.shaded.gson.annotations.SerializedName;
import lombok.AllArgsConstructor;
import lombok.Getter;

@AllArgsConstructor
@Getter
@JsonInclude(JsonInclude.Include.NON_NULL)
public class FormatDto {
    private final JwtDto jwt;
    @JsonProperty("jwt_vc")
    @SerializedName("jwt_vc")
    private final JwtVcDto jwtVC;
    @JsonProperty("ldp_vc")
    @SerializedName("ldp_vc")
    private final LdpVcDto ldpVC;
}