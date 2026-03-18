package io.inji.testrig.apirig.injiverify.utils;

import java.util.HashMap;
import java.util.Map;
import java.util.Properties;

import org.apache.log4j.Logger;

import io.inji.testrig.apirig.injiverify.testrunner.InjiTestRunner;
import io.mosip.testrig.apirig.utils.ConfigManager;

public class InjiVerifyConfigManager extends ConfigManager {
	private static final Logger LOGGER = Logger.getLogger(InjiVerifyConfigManager.class);

	public static void init() {
		Map<String, Object> moduleSpecificPropertiesMap = new HashMap<>();
		// Load scope specific properties
		try {
			String path = InjiTestRunner.getGlobalResourcePath() + "/config/injiVerify.properties";
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

			public static String getInjiVerifyDBURL() {
		return "jdbc:postgresql://" + getproperty("db-server") + ":" + getproperty("db-port") + "/" + getproperty("inji_verify_db_table");
	}

}