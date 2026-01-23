package io.inji.verify.dto.result;

import lombok.AllArgsConstructor;
import lombok.Getter;
import org.json.JSONObject;

import java.util.List;

@AllArgsConstructor
@Getter
public class VPTokenDto {
    List<JSONObject> jsonVpTokens;
    List<String> sdJwtVpTokens;
}
