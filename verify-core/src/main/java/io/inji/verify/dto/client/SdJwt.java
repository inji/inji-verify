package io.inji.verify.dto.client;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.nimbusds.jose.shaded.gson.annotations.SerializedName;
import lombok.AllArgsConstructor;
import lombok.Getter;

import java.util.List;

@AllArgsConstructor
@Getter
public class SdJwt {
    @JsonProperty("sd-jwt_alg_values")
    @SerializedName("sd-jwt_alg_values")
    private List<String> sdAlgValues;

    @JsonProperty("kb-jwt_alg_values")
    @SerializedName("kb-jwt_alg_values")
    private List<String> kbAlgValues;
}