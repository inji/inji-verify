package stepdefinitions;

import static org.testng.Assert.assertEquals;
import static org.testng.Assert.assertTrue;

import org.apache.commons.lang3.exception.ExceptionUtils;
import org.openqa.selenium.By;
import org.openqa.selenium.NoSuchElementException;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.WebElement;
import org.openqa.selenium.support.ui.ExpectedConditions;
import org.openqa.selenium.support.ui.WebDriverWait;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.time.Duration;
import java.util.List;

import com.aventstack.extentreports.ExtentTest;
import com.aventstack.extentreports.Status;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import api.InjiVerifyConfigManager;
import constants.UiConstants;
import io.cucumber.java.en.And;
import io.cucumber.java.en.Then;
import io.cucumber.java.en.When;
import pages.HomePage;
import pages.InjiWebWalletPage;
import pages.UploadQRCode;
import pages.VpVerification;
import utils.BaseTest;
import utils.ExtentReportManager;
import utils.HttpUtils;
import utils.ScreenshotUtil;

public class StepDefInjiWebWallet extends BaseSteps {   // <-- extends BaseSteps

    private static final Logger logger = LoggerFactory.getLogger(StepDefInjiWebWallet.class);

    private final WebDriver driver;
    private final BaseTest baseTest;
    private final HomePage homePage;
    private final InjiWebWalletPage injiWebWalletPage;
    private final VpVerification vpVerification;
    private final UploadQRCode uploadQRCode;
    private final ExtentTest test = ExtentReportManager.getTest();

    private static final String CONFIGURED_WALLET_NAME = InjiVerifyConfigManager.getproperty("injiWebWalletName");
    private static final String HEALTH_INSURANCE_CREDENTIAL =
            InjiVerifyConfigManager.getproperty("stayProtectedIssuerCredentialType");

    public StepDefInjiWebWallet() {
        this.baseTest = new BaseTest();
        this.driver = baseTest.getDriver();
        if (driver == null) {
            throw new RuntimeException("WebDriver is null in StepDefInjiWebWallet.");
        }
        this.homePage = new HomePage(driver);
        this.injiWebWalletPage = new InjiWebWalletPage(driver);
        this.vpVerification = new VpVerification(driver);
        this.uploadQRCode = new UploadQRCode(driver);
    }

    @When("User performs token-based login to inji-web wallet")
    public void user_performs_token_based_login_to_inji_web_wallet() throws Exception {
        final int maxAttempts = 3;
        for (int attempt = 1; attempt <= maxAttempts; attempt++) {
            try {
                String idToken = HttpUtils.getIdToken();
                String sessionCookie = HttpUtils.getSessionCookieFromIdToken(idToken);
                driver.get(HttpUtils.getInjiWebBaseUrl());
                injiWebWalletPage.applySessionCookie(sessionCookie);
                injiWebWalletPage.waitForWalletPageReady();
                injiWebWalletPage.waitForWalletUserPageReady();
                test.log(Status.PASS, "User successfully performed token-based login to inji-web wallet (attempt "
                        + attempt + "/" + maxAttempts + ")");
                return;
            } catch (AssertionError | RuntimeException e) {
                if (attempt == maxAttempts) {
                    logFailure(test, driver, "Token-based login failed after " + maxAttempts + " attempts", e);
                    throw e;
                }
                test.log(Status.WARNING, "Token-based login attempt " + attempt + "/" + maxAttempts
                        + " failed: " + e.getMessage() + ". Retrying...");
            } catch (IOException e) {
                logger.warn("Token-based login attempt {}/{} failed", attempt, maxAttempts, e);

                if (attempt == maxAttempts) {
                    //the Exception is set to null because we don't want to log the request and response body of google login present in the IOException error message in extent report.
                    logFailure(test, driver, "Token-based login failed after " + maxAttempts + " attempts", (Exception)null);
                    throw e;
                }

                test.log(Status.WARNING, "Token-based login attempt " + attempt + "/" + maxAttempts
                        + " failed. Retrying...");
            }
        }
    }

    @Then("User unlocks inji-web wallet with configured passcode")
    public void user_unlocks_inji_web_wallet_with_configured_passcode() {
        try {
            String passcode = HttpUtils.get("INJIWEB_PASSCODE");
            injiWebWalletPage.enterPasscode(passcode);
            test.log(Status.PASS, "Successfully entered wallet passcode.");
        } catch (Exception e) {
            logFailure(test, driver, "Failed to unlock inji-web wallet with passcode", e);
            throw e;
        }
    }

    @Then("User click on submit button")
    public void user_clicks_on_submit_button() {
        try {
            injiWebWalletPage.clickSubmitPasscode();
            test.log(Status.PASS, "Successfully clicked submit button.");
        } catch (Exception e) {
            logFailure(test, driver, "Failed to click submit button", e);
            throw e;
        }
    }

    @Then("User confirms inji-web wallet with configured passcode")
    public void user_confirms_inji_web_wallet_with_configured_passcode() {
        try {
            String passcode = HttpUtils.get("INJIWEB_PASSCODE");
            if (injiWebWalletPage.isConfirmPasscodeVisible()) {
                injiWebWalletPage.enterConfirmPasscode(passcode);
                injiWebWalletPage.clickSubmitPasscode();
                test.log(Status.PASS, "Confirm passcode field was visible — passcode confirmed and submitted.");
            } else {
                test.log(Status.PASS, "Confirm passcode field not visible — skipping confirmation step.");
                injiWebWalletPage.clickSubmitPasscode();
            }
        } catch (Exception e) {
            logFailure(test, driver, "Failed to confirm inji-web wallet with passcode", e);
            throw e;
        }
    }

    @Then("User opens the inji-web issuer catalog")
    public void user_opens_the_inji_web_issuer_catalog() {
        try {
            injiWebWalletPage.navigateToIssuersPage();
            test.log(Status.PASS, "Successfully navigated to the inji-web issuer catalog.");
        } catch (Exception e) {
            logFailure(test, driver, "Failed to open inji-web issuer catalog", e);
            throw e;
        }
    }

    @Then("User switches back to inji-verify tab")
    public void user_switches_back_to_inji_verify_tab() {
        try {
            homePage.switchToVerifyTab();
            test.log(Status.PASS, "Successfully switched back to inji-verify tab.");
        } catch (Exception e) {
            logFailure(test, driver, "Failed to switch back to inji-verify tab", e);
            throw e;
        }
    }

    @Then("User verifies health insurance credential is stored in inji-web wallet")
    public void user_verifies_health_insurance_credential_is_stored_in_inji_web_wallet() {
        try {
            injiWebWalletPage.navigateToUserCredentialsPage();
            injiWebWalletPage.waitForCredentialListPage();
            assertTrue(injiWebWalletPage.isCredentialStored(HEALTH_INSURANCE_CREDENTIAL),
                    "Health Insurance credential is not visible in inji-web wallet.");
            test.log(Status.PASS, "Health Insurance credential is present in inji-web wallet.");
        } catch (Exception e) {
            logFailure(test, driver, "Failed to verify stored Health Insurance credential in inji-web wallet", e);
            throw e;
        }
    }

    @Then("User selects configured inji-web wallet in verifier")
    public void user_selects_configured_inji_web_wallet_in_verifier() {
        try {
            vpVerification.selectWallet();
            test.log(Status.PASS, "Selected the configured inji-web wallet from wallet chooser.");
        } catch (NoSuchElementException e) {
            logFailure(test, driver, "Failed to select the configured inji-web wallet", e);
            throw e;
        }
    }

    @Then("User clicks on Proceed button")
    public void user_clicks_on_proceed_button() {
        try {
            vpVerification.clickOnProceedButton();
            test.log(Status.PASS, "Selected proceed button.");
        } catch (NoSuchElementException e) {
            logFailure(test, driver, "Failed to select the proceed button", e);
            throw e;
        }
    }

    @Then("User click on trust verifier")
    public void user_clicks_on_trust_verifier() {
        try {
        if (vpVerification.isTrustButtonVisible()) {
                vpVerification.trustButton();
                test.log(Status.PASS, "Selected the trust verifier button.");
            } else {
                test.log(Status.PASS, "Trust verifier button not visible — skipping step.");
            }
        } catch (Exception e) {
            logFailure(test, driver, "Unexpected error while clicking the trust verifier button", e);
            throw e;
        }
    }

    @Then("User is redirected to inji-web wallet presentation page")
    public void user_is_redirected_to_inji_web_wallet_presentation_page() {
        try {
            injiWebWalletPage.switchToNewestWindowIfNeeded();
            injiWebWalletPage.focusCurrentWindow();
            injiWebWalletPage.waitForPresentationPage();
            assertTrue(injiWebWalletPage.isPresentationActionVisible(),
                    "Wallet presentation page did not show a present/share action.");
            test.log(Status.PASS, "Successfully reached inji-web wallet presentation page.");
        } catch (Exception e) {
            logFailure(test, driver, "Failed to reach inji-web wallet presentation page", e);
            throw e;
        }
    }

    @Then("User presents the requested health insurance credential from inji-web wallet")
    public void user_presents_the_requested_health_insurance_credential_from_inji_web_wallet() {
        try {
            injiWebWalletPage.selectCredentialForPresentation();
            injiWebWalletPage.clickShowConsentModalButton();
            test.log(Status.PASS, "Successfully presented the requested Health Insurance credential from inji-web wallet.");
        } catch (Exception e) {
            logFailure(test, driver, "Failed to present the requested Health Insurance credential", e);
            throw e;
        }
    }

    @Then("User selects the credential for verification")
    public void user_selects_the_credential_for_verification() {
        try {
            injiWebWalletPage.selectCredentialForPresentation();
            test.log(Status.PASS, "Successfully selected the credential for verification.");
        } catch (Exception e) {
            logFailure(test, driver, "Failed to select the credential for verification", e);
            throw e;
        }
    }

    @Then("User select the second credential for verification")
    public void user_selects_the_second_credential_for_verification() {
        try {
            injiWebWalletPage.selectSecondCredentialForPresentation();
            test.log(Status.PASS, "Successfully selected the second credential for verification.");
        } catch (Exception e) {
            logFailure(test, driver, "Failed to select the second credential for verification", e);
            throw e;
        }
    }

    @Then("User clicks disagree button")
    public void user_clicks_disagree_button() {
        try {
            injiWebWalletPage.clickDeclineButton();
            test.log(Status.PASS, "Successfully clicked the decline button.");
        } catch (Exception e) {
            logFailure(test, driver, "Failed to click the decline button", e);
            throw e;
        }
    }

    @Then("verify disagreed to share error message")
    public void verify_disagreed_to_share_error_message() {
        try {
            injiWebWalletPage.waitForVerifyRedirect();
            vpVerification.waitForVerifyResults();
            String actualMessage = vpVerification.getInformationMessage();
            assertEquals(actualMessage, UiConstants.DECLINED_TO_SHARE_ERROR,
                "Declined to share error message does not match the expected value.");
            test.log(Status.PASS, "Successfully verified the declined to share error message: " + actualMessage);
        } catch (Exception e) {
            logFailure(test, driver, "Failed to verify declined to share error message. Expected: "
                + UiConstants.DECLINED_TO_SHARE_ERROR
                + ", but found: " + vpVerification.getInformationMessage(), e);
            throw e;
        }
    }

    @Then("User clicks consent and share button")
    public void user_clicks_consent_and_share_button() {
        try {
            injiWebWalletPage.clickShowConsentModalButton();
            test.log(Status.PASS, "Successfully clicked consent and share button.");
        } catch (Exception e) {
            logFailure(test, driver, "Failed to click consent and share button", e);
            throw e;
        }
    }

    @Then("User clicks consent and share card button")
    public void user_clicks_consent_and_share_card_button() {
        try {
            injiWebWalletPage.clickConsentAndShareCardButton();
            test.log(Status.PASS, "Successfully clicked consent and share card button.");
        } catch (Exception e) {
            logFailure(test, driver, "Failed to click consent and share card button", e);
            throw e;
        }
    }

    @Then("Verify error message is displayed")
    public void verify_error_message_is_displayed() {
        try {
            String actualToastMessage = injiWebWalletPage.isNoMatchingCredentialsErrorVisible();
            assertEquals(actualToastMessage, UiConstants.NO_MATCHING_CREDENTIALS_ERROR,
                "Toast message does not match the expected value.");
            test.log(Status.PASS, "Successfully verified the error message: " + actualToastMessage);
        } catch (Exception e) {
            logFailure(test, driver, "Failed to verify error message. Expected: "
                + UiConstants.NO_MATCHING_CREDENTIALS_ERROR
                + ", but found: " + injiWebWalletPage.isNoMatchingCredentialsErrorVisible(), e);
            throw e;
        }
    }

    @When("User Click on home button")
    public void user_clicks_on_home_button() {
        try {
            injiWebWalletPage.clickOnHomeButton();
            test.log(Status.PASS, "Successfully clicked on the 'Home' button.");
        } catch (NoSuchElementException e) {
            logFailure(test, driver, "Element not found while clicking the 'Home' button", e);
            throw e;
        }
    }

    @Then("VP result is posted back to inji-verify successfully")
    public void vp_result_is_posted_back_to_inji_verify_successfully() {
        try {
            injiWebWalletPage.waitForVerifyRedirect();
            vpVerification.waitForVerifyResults();

            assertTrue(uploadQRCode.isTickIconVisible(), "VP success icon is not visible in inji-verify.");
            assertEquals(uploadQRCode.getCongratulationtext(), UiConstants.CONGRATULATIONS_MESSAGE,
                    "Unexpected VP success message after returning to inji-verify.");

            test.log(Status.PASS, "VP result was posted back to inji-verify successfully.");
        } catch (Exception e) {
            logFailure(test, driver, "Failed to verify VP result posted back to inji-verify", e);
            throw e;
        }
    }

    @When("VP result for partial sharing")
    public void vp_result_for_partial_sharing() {
        try {
            String actualMessage = injiWebWalletPage.getErrorMessageForPartialSharing();
            assertEquals(actualMessage, UiConstants.PARTIAL_SHARING_MESSAGE);
            test.log(Status.PASS, "Valid QR code message verified successfully: " + actualMessage);
        } catch (NoSuchElementException e) {
            logFailure(test, driver, "Element not found while verifying message for valid QR code", e);
            throw e;
        }
    }

   @Then("Credential {string} is selected and credential {string} is not selected")
    public void credential_is_selected_and_other_is_not(String selectedKeyOrName, String unselectedKeyOrName) {
        try {
            String selectedName = api.VerifiableClaimsConfigManager.resolveCredentialName(selectedKeyOrName);
            String unselectedName = api.VerifiableClaimsConfigManager.resolveCredentialName(unselectedKeyOrName);
            assertTrue(vpVerification.isCredentialSelected(selectedName),
                    "Credential '" + selectedName + "' should be selected.");
            assertTrue(!vpVerification.isCredentialSelected(unselectedName),
                    "Credential '" + unselectedName + "' should be unselected.");
            test.log(Status.PASS, "Verified '" + selectedName + "' is selected and '" + unselectedName + "' is unselected.");
        } catch (Exception e) {
            logFailure(test, driver, "Failed to verify VP credential selection state", e);
            throw e;
        }
    }

    @Then("Verify if user is logged into injiweb")
    public void verify_if_user_is_logged_into_injiweb() {
        try {
            assertTrue(injiWebWalletPage.checkLoginElements(),
                    "User is not logged into inji-web: URL does not contain 'injiweb'/'/user/', or profile pic/name are not visible.");
            test.log(Status.PASS, "User is logged into inji-web — URL, profile pic, and profile name are all visible.");
        } catch (AssertionError e) {
            logAssertionFailure(test, driver, "Failed to verify user is logged into inji-web", e);
            throw e;
        } catch (Exception e) {
            logFailure(test, driver, "Failed to verify user is logged into inji-web", e);
            throw e;
        }
    }

    @And("User clicks on show more cards")
    public void user_clicks_on_show_more_cards() {
        try {
            injiWebWalletPage.clickShowMoreCardsButton();
            test.log(Status.PASS, "Successfully clicked on show more cards button.");
        } catch (Exception e) {
            logFailure(test, driver, "Failed to click on show more cards button", e);
            throw e;
        }
    }

    // NO private logFailure here — inherited from BaseSteps
}