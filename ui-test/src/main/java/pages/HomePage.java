package pages;

import java.util.List;

import org.openqa.selenium.WebDriver;
import org.openqa.selenium.WebElement;
import org.openqa.selenium.support.FindBy;
import org.openqa.selenium.support.PageFactory;

import api.InjiVerifyConfigManager;

import org.openqa.selenium.By;
import org.openqa.selenium.JavascriptExecutor;
import org.openqa.selenium.JavascriptException;
import java.util.Set;
import org.openqa.selenium.Keys;
import org.openqa.selenium.NoSuchElementException;



import base.BasePage;
import utils.WaitUtil;

public class HomePage extends BasePage {

    private static final String stayProtectedIssuer = InjiVerifyConfigManager.getproperty("stayProtectedIssuer");
    private static final String stayProtectedIssuerCredentialType = InjiVerifyConfigManager.getproperty("stayProtectedIssuerCredentialType");

	public HomePage(WebDriver driver) {
		super(driver);
		PageFactory.initElements(driver, this);
	}

	@FindBy(xpath = "//nav/div/a/svg")
	WebElement injiVerifyLogo;

	@FindBy(id = "verify-credentials-heading")
	WebElement header;

	@FindBy(xpath = "//p[@id='verify-credentials-description']")
	WebElement SubHeader;

	@FindBy(xpath = "//a[@id='home-button']")
	WebElement homeButton;

	@FindBy(id = "fullname-value")
	WebElement fullNameValue;

	@FindBy(id = "gender-value")
	WebElement fullGenderValue;

	@FindBy(xpath = "//a[@id='verify-credentials-button']")
	WebElement Credentialsbutton;

	@FindBy(xpath = "//button[@id='help-button']")
	WebElement helpButton;
	
	@FindBy(xpath = "//button[.//span[text()='Continue as Guest']]")
	WebElement continueButton;

	@FindBy(xpath = "(//*[@id='help-button']//*[@class='mx-1.5 rotate-180']//*)[1]")
	WebElement Expansionbutton;

	@FindBy(xpath = "(//*[@id='help-button']//*[@class='mx-1.5 ']//*)[2]")
	WebElement ExpansionbuttonAfter;

	@FindBy(xpath = "//div[@id='help-submenu']/a")
	List<WebElement> HelpOptionLinks;

	@FindBy(xpath = "//*[@id='help-button']/*[@stroke='currentColor']")
	WebElement minimizeHelpButton;

	@FindBy(xpath = "//*[@id='upload-qr-code-tab']")
	WebElement UploadQRButton;

	@FindBy(xpath = "//*[@id='scan-qr-code-tab']")
	WebElement ScanQRCodeButton;

	@FindBy(xpath = "//*[@id='copyrights-content']")
	WebElement CopyrightText;

	@FindBy(xpath = "//*[@id='upload-qr-code']")
	WebElement UploadQRCodeStep1Label;

	@FindBy(xpath = "//*[@id='upload-qr-code-description']")
	WebElement UploadQRCodeStep1Description;

	@FindBy(xpath = "//*[@id='verify-document']")
	WebElement UploadQRCodeStep2Label;

	@FindBy(xpath = "//*[@id='verify-document-description']")
	WebElement UploadQRCodeStep2Description;

	@FindBy(xpath = "//*[@id='view-result']")
	WebElement UploadQRCodeStep3Label;

	@FindBy(xpath = "//*[@id='view-result-description']")
	WebElement UploadQRCodeStep3Description;

	@FindBy(id = "scan-qr-code-tab")
	WebElement ScanElement;

	@FindBy(xpath = "//span[@class='mr-1.5']")
	WebElement UploadIcon;

	@FindBy(id = "upload-qr-code-button")
	WebElement UploadButton;

	@FindBy(xpath = "//div[@class='grid text-center content-center justify-center pt-2']")
	WebElement FormatConstraintText;

	@FindBy(xpath = "//*[@id='upload-qr-code-button']")
	WebElement QRUploadButton;

	@FindBy(xpath = "//button[@id='ble-tab']")
	WebElement bleTab;

	@FindBy(xpath = "//input[@type='text']")
	WebElement SearchBox;

	@FindBy(xpath = "//p[@data-testid='IntroBox-SubText']")
	WebElement IntroSubText;

	@FindBy(xpath = "//*[@data-testid='ItemBox-Text']")
	WebElement mosipCrdentials;

	@FindBy(xpath = "(//label[contains(@class, 'w-full h-full') and contains(text(), 'Once')])[1]")
	WebElement getValidityDropdown;

	@FindBy(xpath = "(//label[contains(@data-testid, 'DataShareContent-Validity-Times-DropDown-NoLimit') and contains(text(), 'No Limit')])[1]")
	WebElement getOnNoLimit;

	@FindBy(xpath = "//button[contains(@data-testid, 'DataShareFooter-Success-Button')]")
	WebElement getOnOnProceed;
	
	@FindBy(xpath = "//h3[@data-testid='ItemBox-Text' and text()='Health Insurance']")
	WebElement healthInsurance;

	@FindBy(xpath = "//div[@data-testid='ItemBox-Outer-Container-0']")
	WebElement isMosipNationalId;

	@FindBy(xpath = "//input[@id='Otp_mosip-vid']")
	WebElement vidTextBox;

	@FindBy(xpath = "//button[@id='get_otp']")
	WebElement getOtp;

	@FindBy(xpath = "//button[@id='verify_otp']")
	WebElement verifyOtp;

	@FindBy(xpath = "//p[@data-testid='title-download-result']")
	WebElement succsessMessage;

	@FindBy(xpath = "//label[text() = 'Enter Full Name']")
	WebElement enterFullnameTextBox;

	@FindBy(xpath = "//button[@id='verify_form']")
	WebElement verifyButton;

	@FindBy(xpath = "//button[@id='home-button']")
	WebElement HomeButton;

	@FindBy(xpath = "//*[@data-testid='HomeBanner-Guest-Login']")
	WebElement guestLogin;

	@FindBy(xpath = "//p[text() = 'Something went wrong with your request. Please check and try again.']")
	WebElement errorMeassage;

	@FindBy(xpath = "(//span[contains(@class, 'bg-gradient-to-r') and contains(text(), 'Get Started')])[1]")
	WebElement getStartedButton;


	public Boolean isLogoDisplayed() {
		return injiVerifyLogo.isDisplayed();

	}

	public String isPageTitleDisplayed() {
		return driver.getCurrentUrl();
	}

	public String getPageTitle() {
		return driver.getTitle();
	}

	public String getHeader() {
		return getText(driver, header);

	}

	public String getSubHeader() {
		return getText(driver, SubHeader);
	}

	public Boolean isHomeButtonDisplayed() {
		return isElementIsVisible(driver, homeButton);
	}

	public Boolean isVerifyCredentialsbuttonDisplayed() {
		return isElementIsVisible(driver, Credentialsbutton);
	}

	public Boolean isHelpbuttonDisplayed() {
		return isElementIsVisible(driver, helpButton);
	}

	public Boolean isExpansionbuttonDisplayedBefore() {
		return isElementIsVisible(driver, Expansionbutton);
	}

	public void clickOnHelpButton() {
		clickOnElement(driver, helpButton);
	}
	
	public void clickOnContinueButton() {
		clickOnElement(driver, continueButton);
	}

	public Boolean isExpansionbuttonDisplayedAfter() {
		return isElementIsVisible(driver, ExpansionbuttonAfter);
	}

	public Boolean verifyHelpOptionLinks() {
		return verifyHomePageLinks(driver, HelpOptionLinks);

	}

	public void minimizeHelpButton() {
		clickOnElement(driver, minimizeHelpButton);
	}

	public void clickOnHomeButton() {
		clickOnElement(driver, homeButton);
	}

	public Boolean isUploadQRButtonVisible() {
		return isElementIsVisible(driver, UploadQRButton);
	}

	public Boolean isScanQRCodeButtonVisible() {
		return isElementIsVisible(driver, UploadQRButton);
	}

	public Boolean isVPverificationButtonVisible() {
		return isElementIsVisible(driver, ScanQRCodeButton);
	}

	public Boolean isBLEButtonVisible() {
		return isElementIsVisible(driver, bleTab);
	}

	public String getVerifyCopyrightText() {
		return getText(driver, CopyrightText);

	}

	public String getUploadQRCodeStep1Label() {
		return getText(driver, UploadQRCodeStep1Label);

	}

	public String getUploadQRCodeStep1Description() {
		return getText(driver, UploadQRCodeStep1Description);

	}

	public String getUploadQRCodeStep2Label() {
		return getText(driver, UploadQRCodeStep2Label);

	}

	public String getUploadQRCodeStep2Description() {
		return getText(driver, UploadQRCodeStep2Description);

	}

	public String getUploadQRCodeStep3Label() {
		return getText(driver, UploadQRCodeStep3Label);

	}

	public String getUploadQRCodeStep3Description() {
		return getText(driver, UploadQRCodeStep3Description);

	}

	public Boolean isScanElementIsVisible() {
		return isElementIsVisible(driver, ScanElement);
	}

	public Boolean isUploadIconIsVisible() {
		return isElementIsVisible(driver, UploadIcon);
	}

	public Boolean isUploadButtonIsVisible() {
		driver.navigate().refresh();
		return isElementIsVisible(driver, UploadButton);
	}

		public Boolean isUploadButtonIsVisibleAfterIdle() {
		driver.navigate().refresh();
		return isElementIsVisibleAfterIdle(driver, UploadButton);
	}

	public String getFormatConstraintText() {
		System.out.println(getText(driver, FormatConstraintText));
		return getText(driver, FormatConstraintText);

	}

	public void clickOnQRUploadButton() {
		clickOnElement(driver, QRUploadButton);
	}


	public void enterIssuersInSearchBox(String string) {
		enterText(driver, By.xpath("//input[@type='text']"), string);

	}

	public void clickOnDownloadMosipCredentials() {
		clickOnElement(driver,mosipCrdentials);
	}

    public void clickOnStayProtectedCredentials() {
        WebElement stayProtectedIssuerElement = driver.findElement(By.xpath("//h3[text()='" + stayProtectedIssuer + "']"));
        clickOnElement(driver,stayProtectedIssuerElement);
    }

	public void clickOnGetStartedButton() {
		if(isElementIsVisible(driver,getStartedButton)) {
		clickOnElement(driver,getStartedButton);
		}
	}

	public void clickOnValidityDropdown() {
		clickOnElement(driver,getValidityDropdown);
	}

	public void clickOnNoLimit() {
		clickOnElement(driver,getOnNoLimit);
	}


	public Boolean isMosipNationalIdDisplayed() {
		try {
			Thread.sleep(2000);
		} catch (InterruptedException e) {
			e.printStackTrace();
		}
		return isElementIsVisible(driver, isMosipNationalId);
	}

	public void clickOnMosipNationalId() {
		try {
			Thread.sleep(2000);
		} catch (InterruptedException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		clickOnElement(driver, isMosipNationalId);
	}

	public void clickOnStayProtectedCredentialType() {
       		// Use explicit wait (configured via explicitWaitTimeout) instead of Thread.sleep
		WaitUtil.waitForClickability(driver, healthInsurance);
		clickOnElement(driver, healthInsurance);
	}


	public void clickOnOnProceed() {
		try {
			Thread.sleep(2000);
		} catch (InterruptedException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		clickOnElement(driver, getOnOnProceed);
	}

	public void enterVid(String string) {
		enterText(driver, By.xpath("//input[@id='Otp_mosip-vid']"), string);
	}

	public void clickOnGetOtpButton() {
		clickOnElement(driver, getOtp);
	}
	public void enterOtp( String otpString) {
		try {
			Thread.sleep(3000);
		} catch (InterruptedException e) {
			throw new RuntimeException(e);
		}
		for (int i = 0; i < otpString.length(); i++) {
			String locator = "(//input[@class='pincode-input-text'])[" + (i + 1) + "]";
			driver.findElement(By.xpath(locator)).sendKeys(String.valueOf(otpString.charAt(i)));
		}
	}

	public void clickOnVerify() {
		clickOnElement(driver, verifyOtp);
	}

	public String isSuccessMessageDisplayed() {
	    String message = "";
	    int retryCount = 0;

	    while (retryCount < 2) {
	        try {
	            Thread.sleep(9000);
	        } catch (InterruptedException e) {
	            e.printStackTrace();
	        }

	        message = getText(driver, succsessMessage);
	        if ("Success!".equalsIgnoreCase(message.trim())) {
	            break;
	        } else {
	            retryCount++;
	        }
	    }

	    return message;
	}


	public  void openNewTab(){
		String url = InjiVerifyConfigManager.getInjiWebUi();
		((JavascriptExecutor) driver).executeScript("window.open(arguments[0])", url);
		Set<String> allWindowHandles = driver.getWindowHandles();
		System.out.println(allWindowHandles);
		if (allWindowHandles.size() >= 2) {
			String secondWindowHandle = allWindowHandles.toArray(new String[0])[1];
			String firstWindowHandle = allWindowHandles.toArray(new String[0])[0];
			driver.switchTo().window(secondWindowHandle);
		}
	}

	public  void switchToWebTab(){
		Set<String> allWindowHandles = driver.getWindowHandles();
		System.out.println(allWindowHandles);
		if (allWindowHandles.size() >= 2) {
			String secondWindowHandle = allWindowHandles.toArray(new String[0])[1];
			String firstWindowHandle = allWindowHandles.toArray(new String[0])[0];
			// Switch to the second window
			driver.switchTo().window(secondWindowHandle);
		}
	}

	public  void switchToVerifyTab(){
		Set<String> allWindowHandles = driver.getWindowHandles();
		System.out.println(allWindowHandles);
		if (allWindowHandles.size() >= 2) {
			String secondWindowHandle = allWindowHandles.toArray(new String[0])[1];
			String firstWindowHandle = allWindowHandles.toArray(new String[0])[0];
			driver.switchTo().window(firstWindowHandle);
		}
	}
	public void enterPolicyNumer(String string) {
		enterText(driver, By.xpath("//input[@id='_form_policyNumber']"), string);
	}

	public void enterFullName(String string) {
		enterText(driver, By.xpath("//input[@id='_form_fullName']"), string);
	}
    public void selectDateOfBirth(String string) {

        driver.findElement(By.xpath("//input[@id='_form_fullName']")).sendKeys(Keys.TAB);
        driver.findElement(By.id("_form_dob")).sendKeys(string);
        try {
            Thread.sleep(2000);
        } catch (InterruptedException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }
    }

	public void clickOnLogin() {
		clickOnElement(driver,verifyButton );
	}

	public Boolean isErrorMessageVisible() {
		return isElementIsVisible(driver, errorMeassage);
	}

	public void clickOnContinueAsGuest() {
		clickOnElement(driver,guestLogin );
	}

	public String getNameValueInArabic() {
		return getText(driver, fullNameValue);

	}

	public String getNameValueInFrench() {
		return getText(driver, fullNameValue);

	}

	public String getGenderValueInArabic() {
		return getText(driver, fullGenderValue);

	}

	public String getGenderValueInFrench() {
		return getText(driver, fullGenderValue);

	}



	}