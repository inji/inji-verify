package pages;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.ArrayList;
import java.util.LinkedHashSet;
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

import org.openqa.selenium.support.ui.ExpectedConditions;
import org.openqa.selenium.support.ui.WebDriverWait;
import java.time.Duration;



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

	@FindBy(id = "no-internet-connection")
	WebElement noInternetConnection;

	@FindBy(id = "no-internet-description")
	WebElement noInternetDescription;

	@FindBy(id = "please-try-again-button")
	WebElement tryAgainButton;


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
        WaitUtil.waitForVisibility(driver, isMosipNationalId, BasePage.getTimeout());
        return true;
    } catch (Exception e) {
        return false;
    }
}

public void clickOnMosipNationalId() {
    WaitUtil.waitForClickability(driver, isMosipNationalId);
    clickOnElement(driver, isMosipNationalId);
}

	public void clickOnStayProtectedCredentialType() {
		WaitUtil.waitForClickability(driver, healthInsurance);
		clickOnElement(driver, healthInsurance);
	}


public void clickOnOnProceed() {
    WaitUtil.waitForClickability(driver, getOnOnProceed);
    clickOnElement(driver, getOnOnProceed);
}

	public void enterVid(String string) {
		enterText(driver, By.xpath("//input[@id='Otp_mosip-vid']"), string);
	}

	public void clickOnGetOtpButton() {
		clickOnElement(driver, getOtp);
	}

public void enterOtp(String otpString) {

    // Wait until first OTP box is visible
    WebDriverWait wait = new WebDriverWait(driver, Duration.ofSeconds(BasePage.getTimeout()));
    wait.until(ExpectedConditions.visibilityOfElementLocated(
            By.xpath("(//input[@class='pincode-input-text'])[1]")));

    for (int i = 0; i < otpString.length(); i++) {
        String locator = "(//input[@class='pincode-input-text'])[" + (i + 1) + "]";
        driver.findElement(By.xpath(locator))
              .sendKeys(String.valueOf(otpString.charAt(i)));
    }
}

	public void clickOnVerify() {
		clickOnElement(driver, verifyOtp);
	}

public String isSuccessMessageDisplayed() {
    try {
        new WebDriverWait(driver, Duration.ofSeconds(getTimeout() * 3L))
                .until(ExpectedConditions.textToBePresentInElement(succsessMessage, "Success!"));
    } catch (Exception e) {
        // ignore if not found within timeout — proceed to return whatever text is currently present
    }

    String successText = getText(driver, succsessMessage);
    if (successText == null) return null;
    // Strip invisible Unicode characters that trim() misses (e.g. non-breaking space U+00A0,
    // zero-width space U+200B, BOM U+FEFF, zero-width joiners U+200C/200D) before comparing.
    return successText.trim().replaceAll("[\\u00A0\\u200B\\uFEFF\\u200C\\u200D\\u00AD]", "");
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
	public void selectDateOfBirth(String dob) {

    WebElement fullNameField = driver.findElement(By.id("_form_fullName"));
    WebElement dobField = driver.findElement(By.id("_form_dob"));
    String formattedDob = resolveAcceptedDateOfBirthFormat(dob, dobField);

    WaitUtil.waitForClickability(driver, fullNameField);
    fullNameField.sendKeys(Keys.TAB);

    WaitUtil.waitForClickability(driver, dobField);
    dobField.clear();
    dobField.sendKeys(formattedDob);
    dobField.sendKeys(Keys.TAB);
}

    private String resolveAcceptedDateOfBirthFormat(String rawDate, WebElement dobField) {
        if (rawDate == null || rawDate.trim().isEmpty()) {
            return rawDate;
        }

        String trimmedDate = rawDate.trim();
        if (!trimmedDate.matches("\\d{4}-\\d{2}-\\d{2}")) {
            return trimmedDate;
        }

        LocalDate parsedDate = LocalDate.parse(trimmedDate);
        for (String datePattern : resolveCandidateDatePatterns(dobField)) {
            String formattedDob = parsedDate.format(DateTimeFormatter.ofPattern(datePattern));
            if (tryDateCandidate(dobField, formattedDob, parsedDate)) {
                return formattedDob;
            }
        }

        return parsedDate.format(DateTimeFormatter.ofPattern("dd-MM-yyyy"));
    }

    private List<String> resolveCandidateDatePatterns(WebElement dobField) {
        LinkedHashSet<String> candidatePatterns = new LinkedHashSet<>();
        String placeholderPattern = sanitizePattern(dobField.getAttribute("placeholder"));
        if (placeholderPattern != null) {
            candidatePatterns.add(placeholderPattern);
            candidatePatterns.addAll(withAlternateSeparators(placeholderPattern));
        }

        String browserPattern = resolvePatternFromBrowserLocale();
        if (browserPattern != null) {
            candidatePatterns.add(browserPattern);
            candidatePatterns.addAll(withAlternateSeparators(browserPattern));
        }

        candidatePatterns.add("dd-MM-yyyy");
        candidatePatterns.add("MM-dd-yyyy");
        candidatePatterns.add("dd/MM/yyyy");
        candidatePatterns.add("MM/dd/yyyy");
        candidatePatterns.add("yyyy-MM-dd");

        return new ArrayList<>(candidatePatterns);
    }

    private String resolvePatternFromBrowserLocale() {
        try {
            Object result = ((JavascriptExecutor) driver).executeScript(
                    "const locale = (navigator.languages && navigator.languages.length ? navigator.languages[0] : navigator.language) || 'en-US';"
                            + "return new Intl.DateTimeFormat(locale).format(new Date(1983, 10, 23));");

            if (!(result instanceof String sampleDate) || sampleDate.trim().isEmpty()) {
                return null;
            }

            return derivePatternFromFormattedSample(sampleDate.trim());
        } catch (Exception e) {
            return null;
        }
    }

    private String derivePatternFromFormattedSample(String sampleDate) {
        String separator = sampleDate.replaceAll(".*?(\\D+).*", "$1");
        String[] parts = sampleDate.split("\\D+");
        if (parts.length != 3) {
            return null;
        }

        String[] tokens = new String[3];
        boolean[] used = new boolean[3];

        for (int i = 0; i < parts.length; i++) {
            String part = parts[i];
            if ("1983".equals(part)) {
                tokens[i] = "yyyy";
                used[i] = true;
            } else if ("23".equals(part)) {
                tokens[i] = "dd";
                used[i] = true;
            } else if ("11".equals(part)) {
                tokens[i] = "MM";
                used[i] = true;
            }
        }

        for (int i = 0; i < used.length; i++) {
            if (!used[i]) {
                return null;
            }
        }

        String normalizedSeparator = separator == null || separator.trim().isEmpty() ? "/" : separator.substring(0, 1);
        return String.join(normalizedSeparator, tokens);
    }

    private String sanitizePattern(String candidatePattern) {
        if (candidatePattern == null || candidatePattern.trim().isEmpty()) {
            return null;
        }

        String normalized = candidatePattern.trim()
                .replaceAll("(?i)day", "dd")
                .replaceAll("(?i)month", "MM")
                .replaceAll("(?i)year", "yyyy")
                .replaceAll("d+", "dd")
                .replaceAll("M+", "MM")
                .replaceAll("y+", "yyyy");

        String separator = normalized.replaceAll("[dMy]", "");
        String normalizedSeparator = separator.isEmpty() ? "/" : separator.substring(0, 1);
        String compact = normalized.replaceAll("[^dMy]+", normalizedSeparator);
        String[] parts = compact.split(java.util.regex.Pattern.quote(normalizedSeparator));
        if (parts.length != 3) {
            return null;
        }

        if (isValidDateToken(parts[0]) && isValidDateToken(parts[1]) && isValidDateToken(parts[2])) {
            return String.join(normalizedSeparator, parts);
        }

        return null;
    }

    private boolean isValidDateToken(String token) {
        return "dd".equals(token) || "MM".equals(token) || "yyyy".equals(token);
    }

    private List<String> withAlternateSeparators(String pattern) {
        List<String> variants = new ArrayList<>();
        if (pattern == null || pattern.trim().isEmpty()) {
            return variants;
        }

        String tokenizedPattern = pattern.replace('/', '|').replace('-', '|').replace('.', '|');
        variants.add(tokenizedPattern.replace('|', '-'));
        variants.add(tokenizedPattern.replace('|', '/'));
        variants.add(tokenizedPattern.replace('|', '.'));
        return variants;
    }

    private boolean tryDateCandidate(WebElement dobField, String candidateValue, LocalDate expectedDate) {
        try {
            WaitUtil.waitForClickability(driver, dobField);
            dobField.clear();
            dobField.sendKeys(candidateValue);
            dobField.sendKeys(Keys.TAB);

            String actualValue = dobField.getAttribute("value");
            if (actualValue == null || actualValue.trim().isEmpty()) {
                return false;
            }

            String normalizedActual = normalizeDateValue(actualValue);
            String normalizedCandidate = normalizeDateValue(candidateValue);
            if (normalizedActual.equals(normalizedCandidate)) {
                return true;
            }

            return doesFieldValueMatchExpectedDate(actualValue, expectedDate);
        } catch (Exception e) {
            return false;
        }
    }

    private boolean doesFieldValueMatchExpectedDate(String actualValue, LocalDate expectedDate) {
        String[] candidatePatterns = {"dd-MM-yyyy", "MM-dd-yyyy", "dd/MM/yyyy", "MM/dd/yyyy", "yyyy-MM-dd", "dd.MM.yyyy", "MM.dd.yyyy"};
        for (String pattern : candidatePatterns) {
            try {
                LocalDate parsedActual = LocalDate.parse(actualValue.trim(), DateTimeFormatter.ofPattern(pattern));
                if (expectedDate.equals(parsedActual)) {
                    return true;
                }
            } catch (DateTimeParseException ignored) {
            }
        }
        return false;
    }

    private String normalizeDateValue(String dateValue) {
        return dateValue == null ? "" : dateValue.replaceAll("\\D", "");
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

	public String getNoInternetTitle() {
		return getText(driver, noInternetConnection);
	}

	public String getNoInternetDescription() {
		return getText(driver, noInternetDescription);
	}

	public boolean isTryAgainButtonVisible() {
		return isElementIsVisible(driver, tryAgainButton);
	}

	public void clickOnTryAgainButton() {
		clickOnElement(driver, tryAgainButton);
	}
}
