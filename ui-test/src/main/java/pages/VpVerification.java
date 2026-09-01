package pages;

import org.openqa.selenium.By;
import org.openqa.selenium.JavascriptExecutor;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.WebElement;
import org.openqa.selenium.support.FindBy;
import org.openqa.selenium.support.PageFactory;
import org.openqa.selenium.support.ui.ExpectedConditions;
import org.openqa.selenium.support.ui.WebDriverWait;
import java.time.Duration;

import base.BasePage;

public class VpVerification extends BasePage {

	public VpVerification(WebDriver driver) {
		super(driver);
		PageFactory.initElements(driver, this);
	}

    @FindBy(id = "vp-verification-tab")
	WebElement vpVerificationTab;

	@FindBy(id = "tabs-carousel-right-icon")
	WebElement rightArrow;

	@FindBy(id = "selection-panel-back-button")
	WebElement vpGoBack;

	@FindBy(xpath = "//div[contains(@class, 'bg-default_theme-gradient')]/span[text()='✓']")
	WebElement mosipVC;

	@FindBy(xpath = "//p[@id='alert-message']")
	WebElement vpVerificationAlertMsg;

	@FindBy(id = "camera-access-denied-okay-button")
	WebElement generateQRCodeButton;

	@FindBy(id = "request-credentials-button")
	WebElement verifiableCredentialsButton;

	@FindBy(xpath = "//*[name()='path' and contains(@fill,'#000000')]")
	WebElement verificationQrCode;

    @FindBy(id = "ovp-loader")
	WebElement loadingScreen;

	@FindBy(xpath = "//h1[contains(@class,'text-selectorPanelTitle') and contains(text(),'Verifiable Credential Selection Panel')]")
	WebElement verifiableCredentialPanel;

	@FindBy(xpath = "(//div[contains(@class,'bg-default_theme-gradient') and contains(@class,'rounded-full')]/div[text()='2'])")
	WebElement VPverificationstep3LabelAfter;

	@FindBy(id = "initiate-vp-request-process-description")
	WebElement VpVerificationQrCodeStep1Description;

	@FindBy(id = "initiate-vp-request-process")
	WebElement vpVerificationQrCodeStep1Label;

	@FindBy(id = "select-credential-types")
	WebElement vpVerificationQrCodeStep2Label;

	@FindBy(id = "select-credential-types-description")
	WebElement vpVerificationQrCodeStep2Description;

	@FindBy(id = "share-verifiable-credentials-from-wallet")
	WebElement vpVerificationQrCodeStep3Label;

	@FindBy(id = "share-verifiable-credentials-from-wallet-description")
	WebElement vpVerificationQrCodeStep3Description;

	@FindBy(id = "view-verification-results")
	WebElement vpVerificationQrCodeStep4Label;

	@FindBy(id = "view-verification-results-description")
	WebElement vpVerificationQrCodeStep4Description;

	@FindBy(xpath = "//span[@class='text-sortByText font-semibold text-smallTextSize ml-2' and text()='Sort by']")
	WebElement SortButton;

	@FindBy(id = "verification-generate-qr-code-button")
	WebElement GenerateQrCodeButton;

	@FindBy(xpath = "//span[@class='walletName' and text()='Inji Wallet']")
	WebElement WalletButton;

	@FindBy(id = "wallet-selector-proceed-button")
	WebElement ProceedButton;

	@FindBy(xpath = "//button[contains(@class,'text-sortByText') and contains(text(),'Sort (A-Z)')]")
	WebElement SortAtoZButton;

	@FindBy(xpath = "//button[contains(@class,'text-sortByText') and contains(text(),'Sort (Z-A)')]")
	WebElement SortZtoAButton;

	@FindBy(xpath = "//button[@id='verification-back-button']")
	WebElement backButton;

	@FindBy(xpath = "(//button[contains(@class,'cancelButton') and contains(text(),'Cancel')])")
	WebElement cancelButton;

	@FindBy(id = "verification-open-wallet-button")
	WebElement openWalletButton;

	@FindBy(xpath = "//span[text()='Inji Wallet']")
	WebElement injiWallet;

	@FindBy(id = "wallet-selector-proceed-button")
	WebElement proccedButton;

	@FindBy(xpath = "//span[text()='Yes, I trust this Verifier']/parent::div")
	WebElement trustButton;


	public String getVpVerificationQrCodeStep1Description() {
		return getText(driver, VpVerificationQrCodeStep1Description);
	}

	public String getTransactionTerminatedText() {
		return getText(driver, vpVerificationAlertMsg);
	}

	public String getVpVerificationQrCodeStep1Label() {
		return getText(driver, vpVerificationQrCodeStep1Label);
	}

	public String getVpVerificationQrCodeStep2Label() {
		return getText(driver, vpVerificationQrCodeStep2Label);
	}

	public String getVpVerificationQrCodeStep2Description() {
		return getText(driver, vpVerificationQrCodeStep2Description);
	}

	public String getVpVerificationQrCodeStep3Label() {
		return getText(driver, vpVerificationQrCodeStep3Label);
	}

	public String getVpVerificationQrCodeStep3Description() {
		return normalizeVisibleText(getText(driver, vpVerificationQrCodeStep3Description));
	}

	public String getVpVerificationQrCodeStep4Label() {
		return getText(driver, vpVerificationQrCodeStep4Label);
	}

	public String getVpVerificationQrCodeStep4Description() {
		return getText(driver, vpVerificationQrCodeStep4Description);
	}

	public Boolean isVisibleVerifiableCredentialsButton() {
		return isElementIsVisible(driver, verifiableCredentialsButton);
	}

	public Boolean isVpVerificationQrCodeGenerated() {
		return isElementIsVisible(driver, verificationQrCode);
	}

	public Boolean isLoadingScreenDisplayed() {
		return isElementIsVisible(driver, loadingScreen);
	}

	public void clickOnVerifiableCredentialsButton() {
		openVerifiableCredentialsPanel();
		// Re-open recovery is first-open only. Later selection/assertion helpers must not
		// close/reopen the panel or they would discard multi-select checkbox state.
		ensureCredentialOptionsLoadedOnOpen();
	}

	private void openVerifiableCredentialsPanel() {
		try {
			// Set viewport and prevent zooming
			((JavascriptExecutor) driver)
					.executeScript("document.querySelector('meta[name=viewport]').setAttribute('content', "
							+ "'width=device-width, initial-scale=1.0, maximum-scale=1.0, user-scalable=0');");

			// Remove the copyright element temporarily to prevent interference
			((JavascriptExecutor) driver).executeScript("var copyright = document.getElementById('copyrights-content');"
					+ "if(copyright) { copyright.style.display = 'none'; }");

			// Scroll the button into view
			((JavascriptExecutor) driver).executeScript(
					"arguments[0].scrollIntoView({behavior: 'instant', block: 'center'});", verifiableCredentialsButton);

			// Click the button
			((JavascriptExecutor) driver).executeScript("arguments[0].click();", verifiableCredentialsButton);

			// Restore the copyright element
			((JavascriptExecutor) driver).executeScript("var copyright = document.getElementById('copyrights-content');"
					+ "if(copyright) { copyright.style.display = ''; }");
		} catch (Exception e) {
			throw new RuntimeException("Failed to click verifiable credentials button: " + e.getMessage());
		}
	}

	/**
	 * Claims are loaded asynchronously from config.json. Opening the panel before that
	 * finishes leaves an empty list that does not refresh. Re-open until rows appear.
	 * Only used on first open — not from per-credential guards after selections start.
	 */
	private void ensureCredentialOptionsLoadedOnOpen() {
		By credentialRow = By.xpath("//li[contains(@id,'-ItemBox')]");
		int rowWaitSeconds = Math.max(8, getTimeout() / 3);
		utils.WaitUtil.retryWithRecovery(
				() -> utils.WaitUtil.waitForPresence(driver, credentialRow, rowWaitSeconds),
				() -> {
					if (!driver.findElements(By.id("selection-panel-back-button")).isEmpty()) {
						clickOnGoBack();
					}
					new WebDriverWait(driver, Duration.ofSeconds(getTimeout()))
							.until(ExpectedConditions.elementToBeClickable(verifiableCredentialsButton));
					openVerifiableCredentialsPanel();
					new WebDriverWait(driver, Duration.ofSeconds(getTimeout()))
							.until(ExpectedConditions.visibilityOf(verifiableCredentialPanel));
				},
				4);
	}

	/**
	 * Waits for any credential row to be present without closing/reopening the panel.
	 * Safe to call after checkboxes have already been selected.
	 */
	public void waitForCredentialOptionsToLoad() {
		By credentialRow = By.xpath("//li[contains(@id,'-ItemBox')]");
		utils.WaitUtil.waitForPresence(driver, credentialRow, Math.max(8, getTimeout() / 3));
	}

	public String isVerifiableCredentialSelectionPannelDisplayed() {
		waitForCredentialOptionsToLoad();
		return getText(driver, verifiableCredentialPanel);
	}

	public void clickOnVPVerificationTab() {
		clickOnElement(driver, vpVerificationTab);
	}

	public void clickOnRightArrow() {
		clickOnElement(driver, rightArrow);
	}

	public boolean isGoBackButtonVisible() {
		return isElementIsVisible(driver, vpGoBack);
	}

	public void clickOnGoBack() {
		clickOnElement(driver, vpGoBack);
	}

	public void clickOnMosipVC() {
		clickOnElement(driver, mosipVC);
	}

	public void clickOnGenerateQRCodeButton() {
		clickOnElement(driver, generateQRCodeButton);
	}

	public String getInformationMessage() {
		return getText(driver, vpVerificationAlertMsg);
	}

	public void enterVcInSearchBox(String string) {
		enterText(driver, By
				.xpath("//input[@type='text' and contains(@placeholder, 'Search for the Verifiable Credential type')]"),
				string);

	}

	public boolean isVisibleVPverificationstep3LabelAfter() {
		return isElementIsVisible(driver, VPverificationstep3LabelAfter);
	}

	public void clickOnSortButton() {
		clickOnElement(driver, SortButton);
	}

	public void clickOnGenerateQrCodeButton() {
		clickOnElement(driver, GenerateQrCodeButton);
	}

	public void clickOnWalletButton() {
		clickOnElement(driver, WalletButton);
	}

	public void clickOnProceedButton() {
		clickOnElement(driver, ProceedButton);
	}

	public void clickOnSortAtoZButton() {
		clickOnElement(driver, SortAtoZButton);
	}

	public void clickOnSortZtoAButton() {
		clickOnElement(driver, SortZtoAButton);
	}

	public void enterCredentialType(String string) {
		enterText(driver, By.xpath(
				"//input[@placeholder='Search VC Type']"),
				string);
	}

	public void clickOnBackButton() {
		clickOnElement(driver, vpGoBack);
	}

	public void clickOnCancelButton() {
		clickOnElement(driver, cancelButton);
	}

	public void clickOnOpenWalletButton() {
		clickOnElement(driver, openWalletButton);
	}

		public void selectWallet() {
		clickOnElement(driver, injiWallet);
	}

	public void proccedButton() {
		clickOnElement(driver, proccedButton);
	}

	public void trustButton() {
		new WebDriverWait(driver, Duration.ofSeconds(getTimeout()));
		clickOnElement(driver, trustButton);
	}

	public boolean isWalletOptionVisible(String walletName) {
		String normalizedWalletName = normalizeVisibleText(walletName);
		if (normalizedWalletName == null) {
			normalizedWalletName = "";
		}
		normalizedWalletName = normalizedWalletName.toLowerCase().replace("-", " ");
		return !driver.findElements(By.xpath(
				"//span[contains(@class,'walletName') and contains(translate(translate(normalize-space(.),"
						+ "'ABCDEFGHIJKLMNOPQRSTUVWXYZ','abcdefghijklmnopqrstuvwxyz'),'-',' '),'" + normalizedWalletName
						+ "')]")).isEmpty();
	}


	private static String toXPathLiteral(String value) {
		if (value == null) {
			return "''";
		}
		if (!value.contains("'")) {
			return "'" + value + "'";
		}
		if (!value.contains("\"")) {
			return "\"" + value + "\"";
		}
		StringBuilder expression = new StringBuilder("concat(");
		String[] parts = value.split("'");
		for (int i = 0; i < parts.length; i++) {
			if (i > 0) {
				expression.append(", \"'\", ");
			}
			expression.append("'").append(parts[i]).append("'");
		}
		expression.append(")");
		return expression.toString();
	}

	public void clickOnCredentialChecklist(String credentialName) {
		waitForCredentialOptionsToLoad();
		By locator = By.xpath("//li[@id=" + toXPathLiteral(credentialName + "-ItemBox")
				+ "]//label | //label[@for=" + toXPathLiteral(credentialName) + "]");
		clickOnElement(driver, waitForElementClickable(driver, locator));
	}

	public boolean isCredentialTypeVisible(String credentialName) {
		waitForCredentialOptionsToLoad();
		By locator = By.xpath(
				"//li[@id=" + toXPathLiteral(credentialName + "-ItemBox") + "] | "
						+ "//span[contains(@class, 'text-smallTextSize') and contains(text(), "
						+ toXPathLiteral(credentialName) + ")]");
		try {
			new WebDriverWait(driver, Duration.ofSeconds(getTimeout()))
					.until(ExpectedConditions.visibilityOfElementLocated(locator));
			return true;
		} catch (org.openqa.selenium.TimeoutException e) {
			return false;
		}
	}

	public boolean isCredentialSelected(String credentialName) {
		waitForCredentialOptionsToLoad();
		By checkboxLocator = By.xpath("//li[@id=" + toXPathLiteral(credentialName + "-ItemBox")
				+ "]//input[@type='checkbox'] | //label[@for=" + toXPathLiteral(credentialName)
				+ "]//input[@type='checkbox']");
		WebElement checkbox = new WebDriverWait(driver, Duration.ofSeconds(getTimeout())).until(
				ExpectedConditions.presenceOfElementLocated(checkboxLocator));
		if (checkbox.isSelected() || "true".equalsIgnoreCase(checkbox.getAttribute("checked"))
				|| "true".equalsIgnoreCase(checkbox.getAttribute("aria-checked"))) {
			return true;
		}
		// Custom checkbox UI keeps the input hidden; selected state is shown via checkmark.
		return !driver.findElements(By.xpath("//li[@id=" + toXPathLiteral(credentialName + "-ItemBox")
				+ "]//span[normalize-space()='✓' and contains(@class,'block')]")).isEmpty();
	}

	public void waitForOpenWalletButton() {
		new WebDriverWait(driver, Duration.ofSeconds(getTimeout()))
				.until(ExpectedConditions.elementToBeClickable(openWalletButton));
	}

	public void waitForRequestCredentialsButton() {
		new WebDriverWait(driver, Duration.ofSeconds(getTimeout()))
				.until(ExpectedConditions.elementToBeClickable(verifiableCredentialsButton));
	}

	public void waitForWalletChooser() {
		new WebDriverWait(driver, Duration.ofSeconds(getTimeout())).until(
				ExpectedConditions.or(
						ExpectedConditions.presenceOfElementLocated(By.xpath("//span[contains(@class,'walletName')]")),
						ExpectedConditions.visibilityOf(ProceedButton)));
	}

	public void waitForVerifyResults() {
		new WebDriverWait(driver, Duration.ofSeconds(getTimeout() * 2))
				.until(ExpectedConditions.or(
						ExpectedConditions.urlContains("injiverify"),
						ExpectedConditions.presenceOfElementLocated(By.id("vc-result-display-message")),
						ExpectedConditions.presenceOfElementLocated(By.id("success_message_icon"))));
	}


}
