package pages;

import base.BasePage;

import org.openqa.selenium.By;
import org.openqa.selenium.StaleElementReferenceException;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.WebElement;
import org.openqa.selenium.support.FindBy;
import java.time.Duration;
import org.openqa.selenium.support.PageFactory;

public class UploadQRCode extends BasePage {

	public UploadQRCode(WebDriver driver) {
		super(driver);
		PageFactory.initElements(driver, this);
	}

	@FindBy(xpath = "//div[@class='col-start-1 col-end-13 block mb-2.5']")
	WebElement ErrorIcon;

	@FindBy(xpath = "//p[@id='vc-result-display-message']")
	WebElement ErrorTextInvalidQRCode;

	@FindBy(id = "vc-result-display-message")
	WebElement ErrorTextExpiredQRCode;

	@FindBy(id = "alert-message")
	WebElement ErrorTextLargeSizeQRCode;

	@FindBy(xpath = "//div[@class='grid content-center justify-center w-[100%] h-[320px] text-[#000000] opacity-10']")
	WebElement BlankImageQRArea;

	@FindBy(id = "verify-document")
	WebElement UploadQRCodeStep2LabelAfter;

	@FindBy(id = "view-result")
	WebElement UploadQRCodeStep3LabelAfter;

	@FindBy(xpath = "//*[contains(text(), 'QR code uploaded successfully!')]")
	WebElement QRCodeUploadedSuccessToastMessage;

	@FindBy(id = "success_message_icon")
	WebElement TickIconVisible;

	@FindBy(xpath = "//p[@id='vc-result-display-message']")
	WebElement Congratulationtext;

	@FindBy(xpath = "//span[text()='Verify Another QR code']")
	WebElement VerifyAnotherQRcodeButton;

	@FindBy(xpath = "//button[@data-testid='Language-Selector-Button']")
	WebElement languageDropdownButton;

	@FindBy(xpath = "//button[@type='button' and text()='عربي']")
	WebElement arabicLanguageButton;

	@FindBy(xpath = "//button[@type='button' and contains(normalize-space(.), 'Français')]")
	WebElement frenchLanguageButton;

	@FindBy(xpath = "//li[@data-testid='Language-Selector-DropDown-Item-ar']//button[contains(text(),'عربي')]")
	WebElement arabicLanguageSelected;

	@FindBy(xpath = "//button[@type='button' and contains(normalize-space(.), 'Français')]")
	WebElement frenchLanguageSelected;

	@FindBy(xpath = "//a[@id='home-button']")
	WebElement HomeButton;

	@FindBy(xpath = "//*[@id='alert-message']")
	WebElement ErromessageForUnSupportedFromat;

	@FindBy(xpath = "//div[@class='fixed top-[80px] lg:top-[44px] right-4 lg:right-2] py-[22px] px-[18px] text-white rounded-[12px] shadow-lg bg-[#D73E3E] ']")
	WebElement ErrorMessageLargerFileSize;

	@FindBy(xpath = "//*[@id='vc-result-display-message']")
	WebElement ErrorMessageForExpiredQRCode;

	@FindBy(xpath = "//span[@id='please-try-again-button']")
	WebElement PleaseTryAgain;

	@FindBy(xpath = "//input[@type='file']")
	WebElement uploadpath;

	@FindBy(xpath = "//a[@id='verify-credentials-button']")
	WebElement Credentialsbutton;

	@FindBy(id = "upload-qr-code-button")
	WebElement UploadQRCodeButton;

	@FindBy(id = "policyissuedon-value")
	WebElement PolicyIssuedOnValue;

	@FindBy(id = "policyexpireson-value")
	WebElement PolicyExpiresOnValue;

	@FindBy(id = "fullname-value")
	WebElement fullNameValue;

	public void clickOnUploadQRCodePng() {
		uploadFile(driver, UploadQRCodeButton, "InsuranceCredential0.png");
	}

	public void clickOnAnotherUploadQRCodePng() {
		uploadFile(driver, UploadQRCodeButton, "InsuranceCredential0.png");
	}

	public void clickOnUploadQRCodeJpg() {
		uploadFile(driver, UploadQRCodeButton, "InsuranceCredential0.jpg");
	}

	public void clickOnAnotherUploadQRCodeJpg() {
		uploadFile(driver, UploadQRCodeButton, "InsuranceCredential0.jpg");
	}

	public void uploadMultiLanguageVc() {
		uploadFileForStaticQr(driver, UploadQRCodeButton, "multilanguage.PNG");
	}

	public void clickOnUploadQRCodePdf() {
		uploadFile(driver, UploadQRCodeButton, "InsuranceCredential.pdf");
	}

	public void clickOnAnotherUploadQRCodePdf() {
		uploadFile(driver, UploadQRCodeButton, "InsuranceCredential.pdf");
	}

	public void clickOnUploadQRCodeJpeg() {
		uploadFile(driver, UploadQRCodeButton, "InsuranceCredential0.jpeg");
	}

	public void clickOnAnotherUploadQRCodeJpeg() {
		uploadFile(driver, UploadQRCodeButton, "InsuranceCredential0.jpeg");
	}

	public void clickOnUploadQRCodeHtml() {
		uploadFileForStaticQr(driver, UploadQRCodeButton, "QRCode_UnsupportedHtml.html");
	}

	public void clickOnUploadQRCodeInvalid() {
		uploadFileForStaticQr(driver, UploadQRCodeButton, "Invalid.png");
	}

	public void clickOnUploadQRCodeDownloadedFromPhone() {
		uploadFileForStaticQr(driver, UploadQRCodeButton, "VcDownloadedFromMobileApp.jpg");
	}

	public void clickOnUploadExpiredQRCodepngExpired() {
		driver.manage().timeouts().implicitlyWait(Duration.ofSeconds(30));
		uploadFileForStaticQr(driver, UploadQRCodeButton, "Expired_QRCode.png");
	}

	public void clickOnUploadLargeSizeQRCode() {
		uploadFileForStaticQr(driver, UploadQRCodeButton, "largesize.PNG");

	}

	public void clickOnUploadBlurQRCode() {
		uploadFileForStaticQr(driver, UploadQRCodeButton, "blur.PNG");

	}

	public void clickOnUploadMultipleQRCode() {
		uploadFileForStaticQr(driver, UploadQRCodeButton, "multiple_image.jpg");

	}

	public void clickOnUploadSDJwtQRCode() {
		uploadFileForStaticQr(driver, UploadQRCodeButton, "SD_Jwt_QRCode.PNG");

	}

	public void clickOnUploadSVGQRCode() {
		uploadFileForStaticQr(driver, UploadQRCodeButton, "SVG.png");

	}

	public void clickOnUploadInvalidPdf() {
		uploadFileForStaticQr(driver, UploadQRCodeButton, "invalid.pdf");

	}

	public void clickOnUploadExpiredQRCodeJpgExpired() {
		uploadFileForStaticQr(driver, UploadQRCodeButton, "Expired_QRCode.jpg");

	}

	public void clickOnUploadExpiredQRCodeJpegExpired() {
		uploadFileForStaticQr(driver, UploadQRCodeButton, "Expired_QRCode.jpeg");

	}

	public void clickOnUploadExpiredQRCodePdfExpired() {
		uploadFileForStaticQr(driver, UploadQRCodeButton, "Expired_QRCode.pdf");

	}

	public void clickOnUploadQRCodeLargeFileSize() {
		uploadFileForStaticQr(driver, UploadQRCodeButton, "LargeFileSize.png");

	}

	public boolean isVisibleErrorIcon() {
		return isElementIsVisible(driver, ErrorIcon);

	}

	public String getErrorTextInvalidQRCode() {

		return getText(driver, ErrorTextInvalidQRCode);
	}

	public String getErrorTextExpiredQRCode() {

		return getText(driver, ErrorTextExpiredQRCode);
	}

	public boolean isVisibleBlankImageQRArea() {
		return isElementIsVisible(driver, BlankImageQRArea);

	}

	public boolean isVisibleUploadQRCodeStep2LabelAfter() {
		return isElementIsVisible(driver, UploadQRCodeStep2LabelAfter);
	}
	

	public boolean isVisibleUploadQRCodeStep3LabelAfter() {
		return isElementIsVisible(driver, UploadQRCodeStep3LabelAfter);

	}

	public String getQRCodeUploadedSuccessToastMessage() {

		return getText(driver, QRCodeUploadedSuccessToastMessage);
	}

	public boolean isTickIconVisible() {
		return isElementIsVisible(driver, TickIconVisible);

	}

	public String getCongratulationtext() {
		return getText(driver, Congratulationtext);
	}

	public boolean isVisibleVerifyAnotherQRCodeButton() {
		return isElementIsVisible(driver, VerifyAnotherQRcodeButton);

	}

	public boolean isVisiblePolicyIssuedOnValue() {
		return isElementIsVisible(driver, PolicyIssuedOnValue);

	}

	public boolean isVisiblePolicyExpiresOnValue() {
		return isElementIsVisible(driver, PolicyExpiresOnValue);

	}

	public boolean isVisibleFullNameValue() {
		return isElementIsVisible(driver, fullNameValue);

	}
	

	public void clickOnAnotherQRCodeButton() {
		clickOnElement(driver, VerifyAnotherQRcodeButton);

	}

	public void clickOnLanguageDropdown() {
		clickOnElement(driver, languageDropdownButton);

	}

	public void clickOnHomeButton() {
		clickOnElement(driver, HomeButton);
	}

	public void clickVerifyCredentialsButton() {
		clickOnElement(driver, Credentialsbutton);
	}

	public void refreshBrowserAfterVerification() {
		refreshBrowser(driver);
	}

	public String getErromessageForUnSupportedFromat() {
		return getText(driver, ErromessageForUnSupportedFromat);
	}

	public String getErrorMessageLargerFileSize() {

		return getText(driver, ErrorMessageLargerFileSize);
	}

	public String getErrorMessageForExpiredQRCode() {

		return getText(driver, ErrorTextExpiredQRCode);
	}

	public String getErrorMessageForLargeSizeQRCode() {

		return getText(driver, ErrorTextLargeSizeQRCode);
	}

	public String getErrorMessageForBlurQRCode() {

		return getText(driver, ErrorTextLargeSizeQRCode);
	}

	public void browserBackButtonAfterVerification() {
		browserBackButton(driver);
	}

	public void clickOnPleaseTryAgain() {
		clickOnElement(driver, PleaseTryAgain);
	}

	public boolean isLanguageDropdownVisible() {
		return isElementIsVisible(driver, languageDropdownButton);
	}

	public boolean isArabicLanguageSelected() {
		return isElementIsVisible(driver, arabicLanguageSelected);
	}

	public boolean isFrenchLanguageSelected() {
		return isElementIsVisible(driver, frenchLanguageSelected);
	}

	public void selectArabicLanguage() {
		clickOnElement(driver, arabicLanguageButton);
	}

	public void selectFrenchLanguage() {
		clickOnElement(driver, frenchLanguageButton);
	}

}