package base;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.IOException;
import java.net.HttpURLConnection;
import java.net.URL;
import java.time.Duration;
import java.util.List;

import org.openqa.selenium.*;
import org.openqa.selenium.remote.LocalFileDetector;
import org.openqa.selenium.remote.RemoteWebDriver;
import org.openqa.selenium.remote.RemoteWebElement;
import org.openqa.selenium.support.ui.ExpectedConditions;
import org.openqa.selenium.support.ui.FluentWait;
import org.openqa.selenium.support.ui.WebDriverWait;

import utils.WaitUtil;
import api.InjiVerifyConfigManager;

public class BasePage {
	
	private static final Logger logger = LoggerFactory.getLogger(BasePage.class);

    protected WebDriver driver;

    protected BasePage(WebDriver driver) {
        this.driver = driver;
    }

    public void waitForElementVisible(WebDriver driver, WebElement element) {
        WaitUtil.waitForVisibility(driver, element, getTimeout());
    }

    public void clickOnElement(WebDriver driver, WebElement element) {
        WaitUtil.waitForClickability(driver, element);
        element.click();
    }

    protected WebElement waitForElementClickable(WebDriver driver, By locator) {
        WebDriverWait wait = new WebDriverWait(driver, Duration.ofSeconds(getTimeout()));
        return wait.until(ExpectedConditions.elementToBeClickable(locator));
    }

public boolean isElementIsVisible(WebDriver driver, WebElement element) {
    int maxRetries = 5;
    int attempts = 0;

    while (attempts < maxRetries) {
        try {
            WaitUtil.waitForVisibility(driver, element, getTimeout());
            return element.isDisplayed();

        } catch (StaleElementReferenceException e) {
            logger.error("⚠️ Attempt " + (attempts + 1) + ": Element went stale. Retrying...");
            attempts++;

            try {
                // Re-wait explicitly instead of Thread.sleep
                WaitUtil.waitForVisibility(driver, element, getTimeout());
            } catch (Exception ex) {
                logger.error("Retry wait failed.");
            }

        } catch (TimeoutException e) {
            logger.error("⏰ Timeout waiting for element to be visible.");
            return false;
        }
    }
    return false;
}

public boolean isElementIsVisibleAfterIdle(WebDriver driver, WebElement element) {
    int maxRetries = 5;
    int attempts = 0;

    while (attempts < maxRetries) {
        try {
            WaitUtil.waitForVisibility(driver, element, getTimeout() * 4);
            return element.isDisplayed();

        } catch (StaleElementReferenceException e) {
            logger.error("⚠️ Attempt " + (attempts + 1) + ": Element went stale. Retrying...");
            attempts++;

        } catch (TimeoutException e) {
            logger.error("⏰ Timeout waiting for element to be visible.");
            return false;
        }
    }
    return false;
}

    public String getText(WebDriver driver, WebElement element) {
        waitForElementVisible(driver, element);
        return element.getText();
    }

    public Boolean isButtonEnabled(WebDriver driver, WebElement element) {
        waitForElementVisible(driver, element);
        return element.isEnabled();
    }

    public void enterText(WebDriver driver, By locator, String text) {
        WebElement element = new WebDriverWait(driver, Duration.ofSeconds(getTimeout()))
                .until(ExpectedConditions.presenceOfElementLocated(locator));
        element.clear();
        element.sendKeys(text);
    }

    public void refreshBrowser(WebDriver driver) {
        driver.navigate().refresh();
    }

    public void browserBackButton(WebDriver driver) {
        driver.navigate().back();
    }

public void uploadFile(WebDriver driver, WebElement fileInput, String filename) {

    String filePath = System.getProperty("user.dir") + File.separator + filename;
    File file = new File(filePath);

    if (!file.exists()) {
        throw new RuntimeException("File not found: " + filePath);
    }

    if (driver instanceof RemoteWebDriver) {
        ((RemoteWebDriver) driver).setFileDetector(new LocalFileDetector());
    }

    WaitUtil.waitForPresence(driver, By.xpath("//input[@type='file']"), getTimeout());

    fileInput.sendKeys(file.getAbsolutePath());
}


public void uploadFileForStaticQr(WebDriver driver, WebElement fileInputTrigger, String filename) {

    String filePath = System.getProperty("user.dir")
            + File.separator + "src"
            + File.separator + "test"
            + File.separator + "resources"
            + File.separator + "QRCodes"
            + File.separator + filename;

    File file = new File(filePath);

    if (!file.exists()) {
        throw new RuntimeException("❌ File not found: " + filePath);
    }

    By inputLocator = By.xpath("//input[@type='file']");

    int retries = 3;

    while (retries-- > 0) {
        try {

            // Click trigger (if required by UI)
            WaitUtil.waitForClickability(driver, fileInputTrigger);
            fileInputTrigger.click();

            // ✅ WAIT FOR PRESENCE NOT VISIBILITY
            WebDriverWait wait = new WebDriverWait(driver, Duration.ofSeconds(getTimeout()));
            WebElement fileInputElement =
                    wait.until(ExpectedConditions.presenceOfElementLocated(inputLocator));

            // ✅ Upload file (works even if hidden)
            fileInputElement.sendKeys(file.getAbsolutePath());

            logger.info("✅ File uploaded successfully.");
            return;

        } catch (StaleElementReferenceException e) {
            logger.error("⚠️ Stale element caught, retrying...");
        }
    }

    throw new RuntimeException("❌ Failed to upload file after retries.");
}

    public void waitForElementVisibleWithPolling(WebDriver driver, WebElement element) {
        FluentWait<WebDriver> wait = new FluentWait<>(driver)
                .withTimeout(Duration.ofSeconds(getTimeout() * 3))
                .pollingEvery(Duration.ofMillis(300))
                .ignoring(NoSuchElementException.class);
        wait.until(ExpectedConditions.visibilityOf(element));
    }

    public Boolean verifyHomePageLinks(WebDriver driver, List<WebElement> links) {
        boolean allLinksValid = true;

        for (WebElement link : links) {
            String url = link.getAttribute("href");

            if (url != null && !url.isEmpty()) {
                HttpURLConnection httpConn = null;
                try {
                    URL linkUrl = new URL(url);
                    httpConn = (HttpURLConnection) linkUrl.openConnection();
                    httpConn.setRequestMethod("HEAD");
                    httpConn.connect();
                    int responseCode = httpConn.getResponseCode();

                    if (responseCode < 200 || responseCode >= 400) {
                        logger.error(url + " - Broken link (Status " + responseCode + ")");
                        allLinksValid = false;
                    } else {
                        logger.info(url + " - Valid link (Status " + responseCode + ")");
                    }
                } catch (IOException e) {
                    logger.error(url + " - Exception occurred: " + e.getMessage());
                    allLinksValid = false;
                } finally {
                    if (httpConn != null) {
                        httpConn.disconnect();
                    }
                }
            }
        }
        return allLinksValid;
    }

    protected void sendKeysToTextBox(WebDriver driver, WebElement element, String text) {
        WaitUtil.waitForVisibility(driver, element, getTimeout());
        element.sendKeys(text);
    }
    
	public static int getTimeout() {
		try {
			String timeout = InjiVerifyConfigManager.getproperty("explicitWaitTimeout");
			if (timeout != null && !timeout.isEmpty()) {
				return Integer.parseInt(timeout);
			}
			return 30;
		} catch (NumberFormatException e) {
            logger.error("Invalid explicitWaitTimeout value from properties. Using default 30 seconds.");
            return 30;
		}
}
}
