package io.inji.verify.dto.result;

import lombok.AllArgsConstructor;
import lombok.Getter;
import org.json.JSONObject;

import java.util.Map;

@AllArgsConstructor
@Getter
public class DcqlVPTokenDto {
    Map<String, JSONObject> ldpVpTokens;
    Map<String, String> sdJwtTokens;
}
