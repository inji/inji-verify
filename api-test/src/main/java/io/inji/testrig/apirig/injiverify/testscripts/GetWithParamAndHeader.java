package io.inji.testrig.apirig.injiverify.testscripts;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.Base64;
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

import com.auth0.jwt.JWT;
import com.auth0.jwt.interfaces.DecodedJWT;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonMappingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.itextpdf.text.pdf.PdfReader;
import com.itextpdf.text.pdf.parser.PdfTextExtractor;

import io.inji.testrig.apirig.injiverify.utils.InjiVerifyConfigManager;
import io.inji.testrig.apirig.injiverify.utils.InjiVerifyUtil;
import io.mosip.testrig.apirig.dto.OutputValidationDto;
import io.mosip.testrig.apirig.dto.TestCaseDTO;
import io.mosip.testrig.apirig.testrunner.HealthChecker;
import io.mosip.testrig.apirig.utils.AdminTestException;
import io.mosip.testrig.apirig.utils.AdminTestUtil;
import io.mosip.testrig.apirig.utils.AuthTestsUtil;
import io.mosip.testrig.apirig.utils.AuthenticationTestException;
import io.mosip.testrig.apirig.utils.EncryptDecrptUtil;
import io.mosip.testrig.apirig.utils.GlobalConstants;
import io.mosip.testrig.apirig.utils.GlobalMethods;
import io.mosip.testrig.apirig.utils.OutputValidationUtil;
import io.mosip.testrig.apirig.utils.ReportUtil;
import io.mosip.testrig.apirig.utils.SecurityXSSException;
import io.restassured.response.Response;

public class GetWithParamAndHeader extends InjiVerifyUtil implements ITest {
	private static final Logger logger = Logger.getLogger(GetWithParamAndHeader.class);
	protected String testCaseName = "";
	public Response response = null;
	public String pathParams = null;
	public String headers = null;
	public byte[] pdf = null;
	public String pdfAsText = null;
	Map<String, List<OutputValidationDto>> ouputValid = null;

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
		headers = context.getCurrentXmlTest().getLocalParameters().get("headers");
		pathParams = context.getCurrentXmlTest().getLocalParameters().get("pathParams");
		logger.info("Started executing yml: " + ymlFile);
		return getYmlTestData(ymlFile);
	}

	/**
	 * Test method for OTP Generation execution
	 * 
	 * @param objTestParameters
	 * @param testScenario
	 * @param testcaseName
	 * @throws AuthenticationTestException
	 * @throws AdminTestException
	 * @throws JsonProcessingException 
	 * @throws JsonMappingException 
	 */
	@Test(dataProvider = "testcaselist")
	public void test(TestCaseDTO testCaseDTO) throws AuthenticationTestException, AdminTestException, SecurityXSSException, JsonMappingException, JsonProcessingException {
		testCaseName = testCaseDTO.getTestCaseName();
		testCaseName = isTestCaseValidForExecution(testCaseDTO);
		if (HealthChecker.signalTerminateExecution) {
			throw new SkipException(
					GlobalConstants.TARGET_ENV_HEALTH_CHECK_FAILED + HealthChecker.healthCheckFailureMapS);
		}

			String inputJson = getJsonFromTemplate(testCaseDTO.getInput(), testCaseDTO.getInputTemplate());

			inputJson = inputJsonModuleKeyWordHandler(inputJson, testCaseName);

			String outputJson = getJsonFromTemplate(testCaseDTO.getOutput(), testCaseDTO.getOutputTemplate());
			outputJson = inputJsonModuleKeyWordHandler(outputJson, testCaseName);

			response = getWithPathParamsBodyHeadersAndCookie(injiVerifyBaseUrl + testCaseDTO.getEndPoint(), inputJson,
					COOKIENAME, testCaseDTO.getRole(), testCaseDTO.getTestCaseName(), pathParams, headers);

			if (testCaseName.contains("_GetVpRequestWithDID_")) {
			    String finalJsonString = InjiVerifyUtil.decodeAndCombineJwt(response.asString());

			    // Report both header and payload separately if needed
			    DecodedJWT jwt = JWT.decode(response.asString());
			    String headerJson = InjiVerifyUtil.decodeBase64Url(jwt.getHeader());
			    String payloadJson = InjiVerifyUtil.decodeBase64Url(jwt.getPayload());
			    GlobalMethods.reportResponse(headerJson, null, payloadJson, true);

			    ouputValid = OutputValidationUtil.doJsonOutputValidation(finalJsonString, outputJson, testCaseDTO,
			            response.getStatusCode());
			} else {

				ouputValid = OutputValidationUtil.doJsonOutputValidation(response.asString(), outputJson, testCaseDTO,
						response.getStatusCode());
			}
			Reporter.log(ReportUtil.getOutputValidationReport(ouputValid));
			if (!OutputValidationUtil.publishOutputResult(ouputValid))
				throw new AdminTestException("Failed at output validation");
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

