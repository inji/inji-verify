package pages;

import base.BasePage;
import constants.UiConstants;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.WebElement;
import org.openqa.selenium.support.FindBy;
import org.openqa.selenium.support.PageFactory;
import org.openqa.selenium.support.ui.ExpectedConditions;
import org.openqa.selenium.support.ui.WebDriverWait;
import org.openqa.selenium.TimeoutException;

import java.time.Duration;
import utils.WaitUtil;

public class ScanQRCodePage extends BasePage {

	public ScanQRCodePage(WebDriver driver) {
		super(driver);
		PageFactory.initElements(driver, this);
	}

	@FindBy(xpath = "//*[@id='scan-qr-code-tab']")
	WebElement ScanQRButtonTab;

	@FindBy(xpath = "//div[@id='scan-qr-code']")
	WebElement ScanQRCodeStep1Label;

	@FindBy(xpath = "//div[@id='scan-qr-code-description']")
	WebElement ScanQRCodeStep1Description;

	@FindBy(id = "activate-camera-and-position-qr-code")
	WebElement ScanQRCodeStep2Label;

	@FindBy(xpath = "//div[@id='activate-camera-and-position-qr-code-description']")
	WebElement ScanQRCodeStep2Description;

	@FindBy(id = "verification-in-progress")
	WebElement ScanQRCodeStep3Label;

	@FindBy(xpath = "//div[@id='verification-in-progress-description']")
	WebElement ScanQRCodeStep3Description;

	@FindBy(xpath = "(//div[@class='ml-[10px] text-[16px]  font-bold text-[#868686]'])[3]")
	WebElement ScanQRCodeStep4Label;

	@FindBy(xpath = "//div[@id='view-result-description']")
	WebElement ScanQRCodeStep4Description;

	@FindBy(xpath = "//div[@class='grid bg-default_theme-lighter-gradient rounded-[12px] w-[250px] lg:w-[320px] aspect-square content-center justify-center']")
	WebElement ScanQRCodeArea;

	@FindBy(xpath = "//*[name()='svg' and @width='24' and @height='22']")
	WebElement ScanQRCodeIcon;

	@FindBy(id="scan-button")
	WebElement scanQRCodeButton;

	@FindBy(xpath="//span[text()='Verify Another QR code']")
	WebElement verifyAnotherQRCodeButton;

	@FindBy(xpath = "(//div[contains(@class,'bg-default_theme-gradient') and contains(@class,'rounded-full')]/div[text()='3'])")
	WebElement ScanQRCodeStep2LabelAfter;

	@FindBy(xpath = "//div[@style='width: 316px; padding-top: 100%; overflow: hidden; position: relative; place-content: center; display: grid; place-items: center; border-radius: 12px;']")
	WebElement ImageAreaElement;

	@FindBy(xpath = "//button[@id='verification-back-button']")
	WebElement backButton;

	@FindBy(css = "div.qrcode-container video.qr-video")
	WebElement activeScanVideo;

	@FindBy(id="camera-access-denied-okay-button")
	WebElement okayButton;

	@FindBy(id = "camera-access-denied")
	WebElement cameraAccessDeniedTitle;

	@FindBy(id = "camera-access-denied-description")
	WebElement cameraAccessDeniedDescription;

	@FindBy(xpath = "//*[name()='svg' and @width='54' and @height='53']")
	WebElement cameraAccessDeniedIcon;

	@FindBy(xpath = "//div[@id='scanning-line']")
	WebElement ScanLine;

	//@FindBy(xpath = "//div[@class = 'fixed top-[80px] lg:top-[44px] right-4 lg:right-2] py-[22px] px-[18px] text-white rounded-[12px] shadow-lg bg-[#D73E3E] ']")
	@FindBy(xpath = "//*[@id='alert-message']")	
	WebElement alertMessage;

	@FindBy(id = "close_icon")
	WebElement CloseIconTimeoutMessage;

	@FindBy(id = "vc-result-display-message")
	WebElement statusMessage;

	@FindBy(id = "success_message_icon")
	WebElement successMessageIcon;

	@FindBy(id = "failed_icon")
	WebElement failedIcon;

	public void clickOnScanQRButtonTab() {
		clickOnElement(driver, ScanQRButtonTab);
	}

	public String getScanQRCodeStep1Label() {
		return getText(driver, ScanQRCodeStep1Label);

	}

	public String getScanQRCodeStep1Description() {
		return getText(driver, ScanQRCodeStep1Description);

	}

	public String getScanQRCodeStep2Label() {
		return getText(driver, ScanQRCodeStep2Label);

	}

	public String getScanQRCodeStep2Description() {
		return getText(driver, ScanQRCodeStep2Description);

	}

	public String getScanQRCodeStep3Label() {
		waitForScanVerificationResultState();
		return getText(driver, ScanQRCodeStep3Label);

	}

	public String getScanQRCodeStep3Description() {
		return getText(driver, ScanQRCodeStep3Description);

	}

	public String getScanQRCodeStep4Label() {
		waitForScanVerificationResultState();
		return getText(driver, ScanQRCodeStep4Label);

	}

	public String getScanQRCodeStep4Description() {
		return getText(driver, ScanQRCodeStep4Description);

	}

	public Boolean isVisibleScanQRCodeArea() {
		return isElementIsVisible(driver, ScanQRCodeArea);
	}

	public Boolean isVisibleScanQRCodeIcon() {
		return isElementIsVisible(driver, ScanQRCodeIcon);
	}

	public Boolean isVisibleScanQRCodeButton() {
		return isElementIsVisible(driver, scanQRCodeButton);
	}

	public boolean isVisibleScanQRCodeStep2LabelAfter() {
		return isElementIsVisible(driver, ScanQRCodeStep2LabelAfter);
	}

	public boolean isVisibleImageAreaElement() {
		return isElementIsVisible(driver, ImageAreaElement);
	}

	public boolean isVisibleBackButton() {
		return isElementIsVisible(driver, backButton);
	}

	public boolean isVisibleActiveScanVideo() {
		return isElementIsVisible(driver, activeScanVideo);
	}

	public boolean isVerificationInProgressVisible() {
		return isElementIsVisible(driver, ScanQRCodeStep3Label);
	}

	public boolean isScanFlowActiveOrVerificationInProgress() {
		return isCameraUiActiveWithoutWait()
				|| isVerificationInProgressVisible();
	}

	public boolean isScanFlowActiveOrVerificationInProgressWithoutWait() {
		return isCameraUiActiveWithoutWait()
				|| isDisplayedWithoutWaiting(ScanQRCodeStep3Label);
	}

	public boolean isCameraAccessRestoredAndScanUsable() {
		return isCameraUiActiveWithoutWait()
				|| hasFinalScanVerificationResultVisible();
	}

	public boolean isScanFlowUsableAfterCameraPermission() {
		waitForCameraPermissionOutcome();
		waitForStableCameraUsableState();
		return !isCameraAccessDeniedTitleVisibleWithoutWait()
				&& (isCameraUiActiveWithoutWait() || hasFinalScanVerificationResultVisible());
	}

	public boolean isCameraPermissionPromptFlowTriggered() {
		waitForCameraPermissionOutcome();
		return !isCameraAccessDeniedTitleVisibleWithoutWait()
				&& (isCameraUiActiveWithoutWait()
				|| isDisplayedWithoutWaiting(ScanQRCodeStep3Label)
				|| hasFinalScanVerificationResultVisible()
				|| isScanCompletionStepVisible());
	}

	public void clickOnBackButton() {
		clickOnElement(driver, backButton);
	}

	public void clickOnOkayButton() {
		clickOnElement(driver, okayButton);
		waitForCameraAccessDeniedPopupToClose();
		WaitUtil.waitForClickability(driver, scanQRCodeButton);
	}

	public boolean isCameraAccessDeniedTitleVisible() {
		return isElementIsVisible(driver, cameraAccessDeniedTitle);
	}

	public boolean isCameraAccessDeniedTitleVisibleWithoutWait() {
		return isDisplayedWithoutWaiting(cameraAccessDeniedTitle);
	}

	public String getCameraAccessDeniedTitle() {
		return getText(driver, cameraAccessDeniedTitle);
	}

	public boolean isCameraAccessDeniedDescriptionVisible() {
		return isElementIsVisible(driver, cameraAccessDeniedDescription);
	}

	public String getCameraAccessDeniedDescription() {
		return getText(driver, cameraAccessDeniedDescription);
	}

	public boolean isCameraAccessDeniedIconVisible() {
		return isElementIsVisible(driver, cameraAccessDeniedIcon);
	}

	public boolean isOkayButtonVisible() {
		return isElementIsVisible(driver, okayButton);
	}

	public String getOkayButtonText() {
		return getText(driver, okayButton);
	}

	public boolean isVisibleScanLine() {
		return isElementIsVisible(driver, ScanLine);
	}

	public String getTextScannerTimeoutMessage() {

		waitForElementVisibleWithPolling(driver, alertMessage);

		return getText(driver, alertMessage);
	}

	public boolean isVisibleCloseIconTimeoutMessage() {
		return isElementIsVisible(driver, CloseIconTimeoutMessage);
	}

	public void clickOnCloseIconTimeoutMessage() {
		clickOnElement(driver, CloseIconTimeoutMessage);
	}

	public boolean isTimeoutMessageClosedWithinFiveSeconds() {
		try {
			return new WebDriverWait(driver, Duration.ofSeconds(5))
					.until(ExpectedConditions.invisibilityOf(alertMessage));
		} catch (Exception e) {
			return false;
		}
	}

	public boolean isScanAnotherQrCodeOptionVisible() {
		return isElementIsVisible(driver, verifyAnotherQRCodeButton);
	}


	public void clickOnScanQrCodeButton() {
		clickOnElement(driver, scanQRCodeButton);
	}

	public void clickOnScanQrCodeButtonAndWaitForFlow() {
		executeScanFlowWithSingleRecovery();
	}

	public String getScanQRCodeStep2LabelClass() {
		return getAttributeValue(driver, ScanQRCodeStep2Label, UiConstants.CLASS);
	}

	public String getStatusMessage() {
		waitForFinalScanVerificationResult();
		return getText(driver, statusMessage);
	}

	public boolean isSuccessIconDisplayed() {
		waitForScanCameraActive();
		waitForSuccessIconOrFailureState();
		// Use isDisplayedWithoutWaiting instead of isElementIsVisible so there is no
		// additional 30-second timeout window after the dedicated wait above. This
		// prevents missing the icon due to a brief page transition (low-light race)
		// or a redirect during verification (2mp redirect case).
		return isDisplayedWithoutWaiting(successMessageIcon);
	}

	public boolean isErrorIconDisplayed() {
		waitForFinalScanVerificationResult();
		return isElementIsVisible(driver, failedIcon);
	}

	public boolean isAlertMessageDisplayed() {
		waitForFinalScanVerificationResult();
		return isElementIsVisible(driver, alertMessage);
	}

	public boolean isStatusMessageDisplayed() {
		waitForFinalScanVerificationResult();
		return isElementIsVisible(driver, statusMessage);
	}

	public String getScanQRCodeStep3LabelClass() {
		waitForFinalScanVerificationResult();
		return getAttributeValue(driver, ScanQRCodeStep3Label, UiConstants.CLASS);
	}

	public String getScanQRCodeStep4LabelClass() {
		waitForFinalScanVerificationResult();
		return getAttributeValue(driver, ScanQRCodeStep4Label, UiConstants.CLASS);
	}

	public String getAlertMessage() {
		waitForFinalScanVerificationResult();
		return getText(driver, alertMessage);
	}

	/**
	 * Waits until the camera stream is active (scan line or video visible) OR the
	 * result has already appeared (fast scan). This prevents the 180-second
	 * verification countdown from starting while the scan box is still in its
	 * camera-loading state, which happens when large Y4M files take time to
	 * initialise in Chrome under parallel load.
	 */
	public boolean waitForScanCameraActive() {
		try {
			new WebDriverWait(driver, Duration.ofSeconds((long) getTimeout() * getScanVerificationTimeoutMultiplier()))
					.until(webDriver -> isDisplayedWithoutWaiting(ScanLine)
							|| isDisplayedWithoutWaiting(activeScanVideo)
							|| isDisplayedWithoutWaiting(backButton)
							|| isDisplayedWithoutWaiting(ImageAreaElement)
							|| hasFinalScanVerificationResultVisible());
			return true;
		} catch (TimeoutException e) {
			return false;
		}
	}

	public void waitForScanVerificationResultState() {
		try {
			new WebDriverWait(driver, Duration.ofSeconds((long) getTimeout() * getScanVerificationTimeoutMultiplier()))
					.until(webDriver -> hasAnyScanVerificationResultVisible());
		} catch (TimeoutException e) {
			// Fall through to the existing assertion path so the test still fails with the current page state.
		}
	}

	/**
	 * Waits specifically for the success icon to appear, or for an unambiguous
	 * failure indicator (error icon / alert message). Unlike waitForFinalScanVerificationResult,
	 * this does NOT return early on statusMessage or scanQRCodeButton, which can appear
	 * before the success icon and cause isSuccessIconDisplayed to miss it.
	 * Also survives page redirects during VC verification because isDisplayedWithoutWaiting
	 * re-fetches the PageFactory proxy on every poll.
	 */
	private void waitForSuccessIconOrFailureState() {
		long timeoutSeconds = (long) getTimeout() * getScanVerificationTimeoutMultiplier();
		try {
			new WebDriverWait(driver, Duration.ofSeconds(timeoutSeconds))
					.until(webDriver -> isDisplayedWithoutWaiting(successMessageIcon)
							|| isDisplayedWithoutWaiting(failedIcon)
							|| isDisplayedWithoutWaiting(alertMessage));
		} catch (TimeoutException e) {
			// Fall through — the final isDisplayedWithoutWaiting check will record the state.
		}
	}

	public void waitForFinalScanVerificationResult() {
		long timeoutSeconds = (long) getTimeout() * getScanVerificationTimeoutMultiplier();
		try {
			new WebDriverWait(driver, Duration.ofSeconds(timeoutSeconds))
					.until(webDriver -> hasFinalScanVerificationResultVisible());
		} catch (TimeoutException e) {
			// Fall through to the existing assertion path so the test still fails with the current page state.
		}
	}

	private boolean hasFinalScanVerificationResultVisible() {
		return isDisplayedWithoutWaiting(successMessageIcon)
				|| isDisplayedWithoutWaiting(failedIcon)
				|| isDisplayedWithoutWaiting(alertMessage)
				|| hasNonEmptyResultMessage();
	}

	private boolean hasAnyScanVerificationResultVisible() {
		return isDisplayedWithoutWaiting(successMessageIcon)
				|| isDisplayedWithoutWaiting(failedIcon)
				|| isDisplayedWithoutWaiting(alertMessage)
				|| hasNonEmptyResultMessage()
				|| isScanCompletionStepVisible();
	}

	private void waitForCameraPermissionOutcome() {
		long timeoutSeconds = (long) getTimeout() * getScanVerificationTimeoutMultiplier();
		try {
			new WebDriverWait(driver, Duration.ofSeconds(timeoutSeconds))
					.until(webDriver -> isDisplayedWithoutWaiting(cameraAccessDeniedTitle)
							|| isCameraUiActiveWithoutWait()
							|| isDisplayedWithoutWaiting(ScanQRCodeStep3Label)
							|| hasFinalScanVerificationResultVisible());
		} catch (TimeoutException e) {
			// Fall through to the final assertion path so the step still reports the page state.
		}
	}

	private void waitForStableCameraUsableState() {
		long timeoutSeconds = (long) getTimeout() * getScanVerificationTimeoutMultiplier();
		try {
			new WebDriverWait(driver, Duration.ofSeconds(timeoutSeconds))
					.until(webDriver -> isDisplayedWithoutWaiting(cameraAccessDeniedTitle)
							|| isCameraUiActiveWithoutWait()
							|| hasFinalScanVerificationResultVisible());
		} catch (TimeoutException e) {
			// Fall through to the final assertion path so the step still reports the page state.
		}
	}

	private void executeScanFlowWithSingleRecovery() {
		clickOnScanQrCodeButton();
		if (waitForScanStartOutcome()) {
			return;
		}
		refreshBrowser(driver);
		WaitUtil.waitForClickability(driver, scanQRCodeButton);
		clickOnScanQrCodeButton();
		waitForScanStartOutcome();
	}

	private boolean waitForScanStartOutcome() {
		long timeoutSeconds = (long) getTimeout() * getScanVerificationTimeoutMultiplier();
		try {
			return new WebDriverWait(driver, Duration.ofSeconds(timeoutSeconds))
					.until(webDriver -> isDisplayedWithoutWaiting(cameraAccessDeniedTitle)
							|| isCameraUiActiveWithoutWait()
							|| isDisplayedWithoutWaiting(ScanQRCodeStep3Label)
							|| hasFinalScanVerificationResultVisible());
		} catch (TimeoutException e) {
			return false;
		}
	}

	private boolean isCameraUiActiveWithoutWait() {
		return isDisplayedWithoutWaiting(activeScanVideo)
				|| isDisplayedWithoutWaiting(backButton)
				|| isDisplayedWithoutWaiting(ScanLine)
				|| isDisplayedWithoutWaiting(ImageAreaElement);
	}

	private boolean isScanCompletionStepVisible() {
		return isDisplayedWithoutWaiting(ScanQRCodeStep2LabelAfter)
				|| hasStepMarkedComplete(ScanQRCodeStep3Label)
				|| hasStepMarkedComplete(ScanQRCodeStep4Label);
	}

	private boolean hasStepMarkedComplete(WebElement stepElement) {
		try {
			if (stepElement == null || !stepElement.isDisplayed()) {
				return false;
			}
			String classValue = stepElement.getAttribute("class");
			return classValue != null && classValue.contains("text-black");
		} catch (Exception e) {
			return false;
		}
	}

	private boolean hasNonEmptyResultMessage() {
		try {
			return statusMessage != null
					&& statusMessage.isDisplayed()
					&& statusMessage.getText() != null
					&& !statusMessage.getText().trim().isEmpty();
		} catch (Exception e) {
			return false;
		}
	}

	private boolean isDisplayedWithoutWaiting(WebElement element) {
		try {
			return element != null && element.isDisplayed();
		} catch (Exception e) {
			return false;
		}
	}

	private void waitForCameraAccessDeniedPopupToClose() {
		try {
			new WebDriverWait(driver, Duration.ofSeconds(getTimeout()))
					.until(webDriver -> !isDisplayedWithoutWaiting(cameraAccessDeniedTitle));
		} catch (TimeoutException e) {
			// Fall through so the existing assertion path reports the remaining popup state.
		}
	}
}
