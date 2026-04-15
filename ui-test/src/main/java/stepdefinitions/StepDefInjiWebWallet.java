package stepdefinitions;

import static org.testng.Assert.assertEquals;
import static org.testng.Assert.assertTrue;

import org.apache.commons.lang.exception.ExceptionUtils;
import org.openqa.selenium.By;
import org.openqa.selenium.NoSuchElementException;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.WebElement;
import org.openqa.selenium.support.ui.ExpectedConditions;
import org.openqa.selenium.support.ui.WebDriverWait;

import java.time.Duration;
import java.util.List;

import com.aventstack.extentreports.ExtentTest;
import com.aventstack.extentreports.Status;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import api.InjiVerifyConfigManager;
import constants.UiConstants;
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

public class StepDefInjiWebWallet {

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
	    String idToken = HttpUtils.getIdToken();
		String sessionCookie = HttpUtils.getSessionCookieFromIdToken(idToken);
		driver.get(HttpUtils.getInjiWebBaseUrl());
		try {
			injiWebWalletPage.applySessionCookie(sessionCookie);
			injiWebWalletPage.waitForWalletPageReady();
			test.log(Status.PASS, "User successfully performed token-based login to inji-web wallet");
		} catch (AssertionError | NoSuchElementException e) {
			test.log(Status.FAIL, "Assertion/Element error: " + e.getMessage());
			test.log(Status.FAIL, ExceptionUtils.getStackTrace(e));
			ScreenshotUtil.attachScreenshot(driver, "FailureScreenshot");
			throw e;
		}
	}


	@Then("User unlocks inji-web wallet with configured passcode")
	public void user_unlocks_inji_web_wallet_with_configured_passcode() {
		try {
			String passcode = HttpUtils.get("INJIWEB_PASSCODE");
			injiWebWalletPage.enterPasscode(passcode);
			test.log(Status.PASS, "Successfully entered wallet passcode.");
		} catch (Exception e) {
			logFailure("Failed to unlock inji-web wallet with passcode", e);
			throw e;
		}
	}

	@Then("User click on submit button")
	public void user_clicks_on_submit_button() {
		try {
			injiWebWalletPage.clickSubmitPasscode();
			test.log(Status.PASS, "Successfully clicked submit button.");
		} catch (Exception e) {
			logFailure("Failed to click submit button", e);
			throw e;
		}
	}

		@Then("User confirms inji-web wallet with configured passcode")
	public void user_confirms_inji_web_wallet_with_configured_passcode() {
		try {
			String passcode = HttpUtils.get("INJIWEB_PASSCODE");
			injiWebWalletPage.enterConfirmPasscode(passcode);
			injiWebWalletPage.clickSubmitPasscode();
			test.log(Status.PASS, "Successfully entered wallet passcode.");
		} catch (Exception e) {
			logFailure("Failed to confirm inji-web wallet with passcode", e);
			throw e;
		}
	}

	@Then("User opens the inji-web issuer catalog")
	public void user_opens_the_inji_web_issuer_catalog() {
		try {
			injiWebWalletPage.navigateToIssuersPage();
			test.log(Status.PASS, "Successfully navigated to the inji-web issuer catalog.");
		} catch (Exception e) {
			logFailure("Failed to open inji-web issuer catalog", e);
			throw e;
		}
	}

	@Then("User switches back to inji-verify tab")
	public void user_switches_back_to_inji_verify_tab() {
		try {
			homePage.switchToVerifyTab();
			test.log(Status.PASS, "Successfully switched back to inji-verify tab.");
		} catch (Exception e) {
			logFailure("Failed to switch back to inji-verify tab", e);
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
			logFailure("Failed to verify stored Health Insurance credential in inji-web wallet", e);
			throw e;
		}
	}

	@Then("User selects configured inji-web wallet in verifier")
	public void user_selects_configured_inji_web_wallet_in_verifier() {
		try {
			   vpVerification.selectWallet();
            test.log(Status.PASS, "Selected the configured inji-web wallet from wallet chooser.");
        } catch (NoSuchElementException e) {
            logFailure("Failed to select the configured inji-web wallet", e);
            throw e;
        }
	}

	@Then("User clicks on Proceed button")
	public void user_clicks_on_proceed_button() {
		try {
g			   vpVerification.proccedButton();
            test.log(Status.PASS, "Selected proceed button.");
        } catch (NoSuchElementException e) {
            logFailure("Failed to select the proceed button", e);
            throw e;
        }
	}

	
	@Then("User click on trust verifier")
	public void user_clicks_on_trust_verifier() {
		try {
			   vpVerification.trustButton();
            test.log(Status.PASS, "Selected the trust verifier button.");
        } catch (NoSuchElementException e) {
            logFailure("Failed to select the trust verifier button", e);
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
			logFailure("Failed to reach inji-web wallet presentation page", e);
			throw e;
		}
	}

	@Then("User presents the requested health insurance credential from inji-web wallet")
	public void user_presents_the_requested_health_insurance_credential_from_inji_web_wallet() {
		try {
			injiWebWalletPage.selectCredentialForPresentation();
			injiWebWalletPage.clickConsentAndShareButton();
			test.log(Status.PASS, "Successfully presented the requested Health Insurance credential from inji-web wallet.");
		} catch (Exception e) {
			logFailure("Failed to present the requested Health Insurance credential", e);
			throw e;
		}
	}

	@Then("User selects the credential for verification")
	public void user_selects_the_credential_for_verification() {
		try {
			injiWebWalletPage.selectCredentialForPresentation();
			test.log(Status.PASS, "Successfully selected the credential for verification.");
		} catch (Exception e) {
			logFailure("Failed to select the credential for verification", e);
			throw e;
		}
	}

	@Then("User select the second credential for verification")
	public void user_selects_the_second_credential_for_verification() {
		try {
			injiWebWalletPage.selectSecondCredentialForPresentation();
			test.log(Status.PASS, "Successfully selected the second credential for verification.");
		} catch (Exception e) {
			logFailure("Failed to select the second credential for verification", e);
			throw e;
		}
	}

	@Then("User clicks consent and share button")
	public void user_clicks_consent_and_share_button() {
		try {
			injiWebWalletPage.clickConsentAndShareButton();
			test.log(Status.PASS, "Successfully clicked consent and share button.");
		} catch (Exception e) {
			logFailure("Failed to click consent and share button", e);
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
            logFailure("Failed to verify error message. Expected: " 
                + UiConstants.NO_MATCHING_CREDENTIALS_ERROR + ", but found: " + injiWebWalletPage.isNoMatchingCredentialsErrorVisible(), e);
            throw e;
        }
	}
	
	@When("User Click on home button")
    public void user_clicks_on_home_button() {
        try {
            injiWebWalletPage.clickOnHomeButton();
            test.log(Status.PASS, "Successfully clicked on the 'Home' button.");
        } catch (NoSuchElementException e) {
            logFailure("Element not found while clicking the 'Home' button", e);
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
			logFailure("Failed to verify VP result posted back to inji-verify", e);
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
	        logFailure("Element not found while verifying message for valid QR code", e);
	        throw e;
	    }
	}

	@Then("Health Insurance is selected and MOSIP ID is unselected")
	public void health_insurance_is_selected_and_mosip_id_is_unselected() {
		try {
			assertTrue(vpVerification.isHealthInsuranceSelected(), "Health Insurance should be selected.");
			assertTrue(!vpVerification.isMosipIdSelected(), "MOSIP ID should be unselected.");
			test.log(Status.PASS, "Verified Health Insurance is selected and MOSIP ID is unselected.");
		} catch (Exception e) {
			logFailure("Failed to verify VP credential selection state", e);
			throw e;
		}
	}

	private void logFailure(String message, Throwable throwable) {
		test.log(Status.FAIL, message + ": " + throwable.getMessage());
		if (throwable instanceof Exception) {
			test.log(Status.FAIL, ExceptionUtils.getStackTrace((Exception) throwable));
		}
		ScreenshotUtil.attachScreenshot(driver, "FailureScreenshot");
	}
}
