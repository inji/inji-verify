package stepdefinitions;

import com.aventstack.extentreports.ExtentTest;
import com.aventstack.extentreports.Status;
import io.cucumber.java.Before;
import io.cucumber.java.BeforeStep;
import io.inji.testrig.apirig.injiverify.testscripts.SimplePostForAutoGenId;
import org.apache.commons.lang3.exception.ExceptionUtils;
import org.openqa.selenium.WebDriver;
import pages.BLE;
import pages.HomePage;
import pages.ScanQRCodePage;
import pages.UploadQRCode;
import pages.VpVerification;
import utils.BaseTest;
import utils.ExtentReportManager;
import utils.ScreenshotUtil;

abstract class BaseSteps {
    protected String pageTitle;
    protected WebDriver driver;
    protected BaseTest baseTest;
    protected HomePage homePage;
    protected BLE ble;
    protected VpVerification vpverification;
    protected ScanQRCodePage scanqrcode;
    protected UploadQRCode uploadqrcode;
    protected ExtentTest test;

    protected static final String policyNumber = SimplePostForAutoGenId.policyNumber;
    protected static final String fullName = SimplePostForAutoGenId.fullName;
    protected static final String dob = SimplePostForAutoGenId.dob;
    protected static final String screenshotPath = System.getProperty("user.dir") + "/test-output/screenshots";

    protected BaseSteps() {
        // Driver is not available yet at construction time with parallel execution.
        // Initialization is deferred to initStepDependencies() below.
    }

    /**
     * Runs after BaseTest.beforeAll() which uses the Cucumber default order (10000).
     * A higher order value means this hook runs later, so the ThreadLocal driver is ready.
     * Initialises driver, page objects, and the Extent test reference for this scenario.
     */
    @Before(order = 11000)
    public void initStepDependencies() {
        ensureStepDependenciesInitialized();
    }

    @BeforeStep(order = 11000)
    public void initStepDependenciesBeforeEachStep() {
        ensureStepDependenciesInitialized(false);
    }

    protected void ensureStepDependenciesInitialized() {
        ensureStepDependenciesInitialized(true);
    }

    protected void ensureStepDependenciesInitialized(boolean throwIfDriverNull) {
        this.baseTest = new BaseTest();
        this.driver = baseTest.getDriver();
        if (driver == null) {
            if (throwIfDriverNull) {
                throw new RuntimeException("WebDriver is null in BaseSteps! Check if BaseTest.beforeAll() ran correctly.");
            }
            return;
        }
        if (this.test == null) {
            this.test = ExtentReportManager.getTest();
        }
        if (this.homePage == null) {
            this.homePage = new HomePage(driver);
        }
        if (this.ble == null) {
            this.ble = new BLE(driver);
        }
        if (this.vpverification == null) {
            this.vpverification = new VpVerification(driver);
        }
        if (this.scanqrcode == null) {
            this.scanqrcode = new ScanQRCodePage(driver);
        }
        if (this.uploadqrcode == null) {
            this.uploadqrcode = new UploadQRCode(driver);
        }
    }

    public static void logFailure(ExtentTest test, WebDriver driver, String message, Exception e) {
        ExtentTest activeTest = test != null ? test : ExtentReportManager.getTest();
        if (activeTest != null) {
            activeTest.log(Status.FAIL, message + ": " + e.getMessage());
            activeTest.log(Status.FAIL, ExceptionUtils.getStackTrace(e));
        }
        if (driver != null) {
            ScreenshotUtil.attachScreenshot(driver, "FailureScreenshot");
            BaseTest.markFailureScreenshotCaptured();
        }
    }

    public static void logAssertionFailure(ExtentTest test, WebDriver driver, String message, AssertionError error) {
        ExtentTest activeTest = test != null ? test : ExtentReportManager.getTest();
        if (activeTest != null) {
            activeTest.log(Status.FAIL, message + ": " + error.getMessage());
        }
        if (driver != null) {
            ScreenshotUtil.attachScreenshot(driver, "FailureScreenshot");
            BaseTest.markFailureScreenshotCaptured();
        }
    }

    public void logFailure(ExtentTest test, WebDriver driver, String message, Throwable throwable) {
        ExtentTest activeTest = test != null ? test : ExtentReportManager.getTest();
        if (activeTest != null) {
            activeTest.log(Status.FAIL, message);
            activeTest.log(Status.FAIL, throwable);
        }
        if (driver != null) {
            ScreenshotUtil.attachScreenshot(driver, "FailureScreenshot");
            BaseTest.markFailureScreenshotCaptured();
        }
    }
}
