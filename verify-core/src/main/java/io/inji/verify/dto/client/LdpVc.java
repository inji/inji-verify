package io.inji.verify.dto.client;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.nimbusds.jose.shaded.gson.annotations.SerializedName;
import lombok.AllArgsConstructor;
import lombok.Getter;

import java.util.List;

@AllArgsConstructor
@Getter
public class LdpVc {
    @JsonProperty("proof_type")
    @SerializedName("proof_type")
    private List<String> proofType;
}