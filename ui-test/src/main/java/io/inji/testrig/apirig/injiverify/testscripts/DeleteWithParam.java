package io.inji.testrig.apirig.injiverify.testscripts;

import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import io.mosip.testrig.apirig.utils.*;
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

import api.InjiVerifyConfigManager;
import api.InjiVerifyUtil;
import io.mosip.testrig.apirig.dto.OutputValidationDto;
import io.mosip.testrig.apirig.dto.TestCaseDTO;
import io.mosip.testrig.apirig.testrunner.HealthChecker;
import io.restassured.response.Response;

public class DeleteWithParam extends InjiVerifyUtil implements ITest {
	private static final Logger logger = Logger.getLogger(DeleteWithParam.class);
	protected String testCaseName = "";
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
		logger.info("Started executing yml: " + ymlFile);
		return getYmlTestData(ymlFile);
	}

	/**
	 * Test method for OTP Generation execution
	 *
	 * @throws AuthenticationTestException
	 * @throws AdminTestException
	 */
	@Test(dataProvider = "testcaselist")
	public void test(TestCaseDTO testCaseDTO) throws AuthenticationTestException, AdminTestException {
		testCaseName = testCaseDTO.getTestCaseName();
		testCaseDTO = InjiVerifyUtil.isTestCaseValidForTheExecution(testCaseDTO);
		if (HealthChecker.signalTerminateExecution) {
			throw new SkipException(
					GlobalConstants.TARGET_ENV_HEALTH_CHECK_FAILED + HealthChecker.healthCheckFailureMapS);
		}
		String[] templateFields = testCaseDTO.getTemplateFields();

		if (testCaseDTO.getTemplateFields() != null && templateFields.length > 0) {
			ArrayList<JSONObject> inputtestCases = AdminTestUtil.getInputTestCase(testCaseDTO);
			ArrayList<JSONObject> outputtestcase = AdminTestUtil.getOutputTestCase(testCaseDTO);

			for (int i = 0; i < languageList.size(); i++) {
                try {
                    response = deleteWithPathParamAndCookie(ApplnURI + testCaseDTO.getEndPoint(),
                            getJsonFromTemplate(inputtestCases.get(i).toString(), testCaseDTO.getInputTemplate()),
                            COOKIENAME, testCaseDTO.getRole(), testCaseDTO.getTestCaseName());
                } catch (SecurityXSSException e) {
                    throw new RuntimeException(e);
                }

                Map<String, List<OutputValidationDto>> ouputValid = OutputValidationUtil.doJsonOutputValidation(
						response.asString(),
						getJsonFromTemplate(outputtestcase.get(i).toString(), testCaseDTO.getOutputTemplate()),
						testCaseDTO, response.getStatusCode());
				Reporter.log(ReportUtil.getOutputValidationReport(ouputValid));

				if (!OutputValidationUtil.publishOutputResult(ouputValid))
					throw new AdminTestException("Failed at output validation");
			}
		}

		else {
			
			String inputJson = getJsonFromTemplate(testCaseDTO.getInput(), testCaseDTO.getInputTemplate());
			
			inputJson = InjiVerifyUtil.inputstringKeyWordHandeler(inputJson, testCaseName);

			if (testCaseName.contains("ESignet_")) {
				if (InjiVerifyConfigManager.isInServiceNotDeployedList(GlobalConstants.ESIGNET)) {
					throw new SkipException("esignet is not deployed hence skipping the testcase");
				}

				String tempUrl = ApplnURI;

				if (testCaseDTO.getEndPoint().startsWith("$SUNBIRDBASEURL$") && testCaseName.contains("SunBirdR")) {

					if (InjiVerifyConfigManager.isInServiceNotDeployedList("sunbirdrc"))
						throw new SkipException(GlobalConstants.SERVICE_NOT_DEPLOYED_MESSAGE);

					if (InjiVerifyConfigManager.getSunbirdBaseURL() != null && !InjiVerifyConfigManager.getSunbirdBaseURL().isBlank())
						tempUrl = InjiVerifyConfigManager.getSunbirdBaseURL();
					testCaseDTO.setEndPoint(testCaseDTO.getEndPoint().replace("$SUNBIRDBASEURL$", ""));
				}

                try {
                    response = deleteWithPathParamAndCookie(tempUrl + testCaseDTO.getEndPoint(), inputJson, COOKIENAME,
                            testCaseDTO.getRole(), testCaseDTO.getTestCaseName());
                } catch (SecurityXSSException e) {
                    throw new RuntimeException(e);
                }

            } else {
                try {
                    response = deleteWithPathParamAndCookie(ApplnURI + testCaseDTO.getEndPoint(), inputJson, COOKIENAME,
                            testCaseDTO.getRole(), testCaseDTO.getTestCaseName());
                } catch (SecurityXSSException e) {
                    throw new RuntimeException(e);
                }
            }
			Map<String, List<OutputValidationDto>> ouputValid = OutputValidationUtil.doJsonOutputValidation(
					response.asString(), getJsonFromTemplate(testCaseDTO.getOutput(), testCaseDTO.getOutputTemplate()),
					testCaseDTO, response.getStatusCode());
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
		try {
			Field method = TestResult.class.getDeclaredField("m_method");
			method.setAccessible(true);
			method.set(result, result.getMethod().clone());
			BaseTestMethod baseTestMethod = (BaseTestMethod) result.getMethod();
			Field f = baseTestMethod.getClass().getSuperclass().getDeclaredField("m_methodName");
			f.setAccessible(true);
			f.set(baseTestMethod, testCaseName);
		} catch (Exception e) {
			Reporter.log("Exception : " + e.getMessage());
		}
	}
}