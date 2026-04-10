package io.inji.testrig.apirig.injiverify.testscripts;

import java.util.ArrayList;
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

import io.inji.testrig.apirig.injiverify.utils.InjiVerifyConfigManager;
import io.inji.testrig.apirig.injiverify.utils.InjiVerifyUtil;
import io.mosip.testrig.apirig.dto.OutputValidationDto;
import io.mosip.testrig.apirig.dto.TestCaseDTO;
import io.mosip.testrig.apirig.testrunner.HealthChecker;
import io.mosip.testrig.apirig.utils.AdminTestException;
import io.mosip.testrig.apirig.utils.AdminTestUtil;
import io.mosip.testrig.apirig.utils.AuthenticationTestException;
import io.mosip.testrig.apirig.utils.GlobalConstants;
import io.mosip.testrig.apirig.utils.OutputValidationUtil;
import io.mosip.testrig.apirig.utils.ReportUtil;
import io.mosip.testrig.apirig.utils.SecurityXSSException;
import io.restassured.response.Response;

public class PostWithBodyAndPathParams extends InjiVerifyUtil implements ITest {
	private static final Logger logger = Logger.getLogger(PostWithBodyAndPathParams.class);
	protected String testCaseName = "";
	String pathParams = null;
	public Response response = null;

	@BeforeClass
	public static void setLogLevel() {
		if (InjiVerifyConfigManager.IsDebugEnabled())
			logger.setLevel(Level.ALL);
		else
			logger.setLevel(Level.ERROR);
	}

	/**
	 * get current testcaseName
	 */
	@Override
	public String getTestName() {
		return testCaseName;
	}

	/**
	 * Data provider class provides test case list
	 * 
	 * @return object of data provider
	 */
	@DataProvider(name = "testcaselist")
	public Object[] getTestCaseList(ITestContext context) {
		String ymlFile = context.getCurrentXmlTest().getLocalParameters().get("ymlFile");
		pathParams = context.getCurrentXmlTest().getLocalParameters().get("pathParams");
		logger.info("Started executing yml: " + ymlFile);
		return getYmlTestData(ymlFile);
	}

	/**
	 * Test method for POST requests with body and path parameters.
	 * 
	 * `@param` testCaseDTO the test case data transfer object
	 * `@throws` AuthenticationTestException
	 * `@throws` AdminTestException
	 * `@throws` SecurityXSSException
	 */
	@Test(dataProvider = "testcaselist")
	public void test(TestCaseDTO testCaseDTO) throws AuthenticationTestException, AdminTestException, SecurityXSSException {
		testCaseName = testCaseDTO.getTestCaseName();
		String[] templateFields = testCaseDTO.getTemplateFields();
		testCaseName = InjiVerifyUtil.isTestCaseValidForExecution(testCaseDTO);
		if (HealthChecker.signalTerminateExecution) {
			throw new SkipException(
					GlobalConstants.TARGET_ENV_HEALTH_CHECK_FAILED + HealthChecker.healthCheckFailureMapS);
		}

		testCaseDTO = AdminTestUtil.filterHbs(testCaseDTO);
		String inputJson = getJsonFromTemplate(testCaseDTO.getInput(), testCaseDTO.getInputTemplate());
		String outputJson = getJsonFromTemplate(testCaseDTO.getOutput(), testCaseDTO.getOutputTemplate());


		if (inputJson != null && inputJson.contains("$RESPONSE_CODE$")) {
			String responseCode = getResponseCode(); // must be implemented or already available
			if (responseCode == null || responseCode.isEmpty()) {
				throw new AdminTestException("RESPONSE_CODE is null or empty. Cannot proceed.");
			}
			inputJson = inputJson.replace("$RESPONSE_CODE$", responseCode);
			logger.info("Resolved RESPONSE_CODE: " + responseCode);
		}

		logger.info("Final Input JSON: " + inputJson);

		if (testCaseDTO.getTemplateFields() != null && templateFields.length > 0) {
			ArrayList<JSONObject> inputtestCases = AdminTestUtil.getInputTestCase(testCaseDTO);
			ArrayList<JSONObject> outputtestcase = AdminTestUtil.getOutputTestCase(testCaseDTO);
			for (int i = 0; i < languageList.size(); i++) {

				String finalInput = getJsonFromTemplate(inputtestCases.get(i).toString(),
						testCaseDTO.getInputTemplate());

				if (finalInput != null && finalInput.contains("$RESPONSE_CODE$")) {
					String responseCode = getResponseCode();
					if (responseCode == null || responseCode.isEmpty()) {
						throw new AdminTestException("RESPONSE_CODE is null or empty. Cannot proceed.");
					}
					finalInput = finalInput.replace("$RESPONSE_CODE$", responseCode);
					logger.info("Resolved RESPONSE_CODE (loop): " + responseCode);
				}

				logger.info("Final Input JSON (loop): " + finalInput);

				response = postWithPathParamsBodyAndCookie(
						injiVerifyBaseUrl + testCaseDTO.getEndPoint(),
						finalInput,
						COOKIENAME,
						testCaseDTO.getRole(),
						testCaseDTO.getTestCaseName(),
						pathParams);

				Map<String, List<OutputValidationDto>> ouputValid = OutputValidationUtil.doJsonOutputValidation(
						response.asString(),
						getJsonFromTemplate(outputtestcase.get(i).toString(), testCaseDTO.getOutputTemplate()),
						testCaseDTO, response.getStatusCode());
				Reporter.log(ReportUtil.getOutputValidationReport(ouputValid));

				if (!OutputValidationUtil.publishOutputResult(ouputValid))
					throw new AdminTestException("Failed at output validation");
			}
		} else {

			response = postWithPathParamsBodyAndCookie(
					injiVerifyBaseUrl + testCaseDTO.getEndPoint(),
					inputJson,
					COOKIENAME,
					testCaseDTO.getRole(),
					testCaseDTO.getTestCaseName(),
					pathParams);

			Map<String, List<OutputValidationDto>> ouputValid = OutputValidationUtil
					.doJsonOutputValidation(response.asString(), outputJson, testCaseDTO, response.getStatusCode());
			Reporter.log(ReportUtil.getOutputValidationReport(ouputValid));

			if (!OutputValidationUtil.publishOutputResult(ouputValid))
				throw new AdminTestException("Failed at output validation");
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