package stepdefinitions;

import com.aventstack.extentreports.Status;
import constants.UiConstants;
import io.cucumber.java.en.And;
import io.cucumber.java.en.Then;
import io.cucumber.java.en.When;
import org.openqa.selenium.NoSuchElementException;
import org.openqa.selenium.chrome.ChromeDriver;
import org.testng.Assert;
import utils.BaseTest;

import java.net.URI;
import java.util.HashMap;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static org.testng.Assert.assertEquals;

public class ScanQrCodeSteps extends BaseSteps {

    @When("click on scan qr code tab")
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
        verifyStepText(scanqrcode.getScanQRCodeStep1Label(), UiConstants.SCAN_QR_CODE_STEP1_LABEL, "Scan QR Code Step 1 label");
    }

    @When("Verify scan qr code step1 description")
    public void verifyScanQRCodeStep1Description() {
        verifyStepText(scanqrcode.getScanQRCodeStep1Description(), UiConstants.SCAN_QR_CODE_STEP1_DESCRIPTION, "Scan QR Code Step 1 description");
    }

    @When("Verify scan qr code step2 label")
    public void verifyScanQRCodeStep2Label() {
        verifyStepText(scanqrcode.getScanQRCodeStep2Label(), UiConstants.SCAN_QR_CODE_STEP2_LABEL, "Scan QR Code Step 2 label");
    }

    @When("Verify scan qr code step2 description")
    public void verifyScanQRCodeStep2Description() {
        verifyStepText(scanqrcode.getScanQRCodeStep2Description(), UiConstants.SCAN_QR_CODE_STEP2_DESCRIPTION, "Scan QR Code Step 2 description");
    }

    @When("Verify scan qr code step3 label")
    public void verifyScanQRCodeStep3Label() {
        verifyStepText(scanqrcode.getScanQRCodeStep3Label(), UiConstants.SCAN_QR_CODE_STEP3_LABEL, "Scan QR Code Step 3 label");
    }

    @When("Verify scan qr code step3 description")
    public void verifyScanQRCodeStep3Description() {
        verifyStepText(scanqrcode.getScanQRCodeStep3Description(), UiConstants.SCAN_QR_CODE_STEP3_DESCRIPTION, "Scan QR Code Step 3 description");
    }

    @When("Verify scan qr code step4 label")
    public void verifyScanQRCodeStep4Label() {
        verifyStepText(scanqrcode.getScanQRCodeStep4Label(), UiConstants.SCAN_QR_CODE_STEP4_LABEL, "Scan QR Code Step 4 label");
    }

    @When("Verify scan qr code step4 description")
    public void verifyScanQRCodeStep4Description() {
        verifyStepText(scanqrcode.getScanQRCodeStep4Description(), UiConstants.SCAN_QR_CODE_STEP4_DESCRIPTION, "Scan QR Code Step 4 description");
    }

    @When("Verify scan qr code area")
    public void verifyScanQRCodeArea() {
        assertVisible(scanqrcode.isVisibleScanQRCodeArea(), "Scan QR Code area is not visible.", "Scan QR Code area is visible.");
    }

    @When("verify scan qr code icon")
    public void verifyScanQRCodeIcon() {
        assertVisible(scanqrcode.isVisibleScanQRCodeIcon(), "Scan QR Code icon is not visible.", "Scan QR Code icon is visible.");
    }

    @When("verify scan qr code button")
    public void verifyScanQRCodeButton() {
        assertVisible(scanqrcode.isVisibleScanQRCodeButton(), "Scan QR Code button is not visible.", "Scan QR Code button is visible.");
    }

    @When("verify click on scan qr code button")
    public void verifyClickOnScanQRCodeButton() {
        verifyScanQRCodeButton();
    }

    @When("Verify scan qr code step2 label after")
    public void verifyScanQRCodeStep2LabelAfter() {
        try {
            Assert.assertEquals(scanqrcode.getScanQRCodeStep2Label(), UiConstants.SCAN_QR_CODE_STEP2_LABEL);
            Assert.assertEquals(scanqrcode.getScanQRCodeStep2LabelClass(), UiConstants.SCAN_QR_CODE_LABEL_FONT_AFTER_STEP);
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
            test.log(Status.PASS, "Successfully clicked on the Back button.");
        } catch (NoSuchElementException e) {
            logFailure(test, driver, "Failed to find the Back button while attempting to click", e);
            throw e;
        } catch (Exception e) {
            logFailure(test, driver, "Unexpected error while clicking on the Back button", e);
            throw e;
        }
    }

    @When("Verify scan line on scanning area")
    public void verify_scan_line_on_scanning_area() {
        assertVisible(scanqrcode.isVisibleScanLine(), "Scan line is not visible on the scanning area.", "Scan line is visible on the scanning area.");
    }

    @When("Verify idle timeout message for scan QR code")
    public void verify_idle_timeout_message_for_scan_qr_code() {
        verifyStepText(scanqrcode.getTextScannerTimeoutMessage(), UiConstants.ERROR_MESSAGE_SCAN_TIMEOUT, "Idle timeout message");
    }

    @When("Verify close button on timeout message")
    public void verify_close_button_on_timeout_message() {
        assertVisible(scanqrcode.isVisibleCloseIconTimeoutMessage(), "Close button on timeout message is not visible.", "Close button on timeout message is visible.");
    }

    @When("click on close button on timeout message")
    public void verify_click_on_close_button_on_timeout_message() {
        try {
            scanqrcode.clickOnCloseIconTimeoutMessage();
            test.log(Status.PASS, "Successfully clicked on the close button on the timeout message.");
        } catch (NoSuchElementException e) {
            logFailure(test, driver, "Element not found while clicking on close button on timeout message", e);
            throw e;
        } catch (Exception e) {
            logFailure(test, driver, "Unexpected error while clicking on close button on timeout message", e);
            throw e;
        }
    }

    @Then("validate timeout message disappears within five seconds")
    public void validateTimeoutMessageDisappearsWithinFiveSeconds() {
        try {
            assertTrue(
                    "Timeout message did not disappear within five seconds after clicking close icon.",
                    scanqrcode.isTimeoutMessageClosedWithinFiveSeconds()
            );
            test.log(Status.PASS, "Timeout message disappeared within five seconds after clicking close icon.");
        } catch (AssertionError e) {
            test.log(Status.FAIL, "Timeout message disappearance validation failed: " + e.getMessage());
            throw e;
        } catch (Exception e) {
            logFailure(test, driver, "Unexpected error while validating timeout message disappearance", e);
            throw e;
        }
    }

    @Then("verify scan qr code area")
    public void verifyScanQrCodeAreaAgain() {
        assertVisible(scanqrcode.isVisibleScanQRCodeArea(), "Verification failed: Scan QR Code area is not visible.", "Successfully verified that the Scan QR Code area is visible.");
    }

    @And("click on scan qr code button")
    public void clickOnScanQrCodeButton() {
        try {
            scanqrcode.clickOnScanQrCodeButton();
            test.log(Status.PASS, "Successfully clicked on scan QR code button.");
        } catch (NoSuchElementException e) {
            logFailure(test, driver, "Element not found while clicking scan QR code button", e);
            throw e;
        } catch (Exception e) {
            logFailure(test, driver, "Unexpected error while clicking scan QR code button", e);
            throw e;
        }
    }

    @Then("validate that QR code is scanned successfully")
    public void validateThatQRCodeIsScannedSuccessfully() {
        try {
            assertTrue("Verify if the success icon is displayed with valid VC", scanqrcode.isSuccessIconDisplayed());
            assertEquals(scanqrcode.getStatusMessage(), UiConstants.VERIFICATION_SUCCESS_MESSAGE);
            test.log(Status.PASS, "QR code scanned successfully.");
        } catch (AssertionError e) {
            logAssertionFailure(test, driver, "QR code scan validation failed", e);
            throw e;
        } catch (NoSuchElementException e) {
            logFailure(test, driver, "Element not found while validating QR code scan", e);
            throw e;
        } catch (Exception e) {
            logFailure(test, driver, "Unexpected error while validating QR code scan", e);
            throw e;
        }
    }

    @And("validate the step 3 after scanning the QR code")
    public void validateTheStep3AfterScanningTheQRCode() {
        validateStepAfterScan(scanqrcode.getScanQRCodeStep3Label(), scanqrcode.getScanQRCodeStep3LabelClass(), UiConstants.SCAN_QR_CODE_STEP3_LABEL);
    }

    @And("validate the step 4 after scanning the QR code")
    public void validateTheStep4AfterScanningTheQRCode() {
        validateStepAfterScan(scanqrcode.getScanQRCodeStep4Label(), scanqrcode.getScanQRCodeStep4LabelClass(), UiConstants.SCAN_QR_CODE_STEP4_LABEL);
    }

    @Then("validate the error message for half QR code")
    public void validateTheErrorMessageForHalfQRCode() {
        verifyGenericScanFailure("Half QR code should not be scanned successfully.");
        verifyAlertMessage(scanqrcode.getAlertMessage(), "Half QR code scan failure validated successfully.");
    }

    @Then("validate the error message for invalid QR code")
    public void validateTheErrorMessageForInvalidQRCode() {
        verifyStatusError(scanqrcode.isErrorIconDisplayed(), scanqrcode.getStatusMessage(), UiConstants.ERROR_MESSAGE_INVALID_QR, "Invalid QR code scan failure validated successfully.");
    }

    @Then("validate scan another qr code option is visible after invalid scan")
    public void validateScanAnotherQrCodeOptionIsVisibleAfterInvalidScan() {
        try {
            assertTrue(
                    "Scan another QR code option is not visible after invalid scan.",
                    scanqrcode.isScanAnotherQrCodeOptionVisible()
            );
            test.log(Status.PASS, "Scan another QR code option is visible after invalid scan.");
        } catch (AssertionError e) {
            test.log(Status.FAIL, "Scan another QR code option validation failed: " + e.getMessage());
            throw e;
        } catch (Exception e) {
            logFailure(test, driver, "Unexpected error while validating scan another QR code option after invalid scan", e);
            throw e;
        }
    }

    @Then("validate the error message for expired QR code")
    public void validateTheErrorMessageForExpiredQRCode() {
        verifyStatusError(scanqrcode.isErrorIconDisplayed(), scanqrcode.getStatusMessage(), UiConstants.ERROR_MESSAGE_EXPIRED_QR, "Expired QR code scan failure validated successfully.");
    }

    @And("deny camera access for scan qr code")
    public void denyCameraAccessForScanQrCode() {
        updateCameraPermissionForScanQrCode("denied", "deny");
    }

    @And("allow camera access for scan qr code")
    public void allowCameraAccessForScanQrCode() {
        updateCameraPermissionForScanQrCode("granted", "allow");
    }

    @And("reset camera access for scan qr code to prompt")
    public void resetCameraAccessForScanQrCodeToPrompt() {
        updateCameraPermissionForScanQrCode("prompt", "reset to prompt");
    }

    @Then("validate the camera access denied popup")
    public void validateTheCameraAccessDeniedPopup() {
        try {
            assertTrue("Verify if the camera access denied title is visible", scanqrcode.isCameraAccessDeniedTitleVisible());
            assertEquals(scanqrcode.getCameraAccessDeniedTitle(), UiConstants.CAMERA_ACCESS_DENIED_TITLE);
            assertTrue("Verify if the camera access denied description is visible", scanqrcode.isCameraAccessDeniedDescriptionVisible());
            assertEquals(scanqrcode.getCameraAccessDeniedDescription(), UiConstants.CAMERA_ACCESS_DENIED_DESCRIPTION);
            assertTrue("Verify if the camera denied icon is visible", scanqrcode.isCameraAccessDeniedIconVisible());
            assertTrue("Verify if the camera access denied Okay button is visible", scanqrcode.isOkayButtonVisible());
            assertEquals(scanqrcode.getOkayButtonText(), UiConstants.CAMERA_ACCESS_DENIED_OKAY_BUTTON);
            test.log(Status.PASS, "Camera access denied popup validated successfully.");
        } catch (AssertionError e) {
            test.log(Status.FAIL, "Camera access denied popup validation failed: " + e.getMessage());
            throw e;
        } catch (NoSuchElementException e) {
            logFailure(test, driver, "Element not found while validating camera access denied popup", e);
            throw e;
        } catch (Exception e) {
            logFailure(test, driver, "Unexpected error while validating camera access denied popup", e);
            throw e;
        }
    }

    @Then("validate camera can be accessed again for scan qr code")
    public void validateCameraCanBeAccessedAgainForScanQrCode() {
        assertVisible(scanqrcode.isVisibleScanLine(), "Verify if the scan line is visible after re-enabling camera access", "Camera access restored successfully for scan QR code flow.");
    }

    @Then("validate camera permission prompt flow is triggered for scan qr code")
    public void validateCameraPermissionPromptFlowIsTriggeredForScanQrCode() {
        try {
            assertTrue("Verify if the live scan area is visible when scan is initiated for the first time",
                    scanqrcode.isVisibleActiveScanVideo());
            assertTrue("Verify if the scanning line is visible when scan is initiated for the first time",
                    scanqrcode.isVisibleScanLine());
            assertTrue("Verify if the back action is available after opening the scan view for the first time",
                    scanqrcode.isVisibleBackButton());
            test.log(Status.PASS, "First-time scan QR code flow triggered camera permission request path.");
        } catch (AssertionError e) {
            test.log(Status.FAIL, "First-time camera permission flow validation failed: " + e.getMessage());
            throw e;
        } catch (Exception e) {
            logFailure(test, driver, "Unexpected error while validating first-time camera permission flow", e);
            throw e;
        }
    }

    @Then("validate scan qr code works without denied popup when camera access is allowed")
    public void validateScanQrCodeWorksWithoutDeniedPopupWhenCameraAccessIsAllowed() {
        try {
            assertTrue(
                    "Verify if scan remains active or verification has started after allowing camera access",
                    scanqrcode.isScanFlowActiveOrVerificationInProgress()
            );
            assertFalse("Camera access denied popup should not be visible when camera access is allowed.", scanqrcode.isCameraAccessDeniedTitleVisible());
            test.log(Status.PASS, "Scan QR code camera opened successfully without denied popup.");
        } catch (AssertionError e) {
            logAssertionFailure(test, driver, "Allowed camera access validation failed", e);
            throw e;
        } catch (Exception e) {
            logFailure(test, driver, "Unexpected error while validating allowed camera access for scan QR code", e);
            throw e;
        }
    }

    @And("turn off internet connection")
    public void turnOffInternetConnection() {
        try {
            if (BaseTest.isUsingBrowserStack()) {
                BaseTest.updateBrowserStackNetworkProfile("no-network");
                test.log(Status.PASS, "Internet connection disabled in BrowserStack using no-network profile.");
                return;
            }

            if (!(driver instanceof ChromeDriver chromeDriver)) {
                throw new IllegalStateException("Offline mode is supported only with local ChromeDriver or BrowserStack remote sessions.");
            }

            chromeDriver.executeCdpCommand("Network.enable", new HashMap<>());

            HashMap<String, Object> offlineSettings = new HashMap<>();
            offlineSettings.put("offline", true);
            offlineSettings.put("latency", 0);
            offlineSettings.put("downloadThroughput", 0);
            offlineSettings.put("uploadThroughput", 0);
            chromeDriver.executeCdpCommand("Network.emulateNetworkConditions", offlineSettings);

            test.log(Status.PASS, "Internet connection disabled in local Chrome for offline validation.");
        } catch (Exception e) {
            logFailure(test, driver, "Unable to disable internet connection for offline validation", e);
            throw e;
        }
    }

    @Then("validate the error message for no internet connection")
    public void validateTheErrorMessageForNoInternetConnection() {
        try {
            assertEquals(homePage.getNoInternetTitle(), UiConstants.NO_INTERNET_TITLE);
            assertEquals(homePage.getNoInternetDescription(), UiConstants.NO_INTERNET_DESCRIPTION);
            test.log(Status.PASS, "Offline scan failure state validated successfully.");
        } catch (AssertionError e) {
            test.log(Status.FAIL, "Offline scan validation failed: " + e.getMessage());
            throw e;
        } catch (Exception e) {
            logFailure(test, driver, "Unexpected error while validating offline scan behavior", e);
            throw e;
        }
    }

    @Then("validate scan does not work for visually modified qr code")
    public void validateScanDoesNotWorkForVisuallyModifiedQrCode() {
        verifyGenericScanFailure("Visually modified QR code should not be scanned successfully.");
        verifyAlertMessage(scanqrcode.getAlertMessage(), "Visual-effect QR code scan failure validated successfully.");
    }

    @And("validate that try again button is visible")
    public void validateThatTryAgainButtonIsVisible() {
        try {
            assertTrue("Verify if the 'Try Again' button is visible in offline scan failure state", homePage.isTryAgainButtonVisible());
            test.log(Status.PASS, "'Try Again' button visibility validated successfully in offline scan failure state.");
        } catch (AssertionError e) {
            test.log(Status.FAIL, "'Try Again' button visibility validation failed: " + e.getMessage());
            throw e;
        } catch (Exception e) {
            logFailure(test, driver, "Unexpected error while validating 'Try Again' button visibility in offline scan failure state", e);
            throw e;
        }
    }

    @When("click on try again button")
    public void clickOnTryAgainButton() {
        try {
            homePage.clickOnTryAgainButton();
            test.log(Status.PASS, "Successfully clicked on 'Try Again' button in offline scan failure state.");
        } catch (NoSuchElementException e) {
            logFailure(test, driver, "Element not found while clicking 'Try Again' button in offline scan failure state", e);
            throw e;
        } catch (Exception e) {
            logFailure(test, driver, "Unexpected error while clicking 'Try Again' button in offline scan failure state", e);
            throw e;
        }
    }

    private void updateCameraPermissionForScanQrCode(String setting, String action) {
        try {
            if (!(driver instanceof ChromeDriver chromeDriver)) {
                throw new IllegalStateException("Camera permission updates are supported only with local ChromeDriver.");
            }
            URI currentUri = URI.create(chromeDriver.getCurrentUrl());
            String origin = currentUri.getScheme() + "://" + currentUri.getAuthority();
            chromeDriver.executeCdpCommand("Browser.resetPermissions", new HashMap<>());

            if ("prompt".equals(setting)) {
                test.log(Status.PASS, "Camera access reset to prompt for scan QR code flow.");
                return;
            }

            HashMap<String, Object> permissionDescriptor = new HashMap<>();
            permissionDescriptor.put("name", "camera");
            HashMap<String, Object> permissionSettings = new HashMap<>();
            permissionSettings.put("permission", permissionDescriptor);
            permissionSettings.put("setting", setting);
            permissionSettings.put("origin", origin);
            chromeDriver.executeCdpCommand("Browser.setPermission", permissionSettings);
            test.log(Status.PASS, "Camera access updated to '" + action + "' for scan QR code flow.");
        } catch (Exception e) {
            logFailure(test, driver, "Unable to " + action + " camera access for scan QR code flow", e);
            throw e;
        }
    }

    private void verifyStepText(String actual, String expected, String label) {
        try {
            Assert.assertEquals(actual, expected, label + " does not match.");
            test.log(Status.PASS, "Verified " + label + " successfully. Expected: " + expected + ", Actual: " + actual);
        } catch (AssertionError e) {
            logFailure(test, driver, "Mismatch in " + label, e);
            throw e;
        } catch (NoSuchElementException e) {
            logFailure(test, driver, "Element not found while verifying " + label, e);
            throw e;
        } catch (Exception e) {
            logFailure(test, driver, "Unexpected error while verifying " + label, e);
            throw e;
        }
    }

    private void assertVisible(boolean visible, String failMessage, String passMessage) {
        try {
            Assert.assertTrue(visible, failMessage);
            test.log(Status.PASS, passMessage);
        } catch (AssertionError e) {
            logAssertionFailure(test, driver, failMessage, e);
            throw e;
        } catch (Exception e) {
            logFailure(test, driver, failMessage, e);
            throw e;
        }
    }

    private void validateStepAfterScan(String actualLabel, String actualClass, String expectedLabel) {
        try {
            Assert.assertEquals(actualLabel, expectedLabel);
            Assert.assertEquals(actualClass, UiConstants.SCAN_QR_CODE_LABEL_FONT_AFTER_STEP);
            test.log(Status.PASS, "Successfully validated step after scanning the QR code.");
        } catch (AssertionError e) {
            test.log(Status.FAIL, "Step label validation failed after scanning the QR code. Expected: '" + expectedLabel + "', Actual: '" + actualLabel + "'");
            throw e;
        } catch (NoSuchElementException e) {
            logFailure(test, driver, "Element not found while validating step after scanning the QR code", e);
            throw e;
        } catch (Exception e) {
            logFailure(test, driver, "Unexpected error while validating step after scanning the QR code", e);
            throw e;
        }
    }

    private void verifyAlertMessage(String actualMessage, String successMessage) {
        try {
            assertEquals(actualMessage, UiConstants.HALF_QRCODE_FAILURE_MESSAGE);
            test.log(Status.PASS, successMessage);
        } catch (AssertionError e) {
            logAssertionFailure(test, driver, successMessage + " Validation failed", e);
            throw e;
        } catch (NoSuchElementException e) {
            logFailure(test, driver, "Element not found while validating alert message", e);
            throw e;
        } catch (Exception e) {
            logFailure(test, driver, "Unexpected error while validating alert message", e);
            throw e;
        }
    }

    private void verifyGenericScanFailure(String failureMessage) {
        try {
            assertFalse(failureMessage, scanqrcode.isSuccessIconDisplayed());
            boolean failureVisible = scanqrcode.isErrorIconDisplayed()
                    || scanqrcode.isAlertMessageDisplayed()
                    || scanqrcode.isStatusMessageDisplayed();
            assertTrue("Expected scan failure feedback.", failureVisible);
            test.log(Status.PASS, "Scan failure state validated successfully.");
        } catch (AssertionError e) {
            logAssertionFailure(test, driver, "Generic scan failure validation failed", e);
            throw e;
        } catch (Exception e) {
            logFailure(test, driver, "Unexpected error while validating generic scan failure state", e);
            throw e;
        }
    }

    private void verifyStatusError(boolean errorIconDisplayed, String actualMessage, String expectedMessage, String successMessage) {
        try {
            assertTrue("Verify if the error icon is displayed", errorIconDisplayed);
            assertEquals(actualMessage, expectedMessage);
            test.log(Status.PASS, successMessage);
        } catch (AssertionError e) {
            logAssertionFailure(test, driver, successMessage + " Validation failed", e);
            throw e;
        } catch (NoSuchElementException e) {
            logFailure(test, driver, "Element not found while validating scan failure", e);
            throw e;
        } catch (Exception e) {
            logFailure(test, driver, "Unexpected error while validating scan failure", e);
            throw e;
        }
    }
}
