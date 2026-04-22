package api;

import com.github.javafaker.Faker;
import io.mosip.testrig.apirig.dto.TestCaseDTO;
import io.mosip.testrig.apirig.utils.*;
import io.restassured.response.Response;

import org.apache.log4j.Level;
import org.apache.log4j.Logger;
import org.json.JSONArray;
import org.json.JSONObject;
import org.testng.SkipException;

import javax.ws.rs.core.MediaType;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.HashMap;

public class InjiVerifyUtil extends AdminTestUtil {

	private static final Logger logger = Logger.getLogger(InjiVerifyUtil.class);
	private static String otpEnabled = "true";
	private static Faker faker = new Faker();
	private static String fullNameForSunBirdR = generateFullNameForSunBirdR();
	private static String dobForSunBirdR = generateDobForSunBirdR();
	private static String policyNumberForSunBirdR = generateRandomNumberString(9);
	
	public static void setLogLevel() {
		if (InjiVerifyConfigManager.IsDebugEnabled())
			logger.setLevel(Level.ALL);
		else
			logger.setLevel(Level.ERROR);
	}

	public static TestCaseDTO isTestCaseValidForTheExecution(TestCaseDTO testCaseDTO) {
		String testCaseName = testCaseDTO.getTestCaseName();
		
		int indexof = testCaseName.indexOf("_");
		String modifiedTestCaseName = testCaseName.substring(indexof + 1);
		
		addTestCaseDetailsToMap(modifiedTestCaseName, testCaseDTO.getUniqueIdentifier());
		
		
		String endpoint = testCaseDTO.getEndPoint();
		String inputJson = testCaseDTO.getInput();

		
		if (SkipTestCaseHandler.isTestCaseInSkippedList(testCaseName)) {
			throw new SkipException(GlobalConstants.KNOWN_ISSUES);
		}
		return testCaseDTO;
	}
	

	
	public static String inputstringKeyWordHandeler(String jsonString, String testCaseName) {

		if (jsonString.contains(GlobalConstants.TIMESTAMP)) {
			jsonString = replaceKeywordWithValue(jsonString, GlobalConstants.TIMESTAMP, generateCurrentUTCTimeStamp());
		}

		if (jsonString.contains("$POLICYNUMBERFORSUNBIRDRC$")) {
			jsonString = replaceKeywordWithValue(jsonString, "$POLICYNUMBERFORSUNBIRDRC$", policyNumberForSunBirdR);
		}

		if (jsonString.contains("$FULLNAMEFORSUNBIRDRC$")) {
			jsonString = replaceKeywordWithValue(jsonString, "$FULLNAMEFORSUNBIRDRC$", fullNameForSunBirdR);
		}

		if (jsonString.contains("$DOBFORSUNBIRDRC$")) {
			jsonString = replaceKeywordWithValue(jsonString, "$DOBFORSUNBIRDRC$", dobForSunBirdR);
		}

		return jsonString;

	}
	


	public static String generateFullNameForSunBirdR() {
		return faker.name().fullName();
	}

	public static String generateDobForSunBirdR() {
		Faker faker = new Faker();
		LocalDate dob = faker.date().birthday().toInstant().atZone(java.time.ZoneId.systemDefault()).toLocalDate();
		DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd");
		return dob.format(formatter);
	}
	
	public static JSONArray mimotoActuatorResponseArray = null;

	public static String getValueFromMimotoActuator(String section, String key) {
		String url = ApplnURI + ConfigManager.getproperty("actuatorMimotoEndpoint");
		if (!(System.getenv("useOldContextURL") == null)
				&& !(System.getenv("useOldContextURL").isBlank())
				&& System.getenv("useOldContextURL").equalsIgnoreCase("true")) {
			if (url.contains("/v1/mimoto/")) {
				url = url.replace("/v1/mimoto/", "/residentmobileapp/");
			}
		}
		String actuatorCacheKey = url + section + key;
		String value = actuatorValueCache.get(actuatorCacheKey);
		if (value != null && !value.isEmpty())
			return value;

		try {
			if (mimotoActuatorResponseArray == null) {
				Response response = null;
				JSONObject responseJson = null;
				response = RestClient.getRequest(url, MediaType.APPLICATION_JSON, MediaType.APPLICATION_JSON);

				responseJson = new JSONObject(response.getBody().asString());
				mimotoActuatorResponseArray = responseJson.getJSONArray("propertySources");
			}
			for (int i = 0, size = mimotoActuatorResponseArray.length(); i < size; i++) {
				JSONObject eachJson = mimotoActuatorResponseArray.getJSONObject(i);
				if (eachJson.get("name").toString().contains(section)) {
					value = eachJson.getJSONObject(GlobalConstants.PROPERTIES).getJSONObject(key)
							.get(GlobalConstants.VALUE).toString();
					if (ConfigManager.IsDebugEnabled())
					break;
				}
			}
			actuatorValueCache.put(actuatorCacheKey, value);

			return value;
		} catch (Exception e) {

			return "";
		}

	}

	public static String getSunbirdBaseURL() {
		return InjiVerifyUtil.getValueFromMimotoActuator("overrides", "mosip.sunbird.url");
	}

	public static HashMap<String, Integer> getActuatorValues(HashMap<String, String> keyMapping) throws Exception {
		HashMap<String, Integer> result = new HashMap<>();
		String actuatorUrl = ConfigManager.getproperty("apiInternalEndPoint")
				+ ConfigManager.getproperty("actuatorMimotoEndpoint");
		logger.info("Printing actauator url" + actuatorUrl);
		Response response = RestClient.getRequest(actuatorUrl, MediaType.APPLICATION_JSON, MediaType.APPLICATION_JSON);
		if (response.getStatusCode() != 200) {
			throw new RuntimeException("Failed to fetch actuator values. HTTP error: " + response.getStatusCode());
		}

		JSONObject root = new JSONObject(response.getBody().asString());

		if (!root.has("propertySources")) {
			throw new RuntimeException("[ERROR] No propertySources found in actuator response!");
		}

		for (Object srcObj : root.getJSONArray("propertySources")) {
			if (!(srcObj instanceof JSONObject))
				continue;

			JSONObject src = (JSONObject) srcObj;
			JSONObject props = src.optJSONObject("properties");
			if (props == null)
				continue;

			for (String actuatorKey : keyMapping.keySet()) {
				String resultKey = keyMapping.get(actuatorKey);

				if (!result.containsKey(resultKey) && props.has(actuatorKey)) {
					String rawValue = props.getJSONObject(actuatorKey).optString("value", "0");

					try {
						int intValue = Integer.parseInt(rawValue.trim());
						result.put(resultKey, intValue);
					} catch (NumberFormatException e) {
						System.err
								.println("[WARN] Failed to parse value for key " + actuatorKey + " (" + rawValue + ")");
					}
				}
			}
			if (result.size() == keyMapping.size()) {
				logger.info("All keys mapped successfully");
				break;
			}
		}
		if (result.isEmpty()) {
			logger.info("No actuator values matched the provided key mapping.\"");
		}

		return result;
	}

}