package pages;

import base.BasePage;

import java.util.HashMap;
import java.util.List;
import org.openqa.selenium.By;
import org.openqa.selenium.JavascriptExecutor;
import org.openqa.selenium.Keys;
import org.openqa.selenium.StaleElementReferenceException;
import org.openqa.selenium.TimeoutException;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.WebElement;
import org.openqa.selenium.chrome.ChromeDriver;
import org.openqa.selenium.interactions.Actions;
import org.openqa.selenium.support.FindBy;
import org.openqa.selenium.support.ui.WebDriverWait;
import java.time.Duration;
import org.openqa.selenium.support.PageFactory;
import utils.BaseTest;
import utils.WaitUtil;

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


	@FindBy(xpath = "//a[@id='home-button']")
	WebElement HomeButton;

	@FindBy(xpath = "//*[@id='alert-message']")
	WebElement ErromessageForUnSupportedFromat;

	@FindBy(xpath = "//div[@class='fixed top-[80px] lg:top-[44px] right-4 lg:right-2] py-[22px] px-[18px] text-white rounded-[12px] shadow-lg bg-[#D73E3E] ']")
	WebElement ErrorMessageLargerFileSize;

	@FindBy(xpath = "//*[@id='vc-result-display-message']")
	WebElement ErrorMessageForExpiredQRCode;

	@FindBy(id = "please-try-again-button")
	WebElement PleaseTryAgain;

	@FindBy(xpath = "//input[@type='file']")
	WebElement uploadpath;

	@FindBy(xpath = "//a[@id='verify-credentials-button']")
	WebElement Credentialsbutton;

	@FindBy(id = "upload-qr-code-button")
	WebElement UploadQRCodeButton;

	@FindBy(xpath = "//span[@id='upload-qr-code-button']/parent::div")
	WebElement UploadQRCodeButtonContainer;

	@FindBy(xpath = "//div[@role='button' and @tabindex='0' and contains(@class,'cursor-pointer')]")
	WebElement UploadQRCodeDropZone;

	@FindBy(xpath = "//*[contains(@fill,'url(#paint0_linear_14_2658)')]")
	WebElement GradientUploadSvgIcon;

	@FindBy(xpath = "//span[@id='upload-qr-code-button']/preceding-sibling::span//*[local-name()='svg']")
	WebElement UploadSvgIcon;

	@FindBy(id = "policyissuedon-value")
	WebElement PolicyIssuedOnValue;

	@FindBy(id = "policyexpireson-value")
	WebElement PolicyExpiresOnValue;

	@FindBy(id = "fullname-value")
	WebElement fullNameValue;

	@FindBy(id = "policynumber-value")
	WebElement PolicyNumberValue;

	@FindBy(id = "policyname-value")
	WebElement PolicyNameValue;

	@FindBy(id = "benefits-value")
	WebElement BenefitsValue;

	@FindBy(id = "mobile-value")
	WebElement MobileValue;

	@FindBy(id = "email-value")
	WebElement EmailValue;

	@FindBy(id = "gender-value")
	WebElement GenderValue;

	@FindBy(id = "dob-value")
	WebElement DobValue;

	public void clickOnUploadQRCodePng() {
		uploadFile(driver, uploadpath, BaseTest.getInsuranceCredentialPngPath());
	}

	public void clickOnAnotherUploadQRCodePng() {
		uploadFile(driver, uploadpath, BaseTest.getInsuranceCredentialPngPath());
	}

	public void uploadPngAndWaitForVerificationResult() {
		uploadWithSingleRecovery(() -> clickOnUploadQRCodePng());
	}

	public void uploadAnotherPngAndWaitForVerificationResult() {
		uploadWithSingleRecovery(() -> clickOnAnotherUploadQRCodePng());
	}

	public void clickOnUploadQRCodeJpg() {
		uploadFile(driver, uploadpath, BaseTest.getInsuranceCredentialJpgPath());
	}

	public void clickOnAnotherUploadQRCodeJpg() {
		uploadFile(driver, uploadpath, BaseTest.getInsuranceCredentialJpgPath());
	}

	public void uploadJpgAndWaitForVerificationResult() {
		uploadWithSingleRecovery(() -> clickOnUploadQRCodeJpg());
	}

	public void uploadAnotherJpgAndWaitForVerificationResult() {
		uploadWithSingleRecovery(() -> clickOnAnotherUploadQRCodeJpg());
	}

	public void uploadMultiLanguageVc() {
		uploadFileForStaticQr(driver, UploadQRCodeButton, "multilanguage.PNG");
	}

	public void clickOnUploadQRCodePdf() {
		uploadFile(driver, uploadpath, BaseTest.getDownloadedInsurancePdfPath());
	}

	public void clickOnAnotherUploadQRCodePdf() {
		uploadFile(driver, uploadpath, BaseTest.getDownloadedInsurancePdfPath());
	}

	public void uploadPdfAndWaitForVerificationResult() {
		uploadWithSingleRecovery(() -> clickOnUploadQRCodePdf());
	}

	public void clickOnUploadOnetimeQRCodePdf() {
		uploadFile(driver, uploadpath, BaseTest.getOnetimeInsuranceCredentialPdfPath());
	}

	public void uploadOnetimeQRCodeAndWaitForVerificationResult() {
		clickOnUploadOnetimeQRCodePdf();
        waitForVerificationResultState();;
	}

	public void clickOnUploadFarmerCredentialPdf() {
		uploadFile(driver, uploadpath, BaseTest.getDownloadedFarmerCredentialPdfPath());
	}

	public void uploadFarmerCredentialAndWaitForVerificationResult() {
		uploadWithSingleRecovery(() -> clickOnUploadFarmerCredentialPdf());
	}

	public void uploadAnotherPdfAndWaitForVerificationResult() {
		uploadWithSingleRecovery(() -> clickOnAnotherUploadQRCodePdf());
	}

	public void clickOnUploadQRCodeJpeg() {
		uploadFile(driver, uploadpath, BaseTest.getInsuranceCredentialJpegPath());
	}

	public void clickOnAnotherUploadQRCodeJpeg() {
		uploadFile(driver, uploadpath, BaseTest.getInsuranceCredentialJpegPath());
	}

	public void uploadJpegAndWaitForVerificationResult() {
		uploadWithSingleRecovery(() -> clickOnUploadQRCodeJpeg());
	}

	public void uploadJpegAndWaitForOfflineResult() {
		clickOnUploadQRCodeJpeg();
		new WebDriverWait(driver, Duration.ofSeconds((long) getTimeout() * 4L))
				.until(webDriver -> hasAnyOfflineVerificationResultVisible());
	}
	public void uploadAnotherJpegAndWaitForVerificationResult() {
		uploadWithSingleRecovery(() -> clickOnAnotherUploadQRCodeJpeg());
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
		WaitUtil.waitForClickability(driver, UploadQRCodeButton);
		uploadFileForStaticQr(driver, UploadQRCodeButton, "Expired_QRCode.png");
	}

	public void clickOnUploadLargeSizeQRCode() {
		uploadFileForStaticQr(driver, UploadQRCodeButton, "largesize.PNG");

	}

	public void clickOnUploadSmallSizeQRCode() {
		uploadFileForStaticQr(driver, UploadQRCodeButton, "SmallFileSize.png");

	}

	public void clickOnUploadBoundaryMinSizeQRCode() {
		uploadFileForStaticQr(driver, UploadQRCodeButton, "QRCode_10KB.png");

	}

	public void clickOnUploadBoundaryMaxSizeQRCode() {
		uploadFileForStaticQr(driver, UploadQRCodeButton, "QRCode_5MB.png");

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

	public void clickOnUploadSDJwtWithExpiredCertificate() {
		uploadFileForStaticQr(driver, UploadQRCodeButton, "SDJWTwithExpiredCertificate.PNG");
	}

	public void clickOnUploadSDJwtWithIncorrectDigestValue() {
		uploadFileForStaticQr(driver, UploadQRCodeButton, "SDJWTwithIncorectDigestValue.PNG");
	}

	public void clickOnUploadSDJwtWithIncorrectDisclosureValue() {
		uploadFileForStaticQr(driver, UploadQRCodeButton, "SDJWTwithIncorectDisclosureValue.PNG");
	}

	public void clickOnUploadSDJwtWithIncorrectSignature() {
		uploadFileForStaticQr(driver, UploadQRCodeButton, "SDJWTwithIncorrectSignature.PNG");
	}

	public void clickOnUploadSDJwtWithInvalidDigestLength() {
		uploadFileForStaticQr(driver, UploadQRCodeButton, "SDJWTwithInvalidDigestLength.PNG");
	}

	public void clickOnUploadSDJwtWithMalformedQRCode() {
		uploadFileForStaticQr(driver, UploadQRCodeButton, "SDJWTwithMalformed_QrCode.PNG");
	}

	public void clickOnUploadSDJwtWithNBFSetToFuture() {
		uploadFileForStaticQr(driver, UploadQRCodeButton, "SDJWTwithNBFsetToFuture.PNG");
	}

	public void clickOnUploadSDJwtWithUnsupportedAlgorithm() {
		uploadFileForStaticQr(driver, UploadQRCodeButton, "SDJWTwithUnsuportedAlgoritham.PNG");
	}

	public void clickOnUploadSDJwtWithMissingDigest() {
		uploadFileForStaticQr(driver, UploadQRCodeButton, "SDJWTwithMissingDigest.PNG");
	}

	public void clickOnUploadSDJwtWithSelectiveDisclosure() {
		uploadFileForStaticQr(driver, UploadQRCodeButton, "SDJWTwithSelectiveDisclosure.PNG");
	}

	public String getSDJwtFirstNameValue() {
		waitForVerificationSuccessState();
		return getText(driver, driver.findElement(By.id("firstname-value")));
	}

	public boolean isSDJwtLastNameFieldVisible() {
		waitForVerificationSuccessState();
		return !driver.findElements(By.id("lastname-value")).isEmpty()
				&& driver.findElements(By.id("lastname-value")).get(0).isDisplayed();
	}

	public void clickOnUploadSVGQRCode() {
		uploadFileForStaticQr(driver, UploadQRCodeButton, "SVG.png");

	}

	public void clickOnUploadSvgInvalidMediaTypeQRCode() {
		uploadFileForStaticQr(driver, UploadQRCodeButton, "svgWithInvalidMediaType.png");

	}

	public void clickOnUploadSvgMissingDataQRCode() {
		uploadFileForStaticQr(driver, UploadQRCodeButton, "svgWithMissingData.png");

	}

	public void clickOnUploadClaim169QRCode() {
		uploadFileForStaticQr(driver, UploadQRCodeButton, "claim.PNG");

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

	public boolean isUploadFileInputPresent() {
		return !driver.findElements(By.xpath("//input[@type='file']")).isEmpty();
	}

	public boolean isUploadFileInputEnabled() {
		try {
			return uploadpath.isEnabled();
		} catch (Exception e) {
			return false;
		}
	}

	public boolean isVisibleErrorIcon() {
		waitForVerificationErrorState();
		return isElementIsVisible(driver, ErrorIcon);

	}

	public String getErrorTextInvalidQRCode() {
		waitForVerificationErrorState();

		return getText(driver, ErrorTextInvalidQRCode);
	}

	public String getErrorTextExpiredQRCode() {
		waitForVerificationErrorState();

		return getText(driver, ErrorTextExpiredQRCode);
	}

	public boolean isVisibleBlankImageQRArea() {
		return isElementIsVisible(driver, BlankImageQRArea);

	}

	public boolean isVisibleUploadQRCodeStep2LabelAfter() {
		waitForVerificationResultState();
		return isElementIsVisible(driver, UploadQRCodeStep2LabelAfter);
	}
	

	public boolean isVisibleUploadQRCodeStep3LabelAfter() {
		waitForVerificationResultState();
		return isElementIsVisible(driver, UploadQRCodeStep3LabelAfter);

	}

	public String getQRCodeUploadedSuccessToastMessage() {

		return getText(driver, QRCodeUploadedSuccessToastMessage);
	}

	public boolean isTickIconVisible() {
		waitForVerificationSuccessState();
		return isElementIsVisible(driver, TickIconVisible);

	}

	public String getCongratulationtext() {
		waitForVerificationSuccessState();
		return getText(driver, Congratulationtext);
	}

	public boolean isVisibleVerifyAnotherQRCodeButton() {
		waitForVerifyAnotherQrCodeButtonState();
		return isElementIsVisible(driver, VerifyAnotherQRcodeButton);

	}

	public boolean isVisiblePolicyIssuedOnValue() {
		waitForVerificationSuccessState();
		return isElementIsVisible(driver, PolicyIssuedOnValue);

	}

	public boolean isVisiblePolicyExpiresOnValue() {
		waitForVerificationSuccessState();
		return isElementIsVisible(driver, PolicyExpiresOnValue);

	}

	public String getPolicyIssuedOnValue() {
		waitForVerificationSuccessState();
		return getText(driver, PolicyIssuedOnValue);
	}

	public String getPolicyExpiresOnValue() {
		waitForVerificationSuccessState();
		return getText(driver, PolicyExpiresOnValue);
	}

	public boolean isVisibleFullNameValue() {
		waitForVerificationSuccessState();
		return isElementIsVisible(driver, fullNameValue);

	}

	public String getFullNameValue() {
		waitForVerificationSuccessState();
		return getText(driver, fullNameValue);
	}

	public String getPolicyNumberValue() {
		waitForVerificationSuccessState();
		return getText(driver, PolicyNumberValue);
	}

	public String getPolicyNameValue() {
		waitForVerificationSuccessState();
		return getText(driver, PolicyNameValue);
	}

	public String getBenefitsValue() {
		waitForVerificationSuccessState();
		return getText(driver, BenefitsValue);
	}

	public String getMobileValue() {
		waitForVerificationSuccessState();
		return getText(driver, MobileValue);
	}

	public String getEmailValue() {
		waitForVerificationSuccessState();
		return getText(driver, EmailValue);
	}

	public String getGenderValue() {
		waitForVerificationSuccessState();
		return getText(driver, GenderValue);
	}

	public String getDobValue() {
		waitForVerificationSuccessState();
		return getText(driver, DobValue);
	}


	public void clickOnAnotherQRCodeButton() {
		waitForVerifyAnotherQrCodeButtonState();
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
		waitForAlertMessageState();
		return getText(driver, ErromessageForUnSupportedFromat);
	}

	public String getErrorMessageLargerFileSize() {

		return getText(driver, ErrorMessageLargerFileSize);
	}

	public String getErrorMessageForExpiredQRCode() {
		waitForVerificationMessageState();

		return getText(driver, ErrorTextExpiredQRCode);
	}

	public String getErrorMessageForVerificationLimitReached() {
		waitForAlertMessageState();

		return getText(driver, ErrorTextLargeSizeQRCode);
	}

	public String getErrorMessageForLargeSizeQRCode() {
		waitForAlertMessageState();

		return getText(driver, ErrorTextLargeSizeQRCode);
	}

	public String getErrorMessageForBlurQRCode() {
		waitForAlertMessageState();

		return getText(driver, ErrorTextLargeSizeQRCode);
	}

	public void browserBackButtonAfterVerification() {
		browserBackButton(driver);
	}

	public void clickOnPleaseTryAgain() {
		clickOnElement(driver, PleaseTryAgain);
	}

	public void hoverOverUploadButton() {
		hoverOverElement(driver, UploadQRCodeButtonContainer);
	}

	public void hoverOffUploadButton() {
		hoverOverElement(driver, driver.findElement(org.openqa.selenium.By.tagName("body")));
	}

	public boolean isGradientUploadIconVisible() {
		try {
			return GradientUploadSvgIcon.isDisplayed();
		} catch (Exception e) {
			return false;
		}
	}

	public boolean isUploadSvgIconVisible() {
		try {
			return UploadSvgIcon.isDisplayed();
		} catch (Exception e) {
			return false;
		}
	}

	// Host serving the Farmer credential's SVG render template. The Farmer VC
	// (whether the static SVG.png asset or the dynamically issued PDF) resolves its
	// renderMethod.template.id to the "injicertify-farmer" host; blocking it forces the
	// template fetch to fail without affecting the verify-service /v2/vc-verification call.
	private static final String SVG_TEMPLATE_URL_PATTERN = "*injicertify-farmer*";

	public void blockSvgTemplateNetworkRequests() {
		ChromeDriver chromeDriver = getLocalChromeDriverOrThrow("block SVG credential template network requests");
		chromeDriver.executeCdpCommand("Network.enable", new HashMap<>());
		HashMap<String, Object> params = new HashMap<>();
		params.put("urls", List.of(SVG_TEMPLATE_URL_PATTERN));
		chromeDriver.executeCdpCommand("Network.setBlockedURLs", params);
	}

	public void throttleSvgTemplateNetworkRequests() {
		ChromeDriver chromeDriver = getLocalChromeDriverOrThrow("throttle SVG credential template network requests");
		chromeDriver.executeCdpCommand("Network.enable", new HashMap<>());
		HashMap<String, Object> conditions = new HashMap<>();
		conditions.put("offline", false);
		conditions.put("latency", 1000);
		// Moderate "slow network" throughput (~100 KB/s). Aggressive enough to represent a
		// throttled connection, but high enough that the large (~2.3 MB) SVG template still
		// finishes loading within the render wait so it actually renders.
		conditions.put("downloadThroughput", 100_000);
		conditions.put("uploadThroughput", 100_000);
		chromeDriver.executeCdpCommand("Network.emulateNetworkConditions", conditions);
	}

	private ChromeDriver getLocalChromeDriverOrThrow(String action) {
		if (!(driver instanceof ChromeDriver chromeDriver)) {
			throw new IllegalStateException(
					"Unable to " + action + ": supported only with local ChromeDriver.");
		}
		return chromeDriver;
	}

	public String getErrorMessageForSvgTemplateFailure() {
		waitForAlertMessageState();
		return getText(driver, ErrorTextLargeSizeQRCode);
	}

	public boolean isVcDetailsGridVisible() {
		By valueRowLocator = By.xpath("//*[substring(@id, string-length(@id) - 5) = '-value']");
		try {
			new WebDriverWait(driver, Duration.ofSeconds((long) getTimeout() * 2L))
					.until(d -> d.findElements(valueRowLocator).stream().anyMatch(WebElement::isDisplayed));
			return true;
		} catch (Exception e) {
			return false;
		}
	}

	// The rendered SVG credential template is injected inside VcSvgTemplate's wrapper,
	// whose exact class string ("w-full flex justify-center items-center") is unique to
	// that component. Matching an <svg> inside it is an unambiguous "template rendered"
	// signal, avoiding false positives from the ResultSummary status icon (flex-col, no
	// w-full) or the QrScanner container (adds h-full).
	private static final By SVG_TEMPLATE_LOCATOR = By.xpath(
			"//div[@class='w-full flex justify-center items-center']//*[local-name()='svg']");

	public boolean isSvgTemplateRendered() {
		try {
			new WebDriverWait(driver, Duration.ofSeconds((long) getTimeout() * 4L))
					.until(d -> d.findElements(SVG_TEMPLATE_LOCATOR).stream().anyMatch(WebElement::isDisplayed));
			return true;
		} catch (Exception e) {
			return false;
		}
	}

	public boolean isSvgTemplateRenderedNow() {
		try {
			return driver.findElements(SVG_TEMPLATE_LOCATOR).stream().anyMatch(WebElement::isDisplayed);
		} catch (Exception e) {
			return false;
		}
	}

	public boolean isLanguageDropdownVisible() {
		return isElementIsVisible(driver, languageDropdownButton);
	}


	private void waitForVerificationSuccessState() {
		try {
			WaitUtil.waitForVisibility(driver, TickIconVisible, getTimeout() * 4);
		} catch (TimeoutException e) {
			// Fall through to the existing assertion path so the test still fails with the page state.
		}
	}

	private void waitForVerificationErrorState() {
		try {
			WaitUtil.waitForVisibility(driver, ErrorTextExpiredQRCode, getTimeout() * 4);
		} catch (TimeoutException e) {
			// Fall through to the existing assertion path so the test still fails with the page state.
		}
	}

	private void waitForVerificationMessageState() {
		try {
			WaitUtil.waitForVisibility(driver, ErrorTextExpiredQRCode, getTimeout() * 4);
		} catch (TimeoutException e) {
			// Fall through to the existing assertion path so the test still fails with the page state.
		}
	}

	private void waitForAlertMessageState() {
		try {
			WaitUtil.waitForVisibility(driver, ErrorTextLargeSizeQRCode, getTimeout() * 4);
		} catch (TimeoutException e) {
			// Fall through to the existing assertion path so the test still fails with the page state.
		}
	}

	private void waitForVerifyAnotherQrCodeButtonState() {
		try {
			WaitUtil.waitForVisibility(driver, VerifyAnotherQRcodeButton, getTimeout() * 4);
		} catch (TimeoutException e) {
			// Fall through to the existing assertion path so the test still fails with the page state.
		}
	}

	public void waitForVerificationResultState() {
		new WebDriverWait(driver, Duration.ofSeconds((long) getTimeout() * 4L))
				.until(webDriver -> hasAnyVerificationResultVisible());
	}

	private void uploadWithSingleRecovery(Runnable uploadAction) {
		try {
			uploadAction.run();
			waitForVerificationResultState();
		} catch (TimeoutException firstTimeout) {
			refreshBrowser(driver);
			WaitUtil.waitForClickability(driver, UploadQRCodeButton);
			uploadAction.run();
			waitForVerificationResultState();
		}
	}

	private boolean hasAnyVerificationResultVisible() {
		return isDisplayedWithoutWaiting(TickIconVisible)
				|| isDisplayedWithoutWaiting(VerifyAnotherQRcodeButton)
				|| isDisplayedWithoutWaiting(ErrorTextExpiredQRCode)
				|| isDisplayedWithoutWaiting(ErrorTextLargeSizeQRCode)
				|| isDisplayedWithoutWaiting(UploadQRCodeStep2LabelAfter)
				|| isDisplayedWithoutWaiting(UploadQRCodeStep3LabelAfter);
	}

	private boolean hasAnyOfflineVerificationResultVisible() {
		return isDisplayedWithoutWaiting(TickIconVisible)
				|| isDisplayedWithoutWaiting(VerifyAnotherQRcodeButton)
				|| isDisplayedWithoutWaiting(ErrorTextExpiredQRCode)
				|| isDisplayedWithoutWaiting(ErrorTextLargeSizeQRCode)
				|| isDisplayedWithoutWaiting(UploadQRCodeStep2LabelAfter)
				|| isDisplayedWithoutWaiting(UploadQRCodeStep3LabelAfter)
				|| isDisplayedWithoutWaiting(PleaseTryAgain);
	}

	private boolean isDisplayedWithoutWaiting(WebElement element) {
		try {
			return element != null && element.isDisplayed();
		} catch (Exception e) {
			return false;
		}
	}

	public void selectLanguage(String displayName) {
		WebElement button = driver.findElement(
			By.xpath("//button[@type='button' and contains(normalize-space(.), '" + displayName + "')]"));
		clickOnElement(driver, button);
	}

	public boolean isLanguageSelected(String displayName) {
		List<WebElement> found = driver.findElements(
			By.xpath("//button[@data-testid='Language-Selector-Button']//p[contains(normalize-space(.), '" + displayName + "')]"));
		return !found.isEmpty() && found.get(0).isDisplayed();
	}

	public boolean isLanguageHighlightedWithGradient(String displayName) {
		List<WebElement> found = driver.findElements(By.xpath(
			"//button[@role='option' and .//span[contains(normalize-space(.), '" + displayName + "') and contains(@class, 'bg-default_theme-gradient')]]"));
		return !found.isEmpty() && found.get(0).isDisplayed();
	}

	public boolean isLanguageTickVisible(String displayName) {
		// //svg fails across SVG namespace; local-name()='svg' is namespace-agnostic
		List<WebElement> found = driver.findElements(By.xpath(
			"//button[@role='option' and .//span[contains(normalize-space(.), '" + displayName + "')] and .//*[local-name()='svg']]"));
		return !found.isEmpty() && found.get(0).isDisplayed();
	}

	public boolean isUploadButtonFocused() {
		JavascriptExecutor js = (JavascriptExecutor) driver;
		Boolean isFocused = (Boolean) js.executeScript(
				"var el = document.activeElement;" +
				"return el != null && el.getAttribute('role') === 'button' && el.getAttribute('tabindex') === '0' && el.classList.contains('cursor-pointer');");
		return Boolean.TRUE.equals(isFocused);
	}

	public void tabToUploadButton() {
		WaitUtil.waitForClickability(driver, UploadQRCodeDropZone);
		Actions actions = new Actions(driver);
		int maxTabs = 25;
		for (int i = 0; i < maxTabs; i++) {
			if (isUploadButtonFocused()) {
				break;
			}
			actions.sendKeys(Keys.TAB).perform();
		}
	}

}
