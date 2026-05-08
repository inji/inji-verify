package io.inji.verify.dto.client;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.nimbusds.jose.shaded.gson.annotations.SerializedName;
import lombok.AllArgsConstructor;
import lombok.Getter;

@AllArgsConstructor
@Getter
public class VpFormatsSupported {
    @JsonProperty("ldp_vp")
    @SerializedName("ldp_vp")
    private LdpVp ldpVp;

    @JsonProperty("vc+sd-jwt")
    @SerializedName("vc+sd-jwt")
    private SdJwt sdJwt;
}