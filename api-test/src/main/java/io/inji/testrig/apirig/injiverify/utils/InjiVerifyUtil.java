package io.inji.testrig.apirig.injiverify.utils;

import static io.restassured.RestAssured.given;

import java.util.ArrayList;
import java.util.Base64;
import java.util.List;
import java.util.Map;

import org.apache.log4j.Logger;
import org.testng.SkipException;

import com.auth0.jwt.JWT;
import com.auth0.jwt.interfaces.DecodedJWT;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;

import io.mosip.testrig.apirig.dbaccess.DBManager;
import io.mosip.testrig.apirig.dto.TestCaseDTO;
import io.mosip.testrig.apirig.utils.AdminTestUtil;
import io.mosip.testrig.apirig.utils.CertsUtil;
import io.mosip.testrig.apirig.utils.ConfigManager;
import io.mosip.testrig.apirig.utils.GlobalConstants;
import io.mosip.testrig.apirig.utils.GlobalMethods;
import io.mosip.testrig.apirig.utils.SkipTestCaseHandler;
import io.restassured.RestAssured;
import io.restassured.config.EncoderConfig;
import io.restassured.config.HttpClientConfig;
import io.restassured.config.RestAssuredConfig;
import io.restassured.response.Response;

public class InjiVerifyUtil extends AdminTestUtil {

	private static final Logger logger = Logger.getLogger(InjiVerifyUtil.class);
	public static final String injiVerifyBaseUrl = InjiVerifyConfigManager
			.getproperty(InjiVerifyConstants.INJI_VERIFY_BASE_URL);
	public static List<String> testCasesInRunScope = new ArrayList<>();

	private static String responseCode;
	private static String transactionCookie;

	private static String transactionCookieTrue;
	private static String transactionCookieFalse;

	public static String getTransactionCookieTrue() {
		return transactionCookieTrue;
	}

	public static String getTransactionCookieFalse() {
		return transactionCookieFalse;
	}

	public static void setTransactionCookieByFlag(String cookie, boolean responseCodeValidationRequired) {
		if (responseCodeValidationRequired) {
			transactionCookieTrue = cookie;
			logger.info("Saved as TRANSACTION_COOKIE_TRUE: " + cookie);
		} else {
			transactionCookieFalse = cookie;
			logger.info("Saved as TRANSACTION_COOKIE_FALSE: " + cookie);
		}
	}

	public static String getTransactionCookie() {
		return transactionCookie;
	}

	public static void setTransactionCookie(String cookie) {
		transactionCookie = cookie;
	}
	public static String getResponseCode() {
		return responseCode;
	}

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

		if (jsonString.contains("$RESPONSE_CODE$")) {
			jsonString = replaceKeywordValue(jsonString, "$RESPONSE_CODE$", getResponseCode());
		}
		if (jsonString.contains("$TRANSACTION_COOKIE$")) {
			jsonString = replaceKeywordValue(jsonString, "$TRANSACTION_COOKIE$", getTransactionCookie());
		}

		if (jsonString.contains("$TRANSACTION_COOKIE_TRUE$")) {
			jsonString = replaceKeywordValue(jsonString, "$TRANSACTION_COOKIE_TRUE$", getTransactionCookieTrue());
		}
		if (jsonString.contains("$TRANSACTION_COOKIE_FALSE$")) {
			jsonString = replaceKeywordValue(jsonString, "$TRANSACTION_COOKIE_FALSE$", getTransactionCookieFalse());
		}
		return jsonString;

	}

	public static String replaceKeywordValue(String jsonString, String keyword, String value) {
		if (value != null && !value.isEmpty())
			return jsonString.replace(keyword, value);
		else
			throw new SkipException("Marking testcase as skipped as required field is empty " + keyword);
	}

	// Remove below two methods once after releasing the apitest-commons-1.3.3
	protected Response postWithBodyAndCookieForAutoGeneratedIdForUrlEncoded(String url, String jsonInput,
			String testCaseName, String idKeyName) {
		Response response = null;
		String inputJson = inputJsonKeyWordHandeler(jsonInput, testCaseName);
		ObjectMapper mapper = new ObjectMapper();
		Map<String, String> map = null;
		try {
			map = mapper.readValue(inputJson, Map.class);
			logger.info(GlobalConstants.POST_REQ_URL + url);
			logger.info(inputJson);
			GlobalMethods.reportRequest(null, inputJson, url);
			response = postRequestWithFormDataBody(url, map);
			GlobalMethods.reportResponse(response.getHeaders().asList().toString(), url, response);

			try {
				String redirectUri = null;

				try {
					redirectUri = response.jsonPath().getString("redirect_uri");
				} catch (Exception ignored) {}

				if (redirectUri == null || redirectUri.isEmpty()) {
					redirectUri = response.getHeader("Location");
				}

				if (redirectUri != null && redirectUri.contains("response_code=")) {

					String value = redirectUri.split("response_code=")[1];

					if (value.contains("&")) {
						value = value.split("&")[0];
					}

					value = value.replace("\\u003d", "=");

					logger.info("Extracted response_code: " + value);

					responseCode = value;

				}

			} catch (Exception e) {
				logger.error("Failed extracting response_code", e);
			}

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