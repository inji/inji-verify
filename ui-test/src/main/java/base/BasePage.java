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

import utils.BaseTest;
import utils.WaitUtil;

public class BasePage {
	
	private static final Logger logger = LoggerFactory.getLogger(BasePage.class);

    protected WebDriver driver;

    protected BasePage(WebDriver driver) {
        this.driver = driver;
    }

    public void waitForElementVisible(WebDriver driver, WebElement element) {
        WaitUtil.waitForVisibility(driver, element);
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
                WaitUtil.waitForVisibility(driver, element);
                return element.isDisplayed();
            } catch (StaleElementReferenceException e) {
                logger.error("⚠️ Attempt " + (attempts + 1) + ": Element went stale. Retrying...");
                attempts++;
                try {
                    Thread.sleep(900);
                } catch (InterruptedException ie) {
                    Thread.currentThread().interrupt();
                    return false;
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
                WebDriverWait wait = new WebDriverWait(driver, Duration.ofSeconds(getTimeout() * 4)); // scaling for idle
                wait.until(ExpectedConditions.visibilityOf(element));
                return element.isDisplayed();
            } catch (StaleElementReferenceException e) {
                logger.error("⚠️ Attempt " + (attempts + 1) + ": Element went stale. Retrying...");
                attempts++;
                try {
                    Thread.sleep(900);
                } catch (InterruptedException ie) {
                    Thread.currentThread().interrupt();
                    return false;
                }
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

    public void uploadFile(WebDriver driver, WebElement fileInputTrigger, String filename) {
        String filePath = System.getProperty("user.dir") + File.separator + filename;
        File file = new File(filePath);

        if (!file.exists()) {
            throw new RuntimeException("❌ File not found: " + filePath);
        }

        By inputLocator = By.xpath("//input[@type='file']");
        int retries = 3;
        while (retries-- > 0) {
            try {
                fileInputTrigger.click();
                WebElement fileInputElement = driver.findElement(inputLocator);
                if (fileInputElement instanceof RemoteWebElement) {
                    ((RemoteWebElement) fileInputElement).setFileDetector(new LocalFileDetector());
                }
                fileInputElement.sendKeys(file.getAbsolutePath());
                logger.info("✅ File uploaded successfully.");
                return;
            } catch (StaleElementReferenceException e) {
                logger.error("⚠️ Caught stale element exception, retrying...");
                try { Thread.sleep(1000); } catch (InterruptedException ie) {
                Thread.currentThread().interrupt();
                }
            }
        }

        throw new RuntimeException("❌ Failed to upload file due to repeated stale element exceptions.");
    }

    public void uploadFileForStaticQr(WebDriver driver, WebElement fileInputTrigger, String filename) {
        String filePath;
        String os = System.getProperty("os.name").toLowerCase();
        if (os.contains("win")) {
            filePath = System.getProperty("user.dir") + "\\src\\test\\resources\\QRCodes\\" + filename;
        } else {
            filePath = System.getProperty("user.dir") + "/QRCodes/" + filename;
        }

        File file = new File(filePath);
        if (!file.exists()) {
            throw new RuntimeException("❌ File not found: " + filePath);
        }

        By inputLocator = By.xpath("//input[@type='file']");
        int retries = 3;
        while (retries-- > 0) {
            try {
                fileInputTrigger.click();
                WebElement fileInputElement = driver.findElement(inputLocator);
                if (fileInputElement instanceof RemoteWebElement) {
                    ((RemoteWebElement) fileInputElement).setFileDetector(new LocalFileDetector());
                }
                fileInputElement.sendKeys(file.getAbsolutePath());
                logger.info("✅ File uploaded successfully.");
                return;
            } catch (StaleElementReferenceException e) {
                logger.error("⚠️ Caught stale element exception, retrying...");
                try { Thread.sleep(1000); } catch (InterruptedException ignored) {}
            }
        }

        throw new RuntimeException("❌ Failed to upload file due to repeated stale element exceptions.");
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
        WaitUtil.waitForVisibility(driver, element);
        element.sendKeys(text);
    }
    
	public static int getTimeout() {
		try {
			return Integer.parseInt(System.getProperty("explicitWaitTimeout", "30"));
		} catch (NumberFormatException e) {
            logger.error("Invalid explicitWaitTimeout system property value. Using default 30 seconds.");
            return 30;
		}
}
}
