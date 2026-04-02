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
import java.net.MalformedURLException;
import java.net.URL;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.io.*;
import java.nio.file.Files;
import java.util.Properties;

public class BaseTest extends BaseTestUtil{
	private static final Logger logger = LoggerFactory.getLogger(BaseTest.class);

	public void setDriver(WebDriver driver) {
		BaseTest.driver = driver;
	}

	public static int passedCount = 0;
	public static int failedCount = 0;
	public static int totalCount = 0;

	// ── Known Issues ──────────────────────────────────────────────────────────────
	public static int knownIssueCount = 0;
	private static final ThreadLocal<Boolean> isKnownIssueScenario = new ThreadLocal<>();
	// ─────────────────────────────────────────────────────────────────────────────

	public static WebDriver driver;
	private static final ThreadLocal<Boolean> browserStackSession = ThreadLocal.withInitial(() -> Boolean.TRUE);
	private static final ThreadLocal<String> scanQrCodeFile = new ThreadLocal<>();
	private static final ThreadLocal<String> scanCameraProfile = new ThreadLocal<>();
	private static final ThreadLocal<Boolean> autoAllowScanCamera = ThreadLocal.withInitial(() -> Boolean.TRUE);
	private static final ThreadLocal<String> scenarioRuntimeDir = new ThreadLocal<>();
	private static final Object insuranceArtifactsLock = new Object();
	private static volatile String downloadedInsurancePdfPath;
	private static volatile String insuranceCredentialPngPath;
	private static volatile String insuranceCredentialJpgPath;
	private static volatile String insuranceCredentialJpegPath;

	public static final String url = InjiVerifyConfigManager.getInjiVerifyUi();
	private static final String buildIdentifier = "#" + new SimpleDateFormat("dd-MMM-HH:mm").format(new Date());

	public static JavascriptExecutor jse;
	public String PdfNameForMosip = "MosipVerifiableCredential.pdf";
	public String PdfNameForInsurance = "InsuranceCredential.pdf";
	public String PdfNameForLifeInsurance = "InsuranceCredential.pdf";
	private static ExtentReports extent;
	private static ThreadLocal<ExtentTest> test = new ThreadLocal<>();

	String username = InjiVerifyConfigManager.getproperty("browserstack_username");
	String accessKey = InjiVerifyConfigManager.getproperty("browserstack_access_key");
	public final String URL = "https://" + username + ":" + accessKey + "@hub-cloud.browserstack.com/wd/hub";

	private Scenario scenario;
	Local bsLocal = null;

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
		boolean scanMode = scenario.getSourceTagNames().contains("@scan");
		if (scanMode) {
			scanQrCodeFile.set(resolveScanQrCodeFile(scenario));
			scanCameraProfile.set(resolveScanCameraProfile(scenario));
			autoAllowScanCamera.set(shouldAutoAllowScanCamera(scenario));
		}
		boolean useBrowserStack = browserStackEnabled
				&& !scenario.getSourceTagNames().contains("@withoutBrowserstack")
				&& !scanMode;
		boolean mobileView = scenario.getSourceTagNames().contains("@mobileView");
		browserStackSession.set(useBrowserStack);

		if (useBrowserStack) {
			try {
				if (bsLocal == null || !bsLocal.isRunning()) {
					bsLocal = new Local();
					HashMap<String, String> bsLocalArgs = new HashMap<>();
					bsLocalArgs.put("key", accessKey);
					bsLocalArgs.put("forceLocal", "true");
					try {
						bsLocal.start(bsLocalArgs);
						logger.info("BrowserStack Local tunnel started.");
					} catch (Exception e) {
						logger.error("Failed to start BrowserStack Local tunnel", e);
					}
				}
			} catch (Exception e) {
				logger.error("Failed to initialize BrowserStack Local", e);
			}
		}

		totalCount++;
		ExtentReportManager.initReport();
		ExtentReportManager.createTest(scenario.getName());
		ExtentReportManager.logStep("Scenario Started: " + scenario.getName());

		if (useBrowserStack) {
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
			driver = new RemoteWebDriver(new URL(URL), capabilities);
			((RemoteWebDriver) driver).setFileDetector(new LocalFileDetector());
		} else {
			ChromeOptions chromeOptions = getLocalChromeOptions(scanMode, mobileView);
			driver = new ChromeDriver(chromeOptions);
		}

		jse = (JavascriptExecutor) driver;

		if (!mobileView) {
			driver.manage().window().maximize();
		}

		driver.get(url);
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
			knownIssueCount++;

			ExtentReportManager.getTest().skip(
					"🟠 Skipped due to Known Issue → <a href='" + bugUrl + "' target='_blank'>" + bugId + "</a>");

			ExtentReportManager.flushReport();
			return; // Skip the rest of the teardown — driver was never started
		}
		// ─────────────────────────────────────────────────────────────────────────────

		if (scenario.isFailed()) {
			failedCount++;
			ExtentReportManager.getTest().fail("❌ Scenario Failed: " + scenario.getName());
		} else {
			passedCount++;
			ExtentReportManager.getTest().pass("✅ Scenario Passed: " + scenario.getName());
		}

		markBrowserStackSessionStatus(scenario);
		ExtentReportManager.flushReport();
		try {
			if (isUsingBrowserStack() && bsLocal != null && bsLocal.isRunning() && (passedCount + failedCount == totalCount)) {
				try {
					bsLocal.stop();
					logger.info("🛑 BrowserStack Local tunnel stopped.");
				} catch (Exception e) {
					logger.error("Failed to stop BrowserStack Local tunnel", e);
				}
			}
		} catch (Exception e) {
			logger.error("Error in afterScenario", e);
		} finally {
			if (driver != null) {
				driver.quit();
				driver = null;
			}
			jse = null;
			browserStackSession.remove();
			scanQrCodeFile.remove();
			scanCameraProfile.remove();
			autoAllowScanCamera.remove();
			scenarioRuntimeDir.remove();
		}
	}

	private void markBrowserStackSessionStatus(Scenario scenario) {
		if (!isUsingBrowserStack() || driver == null) {
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
			((JavascriptExecutor) driver).executeScript(executorCommand);
			logger.info("BrowserStack session marked as {} for scenario: {}", status, scenario.getName());
		} catch (Exception e) {
			logger.error("Unable to update BrowserStack session status for scenario: {}", scenario.getName(), e);
		}
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
		if (driver != null) {
			byte[] screenshot = ((TakesScreenshot) driver).getScreenshotAs(OutputType.BYTES);
			ExtentCucumberAdapter.getCurrentStep().addScreenCaptureFromBase64String(
					java.util.Base64.getEncoder().encodeToString(screenshot),
					"Failure Screenshot"
			);
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
		return driver;
	}

	public JavascriptExecutor getJse() {
		return jse;
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

	public static void pushReportsToS3() {

		try {
			Thread.sleep(20000);
		} catch (InterruptedException e) {
			e.printStackTrace();
		}

		String timestamp = new SimpleDateFormat("yyyy-MM-dd-HH-mm").format(new Date());
		String name = InjiVerifyConfigManager.getInjiVerifyUiBaseUrl() + "-" + timestamp + "-T-" + totalCount + "-P-" + passedCount + "-F-" + failedCount + ".html";
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
		if (scenario.getSourceTagNames().contains("@camera_lt_8mp")) {
			return "lt_8mp";
		}
		if (scenario.getSourceTagNames().contains("@camera_gt_15mp")) {
			return "gt_15mp";
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
