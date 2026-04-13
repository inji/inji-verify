package api;

import java.util.HashMap;
import java.util.Map;
import java.util.Properties;

import org.apache.log4j.Logger;

import io.mosip.testrig.apirig.utils.ConfigManager;
import runnerfiles.Runner;

public class InjiVerifyConfigManager extends ConfigManager{

	private static final Logger LOGGER = Logger.getLogger(InjiVerifyConfigManager.class);

	public static void init() {
		Map<String, Object> moduleSpecificPropertiesMap = new HashMap<>();
		// Load scope specific properties
		try {
			String path = Runner.getGlobalResourcePath() + "/config/injiVerify.properties";
			Properties props = getproperties(path);
			// Convert Properties to Map and add to moduleSpecificPropertiesMap
			for (String key : props.stringPropertyNames()) {
				moduleSpecificPropertiesMap.put(key, props.getProperty(key));
			}
		} catch (Exception e) {
			LOGGER.error(e.getMessage());
		}
		// Add module specific properties as well.
		init(moduleSpecificPropertiesMap);
	}

	public static String getInjiVerifyUi() { return getproperty("injiverify"); }
	public static String getInjiWebUi() { return getproperty("injiweb"); }
	public static String getEsignetBaseUrl() { return getproperty("eSignetbaseurl");}

	public static String getapiEndUser() { return getproperty("apiEnvUser"); }

	public static String getIssuerSearchText() { return getproperty("issuerSearchText"); }

	public static String getIssuerSearchTextForSunbird() { return getproperty("issuerSearchTextforSunbird"); }

	public static String getInjiVerifyUiBaseUrl() {
		String url = getproperty("injiverify");
		String  temp =url.replaceFirst("https?://", "");
		String  baseurl =temp.replaceFirst("/", "");
		return baseurl;
	}

	public static String getSunbirdBaseURL() {
		return InjiVerifyUtil.getValueFromMimotoActuator("overrides", "mosip.sunbird.url");

	}

}