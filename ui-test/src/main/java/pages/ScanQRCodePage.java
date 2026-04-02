package pages;

import base.BasePage;
import constants.UiConstants;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.WebElement;
import org.openqa.selenium.support.FindBy;
import org.openqa.selenium.support.PageFactory;
import org.openqa.selenium.support.ui.ExpectedConditions;
import org.openqa.selenium.support.ui.WebDriverWait;

import java.time.Duration;

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

	@FindBy(xpath = "(//div[contains(@class,'bg-default_theme-gradient') and contains(@class,'rounded-full')]/div[text()='3'])")
	WebElement ScanQRCodeStep2LabelAfter;

	@FindBy(xpath = "//div[@style='width: 316px; padding-top: 100%; overflow: hidden; position: relative; place-content: center; display: grid; place-items: center; border-radius: 12px;']")
	WebElement ImageAreaElement;

	@FindBy(xpath = "//button[@id='verification-back-button']")
	WebElement backButton;

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
		return getText(driver, ScanQRCodeStep3Label);

	}

	public String getScanQRCodeStep3Description() {
		return getText(driver, ScanQRCodeStep3Description);

	}

	public String getScanQRCodeStep4Label() {
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

	public void clickOnBackButton() {
		clickOnElement(driver, backButton);
	}

	public void clickOnOkayButton() {
		clickOnElement(driver, okayButton);
	}

	public boolean isCameraAccessDeniedTitleVisible() {
		return isElementIsVisible(driver, cameraAccessDeniedTitle);
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
		return isElementIsVisible(driver, scanQRCodeButton);
	}


	public void clickOnScanQrCodeButton() {
		clickOnElement(driver, scanQRCodeButton);
	}

	public String getScanQRCodeStep2LabelClass() {
		return getAttributeValue(driver, ScanQRCodeStep2Label, UiConstants.CLASS);
	}

	public String getStatusMessage() {
		return getText(driver, statusMessage);
	}

	public boolean isSuccessIconDisplayed() {
		return isElementIsVisible(driver, successMessageIcon);
	}

	public boolean isErrorIconDisplayed() {
		return isElementIsVisible(driver, failedIcon);
	}

	public boolean isAlertMessageDisplayed() {
		return isElementIsVisible(driver, alertMessage);
	}

	public boolean isStatusMessageDisplayed() {
		return isElementIsVisible(driver, statusMessage);
	}

	public String getScanQRCodeStep3LabelClass() {
		return getAttributeValue(driver, ScanQRCodeStep3Label, UiConstants.CLASS);
	}

	public String getScanQRCodeStep4LabelClass() {
		return getAttributeValue(driver, ScanQRCodeStep4Label, UiConstants.CLASS);
	}

	public String getAlertMessage() {
		return getText(driver, alertMessage);
	}
}
