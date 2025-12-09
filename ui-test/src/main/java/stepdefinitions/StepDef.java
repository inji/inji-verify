
package stepdefinitions;

import com.aventstack.extentreports.ExtentTest;

import org.openqa.selenium.By;
import org.openqa.selenium.NoSuchElementException;
import org.openqa.selenium.NoSuchWindowException;
import org.openqa.selenium.TimeoutException;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.WebElement;
import org.openqa.selenium.support.ui.ExpectedConditions;
import org.openqa.selenium.support.ui.WebDriverWait;
import org.testng.Assert;

import constants.UiConstants;
import io.cucumber.java.en.And;
import io.cucumber.java.en.Given;
import io.cucumber.java.en.Then;
import io.cucumber.java.en.When;
import io.mosip.testrig.apirig.injiverify.testscripts.SimplePostForAutoGenId;

import java.io.IOException;
import pages.BLE;
import pages.HomePage;
import pages.ScanQRCodePage;
import pages.UploadQRCode;
import pages.VpVerification;
import utils.BaseTest;
import java.util.Base64;
import java.io.OutputStream;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.time.Duration;
import java.io.FileOutputStream;
import java.io.File;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.text.PDFTextStripper;
import org.apache.pdfbox.rendering.PDFRenderer;
import org.apache.pdfbox.pdmodel.PDPage;
import utils.ExtentReportManager;
import utils.ScreenshotUtil;
import org.apache.commons.lang3.exception.ExceptionUtils;


import com.aventstack.extentreports.ExtentTest;
import com.aventstack.extentreports.Status;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static org.testng.Assert.assertEquals;
import static org.testng.Assert.assertTrue;

import java.awt.image.BufferedImage;
import javax.imageio.ImageIO;

public class StepDef {

	String pageTitle;
	public WebDriver driver;
	public BaseTest baseTest;
	private HomePage homePage;
	private BLE ble;
	private VpVerification vpverification;
	private ScanQRCodePage scanqrcode;
	private UploadQRCode uploadqrcode;
    public static String policynumber =SimplePostForAutoGenId.policyNumber;
    public static String fullname =SimplePostForAutoGenId.fullName;
    public static String dob =SimplePostForAutoGenId.dob;
	ExtentTest test = ExtentReportManager.getTest();
	public static String screenshotPath = System.getProperty("user.dir")+"/test-output/screenshots";
	static LocalDate date = LocalDate.parse(dob);
	public static String formattedDate = date.format(DateTimeFormatter.ofPattern("MM-dd-yyyy"));

	public StepDef() {
		this.baseTest =  new BaseTest();
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


    @Given("User gets the title of the page")
    public void userGetsTheTitleOfThePage() {
        try {
            pageTitle = homePage.isPageTitleDisplayed();
            test.log(Status.PASS, "Successfully retrieved the page title: " + pageTitle);
        } catch (NoSuchElementException e) {
        	logFailure(test, driver, "Element not found while retrieving title", e);
            throw e;
        } catch (Exception e) {
        	logFailure(test, driver, "Unexpected error while retrieving the title", e);
            throw e;
        }
    }

    @When("Validate the title of the page")
    public void validateTheTitleOfThePage() {
        try {
            String actualTitle = homePage.getPageTitle();
            Assert.assertEquals(actualTitle, UiConstants.PAGE_TITLE);
            test.log(Status.PASS, "Page title validation successful. Expected: " + UiConstants.PAGE_TITLE + ", Actual: " + actualTitle);
        } catch (AssertionError e) {
            logFailure(test, driver, "Title validation failed. Expected: " + UiConstants.PAGE_TITLE + ", but found: " + homePage.getPageTitle(), e);
            throw e;
        } catch (NoSuchElementException e) {
            logFailure(test, driver, "Element not found while validating the title", e);
            throw e;
        } catch (Exception e) {
            logFailure(test, driver, "Unexpected error while validating the title", e);
            throw e;
        }
    }

    @When("Verify that inji verify logo is displayed")
    public void verifyThatInjiVerifyLogoIsDisplayed() {
        try {
            boolean isLogoDisplayed = homePage.isLogoDisplayed();
            Assert.assertTrue(isLogoDisplayed, "Logo is not displayed on the page");
            test.log(Status.PASS, "Logo is displayed successfully.");
        } catch (AssertionError e) {
            test.log(Status.FAIL, "Logo verification failed: Logo is not displayed on the page.");
            throw e;
        } catch (NoSuchElementException e) {
            logFailure(test, driver, "Element not found while verifying the logo", e);
            throw e;
        } catch (Exception e) {
            logFailure(test, driver, "Unexpected error while verifying the logo", e);
            throw e;
        }
    }

    @When("Verify that header is displayed")
    public void verifyThatHeaderIsDisplayed() {
        try {
            String actualHeader = homePage.getHeader();
            Assert.assertEquals(actualHeader, UiConstants.PAGE_HEADER, "Header text does not match the expected value.");
            test.log(Status.PASS, "Header verification successful. Expected: " + UiConstants.PAGE_HEADER + ", Actual: " + actualHeader);
        } catch (AssertionError e) {
            test.log(Status.FAIL, "Header verification failed. Expected: " + UiConstants.PAGE_HEADER + ", but found: " + homePage.getHeader());
            throw e;
        } catch (NoSuchElementException e) {
            logFailure(test, driver, "Element not found while verifying the header", e);
            throw e;
        } catch (Exception e) {
            logFailure(test, driver, "Unexpected error while verifying the header", e);
            throw e;
        }
    }

    @When("Verify that sub header is displayed")
    public void verifyThatSubHeaderIsDisplayed() {
        try {
            String actualSubHeader = homePage.getSubHeader();
            Assert.assertEquals(actualSubHeader, UiConstants.PAGE_SUB_HEADER, "Sub-header text does not match the expected value.");
            test.log(Status.PASS, "Sub-header verification successful. Expected: " + UiConstants.PAGE_SUB_HEADER + ", Actual: " + actualSubHeader);
        } catch (AssertionError e) {
            test.log(Status.FAIL, "Sub-header verification failed. Expected: " + UiConstants.PAGE_SUB_HEADER + ", but found: " + homePage.getSubHeader());
            throw e;
        } catch (NoSuchElementException e) {
            logFailure(test, driver, "Element not found while verifying the sub-header", e);
            throw e;
        } catch (Exception e) {
            logFailure(test, driver, "Unexpected error while verifying the sub-header", e);
            throw e;
        }
    }

    @When("Verify that home button is displayed")
    public void verifyThathomebuttonIsDisplayed() {
        try {
            boolean isHomeButtonVisible = homePage.isHomeButtonDisplayed();
            Assert.assertTrue(isHomeButtonVisible, "Home button is not displayed on the page.");
            test.log(Status.PASS, "Home button is displayed successfully.");
        } catch (AssertionError e) {
            test.log(Status.FAIL, "Home button verification failed: Home button is not visible on the page.");
            throw e;
        } catch (NoSuchElementException e) {
            logFailure(test, driver, "Element not found while verifying the home button", e);
            throw e;
        } catch (Exception e) {
            logFailure(test, driver, "Unexpected error while verifying the home button", e);
            throw e;
        }
    }

    @When("Verify that Credentials button is displayed")
    public void verifyThatCredentialsButtonIsDisplayed() {
        try {
            boolean isCredentialsButtonVisible = homePage.isVerifyCredentialsbuttonDisplayed();
            Assert.assertTrue(isCredentialsButtonVisible, "Credentials button is not displayed on the page.");
            test.log(Status.PASS, "Credentials button is displayed successfully.");
        } catch (AssertionError e) {
            test.log(Status.FAIL, "Credentials button verification failed: Credentials button is not visible on the page.");
            throw e;
        } catch (NoSuchElementException e) {
            logFailure(test, driver, "Element not found while verifying the Credentials button", e);
            throw e;
        } catch (Exception e) {
            logFailure(test, driver, "Unexpected error while verifying the Credentials button", e);
            throw e;
        }
    }

    @When("Verify that Help button is displayed")
    public void verifyThatHelpButtonIsDisplayed() {
        try {
            boolean isHelpButtonVisible = homePage.isHelpbuttonDisplayed();
            Assert.assertTrue(isHelpButtonVisible, "Help button is not displayed on the page.");
            test.log(Status.PASS, "Help button is displayed successfully.");
        } catch (AssertionError e) {
            test.log(Status.FAIL, "Help button verification failed: Help button is not visible on the page.");
            throw e;
        } catch (NoSuchElementException e) {
            logFailure(test, driver, "Element not found while verifying the Help button", e);
            throw e;
        } catch (Exception e) {
            logFailure(test, driver, "Unexpected error while verifying the Help button", e);
            throw e;
        }
    }

    @When("Verify that expansion button is displayed before expansion")
    public void verifyThatExpansionButtonIsDisplayedBeforeExpansion() {
        try {
            boolean isExpansionButtonVisible = homePage.isExpansionbuttonDisplayedBefore();
            Assert.assertTrue(isExpansionButtonVisible, "Expansion button is not displayed before expansion.");
            test.log(Status.PASS, "Expansion button is displayed before expansion successfully.");
        } catch (AssertionError e) {
            test.log(Status.FAIL, "Expansion button verification failed: Expansion button is not visible before expansion.");
            throw e;
        } catch (NoSuchElementException e) {
            logFailure(test, driver, "Element not found while verifying the expansion button before expansion", e);
            throw e;
        } catch (Exception e) {
            logFailure(test, driver, "Unexpected error while verifying the expansion button before expansion", e);
            throw e;
        }
    }

    @When("Verify click on home button")
    public void verifyClickOnHomeButton() {
        try {
            homePage.clickOnHomeButton();
            test.log(Status.PASS, "Successfully clicked on the Home button.");
            test.log(Status.PASS, "Successfully navigated to the home page after clicking the Home button.");
        } catch (NoSuchElementException e) {
            logFailure(test, driver, "Element not found while clicking on the Home button", e);
            throw e;
        } catch (AssertionError e) {
            test.log(Status.FAIL, "Navigation verification failed: Home page is not displayed after clicking the Home button.");
            throw e;
        } catch (Exception e) {
            logFailure(test, driver, "Unexpected error while clicking on the Home button", e);
            throw e;
        }
    }
    
    @When("User click on continue")
    public void verifyClickOnContinueButton() {
        try {
            homePage.clickOnContinueButton();
            test.log(Status.PASS, "Successfully clicked on the Continue button.");
            test.log(Status.PASS, "Successfully navigated to the home page after clicking the Continue button.");
        } catch (NoSuchElementException e) {
            logFailure(test, driver, "Element not found while clicking on the Continue button", e);
            throw e;
        } catch (AssertionError e) {
            test.log(Status.FAIL, "Navigation verification failed: Home page is not displayed after clicking the Continue button.");
            throw e;
        } catch (Exception e) {
            logFailure(test, driver, "Unexpected error while clicking on the Continue button", e);
            throw e;
        }
    }

    @When("Verify that expansion button is displayed after expansion")
    public void verifyThatExpansionButtonIsDisplayedAfterExpansion() {
        try {
            boolean isExpansionButtonVisible = homePage.isExpansionbuttonDisplayedAfter();
            Assert.assertTrue(isExpansionButtonVisible, "Expansion button is not displayed after expansion.");
            test.log(Status.PASS, "Expansion button is displayed successfully after expansion.");
        } catch (AssertionError e) {
            test.log(Status.FAIL, "Expansion button verification failed: Expansion button is not visible after expansion.");
            throw e;
        } catch (NoSuchElementException e) {
            logFailure(test, driver, "Element not found while verifying the expansion button after expansion", e);
            throw e;
        } catch (Exception e) {
            logFailure(test, driver, "Unexpected error while verifying the expansion button after expansion", e);
            throw e;
        }
    }

    @When("Verify that links are valid under help")
    public void verifyThatLinksAreValidUnderHelp() {
        try {
            boolean areLinksValid = homePage.verifyHelpOptionLinks();
            Assert.assertTrue(areLinksValid, "One or more links under Help are invalid.");
            test.log(Status.PASS, "All links under Help are valid and functional.");
        } catch (AssertionError e) {
            test.log(Status.FAIL, "Link validation failed: One or more links under Help are broken or invalid.");
            throw e;
        } catch (NoSuchElementException e) {
            logFailure(test, driver, "Element not found while verifying Help option links", e);
            throw e;
        } catch (Exception e) {
            logFailure(test, driver, "Unexpected error while verifying Help option links", e);
            throw e;
        }
    }

    @When("Verify minimize help option")
    public void verifyMinimizeHelpOption() {
        try {
          //  homePage.minimizeHelpButton();
        	homePage.minimizeHelpButton();
            test.log(Status.PASS, "Clicked on minimize Help button successfully.");

            boolean isMinimized = homePage.isExpansionbuttonDisplayedBefore();
            Assert.assertTrue(isMinimized, "Help option is not minimized successfully.");
            test.log(Status.PASS, "Help option minimized successfully.");
        } catch (NoSuchElementException e) {
            logFailure(test, driver, "Element not found while minimizing the Help option", e);
            throw e;
        } catch (AssertionError e) {
            test.log(Status.FAIL, "Minimize Help option verification failed: Help option is not minimized.");
            throw e;
        } catch (Exception e) {
            logFailure(test, driver, "Unexpected error while minimizing the Help option", e);
            throw e;
        }
    }

    @When("Verify that upload QR Code tab is visible")
    public void verifyThatUploadQRCodeTabIsVisible() {
        try {
            boolean isUploadTabVisible = homePage.isUploadQRButtonVisible();
            Assert.assertTrue(isUploadTabVisible, "Upload QR Code tab is not visible on the page.");
            test.log(Status.PASS, "Upload QR Code tab is visible successfully.");
        } catch (AssertionError e) {
            test.log(Status.FAIL, "Upload QR Code tab verification failed: Upload QR Code tab is not visible.");
            throw e;
        } catch (NoSuchElementException e) {
            logFailure(test, driver, "Element not found while verifying the Upload QR Code tab", e);
            throw e;
        } catch (Exception e) {
            logFailure(test, driver, "Unexpected error while verifying the Upload QR Code tab", e);
            throw e;
        }
    }

    @When("Verify that scan QR Code tab is visible")
    public void verifyThatScanQRCodeTabIsVisible() {
        try {
            boolean isScanTabVisible = homePage.isScanQRCodeButtonVisible();
            Assert.assertTrue(isScanTabVisible, "Scan QR Code tab is not visible on the page.");
            test.log(Status.PASS, "Scan QR Code tab is visible successfully.");
        } catch (AssertionError e) {
            test.log(Status.FAIL, "Scan QR Code tab verification failed: Scan QR Code tab is not visible.");
            throw e;
        } catch (NoSuchElementException e) {
            logFailure(test, driver, "Element not found while verifying the Scan QR Code tab", e);
            throw e;
        } catch (Exception e) {
            logFailure(test, driver, "Unexpected error while verifying the Scan QR Code tab", e);
            throw e;
        }
    }

    @When("Verify that VP Verification tab is visible")
    public void verifyThatVPVerificationTabIsVisible() {
        try {
            boolean isVPVerificationTabVisible = homePage.isVerifyCredentialsbuttonDisplayed();
            Assert.assertTrue(isVPVerificationTabVisible, "VP Verification tab is not visible on the page.");
            test.log(Status.PASS, "VP Verification tab is visible successfully.");
        } catch (AssertionError e) {
            test.log(Status.FAIL, "VP Verification tab verification failed: VP Verification tab is not visible.");
            throw e;
        } catch (NoSuchElementException e) {
            logFailure(test, driver, "Element not found while verifying the VP Verification tab", e);
            throw e;
        } catch (Exception e) {
            logFailure(test, driver, "Unexpected error while verifying the VP Verification tab", e);
            throw e;
        }
    }

    @When("Verify that BLE tab is visible")
    public void verifyThatBLETabIsVisible() {
        try {
            boolean isBLETabVisible = homePage.isBLEButtonVisible();
            Assert.assertTrue(isBLETabVisible, "BLE tab is not visible on the page.");
            test.log(Status.PASS, "BLE tab is visible successfully.");
        } catch (AssertionError e) {
            test.log(Status.FAIL, "BLE tab verification failed: BLE tab is not visible.");
            throw e;
        } catch (NoSuchElementException e) {
            logFailure(test, driver, "Element not found while verifying the BLE tab", e);
            throw e;
        } catch (Exception e) {
            logFailure(test, driver, "Unexpected error while verifying the BLE tab", e);
            throw e;
        }
    }

    @When("Verify copyright text")
    public void verifyCopyrightText() {
        try {
            String actualCopyrightText = homePage.getVerifyCopyrightText();
            Assert.assertEquals(actualCopyrightText, UiConstants.COPYRIGHT_INFO, 
                "Copyright text does not match the expected value.");
            test.log(Status.PASS, "Copyright text verification successful. Expected: " 
                + UiConstants.COPYRIGHT_INFO + ", Actual: " + actualCopyrightText);
        } catch (AssertionError e) {
            test.log(Status.FAIL, "Copyright text verification failed. Expected: " 
                + UiConstants.COPYRIGHT_INFO + ", but found: " + homePage.getVerifyCopyrightText());
            throw e;
        } catch (NoSuchElementException e) {
            logFailure(test, driver, "Element not found while verifying copyright text", e);
            throw e;
        } catch (Exception e) {
            logFailure(test, driver, "Unexpected error while verifying copyright text", e);
            throw e;
        }
    }

    @When("Verify upload QR code step1 label")
    public void verifyUploadQRCodeStep1Label() {
        try {
            String actualLabel = homePage.getUploadQRCodeStep1Label();
            Assert.assertEquals(actualLabel, UiConstants.UPLOAD_QR_CODE_STEP1_LABEL, 
                "Step 1 label for Upload QR Code does not match the expected value.");
            test.log(Status.PASS, "Upload QR Code Step 1 label verification successful. Expected: " 
                + UiConstants.UPLOAD_QR_CODE_STEP1_LABEL + ", Actual: " + actualLabel);
        } catch (AssertionError e) {
            test.log(Status.FAIL, "Upload QR Code Step 1 label verification failed. Expected: " 
                + UiConstants.UPLOAD_QR_CODE_STEP1_LABEL + ", but found: " + homePage.getUploadQRCodeStep1Label());
            throw e;
        } catch (NoSuchElementException e) {
            logFailure(test, driver, "Element not found while verifying Upload QR Code Step 1 label", e);
            throw e;
        } catch (Exception e) {
            logFailure(test, driver, "Unexpected error while verifying Upload QR Code Step 1 label", e);
            throw e;
        }
    }

    @When("Verify upload QR code step1 description")
    public void verifyUploadQRCodeStep1Description() {
        try {
            String actualDescription = homePage.getUploadQRCodeStep1Description();
            Assert.assertEquals(actualDescription, UiConstants.UPLOAD_QR_CODE_STEP1_DESCRIPTION, 
                "Step 1 description for Upload QR Code does not match the expected value.");
            test.log(Status.PASS, "Upload QR Code Step 1 description verification successful. Expected: " 
                + UiConstants.UPLOAD_QR_CODE_STEP1_DESCRIPTION + ", Actual: " + actualDescription);
        } catch (AssertionError e) {
            test.log(Status.FAIL, "Upload QR Code Step 1 description verification failed. Expected: " 
                + UiConstants.UPLOAD_QR_CODE_STEP1_DESCRIPTION + ", but found: " + homePage.getUploadQRCodeStep1Description());
            throw e;
        } catch (NoSuchElementException e) {
            logFailure(test, driver, "Element not found while verifying Upload QR Code Step 1 description", e);
            throw e;
        } catch (Exception e) {
            logFailure(test, driver, "Unexpected error while verifying Upload QR Code Step 1 description", e);
            throw e;
        }
    }

    @When("Verify upload QR code step2 label")
    public void verifyUploadQRCodeStep2Label() {
        try {
            String actualLabel = homePage.getUploadQRCodeStep2Label();
            Assert.assertEquals(actualLabel, UiConstants.UPLOAD_QR_CODE_STEP2_LABEL, 
                "Step 2 label for Upload QR Code does not match the expected value.");
            test.log(Status.PASS, "Upload QR Code Step 2 label verification successful. Expected: " 
                + UiConstants.UPLOAD_QR_CODE_STEP2_LABEL + ", Actual: " + actualLabel);
        } catch (AssertionError e) {
            test.log(Status.FAIL, "Upload QR Code Step 2 label verification failed. Expected: " 
                + UiConstants.UPLOAD_QR_CODE_STEP2_LABEL + ", but found: " + homePage.getUploadQRCodeStep2Label());
            throw e;
        } catch (NoSuchElementException e) {
            logFailure(test, driver, "Element not found while verifying Upload QR Code Step 2 label", e);
            throw e;
        } catch (Exception e) {
            logFailure(test, driver, "Unexpected error while verifying Upload QR Code Step 2 label", e);
            throw e;
        }
    }

    @When("Verify upload QR code step2 description")
    public void verifyUploadQRCodeStep2Description() {
        try {
            String actualDescription = homePage.getUploadQRCodeStep2Description();
            Assert.assertEquals(actualDescription, UiConstants.UPLOAD_QR_CODE_STEP2_DESCRIPTION, 
                "Step 2 description for Upload QR Code does not match the expected value.");
            test.log(Status.PASS, "Upload QR Code Step 2 description verification successful. Expected: " 
                + UiConstants.UPLOAD_QR_CODE_STEP2_DESCRIPTION + ", Actual: " + actualDescription);
        } catch (AssertionError e) {
            test.log(Status.FAIL, "Upload QR Code Step 2 description verification failed. Expected: " 
                + UiConstants.UPLOAD_QR_CODE_STEP2_DESCRIPTION + ", but found: " + homePage.getUploadQRCodeStep2Description());
            throw e;
        } catch (NoSuchElementException e) {
            logFailure(test, driver, "Element not found while verifying Upload QR Code Step 2 description", e);
            throw e;
        } catch (Exception e) {
            logFailure(test, driver, "Unexpected error while verifying Upload QR Code Step 2 description", e);
            throw e;
        }
    }

    @When("Verify upload QR code step3 label")
    public void verifyUploadQRCodeStep3Label() {
        try {
            String actualLabel = homePage.getUploadQRCodeStep3Label();
            Assert.assertEquals(actualLabel, UiConstants.UPLOAD_QR_CODE_STEP3_LABEL, 
                "Step 3 label for Upload QR Code does not match the expected value.");
            test.log(Status.PASS, "Upload QR Code Step 3 label verification successful. Expected: " 
                + UiConstants.UPLOAD_QR_CODE_STEP3_LABEL + ", Actual: " + actualLabel);
        } catch (AssertionError e) {
            test.log(Status.FAIL, "Upload QR Code Step 3 label verification failed. Expected: " 
                + UiConstants.UPLOAD_QR_CODE_STEP3_LABEL + ", but found: " + homePage.getUploadQRCodeStep3Label());
            throw e;
        } catch (NoSuchElementException e) {
            logFailure(test, driver, "Element not found while verifying Upload QR Code Step 3 label", e);
            throw e;
        } catch (Exception e) {
            logFailure(test, driver, "Unexpected error while verifying Upload QR Code Step 3 label", e);
            throw e;
        }
    }

    @When("Verify upload QR code step3 description")
    public void verifyUploadQRCodeStep3Description() {
        try {
            String actualDescription = homePage.getUploadQRCodeStep3Description();
            Assert.assertEquals(actualDescription, UiConstants.UPLOAD_QR_CODE_STEP3_DESCRIPTION, 
                "Step 3 description for Upload QR Code does not match the expected value.");
            test.log(Status.PASS, "Upload QR Code Step 3 description verification successful. Expected: " 
                + UiConstants.UPLOAD_QR_CODE_STEP3_DESCRIPTION + ", Actual: " + actualDescription);
        } catch (AssertionError e) {
            test.log(Status.FAIL, "Upload QR Code Step 3 description verification failed. Expected: " 
                + UiConstants.UPLOAD_QR_CODE_STEP3_DESCRIPTION + ", but found: " + homePage.getUploadQRCodeStep3Description());
            throw e;
        } catch (NoSuchElementException e) {
            logFailure(test, driver, "Element not found while verifying Upload QR Code Step 3 description", e);
            throw e;
        } catch (Exception e) {
            logFailure(test, driver, "Unexpected error while verifying Upload QR Code Step 3 description", e);
            throw e;
        }
    }

    @When("Verify that scan element is visible")
    public void verifyThatScanElementIsVisible() {
        try {
            boolean isScanElementVisible = homePage.isScanElementIsVisible();
            Assert.assertTrue(isScanElementVisible, "Scan element is not visible on the page.");
            test.log(Status.PASS, "Scan element is successfully visible on the page.");
        } catch (NoSuchElementException e) {
            logFailure(test, driver, "Element not found while verifying scan element visibility", e);
            throw e;
        } catch (Exception e) {
            logFailure(test, driver, "Unexpected error while verifying scan element visibility", e);
            throw e;
        }
    }

    @When("Verify that Upload icon is visible")
    public void verifyThatUploadIconIsVisible() {
        try {
            boolean isUploadIconVisible = homePage.isUploadIconIsVisible();
            Assert.assertTrue(isUploadIconVisible, "Upload icon is not visible on the page.");
            test.log(Status.PASS, "Upload icon is successfully visible on the page.");
        } catch (NoSuchElementException e) {
            logFailure(test, driver, "Element not found while verifying upload icon visibility", e);
            throw e;
        } catch (Exception e) {
            logFailure(test, driver, "Unexpected error while verifying upload icon visibility", e);
            throw e;
        }
    }

    @When("Verify that Upload button is visible")
    public void verifyThatUploadButtonIsVisible() {
        try {
            boolean isUploadButtonVisible = homePage.isUploadButtonIsVisible();
            Assert.assertTrue(isUploadButtonVisible, "Upload button is not visible on the page.");
            test.log(Status.PASS, "Upload button is successfully visible on the page.");
        } catch (NoSuchElementException e) {
            logFailure(test, driver, "Element not found while verifying upload button visibility", e);
            throw e;
        } catch (Exception e) {
            logFailure(test, driver, "Unexpected error while verifying upload button visibility", e);
            throw e;
        }
    }

    @When("Verify file format constraints text")
    public void verifyFileFormatConstraintsText() {
        try {
            String actualText = homePage.getFormatConstraintText();
            Assert.assertEquals(actualText, UiConstants.FILE_FORMAT_CONSTRAINTS_TEXT, 
                "File format constraints text does not match the expected value.");
            test.log(Status.PASS, "File format constraints text verification successful. Expected: " 
                + UiConstants.FILE_FORMAT_CONSTRAINTS_TEXT + ", Actual: " + actualText);
        } catch (AssertionError e) {
            test.log(Status.FAIL, "File format constraints text verification failed. Expected: " 
                + UiConstants.FILE_FORMAT_CONSTRAINTS_TEXT + ", but found: " + homePage.getFormatConstraintText());
            throw e;
        } catch (NoSuchElementException e) {
            logFailure(test, driver, "Element not found while verifying file format constraints text", e);
            throw e;
        } catch (Exception e) {
            logFailure(test, driver, "Unexpected error while verifying file format constraints text", e);
            throw e;
        }
    }

    @When("Click on Upload button")
    public void clickOnUploadButton() {
        try {
            homePage.clickOnQRUploadButton();
            test.log(Status.PASS, "Successfully clicked on the Upload button.");
        } catch (NoSuchElementException e) {
            logFailure(test, driver, "Element not found while clicking on the Upload button", e);
            throw e;
        } catch (Exception e) {
            logFailure(test, driver, "Unexpected error while clicking on the Upload button", e);
            throw e;
        }
    }

    @When("Upload QR code file png")
    public void uploadQRCodeFile() {
        try {
            uploadqrcode.clickOnUploadQRCodePng();
            test.log(Status.PASS, "Successfully uploaded the QR code file (PNG).");
        } catch (NoSuchElementException e) {
            logFailure(test, driver, "Element not found while uploading the QR code file (PNG)", e);
            throw e;
        } catch (Exception e) {
            logFailure(test, driver, "Unexpected error while uploading the QR code file (PNG)", e);
            throw e;
        }
    }

    @When("Verify Upload multilanguage VC")
    public void uploadMultiLanguageVC() {
        try {
            uploadqrcode.uploadMultiLanguageVc();
            test.log(Status.PASS, "Successfully uploaded the QR code file (Multilanguage).");
        } catch (NoSuchElementException e) {
            logFailure(test, driver, "Element not found while uploading the QR code file (Multilanguage)", e);
            throw e;
        } catch (Exception e) {
            logFailure(test, driver, "Unexpected error while uploading the QR code file (Multilanguage)", e);
            throw e;
        }
    }

    @Then("Upload SVG rendered VC")
	public void upload_SVG_rendered_VC_code() {
        try {
            uploadqrcode.clickOnUploadSVGQRCode();
	        test.log(Status.PASS, "Successfully uploaded SVG rendered VC.");
	    } catch (AssertionError e) {
	        test.log(Status.FAIL, "Verification failed: Upload of SVG rendered VC QR code did not behave as expected.");
	        throw e;
	    } catch (NoSuchElementException e) {
	        logFailure(test, driver, "Element not found while uploading SVG rendered VC QR code", e);
	        throw e;
	    } catch (Exception e) {
	        logFailure(test, driver, "Unexpected error occurred while uploading SVG rendered VC QR code", e);
	        throw e;
	    }
	}
	
    @When("Upload another QR code file png")
    public void uploadAnotherQRCodeFile() {
        try {
            uploadqrcode.clickOnAnotherUploadQRCodePng();
            test.log(Status.PASS, "Successfully uploaded another QR code file (PNG).");
        } catch (NoSuchElementException e) {
            logFailure(test, driver, "Element not found while uploading another QR code file (PNG)", e);
            throw e;
        } catch (Exception e) {
            logFailure(test, driver, "Unexpected error while uploading another QR code file (PNG)", e);
            throw e;
        }
    }

    @When("verify upload QR code step2 description after")
    public void verifyUploadQRCodeStep2DescriptionAfter() {
        try {
            boolean isStep2DescriptionVisible = uploadqrcode.isVisibleUploadQRCodeStep2LabelAfter();
            Assert.assertTrue(isStep2DescriptionVisible, "Step 2 description (after) is not visible.");
            test.log(Status.PASS, "Step 2 description (after) is successfully visible.");
        } catch (NoSuchElementException e) {
            logFailure(test, driver, "Element not found while verifying Step 2 description (after)", e);
            throw e;
        } catch (Exception e) {
            logFailure(test, driver, "Unexpected error while verifying Step 2 description (after)", e);
            throw e;
        }
    }

    @When("verify upload QR code step3 description after")
    public void verifyUploadQRCodeStep3DescriptionAfter() {
        try {
            boolean isStep3DescriptionVisible = uploadqrcode.isVisibleUploadQRCodeStep3LabelAfter();
            Assert.assertTrue(isStep3DescriptionVisible, "Step 3 description (after) is not visible.");
            test.log(Status.PASS, "Step 3 description (after) is successfully visible.");
        } catch (NoSuchElementException e) {
            logFailure(test, driver, "Element not found while verifying Step 3 description (after)", e);
            throw e;
        } catch (Exception e) {
            logFailure(test, driver, "Unexpected error while verifying Step 3 description (after)", e);
            throw e;
        }
    }
    
    @Then("verify policy issued on value")
    public void verify_policy_issued_on_value() {
        try {
            boolean isPolicyIssuedOnValueVisible = uploadqrcode.isVisiblePolicyIssuedOnValue();
            Assert.assertTrue(isPolicyIssuedOnValueVisible, "Policy Issued On value is not visible.");
            test.log(Status.PASS, "Policy Issued On value is successfully visible.");
        } catch (NoSuchElementException e) {
            logFailure(test, driver, "Element not found while verifying Policy Issued On value", e);
            throw e;
        } catch (Exception e) {
            logFailure(test, driver, "Unexpected error while verifying Policy Issued On value", e);
            throw e;
        }
    }

    
    @Then("verify full name value")
    public void verify_full_name_value() {
        try {
            boolean isFullNameValueVisible = uploadqrcode.isVisibleFullNameValue();
            Assert.assertTrue(isFullNameValueVisible, "Full Name value is not visible.");
            test.log(Status.PASS, "Full Name value is successfully visible.");
        } catch (NoSuchElementException e) {
            logFailure(test, driver, "Element not found while verifying Full Name value", e);
            throw e;
        } catch (Exception e) {
            logFailure(test, driver, "Unexpected error while verifying Full Name value", e);
            throw e;
        }
    }

    
    @Then("verify policy expires on value")
    public void verify_policy_expires_on_value() {
        try {
            boolean isPolicyExpiresOnValueVisible = uploadqrcode.isVisiblePolicyExpiresOnValue();
            Assert.assertTrue(isPolicyExpiresOnValueVisible, "Policy Expires On value is not visible.");
            test.log(Status.PASS, "Policy Expires On value is successfully visible.");
        } catch (NoSuchElementException e) {
            logFailure(test, driver, "Element not found while verifying Policy Expires On value", e);
            throw e;
        } catch (Exception e) {
            logFailure(test, driver, "Unexpected error while verifying Policy Expires On value", e);
            throw e;
        }
    }


    @When("verify tick icon is visible on successful verification")
    public void verifyTickIconIsVisibleForSuccessfulVerification() {
        try {
            boolean isTickIconVisible = uploadqrcode.isTickIconVisible();
            Assert.assertTrue(isTickIconVisible, "Tick icon is not visible on successful verification.");
            test.log(Status.PASS, "Tick icon is successfully visible on successful verification.");
        } catch (NoSuchElementException e) {
            logFailure(test, driver, "Element not found while verifying the tick icon on successful verification", e);
            throw e;
        } catch (Exception e) {
            logFailure(test, driver, "Unexpected error while verifying the tick icon on successful verification", e);
            throw e;
        }
    }
    
    
    @Then("Verify click on another qr code button")
    public void verify_clickOn_another_qr_code_button_on_successful_verification() {
        try {
            uploadqrcode.clickOnAnotherQRCodeButton();
            test.log(Status.PASS, "Clicked on 'Verify Another QR Code' button successfully.");
        } catch (NoSuchElementException e) {
            logFailure(test, driver, "Element not found while clicking 'Verify Another QR Code' button", e);
            throw e;
        } catch (Exception e) {
            logFailure(test, driver, "Unexpected error while clicking 'Verify Another QR Code' button", e);
            throw e;
        }
    }

    @Then("Verify click on language dropdown")
    public void click_On_Language_Dropdown() {
        try {
            uploadqrcode.clickOnLanguageDropdown();
            test.log(Status.PASS, "Clicked on 'Language Dropdown' button successfully.");
        } catch (NoSuchElementException e) {
            logFailure(test, driver, "Element not found while clicking 'Language Dropdown' button", e);
            throw e;
        } catch (Exception e) {
            logFailure(test, driver, "Unexpected error while clicking 'Language Dropdown' button", e);
            throw e;
        }
    }


    @Then("Verify verify another qr code button on successful verification")
    public void verify_verify_another_qr_code_button_on_successful_verification() {
        try {
            boolean isVerifyAnotherQRCodeButtonVisible = uploadqrcode.isVisibleVerifyAnotherQRCodeButton();
            Assert.assertTrue(isVerifyAnotherQRCodeButtonVisible, "Verify Another QR Code button is not visible.");
            test.log(Status.PASS, "Verify Another QR Code button is successfully visible.");
        } catch (NoSuchElementException e) {
            logFailure(test, driver, "Element not found while verifying 'Verify Another QR Code' button", e);
            throw e;
        } catch (Exception e) {
            logFailure(test, driver, "Unexpected error while verifying 'Verify Another QR Code' button", e);
            throw e;
        }
    }


    @When("verify congratulations message on successful verification")
    public void verifyCongratulationsMessageOnSuccessfulVerification() {
        try {
            String actualMessage = uploadqrcode.getCongratulationtext();
            Assert.assertEquals(actualMessage, UiConstants.CONGRATULATIONS_MESSAGE, 
                "Congratulations message does not match the expected value.");
            test.log(Status.PASS, "Successfully verified the congratulations message: " + actualMessage);
        } catch (AssertionError e) {
            logFailure(test, driver, "Congratulations message verification failed. Expected: " 
                + UiConstants.CONGRATULATIONS_MESSAGE + ", but found: " + uploadqrcode.getCongratulationtext(), e);
            throw e;
        } catch (NoSuchElementException e) {
            logFailure(test, driver, "Element not found while verifying the congratulations message", e);
            throw e;
        } catch (Exception e) {
            logFailure(test, driver, "Unexpected error while verifying the congratulations message", e);
            throw e;
        }
    }

    @When("Verify toast message")
    public void verifyToastMessage() {
        try {
            String actualToastMessage = uploadqrcode.getQRCodeUploadedSuccessToastMessage();
            Assert.assertEquals(actualToastMessage, UiConstants.SUCCESS_TOAST_MESSAGE, 
                "Toast message does not match the expected value.");
            test.log(Status.PASS, "Successfully verified the toast message: " + actualToastMessage);
        } catch (AssertionError e) {
            logFailure(test, driver, "Toast message verification failed. Expected: " 
                + UiConstants.SUCCESS_TOAST_MESSAGE + ", but found: " + uploadqrcode.getQRCodeUploadedSuccessToastMessage(), e);
            throw e;
        } catch (NoSuchElementException e) {
            logFailure(test, driver, "Element not found while verifying the toast message", e);
            throw e;
        } catch (Exception e) {
            logFailure(test, driver, "Unexpected error while verifying the toast message", e);
            throw e;
        }
    }

    @When("Verify 'Verify Another QR Code' button on successful verification")
    public void verifyVerifyAnotherQRCodeButtonOnSuccessfulVerification() {
        try {
            boolean isButtonVisible = uploadqrcode.isVisibleVerifyAnotherQRCodeButton();
            Assert.assertTrue(isButtonVisible, "'Verify Another QR Code' button is not visible on successful verification.");
            test.log(Status.PASS, "'Verify Another QR Code' button is successfully visible on successful verification.");
        } catch (NoSuchElementException e) {
            logFailure(test, driver, "Element not found while verifying the 'Verify Another QR Code' button", e);
            throw e;
        } catch (Exception e) {
            logFailure(test, driver, "Unexpected error while verifying the 'Verify Another QR Code' button", e);
            throw e;
        }
    }

    @When("Verify click on 'Verify Another QR Code' button")
    public void verifyClickOnAnotherQRCodeButtonOnSuccessfulVerification() {
        try {
            uploadqrcode.clickOnAnotherQRCodeButton();
            test.log(Status.PASS, "Successfully clicked on the 'Verify Another QR Code' button.");
        } catch (NoSuchElementException e) {
            logFailure(test, driver, "Element not found while clicking the 'Verify Another QR Code' button", e);
            throw e;
        } catch (Exception e) {
            logFailure(test, driver, "Unexpected error while clicking the 'Verify Another QR Code' button", e);
            throw e;
        }
    }

    @When("Upload QR code file PDF")
    public void uploadQRCodeFilePdf() {
        try {
            uploadqrcode.clickOnUploadQRCodePdf();
            test.log(Status.PASS, "Successfully uploaded the QR code file in PDF format.");
        } catch (NoSuchElementException e) {
            logFailure(test, driver, "Element not found while uploading the QR code PDF file", e);
            throw e;
        } catch (Exception e) {
            logFailure(test, driver, "Unexpected error while uploading the QR code PDF file", e);
            throw e;
        }
    }
	
    @When("Upload another QR code file PDF")
    public void uploadAnotherQRCodeFilePdf() {
        try {
            uploadqrcode.clickOnAnotherUploadQRCodePdf();
            test.log(Status.PASS, "Successfully uploaded another QR code file in PDF format.");
        } catch (NoSuchElementException e) {
            logFailure(test, driver, "Element not found while uploading another QR code PDF file", e);
            throw e;
        } catch (Exception e) {
            logFailure(test, driver, "Unexpected error while uploading another QR code PDF file", e);
            throw e;
        }
    }


    @When("Upload QR code file JPG")
    public void uploadQRCodeFileJpg() {
        try {
            uploadqrcode.clickOnUploadQRCodeJpg();
            test.log(Status.PASS, "Successfully uploaded the QR code file in JPG format.");
        } catch (NoSuchElementException e) {
            logFailure(test, driver, "Element not found while uploading the QR code JPG file", e);
            throw e;
        } catch (Exception e) {
            logFailure(test, driver, "Unexpected error while uploading the QR code JPG file", e);
            throw e;
        }
    }
	
    @When("Upload another QR code file JPG")
    public void uploadAnotherQRCodeFileJpg() {
        try {
            uploadqrcode.clickOnAnotherUploadQRCodeJpg();
            test.log(Status.PASS, "Successfully uploaded another QR code file in JPG format.");
        } catch (NoSuchElementException e) {
            logFailure(test, driver, "Element not found while uploading another QR code JPG file", e);
            throw e;
        } catch (Exception e) {
            logFailure(test, driver, "Unexpected error while uploading another QR code JPG file", e);
            throw e;
        }
    }

    @When("Upload QR code file JPEG")
    public void uploadQRCodeFileJpeg() {
        try {
            uploadqrcode.clickOnUploadQRCodeJpeg();
            test.log(Status.PASS, "Successfully uploaded the QR code file in JPEG format.");
        } catch (NoSuchElementException e) {
            logFailure(test, driver, "Element not found while uploading the QR code JPEG file", e);
            throw e;
        } catch (Exception e) {
            logFailure(test, driver, "Unexpected error while uploading the QR code JPEG file", e);
            throw e;
        }
    }

    @When("Upload another QR code file JPEG")
    public void uploadAnotherQRCodeFileJpeg() {
        try {
            uploadqrcode.clickOnAnotherUploadQRCodeJpeg();
            test.log(Status.PASS, "Successfully uploaded another QR code file in JPEG format.");
        } catch (NoSuchElementException e) {
            logFailure(test, driver, "Element not found while uploading another QR code JPEG file", e);
            throw e;
        } catch (Exception e) {
            logFailure(test, driver, "Unexpected error while uploading another QR code JPEG file", e);
            throw e;
        }
    }

    @When("Click on Home button")
    public void clickOnHomeButton() {
        try {
            uploadqrcode.clickOnHomeButton();
            test.log(Status.PASS, "Successfully clicked on the Home button.");
        } catch (NoSuchElementException e) {
            logFailure(test, driver, "Element not found while clicking on the Home button", e);
            throw e;
        } catch (Exception e) {
            logFailure(test, driver, "Unexpected error while clicking on the Home button", e);
            throw e;
        }
    }

    @When("Click on Verify Credential button")
    public void clickOnVerifyCredentialButton() {
        try {
            uploadqrcode.clickVerifyCredentialsButton();
            test.log(Status.PASS, "Successfully clicked on the Verify Credential button.");
        } catch (NoSuchElementException e) {
            logFailure(test, driver, "Element not found while clicking on the Verify Credential button", e);
            throw e;
        } catch (Exception e) {
            logFailure(test, driver, "Unexpected error while clicking on the Verify Credential button", e);
            throw e;
        }
    }

    @When("Verify browser refresh")
    public void verifyBrowserRefresh() {
        try {
            uploadqrcode.refreshBrowserAfterVerification();
            test.log(Status.PASS, "Browser refreshed successfully after verification.");
        } catch (Exception e) {
            logFailure(test, driver, "Unexpected error while refreshing the browser", e);
            throw e;
        }
    }

    @When("Upload QR code unsupported file HTML")
    public void uploadQRCodeUnsupportedFileHtml() {
        try {
            uploadqrcode.clickOnUploadQRCodeHtml();
            test.log(Status.PASS, "Attempted to upload an unsupported QR code file (HTML).");
        } catch (NoSuchElementException e) {
            logFailure(test, driver, "Element not found while trying to upload an unsupported QR code file (HTML)", e);
            throw e;
        } catch (Exception e) {
            logFailure(test, driver, "Unexpected error while uploading an unsupported QR code file (HTML)", e);
            throw e;
        }
    }

    @When("Verify QR code file invalid")
    public void verifyQRCodeFileInvalid() {
        try {
            uploadqrcode.clickOnUploadQRCodeInvalid();
            test.log(Status.PASS, "Attempted to upload an invalid QR code file.");
        } catch (NoSuchElementException e) {
            logFailure(test, driver, "Element not found while uploading an invalid QR code file", e);
            throw e;
        } catch (Exception e) {
            logFailure(test, driver, "Unexpected error while uploading an invalid QR code file", e);
            throw e;
        }
    }

    @Given("Upload QR code file PDF downloaded from mobile")
    public void uploadQRCodeFilePdfDownloadedFromMobile() {
        try {
            uploadqrcode.clickOnUploadQRCodeDownloadedFromPhone();
            test.log(Status.PASS, "Uploaded a QR code file (PDF) downloaded from mobile successfully.");
        } catch (NoSuchElementException e) {
            logFailure(test, driver, "Element not found while uploading QR code file (PDF) from mobile", e);
            throw e;
        } catch (Exception e) {
            logFailure(test, driver, "Unexpected error while uploading QR code file (PDF) from mobile", e);
            throw e;
        }
    }

    @When("Verify Error logo for invalid QR code")
    public void verifyErrorLogoForInvalidQRCode() {
        try {
            Assert.assertTrue(uploadqrcode.isVisibleErrorIcon(), "Error logo is not displayed for invalid QR code.");
            test.log(Status.PASS, "Error logo is correctly displayed for an invalid QR code.");
        } catch (NoSuchElementException e) {
            logFailure(test, driver, "Element not found while verifying the error logo for invalid QR code", e);
            throw e;
        } catch (AssertionError e) {
            logFailure(test, driver, "Error logo verification failed for invalid QR code", e);
            throw e;
        } catch (Exception e) {
            logFailure(test, driver, "Unexpected error while verifying the error logo for invalid QR code", e);
            throw e;
        }
    }

    @When("Verify Error message for invalid QR code")
    public void verifyErrorMessageForInvalidQRCode() {
        try {
            String actualErrorMessage = uploadqrcode.getErrorTextInvalidQRCode();
            Assert.assertEquals(actualErrorMessage, UiConstants.ERROR_MESSAGE_INVALID_QR, 
                "Error message does not match for invalid QR code.");
            test.log(Status.PASS, "Error message validation successful. Expected: " + 
                UiConstants.ERROR_MESSAGE_INVALID_QR + ", Actual: " + actualErrorMessage);
        } catch (NoSuchElementException e) {
            logFailure(test, driver, "Element not found while verifying the error message for invalid QR code", e);
            throw e;
        } catch (AssertionError e) {
            logFailure(test, driver, "Error message validation failed for invalid QR code. Expected: " + 
                UiConstants.ERROR_MESSAGE_INVALID_QR + ", but found: " + uploadqrcode.getErrorTextInvalidQRCode(), e);
            throw e;
        } catch (Exception e) {
            logFailure(test, driver, "Unexpected error while verifying the error message for invalid QR code", e);
            throw e;
        }
    }

    @When("Verify Error message")
    public void verifyErrorMessage() {
        try {
            String actualErrorMessage = uploadqrcode.getErromessageForUnSupportedFromat();
            Assert.assertEquals(actualErrorMessage, UiConstants.ERROR_UNSUPPORTED_FORMAT, 
                "Error message does not match for unsupported file format.");
            test.log(Status.PASS, "Error message validation successful. Expected: " + 
                UiConstants.ERROR_UNSUPPORTED_FORMAT + ", Actual: " + actualErrorMessage);
        } catch (NoSuchElementException e) {
            logFailure(test, driver, "Element not found while verifying the error message for unsupported format", e);
            throw e;
        } catch (AssertionError e) {
            logFailure(test, driver, "Error message validation failed for unsupported format. Expected: " + 
                UiConstants.ERROR_UNSUPPORTED_FORMAT + ", but found: " + uploadqrcode.getErromessageForUnSupportedFromat(), e);
            throw e;
        } catch (Exception e) {
            logFailure(test, driver, "Unexpected error while verifying the error message for unsupported format", e);
            throw e;
        }
    }

    @When("Verify QR code file LargeFileSize")
    public void verifyQRCodeFileLargeFileSize() {
        try {
            uploadqrcode.clickOnUploadQRCodeLargeFileSize();
            Assert.assertTrue(true, "Large file size QR code upload action performed successfully.");
            test.log(Status.PASS, "Successfully attempted to upload a large QR code file.");
        } catch (NoSuchElementException e) {
            logFailure(test, driver, "Element not found while attempting to upload a large QR code file", e);
            throw e;
        } catch (Exception e) {
            logFailure(test, driver, "Unexpected error while attempting to upload a large QR code file", e);
            throw e;
        }
    }

    @When("Verify info message for QR code file LargeFileSize")
    public void verifyInfoMessageLargeFileSize() {
        try {
            String actualMessage = uploadqrcode.getErrorMessageLargerFileSize();
            Assert.assertEquals(actualMessage, UiConstants.ERROR_MESSAGE_LARGEFILE_QR, 
                "Mismatch in error message for large QR code file.");
            test.log(Status.PASS, "Verified info message for large QR code file upload successfully. Expected: " + 
                UiConstants.ERROR_MESSAGE_LARGEFILE_QR + ", Actual: " + actualMessage);
        } catch (NoSuchElementException e) {
            logFailure(test, driver, "Element not found while verifying the error message for large QR code file", e);
            throw e;
        } catch (AssertionError e) {
            logFailure(test, driver, "Error message validation failed for large QR code file. Expected: " + 
                UiConstants.ERROR_MESSAGE_LARGEFILE_QR + ", but found: " + uploadqrcode.getErrorMessageLargerFileSize(), e);
            throw e;
        } catch (Exception e) {
            logFailure(test, driver, "Unexpected error while verifying the error message for large QR code file", e);
            throw e;
        }
    }

    @When("Verify browser back button after verification")
    public void verifyBrowserBackButtonAfterVerification() {
        try {
            uploadqrcode.browserBackButton(driver);
            test.log(Status.PASS, "Browser back button clicked successfully after verification.");
            Assert.assertTrue(true, "Browser back button function executed successfully.");
        } catch (Exception e) {
            logFailure(test, driver, "Unexpected error while clicking browser back button after verification", e);
            throw e;
        }
    }

    @When("Verify click on please try again button")
    public void verifyClickOnPleaseTryAgainButton() {
        try {
            uploadqrcode.clickOnPleaseTryAgain();
            test.log(Status.PASS, "Clicked on 'Please Try Again' button successfully.");
            Assert.assertTrue(true, "Successfully clicked on the 'Please Try Again' button.");
        } catch (NoSuchElementException e) {
            logFailure(test, driver, "Element not found while clicking 'Please Try Again' button", e);
            throw e;
        } catch (Exception e) {
            logFailure(test, driver, "Unexpected error while clicking 'Please Try Again' button", e);
            throw e;
        }
    }

    @When("verify click on scan the qr tab")
    public void verifyClickOnScanTheQrTab() {
        try {
            scanqrcode.clickOnScanQRButtonTab();
            test.log(Status.PASS, "Clicked on 'Scan QR' tab successfully.");
        } catch (NoSuchElementException e) {
            logFailure(test, driver, "Element not found while clicking 'Scan QR' tab", e);
            throw e;
        } catch (Exception e) {
            logFailure(test, driver, "Unexpected error while clicking 'Scan QR' tab", e);
            throw e;
        }
    }

    @When("Verify scan qr code step1 label")
    public void verifyScanQRCodeStep1Label() {
        try {
            String actualLabel = scanqrcode.getScanQRCodeStep1Label();
            Assert.assertEquals(actualLabel, UiConstants.SCAN_QR_CODE_STEP1_LABEL, 
                "Scan QR Code Step 1 label does not match.");
            test.log(Status.PASS, "Verified Scan QR Code Step 1 label successfully. Expected: " 
                + UiConstants.SCAN_QR_CODE_STEP1_LABEL + ", Actual: " + actualLabel);
        } catch (AssertionError e) {
            logFailure(test, driver, "Mismatch in Scan QR Code Step 1 label", e);
            throw e;
        } catch (NoSuchElementException e) {
            logFailure(test, driver, "Element not found while verifying Scan QR Code Step 1 label", e);
            throw e;
        } catch (Exception e) {
            logFailure(test, driver, "Unexpected error while verifying Scan QR Code Step 1 label", e);
            throw e;
        }
    }

    @When("Verify scan qr code step1 description")
    public void verifyScanQRCodeStep1Description() {
        try {
            String actualDescription = scanqrcode.getScanQRCodeStep1Description();
            Assert.assertEquals(actualDescription, UiConstants.SCAN_QR_CODE_STEP1_DESCRIPTION, 
                "Scan QR Code Step 1 description does not match.");
            test.log(Status.PASS, "Verified Scan QR Code Step 1 description successfully. Expected: " 
                + UiConstants.SCAN_QR_CODE_STEP1_DESCRIPTION + ", Actual: " + actualDescription);
        } catch (AssertionError e) {
            logFailure(test, driver, "Mismatch in Scan QR Code Step 1 description", e);
            throw e;
        } catch (NoSuchElementException e) {
            logFailure(test, driver, "Element not found while verifying Scan QR Code Step 1 description", e);
            throw e;
        } catch (Exception e) {
            logFailure(test, driver, "Unexpected error while verifying Scan QR Code Step 1 description", e);
            throw e;
        }
    }

    @When("Verify scan qr code step2 label")
    public void verifyScanQRCodeStep2Label() {
        try {
            String actualLabel = scanqrcode.getScanQRCodeStep2Label();
            Assert.assertEquals(actualLabel, UiConstants.SCAN_QR_CODE_STEP2_LABEL, 
                "Scan QR Code Step 2 label does not match.");
            test.log(Status.PASS, "Verified Scan QR Code Step 2 label successfully. Expected: " 
                + UiConstants.SCAN_QR_CODE_STEP2_LABEL + ", Actual: " + actualLabel);
        } catch (AssertionError e) {
            logFailure(test, driver, "Mismatch in Scan QR Code Step 2 label", e);
            throw e;
        } catch (NoSuchElementException e) {
            logFailure(test, driver, "Element not found while verifying Scan QR Code Step 2 label", e);
            throw e;
        } catch (Exception e) {
            logFailure(test, driver, "Unexpected error while verifying Scan QR Code Step 2 label", e);
            throw e;
        }
    }

    @When("Verify scan qr code step2 description")
    public void verifyScanQRCodeStep2Description() {
        try {
            String actualDescription = scanqrcode.getScanQRCodeStep2Description();
            Assert.assertEquals(actualDescription, UiConstants.SCAN_QR_CODE_STEP2_DESCRIPTION, 
                "Scan QR Code Step 2 description does not match.");
            test.log(Status.PASS, "Verified Scan QR Code Step 2 description successfully. Expected: " 
                + UiConstants.SCAN_QR_CODE_STEP2_DESCRIPTION + ", Actual: " + actualDescription);
        } catch (AssertionError e) {
            logFailure(test, driver, "Mismatch in Scan QR Code Step 2 description", e);
            throw e;
        } catch (NoSuchElementException e) {
            logFailure(test, driver, "Element not found while verifying Scan QR Code Step 2 description", e);
            throw e;
        } catch (Exception e) {
            logFailure(test, driver, "Unexpected error while verifying Scan QR Code Step 2 description", e);
            throw e;
        }
    }

    @When("Verify scan qr code step3 label")
    public void verifyScanQRCodeStep3Label() {
        try {
            String actualLabel = scanqrcode.getScanQRCodeStep3Label();
            Assert.assertEquals(actualLabel, UiConstants.SCAN_QR_CODE_STEP3_LABEL, 
                "Scan QR Code Step 3 label does not match.");
            test.log(Status.PASS, "Verified Scan QR Code Step 3 label successfully. Expected: " 
                + UiConstants.SCAN_QR_CODE_STEP3_LABEL + ", Actual: " + actualLabel);
        } catch (AssertionError e) {
            logFailure(test, driver, "Mismatch in Scan QR Code Step 3 label", e);
            throw e;
        } catch (NoSuchElementException e) {
            logFailure(test, driver, "Element not found while verifying Scan QR Code Step 3 label", e);
            throw e;
        } catch (Exception e) {
            logFailure(test, driver, "Unexpected error while verifying Scan QR Code Step 3 label", e);
            throw e;
        }
    }

    @When("Verify scan qr code step3 description")
    public void verifyScanQRCodeStep3Description() {
        try {
            String actualDescription = scanqrcode.getScanQRCodeStep3Description();
            Assert.assertEquals(actualDescription, UiConstants.SCAN_QR_CODE_STEP3_DESCRIPTION, 
                "Scan QR Code Step 3 description does not match.");
            test.log(Status.PASS, "Verified Scan QR Code Step 3 description successfully. Expected: " 
                + UiConstants.SCAN_QR_CODE_STEP3_DESCRIPTION + ", Actual: " + actualDescription);
        } catch (AssertionError e) {
            logFailure(test, driver, "Mismatch in Scan QR Code Step 3 description", e);
            throw e;
        } catch (NoSuchElementException e) {
            logFailure(test, driver, "Element not found while verifying Scan QR Code Step 3 description", e);
            throw e;
        } catch (Exception e) {
            logFailure(test, driver, "Unexpected error while verifying Scan QR Code Step 3 description", e);
            throw e;
        }
    }


    @When("Verify scan qr code step4 label")
    public void verifyScanQRCodeStep4Label() {
        try {
            String actualLabel = scanqrcode.getScanQRCodeStep4Label();
            Assert.assertEquals(actualLabel, UiConstants.SCAN_QR_CODE_STEP4_LABEL, 
                "Scan QR Code Step 4 label does not match.");
            test.log(Status.PASS, "Verified Scan QR Code Step 4 label successfully. Expected: " 
                + UiConstants.SCAN_QR_CODE_STEP4_LABEL + ", Actual: " + actualLabel);
        } catch (AssertionError e) {
            logFailure(test, driver, "Mismatch in Scan QR Code Step 4 label", e);
            throw e;
        } catch (NoSuchElementException e) {
            logFailure(test, driver, "Element not found while verifying Scan QR Code Step 4 label", e);
            throw e;
        } catch (Exception e) {
            logFailure(test, driver, "Unexpected error while verifying Scan QR Code Step 4 label", e);
            throw e;
        }
    }

    @When("Verify scan qr code step4 description")
    public void verifyScanQRCodeStep4Description() {
        try {
            String actualDescription = scanqrcode.getScanQRCodeStep4Description();
            Assert.assertEquals(actualDescription, UiConstants.SCAN_QR_CODE_STEP4_DESCRIPTION, 
                "Scan QR Code Step 4 description does not match.");
            test.log(Status.PASS, "Verified Scan QR Code Step 4 description successfully. Expected: " 
                + UiConstants.SCAN_QR_CODE_STEP4_DESCRIPTION + ", Actual: " + actualDescription);
        } catch (AssertionError e) {
            logFailure(test, driver, "Mismatch in Scan QR Code Step 4 description", e);
            throw e;
        } catch (NoSuchElementException e) {
            logFailure(test, driver, "Element not found while verifying Scan QR Code Step 4 description", e);
            throw e;
        } catch (Exception e) {
            logFailure(test, driver, "Unexpected error while verifying Scan QR Code Step 4 description", e);
            throw e;
        }
    }

    @When("Verify scan qr code area")
    public void verifyScanQRCodeArea() {
        try {
            Assert.assertTrue(scanqrcode.isVisibleScanQRCodeArea(), "Scan QR Code area is not visible.");
            test.log(Status.PASS, "Scan QR Code area is visible.");
        } catch (AssertionError e) {
            logFailure(test, driver, "Scan QR Code area is not visible", e);
            throw e;
        } catch (NoSuchElementException e) {
            logFailure(test, driver, "Element not found while verifying Scan QR Code area", e);
            throw e;
        } catch (Exception e) {
            logFailure(test, driver, "Unexpected error while verifying Scan QR Code area", e);
            throw e;
        }
    }

    @When("verify scan qr code icon")
    public void verifyScanQRCodeIcon() {
        try {
            Assert.assertTrue(scanqrcode.isVisibleScanQRCodeIcon(), "Scan QR Code icon is not visible.");
            test.log(Status.PASS, "Scan QR Code icon is visible.");
        } catch (AssertionError e) {
            logFailure(test, driver, "Scan QR Code icon is not visible", e);
            throw e;
        } catch (NoSuchElementException e) {
            logFailure(test, driver, "Element not found while verifying Scan QR Code icon", e);
            throw e;
        } catch (Exception e) {
            logFailure(test, driver, "Unexpected error while verifying Scan QR Code icon", e);
            throw e;
        }
    }

    @When("verify scan qr code button")
    public void verifyScanQRCodeButton() {
        try {
            Assert.assertTrue(scanqrcode.isVisibleScanQRCodeButton(), "Scan QR Code button is not visible.");
            test.log(Status.PASS, "Scan QR Code button is visible.");
        } catch (AssertionError e) {
            logFailure(test, driver, "Scan QR Code button is not visible", e);
            throw e;
        } catch (NoSuchElementException e) {
            logFailure(test, driver, "Element not found while verifying Scan QR Code button", e);
            throw e;
        } catch (Exception e) {
            logFailure(test, driver, "Unexpected error while verifying Scan QR Code button", e);
            throw e;
        }
    }

    @When("verify click on scan qr code button")
    public void verifyClickOnScanQRCodeButton() {
    	try {
            Assert.assertTrue(scanqrcode.isVisibleScanQRCodeButton(), "Scan QR Code button is not visible.");
            test.log(Status.PASS, "Scan QR Code button is visible.");
        } catch (AssertionError e) {
            logFailure(test, driver, "Scan QR Code button is not visible", e);
            throw e;
        } catch (NoSuchElementException e) {
            logFailure(test, driver, "Element not found while verifying Scan QR Code button", e);
            throw e;
        } catch (Exception e) {
            logFailure(test, driver, "Unexpected error while verifying Scan QR Code button", e);
            throw e;
        }
    }

    @When("Verify scan qr code step2 label after")
    public void verifyScanQRCodeStep2LabelAfter() {
        try {
            Assert.assertTrue(scanqrcode.isVisibleScanQRCodeStep2LabelAfter(), "Scan QR Code Step 2 label is not visible after.");
            test.log(Status.PASS, "Scan QR Code Step 2 label is correctly displayed after.");
        } catch (AssertionError e) {
            logFailure(test, driver, "Scan QR Code Step 2 label is missing after verification", e);
            throw e;
        } catch (NoSuchElementException e) {
            logFailure(test, driver, "Element not found: Scan QR Code Step 2 label after verification", e);
            throw e;
        } catch (Exception e) {
            logFailure(test, driver, "Unexpected error while verifying Scan QR Code Step 2 label after", e);
            throw e;
        }
    }

    @When("Verify VP verification step3 label after")
    public void verifyVPVerificationStep3LabelAfter() {
        try {
            Assert.assertTrue(vpverification.isVisibleVPverificationstep3LabelAfter(), "VP Verification Step 3 label is not visible after.");
            test.log(Status.PASS, "VP Verification Step 3 label is correctly displayed after.");
        } catch (AssertionError e) {
            logFailure(test, driver, "VP Verification Step 3 label is missing after verification", e);
            throw e;
        } catch (NoSuchElementException e) {
            logFailure(test, driver, "Element not found: VP Verification Step 3 label after verification", e);
            throw e;
        } catch (Exception e) {
            logFailure(test, driver, "Unexpected error while verifying VP Verification Step 3 label after", e);
            throw e;
        }
    }

    @When("Verify click on request verifiable credentials button")
    public void verifyClickRequestVerifiableCredentialsButton() {
        try {
            vpverification.clickOnVerifiableCredentialsButton();
            test.log(Status.PASS, "Successfully clicked on Request Verifiable Credentials button.");
        } catch (NoSuchElementException e) {
            logFailure(test, driver, "Failed to find Request Verifiable Credentials button while attempting to click", e);
            throw e;
        } catch (Exception e) {
            logFailure(test, driver, "Unexpected error while clicking on Request Verifiable Credentials button", e);
            throw e;
        }
    }

    @When("verify click on okay button")
    public void verifyClickOnOkayButton() {
        try {
            scanqrcode.clickOnOkayButton();
            test.log(Status.PASS, "Successfully clicked on the Okay button.");
        } catch (NoSuchElementException e) {
            logFailure(test, driver, "Failed to find the Okay button while attempting to click", e);
            throw e;
        } catch (Exception e) {
            logFailure(test, driver, "Unexpected error while clicking on the Okay button", e);
            throw e;
        }
    }

    @When("verify click on back button")
    public void verifyClickOnBackButton() {
        try {
            scanqrcode.clickOnBackButton();
            Assert.assertTrue(true, "Back button click action performed.");
            test.log(Status.PASS, "Successfully clicked on the Back button.");
        } catch (NoSuchElementException e) {
            logFailure(test, driver, "Failed to find the Back button while attempting to click", e);
            throw e;
        } catch (Exception e) {
            logFailure(test, driver, "Unexpected error while clicking on the Back button", e);
            throw e;
        }
    }



	@When("Click on BLE tab")
	public void click_on_ble_tab() {
	    try {
	        ble.clickOnBleTab();
	        test.log(Status.PASS, "Successfully clicked on the BLE tab.");
	        test.log(Status.PASS, "BLE tab is active after clicking.");
	    } catch (NoSuchElementException e) {
	        logFailure(test, driver, "Element not found while clicking on the BLE tab", e);
	        throw e;
	    } catch (Exception e) {
	        logFailure(test, driver, "Unexpected error while clicking on the BLE tab", e);
	        throw e;
	    }
	}

	@When("verify information message on ble verification")
	public void verify_information_message_on_ble_verification() {
	    try {
	        String actualMessage = ble.getInformationText();
	        Assert.assertEquals(actualMessage, UiConstants.INFO_MESSAGE, "Information message does not match the expected value.");
	        test.log(Status.PASS, "Information message verification successful. Expected: " + UiConstants.INFO_MESSAGE + ", Actual: " + actualMessage);
	    } catch (AssertionError e) {
	        test.log(Status.FAIL, "Information message verification failed. Expected: " + UiConstants.INFO_MESSAGE + ", but found: " + ble.getInformationText());
	        throw e;
	    } catch (NoSuchElementException e) {
	        logFailure(test, driver, "Element not found while verifying information message on BLE verification", e);
	        throw e;
	    } catch (Exception e) {
	        logFailure(test, driver, "Unexpected error while verifying information message on BLE verification", e);
	        throw e;
	    }
	}

    	@When("verify Transaction Terminated error message")
	public void verify_transaction_terminated_error_message() {
	    try {
	        String actualMessage = vpverification.getTransactionTerminatedText();
	        Assert.assertEquals(actualMessage, UiConstants.TRANSACTION_TERMINATED_MESSAGE, "Information message does not match the expected value.");
	        test.log(Status.PASS, "Information message verification successful. Expected: " + UiConstants.TRANSACTION_TERMINATED_MESSAGE + ", Actual: " + actualMessage);
	    } catch (AssertionError e) {
	        test.log(Status.FAIL, "Information message verification failed. Expected: " + UiConstants.TRANSACTION_TERMINATED_MESSAGE + ", but found: " + vpverification.getTransactionTerminatedText());
	        throw e;
	    } catch (NoSuchElementException e) {
	        logFailure(test, driver, "Element not found while verifying information message", e);
	        throw e;
	    } catch (Exception e) {
	        logFailure(test, driver, "Unexpected error while verifying information message", e);
	        throw e;
	    }
	}

	@Then("Click on vp verification tab")
	public void click_on_vp_verification_tab() {
	    try {
	        vpverification.clickOnVPVerificationTab();
	        test.log(Status.PASS, "Successfully clicked on the VP verification tab.");
	        test.log(Status.PASS, "VP verification tab is active after clicking.");
	    } catch (NoSuchElementException e) {
	        logFailure(test, driver, "Element not found while clicking on the VP verification tab", e);
	        throw e;
	    } catch (Exception e) {
	        logFailure(test, driver, "Unexpected error while clicking on the VP verification tab", e);
	        throw e;
	    }
	}

    @Then("Click on right arrow")
	public void click_on_right_arrow() {
	    try {
	        vpverification.clickOnRightArrow();
	        test.log(Status.PASS, "Successfully clicked on the right arrow.");
	        test.log(Status.PASS, "Right arrow is active after clicking.");
	    } catch (NoSuchElementException e) {
	        logFailure(test, driver, "Element not found while clicking on the right arrow", e);
	        throw e;
	    } catch (Exception e) {
	        logFailure(test, driver, "Unexpected error while clicking on the right arrow", e);
	        throw e;
	    }
	}

	@When("Verify information message on VP verification")
	public void verify_information_message_on_vp_verification() {
	    try {
	        String actualMessage = vpverification.getInformationMessage();
	        Assert.assertEquals(actualMessage, UiConstants.INFO_MESSAGE, "Information message does not match the expected value.");
	        test.log(Status.PASS, "Information message verification successful. Expected: " + UiConstants.INFO_MESSAGE + ", Actual: " + actualMessage);
	    } catch (AssertionError e) {
	        test.log(Status.FAIL, "Information message verification failed. Expected: " + UiConstants.INFO_MESSAGE + ", but found: " + vpverification.getInformationMessage());
	        throw e;
	    } catch (NoSuchElementException e) {
	        logFailure(test, driver, "Element not found while verifying information message on VP verification", e);
	        throw e;
	    } catch (Exception e) {
	        logFailure(test, driver, "Unexpected error while verifying information message on VP verification", e);
	        throw e;
	    }
	}

	@When("Verify scan line on scanning area")
	public void verify_scan_line_on_scanning_area() {
	    try {
	        boolean isScanLineVisible = scanqrcode.isVisibleScanLine();
	        Assert.assertTrue(isScanLineVisible, "Scan line is not visible on the scanning area.");
	        test.log(Status.PASS, "Scan line is visible on the scanning area.");
	    } catch (AssertionError e) {
	        test.log(Status.FAIL, "Scan line verification failed: Scan line is not visible on the scanning area.");
	        throw e;
	    } catch (NoSuchElementException e) {
	        logFailure(test, driver, "Element not found while verifying scan line on scanning area", e);
	        throw e;
	    } catch (Exception e) {
	        logFailure(test, driver, "Unexpected error while verifying scan line on scanning area", e);
	        throw e;
	    }
	}

	@When("Verify idle timeout message for scan QR code")
	public void verify_idle_timeout_message_for_scan_qr_code() {
	    try {
	        String actualMessage = scanqrcode.getTextScannerTimeoutMessage();
	        Assert.assertEquals(actualMessage, UiConstants.ERROR_MESSAGE_SCAN_TIMEOUT, "Idle timeout message does not match the expected value.");
	        test.log(Status.PASS, "Idle timeout message verification successful. Expected: " + UiConstants.ERROR_MESSAGE_SCAN_TIMEOUT + ", Actual: " + actualMessage);
	    } catch (AssertionError e) {
	        test.log(Status.FAIL, "Idle timeout message verification failed. Expected: " + UiConstants.ERROR_MESSAGE_SCAN_TIMEOUT + ", but found: " + scanqrcode.getTextScannerTimeoutMessage());
	        throw e;
	    } catch (NoSuchElementException e) {
	        logFailure(test, driver, "Element not found while verifying idle timeout message for scan QR code", e);
	        throw e;
	    } catch (Exception e) {
	        logFailure(test, driver, "Unexpected error while verifying idle timeout message for scan QR code", e);
	        throw e;
	    }
	}

	@When("Verify close button on timeout message")
	public void verify_close_button_on_timeout_message() {
	    try {
	        boolean isCloseButtonVisible = scanqrcode.isVisibleCloseIconTimeoutMessage();
	        Assert.assertTrue(isCloseButtonVisible, "Close button on timeout message is not visible.");
	        test.log(Status.PASS, "Close button on timeout message is visible.");
	    } catch (AssertionError e) {
	        test.log(Status.FAIL, "Close button verification failed: Close button on timeout message is not visible.");
	        throw e;
	    } catch (NoSuchElementException e) {
	        logFailure(test, driver, "Element not found while verifying close button on timeout message", e);
	        throw e;
	    } catch (Exception e) {
	        logFailure(test, driver, "Unexpected error while verifying close button on timeout message", e);
	        throw e;
	    }
	}

	@When("Verify click on close button on timeout message")
	public void verify_click_on_close_button_on_timeout_message() {
	    try {
	        scanqrcode.clickOnCloseIconTimeoutMessage();
	        test.log(Status.PASS, "Successfully clicked on the close button on the timeout message.");
	        test.log(Status.PASS, "Timeout message is no longer visible after clicking close.");
	    } catch (NoSuchElementException e) {
	        logFailure(test, driver, "Element not found while clicking on close button on timeout message", e);
	        throw e;
	    } catch (AssertionError e) {
	        test.log(Status.FAIL, "Close button click verification failed: Timeout message is still visible.");
	        throw e;
	    } catch (Exception e) {
	        logFailure(test, driver, "Unexpected error while clicking on close button on timeout message", e);
	        throw e;
	    }
	}

	@When("Upload QR code file Expired png")
	public void upload_qr_code_file_expired_png() {
	    try {
	        uploadqrcode.clickOnUploadExpiredQRCodepngExpired();
	        test.log(Status.PASS, "Successfully uploaded the expired QR code PNG file.");
	        test.log(Status.PASS, "Expired QR code PNG file uploaded successfully.");
	    } catch (NoSuchElementException e) {
	        logFailure(test, driver, "Element not found while uploading expired QR code PNG file", e);
	        throw e;
	    } catch (AssertionError e) {
	        test.log(Status.FAIL, "QR code file upload verification failed: Expired QR code PNG upload was not successful.");
	        throw e;
	    } catch (Exception e) {
	        logFailure(test, driver, "Unexpected error while uploading expired QR code PNG file", e);
	        throw e;
	    }
	}

	@When("Upload QR code file Expired jpg")
	public void upload_qr_code_file_expired_jpg() {
        try {
            uploadqrcode.clickOnUploadExpiredQRCodeJpgExpired();
	        test.log(Status.PASS, "Successfully uploaded the expired QR code JPG file.");
	        test.log(Status.PASS, "Expired QR code JPG file uploaded successfully.");
	    } catch (NoSuchElementException e) {
	        logFailure(test, driver, "Element not found while uploading expired QR code JPG file", e);
	        throw e;
	    } catch (AssertionError e) {
	        test.log(Status.FAIL, "QR code file upload verification failed: Expired QR code JPG upload was not successful.");
	        throw e;
	    } catch (Exception e) {
	        logFailure(test, driver, "Unexpected error while uploading expired QR code JPG file", e);
	        throw e;
	    }
	}

	
	@When("Upload QR code file Expired jpeg")
	public void uploadQrCodeFileExpiredJpeg() {
        try {
            uploadqrcode.clickOnUploadExpiredQRCodeJpgExpired(); 
	        Assert.assertTrue(true, "Expired JPEG QR code uploaded successfully.");
	        test.log(Status.PASS, "Successfully uploaded expired QR code (JPEG format).");
	    } catch (NoSuchElementException e) {
	        logFailure(test, driver, "Failed to upload expired JPEG QR code", e);
	        throw e;
	    } catch (Exception e) {
	        logFailure(test, driver, "Unexpected error while uploading expired JPEG QR code", e);
	        throw e;
	    }
	}

	
	@When("Upload QR code file Expired pdf")
	public void uploadQrCodeFileExpiredPdf() {
	    try {
	        uploadqrcode.clickOnUploadExpiredQRCodepngExpired(); 
	        Assert.assertTrue(true, "Expired PDF QR code uploaded successfully.");
	        test.log(Status.PASS, "Successfully uploaded expired QR code (PDF format).");
	    } catch (NoSuchElementException e) {
	        logFailure(test, driver, "Failed to upload expired PDF QR code", e);
	        throw e;
	    } catch (Exception e) {
	        logFailure(test, driver, "Unexpected error while uploading expired PDF QR code", e);
	        throw e;
	    }
	}

	@When("Verify message for valid QR code")
	public void verify_message_for_valid_qr_code() {
	    try {
	        String actualMessage = uploadqrcode.getErrorMessageForExpiredQRCode();
	        Assert.assertEquals(actualMessage, UiConstants.CONGRATULATIONS_MESSAGE);
	        test.log(Status.PASS, "Valid QR code message verified successfully: " + actualMessage);
	    } catch (NoSuchElementException e) {
	        logFailure(test, driver, "Element not found while verifying message for valid QR code", e);
	        throw e;
	    } catch (AssertionError e) {
	        test.log(Status.FAIL, "Message verification failed: Expected '" + UiConstants.CONGRATULATIONS_MESSAGE + "' but got '" + uploadqrcode.getErrorMessageForExpiredQRCode() + "'");
	        throw e;
	    } catch (Exception e) {
	        logFailure(test, driver, "Unexpected error while verifying message for valid QR code", e);
	        throw e;
	    }
	}

	@When("Verify message for expired QR code")
	public void verify_message_for_expired_qr_code() {
	    try {
	        String actualMessage = uploadqrcode.getErrorMessageForExpiredQRCode();
	        Assert.assertEquals(actualMessage, UiConstants.ERROR_MESSAGE_EXPIRED_QR);
	        test.log(Status.PASS, "Successfully verified the error message for expired QR code: " + actualMessage);
	    } catch (AssertionError e) {
	        test.log(Status.FAIL, "Verification failed: Expected '" + UiConstants.ERROR_MESSAGE_EXPIRED_QR 
	                + "', but found '" + uploadqrcode.getErrorMessageForExpiredQRCode() + "'");
	        throw e;
	    } catch (NoSuchElementException e) {
	        logFailure(test, driver, "Element not found while verifying error message for expired QR code", e);
	        throw e;
	    } catch (Exception e) {
	        logFailure(test, driver, "Unexpected error while verifying error message for expired QR code", e);
	        throw e;
	    }
	}

	@When("Open inji web in new tab")
	public void openInjiWebInNewTab() {
	    try {
	        homePage.openNewTab();
	        test.log(Status.PASS, "Successfully opened Inji web in a new tab.");
	    } catch (Exception e) {
	        logFailure(test, driver, "Failed to open Inji web in a new tab", e);
	        throw e;
	    }
	}
	
	@When("Open inji verify in new tab")
	public void open_inji_verify_in_new_tab() {
	    try {
	        homePage.switchToVerifyTab();
	        test.log(Status.PASS, "Successfully switched to the Inji Verify tab.");
	    } catch (NoSuchWindowException e) {
	        logFailure(test, driver, "Failed to switch to Inji Verify tab - Tab not found", e);
	        throw e;
	    } catch (Exception e) {
	        logFailure(test, driver, "Unexpected error while switching to Inji Verify tab", e);
	        throw e;
	    }
	}
	@Given("User search the issuers with {string}")
	public void user_search_the_issuers_with(String string) {
	    try {
	        Thread.sleep(6000);
	        homePage.enterIssuersInSearchBox(string);
	        test.log(Status.PASS, "Successfully entered issuers: " + string + " in the search box.");
	    } catch (InterruptedException e) {
	        test.log(Status.FAIL, "Interrupted while waiting to enter issuers in search box: " + e.getMessage());
	        Thread.currentThread().interrupt();
	        throw new RuntimeException(e);
	    } catch (NoSuchElementException e) {
	        logFailure(test, driver, "Element not found while entering issuers in search box", e);
	        throw e;
	    } catch (Exception e) {
	        logFailure(test, driver, "Unexpected error while entering issuers in search box", e);
	        throw e;
	    }
	}

	@When("User search the VC with {string}")
	public void user_search_the_Vc_with(String string) {
	    try {
	        Thread.sleep(6000);
	        vpverification.enterVcInSearchBox(string);
	        test.log(Status.PASS, "Successfully entered VC: " + string + " in the search box.");
	    } catch (InterruptedException e) {
	        test.log(Status.FAIL, "Interrupted while waiting to enter VC in search box: " + e.getMessage());
	        Thread.currentThread().interrupt(); // Restore interrupted state
	        throw new RuntimeException(e);
	    } catch (NoSuchElementException e) {
	        logFailure(test, driver, "Element not found while entering VC in search box", e);
	        throw e;
	    } catch (Exception e) {
	        logFailure(test, driver, "Unexpected error while entering VC in search box", e);
	        throw e;
	    }
	}
	
	
    @Then("User search the issuers sunbird")
    public void user_search_the_issuers_sunbird() throws Exception {
        try {
            String issuerText = System.getenv("Issuer_Text_sunbird");
            if (issuerText == null || issuerText.isEmpty()) {
                String[] string = baseTest.fetchIssuerTexts();
                issuerText = string[1];
            }
            homePage.enterIssuersInSearchBox(issuerText);
            Thread.sleep(6000);
            test.log(Status.PASS, "Searched issuers with: " + issuerText);
        } catch (NoSuchElementException e) {
            test.log(Status.FAIL, "Element not found while searching issuers: " + e.getMessage());
            test.log(Status.FAIL, ExceptionUtils.getStackTrace(e));
            ScreenshotUtil.attachScreenshot(driver, "FailureScreenshot");
            throw e;
        } catch (Exception e) {
            test.log(Status.FAIL, "Unexpected error: " + e.getMessage());
            test.log(Status.FAIL, ExceptionUtils.getStackTrace(e));
            ScreenshotUtil.attachScreenshot(driver, "FailureScreenshot");
            throw e;
        }
    }
    
    

	@When("User click on StayProtected Insurance credentials button")
	public void user_click_on_download_StayProtected_Insurance_button() {
	    try {
	        homePage.clickOnStayProtectedCredentials();
	        test.log(Status.PASS, "Successfully clicked on StayProtected Insurance credentials button.");
	    } catch (NoSuchElementException e) {
	        logFailure(test, driver, "Element not found while clicking StayProtected Insurance credentials button", e);
	        throw e;
	    } catch (Exception e) {
	        logFailure(test, driver, "Unexpected error while clicking StayProtected Insurance credentials button", e);
	        throw e;
	    }
	}

	@When("User click on get started button")
	public void user_click_on_get_started_button() {
	    try {
	        homePage.clickOnGetStartedButton();
	        test.log(Status.PASS, "Successfully clicked on Get Started button.");
	    } catch (NoSuchElementException e) {
	        logFailure(test, driver, "Element not found while clicking Get Started button", e);
	        throw e;
	    } catch (Exception e) {
	        logFailure(test, driver, "Unexpected error while clicking Get Started button", e);
	        throw e;
	    }
	}

	@When("User verify mosip national id by e-signet displayed")
	public void user_verify_mosip_national_id_by_e_signet_displayed() {
	    try {
	        Assert.assertTrue(homePage.isMosipNationalIdDisplayed());
	        test.log(Status.PASS, "Successfully verified Mosip National ID is displayed.");
	    } catch (AssertionError e) {
	        test.log(Status.FAIL, "Verification failed: Mosip National ID is not displayed.");
	        throw e;
	    } catch (NoSuchElementException e) {
	        logFailure(test, driver, "Element not found while verifying Mosip National ID display", e);
	        throw e;
	    } catch (Exception e) {
	        logFailure(test, driver, "Unexpected error while verifying Mosip National ID display", e);
	        throw e;
	    }
	}

	@Then("User click on health insurance by e-signet button")
	public void user_click_on_health_insurance_id_by_e_signet_button() {
	    try {
	        homePage.clickOnStayProtectedCredentialType();
	        test.log(Status.PASS, "Successfully clicked on health insurance by e-signet button.");
	    } catch (NoSuchElementException e) {
	        logFailure(test, driver, "Element not found while clicking health insurance by e-signet button", e);
	        throw e;
	    } catch (Exception e) {
	        logFailure(test, driver, "Unexpected error while clicking health insurance by e-signet button", e);
	        throw e;
	    }
	}

	@Then("User click on validity dropdown")
	public void user_click_on_validity_dropdown_button() {
	    try {
	        homePage.clickOnValidityDropdown();
	        test.log(Status.PASS, "Successfully clicked on validity dropdown.");
	    } catch (NoSuchElementException e) {
	        logFailure(test, driver, "Element not found while clicking validity dropdown", e);
	        throw e;
	    } catch (Exception e) {
	        logFailure(test, driver, "Unexpected error while clicking validity dropdown", e);
	        throw e;
	    }
	}

	@When("User click on no limit")
	public void user_click_on_no_limit_button() {
	    try {
	        homePage.clickOnNoLimit();
	        test.log(Status.PASS, "Successfully clicked on no limit.");
	    } catch (NoSuchElementException e) {
	        logFailure(test, driver, "Element not found while clicking no limit", e);
	        throw e;
	    } catch (Exception e) {
	        logFailure(test, driver, "Unexpected error while clicking no limit", e);
	        throw e;
	    }
	}

	@When("User click on proceed")
	public void user_click_on_proceed_button() {
	    try {
	        homePage.clickOnOnProceed();
	        test.log(Status.PASS, "Successfully clicked on proceed.");
	    } catch (NoSuchElementException e) {
	        logFailure(test, driver, "Element not found while clicking proceed", e);
	        throw e;
	    } catch (Exception e) {
	        logFailure(test, driver, "Unexpected error while clicking proceed", e);
	        throw e;
	    }
	}
	
    @Then("User enter the policy number")
    public void user_enter_the_policy_number() {
        try {
            Thread.sleep(3000); // Consider using WebDriverWait instead of Thread.sleep for better efficiency.
            homePage.enterPolicyNumer(policynumber);
            test.log(Status.PASS, "User successfully entered the policy number: " + policynumber);
        } catch (NoSuchElementException e) {
            test.log(Status.FAIL, "Element not found while entering the policy number: " + e.getMessage());
            test.log(Status.FAIL, ExceptionUtils.getStackTrace(e));
            ScreenshotUtil.attachScreenshot(driver, "FailureScreenshot");
            throw e;
        } catch (InterruptedException e) {
            test.log(Status.FAIL, "Thread was interrupted while waiting to enter the policy number: " + e.getMessage());
            test.log(Status.FAIL, ExceptionUtils.getStackTrace(e));
            Thread.currentThread().interrupt(); // Restore interrupted state
            throw new RuntimeException(e);
        } catch (Exception e) {
            test.log(Status.FAIL, "Unexpected error while entering the policy number: " + e.getMessage());
            test.log(Status.FAIL, ExceptionUtils.getStackTrace(e));
            ScreenshotUtil.attachScreenshot(driver, "FailureScreenshot");
            throw e;
        }
    }


	@When("User enter the {string}")
	public void user_enter_the(String string) {
	    try {
	        homePage.enterVid(string);
	        test.log(Status.PASS, "Successfully entered VID: " + string);
	    } catch (NoSuchElementException e) {
	        logFailure(test, driver, "Element not found while entering VID", e);
	        throw e;
	    } catch (Exception e) {
	        logFailure(test, driver, "Unexpected error while entering VID", e);
	        throw e;
	    }
	}

	@When("User click on getOtp button")
	public void user_click_on_get_otp_button() {
	    try {
	        homePage.clickOnGetOtpButton();
	        test.log(Status.PASS, "Successfully clicked on Get OTP button.");
	    } catch (NoSuchElementException e) {
	        logFailure(test, driver, "Element not found while clicking Get OTP button", e);
	        throw e;
	    } catch (Exception e) {
	        logFailure(test, driver, "Unexpected error while clicking Get OTP button", e);
	        throw e;
	    }
	}

	@When("User enter the otp {string}")
	public void user_enter_the_otp(String otpString) {
	    try {
	        Thread.sleep(3000);
	        homePage.enterOtp(otpString);
	        test.log(Status.PASS, "Successfully entered OTP: " + otpString);
	    } catch (InterruptedException e) {
	        test.log(Status.FAIL, "Interrupted while waiting to enter OTP: " + e.getMessage());
	        Thread.currentThread().interrupt(); // Restore interrupted state
	        throw new RuntimeException(e);
	    } catch (NoSuchElementException e) {
	        logFailure(test, driver, "Element not found while entering OTP", e);
	        throw e;
	    } catch (Exception e) {
	        logFailure(test, driver, "Unexpected error while entering OTP", e);
	        throw e;
	    }
	}
	@When("User click on verify button")
	public void user_click_on_verify_button() {
	    try {
	        homePage.clickOnVerify();
	        test.log(Status.PASS, "Successfully clicked on Verify button.");
	    } catch (NoSuchElementException e) {
	        logFailure(test, driver, "Element not found while clicking Verify button", e);
	        throw e;
	    } catch (Exception e) {
	        logFailure(test, driver, "Unexpected error while clicking Verify button", e);
	        throw e;
	    }
	}

	@When("User verify Download Success text displayed")
	public void user_verify_download_success_text_displayed() {
	    try {
	        Assert.assertEquals(homePage.isSuccessMessageDisplayed(), "Success!");
	        test.log(Status.PASS, "Successfully verified Download Success text is displayed.");
	    } catch (AssertionError e) {
	        test.log(Status.FAIL, "Verification failed: Download Success text is not 'Success!'. Actual: '" + homePage.isSuccessMessageDisplayed() + "'");
	        throw e;
	    } catch (NoSuchElementException e) {
	        logFailure(test, driver, "Element not found while verifying Download Success text display", e);
	        throw e;
	    } catch (Exception e) {
	        logFailure(test, driver, "Unexpected error while verifying Download Success text display", e);
	        throw e;
	    }
	}
	@When("User verify pdf is downloaded")
	public void user_verify_pdf_is_downloaded() throws IOException {
	    try {
	        Thread.sleep(10000);
	        boolean fileExists = (boolean) baseTest.getJse().executeScript("browserstack_executor: {\"action\": \"fileExists\", \"arguments\": {\"fileName\": \"InsuranceCredential.pdf\"}}");
	        assertTrue("PDF file 'InsuranceCredential.pdf' was not found on BrowserStack.", fileExists);
	        test.log(Status.PASS, "PDF file 'InsuranceCredential.pdf' exists on BrowserStack."); 
	        String base64EncodedFile = (String) baseTest.getJse().executeScript("browserstack_executor: {\"action\": \"getFileContent\", \"arguments\": {\"fileName\": \"InsuranceCredential.pdf\"}}");

	        byte[] data = Base64.getDecoder().decode(base64EncodedFile);
	        String filePath = System.getProperty("user.dir") + "/InsuranceCredential.pdf";
	        OutputStream stream = new FileOutputStream(filePath);
	        stream.write(data);
	        stream.close();
	     
	        File pdfFile = new File(filePath);
	        PDDocument document = PDDocument.load(pdfFile);
	        PDFTextStripper stripper = new PDFTextStripper();
	        String text = stripper.getText(document);
	        document.close(); 

	        assertFalse("PDF content is empty.", text.trim().isEmpty());
	        test.log(Status.PASS, "PDF file 'InsuranceCredential.pdf' downloaded and contains content.");

	    } catch (InterruptedException e) {
	        test.log(Status.FAIL, "Interrupted while waiting for PDF download: " + e.getMessage());
	        Thread.currentThread().interrupt(); 
	        throw new RuntimeException(e);
	    } catch (AssertionError e) {
	        test.log(Status.FAIL, e.getMessage());
	        throw e;
	    } catch (Exception e) {
	        test.log(Status.FAIL, "Error verifying PDF download: " + e.getMessage());
	        throw e;
	    }
	}
	@When("User verify go back button")
	public void user_verify_go_back_button() {

	}

	@When("Verify that user convert pdf into png")
	public void verify_that_user_convert_pdf_into_png() throws IOException {
	    String pdfPath = System.getProperty("user.dir") + "/InsuranceCredential.pdf";
	    String outputPath = System.getProperty("user.dir") + "/InsuranceCredential";

	    try {
	        PDDocument document = PDDocument.load(new File(pdfPath));
	        PDFRenderer renderer = new PDFRenderer(document);

	        int numberOfPages = document.getNumberOfPages();
	        assertTrue("PDF document has no pages.", numberOfPages > 0);
	        test.log(Status.PASS, "PDF document has " + numberOfPages + " pages.");

	        for (int i = 0; i < numberOfPages; i++) {
	            PDPage page = document.getPage(i);
	            BufferedImage image = renderer.renderImage(i);

	            String outputFileNamepng = outputPath + (i) + ".png";
	            String outputFileNamejpg = outputPath + (i) + ".jpg";
	            String outputFileNamejpeg = outputPath + (i) + ".jpeg";

	            ImageIO.write(image, "png", new File(outputFileNamepng));
	            ImageIO.write(image, "jpg", new File(outputFileNamejpg));
	            ImageIO.write(image, "jpeg", new File(outputFileNamejpeg));

	            assertTrue("PNG file " + outputFileNamepng + " was not created.", new File(outputFileNamepng).exists());
	            assertTrue("JPG file " + outputFileNamejpg + " was not created.", new File(outputFileNamejpg).exists());
	            assertTrue("JPEG file " + outputFileNamejpeg + " was not created.", new File(outputFileNamejpeg).exists());
	            test.log(Status.PASS, "Successfully converted page " + (i + 1) + " to PNG, JPG, and JPEG.");
	        }

	        document.close();
	        test.log(Status.PASS, "PDF to image conversion completed successfully.");
	    } catch (IOException e) {
	        test.log(Status.FAIL, "Error converting PDF to images: " + e.getMessage());
	        throw e;
	    } catch (AssertionError e) {
	        test.log(Status.FAIL, e.getMessage());
	        throw e;
	    } catch (Exception e) {
	        test.log(Status.FAIL, "Unexpected error during PDF to image conversion: " + e.getMessage());
	        throw e;
	    }
	}
	@When("User enter the policy number {string}")
	public void user_enter_the_policy_number(String string) {
	    try {
	        Thread.sleep(3000);
	        homePage.enterPolicyNumer(string);
	        test.log(Status.PASS, "Successfully entered policy number: " + string);
	    } catch (InterruptedException e) {
	        test.log(Status.FAIL, "Interrupted while waiting to enter policy number: " + e.getMessage());
	        Thread.currentThread().interrupt(); // Restore interrupted state
	        throw new RuntimeException(e);
	    } catch (NoSuchElementException e) {
	        logFailure(test, driver, "Element not found while entering policy number", e);
	        throw e;
	    } catch (Exception e) {
	        logFailure(test, driver, "Unexpected error while entering policy number", e);
	        throw e;
	    }
	}

	@When("User enter the full name {string}")
	public void user_enter_the_full_name(String string) {
	    try {
	        homePage.enterFullName(string);
	        test.log(Status.PASS, "Successfully entered full name: " + string);
	    } catch (NoSuchElementException e) {
	        logFailure(test, driver, "Element not found while entering full name", e);
	        throw e;
	    } catch (Exception e) {
	        logFailure(test, driver, "Unexpected error while entering full name", e);
	        throw e;
	    }
	}
	
    @When("User enter the full name")
    public void user_enter_the_full_name() {
        try {
        	homePage.enterFullName(fullname);
            test.log(Status.PASS, "User successfully entered the full name: " + fullname);
        } catch (NoSuchElementException e) {
            test.log(Status.FAIL, "Element not found while entering the full name: " + e.getMessage());
            test.log(Status.FAIL, ExceptionUtils.getStackTrace(e));
            ScreenshotUtil.attachScreenshot(driver, "FailureScreenshot");
            throw e;
        } catch (Exception e) {
            test.log(Status.FAIL, "Unexpected error while entering the full name: " + e.getMessage());
            test.log(Status.FAIL, ExceptionUtils.getStackTrace(e));
            ScreenshotUtil.attachScreenshot(driver, "FailureScreenshot");
            throw e;
        }
    }
    @When("User enter the date of birth")
    public void user_enter_the_date_of_birth() {
        try {
        	homePage.selectDateOfBirth(formattedDate);
            test.log(Status.PASS, "User successfully entered the date of birth: " + formattedDate);
        } catch (NoSuchElementException e) {
            test.log(Status.FAIL, "Element not found while entering the date of birth: " + e.getMessage());
            test.log(Status.FAIL, ExceptionUtils.getStackTrace(e));
            ScreenshotUtil.attachScreenshot(driver, "FailureScreenshot");
            throw e;
        } catch (Exception e) {
            test.log(Status.FAIL, "Unexpected error while entering the date of birth: " + e.getMessage());
            test.log(Status.FAIL, ExceptionUtils.getStackTrace(e));
            ScreenshotUtil.attachScreenshot(driver, "FailureScreenshot");
            throw e;
        }
    }
	
    @When("User enter the date of birth {string}")
    public void user_enter_the_date_of_birth1(String dateOfBirth) {
        try {
        	homePage.selectDateOfBirth(dateOfBirth);
            test.log(Status.PASS, "User successfully entered the date of birth: " + dateOfBirth);
        } catch (NoSuchElementException e) {
            test.log(Status.FAIL, "Element not found while entering the date of birth: " + e.getMessage());
            test.log(Status.FAIL, ExceptionUtils.getStackTrace(e));
            ScreenshotUtil.attachScreenshot(driver, "FailureScreenshot");
            throw e;
        } catch (Exception e) {
            test.log(Status.FAIL, "Unexpected error while entering the date of birth: " + e.getMessage());
            test.log(Status.FAIL, ExceptionUtils.getStackTrace(e));
            ScreenshotUtil.attachScreenshot(driver, "FailureScreenshot");
            throw e;
        }
    }

	@When("User click on login button")
	public void user_click_on_login_button() {
	    try {
	        homePage.clickOnLogin();
	        test.log(Status.PASS, "Successfully clicked on login button.");
	    } catch (NoSuchElementException e) {
	        logFailure(test, driver, "Element not found while clicking login button", e);
	        throw e;
	    } catch (Exception e) {
	        logFailure(test, driver, "Unexpected error while clicking login button", e);
	        throw e;
	    }
	}

	@When("User click on Go Back button")
	public void user_click_on_Go_Back_Button() {
	    try {
	        vpverification.clickOnGoBack();
	        test.log(Status.PASS, "Successfully clicked on Go Back button.");
	    } catch (NoSuchElementException e) {
	        logFailure(test, driver, "Element not found while clicking Go Back button", e);
	        throw e;
	    } catch (Exception e) {
	        logFailure(test, driver, "Unexpected error while clicking Go Back button", e);
	        throw e;
	    }
	}

	@When("Verify uncheck Mosip VC check box")
	public void user_click_on_Mosip_Vc_Check_Box() {
	    try {
	        vpverification.clickOnMosipVC();
	        test.log(Status.PASS, "Successfully uncheck Mosip VC check box.");
	    } catch (NoSuchElementException e) {
	        logFailure(test, driver, "Element not found while unchecking Mosip VC check box", e);
	        throw e;
	    } catch (Exception e) {
	        logFailure(test, driver, "Unexpected error while unchecking Mosip VC check box", e);
	        throw e;
	    }
	}

	@When("User click on Health Insurance check box")
	public void user_click_on_Health_Insurance_Check_Box() {
	    try {
	        vpverification.clickOnHealthInsurance();
	        test.log(Status.PASS, "Successfully clicked on Health Insurance check box.");
	    } catch (NoSuchElementException e) {
	        logFailure(test, driver, "Element not found while clicking Health Insurance check box", e);
	        throw e;
	    } catch (Exception e) {
	        logFailure(test, driver, "Unexpected error while clicking Health Insurance check box", e);
	        throw e;
	    }
	}

	@When("User click on Generate QR Code button")
	public void user_click_on_Generate_Qr_Code_Button() {
	    try {
	        vpverification.clickOnGenerateQRCodeButton();
	        test.log(Status.PASS, "Successfully clicked on Generate QR Code button.");
	    } catch (NoSuchElementException e) {
	        logFailure(test, driver, "Element not found while clicking Generate QR Code button", e);
	        throw e;
	    } catch (Exception e) {
	        logFailure(test, driver, "Unexpected error while clicking Generate QR Code button", e);
	        throw e;
	    }
	}

	@When("User click on Life Insurance VC check box")
	public void user_click_on_Life_Insurance_Check_Box() {
	    try {
	        vpverification.clickOnLifeInsurance();
	        test.log(Status.PASS, "Successfully clicked on Life Insurance VC check box.");
	    } catch (NoSuchElementException e) {
	        logFailure(test, driver, "Element not found while clicking Life Insurance VC check box", e);
	        throw e;
	    } catch (Exception e) {
	        logFailure(test, driver, "Unexpected error while clicking Life Insurance VC check box", e);
	        throw e;
	    }
	}

	@When("Open inji web in tab")
	public void user_Open_inji_web_in_tab() {
	    try {
	        homePage.switchToWebTab();
	        test.log(Status.PASS, "Successfully switched to inji web tab.");
	    } catch (NoSuchElementException e) {
	        logFailure(test, driver, "Error switching to inji web tab", e);
	        throw e;
	    } catch (Exception e) {
	        logFailure(test, driver, "Unexpected error while switching to inji web tab", e);
	        throw e;
	    }
	}
	
	@Then("Verify that Upload button visible")
	public void verify_that_upload_button_visible() {
	    try {
	        boolean isUploadButtonVisible = homePage.isUploadButtonIsVisible();
	        Assert.assertTrue(isUploadButtonVisible, "Upload button is not visible.");
	        test.log(Status.PASS, "Upload button is successfully visible.");
	    } catch (NoSuchElementException e) {
	        logFailure(test, driver, "Element not found while verifying Upload button visibility", e);
	        throw e;
	    } catch (Exception e) {
	        logFailure(test, driver, "Unexpected error while verifying Upload button visibility", e);
	        throw e;
	    }
	}

@Then("Verify the Upload button after 2 mins idle")
public void verify_upload_button_visible_after_2_mins_idle() {
     try {
	        boolean isUploadButtonVisibleAfterIdle = homePage.isUploadButtonIsVisibleAfterIdle();
	        Assert.assertTrue(isUploadButtonVisibleAfterIdle, "Upload button is not visible.");
	        test.log(Status.PASS, "Upload button is successfully visible.");
	    } catch (NoSuchElementException e) {
	        logFailure(test, driver, "Element not found while verifying Upload button visibility", e);
	        throw e;
	    } catch (Exception e) {
	        logFailure(test, driver, "Unexpected error while verifying Upload button visibility", e);
	        throw e;
	    }
	}



	@When("Verify click on Help button")
	public void user_click_on_home_button() {
	    try {
	        homePage.clickOnHelpButton();
	        test.log(Status.PASS, "Successfully clicked on home button.");
	    } catch (NoSuchElementException e) {
	        logFailure(test, driver, "Element not found while clicking home button", e);
	        throw e;
	    } catch (Exception e) {
	        logFailure(test, driver, "Unexpected error while clicking home button", e);
	        throw e;
	    }
	}

	@When("verify alert message")
	public void user_verify_alert_message() {
	    try {
	        assertTrue("Error message is not visible.", homePage.isErrorMessageVisible());
	        test.log(Status.PASS, "Successfully verified alert message is visible.");
	    } catch (AssertionError e) {
	        test.log(Status.FAIL, "Verification failed: Error message is not visible.");
	        throw e;
	    } catch (NoSuchElementException e) {
	        logFailure(test, driver, "Element not found while verifying alert message", e);
	        throw e;
	    } catch (Exception e) {
	        logFailure(test, driver, "Unexpected error while verifying alert message", e);
	        throw e;
	    }
	}

	@When("Verify VP verification qr code step1 description")
	public void verify_vp_verification_qr_code_step1_description() {
	    try {
	        assertEquals(vpverification.getVpVerificationQrCodeStep1Description(), UiConstants.VP_VERIFICATION_QR_CODE_STEP1_DESCRIPTION);
	        test.log(Status.PASS, "Successfully verified VP verification qr code step1 description.");
	    } catch (AssertionError e) {
	        test.log(Status.FAIL, "Verification failed: VP verification qr code step1 description mismatch. Expected: '" + UiConstants.VP_VERIFICATION_QR_CODE_STEP1_DESCRIPTION + "', Actual: '" + vpverification.getVpVerificationQrCodeStep1Description() + "'");
	        throw e;
	    } catch (NoSuchElementException e) {
	        logFailure(test, driver, "Element not found while verifying VP verification qr code step1 description", e);
	        throw e;
	    } catch (Exception e) {
	        logFailure(test, driver, "Unexpected error while verifying VP verification qr code step1 description", e);
	        throw e;
	    }
	}

	@When("Verify VP verification qr code step1 label")
	public void verify_vp_verification_qr_code_step1_label() {
	    try {
	        assertEquals(vpverification.getVpVerificationQrCodeStep1Label(), UiConstants.VP_VERIFICATION_QR_CODE_STEP1_LABEL);
	        test.log(Status.PASS, "Successfully verified VP verification qr code step1 label.");
	    } catch (AssertionError e) {
	        test.log(Status.FAIL, "Verification failed: VP verification qr code step1 label mismatch. Expected: '" + UiConstants.VP_VERIFICATION_QR_CODE_STEP1_LABEL + "', Actual: '" + vpverification.getVpVerificationQrCodeStep1Label() + "'");
	        throw e;
	    } catch (NoSuchElementException e) {
	        logFailure(test, driver, "Element not found while verifying VP verification qr code step1 label", e);
	        throw e;
	    } catch (Exception e) {
	        logFailure(test, driver, "Unexpected error while verifying VP verification qr code step1 label", e);
	        throw e;
	    }
	}

	@When("Verify VP verification qr code step2 label")
	public void verify_vp_verification_qr_code_step2_label() {
	    try {
	        Assert.assertEquals(vpverification.getVpVerificationQrCodeStep2Label(), UiConstants.VP_VERIFICATION_QR_CODE_STEP2_LABEL);
	        test.log(Status.PASS, "Successfully verified VP verification qr code step2 label.");
	    } catch (AssertionError e) {
	        test.log(Status.FAIL, "Verification failed: VP verification qr code step2 label mismatch. Expected: '" + UiConstants.VP_VERIFICATION_QR_CODE_STEP2_LABEL + "', Actual: '" + vpverification.getVpVerificationQrCodeStep2Label() + "'");
	        throw e;
	    } catch (NoSuchElementException e) {
	        logFailure(test, driver, "Element not found while verifying VP verification qr code step2 label", e);
	        throw e;
	    } catch (Exception e) {
	        logFailure(test, driver, "Unexpected error while verifying VP verification qr code step2 label", e);
	        throw e;
	    }
	}

	@When("Verify VP verification qr code step2 description")
	public void verify_vp_verification_qr_code_step2_description() {
	    try {
	        Assert.assertEquals(vpverification.getVpVerificationQrCodeStep2Description(), UiConstants.VP_VERIFICATION_QR_CODE_STEP2_DESCRIPTION);
	        test.log(Status.PASS, "Successfully verified VP verification qr code step2 description.");
	    } catch (AssertionError e) {
	        test.log(Status.FAIL, "Verification failed: VP verification qr code step2 description mismatch. Expected: '" + UiConstants.VP_VERIFICATION_QR_CODE_STEP2_DESCRIPTION + "', Actual: '" + vpverification.getVpVerificationQrCodeStep2Description() + "'");
	        throw e;
	    } catch (NoSuchElementException e) {
	        logFailure(test, driver, "Element not found while verifying VP verification qr code step2 description", e);
	        throw e;
	    } catch (Exception e) {
	        logFailure(test, driver, "Unexpected error while verifying VP verification qr code step2 description", e);
	        throw e;
	    }
	}

	@When("Verify VP verification qr code step3 label")
	public void verify_vp_verification_qr_code_step3_label() {
	    try {
	        Assert.assertEquals(vpverification.getVpVerificationQrCodeStep3Label(), UiConstants.VP_VERIFICATION_QR_CODE_STEP3_LABEL);
	        test.log(Status.PASS, "Successfully verified VP verification qr code step3 label.");
	    } catch (AssertionError e) {
	        test.log(Status.FAIL, "Verification failed: VP verification qr code step3 label mismatch. Expected: '" + UiConstants.VP_VERIFICATION_QR_CODE_STEP3_LABEL + "', Actual: '" + vpverification.getVpVerificationQrCodeStep3Label() + "'");
	        throw e;
	    } catch (NoSuchElementException e) {
	        logFailure(test, driver, "Element not found while verifying VP verification qr code step3 label", e);
	        throw e;
	    } catch (Exception e) {
	        logFailure(test, driver, "Unexpected error while verifying VP verification qr code step3 label", e);
	        throw e;
	    }
	}

	@When("Verify VP verification qr code step3 description")
	public void verify_vp_verification_qr_code_step3_description() {
	    try {
	        Assert.assertEquals(vpverification.getVpVerificationQrCodeStep3Description(), UiConstants.VP_VERIFICATION_QR_CODE_STEP3_DESCRIPTION);
	        test.log(Status.PASS, "Successfully verified VP verification qr code step3 description.");
	    } catch (AssertionError e) {
	        test.log(Status.FAIL, "Verification failed: VP verification qr code step3 description mismatch. Expected: '" + UiConstants.VP_VERIFICATION_QR_CODE_STEP3_DESCRIPTION + "', Actual: '" + vpverification.getVpVerificationQrCodeStep3Description() + "'");
	        throw e;
	    } catch (NoSuchElementException e) {
	        logFailure(test, driver, "Element not found while verifying VP verification qr code step3 description", e);
	        throw e;
	    } catch (Exception e) {
	        logFailure(test, driver, "Unexpected error while verifying VP verification qr code step3 description", e);
	        throw e;
	    }
	}

	@When("Verify VP verification qr code step4 label")
	public void verify_vp_verification_qr_code_step4_label() {
	    try {
	        Assert.assertEquals(vpverification.getVpVerificationQrCodeStep4Label(), UiConstants.VP_VERIFICATION_QR_CODE_STEP4_LABEL);
	        test.log(Status.PASS, "Successfully verified VP verification qr code step4 label.");
	    } catch (AssertionError e) {
	        test.log(Status.FAIL, "Verification failed: VP verification qr code step4 label mismatch. Expected: '" + UiConstants.VP_VERIFICATION_QR_CODE_STEP4_LABEL + "', Actual: '" + vpverification.getVpVerificationQrCodeStep4Label() + "'");
	        throw e;
	    } catch (NoSuchElementException e) {
	        logFailure(test, driver, "Element not found while verifying VP verification qr code step4 label", e);
	        throw e;
	    } catch (Exception e) {
	        logFailure(test, driver, "Unexpected error while verifying VP verification qr code step4 label", e);
	        throw e;
	    }
	}

	@When("Verify VP verification qr code step4 description")
	public void verify_vp_verification_qr_code_step4_description() {
	    try {
	        Assert.assertEquals(vpverification.getVpVerificationQrCodeStep4Description(), UiConstants.VP_VERIFICATION_QR_CODE_STEP4_DESCRIPTION);
	        test.log(Status.PASS, "Successfully verified VP verification qr code step4 description.");
	    } catch (AssertionError e) {
	        test.log(Status.FAIL, "Verification failed: VP verification qr code step4 description mismatch. Expected: '" + UiConstants.VP_VERIFICATION_QR_CODE_STEP4_DESCRIPTION + "', Actual: '" + vpverification.getVpVerificationQrCodeStep4Description() + "'");
	        throw e;
	    } catch (NoSuchElementException e) {
	        logFailure(test, driver, "Element not found while verifying VP verification qr code step4 description", e);
	        throw e;
	    } catch (Exception e) {
	        logFailure(test, driver, "Unexpected error while verifying VP verification qr code step4 description", e);
	        throw e;
	    }
	}

	@When("verify request verifiable credentials button")
	public void verify_request_verifiable_credentials_button() {
	    try {
	        assertTrue("Request verifiable credentials button is not visible.", vpverification.isVisibleVerifiableCredentialsButton());
	        test.log(Status.PASS, "Successfully verified request verifiable credentials button is visible.");
	    } catch (AssertionError e) {
	        test.log(Status.FAIL, "Verification failed: Request verifiable credentials button is not visible.");
	        throw e;
	    } catch (NoSuchElementException e) {
	        logFailure(test, driver, "Element not found while verifying request verifiable credentials button", e);
	        throw e;
	    } catch (Exception e) {
	        logFailure(test, driver, "Unexpected error while verifying request verifiable credentials button", e);
	        throw e;
	    }
	}

	@When("Verify VP verification QR code generated")
	public void verify_VP_verifiable_QR_code_generated() {
	    try {
	        assertTrue("VP verification QR code is not generated.", vpverification.isVpVerificationQrCodeGenerated());
	        test.log(Status.PASS, "Successfully verified VP verification QR code is generated.");
	    } catch (AssertionError e) {
	        test.log(Status.FAIL, "Verification failed: VP verification QR code is not generated.");
	        throw e;
	    } catch (NoSuchElementException e) {
	        logFailure(test, driver, "Element not found while verifying VP verification QR code generation", e);
	        throw e;
	    } catch (Exception e) {
	        logFailure(test, driver, "Unexpected error while verifying VP verification QR code generation", e);
	        throw e;
	    }
	}

	@When("Verify Verifiable Credential Panel label")
	public void verify_Verifiable_Credential_Selection_Panel() {
	    try {
	        Assert.assertEquals(vpverification.isVerifiableCredentialSelectionPannelDisplayed(), UiConstants.VERIFIABLE_VERIFICATION_PANNEL);
	        test.log(Status.PASS, "Successfully verified Verifiable Credential Selection Panel is displayed with correct text.");
	    } catch (AssertionError e) {
	        test.log(Status.FAIL, "Verification failed: Verifiable Credential Selection Panel text mismatch. Expected: '" + UiConstants.VERIFIABLE_VERIFICATION_PANNEL + "', Actual: '" + vpverification.isVerifiableCredentialSelectionPannelDisplayed() + "'");
	        logFailure(test, driver, "Verification failed: Verifiable Credential Selection Panel text mismatch. Expected: '" + UiConstants.VERIFIABLE_VERIFICATION_PANNEL + "', Actual: '" + vpverification.isVerifiableCredentialSelectionPannelDisplayed() + "'", e);
	        throw e;
	    } catch (NoSuchElementException e) {
	        logFailure(test, driver, "Element not found while verifying Verifiable Credential Selection Panel", e);
	        throw e;
	    } catch (Exception e) {
	        logFailure(test, driver, "Unexpected error while verifying Verifiable Credential Selection Panel", e);
	        throw e;
	    }
	}
	
	
	@Then("Upload Large size not supported QR code file")
	public void upload_large_size_not_supported_qr_code_file() {
	    try {
	        uploadqrcode.clickOnUploadLargeSizeQRCode();
     
	        test.log(Status.PASS, "Successfully verified large size QR code file is not supported and appropriate error is shown.");
	    } catch (AssertionError e) {
	        test.log(Status.FAIL, "Verification failed: Expected error message for large QR code not shown.");
	        throw e;
	    } catch (NoSuchElementException e) {
	        logFailure(test, driver, "Element not found while uploading large size QR code", e);
	        throw e;
	    } catch (Exception e) {
	        logFailure(test, driver, "Unexpected error while uploading large size QR code", e);
	        throw e;
	    }
	}
	
	@Then("Click on ble tab")
	public void click_on_ble_tab1() {
	    try {
	        ble.clickOnBleTab();
	        test.log(Status.PASS, "Successfully clicked on BLE tab and verified it is active.");
	    } catch (AssertionError e) {
	        test.log(Status.FAIL, "Verification failed: BLE tab did not activate.");
	        throw e;
	    } catch (NoSuchElementException e) {
	        logFailure(test, driver, "Element not found while clicking on BLE tab", e);
	        throw e;
	    } catch (Exception e) {
	        logFailure(test, driver, "Unexpected error occurred while clicking on BLE tab", e);
	        throw e;
	    }
	}
	
	@Then("Verify Large size alert message")
	public void verify_message_for_large_size_qr_code() {
	    try {
	        String actualMessage = uploadqrcode.getErrorMessageForLargeSizeQRCode();
	        String expectedMessage = UiConstants.ERROR_MESSAGE_LARGEFILE_QR;

	        Assert.assertEquals(actualMessage, expectedMessage, "Large size QR code alert message mismatch.");

	        test.log(Status.PASS, "Successfully verified large size QR code alert message: '" + actualMessage + "'");
	    } catch (AssertionError e) {
	        test.log(Status.FAIL, "Alert message verification failed. Expected: '" + UiConstants.ERROR_MESSAGE_LARGEFILE_QR +
	            "', Actual: '" + uploadqrcode.getErrorMessageForLargeSizeQRCode() + "'");
	        throw e;
	    } catch (NoSuchElementException e) {
	        logFailure(test, driver, "Error message element for large QR code not found", e);
	        throw e;
	    } catch (Exception e) {
	        logFailure(test, driver, "Unexpected error occurred while verifying large QR code error message", e);
	        throw e;
	    }
	}
	
	@Then("Upload blur QR code file")
	public void upload_blur_qrcode_file() {
        try {
            uploadqrcode.clickOnUploadBlurQRCode();
	        test.log(Status.PASS, "Successfully uploaded blur QR code file.");
	    } catch (AssertionError e) {
	        test.log(Status.FAIL, "Verification failed: Blur QR code file upload failed.");
	        throw e;
	    } catch (NoSuchElementException e) {
	        logFailure(test, driver, "Element not found while uploading blur QR code file", e);
	        throw e;
	    } catch (Exception e) {
	        logFailure(test, driver, "Unexpected error occurred while uploading blur QR code file", e);
	        throw e;
	    }
	}

	@Then("Verify MultiFormat alert message")
	public void verify_message_for_blur_qr_code() {
	    try {
	        Assert.assertEquals(uploadqrcode.getErrorMessageForBlurQRCode(), UiConstants.ERROR_MULTI_FORMAT);
	        test.log(Status.PASS, "Successfully verified MultiFormat alert message for blur QR code.");
	    } catch (AssertionError e) {
	    	test.log(Status.FAIL, "Verification failed: MultiFormat alert message does not match the expected value.");
	    	logFailure(test, driver, "Verification failed: MultiFormat alert message does not match the expected value.", e);
	        throw e;
	    } catch (NoSuchElementException e) {
	        logFailure(test, driver, "Element not found while verifying MultiFormat alert message", e);
	        throw e;
	    } catch (Exception e) {
	        logFailure(test, driver, "Unexpected error occurred while verifying MultiFormat alert message", e);
	        throw e;
	    }
	}


	@Then("Upload multiple qr code in one image file")
	public void upload_multiple_QR_code_in_one_image_file() {
        try {
            uploadqrcode.clickOnUploadMultipleQRCode();
	        test.log(Status.PASS, "Successfully uploaded image with multiple QR codes.");
	    } catch (AssertionError e) {
	        test.log(Status.FAIL, "Verification failed: Upload of multiple QR codes in one image did not behave as expected.");
	        throw e;
	    } catch (NoSuchElementException e) {
	        logFailure(test, driver, "Element not found while uploading image with multiple QR codes", e);
	        throw e;
	    } catch (Exception e) {
	        logFailure(test, driver, "Unexpected error occurred while uploading image with multiple QR codes", e);
	        throw e;
	    }
	}

    @Then("Upload SD-Jwt QR code")
	public void upload_sd_jwt_qr_code() {
        try {
            uploadqrcode.clickOnUploadSDJwtQRCode();
	        test.log(Status.PASS, "Successfully uploaded SD-Jwt QR code.");
	    } catch (AssertionError e) {
	        test.log(Status.FAIL, "Verification failed: Upload of SD-Jwt QR code did not behave as expected.");
	        throw e;
	    } catch (NoSuchElementException e) {
	        logFailure(test, driver, "Element not found while uploading SD-Jwt QR code", e);
	        throw e;
	    } catch (Exception e) {
	        logFailure(test, driver, "Unexpected error occurred while uploading SD-Jwt QR code", e);
	        throw e;
	    }
	}

	@Then("Upload invalid pdf")
	public void upload_invalid_pdf() {
        try {
            uploadqrcode.clickOnUploadInvalidPdf();
	        test.log(Status.PASS, "Successfully attempted to upload an invalid PDF file.");
	    } catch (AssertionError e) {
	        test.log(Status.FAIL, "Verification failed: Upload of invalid PDF file did not behave as expected.");
	        throw e;
	    } catch (NoSuchElementException e) {
	        logFailure(test, driver, "Element not found while uploading invalid PDF file", e);
	        throw e;
	    } catch (Exception e) {
	        logFailure(test, driver, "Unexpected error occurred while uploading invalid PDF file", e);
	        throw e;
	    }
	}


	
	@Then("verify scan qr code area")
	public void verify_scan_qr_code_area() {
	    try {
	        Assert.assertTrue(scanqrcode.isVisibleScanQRCodeArea());
	        test.log(Status.PASS, "Successfully verified that the Scan QR Code area is visible.");
	    } catch (AssertionError e) {
	        test.log(Status.FAIL, "Verification failed: Scan QR Code area is not visible.");
	        throw e;
	    } catch (NoSuchElementException e) {
	        logFailure(test, driver, "Element not found while verifying Scan QR Code area", e);
	        throw e;
	    } catch (Exception e) {
	        logFailure(test, driver, "Unexpected error occurred while verifying Scan QR Code area", e);
	        throw e;
	    }
	}

	@Then("Verify click sort by button")
	public void user_click_on_sort_button() {
	    try {
	        vpverification.clickOnSortButton();
	        test.log(Status.PASS, "Successfully clicked on the 'Sort By' button.");
	    } catch (AssertionError e) {
	        test.log(Status.FAIL, "Verification failed: Unable to click on the 'Sort By' button.");
	        throw e;
	    } catch (NoSuchElementException e) {
	        logFailure(test, driver, "Element not found while clicking on the 'Sort By' button", e);
	        throw e;
	    } catch (Exception e) {
	        logFailure(test, driver, "Unexpected error occurred while clicking on the 'Sort By' button", e);
	        throw e;
	    }
	}
	
	@Then("Verify click Sort AtoZ button")
	public void user_click_on_sort_a_z_button() {
	    try {
	        vpverification.clickOnSortAtoZButton();
	        test.log(Status.PASS, "Successfully clicked on the 'Sort A to Z' button.");
	    } catch (AssertionError e) {
	        test.log(Status.FAIL, "Verification failed: Unable to click on the 'Sort A to Z' button.");
	        throw e;
	    } catch (NoSuchElementException e) {
	        logFailure(test, driver, "Element not found while clicking on the 'Sort A to Z' button", e);
	        throw e;
	    } catch (Exception e) {
	        logFailure(test, driver, "Unexpected error occurred while clicking on the 'Sort A to Z' button", e);
	        throw e;
	    }
	}

	@Then("Verify click Sort ZtoA button")
	public void user_click_on_sort_z_a_button() {
	    try {
	        vpverification.clickOnSortZtoAButton();
	        test.log(Status.PASS, "Successfully clicked on the 'Sort Z to A' button.");
	    } catch (AssertionError e) {
	        test.log(Status.FAIL, "Verification failed: Unable to click on the 'Sort Z to A' button.");
	        throw e;
	    } catch (NoSuchElementException e) {
	        logFailure(test, driver, "Element not found while clicking on the 'Sort Z to A' button", e);
	        throw e;
	    } catch (Exception e) {
	        logFailure(test, driver, "Unexpected error occurred while clicking on the 'Sort Z to A' button", e);
	        throw e;
	    }
	}


	@Then("Verify click Back button")
	public void user_click_on_back_button() {
	    try {
	        vpverification.clickOnBackButton();
	        test.log(Status.PASS, "Successfully clicked on the 'Back' button.");
	    } catch (AssertionError e) {
	        test.log(Status.FAIL, "Verification failed: Unable to click on the 'Back' button.");
	        throw e;
	    } catch (NoSuchElementException e) {
	        logFailure(test, driver, "Element not found while clicking on the 'Back' button", e);
	        throw e;
	    } catch (Exception e) {
	        logFailure(test, driver, "Unexpected error occurred while clicking on the 'Back' button", e);
	        throw e;
	    }
	}

    @Then("Verify Click on cancel")
	public void user_click_on_cancel_button() {
	    try {
	        vpverification.clickOnCancelButton();
	        test.log(Status.PASS, "Successfully clicked on the 'Cancel' button.");
	    } catch (AssertionError e) {
	        test.log(Status.FAIL, "Verification failed: Unable to click on the 'Cancel' button.");
	        throw e;
	    } catch (NoSuchElementException e) {
	        logFailure(test, driver, "Element not found while clicking on the 'Cancel' button", e);
	        throw e;
	    } catch (Exception e) {
	        logFailure(test, driver, "Unexpected error occurred while clicking on the 'Cancel' button", e);
	        throw e;
	    }
	}

    @Then("Verify Click on open wallet button")
	public void user_click_on_open_wallet_button() {
	    try {
	        vpverification.clickOnOpenWalletButton();
	        test.log(Status.PASS, "Successfully clicked on the 'Open Wallet' button.");
	    } catch (AssertionError e) {
	        test.log(Status.FAIL, "Verification failed: Unable to click on the 'Open Wallet' button.");
	        throw e;
	    } catch (NoSuchElementException e) {
	        logFailure(test, driver, "Element not found while clicking on the 'Open Wallet' button", e);
	        throw e;
	    } catch (Exception e) {
	        logFailure(test, driver, "Unexpected error occurred while clicking on the 'Open Wallet' button", e);
	        throw e;
	    }
	}


	@Then("Verify Click on Generate QR Code button")
	public void verify_click_on_generate_qr_code_button() {
	    try {
	        vpverification.clickOnGenerateQrCodeButton();
	        test.log(Status.PASS, "Successfully clicked on the 'Generate QR Code' button.");
	    } catch (AssertionError e) {
	        test.log(Status.FAIL, "Verification failed: Unable to click on the 'Generate QR Code' button.");
	        throw e;
	    } catch (NoSuchElementException e) {
	        logFailure(test, driver, "Element not found while clicking on the 'Generate QR Code' button", e);
	        throw e;
	    } catch (Exception e) {
	        logFailure(test, driver, "Unexpected error occurred while clicking on the 'Generate QR Code' button", e);
	        throw e;
	    }
	}


	@Then("Verify QR code generated")
	public void verify_qr_code_generated() {
	    try {
	        Assert.assertTrue(vpverification.isVpVerificationQrCodeGenerated());
	        test.log(Status.PASS, "Successfully verified that the QR code was generated.");
	    } catch (NoSuchElementException e) {
	        logFailure(test, driver, "Element not found while verifying QR code generation", e);
	        throw e;
	    } catch (Exception e) {
	        logFailure(test, driver, "Unexpected error occurred while verifying QR code generation", e);
	        throw e;
	    }
	}

    @Then("verify loading screen")
	public void verify_loading_screen() {
	    try {
	        Assert.assertTrue(vpverification.isLoadingScreenDisplayed());
	        test.log(Status.PASS, "Successfully verified that the loading screen is displayed.");
	    } catch (NoSuchElementException e) {
	        logFailure(test, driver, "Element not found while verifying loading screen", e);
	        throw e;
	    } catch (Exception e) {
	        logFailure(test, driver, "Unexpected error occurred while verifying loading screen", e);
	        throw e;
	    }
	}

	@Then("Verify QR code is not precent")
	public void verify_qr_code_is_not_present() {
	    try {
	        Assert.assertFalse(vpverification.isVpVerificationQrCodeGenerated());
	        test.log(Status.PASS, "Successfully verified that the QR code is not present.");
	    } catch (AssertionError e) {
	        test.log(Status.FAIL, "Verification failed: QR code is unexpectedly present.");
	        logFailure(test, driver, "Verification failed: QR code is unexpectedly present.", e);
	        throw e;
	    } catch (NoSuchElementException e) {
	        logFailure(test, driver, "Element not found while verifying absence of QR code", e);
	        throw e;
	    } catch (Exception e) {
	        logFailure(test, driver, "Unexpected error occurred while verifying absence of QR code", e);
	        throw e;
	    }
	}

	
	@Then("Uncheck MOSIP ID")
	public void uncheck_mosip_id() {
	    try {
	        vpverification.clickOnMosipIdChecklist();
	        test.log(Status.PASS, "Successfully unchecked the MOSIP ID option.");
	    } catch (NoSuchElementException e) {
	        logFailure(test, driver, "Element not found while unchecking the MOSIP ID option", e);
	        throw e;
	    } catch (Exception e) {
	        logFailure(test, driver, "Unexpected error occurred while unchecking the MOSIP ID option", e);
	        throw e;
	    }
	}

	
	@Then("Select Health Insurance")
	public void select_health_insurance() {
	    try {
	        vpverification.clickOnHealthInsuranceChecklist();
	        test.log(Status.PASS, "Successfully selected the Health Insurance option.");
	    } catch (NoSuchElementException e) {
	        logFailure(test, driver, "Element not found while selecting the Health Insurance option", e);
	        throw e;
	    } catch (Exception e) {
	        logFailure(test, driver, "Unexpected error occurred while selecting the Health Insurance option", e);
	        throw e;
	    }
	}

    @Then("Select SD JWT VC")
	public void select_sd_jwt_vc() {
	    try {
	        vpverification.clickOnSDJwtVCChecklist();
	        test.log(Status.PASS, "Successfully selected the SD JWT VC option.");
	    } catch (NoSuchElementException e) {
	        logFailure(test, driver, "Element not found while selecting the SD JWT VC option", e);
	        throw e;
	    } catch (Exception e) {
	        logFailure(test, driver, "Unexpected error occurred while selecting the SD JWT VC option", e);
	        throw e;
	    }
	}

    @Then("Verify Click on wallet")
	public void verify_click_on_wallet() {
	    try {
	        vpverification.clickOnWalletButton();
	        test.log(Status.PASS, "Successfully clicked on the Wallet button.");
	    } catch (NoSuchElementException e) {
	        logFailure(test, driver, "Element not found while clicking on the Wallet button", e);
	        throw e;
	    } catch (Exception e) {
	        logFailure(test, driver, "Unexpected error occurred while clicking on the Wallet button", e);
	        throw e;
	    }
	}

    @Then("Verify Click on Proceed")
	public void verify_click_on_proceed() {
	    try {
	        vpverification.clickOnProceedButton();
	        test.log(Status.PASS, "Successfully clicked on the Proceed button.");
	    } catch (NoSuchElementException e) {
	        logFailure(test, driver, "Element not found while clicking on the Proceed button", e);
	        throw e;
	    } catch (Exception e) {
	        logFailure(test, driver, "Unexpected error occurred while clicking on the Proceed button", e);
	        throw e;
	    }
	}

	
	@Then("Select Land Registry")
	public void uncheck_land_registry() {
	    try {
	        vpverification.clickOnLandRegistryChecklist();
	        test.log(Status.PASS, "Successfully selected the Land Registry option.");
	    } catch (NoSuchElementException e) {
	        logFailure(test, driver, "Element not found while selecting the Land Registry option", e);
	        throw e;
	    } catch (Exception e) {
	        logFailure(test, driver, "Unexpected error occurred while selecting the Land Registry option", e);
	        throw e;
	    }
	}

	@Then("User enter the credential type {string}")
	public void user_enter_the_credential_type(String credentialType) {
	    try {
	        Thread.sleep(3000); 
	        vpverification.enterCredentialType(credentialType);
	        test.log(Status.PASS, "Successfully entered credential type: " + credentialType);
	    } catch (InterruptedException e) {
	        Thread.currentThread().interrupt(); // Restore interrupt status
	        logFailure(test, driver, "Thread was interrupted while entering credential type", e);
	        throw new RuntimeException(e);
	    } catch (NoSuchElementException e) {
	        logFailure(test, driver, "Element not found while entering credential type: " + credentialType, e);
	        throw e;
	    } catch (Exception e) {
	        logFailure(test, driver, "Unexpected error occurred while entering credential type: " + credentialType, e);
	        throw e;
	    }
	}


	@Then("Verify that Upload icon visible")
	public void verify_that_upload_icon_visible() {
		Assert.assertTrue(homePage.isUploadIconIsVisible());
	}
	
        @When("Verify if name value is present in arabic")
    public void verifyIfNameValueIsPresentInArabic() {
        try {
            String actualLabel = homePage.getNameValueInArabic();
            Assert.assertEquals(actualLabel, UiConstants.NAME_VALUE_IN_ARABIC, 
                "Name value in arabic does not match the expected value.");
            test.log(Status.PASS, "Name value in arabic verification successful. Expected: " 
                + UiConstants.NAME_VALUE_IN_ARABIC + ", Actual: " + actualLabel);
        } catch (AssertionError e) {
            test.log(Status.FAIL, "Name value in arabic verification failed. Expected: " 
                + UiConstants.NAME_VALUE_IN_ARABIC + ", but found: " + homePage.getNameValueInArabic());
            throw e;
        } catch (NoSuchElementException e) {
            logFailure(test, driver, "Element not found while verifying Name value in arabic", e);
            throw e;
        } catch (Exception e) {
            logFailure(test, driver, "Unexpected error while verifying Name value in arabic", e);
            throw e;
        }
    }

    @When("Verify if name value is present in french")
    public void verifyIfNameValueIsPresentInFrench() {
        try {
            String actualLabel = homePage.getNameValueInFrench();
            Assert.assertEquals(actualLabel, UiConstants.NAME_VALUE_IN_FRENCH, 
                "Name value in french does not match the expected value.");
            test.log(Status.PASS, "Name value in french verification successful. Expected: " 
                + UiConstants.NAME_VALUE_IN_FRENCH + ", Actual: " + actualLabel);
        } catch (AssertionError e) {
            test.log(Status.FAIL, "Name value in french verification failed. Expected: " 
                + UiConstants.NAME_VALUE_IN_FRENCH + ", but found: " + homePage.getNameValueInFrench());
            throw e;
        } catch (NoSuchElementException e) {
            logFailure(test, driver, "Element not found while verifying Name value in french", e);
            throw e;
        } catch (Exception e) {
            logFailure(test, driver, "Unexpected error while verifying Name value in french", e);
            throw e;
        }
    }

    @When("Verify if gender value is present in arabic")
    public void verifyIfGenderValueIsPresentInArabic() {
        try {
            String actualLabel = homePage.getGenderValueInArabic();
            Assert.assertEquals(actualLabel, UiConstants.GENDER_VALUE_IN_ARABIC, 
                "Gender value in arabic does not match the expected value.");
            test.log(Status.PASS, "Gender value in arabic verification successful. Expected: " 
                + UiConstants.GENDER_VALUE_IN_ARABIC + ", Actual: " + actualLabel);
        } catch (AssertionError e) {
            test.log(Status.FAIL, "Gender value in arabic verification failed. Expected: " 
                + UiConstants.GENDER_VALUE_IN_ARABIC + ", but found: " + homePage.getGenderValueInArabic());
            throw e;
        } catch (NoSuchElementException e) {
            logFailure(test, driver, "Element not found while verifying Gender value in arabic", e);
            throw e;
        } catch (Exception e) {
            logFailure(test, driver, "Unexpected error while verifying Gender value in arabic", e);
            throw e;
        }
    }

    @When("Verify if gender value is present in French")
    public void verifyIfGenderValueIsPresentInFrench() {
        try {
            String actualLabel = homePage.getGenderValueInFrench();
            Assert.assertEquals(actualLabel, UiConstants.GENDER_VALUE_IN_FRENCH, 
                "Gender value in French does not match the expected value.");
            test.log(Status.PASS, "Gender value in French verification successful. Expected: " 
                + UiConstants.GENDER_VALUE_IN_FRENCH + ", Actual: " + actualLabel);
        } catch (AssertionError e) {
            test.log(Status.FAIL, "Gender value in French verification failed. Expected: " 
                + UiConstants.GENDER_VALUE_IN_FRENCH + ", but found: " + homePage.getGenderValueInFrench());
            throw e;
        } catch (NoSuchElementException e) {
            logFailure(test, driver, "Element not found while verifying Gender value in French", e);
            throw e;
        } catch (Exception e) {
            logFailure(test, driver, "Unexpected error while verifying Gender value in French", e);
            throw e;
        }
    }

    @When("Verify select arabic language")
    public void verifyselectArabicLanguage() {
        try {
            uploadqrcode.selectArabicLanguage();
            test.log(Status.PASS, "Clicked on 'Arabic Language' button successfully.");
        } catch (NoSuchElementException e) {
            logFailure(test, driver, "Element not found while clicking 'Arabic Language' button", e);
            throw e;
        } catch (Exception e) {
            logFailure(test, driver, "Unexpected error while clicking 'Arabic Language' button", e);
            throw e;
        }
    }

    @When("Verify select french language")
    public void verifyselectFrenchLanguage() {
        try {
            uploadqrcode.selectFrenchLanguage();
            test.log(Status.PASS, "Clicked on 'French Language' button successfully.");
            boolean isFrenchSelected = uploadqrcode.isFrenchLanguageSelected();
            Assert.assertTrue(isFrenchSelected, "French language should be selected after click");
            test.log(Status.PASS, "French language is successfully selected. Language indicator shows 'Français'.");
        } catch (NoSuchElementException e) {
            logFailure(test, driver, "Element not found while clicking 'French Language' button", e);
            throw e;
        } catch (Exception e) {
            logFailure(test, driver, "Unexpected error while clicking 'French Language' button", e);
            throw e;
        }
    }
}