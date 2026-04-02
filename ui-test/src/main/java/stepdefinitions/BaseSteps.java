package stepdefinitions;

import com.aventstack.extentreports.ExtentTest;
import com.aventstack.extentreports.Status;
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

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;

abstract class BaseSteps {
    protected String pageTitle;
    protected final WebDriver driver;
    protected final BaseTest baseTest;
    protected final HomePage homePage;
    protected final BLE ble;
    protected final VpVerification vpverification;
    protected final ScanQRCodePage scanqrcode;
    protected final UploadQRCode uploadqrcode;
    protected final ExtentTest test = ExtentReportManager.getTest();

    protected static final String policyNumber = SimplePostForAutoGenId.policyNumber;
    protected static final String fullName = SimplePostForAutoGenId.fullName;
    protected static final String dob = SimplePostForAutoGenId.dob;
    protected static final String formattedDate = dob == null ? null
            : LocalDate.parse(dob).format(DateTimeFormatter.ofPattern("MM-dd-yyyy"));
    protected static final String screenshotPath = System.getProperty("user.dir") + "/test-output/screenshots";

    protected BaseSteps() {
        this.baseTest = new BaseTest();
        this.driver = baseTest.getDriver();
        if (driver == null) {
            throw new RuntimeException("WebDriver is null in StepDef! Check if BaseTest initializes correctly.");
        }
        this.homePage = new HomePage(driver);
        this.ble = new BLE(driver);
        this.vpverification = new VpVerification(driver);
        this.scanqrcode = new ScanQRCodePage(driver);
        this.uploadqrcode = new UploadQRCode(driver);
    }

    public static void logFailure(ExtentTest test, WebDriver driver, String message, Exception e) {
        test.log(Status.FAIL, message + ": " + e.getMessage());
        test.log(Status.FAIL, ExceptionUtils.getStackTrace(e));
        ScreenshotUtil.attachScreenshot(driver, "FailureScreenshot");
    }

    public void logFailure(ExtentTest test, WebDriver driver, String message, Throwable throwable) {
        test.log(Status.FAIL, message);
        test.log(Status.FAIL, throwable);
        ScreenshotUtil.attachScreenshot(driver, "FailureScreenshot");
    }
}
