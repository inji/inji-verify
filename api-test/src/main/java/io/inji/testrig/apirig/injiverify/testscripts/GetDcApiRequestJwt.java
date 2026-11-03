package io.inji.testrig.apirig.injiverify.testscripts;

import java.util.List;


import org.apache.log4j.Level;
import org.apache.log4j.Logger;
import org.json.JSONArray;
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
import java.util.regex.Matcher;

import com.auth0.jwt.JWT;
import com.auth0.jwt.interfaces.DecodedJWT;

import io.inji.testrig.apirig.injiverify.utils.InjiVerifyConfigManager;
import io.inji.testrig.apirig.injiverify.utils.InjiVerifyUtil;
import io.mosip.testrig.apirig.dto.TestCaseDTO;
import io.mosip.testrig.apirig.testrunner.HealthChecker;
import io.mosip.testrig.apirig.utils.AdminTestException;
import io.mosip.testrig.apirig.utils.AuthenticationTestException;
import io.mosip.testrig.apirig.utils.GlobalConstants;
import io.mosip.testrig.apirig.utils.SecurityXSSException;
import io.restassured.RestAssured;
import io.restassured.response.Response;

public class GetDcApiRequestJwt extends InjiVerifyUtil implements ITest {

    private static final Logger logger =
            Logger.getLogger(GetDcApiRequestJwt.class);

    protected String testCaseName = "";

    public Response response;

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

        logger.info("Started executing yml: " + ymlFile);

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

        String inputJson =
                getJsonFromTemplate(
                        testCaseDTO.getInput(),
                        testCaseDTO.getInputTemplate());

        inputJson =
                inputJsonKeyWordHandeler(
                        inputJson,
                        testCaseName);

        inputJson =
                inputJsonModuleKeyWordHandler(
                        inputJson,
                        testCaseName);

        String requestId =
                new JSONObject(inputJson).getString("requestId");

        if (requestId.startsWith("$ID:")) {
            throw new SkipException(
                    "Request ID placeholder was not resolved: " + requestId);
        }

        String endpoint =
                testCaseDTO.getEndPoint()
                        .replace("{requestId}", requestId);

        String url = injiVerifyBaseUrl + endpoint;

     

        response = RestAssured
                .given()
                .relaxedHTTPSValidation()
                .header("Accept", "*/*")
                .when()
                .get(url);

        if (testCaseName.contains("DC_10")) {
            validateDecentralizedIdentifierJwt(response);
        }

        if (testCaseName.contains("DC_11")) {
            validateX509SanDnsJwt(response);
        }

        if (testCaseName.contains("DC_12")) {
            Assert.assertEquals(response.getStatusCode(), 404);
        }
    }

    private void validateDecentralizedIdentifierJwt(Response response)
            throws AdminTestException {

        Assert.assertEquals(response.getStatusCode(), 200);

        String contentType = response.getContentType();

        Assert.assertTrue(
                contentType.contains("oauth-authz-req+jwt"),
                "Response must use application/oauth-authz-req+jwt");

        String jwtValue = response.asString();

        Assert.assertEquals(
                jwtValue.split("\\.").length,
                3,
                "Response must be a signed JWT");

        DecodedJWT decodedJwt = JWT.decode(jwtValue);

        JSONObject header =
                new JSONObject(
                        InjiVerifyUtil.decodeBase64Url(
                                decodedJwt.getHeader()));

        JSONObject claims =
                new JSONObject(
                        InjiVerifyUtil.decodeBase64Url(
                                decodedJwt.getPayload()));

        Assert.assertNotEquals(
                header.optString("alg"),
                "none",
                "JWT must be signed");

        Assert.assertTrue(
                claims.has("response_mode"));

        Assert.assertEquals(
                claims.getString("response_mode"),
                "dc_api");

        Assert.assertTrue(
                claims.has("expected_origins"));

        JSONArray expectedOrigins =
                claims.getJSONArray("expected_origins");

        Assert.assertTrue(
                expectedOrigins.toList()
                        .contains("https://verify.example.com"));

        Assert.assertTrue(
                claims.has("nonce"));

        Assert.assertTrue(
                claims.has("dcql_query"));

        Assert.assertTrue(
                claims.get("dcql_query") instanceof JSONObject);

        Assert.assertTrue(
                claims.has("client_metadata"));

        Assert.assertTrue(
                claims.get("client_metadata") instanceof JSONObject);

        Assert.assertFalse(
                claims.has("state"));

        Assert.assertFalse(
                claims.has("response_uri"));
    }

    private void validateX509SanDnsJwt(Response response)
            throws AdminTestException {

        Assert.assertEquals(response.getStatusCode(), 200);

        String contentType = response.getContentType();

        Assert.assertTrue(
                contentType.contains("oauth-authz-req+jwt"),
                "Response must use application/oauth-authz-req+jwt");

        String jwtValue = response.asString();

        Assert.assertEquals(
                jwtValue.split("\\.").length,
                3,
                "Response must be a signed JWT");

        DecodedJWT decodedJwt = JWT.decode(jwtValue);

        JSONObject header =
                new JSONObject(
                        InjiVerifyUtil.decodeBase64Url(
                                decodedJwt.getHeader()));

        JSONObject claims =
                new JSONObject(
                        InjiVerifyUtil.decodeBase64Url(
                                decodedJwt.getPayload()));

        Assert.assertNotEquals(
                header.optString("alg"),
                "none",
                "JWT must be signed");

        Assert.assertTrue(
                header.has("x5c"),
                "x5c must be present for x509_san_dns");

        Assert.assertTrue(
                header.get("x5c") instanceof JSONArray);

        Assert.assertTrue(
                header.getJSONArray("x5c").length() > 0);

        Assert.assertFalse(
                header.has("kid"),
                "kid must not be present for x509_san_dns");

        Assert.assertEquals(
                claims.getString("response_mode"),
                "dc_api");

        Assert.assertTrue(
                claims.has("expected_origins"));

        Assert.assertTrue(
                claims.getJSONArray("expected_origins")
                        .toList()
                        .contains("https://verify.example.com"));

        Assert.assertTrue(
                claims.has("nonce"));

        Assert.assertTrue(
                claims.has("dcql_query"));

        Assert.assertTrue(
                claims.has("client_metadata"));

        Assert.assertFalse(
                claims.has("state"));

        Assert.assertFalse(
                claims.has("response_uri"));
    }

    @AfterMethod(alwaysRun = true)
    public void setResultTestName(ITestResult result) {
        result.setAttribute(
                "TestCaseName",
                testCaseName);
    }
}