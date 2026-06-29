package io.inji.testrig.apirig.injiverify.testscripts;

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
import io.mosip.testrig.apirig.utils.AuthenticationTestException;
import io.mosip.testrig.apirig.utils.GlobalConstants;
import io.mosip.testrig.apirig.utils.OutputValidationUtil;
import io.mosip.testrig.apirig.utils.ReportUtil;
import io.mosip.testrig.apirig.utils.SecurityXSSException;
import io.restassured.response.Response;

public class SimplePostWithContentType extends InjiVerifyUtil implements ITest {
	private static final Logger logger = Logger.getLogger(SimplePostWithContentType.class);
	private static final String CONTENT_TYPE_FIELD = "contentType";

	protected String testCaseName = "";
	public Response response = null;
	public boolean auditLogCheck = false;

	@BeforeClass
	public static void setLogLevel() {
		if (InjiVerifyConfigManager.IsDebugEnabled())
			logger.setLevel(Level.ALL);
		else
			logger.setLevel(Level.ERROR);
	}

	@Override
	public String getTestName() {
		return testCaseName;
	}

	@DataProvider(name = "testcaselist")
	public Object[] getTestCaseList(ITestContext context) {
		String ymlFile = context.getCurrentXmlTest().getLocalParameters().get("ymlFile");
		logger.info("Started executing yml: " + ymlFile);
		return getYmlTestData(ymlFile);
	}

	@Test(dataProvider = "testcaselist")
	public void test(TestCaseDTO testCaseDTO) throws AuthenticationTestException, AdminTestException, SecurityXSSException {
		testCaseName = testCaseDTO.getTestCaseName();
		testCaseName = InjiVerifyUtil.isTestCaseValidForExecution(testCaseDTO);
		auditLogCheck = testCaseDTO.isAuditLogCheck();
		if (HealthChecker.signalTerminateExecution) {
			throw new SkipException(
					GlobalConstants.TARGET_ENV_HEALTH_CHECK_FAILED + HealthChecker.healthCheckFailureMapS);
		}

		String contentType = "application/json";
		String templateInput;
		try {
			JSONObject inputObj = new JSONObject(testCaseDTO.getInput());
			if (inputObj.has(CONTENT_TYPE_FIELD)) {
				contentType = inputObj.getString(CONTENT_TYPE_FIELD);
				inputObj.remove(CONTENT_TYPE_FIELD);
			}
			templateInput = inputObj.toString();
		} catch (Exception e) {
			logger.error("Error parsing input JSON for content type handling in " + testCaseName, e);
			throw new AdminTestException("Malformed input JSON for " + testCaseName + ": " + e.getMessage());
		}

		String requestBody = getJsonFromTemplate(templateInput, testCaseDTO.getInputTemplate());
		if (requestBody == null || requestBody.isBlank()) {
			throw new AdminTestException("Request body is empty for test case: " + testCaseName);
		}
		response = postWithBodyAndCustomContentType(injiVerifyBaseUrl + testCaseDTO.getEndPoint(), requestBody,
				contentType, auditLogCheck, COOKIENAME, testCaseDTO.getRole(), testCaseDTO.getTestCaseName());
		if (response == null) {
			throw new AdminTestException("No response received for test case: " + testCaseName);
		}

		Map<String, List<OutputValidationDto>> ouputValid = OutputValidationUtil.doJsonOutputValidation(
				response.asString(),
				getJsonFromTemplate(testCaseDTO.getOutput(), testCaseDTO.getOutputTemplate()), testCaseDTO,
				response.getStatusCode());

		Reporter.log(ReportUtil.getOutputValidationReport(ouputValid));

		if (!OutputValidationUtil.publishOutputResult(ouputValid)) {
			throw new AdminTestException("Failed at output validation");
		}
	}

	@AfterMethod(alwaysRun = true)
	public void setResultTestName(ITestResult result) {
		result.setAttribute("TestCaseName", testCaseName);
	}
}
