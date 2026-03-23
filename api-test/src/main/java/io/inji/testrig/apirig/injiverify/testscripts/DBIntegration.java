package io.inji.testrig.apirig.injiverify.testscripts;

import java.lang.reflect.Field;
import java.util.List;
import java.util.Map;

import org.apache.log4j.Level;
import org.apache.log4j.Logger;
import org.json.JSONObject;
import org.testng.ITest;
import org.testng.ITestContext;
import org.testng.ITestResult;
import org.testng.Reporter;
import org.testng.SkipException;
import org.testng.annotations.AfterMethod;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.DataProvider;
import org.testng.annotations.Test;
import org.testng.internal.BaseTestMethod;
import org.testng.internal.TestResult;

import io.inji.testrig.apirig.injiverify.utils.ExtendedDBManager;
import io.inji.testrig.apirig.injiverify.utils.InjiVerifyConfigManager;
import io.inji.testrig.apirig.injiverify.utils.InjiVerifyUtil;
import io.mosip.testrig.apirig.dto.TestCaseDTO;
import io.mosip.testrig.apirig.testrunner.HealthChecker;
import io.mosip.testrig.apirig.utils.AdminTestException;
import io.mosip.testrig.apirig.utils.GlobalConstants;
import io.mosip.testrig.apirig.utils.GlobalMethods;
import io.mosip.testrig.apirig.utils.SecurityXSSException;
import io.restassured.response.Response;

public class DBIntegration extends InjiVerifyUtil implements ITest {
	private static final Logger logger = Logger.getLogger(DBIntegration.class);
	protected String testCaseName = "";
	public String idKeyName = null;
	public Response response = null;

	/**
	 * get current testcaseName
	 */
	@Override
	public String getTestName() {
		return testCaseName;

	}

	@BeforeClass
	public static void setLogLevel() {
		if (InjiVerifyConfigManager.IsDebugEnabled())
			logger.setLevel(Level.ALL);
		else
			logger.setLevel(Level.ERROR);
	}

	/**
	 * Data provider class provides test case list
	 * 
	 * @return object of data provider
	 */
	@DataProvider(name = "testcaselist")
	public Object[] getTestCaseList(ITestContext context) {
		String ymlFile = context.getCurrentXmlTest().getLocalParameters().get("ymlFile");
		idKeyName = context.getCurrentXmlTest().getLocalParameters().get("idKeyName");
		logger.info("Started executing yml: " + ymlFile);
		return getYmlTestData(ymlFile);
	}

	/**
	 * Test method for OTP Generation execution
	 * 
	 * @param objTestParameters
	 * @param testScenario
	 * @param testcaseName
	 * @throws Exception
	 */
	@Test(dataProvider = "testcaselist")
	public void test(TestCaseDTO testCaseDTO) throws Exception, SecurityXSSException {
		testCaseName = testCaseDTO.getTestCaseName();
		InjiVerifyUtil.isTestCaseValidForExecution(testCaseDTO);
		if (HealthChecker.signalTerminateExecution) {
			throw new SkipException(
					GlobalConstants.TARGET_ENV_HEALTH_CHECK_FAILED + HealthChecker.healthCheckFailureMapS);
		}

		String inputJson = getJsonFromTemplate(testCaseDTO.getInput(), testCaseDTO.getInputTemplate());

		inputJson = inputJsonKeyWordHandeler(inputJson, testCaseName);

		// log the processed input to aid debugging
		logger.info("Processed input JSON for " + testCaseName + " => " + inputJson);

		// determine SQL query: either the entire string (raw SQL) or from a JSON field
		String sqlQuery;
		if (inputJson == null || inputJson.trim().isEmpty()) {
			throw new AdminTestException("Input JSON is empty for testCase: " + testCaseName);
		}
		String trimmed = inputJson.trim();
		// remove any BOM characters that might be present from file/template
		trimmed = trimmed.replace("\uFEFF", "");
		if (!trimmed.startsWith("{")) {
			// not a JSON object, treat the input as raw query
			sqlQuery = inputJson;
		} else {
			try {
				JSONObject jsonObject = new JSONObject(inputJson);
				sqlQuery = jsonObject.getString("db_query");
			} catch (Exception e) {
				logger.error("Failed to parse input JSON for " + testCaseName + ": " + inputJson, e);
				throw new AdminTestException("Malformed input JSON for " + testCaseName + ": " + e.getMessage());
			}
		}

		logger.info("DB queries = " + sqlQuery);

		GlobalMethods.reportRequest(null, sqlQuery, "SQL_Insert_Query");

		try {
			if (sqlQuery.trim().toUpperCase().startsWith("SELECT")) {
				List<Map<String, Object>> result = ExtendedDBManager.executeSelectQuery(
						InjiVerifyConfigManager.getInjiVerifyDBURL(),
						InjiVerifyConfigManager.getproperty("db-su-user"),
						InjiVerifyConfigManager.getproperty("postgres-password"),
						InjiVerifyConfigManager.getproperty("inji_verify_schema"), sqlQuery);
				GlobalMethods.reportResponse("No Header", sqlQuery, result.toString(), true);

				logger.info("DB SELECT Result: " + result);

				// 👇 if you only expect one row
				if (!result.isEmpty()) {
					Map<String, Object> row = result.get(0);
					updateCacheFromRow(row, idKeyName, testCaseName);
				}

			} else {
				ExtendedDBManager.executeDBWithQueries(InjiVerifyConfigManager.getInjiVerifyDBURL(),
						InjiVerifyConfigManager.getproperty("db-su-user"),
						InjiVerifyConfigManager.getproperty("postgres-password"),
						InjiVerifyConfigManager.getproperty("inji_verify_schema"), sqlQuery);
				GlobalMethods.reportResponse("No Header", sqlQuery, "Success", true);
			}
		} catch (Exception e) {
			throw new AdminTestException(e.getMessage());
		}

	}

	/**
	 * The method ser current test name to result
	 * 
	 * @param result
	 */
	@AfterMethod(alwaysRun = true)
	public void setResultTestName(ITestResult result) {
		result.setAttribute("TestCaseName", testCaseName);
	}
}