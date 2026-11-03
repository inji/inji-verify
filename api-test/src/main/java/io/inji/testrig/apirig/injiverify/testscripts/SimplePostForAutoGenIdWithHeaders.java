package io.inji.testrig.apirig.injiverify.testscripts;

import java.security.NoSuchAlgorithmException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import io.restassured.response.Response;

import org.apache.log4j.Level;
import org.apache.log4j.Logger;
import org.json.JSONObject;
import org.testng.Assert;
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
import io.mosip.testrig.apirig.utils.RestClient;
import io.mosip.testrig.apirig.utils.SecurityXSSException;

public class SimplePostForAutoGenIdWithHeaders
        extends InjiVerifyUtil implements ITest {

    private static final Logger logger =
            Logger.getLogger(SimplePostForAutoGenIdWithHeaders.class);

    private static final String JSON_KEY_COOKIE = "cookie";
    private static final String HEADER_CONTENT_TYPE = "Content-Type";
    private static final String HEADER_ACCEPT = "Accept";
    private static final String HEADER_COOKIE = "Cookie";

    protected String testCaseName = "";

    public String idKeyName;
    public Response response;
    public boolean sendEsignetToken;
    public boolean auditLogCheck;

    @BeforeClass
    public static void setLogLevel() {
        if (InjiVerifyConfigManager.IsDebugEnabled()) {
            logger.setLevel(Level.ALL);
        } else {
            logger.setLevel(Level.ERROR);
        }
    }

    @Override
    public String getTestName() {
        return testCaseName;
    }

    @DataProvider(name = "testcaselist")
    public Object[] getTestCaseList(ITestContext context) {
        String ymlFile =
                context.getCurrentXmlTest()
                        .getLocalParameters()
                        .get("ymlFile");

        sendEsignetToken =
                context.getCurrentXmlTest()
                        .getLocalParameters()
                        .containsKey("sendEsignetToken");

        idKeyName =
                context.getCurrentXmlTest()
                        .getLocalParameters()
                        .get("idKeyName");

        logger.info("Started executing yml: " + ymlFile);

        return getYmlTestData(ymlFile);
    }

    @Test(dataProvider = "testcaselist")
    public void test(TestCaseDTO testCaseDTO)
            throws AuthenticationTestException,
            AdminTestException,
            NoSuchAlgorithmException,
            SecurityXSSException {

        testCaseName = testCaseDTO.getTestCaseName();

        testCaseName =
                InjiVerifyUtil.isTestCaseValidForExecution(testCaseDTO);

        if (HealthChecker.signalTerminateExecution) {
            throw new SkipException(
                    GlobalConstants.TARGET_ENV_HEALTH_CHECK_FAILED
                            + HealthChecker.healthCheckFailureMapS);
        }

        String inputJson =
                getJsonFromTemplate(
                        testCaseDTO.getInput(),
                        testCaseDTO.getInputTemplate());

        inputJson =
                inputJsonModuleKeyWordHandler(inputJson, testCaseName);

        inputJson =
                InjiVerifyUtil.replaceVpSessionInputPlaceholders(inputJson);

        Map<String, String> requestHeaders =
                getHeadersForDcApiTest(testCaseName);

        boolean sendCookieAsHeader = false;
        String cookieValue = null;

        if (inputJson != null) {
            try {
                JSONObject requestJson = new JSONObject(inputJson);

                if (requestJson.has(JSON_KEY_COOKIE)) {
                    cookieValue =
                            requestJson.optString(JSON_KEY_COOKIE);

                    if (cookieValue != null
                            && !cookieValue.isEmpty()) {
                        requestJson.remove(JSON_KEY_COOKIE);
                        inputJson = requestJson.toString();
                        sendCookieAsHeader = true;
                    }
                }
            } catch (Exception e) {
                logger.error(
                        "Error parsing input JSON for cookie handling",
                        e);
            }
        }

        String outputJson =
                getJsonFromTemplate(
                        testCaseDTO.getOutput(),
                        testCaseDTO.getOutputTemplate());

        outputJson =
                inputJsonModuleKeyWordHandler(outputJson, testCaseName);

        if (sendCookieAsHeader) {
            io.restassured.specification.RequestSpecification request =
                    io.restassured.RestAssured
                            .given()
                            .header(
                                    HEADER_CONTENT_TYPE,
                                    "application/json")
                            .header(
                                    HEADER_ACCEPT,
                                    "application/json")
                            .header(
                                    HEADER_COOKIE,
                                    cookieValue);

            if (!requestHeaders.isEmpty()) {
                request.headers(requestHeaders);
            }

            response =
                    request
                            .body(inputJson)
                            .post(
                                    injiVerifyBaseUrl
                                            + testCaseDTO.getEndPoint());

        } else if (!requestHeaders.isEmpty()
                && "noauth".equals(testCaseDTO.getRole())) {

            response =
                    RestClient.postRequestWithMultipleHeadersWithoutCookie(
                            injiVerifyBaseUrl
                                    + testCaseDTO.getEndPoint(),
                            inputJson,
                            "application/json",
                            "application/json",
                            requestHeaders);

            writeAutoGeneratedIdIfRequired(response);

        } else if (!requestHeaders.isEmpty()) {

            String token =
                    kernelAuthLib.getTokenByRole(
                            testCaseDTO.getRole());

            response =
                    RestClient.postRequestWithMultipleHeaders(
                            injiVerifyBaseUrl
                                    + testCaseDTO.getEndPoint(),
                            inputJson,
                            "application/json",
                            "application/json",
                            COOKIENAME,
                            token,
                            requestHeaders);

            writeAutoGeneratedIdIfRequired(response);

        } else {
            response =
                    postWithBodyAndCookieForAutoGeneratedId(
                            injiVerifyBaseUrl
                                    + testCaseDTO.getEndPoint(),
                            inputJson,
                            auditLogCheck,
                            COOKIENAME,
                            testCaseDTO.getRole(),
                            testCaseDTO.getTestCaseName(),
                            idKeyName,
                            sendEsignetToken);
        }

        try {
            InjiVerifyUtil
                    .captureTransactionCookieFromVpSessionRequestResponse(
                            testCaseDTO.getEndPoint(),
                            testCaseDTO.getTestCaseName(),
                            response,
                            inputJson);
        } catch (Exception e) {
            logger.error(
                    "Error while extracting transaction cookie",
                    e);
        }

        validateDcApiResponse(testCaseName, response);

        InjiVerifyUtil
                .validateAuthorizationDetailsResponseCodeValidationRequiredFromInput(
                        testCaseDTO.getOutputTemplate(),
                        testCaseDTO.isCheckOnlyStatusCodeInResponse(),
                        inputJson,
                        response.asString());

        Map<String, List<OutputValidationDto>> outputValidation =
                OutputValidationUtil.doJsonOutputValidation(
                        response.asString(),
                        outputJson,
                        testCaseDTO,
                        response.getStatusCode());

        Reporter.log(
                ReportUtil.getOutputValidationReport(outputValidation));

        if (!OutputValidationUtil.publishOutputResult(
                outputValidation)) {
            throw new AdminTestException(
                    "Failed at output validation");
        }
    }

    private void writeAutoGeneratedIdIfRequired(
            Response currentResponse) {

        if (currentResponse == null) {
            return;
        }

        try {
            String requestId =
                    currentResponse.jsonPath()
                            .getString("requestId");

            if (requestId != null
                    && !requestId.trim().isEmpty()) {

                writeAutoGeneratedId(
                        currentResponse,
                        "requestId",
                        testCaseName);
            }

            String transactionId =
                    currentResponse.jsonPath()
                            .getString("transactionId");

            if (transactionId != null
                    && !transactionId.trim().isEmpty()) {

                writeAutoGeneratedId(
                        currentResponse,
                        "transactionId",
                        testCaseName);
            }

        } catch (Exception e) {
            logger.debug(
                    "Unable to save generated IDs for testcase: "
                            + testCaseName,
                    e);
        }
    }
    private Map<String, String> getHeadersForDcApiTest(
            String currentTestCaseName) {

        Map<String, String> headers = new HashMap<>();

        if (currentTestCaseName.contains("DC_03")) {
            headers.put(
                    "Referer",
                    "https://verify.example.com/verify");

        } else if (!currentTestCaseName.contains("DC_07")) {
            headers.put(
                    "Origin",
                    "https://verify.example.com");
        }

        return headers;
    }

    private void validateDcApiResponse(
            String currentTestCaseName,
            Response currentResponse) {

        int statusCode = currentResponse.getStatusCode();

        if (currentTestCaseName.contains("DC_01")
                || currentTestCaseName.contains("DC_02")) {

            Assert.assertEquals(statusCode, 201);

            JSONObject body =
                    new JSONObject(currentResponse.asString());

            Assert.assertTrue(body.has("requestId"));
            Assert.assertTrue(body.has("transactionId"));
            Assert.assertTrue(body.has("requestUri"));
            Assert.assertTrue(body.has("responseUri"));

            Assert.assertTrue(
                    body.getString("responseUri")
                            .endsWith("/vp-submission/dc-api"));

            Assert.assertFalse(body.has("authorizationRequest"));
            Assert.assertFalse(body.has("authorization_request"));
            Assert.assertFalse(body.has("authorizationDetails"));

            if (currentTestCaseName.contains(
                    "vp-session-request")) {
                Assert.assertNotNull(
                        currentResponse.getCookie("transaction_id"));
            }
        }

        if (currentTestCaseName.contains("DC_04")
                || currentTestCaseName.contains("DC_05")) {

            assertInvalidRequest(
                    currentResponse,
                    "DC API response_mode requires a decentralized_identifier or x509_san_dns client_id.");
        }

        if (currentTestCaseName.contains("DC_06")) {

            assertInvalidRequest(
                    currentResponse,
                    "responseCodeValidationRequired is not supported with response_mode=dc_api.");
        }

        if (currentTestCaseName.contains("DC_07")) {

            assertInvalidRequest(
                    currentResponse,
                    "Unable to determine verifier origin from Origin or Referer headers.");
        }

        if (currentTestCaseName.contains("DC_08")) {

            assertInvalidRequest(
                    currentResponse,
                    "responseMode must be direct_post or dc_api.");
        }

        if (currentTestCaseName.contains("DC_09")) {

            Assert.assertEquals(statusCode, 201);

            JSONObject body =
                    new JSONObject(currentResponse.asString());

            Assert.assertFalse(body.has("responseUri"));
        }
    }

    private void assertInvalidRequest(
            Response currentResponse,
            String expectedMessage) {

        Assert.assertEquals(
                currentResponse.getStatusCode(),
                400);

        JSONObject body =
                new JSONObject(currentResponse.asString());

        Assert.assertEquals(
                body.getString("errorCode"),
                "invalid_request");

        Assert.assertEquals(
                body.getString("errorMessage"),
                expectedMessage);
    }

    @AfterMethod(alwaysRun = true)
    public void setResultTestName(ITestResult result) {
        result.setAttribute(
                "TestCaseName",
                testCaseName);
    }
}