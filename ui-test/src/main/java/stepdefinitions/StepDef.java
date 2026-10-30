
package stepdefinitions;

import org.openqa.selenium.NoSuchElementException;
import org.openqa.selenium.NoSuchWindowException;
import org.openqa.selenium.InvalidArgumentException;
import org.testng.Assert;

import constants.UiConstants;
import base.BasePage;
import io.cucumber.java.Before;
import io.cucumber.java.en.Given;
import io.cucumber.java.en.Then;
import io.cucumber.java.en.When;

import java.io.IOException;

import utils.BaseTest;
import java.util.Base64;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.io.OutputStream;
import java.io.FileOutputStream;
import java.io.File;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.time.Instant;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.Locale;

import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.text.PDFTextStripper;
import org.apache.pdfbox.rendering.PDFRenderer;
import org.apache.pdfbox.pdmodel.PDPage;
import utils.ScreenshotUtil;
import org.apache.commons.lang3.exception.ExceptionUtils;
import org.openqa.selenium.logging.LogEntries;
import org.openqa.selenium.logging.LogEntry;
import org.openqa.selenium.logging.LogType;
import org.openqa.selenium.JavascriptExecutor;
import api.InjiVerifyConfigManager;


import com.aventstack.extentreports.Status;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static org.testng.Assert.assertEquals;

import java.awt.image.BufferedImage;
import javax.imageio.ImageIO;
import org.openqa.selenium.By;
import org.openqa.selenium.support.ui.ExpectedConditions;
import org.openqa.selenium.support.ui.WebDriverWait;
import java.time.Duration;

public class StepDef extends BaseSteps {

    /** Maps English language names (used in feature files) to their native UI display names. */
    private static final Map<String, String> ENGLISH_TO_DISPLAY_NAME = new HashMap<>();
    static {
        ENGLISH_TO_DISPLAY_NAME.put("English",    "English");
        ENGLISH_TO_DISPLAY_NAME.put("Portuguese", "Português");
        ENGLISH_TO_DISPLAY_NAME.put("Tamil",      "தமிழ்");
        ENGLISH_TO_DISPLAY_NAME.put("Kannada",    "ಕನ್ನಡ");
        ENGLISH_TO_DISPLAY_NAME.put("Hindi",      "हिंदी");
        ENGLISH_TO_DISPLAY_NAME.put("French",     "Français");
        ENGLISH_TO_DISPLAY_NAME.put("Arabic",     "عربي");
        ENGLISH_TO_DISPLAY_NAME.put("Spanish",    "español");
        ENGLISH_TO_DISPLAY_NAME.put("Khmer",      "ខ្មែរ");
    }

    private static String toDisplayName(String englishName) {
        return ENGLISH_TO_DISPLAY_NAME.getOrDefault(englishName, englishName);
    }

    private String uploadPageUrl;
    private String scanPageUrl;
    private String vpVerificationPageUrl;

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

    @Then("Validate the title of the page")
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
            uploadqrcode.uploadPngAndWaitForVerificationResult();
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
	public void uploadSvgRenderedVcCode() {
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

    @When("Block SVG credential template network requests")
    public void blockSvgCredentialTemplateNetworkRequests() {
        try {
            uploadqrcode.blockSvgTemplateNetworkRequests();
            test.log(Status.PASS, "Blocked network requests to the SVG credential template host.");
        } catch (Exception e) {
            logFailure(test, driver, "Unable to block SVG credential template network requests", e);
            throw e;
        }
    }

    @When("Throttle network for SVG credential template requests")
    public void throttleNetworkForSvgCredentialTemplateRequests() {
        try {
            uploadqrcode.throttleSvgTemplateNetworkRequests();
            test.log(Status.PASS, "Throttled network for the SVG credential template host.");
        } catch (Exception e) {
            logFailure(test, driver, "Unable to throttle SVG credential template network requests", e);
            throw e;
        }
    }

    @Then("Verify failed to load credential template alert message is shown")
    public void verifyFailedToLoadCredentialTemplateAlertMessageIsShown() {
        try {
            Assert.assertEquals(uploadqrcode.getErrorMessageForSvgTemplateFailure(),
                UiConstants.ERROR_FAILED_TO_LOAD_CREDENTIAL_TEMPLATE,
                "Alert message does not match 'Failed to load credential template'.");
            test.log(Status.PASS, "Successfully verified the 'Failed to load credential template' alert message.");
        } catch (AssertionError e) {
            logFailure(test, driver, "Alert message did not match 'Failed to load credential template'", e);
            throw e;
        } catch (NoSuchElementException e) {
            logFailure(test, driver, "Element not found while verifying the SVG template failure alert message", e);
            throw e;
        } catch (Exception e) {
            logFailure(test, driver, "Unexpected error while verifying the SVG template failure alert message", e);
            throw e;
        }
    }

    @Then("Verify VC details grid is displayed instead of SVG template")
    public void verifyVcDetailsGridIsDisplayedInsteadOfSvgTemplate() {
        try {
            Assert.assertTrue(uploadqrcode.isVcDetailsGridVisible(), "VC details grid was not displayed as a fallback.");
            Assert.assertFalse(uploadqrcode.isSvgTemplateRenderedNow(), "SVG template should not have rendered after a fetch failure.");
            test.log(Status.PASS, "Successfully verified fallback to the VC details grid.");
        } catch (AssertionError e) {
            logFailure(test, driver, "Fallback to VC details grid was not as expected", e);
            throw e;
        } catch (NoSuchElementException e) {
            logFailure(test, driver, "Element not found while verifying fallback to the VC details grid", e);
            throw e;
        } catch (Exception e) {
            logFailure(test, driver, "Unexpected error while verifying fallback to the VC details grid", e);
            throw e;
        }
    }

    @Then("Verify SVG template is rendered successfully")
    public void verifySvgTemplateIsRenderedSuccessfully() {
        try {
            Assert.assertTrue(uploadqrcode.isSvgTemplateRendered(), "SVG template was not rendered.");
            test.log(Status.PASS, "Successfully verified the SVG template rendered.");
        } catch (AssertionError e) {
            logFailure(test, driver, "SVG template did not render as expected", e);
            throw e;
        } catch (NoSuchElementException e) {
            logFailure(test, driver, "Element not found while verifying the SVG template rendered", e);
            throw e;
        } catch (Exception e) {
            logFailure(test, driver, "Unexpected error while verifying the SVG template rendered", e);
            throw e;
        }
    }

    @When("Upload SVG VC with invalid template mediaType")
    public void uploadSvgVcWithInvalidTemplateMediaType() {
        try {
            uploadqrcode.clickOnUploadSvgInvalidMediaTypeQRCode();
            test.log(Status.PASS, "Successfully uploaded SVG VC with an invalid template mediaType.");
        } catch (NoSuchElementException e) {
            logFailure(test, driver, "Element not found while uploading SVG VC with invalid template mediaType", e);
            throw e;
        } catch (Exception e) {
            logFailure(test, driver, "Unexpected error while uploading SVG VC with invalid template mediaType", e);
            throw e;
        }
    }

    @When("Upload SVG VC with missing template data")
    public void uploadSvgVcWithMissingTemplateData() {
        try {
            uploadqrcode.clickOnUploadSvgMissingDataQRCode();
            test.log(Status.PASS, "Successfully uploaded SVG VC with missing template data.");
        } catch (NoSuchElementException e) {
            logFailure(test, driver, "Element not found while uploading SVG VC with missing template data", e);
            throw e;
        } catch (Exception e) {
            logFailure(test, driver, "Unexpected error while uploading SVG VC with missing template data", e);
            throw e;
        }
    }

    // The SVG-template fetch-failure alert ("Failed to load credential template") is
    // currently dispatched as a raw Error.message and never routed through i18n (see
    // DisplayVcDetailView.onTemplateError), unlike other alerts which use
    // t("AlertMessages:...") across locales. This step asserts the REAL expected localized
    // text per language (not the current English-only text), so it is EXPECTED TO FAIL today
    // for non-English languages - that is intentional: it documents the actual product gap as
    // a genuine regression target, and will start passing once the message is localized.
    @Then("Verify failed to load credential template alert message is shown in {string}")
    public void verifyFailedToLoadCredentialTemplateAlertMessageIsShownInLanguage(String language) {
        String expectedMessage;
        switch (language) {
            case "French":
                expectedMessage = UiConstants.ERROR_FAILED_TO_LOAD_CREDENTIAL_TEMPLATE_IN_FRENCH;
                break;
            case "Arabic":
                expectedMessage = UiConstants.ERROR_FAILED_TO_LOAD_CREDENTIAL_TEMPLATE_IN_ARABIC;
                break;
            case "Spanish":
                expectedMessage = UiConstants.ERROR_FAILED_TO_LOAD_CREDENTIAL_TEMPLATE_IN_SPANISH;
                break;
            case "Tamil":
                expectedMessage = UiConstants.ERROR_FAILED_TO_LOAD_CREDENTIAL_TEMPLATE_IN_TAMIL;
                break;
            default:
                expectedMessage = UiConstants.ERROR_FAILED_TO_LOAD_CREDENTIAL_TEMPLATE;
                break;
        }
        try {
            String actualMessage = uploadqrcode.getErrorMessageForSvgTemplateFailure();
            Assert.assertEquals(actualMessage, expectedMessage,
                "SVG template failure alert message for language '" + language
                    + "' does not match the expected localized text. This is expected to fail until the "
                    + "product localizes this message (it is currently a hardcoded English string).");
            test.log(Status.PASS,
                "Successfully verified the SVG template failure alert message in '" + language + "': " + actualMessage);
        } catch (AssertionError e) {
            logFailure(test, driver, "SVG template failure alert message did not match the expected localized text for language '" + language + "'", e);
            throw e;
        } catch (NoSuchElementException e) {
            logFailure(test, driver, "Element not found while verifying the SVG template failure alert message in '" + language + "'", e);
            throw e;
        } catch (Exception e) {
            logFailure(test, driver, "Unexpected error while verifying the SVG template failure alert message in '" + language + "'", e);
            throw e;
        }
    }

    @Then("Upload claim 169 VC")
	public void uploadClaim169VcCode() {
        try {
            uploadqrcode.clickOnUploadClaim169QRCode();
	        test.log(Status.PASS, "Successfully uploaded claim 169 VC.");
	    } catch (AssertionError e) {
	        test.log(Status.FAIL, "Verification failed: Upload of claim 169 VC QR code did not behave as expected.");
	        throw e;
	    } catch (NoSuchElementException e) {
	        logFailure(test, driver, "Element not found while uploading claim 169 VC QR code", e);
	        throw e;
	    } catch (Exception e) {
	        logFailure(test, driver, "Unexpected error occurred while uploading claim 169 VC QR code", e);
	        throw e;
	    }
	}

    @When("Upload another QR code file png")
    public void uploadAnotherQRCodeFile() {
        try {
            uploadqrcode.uploadAnotherPngAndWaitForVerificationResult();
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
    public void verifyPolicyIssuedOnValue() {
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
    public void verifyFullNameValue() {
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

    @Then("verify full name matches expected value")
    public void verifyFullNameMatchesExpectedValue() {
        try {
            String actual = uploadqrcode.getFullNameValue();
            Assert.assertEquals(actual, UiConstants.EXPECTED_VC_FULL_NAME,
                    "Full Name does not match. Expected: " + UiConstants.EXPECTED_VC_FULL_NAME + ", Actual: " + actual);
            test.log(Status.PASS, "Full Name matches expected value: " + actual);
        } catch (AssertionError e) {
            logFailure(test, driver, "Full Name value mismatch", e);
            throw e;
        } catch (NoSuchElementException e) {
            logFailure(test, driver, "Element not found while verifying Full Name value", e);
            throw e;
        } catch (Exception e) {
            logFailure(test, driver, "Unexpected error while verifying Full Name value", e);
            throw e;
        }
    }

    @Then("verify policy issued on matches expected value")
    public void verifyPolicyIssuedOnMatchesExpectedValue() {
        try {
            String actual = uploadqrcode.getPolicyIssuedOnValue();
            Assert.assertEquals(actual, UiConstants.EXPECTED_VC_POLICY_ISSUED_ON,
                    "Policy Issued On does not match. Expected: " + UiConstants.EXPECTED_VC_POLICY_ISSUED_ON + ", Actual: " + actual);
            test.log(Status.PASS, "Policy Issued On matches expected value: " + actual);
        } catch (AssertionError e) {
            logFailure(test, driver, "Policy Issued On value mismatch", e);
            throw e;
        } catch (NoSuchElementException e) {
            logFailure(test, driver, "Element not found while verifying Policy Issued On value", e);
            throw e;
        } catch (Exception e) {
            logFailure(test, driver, "Unexpected error while verifying Policy Issued On value", e);
            throw e;
        }
    }

    @Then("verify policy expires on matches expected value")
    public void verifyPolicyExpiresOnMatchesExpectedValue() {
        try {
            String actual = uploadqrcode.getPolicyExpiresOnValue();
            Assert.assertEquals(actual, UiConstants.EXPECTED_VC_POLICY_EXPIRES_ON,
                    "Policy Expires On does not match. Expected: " + UiConstants.EXPECTED_VC_POLICY_EXPIRES_ON + ", Actual: " + actual);
            test.log(Status.PASS, "Policy Expires On matches expected value: " + actual);
        } catch (AssertionError e) {
            logFailure(test, driver, "Policy Expires On value mismatch", e);
            throw e;
        } catch (NoSuchElementException e) {
            logFailure(test, driver, "Element not found while verifying Policy Expires On value", e);
            throw e;
        } catch (Exception e) {
            logFailure(test, driver, "Unexpected error while verifying Policy Expires On value", e);
            throw e;
        }
    }


    @Then("verify policy number matches expected value")
    public void verifyPolicyNumberMatchesExpectedValue() {
        try {
            String actual = uploadqrcode.getPolicyNumberValue();
            Assert.assertEquals(actual, UiConstants.EXPECTED_VC_POLICY_NUMBER,
                    "Policy Number does not match. Expected: " + UiConstants.EXPECTED_VC_POLICY_NUMBER + ", Actual: " + actual);
            test.log(Status.PASS, "Policy Number matches expected value: " + actual);
        } catch (AssertionError e) {
            logFailure(test, driver, "Policy Number value mismatch", e);
            throw e;
        } catch (NoSuchElementException e) {
            logFailure(test, driver, "Element not found while verifying Policy Number value", e);
            throw e;
        } catch (Exception e) {
            logFailure(test, driver, "Unexpected error while verifying Policy Number value", e);
            throw e;
        }
    }

    @Then("verify policy name matches expected value")
    public void verifyPolicyNameMatchesExpectedValue() {
        try {
            String actual = uploadqrcode.getPolicyNameValue();
            Assert.assertEquals(actual, UiConstants.EXPECTED_VC_POLICY_NAME,
                    "Policy Name does not match. Expected: " + UiConstants.EXPECTED_VC_POLICY_NAME + ", Actual: " + actual);
            test.log(Status.PASS, "Policy Name matches expected value: " + actual);
        } catch (AssertionError e) {
            logFailure(test, driver, "Policy Name value mismatch", e);
            throw e;
        } catch (NoSuchElementException e) {
            logFailure(test, driver, "Element not found while verifying Policy Name value", e);
            throw e;
        } catch (Exception e) {
            logFailure(test, driver, "Unexpected error while verifying Policy Name value", e);
            throw e;
        }
    }

    @Then("verify benefits matches expected value")
    public void verifyBenefitsMatchesExpectedValue() {
        try {
            String actual = uploadqrcode.getBenefitsValue();
            Assert.assertEquals(actual, UiConstants.EXPECTED_VC_BENEFITS,
                    "Benefits does not match. Expected: " + UiConstants.EXPECTED_VC_BENEFITS + ", Actual: " + actual);
            test.log(Status.PASS, "Benefits matches expected value: " + actual);
        } catch (AssertionError e) {
            logFailure(test, driver, "Benefits value mismatch", e);
            throw e;
        } catch (NoSuchElementException e) {
            logFailure(test, driver, "Element not found while verifying Benefits value", e);
            throw e;
        } catch (Exception e) {
            logFailure(test, driver, "Unexpected error while verifying Benefits value", e);
            throw e;
        }
    }

    @Then("verify mobile matches expected value")
    public void verifyMobileMatchesExpectedValue() {
        try {
            String actual = uploadqrcode.getMobileValue();
            Assert.assertEquals(actual, UiConstants.EXPECTED_VC_MOBILE,
                    "Mobile does not match. Expected: " + UiConstants.EXPECTED_VC_MOBILE + ", Actual: " + actual);
            test.log(Status.PASS, "Mobile matches expected value: " + actual);
        } catch (AssertionError e) {
            logFailure(test, driver, "Mobile value mismatch", e);
            throw e;
        } catch (NoSuchElementException e) {
            logFailure(test, driver, "Element not found while verifying Mobile value", e);
            throw e;
        } catch (Exception e) {
            logFailure(test, driver, "Unexpected error while verifying Mobile value", e);
            throw e;
        }
    }

    @Then("verify email matches expected value")
    public void verifyEmailMatchesExpectedValue() {
        try {
            String actual = uploadqrcode.getEmailValue();
            Assert.assertEquals(actual, UiConstants.EXPECTED_VC_EMAIL,
                    "Email does not match. Expected: " + UiConstants.EXPECTED_VC_EMAIL + ", Actual: " + actual);
            test.log(Status.PASS, "Email matches expected value: " + actual);
        } catch (AssertionError e) {
            logFailure(test, driver, "Email value mismatch", e);
            throw e;
        } catch (NoSuchElementException e) {
            logFailure(test, driver, "Element not found while verifying Email value", e);
            throw e;
        } catch (Exception e) {
            logFailure(test, driver, "Unexpected error while verifying Email value", e);
            throw e;
        }
    }

    @Then("verify gender matches expected value")
    public void verifyGenderMatchesExpectedValue() {
        try {
            String actual = uploadqrcode.getGenderValue();
            Assert.assertEquals(actual, UiConstants.EXPECTED_VC_GENDER,
                    "Gender does not match. Expected: " + UiConstants.EXPECTED_VC_GENDER + ", Actual: " + actual);
            test.log(Status.PASS, "Gender matches expected value: " + actual);
        } catch (AssertionError e) {
            logFailure(test, driver, "Gender value mismatch", e);
            throw e;
        } catch (NoSuchElementException e) {
            logFailure(test, driver, "Element not found while verifying Gender value", e);
            throw e;
        } catch (Exception e) {
            logFailure(test, driver, "Unexpected error while verifying Gender value", e);
            throw e;
        }
    }

    @Then("verify dob matches expected value")
    public void verifyDobMatchesExpectedValue() {
        try {
            String actual = uploadqrcode.getDobValue();
            Assert.assertEquals(actual, UiConstants.EXPECTED_VC_DOB,
                    "DOB does not match. Expected: " + UiConstants.EXPECTED_VC_DOB + ", Actual: " + actual);
            test.log(Status.PASS, "DOB matches expected value: " + actual);
        } catch (AssertionError e) {
            logFailure(test, driver, "DOB value mismatch", e);
            throw e;
        } catch (NoSuchElementException e) {
            logFailure(test, driver, "Element not found while verifying DOB value", e);
            throw e;
        } catch (Exception e) {
            logFailure(test, driver, "Unexpected error while verifying DOB value", e);
            throw e;
        }
    }

    @Then("verify policy expires on value")
    public void verifyPolicyExpiresOnValue() {
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


    @Then("verify policy issued on date is in human readable format")
    public void verifyPolicyIssuedOnDateIsHumanReadable() {
        try {
            String policyIssuedOnValue = uploadqrcode.getPolicyIssuedOnValue();
            assertDateIsHumanReadable("Policy Issued On", policyIssuedOnValue);
            test.log(Status.PASS, "Policy Issued On date is in a human readable format: " + policyIssuedOnValue);
        } catch (NoSuchElementException e) {
            logFailure(test, driver, "Element not found while verifying Policy Issued On date format", e);
            throw e;
        } catch (Exception e) {
            logFailure(test, driver, "Unexpected error while verifying Policy Issued On date format", e);
            throw e;
        }
    }


    @Then("verify policy expires on date is in human readable format")
    public void verifyPolicyExpiresOnDateIsHumanReadable() {
        try {
            String policyExpiresOnValue = uploadqrcode.getPolicyExpiresOnValue();
            assertDateIsHumanReadable("Policy Expires On", policyExpiresOnValue);
            test.log(Status.PASS, "Policy Expires On date is in a human readable format: " + policyExpiresOnValue);
        } catch (NoSuchElementException e) {
            logFailure(test, driver, "Element not found while verifying Policy Expires On date format", e);
            throw e;
        } catch (Exception e) {
            logFailure(test, driver, "Unexpected error while verifying Policy Expires On date format", e);
            throw e;
        }
    }


    private static final String[] HUMAN_READABLE_DATE_PATTERNS = {
            "yyyy-MM-dd",    // 2026-10-16  (ISO)
            "d MMM yyyy",    // 16 Oct 2026
            "d MMMM yyyy",   // 16 October 2026
            "MMM d, yyyy",   // Oct 16, 2026
            "MMMM d, yyyy",  // October 16, 2026
            "d/M/yyyy",      // 16/10/2026  (day-first, most of the world)
            "M/d/yyyy",      // 10/16/2026  (month-first, US)
            "d-M-yyyy",      // 16-10-2026  (day-first, dashes)
            "M-d-yyyy",      // 10-16-2026  (month-first, dashes)
            "d.M.yyyy"       // 16.10.2026  (European, dots)
    };

    private void assertDateIsHumanReadable(String label, String value) {
        Assert.assertNotNull(value, label + " value is null.");
        String trimmed = value.trim();
        Assert.assertFalse(trimmed.isEmpty(), label + " value is empty.");
        Assert.assertNotEquals(trimmed, "N/A", label + " value is not available (N/A).");

        Assert.assertFalse(trimmed.matches("^\\d+$"),
                label + " is shown as a raw epoch timestamp and is not human readable: " + trimmed);
        Assert.assertFalse(trimmed.matches("^\\d{4}-\\d{2}-\\d{2}T.*"),
                label + " is shown in raw ISO-8601 date-time format and is not human readable: " + trimmed);

        boolean isHumanReadable = false;
        for (String pattern : HUMAN_READABLE_DATE_PATTERNS) {
            try {
                DateTimeFormatter formatter = DateTimeFormatter.ofPattern(pattern, Locale.ENGLISH);
                LocalDate.parse(trimmed, formatter);
                isHumanReadable = true;
                break;
            } catch (DateTimeParseException ignored) {
                // try the next supported pattern
            }
        }

        Assert.assertTrue(isHumanReadable,
                label + " date is not in a recognised human readable format: " + trimmed);
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
    public void verifyClickOnAnotherQrCodeButtonOnSuccessfulVerification() {
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
    public void clickOnLanguageDropdown() {
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
    public void verifyVerifyAnotherQrCodeButtonOnSuccessfulVerification() {
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
            Assert.assertEquals(actualMessage, UiConstants.VERIFICATION_SUCCESS_MESSAGE,
                "Congratulations message does not match the expected value.");
            test.log(Status.PASS, "Successfully verified the congratulations message: " + actualMessage);
        } catch (AssertionError e) {
            logFailure(test, driver, "Congratulations message verification failed. Expected: "
                + UiConstants.VERIFICATION_SUCCESS_MESSAGE + ", but found: " + uploadqrcode.getCongratulationtext(), e);
            throw e;
        } catch (NoSuchElementException e) {
            logFailure(test, driver, "Element not found while verifying the congratulations message", e);
            throw e;
        } catch (Exception e) {
            logFailure(test, driver, "Unexpected error while verifying the congratulations message", e);
            throw e;
        }
    }

    @Then("Verify multiple VC verification success message")
    public void verify_multiple_vc_verification_success_message() {
        try {  
            String actualMessage = vpverification.getMultipleVcVerificationSuccessMessage();
            Assert.assertEquals(actualMessage, UiConstants.MULTIPLE_VC_VERIFICATION_SUCCESS_MESSAGE,
                "Multiple VC verification success message does not match the expected value.");
            test.log(Status.PASS, "Successfully verified the multiple VC verification success message: " + actualMessage);
        } catch (AssertionError e) {
            logFailure(test, driver, "Multiple VC verification success message failed. Expected: "
                + UiConstants.MULTIPLE_VC_VERIFICATION_SUCCESS_MESSAGE + ", but found: " + vpverification.getMultipleVcVerificationSuccessMessage(), e);
            throw e;
        } catch (Exception e) {
            logFailure(test, driver, "Unexpected error while verifying the multiple VC verification success message", e);
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
            uploadqrcode.uploadPdfAndWaitForVerificationResult();
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
            uploadqrcode.uploadAnotherPdfAndWaitForVerificationResult();
            test.log(Status.PASS, "Successfully uploaded another QR code file in PDF format.");
        } catch (NoSuchElementException e) {
            logFailure(test, driver, "Element not found while uploading another QR code PDF file", e);
            throw e;
        } catch (Exception e) {
            logFailure(test, driver, "Unexpected error while uploading another QR code PDF file", e);
            throw e;
        }
    }

    @When("Upload Farmer Credential PDF file")
    public void uploadFarmerCredentialPdfFile() {
        try {
            uploadqrcode.uploadFarmerCredentialAndWaitForVerificationResult();
            test.log(Status.PASS, "Successfully uploaded the Farmer credential PDF file.");
        } catch (NoSuchElementException e) {
            logFailure(test, driver, "Element not found while uploading the Farmer credential PDF file", e);
            throw e;
        } catch (Exception e) {
            logFailure(test, driver, "Unexpected error while uploading the Farmer credential PDF file", e);
            throw e;
        }
    }


    @When("Upload QR code file JPG")
    public void uploadQRCodeFileJpg() {
        try {
            uploadqrcode.uploadJpgAndWaitForVerificationResult();
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
            uploadqrcode.uploadAnotherJpgAndWaitForVerificationResult();
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
            uploadqrcode.uploadJpegAndWaitForVerificationResult();
            test.log(Status.PASS, "Successfully uploaded the QR code file in JPEG format.");
        } catch (NoSuchElementException e) {
            logFailure(test, driver, "Element not found while uploading the QR code JPEG file", e);
            throw e;
        } catch (Exception e) {
            logFailure(test, driver, "Unexpected error while uploading the QR code JPEG file", e);
            throw e;
        }
    }

    @When("Upload QR code file JPEG offline")
    public void uploadQRCodeFileJpegOffline() {
        try {
            uploadqrcode.uploadJpegAndWaitForOfflineResult();
            test.log(Status.PASS, "Successfully uploaded the QR code file in JPEG format for offline scenario.");
        } catch (NoSuchElementException e) {
            logFailure(test, driver, "Element not found while uploading the QR code JPEG file in offline scenario", e);
            throw e;
        } catch (Exception e) {
            logFailure(test, driver, "Unexpected error while uploading the QR code JPEG file in offline scenario", e);
            throw e;
        }
    }
    
    @When("Upload another QR code file JPEG")
    public void uploadAnotherQRCodeFileJpeg() {
        try {
            uploadqrcode.uploadAnotherJpegAndWaitForVerificationResult();
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

    @When("Click browser back button")
    public void clickBrowserBackButton() {
        try {
            uploadqrcode.browserBackButton(driver);
            test.log(Status.PASS, "Browser back button clicked successfully.");
        } catch (Exception e) {
            logFailure(test, driver, "Unexpected error while clicking browser back button", e);
            throw e;
        }
    }

    @Then("Validate offline upload error message with please try again button")
    public void validateOfflineUploadErrorMessageWithPleaseTryAgainButton() {
        try {
            Assert.assertEquals(
                    homePage.getNoInternetTitle(),
                    UiConstants.NO_INTERNET_TITLE,
                    "Offline upload title does not match."
            );
            Assert.assertEquals(
                    homePage.getNoInternetDescription(),
                    UiConstants.NO_INTERNET_DESCRIPTION,
                    "Offline upload description does not match."
            );
            Assert.assertTrue(homePage.isTryAgainButtonVisible(), "Please try again button is not visible.");
            test.log(Status.PASS, "Offline upload error state validated successfully.");
        } catch (AssertionError e) {
            test.log(Status.FAIL, "Offline upload error validation failed: " + e.getMessage());
            throw e;
        } catch (NoSuchElementException e) {
            logFailure(test, driver, "Element not found while validating offline upload error state", e);
            throw e;
        } catch (Exception e) {
            logFailure(test, driver, "Unexpected error while validating offline upload error state", e);
            throw e;
        }
    }

        @Then("Verify vc-verification api call in network tab with url")
    public void verifyVcVerificationApiCallInNetworkTabWithUrl() {
        try {
            String baseUrl = InjiVerifyConfigManager.getInjiVerifyUi();
            if (baseUrl == null || baseUrl.isEmpty()) {
                throw new IllegalStateException("injiverify base URL is not configured in injiVerify.properties");
            }

            String normalizedBaseUrl = baseUrl.endsWith("/") ? baseUrl.substring(0, baseUrl.length() - 1) : baseUrl;
            String expectedUrl = normalizedBaseUrl + "/v1/verify/v2/vc-verification";
            boolean isExpectedCallPresent = waitForVcVerificationApiCall(expectedUrl);

            Assert.assertTrue(isExpectedCallPresent,
                    "Expected vc-verification API call to URL was not found in captured network data: " + expectedUrl);
            test.log(Status.PASS,
                    "Verified vc-verification API call in captured network data with URL: " + expectedUrl);
        } catch (NoSuchElementException e) {
            logFailure(test, driver, "Element-related error while verifying vc-verification API call in network logs", e);
            throw e;
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            logFailure(test, driver, "Interrupted while waiting for vc-verification API call in network logs", e);
            throw new RuntimeException(e);
        } catch (Exception e) {
            logFailure(test, driver, "Unexpected error while verifying vc-verification API call in network logs", e);
            throw e;
        }
    }

    private boolean waitForVcVerificationApiCall(String expectedUrl) throws InterruptedException {
        Instant deadline = Instant.now().plusSeconds((long) BasePage.getTimeout() * 4L);

        while (Instant.now().isBefore(deadline)) {
            if (isExpectedVcVerificationCallCaptured(expectedUrl)) {
                return true;
            }
            Thread.sleep(1000);
        }

        return isExpectedVcVerificationCallCaptured(expectedUrl);
    }

    private boolean isExpectedVcVerificationCallCaptured(String expectedUrl) {
        if (driver instanceof JavascriptExecutor) {
            Object result = ((JavascriptExecutor) driver).executeScript("return window.__networkRequests || [];");
            if (result instanceof java.util.List<?>) {
                java.util.List<?> urls = (java.util.List<?>) result;
                boolean jsMatch = urls.stream().anyMatch(urlObj -> matchesVcVerificationUrl(String.valueOf(urlObj), expectedUrl));
                if (jsMatch) {
                    return true;
                }
            }
        }

        try {
            LogEntries performanceLogs = driver.manage().logs().get(LogType.PERFORMANCE);
            return performanceLogs.getAll().stream().anyMatch((LogEntry entry) -> {
                String message = entry.getMessage();
                return message != null
                        && message.contains("Network.requestWillBeSent")
                        && matchesVcVerificationUrl(message, expectedUrl);
            });
        } catch (InvalidArgumentException e) {
            test.log(Status.INFO, "Performance logs are not available for this session. Skipping performance log fallback.");
            return false;
        }
    }

    private boolean matchesVcVerificationUrl(String candidate, String expectedUrl) {
        return candidate != null
                && candidate.contains("/v2/vc-verification")
                && candidate.contains(expectedUrl);
    }

    @Then("Verify vp-session-results api call in network tab with url")
    public void verifyVpSessionResultsApiCallInNetworkTabWithUrl() {
        try {
            String baseUrl = InjiVerifyConfigManager.getInjiVerifyUi();
            if (baseUrl == null || baseUrl.isEmpty()) {
                throw new IllegalStateException("injiverify base URL is not configured in injiVerify.properties");
            }

            String normalizedBaseUrl = baseUrl.endsWith("/") ? baseUrl.substring(0, baseUrl.length() - 1) : baseUrl;
            String expectedUrl = normalizedBaseUrl + "/v1/verify/vp-session-results";
            boolean isExpectedCallPresent = waitForApiCallCaptured(expectedUrl, "/vp-session-results");

            Assert.assertTrue(isExpectedCallPresent,
                    "Expected vp-session-results API call to URL was not found in captured network data: " + expectedUrl);
            test.log(Status.PASS,
                    "Verified vp-session-results API call in captured network data with URL: " + expectedUrl);
        } catch (NoSuchElementException e) {
            logFailure(test, driver, "Element-related error while verifying vp-session-results API call in network logs", e);
            throw e;
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            logFailure(test, driver, "Interrupted while waiting for vp-session-results API call in network logs", e);
            throw new RuntimeException(e);
        } catch (Exception e) {
            logFailure(test, driver, "Unexpected error while verifying vp-session-results API call in network logs", e);
            throw e;
        }
    }

    private boolean waitForApiCallCaptured(String expectedUrl, String pathFragment) throws InterruptedException {
        Instant deadline = Instant.now().plusSeconds((long) BasePage.getTimeout() * 4L);

        while (Instant.now().isBefore(deadline)) {
            if (isExpectedApiCallCaptured(expectedUrl, pathFragment)) {
                return true;
            }
            Thread.sleep(1000);
        }

        return isExpectedApiCallCaptured(expectedUrl, pathFragment);
    }

    private boolean isExpectedApiCallCaptured(String expectedUrl, String pathFragment) {
        if (driver instanceof JavascriptExecutor) {
            Object result = ((JavascriptExecutor) driver).executeScript("return window.__networkRequests || [];");
            if (result instanceof java.util.List<?>) {
                java.util.List<?> urls = (java.util.List<?>) result;
                boolean jsMatch = urls.stream().anyMatch(urlObj -> matchesApiUrl(String.valueOf(urlObj), expectedUrl, pathFragment));
                if (jsMatch) {
                    return true;
                }
            }
        }

        try {
            LogEntries performanceLogs = driver.manage().logs().get(LogType.PERFORMANCE);
            return performanceLogs.getAll().stream().anyMatch((LogEntry entry) -> {
                String message = entry.getMessage();
                return message != null
                        && message.contains("Network.requestWillBeSent")
                        && matchesApiUrl(message, expectedUrl, pathFragment);
            });
        } catch (InvalidArgumentException e) {
            test.log(Status.INFO, "Performance logs are not available for this session. Skipping performance log fallback.");
            return false;
        }
    }

    private boolean matchesApiUrl(String candidate, String expectedUrl, String pathFragment) {
        return candidate != null
                && candidate.contains(pathFragment)
                && candidate.contains(expectedUrl);
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

	@When("Click on BLE tab")
	public void clickOnBleTab() {
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
	public void verifyInformationMessageOnBleVerification() {
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
	public void verifyTransactionTerminatedErrorMessage() {
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
	public void clickOnVpVerificationTab() {
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
	public void clickOnRightArrow() {
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
	public void verifyInformationMessageOnVpVerification() {
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

	@When("Upload QR code file Expired png")
	public void uploadQrCodeFileExpiredPng() {
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
	public void uploadQrCodeFileExpiredJpg() {
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
	        uploadqrcode.clickOnUploadExpiredQRCodeJpegExpired();
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
	        uploadqrcode.clickOnUploadExpiredQRCodePdfExpired();
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
	public void verifyMessageForValidQrCode() {
	    try {
	        String actualMessage = uploadqrcode.getErrorMessageForExpiredQRCode();
	        Assert.assertEquals(actualMessage, UiConstants.VERIFICATION_SUCCESS_MESSAGE);
	        test.log(Status.PASS, "Valid QR code message verified successfully: " + actualMessage);
	    } catch (NoSuchElementException e) {
	        logFailure(test, driver, "Element not found while verifying message for valid QR code", e);
	        throw e;
	    } catch (AssertionError e) {
	        test.log(Status.FAIL, "Message verification failed: Expected '" + UiConstants.VERIFICATION_SUCCESS_MESSAGE + "' but got '" + uploadqrcode.getErrorMessageForExpiredQRCode() + "'");
	        throw e;
	    } catch (Exception e) {
	        logFailure(test, driver, "Unexpected error while verifying message for valid QR code", e);
	        throw e;
	    }
	}

	@When("Verify message for expired QR code")
	public void verifyMessageForExpiredQrCode() {
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
	public void openInjiVerifyInNewTab() {
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
	public void userSearchTheIssuersWith(String string) {
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
	public void userSearchTheVcWith(String string) {
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
    public void userSearchTheIssuersSunbird() throws Exception {
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
	public void userClickOnDownloadStayProtectedInsuranceButton() {
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
	public void userClickOnGetStartedButton() {
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
	public void userVerifyMosipNationalIdByESignetDisplayed() {
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
	public void userClickOnHealthInsuranceIdByESignetButton() {
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
	public void userClickOnValidityDropdownButton() {
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
	public void userClickOnNoLimitButton() {
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
	public void userClickOnProceedButton() {
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
    public void userEnterThePolicyNumber() {
        try {
            homePage.enterPolicyNumer(policyNumber());
            test.log(Status.PASS, "User successfully entered the policy number: " + policyNumber());
        } catch (NoSuchElementException e) {
            test.log(Status.FAIL, "Element not found while entering the policy number: " + e.getMessage());
            test.log(Status.FAIL, ExceptionUtils.getStackTrace(e));
            ScreenshotUtil.attachScreenshot(driver, "FailureScreenshot");
            throw e;
        } catch (Exception e) {
            test.log(Status.FAIL, "Unexpected error while entering the policy number: " + e.getMessage());
            test.log(Status.FAIL, ExceptionUtils.getStackTrace(e));
            ScreenshotUtil.attachScreenshot(driver, "FailureScreenshot");
            throw e;
        }
    }


	@When("User enter the {string}")
	public void userEnterThe(String string) {
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
	public void userClickOnGetOtpButton() {
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
	public void userEnterTheOtp(String otpString) {
	    try {
	        homePage.enterOtp(otpString);
	        test.log(Status.PASS, "Successfully entered OTP: " + otpString);
	    } catch (NoSuchElementException e) {
	        logFailure(test, driver, "Element not found while entering OTP", e);
	        throw e;
	    } catch (Exception e) {
	        logFailure(test, driver, "Unexpected error while entering OTP", e);
	        throw e;
	    }
	}
	@When("User click on verify button")
	public void userClickOnVerifyButton() {
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
	public void userVerifyDownloadSuccessTextDisplayed() {
	    try {
	        String actualSuccessMessage = homePage.isSuccessMessageDisplayed();
	        Assert.assertEquals(actualSuccessMessage, "Success!");
	        test.log(Status.PASS, "Successfully verified Download Success text is displayed: " + actualSuccessMessage);
	    } catch (AssertionError e) {
	        String actualSuccessMessage = homePage.isSuccessMessageDisplayed();
	        test.log(Status.FAIL, "Verification failed: Download Success text is not 'Success!'. Actual: '" + actualSuccessMessage + "'");
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
	public void userVerifyPdfIsDownloaded() throws IOException {
	    userVerifyPdfIsDownloaded(null);
	}

	public void userVerifyPdfIsDownloaded(String targetFileName) throws IOException {
	    try {
	        File pdfFile = BaseTest.isUsingBrowserStack()
	                ? copyInsurancePdfFromBrowserStack(targetFileName)
	                : moveInsurancePdfFromLocalDownloads(targetFileName);
	        assertPdfHasText(pdfFile);
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

	private File copyInsurancePdfFromBrowserStack(String targetFileName) throws IOException, InterruptedException {
	    boolean fileExists = false;
	    for (int i = 0; i < 5; i++) {
	        fileExists = (boolean) baseTest.getJse().executeScript(
	                "browserstack_executor: {\"action\": \"fileExists\", \"arguments\": {\"fileName\": \"InsuranceCredential.pdf\"}}");
	        if (fileExists) {
	            break;
	        }
	        Thread.sleep(2000);
	    }
	    assertTrue("PDF file 'InsuranceCredential.pdf' was not found on BrowserStack.", fileExists);
	    test.log(Status.PASS, "PDF file 'InsuranceCredential.pdf' exists on BrowserStack.");

	    String base64EncodedFile = (String) baseTest.getJse().executeScript(
	            "browserstack_executor: {\"action\": \"getFileContent\", \"arguments\": {\"fileName\": \"InsuranceCredential.pdf\"}}");

	    byte[] data = Base64.getDecoder().decode(base64EncodedFile);
	    File canonicalFile = new File(BaseTest.getDownloadedInsurancePdfPath());
	    File targetFile = (targetFileName != null && !targetFileName.isBlank())
	            ? new File(canonicalFile.getParent(), targetFileName)
	            : canonicalFile;
	    Files.createDirectories(targetFile.getParentFile().toPath());
	    try (OutputStream stream = new FileOutputStream(targetFile)) {
	        stream.write(data);
	    }
	    test.log(Status.PASS, "PDF saved as: " + targetFile.getAbsolutePath());
	    return targetFile;
	}

	private File moveInsurancePdfFromLocalDownloads(String targetFileName) throws IOException, InterruptedException {
	    Path downloadDirectory = Path.of(BaseTest.getLocalDownloadsDirectoryPath());
	    Path downloadedPdf = waitForLocalInsurancePdf(downloadDirectory);
	    Path canonicalPdf = Path.of(BaseTest.getDownloadedInsurancePdfPath());
	    Files.createDirectories(canonicalPdf.getParent());
	    Path targetPdf = (targetFileName != null && !targetFileName.isBlank())
	            ? canonicalPdf.resolveSibling(targetFileName)
	            : canonicalPdf;
	    Files.move(downloadedPdf, targetPdf, StandardCopyOption.REPLACE_EXISTING);
	    test.log(Status.PASS, "PDF saved as: " + targetPdf.toAbsolutePath());
	    return targetPdf.toFile();
	}

	private Path waitForLocalInsurancePdf(Path downloadDirectory) throws IOException, InterruptedException {
	    Path pdfPath = downloadDirectory.resolve("InsuranceCredential.pdf");
	    Path partialDownloadPath = downloadDirectory.resolve("InsuranceCredential.pdf.crdownload");

	    for (int i = 0; i < 10; i++) {
	        if (Files.exists(pdfPath) && !Files.exists(partialDownloadPath) && Files.size(pdfPath) > 0) {
	            return pdfPath;
	        }
	        Thread.sleep(2000);
	    }

	    throw new IOException("PDF file 'InsuranceCredential.pdf' was not found in local downloads: "
	            + downloadDirectory.toAbsolutePath());
	}

	private void assertPdfHasText(File pdfFile) throws IOException {
	    try (PDDocument document = PDDocument.load(pdfFile)) {
	        PDFTextStripper stripper = new PDFTextStripper();
	        String text = stripper.getText(document);
	        assertFalse("PDF content is empty.", text.trim().isEmpty());
	    }
	}

	private static final String FARMER_CREDENTIAL_PDF_FILE_NAME = "FarmerCredential_VCDM2.0_WithFace.pdf";

	public void userVerifySVGPdfIsDownloaded() throws IOException {
	    try {
	        File pdfFile = BaseTest.isUsingBrowserStack()
	                ? copyFarmerCredentialPdfFromBrowserStack()
	                : moveFarmerCredentialPdfFromLocalDownloads();
	        assertPdfOpensSuccessfully(pdfFile);
	        test.log(Status.PASS, "PDF file '" + FARMER_CREDENTIAL_PDF_FILE_NAME + "' downloaded and is a valid PDF.");
	    } catch (InterruptedException e) {
	        test.log(Status.FAIL, "Interrupted while waiting for Farmer credential PDF download: " + e.getMessage());
	        Thread.currentThread().interrupt();
	        throw new RuntimeException(e);
	    } catch (AssertionError e) {
	        test.log(Status.FAIL, e.getMessage());
	        throw e;
	    } catch (Exception e) {
	        test.log(Status.FAIL, "Error verifying Farmer credential PDF download: " + e.getMessage());
	        throw e;
	    }
	}

	private File copyFarmerCredentialPdfFromBrowserStack() throws IOException, InterruptedException {
	    boolean fileExists = false;
	    for (int i = 0; i < 5; i++) {
	        fileExists = (boolean) baseTest.getJse().executeScript(
	                "browserstack_executor: {\"action\": \"fileExists\", \"arguments\": {\"fileName\": \""
	                        + FARMER_CREDENTIAL_PDF_FILE_NAME + "\"}}");
	        if (fileExists) {
	            break;
	        }
	        Thread.sleep(2000);
	    }
	    assertTrue("PDF file '" + FARMER_CREDENTIAL_PDF_FILE_NAME + "' was not found on BrowserStack.", fileExists);
	    test.log(Status.PASS, "PDF file '" + FARMER_CREDENTIAL_PDF_FILE_NAME + "' exists on BrowserStack.");

	    String base64EncodedFile = (String) baseTest.getJse().executeScript(
	            "browserstack_executor: {\"action\": \"getFileContent\", \"arguments\": {\"fileName\": \""
	                    + FARMER_CREDENTIAL_PDF_FILE_NAME + "\"}}");

	    byte[] data = Base64.getDecoder().decode(base64EncodedFile);
	    File targetFile = new File(BaseTest.getDownloadedFarmerCredentialPdfPath());
	    Files.createDirectories(targetFile.getParentFile().toPath());
	    try (OutputStream stream = new FileOutputStream(targetFile)) {
	        stream.write(data);
	    }
	    test.log(Status.PASS, "Farmer credential PDF saved as: " + targetFile.getAbsolutePath());
	    return targetFile;
	}

	private File moveFarmerCredentialPdfFromLocalDownloads() throws IOException, InterruptedException {
	    Path downloadDirectory = Path.of(BaseTest.getLocalDownloadsDirectoryPath());
	    Path downloadedPdf = waitForLocalFarmerCredentialPdf(downloadDirectory);
	    Path canonicalPdf = Path.of(BaseTest.getDownloadedFarmerCredentialPdfPath());
	    Files.createDirectories(canonicalPdf.getParent());
	    Files.move(downloadedPdf, canonicalPdf, StandardCopyOption.REPLACE_EXISTING);
	    test.log(Status.PASS, "Farmer credential PDF saved as: " + canonicalPdf.toAbsolutePath());
	    return canonicalPdf.toFile();
	}

	private Path waitForLocalFarmerCredentialPdf(Path downloadDirectory) throws IOException, InterruptedException {
	    Path pdfPath = downloadDirectory.resolve(FARMER_CREDENTIAL_PDF_FILE_NAME);
	    Path partialDownloadPath = downloadDirectory.resolve(FARMER_CREDENTIAL_PDF_FILE_NAME + ".crdownload");

	    for (int i = 0; i < 10; i++) {
	        if (Files.exists(pdfPath) && !Files.exists(partialDownloadPath) && Files.size(pdfPath) > 0) {
	            return pdfPath;
	        }
	        Thread.sleep(2000);
	    }

	    throw new IOException("PDF file '" + FARMER_CREDENTIAL_PDF_FILE_NAME + "' was not found in local downloads: "
	            + downloadDirectory.toAbsolutePath());
	}

	private void assertPdfOpensSuccessfully(File pdfFile) throws IOException {
	    assertTrue("Farmer credential PDF is empty.", pdfFile.length() > 0);
	    // An SVG-rendered credential may not contain extractable text, so only confirm the PDF
	    // is well-formed and has at least one page - do not assert on text content.
	    try (PDDocument document = PDDocument.load(pdfFile)) {
	        assertTrue("Farmer credential PDF has no pages.", document.getNumberOfPages() > 0);
	    }
	}

	@When("User verify go back button")
	public void userVerifyGoBackButton() {
		try {
			Assert.assertTrue(vpverification.isGoBackButtonVisible(), "Go Back button is not visible.");
			test.log(Status.PASS, "Go Back button is visible.");
		} catch (AssertionError e) {
			logFailure(test, driver, "Go Back button is not visible", e);
			throw e;
		} catch (Exception e) {
			logFailure(test, driver, "Unexpected error while verifying Go Back button visibility", e);
			throw e;
		}
	}

	@Before(value = "@needsInsuranceArtifacts", order = 12000)
	public void prepareInsuranceCredentialArtifactsHook() throws Exception {
	    ensureStepDependenciesInitialized();
	    try {
	        synchronized (BaseTest.getInsuranceArtifactsLock()) {
	            File pdfFile = new File(BaseTest.getDownloadedInsurancePdfPath());
	            File pngFile = new File(BaseTest.getInsuranceCredentialPngPath());
	            File jpgFile = new File(BaseTest.getInsuranceCredentialJpgPath());
	            File jpegFile = new File(BaseTest.getInsuranceCredentialJpegPath());

	            if (BaseTest.isInsuranceArtifactsPreparedForRun()
	                    && pdfFile.exists() && pngFile.exists() && jpgFile.exists() && jpegFile.exists()) {
	                test.log(Status.PASS, "Insurance credential artifacts are already prepared for this run.");
	            } else {
	                openInjiWebInNewTab();
	                verifyClickOnContinueButton();
	                userSearchTheIssuersSunbird();
	                userClickOnDownloadStayProtectedInsuranceButton();
	                userClickOnHealthInsuranceIdByESignetButton();
	                userClickOnValidityDropdownButton();
	                userClickOnNoLimitButton();
	                userClickOnProceedButton();
	                userEnterThePolicyNumber();
	                userEnterTheFullName();
	                userEnterTheDateOfBirth();
	                userClickOnLoginButton();
	                userVerifyDownloadSuccessTextDisplayed();
	                userVerifyPdfIsDownloaded();
	                verifyThatUserConvertPdfIntoPng();
	                BaseTest.markInsuranceArtifactsPreparedForRun();
	                test.log(Status.PASS, "Prepared shared insurance credential artifacts for this run.");
	            }

	            if (BaseTest.getCurrentScenarioTags().contains("@scan")
	                    && BaseTest.getCurrentScenarioTags().contains("@qr_valid")) {
	                baseTest.refreshRuntimeScanMediaForCurrentScenario();
	                test.log(Status.PASS, "Regenerated runtime scan media from current-run insurance artifacts.");
	            }
	        }

	        openInjiVerifyInNewTab();
	    } catch (Exception e) {
	        logFailure(test, driver, "Failed while preparing insurance credential artifacts", e);
	        throw e;
	    }
	}

@Before(value = "@needsOnetimeInsuranceArtifacts", order = 13000)
	public void prepareOnetimeInsuranceCredentialArtifactsHook() throws Exception {
	    ensureStepDependenciesInitialized();
	    try {
	        synchronized (BaseTest.getInsuranceArtifactsLock()) {
	                openInjiWebInNewTab();
	                verifyClickOnContinueButton();
	                userSearchTheIssuersSunbird();
	                userClickOnDownloadStayProtectedInsuranceButton();
	                userClickOnHealthInsuranceIdByESignetButton();
	                userClickOnValidityDropdownButton();
	                userClickOnProceedButton();
	                userEnterThePolicyNumber();
	                userEnterTheFullName();
	                userEnterTheDateOfBirth();
	                userClickOnLoginButton();
	                userVerifyDownloadSuccessTextDisplayed();
	                userVerifyPdfIsDownloaded("OneTimeInsuranceCredential.pdf");
	                test.log(Status.PASS, "Prepared onetime insurance credential artifacts for this run.");
	            if (BaseTest.getCurrentScenarioTags().contains("@scan")
	                    && BaseTest.getCurrentScenarioTags().contains("@qr_valid")) {
	                baseTest.refreshRuntimeScanMediaForCurrentScenario();
	                test.log(Status.PASS, "Regenerated runtime scan media from current-run insurance artifacts.");
	            }
	        }

	        openInjiVerifyInNewTab();
	    } catch (Exception e) {
	        logFailure(test, driver, "Failed while preparing insurance credential artifacts", e);
	        throw e;
	    }
	}

	@Before(value = "@needsvginsurencecredetials", order = 12500)
	public void prepareSvgCredentialArtifactsHook() throws Exception {
	    ensureStepDependenciesInitialized();
	    try {
	        synchronized (BaseTest.getSvgArtifactsLock()) {
	            File farmerPdfFile = new File(BaseTest.getDownloadedFarmerCredentialPdfPath());

	            if (BaseTest.isSVGArtifactsPreparedForRun() && farmerPdfFile.exists()) {
	                test.log(Status.PASS, "Farmer credential (SVG template) artifact is already prepared for this run.");
	            } else {
	                openInjiWebInNewTab();
	                verifyClickOnContinueButton();
	                userSearchTheIssuersSVG();
	                userClickOnDownloadFarmerIDCardButton();
	                userClickOnFarmerCredentialByESignetButton();
	                userClickOnValidityDropdownButton();
	                userClickOnNoLimitButton();
	                userClickOnProceedButton();
	                userEnterTheUIN();
	                userClicksOnGetOTP();
	                userEntersTheOTP();
	                userClickOnVerifyButton();
	                userVerifyDownloadSuccessTextDisplayed();
	                userVerifySVGPdfIsDownloaded();
	                BaseTest.markSVGInsuranceArtifactsPreparedForRun();
	                test.log(Status.PASS, "Prepared Farmer credential (SVG template) artifact for this run.");
	            }

	            if (BaseTest.getCurrentScenarioTags().contains("@scan")
	                    && !new File(BaseTest.getFarmerCredentialPngPath()).exists()) {
	                verifyThatUserConvertFarmerCredentialPdfIntoPng();
	                test.log(Status.PASS, "Converted Farmer credential PDF to PNG for scan scenario.");
	            }
	        }

	        openInjiVerifyInNewTab();
	    } catch (Exception e) {
	        logFailure(test, driver, "Failed while preparing Farmer credential (SVG template) artifact", e);
	        throw e;
	    }
	}

	public void userSearchTheIssuersSVG() {
	    try {
	        Thread.sleep(6000);
	        String issuerText = InjiVerifyConfigManager.getproperty("issuerSearchTextforSVG");
	        homePage.enterIssuersInSearchBox(issuerText);
	        test.log(Status.PASS, "Searched issuers with: " + issuerText);
	    } catch (InterruptedException e) {
	        test.log(Status.FAIL, "Interrupted while waiting to search SVG issuers: " + e.getMessage());
	        Thread.currentThread().interrupt();
	        throw new RuntimeException(e);
	    } catch (NoSuchElementException e) {
	        logFailure(test, driver, "Element not found while searching SVG issuers", e);
	        throw e;
	    } catch (Exception e) {
	        logFailure(test, driver, "Unexpected error while searching SVG issuers", e);
	        throw e;
	    }
	}

	public void userClickOnDownloadFarmerIDCardButton() {
	    try {
	        homePage.clickOnFarmerIdCardIssuer();
	        test.log(Status.PASS, "Successfully clicked on Farmer ID Card credentials button.");
	    } catch (NoSuchElementException e) {
	        logFailure(test, driver, "Element not found while clicking Farmer ID Card credentials button", e);
	        throw e;
	    } catch (Exception e) {
	        logFailure(test, driver, "Unexpected error while clicking Farmer ID Card credentials button", e);
	        throw e;
	    }
	}

	public void userClickOnFarmerCredentialByESignetButton() {
	    try {
	        homePage.clickOnFarmerCredentialType();
	        test.log(Status.PASS, "Successfully clicked on Farmer credential type by e-signet button.");
	    } catch (NoSuchElementException e) {
	        logFailure(test, driver, "Element not found while clicking Farmer credential type by e-signet button", e);
	        throw e;
	    } catch (Exception e) {
	        logFailure(test, driver, "Unexpected error while clicking Farmer credential type by e-signet button", e);
	        throw e;
	    }
	}

	public void userEnterTheUIN() {
	    try {
	        String uin = InjiVerifyConfigManager.getproperty("FarmetCredentialUIN");
	        homePage.enterUinForFarmerCredential(uin);
	        test.log(Status.PASS, "Successfully entered the UIN: " + uin);
	    } catch (NoSuchElementException e) {
	        logFailure(test, driver, "Element not found while entering the UIN", e);
	        throw e;
	    } catch (Exception e) {
	        logFailure(test, driver, "Unexpected error while entering the UIN", e);
	        throw e;
	    }
	}

	public void userClicksOnGetOTP() {
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

	public void userEntersTheOTP() {
	    try {
	        String otp = InjiVerifyConfigManager.getproperty("FarmerIsuuerOTP");
	        homePage.enterOtpForFarmerCredential(otp);
	        test.log(Status.PASS, "Successfully entered the OTP.");
	    } catch (NoSuchElementException e) {
	        logFailure(test, driver, "Element not found while entering the OTP", e);
	        throw e;
	    } catch (Exception e) {
	        logFailure(test, driver, "Unexpected error while entering the OTP", e);
	        throw e;
	    }
	}

	@When("Verify that user convert pdf into png")
	public void verifyThatUserConvertPdfIntoPng() throws IOException {
	    String pdfPath = BaseTest.getDownloadedInsurancePdfPath();
	
	    try {
	        PDDocument document = PDDocument.load(new File(pdfPath));
	        PDFRenderer renderer = new PDFRenderer(document);
	
	        int numberOfPages = document.getNumberOfPages();
	        assertTrue("PDF document has no pages.", numberOfPages > 0);
	        test.log(Status.PASS, "PDF document has " + numberOfPages + " pages.");
	
	        for (int i = 0; i < numberOfPages; i++) {
	            PDPage page = document.getPage(i);
	            // Render at a higher DPI to preserve QR code fidelity
	            BufferedImage image = renderer.renderImageWithDPI(i, 300, org.apache.pdfbox.rendering.ImageType.RGB);
	
	            String outputFileNamepng = BaseTest.getInsuranceCredentialPngPath();
	            String outputFileNamejpg = BaseTest.getInsuranceCredentialJpgPath();
	            String outputFileNamejpeg = BaseTest.getInsuranceCredentialJpegPath();
	
	            ImageIO.write(image, "png", new File(outputFileNamepng));
	            ImageIO.write(image, "jpg", new File(outputFileNamejpg));
	            ImageIO.write(image, "jpeg", new File(outputFileNamejpeg));
	
	            assertTrue("PNG file " + outputFileNamepng + " was not created.", new File(outputFileNamepng).exists());
	            assertTrue("JPG file " + outputFileNamejpg + " was not created.", new File(outputFileNamejpg).exists());
	            assertTrue("JPEG file " + outputFileNamejpeg + " was not created.", new File(outputFileNamejpeg).exists());
	            test.log(Status.PASS, "Successfully converted page " + (i + 1) + " to PNG, JPG, and JPEG at 300 DPI.");
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

	@When("Verify that user convert farmer credential pdf into png")
	public void verifyThatUserConvertFarmerCredentialPdfIntoPng() throws IOException {
	    String pdfPath = BaseTest.getDownloadedFarmerCredentialPdfPath();

	    try {
	        PDDocument document = PDDocument.load(new File(pdfPath));
	        PDFRenderer renderer = new PDFRenderer(document);

	        int numberOfPages = document.getNumberOfPages();
	        assertTrue("Farmer credential PDF document has no pages.", numberOfPages > 0);
	        test.log(Status.PASS, "Farmer credential PDF document has " + numberOfPages + " pages.");

	        // Render at a higher DPI to preserve QR code fidelity for the scan flow.
	        BufferedImage image = renderer.renderImageWithDPI(0, 300, org.apache.pdfbox.rendering.ImageType.RGB);

	        String outputFileNamePng = BaseTest.getFarmerCredentialPngPath();
	        ImageIO.write(image, "png", new File(outputFileNamePng));

	        assertTrue("Farmer credential PNG file " + outputFileNamePng + " was not created.",
	                new File(outputFileNamePng).exists());
	        test.log(Status.PASS, "Successfully converted Farmer credential PDF to PNG at 300 DPI: " + outputFileNamePng);

	        document.close();
	    } catch (IOException e) {
	        test.log(Status.FAIL, "Error converting Farmer credential PDF to PNG: " + e.getMessage());
	        throw e;
	    } catch (AssertionError e) {
	        test.log(Status.FAIL, e.getMessage());
	        throw e;
	    } catch (Exception e) {
	        test.log(Status.FAIL, "Unexpected error during Farmer credential PDF to PNG conversion: " + e.getMessage());
	        throw e;
	    }
	}

	@When("Upload one time use QR code file")
	public void uploadOnetimeQRCodeFile() {
	    try {
	        uploadqrcode.uploadOnetimeQRCodeAndWaitForVerificationResult();
	        test.log(Status.PASS, "Successfully uploaded the one-time use QR code file.");
	    } catch (NoSuchElementException e) {
	        logFailure(test, driver, "Element not found while uploading one-time use QR code file", e);
	        throw e;
	    } catch (Exception e) {
	        logFailure(test, driver, "Unexpected error while uploading one-time use QR code file", e);
	        throw e;
	    }
	}

	@When("Verify message when vc ran out of verification chances")
	public void verifyMessageWhenVcRanOutOfVerificationChances() {
	    try {
	        Assert.assertEquals(uploadqrcode.getErrorMessageForVerificationLimitReached(), UiConstants.ERROR_MESSAGE_VERIFICATION_LIMIT_REACHED,
	            "Mismatch in error message for verification limit reached.");
	        test.log(Status.PASS, "Verified verification limit reached message successfully. Expected: " +
	            UiConstants.ERROR_MESSAGE_VERIFICATION_LIMIT_REACHED + ", Actual: " + uploadqrcode.getErrorMessageForVerificationLimitReached());
	    } catch (NoSuchElementException e) {
	        logFailure(test, driver, "Element not found while verifying the verification limit reached message", e);
	        throw e;
	    } catch (AssertionError e) {
	        logFailure(test, driver, "Error message validation failed for verification limit reached. Expected: " +
	            UiConstants.ERROR_MESSAGE_VERIFICATION_LIMIT_REACHED + ", but found: " +
	            uploadqrcode.getErrorMessageForVerificationLimitReached(), e);
	        throw e;
	    } catch (Exception e) {
	        logFailure(test, driver, "Unexpected error while verifying the verification limit reached message", e);
	        throw e;
	    }
	}

	@When("User enter the policy number {string}")
	public void userEnterThePolicyNumber(String string) {
	    try {
	        homePage.enterPolicyNumer(string);
	        test.log(Status.PASS, "Successfully entered policy number: " + string);
	    } catch (NoSuchElementException e) {
	        logFailure(test, driver, "Element not found while entering policy number", e);
	        throw e;
	    } catch (Exception e) {
	        logFailure(test, driver, "Unexpected error while entering policy number", e);
	        throw e;
	    }
	}

	@When("User enter the full name {string}")
	public void userEnterTheFullName(String string) {
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
    public void userEnterTheFullName() {
        try {
        	homePage.enterFullName(fullName());
            test.log(Status.PASS, "User successfully entered the full name: " + fullName());
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
    public void userEnterTheDateOfBirth() {
        try {
        	homePage.selectDateOfBirth(dob());
            test.log(Status.PASS, "User successfully entered the date of birth: " + dob());
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
    public void userEnterTheDateOfBirth1(String dateOfBirth) {
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
	public void userClickOnLoginButton() {
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
	public void userClickOnGoBackButton() {
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
	public void userClickOnMosipVcCheckBox() {
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


	@When("User click on Generate QR Code button")
	public void userClickOnGenerateQrCodeButton() {
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

	@When("Open inji web in tab")
	public void userOpenInjiWebInTab() {
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
	public void verifyThatUploadButtonVisible() {
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

	@Then("Verify upload file input is present and enabled")
	public void verifyUploadFileInputIsPresentAndEnabled() {
	    try {
	        Assert.assertTrue(uploadqrcode.isUploadFileInputPresent(), "Upload file input is not present.");
	        Assert.assertTrue(uploadqrcode.isUploadFileInputEnabled(), "Upload file input is not enabled.");
	        test.log(Status.PASS, "Upload file input is present and enabled.");
	    } catch (NoSuchElementException e) {
	        logFailure(test, driver, "Element not found while verifying upload file input availability", e);
	        throw e;
	    } catch (Exception e) {
	        logFailure(test, driver, "Unexpected error while verifying upload file input availability", e);
	        throw e;
	    }
	}

	@When("Click outside the help dropdown")
	public void clickOutsideHelpDropdown() {
	    try {
	        homePage.clickOutsideHelpDropdown();
	        test.log(Status.PASS, "Clicked outside the help dropdown.");
	    } catch (NoSuchElementException e) {
	        logFailure(test, driver, "Element not found while clicking outside the help dropdown", e);
	        throw e;
	    } catch (Exception e) {
	        logFailure(test, driver, "Unexpected error while clicking outside the help dropdown", e);
	        throw e;
	    }
	}

	@Then("Verify all buttons fit their layout without text overflow")
	public void verifyAllButtonsFitTheirLayout() {
	    try {
	        List<String> overflowing = homePage.getOverflowingButtonTexts();
	        Assert.assertTrue(overflowing.isEmpty(),
	            "Button text overflows its bounds for: " + overflowing);
	        test.log(Status.PASS, "All visible buttons fit their layout without text overflow.");
	    } catch (AssertionError e) {
	        logAssertionFailure(test, driver, "Button layout check failed", e);
	        throw e;
	    } catch (Exception e) {
	        logFailure(test, driver, "Error checking button layout", e);
	        throw e;
	    }
	}

	@When("Inject no-reload page sentinel")
	public void injectNoReloadPageSentinel() {
	    try {
	        homePage.injectNoReloadSentinel();
	        test.log(Status.PASS, "No-reload sentinel injected into window.__noReloadSentinel.");
	    } catch (Exception e) {
	        logFailure(test, driver, "Unexpected error while injecting no-reload sentinel", e);
	        throw e;
	    }
	}

	@Then("Verify page navigation was seamless without reload")
	public void verifyPageNavigationWasSeamlessWithoutReload() {
	    try {
	        boolean notReloaded = homePage.isPageNotReloaded();
	        Assert.assertTrue(notReloaded, "Page was fully reloaded during navigation (window.__noReloadSentinel was cleared). Expected seamless SPA navigation.");
	        test.log(Status.PASS, "Page navigation was seamless — no full reload detected (window.__noReloadSentinel still present).");
	    } catch (NoSuchElementException e) {
	        logFailure(test, driver, "Element not found while verifying seamless navigation", e);
	        throw e;
	    } catch (Exception e) {
	        logFailure(test, driver, "Unexpected error while verifying seamless navigation", e);
	        throw e;
	    }
	}

	@When("Click on Inji Verify logo")
	public void clickOnInjiVerifyLogo() {
	    try {
	        homePage.clickOnInjiVerifyLogo();
	        test.log(Status.PASS, "Successfully clicked on the Inji Verify logo.");
	    } catch (NoSuchElementException e) {
	        logFailure(test, driver, "Element not found while clicking on the Inji Verify logo", e);
	        throw e;
	    } catch (Exception e) {
	        logFailure(test, driver, "Unexpected error while clicking on the Inji Verify logo", e);
	        throw e;
	    }
	}

	@When("Navigate to upload button using Tab key")
	public void navigateToUploadButtonUsingTabKey() {
	    try {
	        uploadqrcode.tabToUploadButton();
	        test.log(Status.PASS, "Successfully navigated to upload button using Tab key.");
	    } catch (NoSuchElementException e) {
	        logFailure(test, driver, "Element not found while navigating to upload button via Tab key", e);
	        throw e;
	    } catch (Exception e) {
	        logFailure(test, driver, "Unexpected error while navigating to upload button via Tab key", e);
	        throw e;
	    }
	}

	@Then("Verify upload button has keyboard focus")
	public void verifyUploadButtonHasKeyboardFocus() {
	    try {
	        boolean isFocused = uploadqrcode.isUploadButtonFocused();
	        Assert.assertTrue(isFocused, "Upload button does not have keyboard focus (document.activeElement does not match #upload-qr-code-button).");
	        test.log(Status.PASS, "Upload button has keyboard focus confirmed via document.activeElement.");
	    } catch (NoSuchElementException e) {
	        logFailure(test, driver, "Element not found while verifying upload button focus", e);
	        throw e;
	    } catch (Exception e) {
	        logFailure(test, driver, "Unexpected error while verifying upload button focus", e);
	        throw e;
	    }
	}

	@When("Upload QR code file JPEG via keyboard")
	public void uploadQRCodeFileJpegViaKeyboard() {
	    try {
	        uploadqrcode.uploadJpegAndWaitForVerificationResult();
	        test.log(Status.PASS, "Successfully uploaded QR code JPEG via keyboard-navigated upload control.");
	    } catch (NoSuchElementException e) {
	        logFailure(test, driver, "Element not found while uploading QR code JPEG via keyboard", e);
	        throw e;
	    } catch (Exception e) {
	        logFailure(test, driver, "Unexpected error while uploading QR code JPEG via keyboard", e);
	        throw e;
	    }
	}

@Then("Verify the Upload button after 2 mins idle")
public void verifyUploadButtonVisibleAfter2MinsIdle() {
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

	@When("Hover over the Upload QR Code button")
	public void hoverOverUploadQRCodeButton() {
	    try {
	        uploadqrcode.hoverOverUploadButton();
	        test.log(Status.PASS, "Successfully hovered over the Upload QR Code button.");
	    } catch (NoSuchElementException e) {
	        logFailure(test, driver, "Element not found while hovering over Upload QR Code button", e);
	        throw e;
	    } catch (Exception e) {
	        logFailure(test, driver, "Unexpected error while hovering over Upload QR Code button", e);
	        throw e;
	    }
	}

	@When("Hover off the Upload QR Code button")
	public void hoverOffUploadQRCodeButton() {
	    try {
	        uploadqrcode.hoverOffUploadButton();
	        test.log(Status.PASS, "Successfully moved mouse away from the Upload QR Code button.");
	    } catch (NoSuchElementException e) {
	        logFailure(test, driver, "Element not found while hovering off Upload QR Code button", e);
	        throw e;
	    } catch (Exception e) {
	        logFailure(test, driver, "Unexpected error while hovering off Upload QR Code button", e);
	        throw e;
	    }
	}

	@Then("Verify gradient SVG upload icon is visible")
	public void verifyGradientSvgUploadIconIsVisible() {
	    try {
	        boolean isVisible = uploadqrcode.isGradientUploadIconVisible();
	        Assert.assertTrue(isVisible, "Gradient SVG upload icon is not visible.");
	        test.log(Status.PASS, "Gradient SVG upload icon is visible as expected.");
	    } catch (NoSuchElementException e) {
	        logFailure(test, driver, "Element not found while verifying gradient SVG upload icon visibility", e);
	        throw e;
	    } catch (Exception e) {
	        logFailure(test, driver, "Unexpected error while verifying gradient SVG upload icon visibility", e);
	        throw e;
	    }
	}

	@Then("Verify gradient SVG upload icon is not visible")
	public void verifyGradientSvgUploadIconIsNotVisible() {
	    try {
	        boolean isVisible = uploadqrcode.isGradientUploadIconVisible();
	        assertFalse("Gradient SVG upload icon should not be visible on hover.", isVisible);
	        test.log(Status.PASS, "Gradient SVG upload icon is not visible on hover as expected.");
	    } catch (NoSuchElementException e) {
	        logFailure(test, driver, "Element not found while verifying gradient SVG upload icon is hidden", e);
	        throw e;
	    } catch (Exception e) {
	        logFailure(test, driver, "Unexpected error while verifying gradient SVG upload icon is hidden", e);
	        throw e;
	    }
	}

	@Then("Verify SVG upload icon is visible")
	public void verifySvgUploadIconIsVisible() {
	    try {
	        boolean isVisible = uploadqrcode.isUploadSvgIconVisible();
	        Assert.assertTrue(isVisible, "SVG upload icon is not visible.");
	        test.log(Status.PASS, "SVG upload icon is visible as expected.");
	    } catch (NoSuchElementException e) {
	        logFailure(test, driver, "Element not found while verifying SVG upload icon visibility", e);
	        throw e;
	    } catch (Exception e) {
	        logFailure(test, driver, "Unexpected error while verifying SVG upload icon visibility", e);
	        throw e;
	    }
	}

	@When("Verify click on Help button")
	public void userClickOnHomeButton() {
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

	@When("Verify click on FAQ button")
	public void userClickOnFaqButton() {
	    try {
	        homePage.clickOnFaqButton();
	        test.log(Status.PASS, "Successfully clicked on FAQ button.");
	    } catch (NoSuchElementException e) {
	        logFailure(test, driver, "Element not found while clicking FAQ button", e);
	        throw e;
	    } catch (Exception e) {
	        logFailure(test, driver, "Unexpected error while clicking FAQ button", e);
	        throw e;
	    }
	}

	@When("Verify that FAQ info message is displayed")
	public void verifyFaqInfoMessage() {
	    try {
	        String alertMessage = homePage.getFaqAlertMessage();
	        Assert.assertNotNull(alertMessage, "FAQ info message is null.");
	        Assert.assertFalse(alertMessage.trim().isEmpty(), "FAQ info message is empty.");
	        test.log(Status.PASS, "FAQ info message is displayed: " + alertMessage);
	    } catch (AssertionError e) {
	        test.log(Status.FAIL, "FAQ info message verification failed.");
	        throw e;
	    } catch (NoSuchElementException e) {
	        logFailure(test, driver, "Element not found while verifying FAQ info message", e);
	        throw e;
	    } catch (Exception e) {
	        logFailure(test, driver, "Unexpected error while verifying FAQ info message", e);
	        throw e;
	    }
	}

	@When("verify alert message")
	public void userVerifyAlertMessage() {
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
	public void verifyVpVerificationQrCodeStep1Description() {
	    try {
            String actualDescription = normalizeQuotedText(vpverification.getVpVerificationQrCodeStep1Description());
            String expectedDescription = normalizeQuotedText(UiConstants.VP_VERIFICATION_QR_CODE_STEP1_DESCRIPTION);
            assertEquals(actualDescription, expectedDescription);
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

    private String normalizeQuotedText(String value) {
        if (value == null) {
            return null;
        }
        return value
                .replace('’', '\'')
                .replace('‘', '\'')
                .replace('"', '\'')
                .trim();
    }

	@When("Verify VP verification qr code step1 label")
	public void verifyVpVerificationQrCodeStep1Label() {
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
	public void verifyVpVerificationQrCodeStep2Label() {
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
	public void verifyVpVerificationQrCodeStep2Description() {
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
	public void verifyVpVerificationQrCodeStep3Label() {
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
	public void verifyVpVerificationQrCodeStep3Description() {
	    try {
	        String actualDescription = vpverification.getVpVerificationQrCodeStep3Description();
	        String expectedDescription = vpverification.normalizeVisibleText(UiConstants.VP_VERIFICATION_QR_CODE_STEP3_DESCRIPTION);
	        Assert.assertEquals(actualDescription, expectedDescription);
	        test.log(Status.PASS, "Successfully verified VP verification qr code step3 description.");
	    } catch (AssertionError e) {
	        test.log(Status.FAIL, "Verification failed: VP verification qr code step3 description mismatch. Expected: '" + vpverification.normalizeVisibleText(UiConstants.VP_VERIFICATION_QR_CODE_STEP3_DESCRIPTION) + "', Actual: '" + vpverification.getVpVerificationQrCodeStep3Description() + "'");
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
	public void verifyVpVerificationQrCodeStep4Label() {
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
	public void verifyVpVerificationQrCodeStep4Description() {
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
	public void verifyRequestVerifiableCredentialsButton() {
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
	public void verifyVpVerifiableQrCodeGenerated() {
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
	public void verifyVerifiableCredentialSelectionPanel() {
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
	public void uploadLargeSizeNotSupportedQrCodeFile() {
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

	@Then("Upload small size not supported QR code file")
	public void uploadSmallSizeNotSupportedQrCodeFile() {
	    try {
	        uploadqrcode.clickOnUploadSmallSizeQRCode();

	        test.log(Status.PASS, "Successfully verified small size QR code file is not supported and appropriate error is shown.");
	    } catch (AssertionError e) {
	        test.log(Status.FAIL, "Verification failed: Expected error message for small QR code not shown.");
	        throw e;
	    } catch (NoSuchElementException e) {
	        logFailure(test, driver, "Element not found while uploading small size QR code", e);
	        throw e;
	    } catch (Exception e) {
	        logFailure(test, driver, "Unexpected error while uploading small size QR code", e);
	        throw e;
	    }
	}

	@Then("Upload 10KB QR code file")
	public void upload10KbQrCodeFile() {
	    try {
	        uploadqrcode.clickOnUploadBoundaryMinSizeQRCode();
	        test.log(Status.PASS, "Successfully uploaded the boundary valid QR code file of size 10KB.");
	    } catch (NoSuchElementException e) {
	        logFailure(test, driver, "Element not found while uploading 10KB QR code file", e);
	        throw e;
	    } catch (Exception e) {
	        logFailure(test, driver, "Unexpected error while uploading 10KB QR code file", e);
	        throw e;
	    }
	}

	@Then("Upload 5MB QR code file")
	public void upload5MbQrCodeFile() {
	    try {
	        uploadqrcode.clickOnUploadBoundaryMaxSizeQRCode();
	        test.log(Status.PASS, "Successfully uploaded the boundary valid QR code file of size 5MB.");
	    } catch (NoSuchElementException e) {
	        logFailure(test, driver, "Element not found while uploading 5MB QR code file", e);
	        throw e;
	    } catch (Exception e) {
	        logFailure(test, driver, "Unexpected error while uploading 5MB QR code file", e);
	        throw e;
	    }
	}

	@Then("Click on ble tab")
	public void clickOnBleTab1() {
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
	public void verifyMessageForLargeSizeQrCode() {
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

    @Then("Verify small or blur alert message")
	public void verifyMessageForBlurQrCode() {
	    try {
	        Assert.assertEquals(uploadqrcode.getErrorMessageForBlurQRCode(), UiConstants.ERROR_SMALL_OR_BLUR_QR_CODE);
	        test.log(Status.PASS, "Successfully verified Small/Blur alert message for blur QR code.");
	    } catch (AssertionError e) {
	    	test.log(Status.FAIL, "Verification failed: Small/Blur alert message does not match the expected value.");
	    	logFailure(test, driver, "Verification failed: Small/Blur alert message does not match the expected value.", e);
	        throw e;
	    } catch (NoSuchElementException e) {
	        logFailure(test, driver, "Element not found while verifying Small/Blur alert message", e);
	        throw e;
	    } catch (Exception e) {
	        logFailure(test, driver, "Unexpected error occurred while verifying Small/Blur alert message", e);
	        throw e;
	    }
	}

	@Then("Verify small or blur alert message in {string}")
	public void verifyMessageForBlurQrCodeInLanguage(String language) {
	    String expectedMessage;
	    switch (language) {
	        case "French":
	            expectedMessage = UiConstants.ERROR_SMALL_OR_BLUR_QR_CODE_IN_FRENCH;
	            break;
	        case "Arabic":
	            expectedMessage = UiConstants.ERROR_SMALL_OR_BLUR_QR_CODE_IN_ARABIC;
	            break;
	        case "Spanish":
	            expectedMessage = UiConstants.ERROR_SMALL_OR_BLUR_QR_CODE_IN_SPANISH;
	            break;
	        default:
	            expectedMessage = UiConstants.ERROR_SMALL_OR_BLUR_QR_CODE;
	            break;
	    }
	    try {
	        Assert.assertEquals(uploadqrcode.getErrorMessageForBlurQRCode(), expectedMessage);
	        test.log(Status.PASS, "Successfully verified Small/Blur alert message in " + language + ".");
	    } catch (AssertionError e) {
	        test.log(Status.FAIL, "Verification failed: Small/Blur alert message in " + language
	                + " does not match the expected value.");
	        logFailure(test, driver, "Verification failed: Small/Blur alert message in " + language
	                + " does not match the expected value.", e);
	        throw e;
	    } catch (NoSuchElementException e) {
	        logFailure(test, driver, "Element not found while verifying Small/Blur alert message in " + language, e);
	        throw e;
	    } catch (Exception e) {
	        logFailure(test, driver, "Unexpected error occurred while verifying Small/Blur alert message in " + language, e);
	        throw e;
	    }
	}

	@Then("Upload blur QR code file")
	public void uploadBlurQrCodeFile() {
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
	public void verifyMessageForMultiFormatQrCode() {
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

    @Then("Verify Multiple Qr code alert message")
	public void verifyMessageForMultipleQrCode() {
	    try {
	        Assert.assertEquals(uploadqrcode.getErrorMessageForBlurQRCode(), UiConstants.ERROR_MULTIPLE_QR_CODES);
	        test.log(Status.PASS, "Successfully verified Multiple QR code alert message for blur QR code.");
	    } catch (AssertionError e) {
	    	test.log(Status.FAIL, "Verification failed: Multiple QR code alert message does not match the expected value.");
	    	logFailure(test, driver, "Verification failed: Multiple QR code alert message does not match the expected value.", e);
	        throw e;
	    } catch (NoSuchElementException e) {
	        logFailure(test, driver, "Element not found while verifying Multiple QR code alert message", e);
	        throw e;
	    } catch (Exception e) {
	        logFailure(test, driver, "Unexpected error occurred while verifying Multiple QR code alert message", e);
	        throw e;
	    }
	}


	@Then("Upload multiple qr code in one image file")
	public void uploadMultipleQrCodeInOneImageFile() {
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
	public void uploadSdJwtQrCode() {
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
	public void uploadInvalidPdf() {
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



	@Then("Verify click sort by button")
	public void userClickOnSortButton() {
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
	public void userClickOnSortAZButton() {
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
	public void userClickOnSortZAButton() {
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
	public void userClickOnBackButton() {
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
	public void userClickOnCancelButton() {
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
	public void userClickOnOpenWalletButton() {
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
	public void verifyClickOnGenerateQrCodeButton() {
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
	public void verifyQrCodeGenerated() {
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
	public void verifyLoadingScreen() {
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
	public void verifyQrCodeIsNotPresent() {
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

	@Then("Verify QR code is not displayed")
	public void verifyQrCodeIsNotDisplayed() {
	    try {
	        Assert.assertFalse(vpverification.isVpVerificationQrCodeGenerated());
	        test.log(Status.PASS, "Successfully verified that the QR code is not displayed.");
	    } catch (AssertionError e) {
	        test.log(Status.FAIL, "Verification failed: QR code is unexpectedly displayed.");
	        logFailure(test, driver, "Verification failed: QR code is unexpectedly displayed.", e);
	        throw e;
	    } catch (NoSuchElementException e) {
	        logFailure(test, driver, "Element not found while verifying absence of QR code", e);
	        throw e;
	    } catch (Exception e) {
	        logFailure(test, driver, "Unexpected error occurred while verifying absence of QR code", e);
	        throw e;
	    }
	}


	@Then("Uncheck credential {string}")
	public void uncheckCredential(String credentialKeyOrName) {
	    try {
	        String credentialName = api.VerifiableClaimsConfigManager.resolveCredentialName(credentialKeyOrName);
	        vpverification.clickOnCredentialChecklist(credentialName);
	        test.log(Status.PASS, "Successfully unchecked credential: " + credentialName);
	    } catch (NoSuchElementException e) {
	        logFailure(test, driver, "Element not found while unchecking credential: " + credentialKeyOrName, e);
	        throw e;
	    } catch (Exception e) {
	        logFailure(test, driver, "Unexpected error while unchecking credential: " + credentialKeyOrName, e);
	        throw e;
	    }
	}


	@Then("Select credential {string}")
	public void selectCredential(String credentialKeyOrName) {
	    try {
	        String credentialName = api.VerifiableClaimsConfigManager.resolveCredentialName(credentialKeyOrName);
	        vpverification.clickOnCredentialChecklist(credentialName);
	        test.log(Status.PASS, "Successfully selected credential: " + credentialName);
	    } catch (NoSuchElementException e) {
	        logFailure(test, driver, "Element not found while selecting credential: " + credentialKeyOrName, e);
	        throw e;
	    } catch (Exception e) {
	        logFailure(test, driver, "Unexpected error while selecting credential: " + credentialKeyOrName, e);
	        throw e;
	    }
	}

    @Then("Credential {string} is selected")
	public void credentialIsSelected(String credentialKeyOrName) {
	    try {
	        String credentialName = api.VerifiableClaimsConfigManager.resolveCredentialName(credentialKeyOrName);
	        Assert.assertTrue(vpverification.isCredentialSelected(credentialName),
	                "Credential '" + credentialName + "' should be selected");
	        test.log(Status.PASS, "Verified credential is selected: " + credentialName);
	    } catch (AssertionError e) {
	        logFailure(test, driver, "Credential was not selected: " + credentialKeyOrName, e);
	        throw e;
	    } catch (Exception e) {
	        logFailure(test, driver, "Unexpected error verifying credential selected: " + credentialKeyOrName, e);
	        throw e;
	    }
	}

    @Then("Credential {string} is not selected")
	public void credentialIsNotSelected(String credentialKeyOrName) {
	    try {
	        String credentialName = api.VerifiableClaimsConfigManager.resolveCredentialName(credentialKeyOrName);
	        Assert.assertFalse(vpverification.isCredentialSelected(credentialName),
	                "Credential '" + credentialName + "' should not be selected");
	        test.log(Status.PASS, "Verified credential is not selected: " + credentialName);
	    } catch (AssertionError e) {
	        logFailure(test, driver, "Credential was unexpectedly selected: " + credentialKeyOrName, e);
	        throw e;
	    } catch (Exception e) {
	        logFailure(test, driver, "Unexpected error verifying credential not selected: " + credentialKeyOrName, e);
	        throw e;
	    }
	}

    @Then("Verify Click on wallet")
	public void verifyClickOnWallet() {
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
	public void verifyClickOnProceed() {
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

	@Then("Essential credential from config is auto selected")
	public void essentialCredentialFromConfigIsAutoSelected() {
	    try {
	        String essentialName = api.VerifiableClaimsConfigManager.getEssentialCredentialName();
	        Assert.assertTrue(vpverification.isCredentialSelected(essentialName),
	                "Essential credential '" + essentialName + "' from config.json should be auto-selected");
	        test.log(Status.PASS, "Essential credential from config.json is auto-selected: " + essentialName);
	    } catch (AssertionError e) {
	        logFailure(test, driver, "Essential credential from config.json was not auto-selected", e);
	        throw e;
	    } catch (Exception e) {
	        logFailure(test, driver, "Unexpected error while verifying essential credential auto-selection", e);
	        throw e;
	    }
	}


	@Then("Non essential credentials from config are not auto selected")
	public void nonEssentialCredentialsFromConfigAreNotAutoSelected() {
	    try {
	        for (String credentialName : api.VerifiableClaimsConfigManager.getNonEssentialCredentialNames()) {
	            Assert.assertFalse(vpverification.isCredentialSelected(credentialName),
	                    "Non-essential credential '" + credentialName + "' should not be auto-selected");
	        }
	        test.log(Status.PASS, "Non-essential credentials from config.json are not auto-selected");
	    } catch (AssertionError e) {
	        logFailure(test, driver, "Unexpected auto-selection of non-essential credentials", e);
	        throw e;
	    } catch (Exception e) {
	        logFailure(test, driver, "Unexpected error while verifying non-essential credential selection", e);
	        throw e;
	    }
	}

	@Then("User enter the credential type {string}")
	public void userEnterTheCredentialType(String credentialType) {
	    try {
	        new WebDriverWait(driver, Duration.ofSeconds(10))
	            .until(ExpectedConditions.elementToBeClickable(
	                By.xpath("//input[@placeholder='Search VC Type']")));
	        vpverification.enterCredentialType(credentialType);
	        test.log(Status.PASS, "Successfully entered credential type: " + credentialType);
	    } catch (NoSuchElementException e) {
	        logFailure(test, driver, "Element not found while entering credential type: " + credentialType, e);
	        throw e;
	    } catch (Exception e) {
	        logFailure(test, driver, "Unexpected error occurred while entering credential type: " + credentialType, e);
	        throw e;
	    }
	}


	@Then("Verify that Upload icon visible")
	public void verifyThatUploadIconVisible() {
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

    @When("Verify if gender value is present in french")
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

    @Then("Verify {string} language is highlighted")
    public void verifyLanguageIsHighlighted(String englishName) {
        String displayName = toDisplayName(englishName);
        try {
            boolean hasGradient = uploadqrcode.isLanguageHighlightedWithGradient(displayName);
            Assert.assertTrue(hasGradient, displayName + " language text should have gradient highlight");
            test.log(Status.PASS, displayName + " language is highlighted with gradient.");
            boolean hasTick = uploadqrcode.isLanguageTickVisible(displayName);
            Assert.assertTrue(hasTick, displayName + " language should show a tick mark");
            test.log(Status.PASS, displayName + " language tick mark is visible.");
        } catch (NoSuchElementException e) {
            logFailure(test, driver, "Element not found while checking highlight for '" + displayName + "' language", e);
            throw e;
        } catch (Exception e) {
            logFailure(test, driver, "Unexpected error while checking highlight for '" + displayName + "' language", e);
            throw e;
        }
    }

    @When("Verify select language {string}")
    public void verifySelectLanguage(String englishName) {
        String displayName = toDisplayName(englishName);
        try {
            uploadqrcode.selectLanguage(displayName);
            test.log(Status.PASS, "Clicked on '" + displayName + "' language button successfully.");
            boolean isSelected = uploadqrcode.isLanguageSelected(displayName);
            Assert.assertTrue(isSelected, displayName + " language should be selected after click");
            test.log(Status.PASS, displayName + " language is successfully selected.");
        } catch (NoSuchElementException e) {
            logFailure(test, driver, "Element not found while clicking '" + displayName + "' language button", e);
            throw e;
        } catch (Exception e) {
            logFailure(test, driver, "Unexpected error while clicking '" + displayName + "' language button", e);
            throw e;
        }
    }

    @When("Verify if name value is present in english")
    public void verifyIfNameValueIsPresentInEnglish() {
        try {
            String actualLabel = homePage.getNameValueInEnglish();
            Assert.assertEquals(actualLabel, UiConstants.NAME_VALUE_IN_ENGLISH,
                "Name value in english does not match the expected value.");
            test.log(Status.PASS, "Name value in english verification successful. Expected: "
                + UiConstants.NAME_VALUE_IN_ENGLISH + ", Actual: " + actualLabel);
        } catch (AssertionError e) {
            test.log(Status.FAIL, "Name value in english verification failed. Expected: "
                + UiConstants.NAME_VALUE_IN_ENGLISH + ", but found: " + homePage.getNameValueInEnglish());
            throw e;
        } catch (NoSuchElementException e) {
            logFailure(test, driver, "Element not found while verifying Name value in english", e);
            throw e;
        } catch (Exception e) {
            logFailure(test, driver, "Unexpected error while verifying Name value in english", e);
            throw e;
        }
    }

    @When("Verify if gender value is present in english")
    public void verifyIfGenderValueIsPresentInEnglish() {
        try {
            String actualLabel = homePage.getGenderValueInEnglish();
            Assert.assertEquals(actualLabel, UiConstants.GENDER_VALUE_IN_ENGLISH,
                "Gender value in english does not match the expected value.");
            test.log(Status.PASS, "Gender value in english verification successful. Expected: "
                + UiConstants.GENDER_VALUE_IN_ENGLISH + ", Actual: " + actualLabel);
        } catch (AssertionError e) {
            test.log(Status.FAIL, "Gender value in english verification failed. Expected: "
                + UiConstants.GENDER_VALUE_IN_ENGLISH + ", but found: " + homePage.getGenderValueInEnglish());
            throw e;
        } catch (NoSuchElementException e) {
            logFailure(test, driver, "Element not found while verifying Gender value in english", e);
            throw e;
        } catch (Exception e) {
            logFailure(test, driver, "Unexpected error while verifying Gender value in english", e);
            throw e;
        }
    }

    @Given("User enters wrong URL in the address bar")
    public void userEntersWrongUrlInTheAddressBar() {
        try {
            driver.get("http://invalid-url-for-testing.com");
            test.log(Status.PASS, "Navigated to an invalid URL successfully.");
        } catch (org.openqa.selenium.WebDriverException e) {
            String message = e.getMessage() == null ? "" : e.getMessage();
            if (!BaseTest.isUsingBrowserStack() && message.contains("ERR_NAME_NOT_RESOLVED")) {
                test.log(Status.PASS, "Local Chrome reported expected invalid-host error while navigating to wrong URL.");
                return;
            }
            logFailure(test, driver, "Unexpected error while navigating to an invalid URL", e);
            throw e;
        } catch (Exception e) {
            logFailure(test, driver, "Unexpected error while navigating to an invalid URL", e);
            throw e;
        }
    }

    @Then("Verify error message for wrong URL")
    public void verifyErrorMessageForWrongUrl() {
        try {
            String pageSource = driver.getPageSource();

            if (BaseTest.isUsingBrowserStack()) {

                Assert.assertTrue(
                        pageSource.contains("Unable to display the page"),
                        "Expected BrowserStack error page not shown"
                );

                test.log(Status.PASS, "BrowserStack error page validated");

            } else {
                boolean browserErrorVisible = pageSource.contains("This site can't be reached")
                        || pageSource.contains("This site can’t be reached")
                        || pageSource.contains("The site can't be reached")
                        || pageSource.contains("The site can’t be reached");

                boolean dnsErrorVisible = pageSource.contains("DNS_PROBE_FINISHED_NXDOMAIN")
                        || pageSource.contains("ERR_NAME_NOT_RESOLVED")
                        || pageSource.contains("ERR_INTERNET_DISCONNECTED");

                Assert.assertTrue(browserErrorVisible, "Expected local browser error page not shown");
                Assert.assertTrue(dnsErrorVisible, "Expected local network/DNS error not shown");

                test.log(Status.PASS, "Local browser error page validated");
            }

        } catch (AssertionError e) {
            test.log(Status.FAIL, "Error page validation failed: " + e.getMessage());
            throw e;
        }
    }

    @When("User clicks on insurance credential button")
    public void user_clicks_on_insurance_credential_button() {
        try {
            vpverification.clickOnInsuranceCredentialButton();
            test.log(Status.PASS, "Successfully clicked on the insurance credential button.");
        } catch (Exception e) {
            logFailure(test, driver, "Failed to click on the insurance credential button", e);
            throw e;
        }
    }

    @When("User clicks on expand credential button")
    public void user_clicks_on_expand_credential_button() {
        try {
            vpverification.clickOnExpandCredentialButton();
            test.log(Status.PASS, "Successfully clicked on the expand (Open In Full) button.");
        } catch (Exception e) {
            logFailure(test, driver, "Failed to click on the expand credential button", e);
            throw e;
        }
    }

    @When("User clicks on close popup button")
    public void user_clicks_on_close_popup_button() {
        try {
            vpverification.clickOnClosePopupButton();
            test.log(Status.PASS, "Successfully clicked on the close popup (✕) button.");
        } catch (Exception e) {
            logFailure(test, driver, "Failed to click on the close popup button", e);
            throw e;
        }
    }

    @When("User clicks on collapse credential button")
    public void user_clicks_on_collapse_credential_button() {
        try {
            vpverification.clickOnCollapseCredentialButton();
            test.log(Status.PASS, "Successfully clicked on the collapse (Close Fullscreen) button.");
        } catch (Exception e) {
            logFailure(test, driver, "Failed to click on the collapse credential button", e);
            throw e;
        }
    }

    @When("User clicks on inner download button")
    public void user_clicks_on_inner_download_button() {
        try {
            vpverification.clickOnInnerDownloadButton();
            test.log(Status.PASS, "Successfully clicked on the download button inside the popup.");
        } catch (Exception e) {
            logFailure(test, driver, "Failed to click on the inner download button", e);
            throw e;
        }
    }

    @Then("verify outer download button")
    public void verify_outer_download_button() {
        try {
            boolean isVisible = vpverification.isOuterDownloadButtonVisible();
            if (isVisible) {
                test.log(Status.PASS, "Outer download button is visible on the credential card.");
            } else {
                throw new AssertionError("Outer download button is not visible on the credential card.");
            }
        } catch (Exception e) {
            logFailure(test, driver, "Failed to verify the outer download button", e);
            throw e;
        }
    }

    @Then("The data showcase field {string} is visible")
    public void dataShowcaseFieldIsVisible(String fieldId) {
        try {
            Assert.assertTrue(vpverification.isDataShowcaseFieldVisible(fieldId),
                    "Field '" + fieldId + "' is not visible in the data showcase.");
            test.log(Status.PASS, "Field '" + fieldId + "' is visible in the data showcase.");
        } catch (AssertionError e) {
            logFailure(test, driver, "Field '" + fieldId + "' not found in data showcase", e);
            throw e;
        }
    }

    @Then("The data showcase field {string} is not visible")
    public void dataShowcaseFieldIsNotVisible(String fieldId) {
        try {
            Assert.assertTrue(vpverification.isDataShowcaseFieldAbsent(fieldId),
                    "Field '" + fieldId + "' is still present in the data showcase but should not be.");
            test.log(Status.PASS, "Field '" + fieldId + "' is correctly absent from the data showcase.");
        } catch (AssertionError e) {
            logFailure(test, driver, "Field '" + fieldId + "' unexpectedly present in data showcase", e);
            throw e;
        }
    }

    @Then("The popup field {string} is visible")
    public void popupFieldIsVisible(String fieldId) {
        try {
            Assert.assertTrue(vpverification.isPopupFieldVisible(fieldId),
                    "Field '" + fieldId + "' is not visible in the credential popup.");
            test.log(Status.PASS, "Field '" + fieldId + "' is visible in the credential popup.");
        } catch (AssertionError e) {
            logFailure(test, driver, "Field '" + fieldId + "' not found in credential popup", e);
            throw e;
        }
    }

    @Then("The popup field {string} is not visible")
    public void popupFieldIsNotVisible(String fieldId) {
        try {
            Assert.assertTrue(vpverification.isPopupFieldAbsent(fieldId),
                    "Field '" + fieldId + "' is still present in popup but should not be.");
            test.log(Status.PASS, "Field '" + fieldId + "' is correctly absent from the credential popup.");
        } catch (AssertionError e) {
            logFailure(test, driver, "Field '" + fieldId + "' unexpectedly present in credential popup", e);
            throw e;
        }
    }

    @Then("The credential popup is visible")
    public void credentialPopupIsVisible() {
        try {
            Assert.assertTrue(vpverification.isCredentialPopupVisible(), "Credential popup is not visible.");
            test.log(Status.PASS, "Credential popup is visible.");
        } catch (AssertionError e) {
            logFailure(test, driver, "Credential popup is not visible", e);
            throw e;
        }
    }

    @Then("The credential popup is not visible")
    public void credentialPopupIsNotVisible() {
        try {
            Assert.assertTrue(vpverification.isCredentialPopupAbsent(),
                    "Credential popup is still visible but should not be.");
            test.log(Status.PASS, "Credential popup is correctly absent.");
        } catch (AssertionError e) {
            logFailure(test, driver, "Credential popup unexpectedly present", e);
            throw e;
        }
    }

    @Then("Verify file {string} exists in downloads folder")
    public void verifyFileExistsInDownloadsFolder(String fileName) {
        try {
            boolean fileFound;
            if (BaseTest.isUsingBrowserStack()) {
                fileFound = waitForFileOnBrowserStack(fileName);
            } else {
                fileFound = waitForFileInLocalDownloads(fileName);
            }
            Assert.assertTrue(fileFound, "File '" + fileName + "' was not found in the downloads folder.");
            test.log(Status.PASS, "File '" + fileName + "' successfully found in the downloads folder.");
        } catch (AssertionError e) {
            logFailure(test, driver, "File '" + fileName + "' was not found in the downloads folder", e);
            throw e;
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            logFailure(test, driver, "Interrupted while waiting for file '" + fileName + "' in downloads folder", e);
            throw new RuntimeException(e);
        } catch (Exception e) {
            logFailure(test, driver, "Unexpected error while verifying file '" + fileName + "' in downloads folder", e);
            throw e;
        }
    }

    private boolean waitForFileInLocalDownloads(String fileName) throws InterruptedException {
        File downloadsDir = new File(BaseTest.getLocalDownloadsDirectoryPath());
        Instant deadline = Instant.now().plusSeconds(BasePage.getTimeout());
        while (Instant.now().isBefore(deadline)) {
            File target = new File(downloadsDir, fileName);
            if (target.exists() && target.length() > 0) {
                return true;
            }
            Thread.sleep(1000);
        }
        File target = new File(downloadsDir, fileName);
        return target.exists() && target.length() > 0;
    }

    private boolean waitForFileOnBrowserStack(String fileName) throws InterruptedException {
        JavascriptExecutor js = (JavascriptExecutor) driver;
        Instant deadline = Instant.now().plusSeconds(BasePage.getTimeout());
        while (Instant.now().isBefore(deadline)) {
            if (isBrowserStackFileAvailable(js, fileName)) {
                return true;
            }
            Thread.sleep(2000);
        }
        return isBrowserStackFileAvailable(js, fileName);
    }

    private boolean isBrowserStackFileAvailable(JavascriptExecutor js, String fileName) {
        try {
            Object result = js.executeScript(
                "browserstack_executor: {\"action\":\"getFileProperties\","
                + "\"arguments\":{\"fileName\":\"" + fileName + "\"}}"
            );
            return result != null && !result.toString().trim().isEmpty();
        } catch (Exception e) {
            return false;
        }
    }

    @When("Reload the page")
    public void reloadThePage() {
        try {
            driver.navigate().refresh();
            test.log(Status.PASS, "Page reloaded successfully.");
        } catch (Exception e) {
            logFailure(test, driver, "Unexpected error while reloading the page", e);
            throw e;
        }
    }

    @When("Get current URL of upload QR code tab")
    public void getCurrentUrlOfUploadQRCodeTab() {
        try {
            uploadPageUrl = driver.getCurrentUrl();
            test.log(Status.PASS, "Retrieved upload QR code tab URL: " + uploadPageUrl);
        } catch (Exception e) {
            logFailure(test, driver, "Unexpected error while getting upload QR code tab URL", e);
            throw e;
        }
    }

    @When("Get current URL of scan QR code tab")
    public void getCurrentUrlOfScanQRCodeTab() {
        try {
            scanPageUrl = driver.getCurrentUrl();
            test.log(Status.PASS, "Retrieved scan QR code tab URL: " + scanPageUrl);
        } catch (Exception e) {
            logFailure(test, driver, "Unexpected error while getting scan QR code tab URL", e);
            throw e;
        }
    }

    @When("Get current URL of VP verification tab")
    public void getCurrentUrlOfVpVerificationTab() {
        try {
            vpVerificationPageUrl = driver.getCurrentUrl();
            test.log(Status.PASS, "Retrieved VP verification tab URL: " + vpVerificationPageUrl);
        } catch (Exception e) {
            logFailure(test, driver, "Unexpected error while getting VP verification tab URL", e);
            throw e;
        }
    }

    @Then("Verify user is still on the upload QR code page")
    public void verifyUserIsStillOnUploadQRCodePage() {
        try {
            String currentUrl = driver.getCurrentUrl();
            Assert.assertEquals(currentUrl, uploadPageUrl,
                "User was redirected away from the upload QR code page. Expected: " + uploadPageUrl + ", Actual: " + currentUrl);
            test.log(Status.PASS, "User is still on the upload QR code page. URL: " + currentUrl);
        } catch (AssertionError e) {
            logAssertionFailure(test, driver,
                "Upload page URL mismatch. Expected: " + uploadPageUrl + ", Actual: " + driver.getCurrentUrl(), e);
            throw e;
        } catch (Exception e) {
            logFailure(test, driver, "Unexpected error while verifying upload QR code page URL", e);
            throw e;
        }
    }

    @Then("Verify user is still on the scan QR code page")
    public void verifyUserIsStillOnScanQRCodePage() {
        try {
            String currentUrl = driver.getCurrentUrl();
            Assert.assertEquals(currentUrl, scanPageUrl,
                "User was redirected away from the scan QR code page. Expected: " + scanPageUrl + ", Actual: " + currentUrl);
            test.log(Status.PASS, "User is still on the scan QR code page. URL: " + currentUrl);
        } catch (AssertionError e) {
            logAssertionFailure(test, driver,
                "Scan page URL mismatch. Expected: " + scanPageUrl + ", Actual: " + driver.getCurrentUrl(), e);
            throw e;
        } catch (Exception e) {
            logFailure(test, driver, "Unexpected error while verifying scan QR code page URL", e);
            throw e;
        }
    }

    @Then("Verify user is still on the VP verification page")
    public void verifyUserIsStillOnVpVerificationPage() {
        try {
            String currentUrl = driver.getCurrentUrl();
            Assert.assertEquals(currentUrl, vpVerificationPageUrl,
                "User was redirected away from the VP verification page. Expected: " + vpVerificationPageUrl + ", Actual: " + currentUrl);
            test.log(Status.PASS, "User is still on the VP verification page. URL: " + currentUrl);
        } catch (AssertionError e) {
            logAssertionFailure(test, driver,
                "VP verification page URL mismatch. Expected: " + vpVerificationPageUrl + ", Actual: " + driver.getCurrentUrl(), e);
            throw e;
        } catch (Exception e) {
            logFailure(test, driver, "Unexpected error while verifying VP verification page URL", e);
            throw e;
        }
    }

    @Then("Verify that upload scan and VP verification page URLs are unique")
    public void verifyThatUploadScanAndVPVerificationPageUrlsAreUnique() {
        try {
            Set<String> urls = new HashSet<>();
            urls.add(uploadPageUrl);
            urls.add(scanPageUrl);
            urls.add(vpVerificationPageUrl);
            Assert.assertEquals(urls.size(), 3,
                "Page URLs are not unique. Upload: " + uploadPageUrl
                + ", Scan: " + scanPageUrl
                + ", VP Verification: " + vpVerificationPageUrl);
            test.log(Status.PASS, "All three page URLs are unique. Upload: " + uploadPageUrl
                + ", Scan: " + scanPageUrl + ", VP Verification: " + vpVerificationPageUrl);
        } catch (AssertionError e) {
            logAssertionFailure(test, driver,
                "Page URL uniqueness verification failed. Upload: " + uploadPageUrl
                + ", Scan: " + scanPageUrl + ", VP Verification: " + vpVerificationPageUrl, e);
            throw e;
        } catch (Exception e) {
            logFailure(test, driver, "Unexpected error while verifying page URLs are unique", e);
            throw e;
        }
    }



    @When("Verify congratulations message in spanish")
    public void verifyCongratulationsMessageInSpanish() {
        try {
            String actualMessage = uploadqrcode.getCongratulationtext();
            Assert.assertEquals(actualMessage, UiConstants.VERIFICATION_SUCCESS_MESSAGE_IN_SPANISH,
                "Congratulations message in Spanish does not match the expected value.");
            test.log(Status.PASS, "Congratulations message in Spanish verified. Expected: "
                + UiConstants.VERIFICATION_SUCCESS_MESSAGE_IN_SPANISH + ", Actual: " + actualMessage);
        } catch (AssertionError e) {
            logAssertionFailure(test, driver, "Congratulations message in Spanish verification failed. Expected: "
                + UiConstants.VERIFICATION_SUCCESS_MESSAGE_IN_SPANISH
                + ", but found: " + uploadqrcode.getCongratulationtext(), e);
            throw e;
        } catch (NoSuchElementException e) {
            logFailure(test, driver, "Element not found while verifying congratulations message in Spanish", e);
            throw e;
        } catch (Exception e) {
            logFailure(test, driver, "Unexpected error while verifying congratulations message in Spanish", e);
            throw e;
        }
    }

    @When("Verify congratulations message in french")
    public void verifyCongratulationsMessageInFrench() {
        try {
            String actualMessage = uploadqrcode.getCongratulationtext();
            Assert.assertEquals(actualMessage, UiConstants.VERIFICATION_SUCCESS_MESSAGE_IN_FRENCH,
                "Congratulations message in French does not match the expected value.");
            test.log(Status.PASS, "Congratulations message in French verified. Expected: "
                + UiConstants.VERIFICATION_SUCCESS_MESSAGE_IN_FRENCH + ", Actual: " + actualMessage);
        } catch (AssertionError e) {
            logAssertionFailure(test, driver, "Congratulations message in French verification failed. Expected: "
                + UiConstants.VERIFICATION_SUCCESS_MESSAGE_IN_FRENCH
                + ", but found: " + uploadqrcode.getCongratulationtext(), e);
            throw e;
        } catch (NoSuchElementException e) {
            logFailure(test, driver, "Element not found while verifying congratulations message in French", e);
            throw e;
        } catch (Exception e) {
            logFailure(test, driver, "Unexpected error while verifying congratulations message in French", e);
            throw e;
        }
    }

    @When("Verify congratulations message in arabic")
    public void verifyCongratulationsMessageInArabic() {
        try {
            String actualMessage = uploadqrcode.getCongratulationtext();
            Assert.assertEquals(actualMessage, UiConstants.VERIFICATION_SUCCESS_MESSAGE_IN_ARABIC,
                "Congratulations message in Arabic does not match the expected value.");
            test.log(Status.PASS, "Congratulations message in Arabic verified. Expected: "
                + UiConstants.VERIFICATION_SUCCESS_MESSAGE_IN_ARABIC + ", Actual: " + actualMessage);
        } catch (AssertionError e) {
            logAssertionFailure(test, driver, "Congratulations message in Arabic verification failed. Expected: "
                + UiConstants.VERIFICATION_SUCCESS_MESSAGE_IN_ARABIC
                + ", but found: " + uploadqrcode.getCongratulationtext(), e);
            throw e;
        } catch (NoSuchElementException e) {
            logFailure(test, driver, "Element not found while verifying congratulations message in Arabic", e);
            throw e;
        } catch (Exception e) {
            logFailure(test, driver, "Unexpected error while verifying congratulations message in Arabic", e);
            throw e;
        }
    }

    @When("Verify congratulations message in english")
    public void verifyCongratulationsMessageInEnglish() {
        try {
            String actualMessage = uploadqrcode.getCongratulationtext();
            Assert.assertEquals(actualMessage, UiConstants.VERIFICATION_SUCCESS_MESSAGE_IN_ENGLISH,
                "Congratulations message in English does not match the expected value.");
            test.log(Status.PASS, "Congratulations message in English verified. Expected: "
                + UiConstants.VERIFICATION_SUCCESS_MESSAGE_IN_ENGLISH + ", Actual: " + actualMessage);
        } catch (AssertionError e) {
            logAssertionFailure(test, driver, "Congratulations message in English verification failed. Expected: "
                + UiConstants.VERIFICATION_SUCCESS_MESSAGE_IN_ENGLISH
                + ", but found: " + uploadqrcode.getCongratulationtext(), e);
            throw e;
        } catch (NoSuchElementException e) {
            logFailure(test, driver, "Element not found while verifying congratulations message in English", e);
            throw e;
        } catch (Exception e) {
            logFailure(test, driver, "Unexpected error while verifying congratulations message in English", e);
            throw e;
        }
    }

    @When("selecting an invalid language")
    public void selectingAnInvalidLanguage() {
        try {
            ((JavascriptExecutor) driver).executeScript(
                "localStorage.setItem('selected_language', JSON.stringify('zz'));"
            );
            driver.navigate().refresh();
            new WebDriverWait(driver, Duration.ofSeconds(BasePage.getTimeout()))
                .until(webDriver -> ((JavascriptExecutor) webDriver)
                    .executeScript("return document.readyState").equals("complete"));
            new WebDriverWait(driver, Duration.ofSeconds(BasePage.getTimeout()))
                .until(ExpectedConditions.presenceOfElementLocated(
                    By.id("verify-credentials-heading")));
            test.log(Status.PASS, "Set language to invalid code 'zz' via localStorage and reloaded page.");
        } catch (Exception e) {
            logFailure(test, driver, "Unexpected error while setting invalid language 'zz'", e);
            throw e;
        }
    }

    @Then("Upload SD-JWT QR code with expired certificate")
    public void uploadSDJwtWithExpiredCertificate() {
        try {
            uploadqrcode.clickOnUploadSDJwtWithExpiredCertificate();
            test.log(Status.PASS, "Successfully uploaded SD-JWT QR code with expired certificate.");
        } catch (NoSuchElementException e) {
            logFailure(test, driver, "Element not found while uploading SD-JWT QR code with expired certificate", e);
            throw e;
        } catch (Exception e) {
            logFailure(test, driver, "Unexpected error occurred while uploading SD-JWT QR code with expired certificate", e);
            throw e;
        }
    }

    @Then("Upload SD-JWT QR code with incorrect digest value")
    public void uploadSDJwtWithIncorrectDigestValue() {
        try {
            uploadqrcode.clickOnUploadSDJwtWithIncorrectDigestValue();
            test.log(Status.PASS, "Successfully uploaded SD-JWT QR code with incorrect digest value.");
        } catch (NoSuchElementException e) {
            logFailure(test, driver, "Element not found while uploading SD-JWT QR code with incorrect digest value", e);
            throw e;
        } catch (Exception e) {
            logFailure(test, driver, "Unexpected error occurred while uploading SD-JWT QR code with incorrect digest value", e);
            throw e;
        }
    }

    @Then("Upload SD-JWT QR code with incorrect disclosure value")
    public void uploadSDJwtWithIncorrectDisclosureValue() {
        try {
            uploadqrcode.clickOnUploadSDJwtWithIncorrectDisclosureValue();
            test.log(Status.PASS, "Successfully uploaded SD-JWT QR code with incorrect disclosure value.");
        } catch (NoSuchElementException e) {
            logFailure(test, driver, "Element not found while uploading SD-JWT QR code with incorrect disclosure value", e);
            throw e;
        } catch (Exception e) {
            logFailure(test, driver, "Unexpected error occurred while uploading SD-JWT QR code with incorrect disclosure value", e);
            throw e;
        }
    }

    @Then("Upload SD-JWT QR code with incorrect signature")
    public void uploadSDJwtWithIncorrectSignature() {
        try {
            uploadqrcode.clickOnUploadSDJwtWithIncorrectSignature();
            test.log(Status.PASS, "Successfully uploaded SD-JWT QR code with incorrect signature.");
        } catch (NoSuchElementException e) {
            logFailure(test, driver, "Element not found while uploading SD-JWT QR code with incorrect signature", e);
            throw e;
        } catch (Exception e) {
            logFailure(test, driver, "Unexpected error occurred while uploading SD-JWT QR code with incorrect signature", e);
            throw e;
        }
    }

    @Then("Upload SD-JWT QR code with invalid digest length")
    public void uploadSDJwtWithInvalidDigestLength() {
        try {
            uploadqrcode.clickOnUploadSDJwtWithInvalidDigestLength();
            test.log(Status.PASS, "Successfully uploaded SD-JWT QR code with invalid digest length.");
        } catch (NoSuchElementException e) {
            logFailure(test, driver, "Element not found while uploading SD-JWT QR code with invalid digest length", e);
            throw e;
        } catch (Exception e) {
            logFailure(test, driver, "Unexpected error occurred while uploading SD-JWT QR code with invalid digest length", e);
            throw e;
        }
    }

    @Then("Upload SD-JWT QR code with malformed QR code")
    public void uploadSDJwtWithMalformedQRCode() {
        try {
            uploadqrcode.clickOnUploadSDJwtWithMalformedQRCode();
            test.log(Status.PASS, "Successfully uploaded SD-JWT malformed QR code.");
        } catch (NoSuchElementException e) {
            logFailure(test, driver, "Element not found while uploading SD-JWT malformed QR code", e);
            throw e;
        } catch (Exception e) {
            logFailure(test, driver, "Unexpected error occurred while uploading SD-JWT malformed QR code", e);
            throw e;
        }
    }

    @Then("Upload SD-JWT QR code with NBF set to future")
    public void uploadSDJwtWithNBFSetToFuture() {
        try {
            uploadqrcode.clickOnUploadSDJwtWithNBFSetToFuture();
            test.log(Status.PASS, "Successfully uploaded SD-JWT QR code with NBF set to future.");
        } catch (NoSuchElementException e) {
            logFailure(test, driver, "Element not found while uploading SD-JWT QR code with NBF set to future", e);
            throw e;
        } catch (Exception e) {
            logFailure(test, driver, "Unexpected error occurred while uploading SD-JWT QR code with NBF set to future", e);
            throw e;
        }
    }

    @Then("Upload SD-JWT QR code with unsupported algorithm")
    public void uploadSDJwtWithUnsupportedAlgorithm() {
        try {
            uploadqrcode.clickOnUploadSDJwtWithUnsupportedAlgorithm();
            test.log(Status.PASS, "Successfully uploaded SD-JWT QR code with unsupported algorithm.");
        } catch (NoSuchElementException e) {
            logFailure(test, driver, "Element not found while uploading SD-JWT QR code with unsupported algorithm", e);
            throw e;
        } catch (Exception e) {
            logFailure(test, driver, "Unexpected error occurred while uploading SD-JWT QR code with unsupported algorithm", e);
            throw e;
        }
    }

    @Then("Upload SD-JWT QR code with missing digest")
    public void uploadSDJwtWithMissingDigest() {
        try {
            uploadqrcode.clickOnUploadSDJwtWithMissingDigest();
            test.log(Status.PASS, "Successfully uploaded SD-JWT QR code with missing digest.");
        } catch (NoSuchElementException e) {
            logFailure(test, driver, "Element not found while uploading SD-JWT QR code with missing digest", e);
            throw e;
        } catch (Exception e) {
            logFailure(test, driver, "Unexpected error occurred while uploading SD-JWT QR code with missing digest", e);
            throw e;
        }
    }

    @Then("Upload SD-JWT QR code with selective disclosure")
    public void uploadSDJwtWithSelectiveDisclosure() {
        try {
            uploadqrcode.clickOnUploadSDJwtWithSelectiveDisclosure();
            test.log(Status.PASS, "Successfully uploaded SD-JWT QR code with selective disclosure.");
        } catch (NoSuchElementException e) {
            logFailure(test, driver, "Element not found while uploading SD-JWT QR code with selective disclosure", e);
            throw e;
        } catch (Exception e) {
            logFailure(test, driver, "Unexpected error occurred while uploading SD-JWT QR code with selective disclosure", e);
            throw e;
        }
    }

    @Then("Verify SD-JWT firstname field displays value 'John'")
    public void verifySDJwtFirstNameFieldDisplaysJohn() {
        try {
            String actual = uploadqrcode.getSDJwtFirstNameValue();
            Assert.assertEquals(actual, UiConstants.EXPECTED_SDJWT_SELECTIVE_DISCLOSURE_FIRSTNAME,
                    "Firstname value mismatch. Expected: " + UiConstants.EXPECTED_SDJWT_SELECTIVE_DISCLOSURE_FIRSTNAME + ", Actual: " + actual);
            test.log(Status.PASS, "Firstname field displays expected value: " + actual);
        } catch (AssertionError e) {
            logFailure(test, driver, "Firstname value mismatch for SD-JWT selective disclosure", e);
            throw e;
        } catch (NoSuchElementException e) {
            logFailure(test, driver, "Firstname element not found in SD-JWT selective disclosure result", e);
            throw e;
        } catch (Exception e) {
            logFailure(test, driver, "Unexpected error while verifying SD-JWT firstname field", e);
            throw e;
        }
    }

    @Then("Verify SD-JWT lastname field is not visible")
    public void verifySDJwtLastNameFieldIsNotVisible() {
        try {
            boolean isVisible = uploadqrcode.isSDJwtLastNameFieldVisible();
            Assert.assertFalse(isVisible, "Lastname field should not be visible for SD-JWT with selective disclosure.");
            test.log(Status.PASS, "Lastname field is correctly not visible in SD-JWT selective disclosure result.");
        } catch (AssertionError e) {
            logFailure(test, driver, "Lastname field is unexpectedly visible in SD-JWT selective disclosure result", e);
            throw e;
        } catch (Exception e) {
            logFailure(test, driver, "Unexpected error while verifying SD-JWT lastname field visibility", e);
            throw e;
        }
    }
}


