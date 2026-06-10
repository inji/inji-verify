package io.inji.verify.dto.client;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.nimbusds.jose.shaded.gson.annotations.SerializedName;
import lombok.AllArgsConstructor;
import lombok.Getter;

@AllArgsConstructor
@Getter
public class VpFormatsSupported {
    @JsonProperty("ldp_vc")
    @SerializedName("ldp_vc")
    private LdpVc ldpVc;

    @JsonProperty("vc+sd-jwt")
    @SerializedName("vc+sd-jwt")
    private SdJwt sdJwt;

    @JsonProperty("dc+sd-jwt")
    @SerializedName("dc+sd-jwt")
    private SdJwt dcSdJwt;
}