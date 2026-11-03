package io.inji.testrig.apirig.injiverify.testscripts;

import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.TimeUnit;

import org.json.JSONObject;
import org.testng.Assert;
import org.testng.ITest;
import org.testng.ITestContext;
import org.testng.ITestResult;
import org.testng.SkipException;
import org.testng.annotations.AfterMethod;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.DataProvider;
import org.testng.annotations.Test;

import io.inji.testrig.apirig.injiverify.utils.InjiVerifyConfigManager;
import io.inji.testrig.apirig.injiverify.utils.InjiVerifyConstants;
import io.inji.testrig.apirig.injiverify.utils.InjiVerifyUtil;
import io.mosip.testrig.apirig.dto.TestCaseDTO;
import io.mosip.testrig.apirig.testrunner.HealthChecker;
import io.mosip.testrig.apirig.utils.AdminTestException;
import io.mosip.testrig.apirig.utils.AuthenticationTestException;
import io.mosip.testrig.apirig.utils.GlobalConstants;
import io.mosip.testrig.apirig.utils.SecurityXSSException;
import io.restassured.RestAssured;
import io.restassured.response.Response;

public class PostDcApiSubmission extends InjiVerifyUtil implements ITest {

    protected String testCaseName = "";
    protected Response response;

    @BeforeClass
    public static void setLogLevel() {
        if (InjiVerifyConfigManager.IsDebugEnabled()) {
            org.apache.log4j.Logger
                    .getLogger(PostDcApiSubmission.class)
                    .setLevel(org.apache.log4j.Level.ALL);
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

        return getYmlTestData(ymlFile);
    }

    @Test(dataProvider = "testcaselist")
    public void test(TestCaseDTO testCaseDTO)
            throws AuthenticationTestException,
            AdminTestException,
            SecurityXSSException {

        testCaseName = testCaseDTO.getTestCaseName();

        testCaseName =
                InjiVerifyUtil.isTestCaseValidForExecution(testCaseDTO);

        if (HealthChecker.signalTerminateExecution) {
            throw new SkipException(
                    GlobalConstants.TARGET_ENV_HEALTH_CHECK_FAILED
                            + HealthChecker.healthCheckFailureMapS);
        }

        if (testCaseName.contains("DC_23")) {
            waitForRequestExpiry();
        }

        String body =
                getJsonFromTemplate(
                        testCaseDTO.getInput(),
                        testCaseDTO.getInputTemplate());

        body =
                inputJsonKeyWordHandeler(
                        body,
                        testCaseName);

        body =
                inputJsonModuleKeyWordHandler(
                        body,
                        testCaseName);
        if (testCaseDTO.getEndPoint().contains("/dc-api")) {
            body = convertStateToRequestId(body);
        }
        Map<String, String> headers = getHeaders();

        if (testCaseName.contains("DC_24")) {
            Response firstResponse =
                    submitRequest(
                            body,
                            testCaseDTO,
                            headers);

            Assert.assertEquals(
                    firstResponse.getStatusCode(),
                    200,
                    "The first DC_24 submission must succeed");
        }

        response =
                submitRequest(
                        body,
                        testCaseDTO,
                        headers);

        validateResponse(response);
    }

    private String convertStateToRequestId(String body) {
        JSONObject json = new JSONObject(body);

        if (json.has("state")) {
            Object state = json.remove("state");
            json.put("requestId", state);
        }

        json.remove("presentation_submission");

        return json.toString();
    }

    private Map<String, String> getHeaders() {
        Map<String, String> headers = new HashMap<>();

        if (testCaseName.contains("DC_15")) {
            headers.put(
                    "Referer",
                    "https://verify.example.com/verify");
        } else if (testCaseName.contains("DC_25")
        		|| testCaseName.contains("DC_28")
                || testCaseName.contains("DC_29")) {
            // No Origin or Referer header.
        } else if (testCaseName.contains("DC_26")) {
            headers.put(
                    "Origin",
                    "https://evil.example.com");
        } else {
            headers.put(
                    "Origin",
                    "https://verify.example.com");
        }

        return headers;
    }

    private Response submitRequest(
            String body,
            TestCaseDTO testCaseDTO,
            Map<String, String> headers) {

        io.restassured.specification.RequestSpecification request =
                RestAssured.given()
                        .relaxedHTTPSValidation()
                        .headers(headers);

        if (testCaseName.contains("DC_27")|| testCaseName.contains("DC_29")) {
            return request
                    .contentType("application/x-www-form-urlencoded")
                    .body(body)
                    .post(
                            injiVerifyBaseUrl
                                    + testCaseDTO.getEndPoint());
        }

        return request
                .contentType("application/json")
                .accept("application/json")
                .body(body)
                .post(
                        injiVerifyBaseUrl
                                + testCaseDTO.getEndPoint());
    }

    private void validateResponse(Response response)
            throws AdminTestException {

        int status = response.getStatusCode();

        if (testCaseName.contains("DC_13")
                || testCaseName.contains("DC_14")
                || testCaseName.contains("DC_15")) {

            Assert.assertEquals(status, 200);

            Assert.assertTrue(
                    response.asString()
                            .trim()
                            .isEmpty(),
                    "Successful submission must have an empty body");

            return;
        }

        if (testCaseName.contains("DC_27")) {
            Assert.assertEquals(status, 415);
            return;
        }

        if (testCaseName.contains("DC_16")
                || testCaseName.contains("DC_17")
                || testCaseName.contains("DC_23")
                || testCaseName.contains("DC_28")
                || testCaseName.contains("DC_29")) {

            Assert.assertEquals(
                    status,
                    400);

            return;
        }

        String expectedMessage =
                expectedErrorMessage();

        if (expectedMessage != null) {
            Assert.assertEquals(status, 400);

            JSONObject error =
                    new JSONObject(response.asString());

            Assert.assertEquals(
                    error.optString("errorCode"),
                    "invalid_request");

            Assert.assertEquals(
                    error.optString("errorMessage"),
                    expectedMessage);

            return;
        }

        Assert.assertEquals(status, 400);
    }

    private String expectedErrorMessage() {

        if (testCaseName.contains("DC_16")
                || testCaseName.contains("DC_17")) {
            return "requestId is required and cannot be empty.";
        }

        if (testCaseName.contains("DC_18")) {
            return "Either vp_token or error must be provided.";
        }

        if (testCaseName.contains("DC_19")) {
            return "Both vp_token and error cannot be provided together.";
        }

        if (testCaseName.contains("DC_20")) {
            return "If error_description is provided, vp_token must be null.";
        }

        if (testCaseName.contains("DC_21")) {
            return "If error_description is provided, error must also be provided.";
        }

        if (testCaseName.contains("DC_22")) {
            return "Invalid state parameter: No matching VP request found.";
        }

        if (testCaseName.contains("DC_23")) {
            return "Invalid state parameter: VP request has expired.";
        }

        if (testCaseName.contains("DC_24")) {
            return "VP response has already been submitted.";
        }

        if (testCaseName.contains("DC_25")) {
            return "Unable to determine verifier origin from Origin or Referer headers.";
        }

        if (testCaseName.contains("DC_26")) {
            return "Submission Origin is not in the authorization request expected_origins.";
        }

        return null;
    }

    private void waitForRequestExpiry() {
        try {
            int expirySeconds =
                    Integer.parseInt(
                            InjiVerifyConfigManager.getproperty(
                                    InjiVerifyConstants.EXPIRATION_TIME));

            TimeUnit.SECONDS.sleep(
                    expirySeconds + 2L);

        } catch (Exception e) {
            throw new RuntimeException(
                    "Unable to wait for VP request expiry",
                    e);
        }
    }

    @AfterMethod(alwaysRun = true)
    public void setResultTestName(ITestResult result) {
        result.setAttribute(
                "TestCaseName",
                testCaseName);
    }
}