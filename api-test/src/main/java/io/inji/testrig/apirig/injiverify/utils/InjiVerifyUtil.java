package io.inji.testrig.apirig.injiverify.utils;

import static io.restassured.RestAssured.given;

import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Base64;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.apache.log4j.Logger;
import org.json.JSONArray;
import org.json.JSONObject;
import org.testng.SkipException;

import com.auth0.jwt.JWT;
import com.auth0.jwt.interfaces.DecodedJWT;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;

import io.mosip.testrig.apirig.dbaccess.DBManager;
import io.mosip.testrig.apirig.dto.TestCaseDTO;
import io.mosip.testrig.apirig.utils.AdminTestException;
import io.mosip.testrig.apirig.utils.AdminTestUtil;
import io.mosip.testrig.apirig.utils.CertsUtil;
import io.mosip.testrig.apirig.utils.ConfigManager;
import io.mosip.testrig.apirig.utils.GlobalConstants;
import io.mosip.testrig.apirig.utils.GlobalMethods;
import io.mosip.testrig.apirig.utils.SecurityXSSException;
import io.mosip.testrig.apirig.utils.SkipTestCaseHandler;
import io.restassured.RestAssured;
import io.restassured.config.EncoderConfig;
import io.restassured.config.HttpClientConfig;
import io.restassured.config.RestAssuredConfig;
import io.restassured.response.Response;

public class InjiVerifyUtil extends AdminTestUtil {

	private static final Logger logger = Logger.getLogger(InjiVerifyUtil.class);
	private static final String RESPONSE_CODE_PLACEHOLDER = "$RESPONSE_CODE$";
	private static final String RESPONSE_CODE_QUERY_PREFIX = "response_code=";

	private static String responseCode = null;
	private static String transactionCookieTrue = null;
	private static String transactionCookieFalse = null;

	private static final String TRANSACTION_COOKIE_TRUE_PLACEHOLDER = "$TRANSACTION_COOKIE_TRUE$";
	private static final String TRANSACTION_COOKIE_FALSE_PLACEHOLDER = "$TRANSACTION_COOKIE_FALSE$";

	public static final String TC_VP_SESSION_REQUEST_ALL_VALID_SMOKE_SID =
			"CreateNewVerificationRequest_ForVpSessionRequest_All_Valid_Smoke_Sid";

	public static final String TC_VP_SESSION_REQUEST_RESPONSE_CODE_VALIDATION_FALSE_SID =
			"CreateNewVerificationRequest_ForVpSessionRequest_withResponseCodeValidationRequired_asFalse_Valid_Sid";

	public static final String injiVerifyBaseUrl = InjiVerifyConfigManager
			.getproperty(InjiVerifyConstants.INJI_VERIFY_BASE_URL);
	public static List<String> testCasesInRunScope = new ArrayList<>();

	public static String isTestCaseValidForExecution(TestCaseDTO testCaseDTO) {
		String testCaseName = testCaseDTO.getTestCaseName();
		currentTestCaseName = testCaseName;

		int indexof = testCaseName.indexOf("_");
		String modifiedTestCaseName = testCaseName.substring(indexof + 1);

		addTestCaseDetailsToMap(modifiedTestCaseName, testCaseDTO.getUniqueIdentifier());

		if (!testCasesInRunScope.isEmpty()
				&& testCasesInRunScope.contains(testCaseDTO.getUniqueIdentifier()) == false) {
			throw new SkipException(GlobalConstants.NOT_IN_RUN_SCOPE_MESSAGE);
		}

		if (SkipTestCaseHandler.isTestCaseInSkippedList(testCaseName)) {
			throw new SkipException(GlobalConstants.KNOWN_ISSUES);
		}

		// Handle extra workflow dependencies
		if (testCaseDTO != null && testCaseDTO.getAdditionalDependencies() != null
				&& AdminTestUtil.generateDependency == true) {
			addAdditionalDependencies(testCaseDTO);
		}

		return testCaseName;
	}

	public String inputJsonModuleKeyWordHandler(String jsonString, String testCaseName) {
		if (jsonString == null) {
			logger.info(" Request Json String is :" + jsonString);
			return jsonString;
		}

		if (jsonString.contains("$PRESENTATIONDEFINITIONID$")) {
			jsonString = replaceKeywordWithValue(jsonString, "$PRESENTATIONDEFINITIONID$",
					InjiVerifyConfigManager.getproperty(InjiVerifyConstants.PRESENTATION_DEFINITION_ID));
		}

		if (jsonString.contains("$INJIVERIFYBASEURL$")) {
			jsonString = replaceKeywordWithValue(jsonString, "$INJIVERIFYBASEURL$",
					InjiVerifyConfigManager.getproperty(InjiVerifyConstants.INJI_VERIFY_BASE_URL));
		}

		return jsonString;
	}

	public static String getResponseCode() {
		return responseCode;
	}

	public static void setResponseCode(String code) {
		responseCode = code;
	}

	public static String getTransactionCookieTrue() {
		return transactionCookieTrue;
	}

	public static String getTransactionCookieFalse() {
		return transactionCookieFalse;
	}

	public static void setTransactionCookieByFlag(String cookie, boolean flag) {
		if (flag) {
			transactionCookieTrue = cookie;
			logger.info("Saved as TRANSACTION_COOKIE_TRUE: " + cookie);
		} else {
			transactionCookieFalse = cookie;
			logger.info("Saved as TRANSACTION_COOKIE_FALSE: " + cookie);
		}
	}

	public static String replaceResponseCodePlaceholder(String jsonInput) {
		if (jsonInput == null || !jsonInput.contains(RESPONSE_CODE_PLACEHOLDER)) {
			return jsonInput;
		}

		String code = getResponseCode();
		if (code == null || code.isEmpty()) {
			throw new SkipException("RESPONSE_CODE is null or empty. Cannot proceed.");
		}

		return jsonInput.replace(RESPONSE_CODE_PLACEHOLDER, code);
	}

	public static String replaceTransactionCookiePlaceholders(String jsonInput) {
		if (jsonInput == null) {
			return null;
		}
		String out = jsonInput;
		if (out.contains(TRANSACTION_COOKIE_TRUE_PLACEHOLDER)) {
			String transactionCookieValue = getTransactionCookieTrue();
			if (transactionCookieValue != null && !transactionCookieValue.isEmpty()) {
				out = out.replace(TRANSACTION_COOKIE_TRUE_PLACEHOLDER, transactionCookieValue);
			} else {
				logger.warn(TRANSACTION_COOKIE_TRUE_PLACEHOLDER + " present but value is null or empty");
			}
		}
		if (out.contains(TRANSACTION_COOKIE_FALSE_PLACEHOLDER)) {
			String transactionCookieValue = getTransactionCookieFalse();
			if (transactionCookieValue != null && !transactionCookieValue.isEmpty()) {
				out = out.replace(TRANSACTION_COOKIE_FALSE_PLACEHOLDER, transactionCookieValue);
			} else {
				logger.warn(TRANSACTION_COOKIE_FALSE_PLACEHOLDER + " present but value is null or empty");
			}
		}
		return out;
	}

	public static String replaceVpSessionInputPlaceholders(String jsonInput) {
		String jsonWithResponseCodeReplaced = replaceResponseCodePlaceholder(jsonInput);
		return replaceTransactionCookiePlaceholders(jsonWithResponseCodeReplaced);
	}

	public static String extractResponseCodeFromRedirectUri(String redirectUri) {
		if (redirectUri == null || redirectUri.isEmpty() || !redirectUri.contains(RESPONSE_CODE_QUERY_PREFIX)) {
			return null;
		}
		String remainder = redirectUri.split(RESPONSE_CODE_QUERY_PREFIX, 2)[1];
		remainder = remainder.replace("\\u003d", "=");
		int amp = remainder.indexOf('&');
		if (amp >= 0) {
			remainder = remainder.substring(0, amp);
		}
		int hash = remainder.indexOf('#');
		if (hash >= 0) {
			remainder = remainder.substring(0, hash);
		}
		String code = remainder.trim();
		return code.isEmpty() ? null : code;
	}

	public static void captureResponseCodeFromVpSubmissionResponse(Response response, String requestUrl) {
		if (response == null || requestUrl == null || !requestUrl.contains("vp-submission")) {
			return;
		}
		String redirectUri = null;
		try {
			String body = response.asString();
			if (body != null && !body.isEmpty() && body.trim().startsWith("{")) {
				try {
					JSONObject json = new JSONObject(body);
					if (json.has("redirect_uri")) {
						redirectUri = json.optString("redirect_uri", null);
					}
				} catch (Exception ignored) {
				}
			}
		} catch (Exception e) {
			logger.error("Failed reading redirect_uri from VP submission body", e);
		}
		if (redirectUri == null || redirectUri.isEmpty()) {
			try {
				redirectUri = response.getHeader("Location");
			} catch (Exception ignored) {
			}
		}
		if (redirectUri == null || redirectUri.isEmpty()) {
			return;
		}
		String code = extractResponseCodeFromRedirectUri(redirectUri);
		if (code != null && !code.isEmpty()) {
			setResponseCode(code);
		}
	}

	public static void captureTransactionCookieFromVpSessionRequestResponse(String endPoint, String testCaseName,
			Response response, String requestBodyJson) {
		if (endPoint == null || !endPoint.contains("vp-session-request") || response == null) {
			return;
		}
		String setCookieHeader = response.getHeader("Set-Cookie");
		if (setCookieHeader == null || !setCookieHeader.contains("transaction_id=")) {
			return;
		}
		String value = setCookieHeader.split(";", 2)[0];

		if (testCaseName != null) {
			if (testCaseName.contains(TC_VP_SESSION_REQUEST_RESPONSE_CODE_VALIDATION_FALSE_SID)) {
				setTransactionCookieByFlag(value, false);
				return;
			}
			if (testCaseName.contains(TC_VP_SESSION_REQUEST_ALL_VALID_SMOKE_SID)) {
				setTransactionCookieByFlag(value, true);
				return;
			}
		}
		try {
			JSONObject requestJson = new JSONObject(requestBodyJson);
			if (requestJson.has("responseCodeValidationRequired")) {
				boolean flag = requestJson.getBoolean("responseCodeValidationRequired");
				setTransactionCookieByFlag(value, flag);
			}
		} catch (Exception e) {
			logger.error("Failed saving transaction cookie by responseCodeValidationRequired", e);
		}
	}

	private static String replaceGsonUnicodeEqualsEscapes(String s) {
		if (s == null || !s.contains("\\u003d")) {
			return s;
		}
		return s.replace("\\u003d", "=");
	}

	private static String normalizeJsonLikeStringForFormField(String s, ObjectMapper mapper) {
		if (s == null || s.isEmpty()) {
			return s;
		}
		boolean hadUnicodeEqualsEscape = s.contains("\\u003d");
		s = replaceGsonUnicodeEqualsEscapes(s);
		if (!hadUnicodeEqualsEscape) {
			return s;
		}
		String t = s.trim();
		if (!t.startsWith("{") && !t.startsWith("[")) {
			return s;
		}
		try {
			Object tree = mapper.readValue(s, Object.class);
			return mapper.writeValueAsString(tree);
		} catch (Exception e) {
			return s;
		}
	}

	private static Map<String, String> stringifyValuesForUrlEncodedForm(Map<String, Object> raw, ObjectMapper mapper)
			throws JsonProcessingException {
		Map<String, String> out = new LinkedHashMap<>();
		if (raw == null) {
			return out;
		}
		for (Map.Entry<String, Object> e : raw.entrySet()) {
			Object v = e.getValue();
			if (v == null) {
				out.put(e.getKey(), "");
			} else if (v instanceof String) {
				out.put(e.getKey(), normalizeJsonLikeStringForFormField((String) v, mapper));
			} else {
				String nested = mapper.writeValueAsString(v);
				nested = replaceGsonUnicodeEqualsEscapes(nested);
				out.put(e.getKey(), nested);
			}
		}
		return out;
	}

	protected Response postWithBodyAndCustomContentType(String url, String body, String contentType,
			boolean auditLogCheck, String cookieName, String role, String testCaseName)
			throws SecurityXSSException, AdminTestException {
		Response response = null;
		String requestBody = inputJsonKeyWordHandeler(body, testCaseName);
		url = uriKeyWordHandelerUri(url, testCaseName);
		url = GlobalMethods.addToServerEndPointMap(url);
		logger.info(GlobalConstants.POST_REQ_URL + url);
		GlobalMethods.reportRequest(null, requestBody, url);
		try {
			byte[] rawBody = requestBody.getBytes(StandardCharsets.UTF_8);
			if ("noauth".equals(role)) {
				response = given().relaxedHTTPSValidation().header("Content-Type", contentType)
						.accept("application/json").body(rawBody).when().post(url).then().extract().response();
			} else {
				token = kernelAuthLib.getTokenByRole(role);
				response = given().relaxedHTTPSValidation().header("Content-Type", contentType)
						.accept("application/json").cookie(cookieName, token).body(rawBody).when().post(url).then()
						.extract().response();
			}
			GlobalMethods.checkXSSProtectionHeader(response, url);
			GlobalMethods.reportResponse(response.getHeaders().asList().toString(), url, response);
			return response;
		} catch (SecurityXSSException se) {
			String responseHeadersString = (response == null) ? "No response"
					: response.getHeaders().asList().toString();
			logger.error("XSS check failed for URL: " + url + "\nHeaders: " + responseHeadersString, se);
			throw se;
		} catch (Exception e) {
			logger.error(GlobalConstants.EXCEPTION_STRING_2 + e);
			throw new AdminTestException("Failed to POST raw body to " + url + ": " + e.getMessage());
		}
	}

	// Remove below two methods once after releasing the apitest-commons-1.3.3
	protected Response postWithBodyAndCookieForAutoGeneratedIdForUrlEncoded(String url, String jsonInput,
			String testCaseName, String idKeyName) {
		Response response = null;
		String inputJson = inputJsonKeyWordHandeler(jsonInput, testCaseName);
		ObjectMapper mapper = new ObjectMapper();
		Map<String, String> map = null;
		try {
			Map<String, Object> parsed = mapper.readValue(inputJson, new TypeReference<Map<String, Object>>() {
			});
			map = stringifyValuesForUrlEncodedForm(parsed, mapper);
			logger.info(GlobalConstants.POST_REQ_URL + url);
			logger.info(inputJson);
			GlobalMethods.reportRequest(null, inputJson, url);
			response = postRequestWithFormDataBody(url, map);
			GlobalMethods.reportResponse(response.getHeaders().asList().toString(), url, response);

			captureResponseCodeFromVpSubmissionResponse(response, url);

			if (testCaseName.toLowerCase().contains("_sid")) {
				writeAutoGeneratedId(response, idKeyName, testCaseName);
			}
			if (testCaseName.contains("UIN_Cookie") || testCaseName.contains("Vid_Cookie")) {
				String keyName = null;
				if (testCaseName.contains("UIN_Cookie"))
					keyName = ESIGNETUINCOOKIESRESPONSE;
				else
					keyName = ESIGNETVIDCOOKIESRESPONSE;

				CertsUtil.addCertificateToCache(keyName, response.getBody().asString());
			}

			return response;
		} catch (Exception e) {
			logger.error(GlobalConstants.EXCEPTION_STRING_2 + e);
			return response;
		}
	}

	public static Response postRequestWithFormDataBody(String url, Map<String, String> formData) {
		RestAssuredConfig config = RestAssured.config().httpClient(HttpClientConfig.httpClientConfig());
		Response postResponse;
		url = GlobalMethods.addToServerEndPointMap(url);

		EncoderConfig encoderConfig = new EncoderConfig().encodeContentTypeAs("application/x-www-form-urlencoded; charset=utf-8",
				io.restassured.http.ContentType.URLENC);
		logger.info("REST-ASSURED: Sending a POST request to " + url);

		if (ConfigManager.IsDebugEnabled()) {
			postResponse = given().config(config.encoderConfig(encoderConfig)).relaxedHTTPSValidation().formParams(formData)
					.contentType("application/x-www-form-urlencoded; charset=utf-8").log().all().when().post(url).then().extract().response();
		} else {
			postResponse = given().config(config.encoderConfig(encoderConfig)).relaxedHTTPSValidation().formParams(formData)
					.contentType("application/x-www-form-urlencoded; charset=utf-8").when().post(url).then().extract().response();
		}

		return postResponse;
	}

	public static String decodeBase64Url(String encoded) {
		return new String(Base64.getUrlDecoder().decode(encoded));
	}

	/**
	 * Validates client_metadata in Authorization Request JWT payloads conforms to
	 * OpenID4VP v1.0: vp_formats_supported (not vp_formats), ldp_vc (not ldp_vp),
	 * and no client_name. Validated in Java because Mosip output validation cannot
	 * resolve JSON paths containing '+' (e.g. vc+sd-jwt, dc+sd-jwt).
	 */
	private static final List<String> EXPECTED_LDP_PROOF_TYPES = Arrays.asList(
			"Ed25519Signature2018", "Ed25519Signature2020", "RsaSignature2018");
	private static final List<String> EXPECTED_SD_JWT_ALGS = Arrays.asList(
			"RS256", "ES256", "ES256K", "EdDSA");
	private static final String CLIENT_METADATA = "client_metadata";
	private static final String VP_FORMATS = "vp_formats";
	private static final String VP_FORMATS_SUPPORTED = "vp_formats_supported";
	private static final String CLIENT_NAME = "client_name";
	private static final String LDP_VP = "ldp_vp";
	private static final String LDP_VC = "ldp_vc";
	private static final String PROOF_TYPE = "proof_type";
	private static final String SD_JWT_ALG_VALUES = "sd-jwt_alg_values";
	private static final String KB_JWT_ALG_VALUES = "kb-jwt_alg_values";
	private static final String VC_SD_JWT = "vc+sd-jwt";
	private static final String DC_SD_JWT = "dc+sd-jwt";
	private static final String AUTHORIZATION_DETAILS = "authorizationDetails";
	private static final String RESPONSE_CODE_VALIDATION_REQUIRED = "responseCodeValidationRequired";
	private static final String VP_REQUEST_WITH_VALID_DATA_FOR_VP_SESSION_RESULT_TEMPLATE =
			"VpRequestWithvaliddataForVpSessionResult";

	public static void validateOpenId4VpClientMetadata(String payloadJson) throws AdminTestException {
		JSONObject payload = new JSONObject(payloadJson);

		if (!payload.has(CLIENT_METADATA)) {
			throw new AdminTestException(
					"client_metadata must be present in Authorization Request for decentralized_identifier client_id");
		}

		JSONObject clientMetadata = payload.getJSONObject(CLIENT_METADATA);
		assertForbiddenClientMetadataKeys(clientMetadata);

		if (!clientMetadata.has(VP_FORMATS_SUPPORTED)) {
			throw new AdminTestException(
					"client_metadata must include vp_formats_supported per OpenID4VP v1.0");
		}

		JSONObject vpFormatsSupported = clientMetadata.getJSONObject(VP_FORMATS_SUPPORTED);
		assertVpFormatsSupportedKeys(vpFormatsSupported);

		JSONObject ldpVc = vpFormatsSupported.getJSONObject(LDP_VC);
		assertStringArrayEquals(LDP_VC + "." + PROOF_TYPE, toStringList(ldpVc.getJSONArray(PROOF_TYPE)),
				EXPECTED_LDP_PROOF_TYPES);

		assertSdJwtFormat(vpFormatsSupported, VC_SD_JWT);
		assertSdJwtFormat(vpFormatsSupported, DC_SD_JWT);
	}

	private static void assertForbiddenClientMetadataKeys(JSONObject clientMetadata) throws AdminTestException {
		Iterator<String> keys = clientMetadata.keys();
		while (keys.hasNext()) {
			switch (keys.next()) {
				case VP_FORMATS:
					throw new AdminTestException(
							"Deprecated vp_formats must not be present in client_metadata; expected vp_formats_supported per OpenID4VP v1.0");
				case CLIENT_NAME:
					throw new AdminTestException("client_name must not be present in client_metadata");
				default:
					break;
			}
		}
	}

	private static void assertVpFormatsSupportedKeys(JSONObject vpFormatsSupported) throws AdminTestException {
		Iterator<String> keys = vpFormatsSupported.keys();
		while (keys.hasNext()) {
			switch (keys.next()) {
				case LDP_VP:
					throw new AdminTestException(
							"Deprecated ldp_vp must not be present in vp_formats_supported; expected ldp_vc per OpenID4VP v1.0");
				default:
					break;
			}
		}
		if (!vpFormatsSupported.has(LDP_VC)) {
			throw new AdminTestException(
					"vp_formats_supported must include ldp_vc for Linked Data Proof credentials");
		}
	}

	private static void assertSdJwtFormat(JSONObject vpFormatsSupported, String formatKey)
			throws AdminTestException {
		if (!vpFormatsSupported.has(formatKey)) {
			throw new AdminTestException("vp_formats_supported must include " + formatKey);
		}
		JSONObject sdJwtFormat = vpFormatsSupported.getJSONObject(formatKey);
		assertStringArrayEquals(formatKey + "." + SD_JWT_ALG_VALUES,
				toStringList(sdJwtFormat.getJSONArray(SD_JWT_ALG_VALUES)), EXPECTED_SD_JWT_ALGS);
		assertStringArrayEquals(formatKey + "." + KB_JWT_ALG_VALUES,
				toStringList(sdJwtFormat.getJSONArray(KB_JWT_ALG_VALUES)), EXPECTED_SD_JWT_ALGS);
	}

	private static List<String> toStringList(JSONArray array) {
		List<String> values = new ArrayList<>();
		for (int i = 0; i < array.length(); i++) {
			values.add(array.getString(i));
		}
		return values;
	}

	private static void assertStringArrayEquals(String field, List<String> actual, List<String> expected)
			throws AdminTestException {
		Set<String> actualSet = new HashSet<>(actual);
		Set<String> expectedSet = new HashSet<>(expected);
		if (!actualSet.equals(expectedSet)) {
			throw new AdminTestException(field + " expected " + expectedSet + " but was " + actualSet);
		}
	}

	/**
	 * Validates authorizationDetails.responseCodeValidationRequired in VP request
	 * create responses. Done in Java because Mosip output validation does not
	 * reliably resolve nested boolean fields.
	 */
	public static void validateAuthorizationDetailsResponseCodeValidationRequired(String responseJson,
			boolean expected) throws AdminTestException {
		JSONObject response = new JSONObject(responseJson);
		if (!response.has(AUTHORIZATION_DETAILS) || response.isNull(AUTHORIZATION_DETAILS)) {
			throw new AdminTestException(
					"authorizationDetails must be present in VP request create response for pre-registered clientId");
		}
		JSONObject authorizationDetails = response.getJSONObject(AUTHORIZATION_DETAILS);
		if (!authorizationDetails.has(RESPONSE_CODE_VALIDATION_REQUIRED)) {
			throw new AdminTestException("authorizationDetails must include " + RESPONSE_CODE_VALIDATION_REQUIRED);
		}
		boolean actual = authorizationDetails.getBoolean(RESPONSE_CODE_VALIDATION_REQUIRED);
		if (actual != expected) {
			throw new AdminTestException(AUTHORIZATION_DETAILS + "." + RESPONSE_CODE_VALIDATION_REQUIRED
					+ " expected " + expected + " but was " + actual);
		}
	}

	public static boolean usesVpSessionResultOutputTemplate(String outputTemplate) {
		return outputTemplate != null
				&& outputTemplate.contains(VP_REQUEST_WITH_VALID_DATA_FOR_VP_SESSION_RESULT_TEMPLATE);
	}

	/**
	 * Validates authorizationDetails.responseCodeValidationRequired when the test
	 * uses the VP session result output template. Skips when only status code is
	 * checked or input JSON is unavailable.
	 */
	public static void validateAuthorizationDetailsResponseCodeValidationRequiredFromInput(
			String outputTemplate, boolean checkOnlyStatusCodeInResponse, String inputJson, String responseJson)
			throws AdminTestException {
		if (!usesVpSessionResultOutputTemplate(outputTemplate) || checkOnlyStatusCodeInResponse) {
			return;
		}
		if (inputJson == null || inputJson.trim().isEmpty()) {
			throw new AdminTestException(
					"Input JSON is required to validate authorizationDetails.responseCodeValidationRequired");
		}
		if (responseJson == null || responseJson.trim().isEmpty()) {
			throw new AdminTestException(
					"Response body is required to validate authorizationDetails.responseCodeValidationRequired");
		}
		try {
			JSONObject requestJson = new JSONObject(inputJson);
			if (requestJson.has(RESPONSE_CODE_VALIDATION_REQUIRED)) {
				validateAuthorizationDetailsResponseCodeValidationRequired(responseJson,
						requestJson.getBoolean(RESPONSE_CODE_VALIDATION_REQUIRED));
			}
		} catch (AdminTestException e) {
			throw e;
		} catch (Exception e) {
			throw new AdminTestException(
					"Failed to parse input JSON for responseCodeValidationRequired validation: " + e.getMessage());
		}
	}

	public static String decodeAndCombineJwt(String jwtString) {
		try {
			DecodedJWT jwt = JWT.decode(jwtString);

			// Base64 decode header & payload
			String headerJson = decodeBase64Url(jwt.getHeader());
			String payloadJson = decodeBase64Url(jwt.getPayload());

			// Use Jackson to combine into single JSON object
			ObjectMapper mapper = new ObjectMapper();
			ObjectNode combinedJson = mapper.createObjectNode();

			combinedJson.set("header", mapper.readTree(headerJson));
			combinedJson.set("payload", mapper.readTree(payloadJson));

			// Pretty print final JSON
			return mapper.writerWithDefaultPrettyPrinter().writeValueAsString(combinedJson);

		} catch (Exception e) {
			logger.error("Error decoding JWT: " + e.getMessage(), e);
			return null;
		}
	}

	public void updateCacheFromRow(Map<String, Object> row, String idKeyName, String testCaseName) {
		if (row == null || row.isEmpty() || idKeyName == null || idKeyName.trim().isEmpty()) {
			return;
		}

		String[] keys = idKeyName.split(",");
		for (String key : keys) {
			String trimmedKey = key.trim();
			if (!trimmedKey.isEmpty()) {
				if (row.containsKey(trimmedKey)) {
					Object value = row.get(trimmedKey);
					if (value != null) {
						writeAutoGeneratedId(testCaseName, trimmedKey, value.toString());
					} else {
						logger.error("Key '" + trimmedKey + "' has null value in DB row for testCase: " + testCaseName);
					}
				} else {
					logger.error("Key '" + trimmedKey + "' not found in DB row for testCase: " + testCaseName);
				}
			}
		}
	}

	public static void verifyDBCleanup() {
		DBManager.executeDBQueries(InjiVerifyConfigManager.getInjiVerifyDBURL(),
				InjiVerifyConfigManager.getproperty("db-su-user"),
				InjiVerifyConfigManager.getproperty("postgres-password"),
				InjiVerifyConfigManager.getproperty("inji_verify_schema"),
				getGlobalResourcePath() + "/" + "config/DB_delete_script.txt");
	}
}