package pages;

import base.BasePage;
import org.openqa.selenium.By;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.WebElement;
import org.openqa.selenium.support.FindBy;
import org.openqa.selenium.support.PageFactory;

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

	@FindBy(xpath = "//button[@id='scan-button']")
	WebElement ScanQRCodeButtonTo;

	@FindBy(xpath = "(//div[contains(@class,'bg-default_theme-gradient') and contains(@class,'rounded-full')]/div[text()='3'])")
	WebElement ScanQRCodeStep2LabelAfter;

	@FindBy(xpath = "//div[@style='width: 316px; padding-top: 100%; overflow: hidden; position: relative; place-content: center; display: grid; place-items: center; border-radius: 12px;']")
	WebElement ImageAreaElement;

	@FindBy(xpath = "//button[@id='verification-back-button']")
	WebElement BackButton;

	@FindBy(id="camer-access-denied-okay-button")
	WebElement OkayButton;

	@FindBy(xpath = "//div[@id='scanning-line']")
	WebElement ScanLine;

	//@FindBy(xpath = "//div[@class = 'fixed top-[80px] lg:top-[44px] right-4 lg:right-2] py-[22px] px-[18px] text-white rounded-[12px] shadow-lg bg-[#D73E3E] ']")
	@FindBy(xpath = "//*[@id='alert-message']")	
	WebElement ScannerTimeoutMessage;

	@FindBy(xpath = "//div[@class='pl-4 cursor-pointer']")
	WebElement CloseIconTimeoutMessage;

	@FindBy(xpath = "//span[@id='upload-qr-code-button']")
	WebElement UploadQRCodeButton;

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
		return isElementIsVisible(driver, ScanQRCodeButtonTo);
	}

	public void clickOnScanQRButtonButton() {
		clickOnElement(driver, ScanQRCodeButtonTo);
	}

	public boolean isVisibleScanQRCodeStep2LabelAfter() {
		return isElementIsVisible(driver, ScanQRCodeStep2LabelAfter);
	}

	public boolean isVisibleImageAreaElement() {
		return isElementIsVisible(driver, ImageAreaElement);
	}

	public boolean isVisibleBackButton() {
		return isElementIsVisible(driver, BackButton);
	}

	public void clickOnBackButton() {
		clickOnElement(driver, BackButton);
	}

	public void clickOnOkayButton() {
		clickOnElement(driver, OkayButton);
	}

	public boolean isVisibleScanLine() {
		return isElementIsVisible(driver, ScanLine);
	}

	public String getTextScannerTimeoutMessage() {

		waitForElementVisibleWithPolling(driver, ScannerTimeoutMessage);

		return getText(driver, ScannerTimeoutMessage);
	}

	public boolean isVisibleCloseIconTimeoutMessage() {
		return isElementIsVisible(driver, CloseIconTimeoutMessage);
	}

	public void clickOnCloseIconTimeoutMessage() {
		clickOnElement(driver, CloseIconTimeoutMessage);
	}


}