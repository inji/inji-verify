package base;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.file.Files;
import java.time.Duration;
import java.util.Iterator;
import java.util.List;

import javax.imageio.IIOImage;
import javax.imageio.ImageIO;
import javax.imageio.ImageWriteParam;
import javax.imageio.ImageWriter;
import javax.imageio.stream.FileImageOutputStream;

import org.openqa.selenium.*;
import org.openqa.selenium.remote.LocalFileDetector;
import org.openqa.selenium.remote.RemoteWebDriver;
import org.openqa.selenium.remote.RemoteWebElement;
import org.openqa.selenium.support.ui.ExpectedConditions;
import org.openqa.selenium.support.ui.FluentWait;
import org.openqa.selenium.support.ui.WebDriverWait;

import utils.BaseTest;
import utils.WaitUtil;
import api.InjiVerifyConfigManager;

public class BasePage {
	
	private static final Logger logger = LoggerFactory.getLogger(BasePage.class);
    private static final String QR_RESOURCE_DIR = "src" + File.separator + "test" + File.separator + "resources"
            + File.separator + "QRCodes";
    private static final String RUNTIME_MEDIA_DIR = "test-output" + File.separator + "runtime-media";
    private static final String BOUNDARY_MIN_QR_NAME = "QRCode_10KB.jpg";
    private static final String BOUNDARY_MAX_QR_NAME = "QRCode_5MB.png";
    private static final long MIN_QR_TARGET_BYTES = 10_240L;
    private static final long MAX_QR_TARGET_BYTES = 4_999_000L;

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

    public String getAttributeValue(WebDriver driver, WebElement element, String attributeName) {
        waitForElementVisible(driver, element);
        return element.getAttribute(attributeName);
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

    File file = new File(filename);
    if (!file.isAbsolute()) {
        file = new File(System.getProperty("user.dir") + File.separator + filename);
    }

    if (!file.exists()) {
        throw new RuntimeException("File not found: " + file.getAbsolutePath());
    }

    if (BaseTest.isUsingBrowserStack() && driver instanceof RemoteWebDriver) {
        ((RemoteWebDriver) driver).setFileDetector(new LocalFileDetector());
    }

    WaitUtil.waitForPresence(driver, By.xpath("//input[@type='file']"), getTimeout());

    fileInput.sendKeys(file.getAbsolutePath());
}


public void uploadFileForStaticQr(WebDriver driver, WebElement fileInputTrigger, String filename) {

    File file = resolveQrUploadFile(filename);

    if (!file.exists()) {
        throw new RuntimeException("❌ File not found: " + file.getAbsolutePath());
    }

    if (BaseTest.isUsingBrowserStack() && driver instanceof RemoteWebDriver) {
        ((RemoteWebDriver) driver).setFileDetector(new LocalFileDetector());
    }

    By inputLocator = By.xpath("//input[@type='file']");

    int retries = 3;

    while (retries-- > 0) {
        try {
            WebDriverWait wait = new WebDriverWait(driver, Duration.ofSeconds(getTimeout()));
            WebElement fileInputElement =
                    wait.until(ExpectedConditions.presenceOfElementLocated(inputLocator));

            fileInputElement.sendKeys(file.getAbsolutePath());

            logger.info("✅ File uploaded successfully from {}", file.getAbsolutePath());
            return;

        } catch (StaleElementReferenceException e) {
            logger.error("⚠️ Stale element caught, retrying...");
        }
    }

    throw new RuntimeException("❌ Failed to upload file after retries.");
}

    private File resolveQrUploadFile(String filename) {
        if (BOUNDARY_MIN_QR_NAME.equals(filename)) {
            return generateRuntimeMinBoundaryQrFile();
        }
        if (BOUNDARY_MAX_QR_NAME.equals(filename)) {
            return generateRuntimeMaxBoundaryQrFile();
        }
        return new File(System.getProperty("user.dir") + File.separator + QR_RESOURCE_DIR + File.separator + filename);
    }

    private File generateRuntimeMinBoundaryQrFile() {
        File sourceFile = findPrimaryInsuranceCredentialImage();
        File outputFile = new File(getRuntimeMediaDirectory(), BOUNDARY_MIN_QR_NAME);

        try {
            writeBoundaryMinQrFile(sourceFile, outputFile);
            return outputFile;
        } catch (IOException e) {
            throw new RuntimeException("Failed to generate runtime 10KB QR file from " + sourceFile.getAbsolutePath(), e);
        }
    }

    private File generateRuntimeMaxBoundaryQrFile() {
        File sourceFile = findPrimaryInsuranceCredentialImage();
        File outputFile = new File(getRuntimeMediaDirectory(), BOUNDARY_MAX_QR_NAME);

        try {
            writeSquareQrPng(sourceFile, outputFile);
            padFileToExactSize(outputFile, MAX_QR_TARGET_BYTES);
            return outputFile;
        } catch (IOException e) {
            throw new RuntimeException("Failed to generate runtime 5MB QR file from " + sourceFile.getAbsolutePath(), e);
        }
    }

    private File getRuntimeMediaDirectory() {
        File runtimeDir = new File(System.getProperty("user.dir") + File.separator + RUNTIME_MEDIA_DIR);
        if (!runtimeDir.exists() && !runtimeDir.mkdirs()) {
            throw new RuntimeException("Unable to create runtime media directory: " + runtimeDir.getAbsolutePath());
        }
        return runtimeDir;
    }

    private File findPrimaryInsuranceCredentialImage() {
        String[] candidates = new String[] {
                BaseTest.getInsuranceCredentialPngPath(),
                BaseTest.getInsuranceCredentialJpgPath(),
                BaseTest.getInsuranceCredentialJpegPath()
        };

        for (String candidate : candidates) {
            if (candidate == null || candidate.trim().isEmpty()) {
                continue;
            }
            File file = new File(candidate);
            if (file.exists()) {
                return file;
            }
        }

        throw new RuntimeException("Unable to find InsuranceCredential0 source image for runtime QR generation.");
    }

    private void writeBoundaryMinQrFile(File sourceFile, File outputFile) throws IOException {
        BufferedImage croppedQrImage = loadCroppedQrImage(sourceFile);
        if (croppedQrImage == null) {
            throw new IOException("Unsupported image format: " + sourceFile.getAbsolutePath());
        }

        Candidate bestCandidate = null;
        float[] qualities = new float[] {0.08f, 0.06f, 0.05f, 0.04f, 0.03f, 0.02f, 0.01f, 0.008f, 0.005f};
        int maxHeight = croppedQrImage.getHeight();
        int minHeight = Math.min(40, maxHeight);

        for (int height = maxHeight; height >= minHeight; height -= 10) {
            int width = Math.max(2,
                    ((int) Math.round(height * ((double) croppedQrImage.getWidth() / croppedQrImage.getHeight()))) & ~1);
            BufferedImage scaledImage = scaleImage(croppedQrImage, width, height);
            try {
                for (float quality : qualities) {
                    writeJpeg(scaledImage, outputFile, quality);
                    long currentLength = outputFile.length();
                    if (currentLength <= MIN_QR_TARGET_BYTES) {
                        Candidate currentCandidate = new Candidate(width, height, quality, currentLength);
                        if (bestCandidate == null || currentCandidate.height > bestCandidate.height
                                || (currentCandidate.height == bestCandidate.height
                                        && currentCandidate.quality > bestCandidate.quality)) {
                            bestCandidate = currentCandidate;
                        }
                    }
                }
            } finally {
                scaledImage.flush();
            }
        }

        if (bestCandidate == null && maxHeight > 40) {
            for (int height = Math.min(maxHeight, 39); height >= 20; height -= 2) {
                int width = Math.max(2,
                        ((int) Math.round(height * ((double) croppedQrImage.getWidth() / croppedQrImage.getHeight()))) & ~1);
                BufferedImage scaledImage = scaleImage(croppedQrImage, width, height);
                try {
                    for (float quality : qualities) {
                        writeJpeg(scaledImage, outputFile, quality);
                        long currentLength = outputFile.length();
                        if (currentLength <= MIN_QR_TARGET_BYTES) {
                            Candidate currentCandidate = new Candidate(width, height, quality, currentLength);
                            if (bestCandidate == null || currentCandidate.height > bestCandidate.height
                                    || (currentCandidate.height == bestCandidate.height
                                            && currentCandidate.quality > bestCandidate.quality)) {
                                bestCandidate = currentCandidate;
                            }
                        }
                    }
                } finally {
                    scaledImage.flush();
                }
            }
        }

        if (bestCandidate == null) {
            throw new IOException("Unable to generate runtime 10KB QR file from source image.");
        }

        BufferedImage finalImage = scaleImage(croppedQrImage, bestCandidate.width, bestCandidate.height);
        try {
            writeJpeg(finalImage, outputFile, bestCandidate.quality);
            padFileToExactSize(outputFile, MIN_QR_TARGET_BYTES);
        } finally {
            finalImage.flush();
            croppedQrImage.flush();
        }
    }

    private void writeSquareQrPng(File sourceFile, File outputFile) throws IOException {
        BufferedImage sourceImage = loadCroppedQrImage(sourceFile);
        if (sourceImage == null) {
            throw new IOException("Unsupported image format: " + sourceFile.getAbsolutePath());
        }

        int squareSize = Math.max(sourceImage.getWidth(), sourceImage.getHeight());
        BufferedImage squareImage = new BufferedImage(squareSize, squareSize, BufferedImage.TYPE_INT_RGB);
        Graphics2D graphics = squareImage.createGraphics();
        try {
            graphics.setColor(java.awt.Color.WHITE);
            graphics.fillRect(0, 0, squareSize, squareSize);
            int x = (squareSize - sourceImage.getWidth()) / 2;
            int y = (squareSize - sourceImage.getHeight()) / 2;
            graphics.drawImage(sourceImage, x, y, null);
        } finally {
            graphics.dispose();
            sourceImage.flush();
        }

        try {
            ImageIO.write(squareImage, "png", outputFile);
        } finally {
            squareImage.flush();
        }
    }

    private BufferedImage loadCroppedQrImage(File sourceFile) throws IOException {
        BufferedImage sourceImage = ImageIO.read(sourceFile);
        if (sourceImage == null) {
            return null;
        }

        int[] qrBounds = detectQrBounds(sourceImage);
        BufferedImage croppedQrImage = new BufferedImage(qrBounds[2], qrBounds[3], BufferedImage.TYPE_INT_RGB);
        Graphics2D graphics = croppedQrImage.createGraphics();
        try {
            graphics.setColor(java.awt.Color.WHITE);
            graphics.fillRect(0, 0, qrBounds[2], qrBounds[3]);
            graphics.drawImage(sourceImage, 0, 0, qrBounds[2], qrBounds[3], qrBounds[0], qrBounds[1],
                    qrBounds[0] + qrBounds[2], qrBounds[1] + qrBounds[3], null);
        } finally {
            graphics.dispose();
            sourceImage.flush();
        }
        return croppedQrImage;
    }

    private int[] detectQrBounds(BufferedImage image) {
        int searchX = image.getWidth() / 2;
        int searchY = 0;
        int searchWidth = image.getWidth() - searchX;
        int searchHeight = Math.max(1, image.getHeight() / 2);

        int[] rowCounts = new int[searchHeight];
        int[] colCounts = new int[searchWidth];

        for (int y = searchY; y < searchY + searchHeight; y++) {
            for (int x = searchX; x < searchX + searchWidth; x++) {
                if (isDarkPixel(image.getRGB(x, y))) {
                    rowCounts[y - searchY]++;
                    colCounts[x - searchX]++;
                }
            }
        }

        int rowThreshold = Math.max(20, (int) Math.round(searchWidth * 0.18));
        int colThreshold = Math.max(20, (int) Math.round(searchHeight * 0.18));

        int minRow = findFirstIndexAboveThreshold(rowCounts, rowThreshold);
        int maxRow = findLastIndexAboveThreshold(rowCounts, rowThreshold);
        int minCol = findFirstIndexAboveThreshold(colCounts, colThreshold);
        int maxCol = findLastIndexAboveThreshold(colCounts, colThreshold);

        if (minRow == -1 || maxRow == -1 || minCol == -1 || maxCol == -1) {
            return getFallbackQrBounds(image);
        }

        int qrMinX = searchX + minCol;
        int qrMaxX = searchX + maxCol;
        int qrMinY = searchY + minRow;
        int qrMaxY = searchY + maxRow;

        int qrWidth = qrMaxX - qrMinX + 1;
        int qrHeight = qrMaxY - qrMinY + 1;
        int qrSize = Math.max(qrWidth, qrHeight);
        int margin = Math.max(18, (int) Math.round(qrSize * 0.12));
        int cropSize = qrSize + (margin * 2);

        int centerX = qrMinX + (qrWidth / 2);
        int centerY = qrMinY + (qrHeight / 2);

        int cropX = centerX - (cropSize / 2);
        int cropY = centerY - (cropSize / 2);

        if (cropX < 0) {
            cropX = 0;
        }
        if (cropY < 0) {
            cropY = 0;
        }
        if (cropX + cropSize > image.getWidth()) {
            cropX = Math.max(0, image.getWidth() - cropSize);
        }
        if (cropY + cropSize > image.getHeight()) {
            cropY = Math.max(0, image.getHeight() - cropSize);
        }

        cropSize = Math.min(cropSize, Math.min(image.getWidth() - cropX, image.getHeight() - cropY));
        return new int[] { cropX, cropY, cropSize, cropSize };
    }

    private boolean isDarkPixel(int rgb) {
        int red = (rgb >> 16) & 0xFF;
        int green = (rgb >> 8) & 0xFF;
        int blue = rgb & 0xFF;
        int brightness = (red + green + blue) / 3;
        return brightness < 140;
    }

    private int findFirstIndexAboveThreshold(int[] counts, int threshold) {
        for (int index = 0; index < counts.length; index++) {
            if (counts[index] >= threshold) {
                return index;
            }
        }
        return -1;
    }

    private int findLastIndexAboveThreshold(int[] counts, int threshold) {
        for (int index = counts.length - 1; index >= 0; index--) {
            if (counts[index] >= threshold) {
                return index;
            }
        }
        return -1;
    }

    private int[] getFallbackQrBounds(BufferedImage image) {
        int cropX = (int) Math.round(image.getWidth() * 0.46);
        int cropY = (int) Math.round(image.getHeight() * 0.04);
        int cropSize = Math.min(
                image.getWidth() - cropX - (int) Math.round(image.getWidth() * 0.04),
                (int) Math.round(image.getHeight() * 0.34));
        cropSize = Math.max(100, cropSize);
        cropSize = Math.min(cropSize, Math.min(image.getWidth() - cropX, image.getHeight() - cropY));
        return new int[] { cropX, cropY, cropSize, cropSize };
    }

    private BufferedImage scaleImage(BufferedImage sourceImage, int width, int height) {
        BufferedImage scaledImage = new BufferedImage(width, height, BufferedImage.TYPE_BYTE_GRAY);
        Graphics2D graphics = scaledImage.createGraphics();
        try {
            graphics.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_NEAREST_NEIGHBOR);
            graphics.setRenderingHint(RenderingHints.KEY_RENDERING, RenderingHints.VALUE_RENDER_SPEED);
            graphics.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_OFF);
            graphics.setColor(java.awt.Color.WHITE);
            graphics.fillRect(0, 0, width, height);
            graphics.drawImage(sourceImage, 0, 0, width, height, null);
        } finally {
            graphics.dispose();
        }
        return scaledImage;
    }

    private void writeJpeg(BufferedImage image, File outputFile, float quality) throws IOException {
        Iterator<ImageWriter> writers = ImageIO.getImageWritersByFormatName("jpg");
        if (!writers.hasNext()) {
            throw new IOException("No JPEG writer available for runtime QR generation.");
        }

        if (outputFile.exists() && !outputFile.delete()) {
            throw new IOException("Unable to replace runtime JPEG file: " + outputFile.getAbsolutePath());
        }

        ImageWriter writer = writers.next();
        try (FileImageOutputStream outputStream = new FileImageOutputStream(outputFile)) {
            writer.setOutput(outputStream);
            ImageWriteParam writeParam = writer.getDefaultWriteParam();
            if (writeParam.canWriteCompressed()) {
                writeParam.setCompressionMode(ImageWriteParam.MODE_EXPLICIT);
                writeParam.setCompressionQuality(quality);
            }
            writer.write(null, new IIOImage(image, null, null), writeParam);
        } finally {
            writer.dispose();
        }
    }

    private void padFileToExactSize(File file, long targetSize) throws IOException {
        long currentSize = file.length();
        if (currentSize > targetSize) {
            throw new IOException("Generated file exceeds target size: " + currentSize + " > " + targetSize);
        }
        if (currentSize == targetSize) {
            return;
        }

        try (OutputStream outputStream = Files.newOutputStream(file.toPath(), java.nio.file.StandardOpenOption.APPEND)) {
            byte[] buffer = new byte[4096];
            long remaining = targetSize - currentSize;
            while (remaining > 0) {
                int bytesToWrite = (int) Math.min(buffer.length, remaining);
                outputStream.write(buffer, 0, bytesToWrite);
                remaining -= bytesToWrite;
            }
        }
    }

    private static class Candidate {
        private final int width;
        private final int height;
        private final float quality;
        private final long size;

        private Candidate(int width, int height, float quality, long size) {
            this.width = width;
            this.height = height;
            this.quality = quality;
            this.size = size;
        }
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

	public static int getScanVerificationTimeoutMultiplier() {
		try {
			String multiplier = InjiVerifyConfigManager.getproperty("scanVerificationTimeoutMultiplier");
			if (multiplier != null && !multiplier.trim().isEmpty()) {
				return Integer.parseInt(multiplier.trim());
			}
		} catch (NumberFormatException e) {
			logger.error("Invalid scanVerificationTimeoutMultiplier value from properties. Using default 6.");
		}
		return 6;
	}
}
