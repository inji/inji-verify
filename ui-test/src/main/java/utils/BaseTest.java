package utils;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import api.InjiVerifyConfigManager;
import io.cucumber.java.*;
import org.openqa.selenium.JavascriptExecutor;
import org.openqa.selenium.OutputType;
import org.openqa.selenium.TakesScreenshot;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.chrome.ChromeDriver;
import org.openqa.selenium.chrome.ChromeOptions;
import org.openqa.selenium.remote.DesiredCapabilities;
import org.openqa.selenium.remote.LocalFileDetector;
import org.openqa.selenium.remote.RemoteWebDriver;
import com.aventstack.extentreports.ExtentReports;
import com.aventstack.extentreports.ExtentTest;
import com.aventstack.extentreports.cucumber.adapter.ExtentCucumberAdapter;
import com.browserstack.local.Local;

import io.cucumber.plugin.event.PickleStepTestStep;
import io.cucumber.plugin.event.TestStep;
import io.mosip.testrig.apirig.utils.ConfigManager;
import io.mosip.testrig.apirig.utils.S3Adapter;

import com.aventstack.extentreports.Status;
import java.lang.reflect.Field;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.URL;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.io.*;
import java.nio.file.Files;
import java.nio.charset.StandardCharsets;
import java.util.Properties;
import java.util.Base64;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.Semaphore;

public class BaseTest extends BaseTestUtil{
	private static final Logger logger = LoggerFactory.getLogger(BaseTest.class);
	private static final ObjectMapper OBJECT_MAPPER = new ObjectMapper();

	public void setDriver(WebDriver driver) {
		driverHolder.set(driver);
	}

	public static final AtomicInteger passedCount = new AtomicInteger(0);
	public static final AtomicInteger failedCount = new AtomicInteger(0);
	public static final AtomicInteger totalCount = new AtomicInteger(0);

	// ── Known Issues ──────────────────────────────────────────────────────────────
	public static final AtomicInteger knownIssueCount = new AtomicInteger(0);
	private static final ThreadLocal<Boolean> isKnownIssueScenario = new ThreadLocal<>();
	// ─────────────────────────────────────────────────────────────────────────────

	private static final ThreadLocal<WebDriver> driverHolder = new ThreadLocal<>();
	private static final ThreadLocal<Boolean> browserStackSession = ThreadLocal.withInitial(() -> Boolean.TRUE);
	private static final ThreadLocal<String> scanQrCodeFile = new ThreadLocal<>();
	private static final ThreadLocal<String> scanCameraProfile = new ThreadLocal<>();
	private static final ThreadLocal<Boolean> autoAllowScanCamera = ThreadLocal.withInitial(() -> Boolean.TRUE);
	private static final ThreadLocal<String> scenarioRuntimeDir = new ThreadLocal<>();
	private static final ThreadLocal<Boolean> failedStepScreenshotCaptured = ThreadLocal.withInitial(() -> Boolean.FALSE);
	private static final Object insuranceArtifactsLock = new Object();
	private static volatile String downloadedInsurancePdfPath;
	private static volatile String insuranceCredentialPngPath;
	private static volatile String insuranceCredentialJpgPath;
	private static volatile String insuranceCredentialJpegPath;

	public static final String url = InjiVerifyConfigManager.getInjiVerifyUi();
	private static final String buildIdentifier = "#" + new SimpleDateFormat("dd-MMM-HH:mm").format(new Date());

	private static final ThreadLocal<JavascriptExecutor> jseHolder = new ThreadLocal<>();
	public String PdfNameForMosip = "MosipVerifiableCredential.pdf";
	public String PdfNameForInsurance = "InsuranceCredential.pdf";
	public String PdfNameForLifeInsurance = "InsuranceCredential.pdf";
	private static ExtentReports extent;
	private static ThreadLocal<ExtentTest> test = new ThreadLocal<>();

	String username = InjiVerifyConfigManager.getproperty("browserstack_username");
	String accessKey = InjiVerifyConfigManager.getproperty("browserstack_access_key");
	public final String URL = "https://" + username + ":" + accessKey + "@hub-cloud.browserstack.com/wd/hub";

	private Scenario scenario;
	// Shared BrowserStack Local tunnel — started once, reused across parallel scenarios
	private static volatile Local bsLocal = null;
	private static final Object bsLocalLock = new Object();
	// Limits concurrent BrowserStack sessions to the plan's allowed maximum (read from injiVerify.properties)
	private static final Semaphore bsSessionSlots = new Semaphore(
			Integer.parseInt(InjiVerifyConfigManager.getproperty("browserstack_max_sessions").trim()), true);

	@Before
	public void beforeAll(Scenario scenario) throws MalformedURLException {

		this.scenario = scenario;

		// ── Known Issues: skip scenario early if it is a registered known issue ──────
		if (runnerfiles.Runner.knownIssues.containsKey(scenario.getName())) {
			String bugId = runnerfiles.Runner.knownIssues.get(scenario.getName());
			ExtentReportManager.createTest(scenario.getName());
			logger.info("Skipping Known Issue Scenario: {} | Bug: {}", scenario.getName(), bugId);
			isKnownIssueScenario.set(true);
			throw new org.testng.SkipException(
					"Known Issue - Skipped: " + scenario.getName() + " | " + bugId);
		}
		isKnownIssueScenario.set(false);
		// ─────────────────────────────────────────────────────────────────────────────

		boolean browserStackEnabled = Boolean.parseBoolean(InjiVerifyConfigManager.getproperty("runOnBrowserStack").trim());
		initialiseScenarioRuntimeArtifacts(scenario);
		initialiseSharedInsuranceArtifactPaths();
		totalCount.incrementAndGet();
		ExtentReportManager.initReport();
		ExtentReportManager.createTest(scenario.getName());
		ExtentReportManager.logStep("Scenario Started: " + scenario.getName());
		boolean scanMode = scenario.getSourceTagNames().contains("@scan");
		if (scanMode) {
			scanCameraProfile.set(resolveScanCameraProfile(scenario));
			scanQrCodeFile.set(resolveScanQrCodeFile(scenario));
			autoAllowScanCamera.set(shouldAutoAllowScanCamera(scenario));
		}
		boolean useBrowserStack = browserStackEnabled
				&& !scenario.getSourceTagNames().contains("@withoutBrowserstack")
				&& !scanMode;
		boolean mobileView = scenario.getSourceTagNames().contains("@mobileView");
		browserStackSession.set(useBrowserStack);

		if (useBrowserStack) {
			synchronized (bsLocalLock) {
				try {
					if (bsLocal == null || !bsLocal.isRunning()) {
						bsLocal = new Local();
						HashMap<String, String> bsLocalArgs = new HashMap<>();
						bsLocalArgs.put("key", accessKey);
						bsLocalArgs.put("forceLocal", "true");
						bsLocal.start(bsLocalArgs);
						logger.info("BrowserStack Local tunnel started.");
					}
				} catch (Exception e) {
					logger.error("Failed to start BrowserStack Local tunnel", e);
				}
			}
		}

		if (useBrowserStack) {
			try {
				bsSessionSlots.acquire();
			} catch (InterruptedException e) {
				Thread.currentThread().interrupt();
				throw new RuntimeException("Interrupted waiting for a BrowserStack session slot", e);
			}
			DesiredCapabilities capabilities = new DesiredCapabilities();
			HashMap<String, Object> browserstackOptions = new HashMap<>();

			if (mobileView) {
				logger.info("Launching test in MOBILE VIEW (Desktop Emulation) via BrowserStack");

				capabilities.setCapability("goog:chromeOptions", getMobileChromeOptions());
			} else {
				logger.info("Launching test in DESKTOP mode via BrowserStack");
			}

			capabilities.setCapability("browserName", "Chrome");
			capabilities.setCapability("browserVersion", "latest");
			browserstackOptions.put("os", "Windows");
			browserstackOptions.put("osVersion", "10");
			browserstackOptions.put("local", "true");
			browserstackOptions.put("debug", "true");

			browserstackOptions.put("projectName", "InjiVerify UI Suite");
			browserstackOptions.put("buildName", "InjiVerify - " + getEnvName() + " " + buildIdentifier);
			browserstackOptions.put("sessionName", scenario.getName());

			capabilities.setCapability("bstack:options", browserstackOptions);
			logger.info("Final capabilities: {}", capabilities);
			WebDriver newDriver = new RemoteWebDriver(new URL(URL), capabilities);
			((RemoteWebDriver) newDriver).setFileDetector(new LocalFileDetector());
			driverHolder.set(newDriver);
		} else {
			ChromeOptions chromeOptions = getLocalChromeOptions(scanMode, mobileView);
			driverHolder.set(new ChromeDriver(chromeOptions));
		}

		jseHolder.set((JavascriptExecutor) driverHolder.get());

		if (!mobileView) {
			driverHolder.get().manage().window().maximize();
		}

		driverHolder.get().get(url);
	}

	private void initialiseScenarioRuntimeArtifacts(Scenario scenario) {
		try {
			String safeScenarioName = scenario.getName().replaceAll("[^a-zA-Z0-9._-]", "_");
			String uniqueId = safeScenarioName + "-" + System.currentTimeMillis() + "-" + Thread.currentThread().getId();
			File runtimeDir = new File(System.getProperty("user.dir") + File.separator + "test-output" + File.separator
					+ "runtime-media" + File.separator + uniqueId);
			Files.createDirectories(runtimeDir.toPath());
			scenarioRuntimeDir.set(runtimeDir.getAbsolutePath());
		} catch (IOException e) {
			throw new RuntimeException("Unable to create scenario runtime directory.", e);
		}
	}

	private void initialiseSharedInsuranceArtifactPaths() {
		if (downloadedInsurancePdfPath != null && insuranceCredentialPngPath != null && insuranceCredentialJpgPath != null
				&& insuranceCredentialJpegPath != null) {
			return;
		}

		synchronized (insuranceArtifactsLock) {
			if (downloadedInsurancePdfPath != null && insuranceCredentialPngPath != null && insuranceCredentialJpgPath != null
					&& insuranceCredentialJpegPath != null) {
				return;
			}

			try {
				File sharedDir = new File(System.getProperty("user.dir") + File.separator + "test-output" + File.separator
						+ "runtime-media" + File.separator + "shared-insurance-artifacts");
				Files.createDirectories(sharedDir.toPath());
				downloadedInsurancePdfPath = new File(sharedDir, "InsuranceCredential.pdf").getAbsolutePath();
				insuranceCredentialPngPath = new File(sharedDir, "InsuranceCredential0.png").getAbsolutePath();
				insuranceCredentialJpgPath = new File(sharedDir, "InsuranceCredential0.jpg").getAbsolutePath();
				insuranceCredentialJpegPath = new File(sharedDir, "InsuranceCredential0.jpeg").getAbsolutePath();
			} catch (IOException e) {
				throw new RuntimeException("Unable to create shared insurance artifact directory.", e);
			}
		}
	}

	@BeforeStep
	public void beforeStep(Scenario scenario) {
		String stepName = getStepName(scenario);
		ExtentCucumberAdapter.getCurrentStep().log(Status.INFO, "Step Started: " + stepName);
	}

	@AfterStep
	public void afterStep(Scenario scenario) {
		String stepName = getStepName(scenario);

		if (scenario.isFailed()) {
			ExtentCucumberAdapter.getCurrentStep().log(Status.FAIL, "Step Failed: " + stepName);
			captureScreenshot();
			failedStepScreenshotCaptured.set(Boolean.TRUE);
		} else {
			ExtentCucumberAdapter.getCurrentStep().log(Status.PASS, "Step Passed: " + stepName);
		}
	}

	@After
	public void afterScenario(Scenario scenario) {

		// ── Known Issues: handle skipped-due-to-known-issue scenarios ────────────────
		if (scenario.getStatus().toString().equalsIgnoreCase("SKIPPED")
				&& runnerfiles.Runner.knownIssues.containsKey(scenario.getName())) {

			String bugId  = runnerfiles.Runner.knownIssues.get(scenario.getName());
			String bugUrl = "https://mosip.atlassian.net/browse/" + bugId;
			knownIssueCount.incrementAndGet();

			ExtentReportManager.getTest().skip(
					"🟠 Skipped due to Known Issue → <a href='" + bugUrl + "' target='_blank'>" + bugId + "</a>");

			ExtentReportManager.flushReport();
			return; // Skip the rest of the teardown — driver was never started
		}
		// ─────────────────────────────────────────────────────────────────────────────

		if (scenario.isFailed()) {
			failedCount.incrementAndGet();
			attachLocalFailureScreenshotIfNeeded();
			ExtentReportManager.getTest().fail("❌ Scenario Failed: " + scenario.getName());
		} else {
			passedCount.incrementAndGet();
			ExtentReportManager.getTest().pass("✅ Scenario Passed: " + scenario.getName());
		}

		markBrowserStackSessionStatus(scenario);
		attachBrowserStackVideoLinkIfFailed(scenario);
		ExtentReportManager.flushReport();
		try {
			int done = passedCount.get() + failedCount.get();
			if (isUsingBrowserStack() && bsLocal != null && bsLocal.isRunning() && done == totalCount.get()) {
				synchronized (bsLocalLock) {
					done = passedCount.get() + failedCount.get();
					if (bsLocal != null && bsLocal.isRunning() && done == totalCount.get()) {
						try {
							bsLocal.stop();
							logger.info("BrowserStack Local tunnel stopped.");
						} catch (Exception e) {
							logger.error("Failed to stop BrowserStack Local tunnel", e);
						}
					}
				}
			}
		} catch (Exception e) {
			logger.error("Error in afterScenario", e);
		} finally {
			WebDriver currentDriver = driverHolder.get();
			if (currentDriver != null) {
				currentDriver.quit();
			}
			if (isUsingBrowserStack()) {
				bsSessionSlots.release();
			}
			driverHolder.remove();
			jseHolder.remove();
			browserStackSession.remove();
			scanQrCodeFile.remove();
			scanCameraProfile.remove();
			autoAllowScanCamera.remove();
			scenarioRuntimeDir.remove();
			failedStepScreenshotCaptured.remove();
		}
	}

	private void markBrowserStackSessionStatus(Scenario scenario) {
		if (!isUsingBrowserStack() || driverHolder.get() == null) {
			return;
		}

		try {
			String status = scenario.isFailed() ? "failed" : "passed";
			String reason = scenario.isFailed()
					? "Scenario failed: " + scenario.getName()
					: "Scenario passed: " + scenario.getName();
			String executorCommand = String.format(
					"browserstack_executor: {\"action\": \"setSessionStatus\", \"arguments\": {\"status\":\"%s\", \"reason\": \"%s\"}}",
					status,
					escapeForJson(reason)
			);
			((JavascriptExecutor) driverHolder.get()).executeScript(executorCommand);
			logger.info("BrowserStack session marked as {} for scenario: {}", status, scenario.getName());
		} catch (Exception e) {
			logger.error("Unable to update BrowserStack session status for scenario: {}", scenario.getName(), e);
		}
	}

	private void attachBrowserStackVideoLinkIfFailed(Scenario scenario) {
		if (!scenario.isFailed() || !isUsingBrowserStack() || driverHolder.get() == null || ExtentReportManager.getTest() == null) {
			return;
		}

		try {
			String sessionDetailsJson = (String) ((JavascriptExecutor) driverHolder.get())
					.executeScript("browserstack_executor: {\"action\": \"getSessionDetails\"}");
			if (sessionDetailsJson == null || sessionDetailsJson.trim().isEmpty()) {
				return;
			}

			JsonNode sessionDetails = OBJECT_MAPPER.readTree(sessionDetailsJson);
			String link = firstNonBlank(
					getJsonText(sessionDetails, "video_url"),
					getJsonText(sessionDetails, "videoUrl"),
					getJsonText(sessionDetails, "public_url"),
					getJsonText(sessionDetails, "publicUrl"),
					getJsonText(sessionDetails, "dashboard_url"),
					getJsonText(sessionDetails, "dashboardUrl"),
					getJsonText(sessionDetails, "session_url"),
					getJsonText(sessionDetails, "sessionUrl"));

			if (link != null) {
				ExtentReportManager.getTest().fail(
						"<a href='" + escapeHtmlAttribute(link) + "' target='_blank'>Click here for BrowserStack video/session</a>");
				logger.info("Attached BrowserStack video/session link for failed scenario: {}", scenario.getName());
				return;
			}

			String hashedId = firstNonBlank(getJsonText(sessionDetails, "hashed_id"), getJsonText(sessionDetails, "hashedId"));
			if (hashedId != null) {
				ExtentReportManager.getTest().fail("BrowserStack session id: " + hashedId);
			}
		} catch (Exception e) {
			logger.warn("Unable to fetch BrowserStack session details for scenario: {}", scenario.getName(), e);
		}
	}

	private void attachLocalFailureScreenshotIfNeeded() {
		if (isUsingBrowserStack() || ExtentReportManager.getTest() == null
				|| Boolean.TRUE.equals(failedStepScreenshotCaptured.get())) {
			return;
		}

		WebDriver currentDriver = driverHolder.get();
		if (currentDriver == null) {
			return;
		}

		ScreenshotUtil.attachScreenshot(currentDriver, "LocalFailureScreenshot");
		failedStepScreenshotCaptured.set(Boolean.TRUE);
	}

	private String getJsonText(JsonNode node, String fieldName) {
		JsonNode value = node.get(fieldName);
		if (value == null || value.isNull()) {
			return null;
		}
		String text = value.asText();
		return text == null || text.trim().isEmpty() ? null : text;
	}

	private String firstNonBlank(String... values) {
		for (String value : values) {
			if (value != null && !value.trim().isEmpty()) {
				return value;
			}
		}
		return null;
	}

	private String escapeHtmlAttribute(String value) {
		return value.replace("&", "&amp;").replace("'", "&#39;").replace("\"", "&quot;");
	}

	private String escapeForJson(String value) {
		return value
				.replace("\\", "\\\\")
				.replace("\"", "\\\"");
	}

	private String getStepName(Scenario scenario) {
		try {
			Field testCaseField = scenario.getClass().getDeclaredField("testCase");
			testCaseField.setAccessible(true);
			io.cucumber.plugin.event.TestCase testCase = (io.cucumber.plugin.event.TestCase) testCaseField.get(scenario);
			List<TestStep> testSteps = testCase.getTestSteps();

			for (TestStep step : testSteps) {
				if (step instanceof PickleStepTestStep) {
					return ((PickleStepTestStep) step).getStep().getText();
				}
			}
		} catch (Exception e) {
			return "Unknown Step";
		}
		return "Unknown Step";
	}

	private void captureScreenshot() {
		WebDriver currentDriver = driverHolder.get();
		if (currentDriver != null) {
			byte[] screenshot = ((TakesScreenshot) currentDriver).getScreenshotAs(OutputType.BYTES);
			ExtentCucumberAdapter.getCurrentStep().addScreenCaptureFromBase64String(
					java.util.Base64.getEncoder().encodeToString(screenshot),
					"Failure Screenshot"
			);
			failedStepScreenshotCaptured.set(Boolean.TRUE);
		}
	}

	@AfterAll
	public static void afterAll() {
		Runtime.getRuntime().addShutdownHook(new Thread(() -> {
			logger.info("Shutdown hook triggered. Uploading report...");
			if (extent != null) {
				extent.flush();
			}
			pushReportsToS3();
		}));

	}

	public WebDriver getDriver() {
		return driverHolder.get();
	}

	public JavascriptExecutor getJse() {
		return jseHolder.get();
	}

	public static boolean isUsingBrowserStack() {
		return Boolean.TRUE.equals(browserStackSession.get());
	}

	public static String getSelectedScanQrCodeFile() {
		return scanQrCodeFile.get();
	}

	public static String getSelectedScanCameraProfile() {
		return scanCameraProfile.get();
	}

	public static boolean shouldAutoAllowScanCamera() {
		return Boolean.TRUE.equals(autoAllowScanCamera.get());
	}

	public static String getScenarioRuntimeDir() {
		return scenarioRuntimeDir.get();
	}

	public static void markFailureScreenshotCaptured() {
		failedStepScreenshotCaptured.set(Boolean.TRUE);
	}

	public static String getDownloadedInsurancePdfPath() {
		return downloadedInsurancePdfPath;
	}

	public static String getInsuranceCredentialPngPath() {
		return insuranceCredentialPngPath;
	}

	public static String getInsuranceCredentialJpgPath() {
		return insuranceCredentialJpgPath;
	}

	public static String getInsuranceCredentialJpegPath() {
		return insuranceCredentialJpegPath;
	}

	public static Object getInsuranceArtifactsLock() {
		return insuranceArtifactsLock;
	}

	public static void updateBrowserStackNetworkProfile(String networkProfile) {
		WebDriver currentDriver = driverHolder.get();
		if (!Boolean.TRUE.equals(browserStackSession.get()) || !(currentDriver instanceof RemoteWebDriver)) {
			throw new IllegalStateException("BrowserStack network profile can be updated only for active RemoteWebDriver sessions.");
		}

		String sessionId = ((RemoteWebDriver) currentDriver).getSessionId().toString();
		String userName = InjiVerifyConfigManager.getproperty("browserstack_username");
		String accessKey = InjiVerifyConfigManager.getproperty("browserstack_access_key");
		String endpoint = "https://api.browserstack.com/automate/sessions/" + sessionId + "/update_network.json";
		String payload = "{\"networkProfile\":\"" + networkProfile + "\"}";

		HttpURLConnection connection = null;
		try {
			connection = (HttpURLConnection) new URL(endpoint).openConnection();
			connection.setRequestMethod("PUT");
			connection.setRequestProperty("Authorization", "Basic " + Base64.getEncoder()
					.encodeToString((userName + ":" + accessKey).getBytes(StandardCharsets.UTF_8)));
			connection.setRequestProperty("Content-Type", "application/json");
			connection.setDoOutput(true);
			try (OutputStream outputStream = connection.getOutputStream()) {
				outputStream.write(payload.getBytes(StandardCharsets.UTF_8));
			}

			int responseCode = connection.getResponseCode();
			if (responseCode < 200 || responseCode >= 300) {
				String errorBody = readConnectionBody(connection);
				throw new RuntimeException("BrowserStack network profile update failed with status " + responseCode
						+ (errorBody == null || errorBody.isEmpty() ? "" : (": " + errorBody)));
			}
		} catch (IOException e) {
			throw new RuntimeException("Failed to update BrowserStack network profile to '" + networkProfile + "'.", e);
		} finally {
			if (connection != null) {
				connection.disconnect();
			}
		}
	}

	private static String readConnectionBody(HttpURLConnection connection) {
		InputStream stream = null;
		try {
			stream = connection.getErrorStream() != null ? connection.getErrorStream() : connection.getInputStream();
			if (stream == null) {
				return null;
			}
			BufferedReader reader = new BufferedReader(new InputStreamReader(stream, StandardCharsets.UTF_8));
			StringBuilder builder = new StringBuilder();
			String line;
			while ((line = reader.readLine()) != null) {
				builder.append(line);
			}
			return builder.toString();
		} catch (IOException e) {
			return null;
		} finally {
			if (stream != null) {
				try {
					stream.close();
				} catch (IOException ignored) {
				}
			}
		}
	}

	public static void pushReportsToS3() {

		String timestamp = new SimpleDateFormat("yyyy-MM-dd-HH-mm").format(new Date());
		String name = InjiVerifyConfigManager.getInjiVerifyUiBaseUrl() + "-" + timestamp + "-T-" + totalCount.get() + "-P-" + passedCount.get() + "-F-" + failedCount.get() + ".html";
		String newFileName = "InjiVerifyUi-" + name;
		File originalReportFile = new File(System.getProperty("user.dir") + "/test-output/ExtentReport.html");
		File newReportFile = new File(System.getProperty("user.dir") + "/test-output/" + newFileName);

		if (originalReportFile.renameTo(newReportFile)) {
			logger.info("Report renamed to: {}", newFileName);
		} else {
			logger.error("Failed to rename the report file");
		}

		if (ConfigManager.getPushReportsToS3().equalsIgnoreCase("yes")) {
			S3Adapter s3Adapter = new S3Adapter();
			boolean isStoreSuccess = false;
			try {
				isStoreSuccess = s3Adapter.putObject(
						ConfigManager.getS3Account(),
						"",
						null, null,
						newFileName,
						newReportFile
				);
				logger.info("isStoreSuccess:: {}", isStoreSuccess);
			} catch (Exception e) {
				logger.error("Error occurred while pushing the object: {}", e.getLocalizedMessage());
				logger.error("Error details: {}", e.getMessage());
			}
		}
	}

	public static String[] fetchIssuerTexts() {
		String issuerSearchText = null;
		String issuerSearchTextforSunbird = null;
		String propertyFilePath = System.getProperty("user.dir") + "/src/test/resources/config.properties";
		Properties properties = new Properties();

		try (FileInputStream fileInputStream = new FileInputStream(propertyFilePath)) {
			properties.load(fileInputStream);
			issuerSearchText = properties.getProperty("issuerSearchText");
			issuerSearchTextforSunbird = properties.getProperty("issuerSearchTextforSunbird");
		} catch (IOException e) {
			e.printStackTrace();
		}
		return new String[] { issuerSearchText, issuerSearchTextforSunbird };
	}

	private String resolveScanCameraProfile(Scenario scenario) {
		if (scenario.getSourceTagNames().contains("@camera_2mp")) {
			return "2mp";
		}
		if (scenario.getSourceTagNames().contains("@camera_8mp")) {
			return "8mp";
		}
		if (scenario.getSourceTagNames().contains("@camera_15mp")) {
			return "15mp";
		}
		if (scenario.getSourceTagNames().contains("@camera_low_light")) {
			return "low_light";
		}
		return "default";
	}

	private boolean shouldAutoAllowScanCamera(Scenario scenario) {
		return !scenario.getSourceTagNames().contains("@camera_denied")
				&& !scenario.getSourceTagNames().contains("@verifyFirstTimeScanQrCodePermissionPrompt")
				&& !scenario.getSourceTagNames().contains("@verifyScanQrCodeWithAllowedCameraAccess");
	}

}
