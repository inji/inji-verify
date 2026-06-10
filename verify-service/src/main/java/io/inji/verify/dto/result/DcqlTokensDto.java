package io.inji.verify.dto.result;

import lombok.AllArgsConstructor;
import lombok.Getter;
import org.json.JSONObject;

import java.util.List;
import java.util.Map;

@AllArgsConstructor
@Getter
public class DcqlTokensDto {
    private Map<String, List<JSONObject>> ldpVpTokens;
    private Map<String, List<JSONObject>> ldpVcTokens;
    private Map<String, List<String>> sdJwtTokens;
}
