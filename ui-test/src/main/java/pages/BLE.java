package pages;

import base.BasePage;
import org.openqa.selenium.By;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.WebElement;
import org.openqa.selenium.support.FindBy;
import org.openqa.selenium.support.PageFactory;

public class BLE extends BasePage {

	public BLE(WebDriver driver) {
		super(driver);
		PageFactory.initElements(driver, this);
	}

	@FindBy(xpath = "//button[@id='ble-tab']")
	WebElement bleTab;

	@FindBy(xpath = "//p[@id='alert-message']")
	WebElement bleAlertMsg;

	public void clickOnBleTab() {
		clickOnElement(driver, bleTab);
	}

	public String getInformationText() {
		return getText(driver, bleAlertMsg);
	}
}