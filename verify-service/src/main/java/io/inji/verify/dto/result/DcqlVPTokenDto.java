package io.inji.verify.dto.result;

import lombok.AllArgsConstructor;
import lombok.Getter;
import org.json.JSONObject;

import java.util.Map;

@AllArgsConstructor
@Getter
public class DcqlVPTokenDto {
    private Map<String, JSONObject> ldpVpTokens;
    private Map<String, String> sdJwtTokens;
}
