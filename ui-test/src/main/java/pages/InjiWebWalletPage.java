package pages;

import java.time.Duration;
import java.util.List;
import java.util.Set;

import org.openqa.selenium.By;
import org.openqa.selenium.Cookie;
import org.openqa.selenium.JavascriptExecutor;
import org.openqa.selenium.NoSuchElementException;
import org.openqa.selenium.TimeoutException;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.WebElement;
import org.openqa.selenium.support.FindBy;
import org.openqa.selenium.support.PageFactory;
import org.openqa.selenium.support.ui.ExpectedConditions;
import org.openqa.selenium.support.ui.WebDriverWait;

import api.InjiVerifyConfigManager;
import base.BasePage;
import utils.HttpUtils;
import utils.WaitUtil;



public class InjiWebWalletPage extends BasePage {
	
	@FindBy(xpath = "//input[@type='checkbox' and @aria-label='Select Health Insurance credential']")
	WebElement selectVCButton;

	@FindBy(xpath = "(//input[@type='checkbox' and @aria-label='Select Health Insurance credential'])[2]")
	WebElement selectSecondVCButton;

	@FindBy(xpath = "(//button[@data-testid='btn-consent-share'])[2]")
	WebElement consentAndShareButton;

	@FindBy(xpath = "//button[@data-testid='google-login-button']")
	WebElement googleButton;
	
	@FindBy(xpath = "//button[contains(.,'Google') or contains(.,'google') or contains(.,'Continue with Google')]")
	WebElement googleLogin;

	@FindBy(id = "title-no-matching-credentials")
	WebElement noMatchingCredentialsError;

	@FindBy(xpath = "//p[normalize-space()='Not Shared']")
    WebElement partialSharingError;

	@FindBy(xpath = "//span[normalize-space()='Home']")
	WebElement homeButton;

	private static final String PRESENT_BUTTON_XPATH =
			"//button[normalize-space()='Present' or normalize-space()='Share' or normalize-space()='Proceed']";

	public InjiWebWalletPage(WebDriver driver) {
		super(driver);
		PageFactory.initElements(driver, this);
	}

public void enterConfirmPasscode(String string) {
		WebDriverWait wait = new WebDriverWait(driver, Duration.ofSeconds(30));
		wait.until(ExpectedConditions.visibilityOfElementLocated(By.xpath(
				"//div[@data-testid='confirm-passcode-container']//input[@type='password' and @maxlength='1']")));
		List<WebElement> confirmFields = driver.findElements(By
				.xpath("//div[@data-testid='confirm-passcode-container']//input[@type='password' and @maxlength='1']"));

		if (confirmFields.size() < string.length()) {
			throw new RuntimeException("Not enough confirm passcode input fields found: expected " + string.length()
					+ " but found " + confirmFields.size());
		}

		for (int i = 0; i < string.length(); i++) {
			WebElement field = confirmFields.get(i);
			field.click();
			field.clear();
			field.sendKeys(String.valueOf(string.charAt(i)));
			try {
				Thread.sleep(100);
			} catch (InterruptedException e) {
				e.printStackTrace();
			}
		}
	}

	public Boolean isgoogleButtonDisplayed() {
		return isElementPresent(driver, googleButton);
	}

	public void navigateToWalletHome() {
		driver.navigate().to(HttpUtils.getInjiWebBaseUrl() + "/");
	}

	public void navigateToIssuersPage() {
		driver.navigate().to(InjiVerifyConfigManager.getInjiWebUi());
	}

	public void navigateToUserCredentialsPage() {
		driver.navigate().to(HttpUtils.getInjiWebBaseUrl() + "/user/credentials");
	}

	public void applySessionCookie(String sessionCookie) {
		String[] cookieParts = sessionCookie.split("=", 2);
		if (cookieParts.length != 2) {
			throw new RuntimeException("Invalid session cookie format: " + sessionCookie);
		}

		String cookieName = cookieParts[0].trim();
		String cookieValue = cookieParts[1].trim();

		driver.manage().deleteAllCookies();
		Cookie cookie = new Cookie.Builder(cookieName, cookieValue).path("/").isHttpOnly(true).isSecure(true).build();
		driver.manage().addCookie(cookie);
		driver.navigate().refresh();
	}

	public void waitForWalletPageReady() {
		new WebDriverWait(driver, Duration.ofSeconds(getTimeout())).until(webDriver -> {
			String currentUrl = webDriver.getCurrentUrl();
			return currentUrl.contains("injiweb") || currentUrl.contains("/user/");
		});
	}

	public void waitForUserHome() {
		new WebDriverWait(driver, Duration.ofSeconds(getTimeout()))
				.until(ExpectedConditions.urlContains("/user/home"));
	}

	public void waitForCredentialListPage() {
		new WebDriverWait(driver, Duration.ofSeconds(getTimeout()))
				.until(ExpectedConditions.or(
						ExpectedConditions.urlContains("/user/credentials"),
						ExpectedConditions.presenceOfElementLocated(By.xpath(buildTextMatchXpath("Health Insurance")))));
	}

	public void waitForVerifyRedirect() {
		new WebDriverWait(driver, Duration.ofSeconds(getTimeout() * 2))
				.until(ExpectedConditions.urlContains("injiverify"));
	}

	public boolean isGoogleLoginVisible() {
		return isElementPresent(driver, googleLogin);
	}

	public void enterPasscode(String passcode) {
		List<WebElement> segmentedInputs = driver.findElements(
				By.xpath("//input[contains(@class,'pincode-input-text') or @inputmode='numeric']"));
		if (!segmentedInputs.isEmpty()) {
			int length = Math.min(passcode.length(), segmentedInputs.size());
			for (int i = 0; i < length; i++) {
				WebElement input = segmentedInputs.get(i);
				WaitUtil.waitForClickability(driver, input);
				input.clear();
				input.sendKeys(String.valueOf(passcode.charAt(i)));
			}
			return;
		}

		WebElement passcodeInput = new WebDriverWait(driver, Duration.ofSeconds(getTimeout())).until(
				ExpectedConditions.visibilityOfElementLocated(By.xpath(
						"//input[@type='password' or @type='tel' or contains(@placeholder,'Passcode') or contains(@placeholder,'passcode')]")));
		passcodeInput.clear();
		passcodeInput.sendKeys(passcode);
	}

	public void clickSubmitPasscode() {
		WebElement submitButton = new WebDriverWait(driver, Duration.ofSeconds(getTimeout())).until(
				ExpectedConditions.elementToBeClickable(By.xpath(
						"//button[@type='submit' or normalize-space()='Submit' or normalize-space()='Continue' or normalize-space()='Unlock']")));
		submitButton.click();
	}

	public boolean isCredentialStored(String credentialType) {
		return isElementPresent(By.xpath(buildTextMatchXpath(credentialType)));
	}

	public void selectCredentialForPresentation(){
		clickOnElement(driver, selectVCButton);
	}

	public void selectSecondCredentialForPresentation(){
		clickOnElement(driver, selectSecondVCButton);
	}

	public void clickConsentAndShareButton() {
 		clickOnElement(driver, consentAndShareButton);
	}

	public void switchToNewestWindowIfNeeded() {
		Set<String> windowHandles = driver.getWindowHandles();
		if (windowHandles.size() <= 2) {
			return;
		}

		String newestHandle = null;
		for (String handle : windowHandles) {
			newestHandle = handle;
		}
		if (newestHandle != null) {
			driver.switchTo().window(newestHandle);
		}
	}

	public void waitForPresentationPage() {
		new WebDriverWait(driver, Duration.ofSeconds(getTimeout() * 2)).until(webDriver -> {
			String currentUrl = webDriver.getCurrentUrl();
			boolean hasPresentAction = !webDriver.findElements(By.xpath(PRESENT_BUTTON_XPATH)).isEmpty();
			return currentUrl.contains("injiweb") && hasPresentAction;
		});
	}

	public boolean isPresentationActionVisible() {
		return isElementPresent(By.xpath(PRESENT_BUTTON_XPATH));
	}

	public String isNoMatchingCredentialsErrorVisible() {
		return getText(driver, noMatchingCredentialsError);
	}

	public String getErrorMessageForPartialSharing() {
		return getText(driver, partialSharingError);
	}

	public void clickOnHomeButton() {
		clickOnElement(driver, homeButton);
	}

	public void focusCurrentWindow() {
		((JavascriptExecutor) driver).executeScript("window.focus();");
	}

	private boolean isElementPresent(By locator) {
		try {
			new WebDriverWait(driver, Duration.ofSeconds(getTimeout() / 2))
					.until(ExpectedConditions.presenceOfElementLocated(locator));
			return !driver.findElements(locator).isEmpty();
		} catch (TimeoutException | NoSuchElementException e) {
			return false;
		}
	}

	private String buildTextMatchXpath(String text) {
		return "//*[self::h1 or self::h2 or self::h3 or self::h4 or self::span or self::p or self::div]["
				+ buildContainsTextCondition(text) + "]";
	}

	private String buildContainsTextCondition(String text) {
		String normalized = normalizeVisibleText(text);
		if (normalized == null) {
			normalized = "";
		}
		normalized = normalized.toLowerCase().replace("-", " ");
		return "contains(translate(translate(normalize-space(.),'ABCDEFGHIJKLMNOPQRSTUVWXYZ','abcdefghijklmnopqrstuvwxyz'),'-',' '),'"
				+ normalized + "')";
	}
}
