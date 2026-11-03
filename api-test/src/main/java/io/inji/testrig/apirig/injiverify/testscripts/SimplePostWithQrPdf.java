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
import io.inji.testrig.apirig.injiverify.utils.QrPdfUtil;
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

/**
 * Same as SimplePost, but the request field "dcqlQuery" may hold a VC PDF file name
 * (e.g. mock.pdf). The QR printed in the PDF is read, its dcql_query is extracted
 * and put into the request before calling the API.
 */
public class SimplePostWithQrPdf extends InjiVerifyUtil implements ITest {
	private static final Logger logger = Logger.getLogger(SimplePostWithQrPdf.class);
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

		String inputJson = getJsonFromTemplate(testCaseDTO.getInput(), testCaseDTO.getInputTemplate());

		// Read the QR from the PDF and replace the file name with the dcql_query found in it
		JSONObject requestJson = new JSONObject(inputJson);
		String dcqlField = requestJson.optString("dcqlQuery", "");
		if (QrPdfUtil.isPdfFileName(dcqlField)) {
			try {
				requestJson.put("dcqlQuery", new JSONObject(QrPdfUtil.readDcqlQueryFromPdf(dcqlField)));
			} catch (Exception e) {
				throw new AdminTestException("Unable to read dcql_query from PDF " + dcqlField + ": " + e);
			}
			inputJson = requestJson.toString();
		}

		response = postWithBodyAndCookie(injiVerifyBaseUrl + testCaseDTO.getEndPoint(), inputJson, auditLogCheck,
				COOKIENAME, testCaseDTO.getRole(), testCaseDTO.getTestCaseName());
		Map<String, List<OutputValidationDto>> ouputValid = null;
		ouputValid = OutputValidationUtil.doJsonOutputValidation(response.asString(),
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