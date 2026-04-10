package utils;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.awt.Graphics2D;
import java.awt.image.BufferedImage;
import java.net.URI;
import java.nio.file.Files;
import java.nio.file.StandardCopyOption;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import javax.imageio.ImageIO;

import api.InjiVerifyConfigManager;
import io.cucumber.java.Scenario;
import io.mosip.testrig.apirig.testrunner.BaseTestCase;
import org.json.JSONObject;
import org.json.JSONTokener;
import org.openqa.selenium.chrome.ChromeOptions;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class BaseTestUtil {
    private static final Logger logger = LoggerFactory.getLogger(BaseTestUtil.class);
    // Chrome loops Y4M files, so only a small number of frames is needed for the QR scanner
    // to detect the code on the first or second loop. Fewer frames = smaller file = faster
    // Chrome camera initialisation under parallel load.
    private static final int SCAN_VIDEO_FRAMES_HIGH_RES = 5;  // 15mp (~111MB) / 8mp (~62MB)
    private static final int SCAN_VIDEO_FRAMES_DEFAULT   = 10; // 2mp / low_light / default (~15-31MB)
    private static final String RUNTIME_MEDIA_DIR = "test-output" + File.separator + "runtime-media";
    private static final String LOCAL_DOWNLOADS_DIR = RUNTIME_MEDIA_DIR + File.separator + "downloads";
    private static final String RUNTIME_SCAN_IMAGE_NAME = "ScanQrCode-runtime.png";
    private static final String RUNTIME_SCAN_PREVIEW_IMAGE_NAME = "ScanQrCode-runtime-preview.png";
    private static final String SHARED_SCAN_MEDIA_DIR = RUNTIME_MEDIA_DIR + File.separator + "shared-scan-media";
    private static final Object sharedScanMediaLock = new Object();
    private static final Map<String, String> generatedSharedScanVideos = new ConcurrentHashMap<>();

    public JSONObject readConfig(Class<?> obj, String environment) {
        try {
            InputStream inputStream = obj.getClassLoader().getResourceAsStream("config.json");
            if (inputStream == null) {
                throw new RuntimeException("Config file 'config.json' not found");
            }

            JSONTokener tokener = new JSONTokener(inputStream);
            JSONObject jsonObject = new JSONObject(tokener);

            JSONObject config = jsonObject.optJSONObject(environment);
            if (config == null) {
                throw new RuntimeException("Environment '" + environment + "' not found in the config");
            }

            return config;
        } catch (Exception e) {
            e.printStackTrace();
            return null;
        }
    }

    static HashMap<String, Object> getMobileChromeOptions() {
        HashMap<String, Object> chromeOptions = new HashMap<>();
        chromeOptions.put("mobileEmulation", getMobileEmulationOptions());
        return chromeOptions;
    }

    private static HashMap<String, Object> getMobileEmulationOptions() {
        HashMap<String, Object> mobileEmulation = new HashMap<>();
        HashMap<String, Object> deviceMetrics = new HashMap<>();
        deviceMetrics.put("width", 412);
        deviceMetrics.put("height", 915);
        deviceMetrics.put("pixelRatio", 2.625);
        deviceMetrics.put("mobile", true);
        mobileEmulation.put("deviceMetrics", deviceMetrics);
        mobileEmulation.put("userAgent", "Mozilla/5.0 (Linux; Android 12; SM-S901E) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/137.0.0.0 Mobile Safari/537.36");
        return mobileEmulation;
    }

    protected ChromeOptions getLocalChromeOptions(boolean scanMode, boolean mobileView) {
        ChromeOptions chromeOptions = new ChromeOptions();
        chromeOptions.addArguments("--remote-allow-origins=*");
        chromeOptions.addArguments("--disable-notifications");
        chromeOptions.addArguments("--disable-infobars");
        chromeOptions.addArguments("--no-sandbox");
        chromeOptions.addArguments("--disable-dev-shm-usage");
        chromeOptions.setExperimentalOption("prefs", getLocalChromePreferences());
        chromeOptions.setCapability("goog:loggingPrefs", getChromeLoggingPreferences());

        if (isRunningFromIde()) {
            logger.info("Running local ChromeDriver in headed mode from IDE.");
            chromeOptions.addArguments("--start-maximized");
        } else {
            logger.info("Running local ChromeDriver in headless mode.");
            chromeOptions.addArguments("--headless=new");
            chromeOptions.addArguments("--window-size=1920,1080");
        }

        if (scanMode) {
            String videoPath = generateRuntimeY4mFromInsuranceCredential();
            logger.info("Launching test in SCAN mode with local ChromeDriver");
            chromeOptions.addArguments("--use-fake-device-for-media-stream");
            chromeOptions.addArguments("--use-file-for-fake-video-capture=" + videoPath);
            if (BaseTest.shouldAutoAllowScanCamera()) {
                chromeOptions.addArguments("--use-fake-ui-for-media-stream");
            }
        } else if (mobileView) {
            logger.info("Launching test in MOBILE VIEW with local ChromeDriver");
            chromeOptions.setExperimentalOption("mobileEmulation", getMobileEmulationOptions());
        } else {
            logger.info("Launching test in DESKTOP mode with local ChromeDriver");
        }

        return chromeOptions;
    }

    protected Map<String, Object> getChromeLoggingPreferences() {
        HashMap<String, Object> loggingPrefs = new HashMap<>();
        loggingPrefs.put("performance", "ALL");
        loggingPrefs.put("browser", "ALL");
        return loggingPrefs;
    }

    private Map<String, Object> getLocalChromePreferences() {
        File downloadDir = getLocalDownloadsDirectory();
        try {
            Files.createDirectories(downloadDir.toPath());
        } catch (IOException e) {
            throw new RuntimeException("Unable to create local download directory: " + downloadDir.getAbsolutePath(), e);
        }

        HashMap<String, Object> prefs = new HashMap<>();
        prefs.put("download.default_directory", downloadDir.getAbsolutePath());
        prefs.put("download.prompt_for_download", false);
        prefs.put("download.directory_upgrade", true);
        prefs.put("plugins.always_open_pdf_externally", true);
        prefs.put("safebrowsing.enabled", true);
        return prefs;
    }

    protected static File getLocalDownloadsDirectory() {
        return new File(System.getProperty("user.dir"), LOCAL_DOWNLOADS_DIR);
    }

    private String generateRuntimeY4mFromInsuranceCredential() {
        refreshRuntimeScanImages();
        File sourceImage = findScanVideoSourceImage();
        String cameraProfile = getScanCameraProfile();
        try {
            File y4mFile = resolveSharedScanVideo(sourceImage, cameraProfile);
            return y4mFile.getAbsolutePath();
        } catch (IOException e) {
            throw new RuntimeException("Failed to prepare Y4M file from " + sourceImage.getAbsolutePath(), e);
        }
    }

    public void refreshRuntimeScanMediaForCurrentScenario() {
        refreshRuntimeScanImages();
        generateRuntimeY4mFromInsuranceCredential();
    }

    private void refreshRuntimeScanImages() {
        File outputDir = getRuntimeMediaDirectory();
        File sourceImage = findPrimaryInsuranceCredentialImage(outputDir);
        File runtimeQrImage = new File(outputDir, RUNTIME_SCAN_IMAGE_NAME);
        File previewQrImage = new File(outputDir, RUNTIME_SCAN_PREVIEW_IMAGE_NAME);

        try {
            writeRuntimeScanQrCodeImage(sourceImage, runtimeQrImage);
            writeRuntimeScanQrCodePreviewImage(sourceImage, previewQrImage);
            logger.info("Generated fresh runtime scan images: {}, {}",
                    runtimeQrImage.getAbsolutePath(),
                    previewQrImage.getAbsolutePath());
        } catch (IOException e) {
            throw new RuntimeException("Failed to generate runtime scan images from " + sourceImage.getAbsolutePath(), e);
        }
    }

    private File findInsuranceCredentialImage() {
        String selectedScanQrCodeFile = BaseTest.getSelectedScanQrCodeFile();
        if (selectedScanQrCodeFile == null || selectedScanQrCodeFile.trim().isEmpty()) {
            throw new RuntimeException("No scan QR code file selected for current scenario.");
        }

        File file = new File(selectedScanQrCodeFile);
        if (!file.isAbsolute()) {
            file = new File(System.getProperty("user.dir"), selectedScanQrCodeFile);
        }
        if (!file.exists()) {
            throw new RuntimeException("Selected scan QR code image not found: " + file.getAbsolutePath());
        }
        return file;
    }

    private File findScanVideoSourceImage() {
        File runtimeFile = new File(getRuntimeMediaDirectory(), RUNTIME_SCAN_IMAGE_NAME);
        File previewFile = new File(getRuntimeMediaDirectory(), RUNTIME_SCAN_PREVIEW_IMAGE_NAME);
        File fallbackFile = findInsuranceCredentialImage();
        java.util.Set<String> tags = BaseTest.getCurrentScenarioTags();

        if (shouldUseRuntimeScanImage(tags)) {
            if (isReadableImage(runtimeFile)) {
                return runtimeFile;
            }
            if (isReadableImage(previewFile)) {
                return previewFile;
            }
        } else if (shouldUsePreviewScanImage(tags)) {
            if (isReadableImage(previewFile)) {
                return previewFile;
            }
            if (isReadableImage(runtimeFile)) {
                return runtimeFile;
            }
        } else {
            if (isReadableImage(fallbackFile)) {
                return fallbackFile;
            }
            if (isReadableImage(previewFile)) {
                return previewFile;
            }
            if (isReadableImage(runtimeFile)) {
                return runtimeFile;
            }
        }

        if (isReadableImage(fallbackFile)) {
            return fallbackFile;
        }
        throw new RuntimeException("No readable scan video source image available.");
    }

    private boolean shouldUseRuntimeScanImage(java.util.Set<String> tags) {
        return tags.contains("@camera_low_light")
                || tags.contains("@camera_2mp")
                || tags.contains("@camera_8mp")
                || tags.contains("@camera_15mp");
    }

    private boolean shouldUsePreviewScanImage(java.util.Set<String> tags) {
        return tags.contains("@camera_denied")
                || tags.contains("@verifyFirstTimeScanQrCodePermissionPrompt")
                || tags.contains("@verifyScanQrCodeWithAllowedCameraAccess")
                || tags.contains("@qr_valid");
    }

    private void writeY4mFile(File sourceImage, File targetY4m, String cameraProfile) throws IOException {
        BufferedImage inputImage = ImageIO.read(sourceImage);
        if (inputImage == null) {
            throw new IOException("Unsupported image format: " + sourceImage.getAbsolutePath());
        }

        int[] videoSize = getScanVideoSize(cameraProfile);
        BufferedImage image = createScanVideoFrame(inputImage, videoSize[0], videoSize[1]);
        int width = image.getWidth();
        int height = image.getHeight();

        try (OutputStream outputStream = new FileOutputStream(targetY4m)) {
            String header = String.format(
                    "YUV4MPEG2 W%d H%d F30:1 Ip A0:0 C420mpeg2 XYSCSS=420MPEG2 XCOLORRANGE=LIMITED%n",
                    width,
                    height
            );
            outputStream.write(header.getBytes("US-ASCII"));

            byte[] yPlane = new byte[width * height];
            byte[] uPlane = new byte[(width / 2) * (height / 2)];
            byte[] vPlane = new byte[(width / 2) * (height / 2)];

            for (int y = 0; y < height; y += 2) {
                for (int x = 0; x < width; x += 2) {
                    double uSum = 0;
                    double vSum = 0;

                    for (int dy = 0; dy < 2; dy++) {
                        for (int dx = 0; dx < 2; dx++) {
                            int rgb = image.getRGB(x + dx, y + dy);
                            int r = (rgb >> 16) & 0xFF;
                            int g = (rgb >> 8) & 0xFF;
                            int b = rgb & 0xFF;

                            int yy = clamp((int) Math.round(0.299 * r + 0.587 * g + 0.114 * b));
                            double u = -0.168736 * r - 0.331264 * g + 0.5 * b + 128;
                            double v = 0.5 * r - 0.418688 * g - 0.081312 * b + 128;

                            yPlane[(y + dy) * width + (x + dx)] = (byte) yy;
                            uSum += u;
                            vSum += v;
                        }
                    }

                    int uvIndex = (y / 2) * (width / 2) + (x / 2);
                    uPlane[uvIndex] = (byte) clamp((int) Math.round(uSum / 4.0));
                    vPlane[uvIndex] = (byte) clamp((int) Math.round(vSum / 4.0));
                }
            }

            int frameCount = getScanVideoFrameCount(cameraProfile);
            byte[] frameHeader = "FRAME\n".getBytes("US-ASCII");
            for (int frame = 0; frame < frameCount; frame++) {
                outputStream.write(frameHeader);
                outputStream.write(yPlane);
                outputStream.write(uPlane);
                outputStream.write(vPlane);
            }
        }
    }

    private BufferedImage createScanVideoFrame(BufferedImage sourceImage, int videoWidth, int videoHeight) {
        BufferedImage rgbImage = new BufferedImage(videoWidth, videoHeight, BufferedImage.TYPE_INT_RGB);
        Graphics2D graphics = rgbImage.createGraphics();
        try {
            graphics.setColor(java.awt.Color.WHITE);
            graphics.fillRect(0, 0, videoWidth, videoHeight);

            double scale = Math.min(1.0d, Math.min(
                    (double) videoWidth / sourceImage.getWidth(),
                    (double) videoHeight / sourceImage.getHeight()
            ));
            int scaledWidth = Math.max(2, ((int) Math.round(sourceImage.getWidth() * scale)) & ~1);
            int scaledHeight = Math.max(2, ((int) Math.round(sourceImage.getHeight() * scale)) & ~1);
            int x = (videoWidth - scaledWidth) / 2;
            int y = (videoHeight - scaledHeight) / 2;

            graphics.drawImage(sourceImage, x, y, scaledWidth, scaledHeight, null);
        } finally {
            graphics.dispose();
        }
        if ("low_light".equals(getScanCameraProfile())) {
            applyBrightness(rgbImage, 0.38f);
        }
        return rgbImage;
    }

    private int[] getScanVideoSize(String cameraProfile) {
        if ("2mp".equals(cameraProfile)) {
            return new int[] { 1920, 1080 };
        }
        if ("8mp".equals(cameraProfile)) {
            return new int[] { 3840, 2160 };
        }
        if ("15mp".equals(cameraProfile)) {
            return new int[] { 5120, 2880 };
        }
        if ("low_light".equals(cameraProfile)) {
            return new int[] { 1920, 1080 };
        }
        return new int[] { 1920, 1080 };
    }

    private int getScanVideoFrameCount(String cameraProfile) {
        if ("15mp".equals(cameraProfile) || "8mp".equals(cameraProfile)) {
            return SCAN_VIDEO_FRAMES_HIGH_RES;
        }
        return SCAN_VIDEO_FRAMES_DEFAULT;
    }

    private String getScanCameraProfile() {
        String cameraProfile = BaseTest.getSelectedScanCameraProfile();
        return cameraProfile == null || cameraProfile.trim().isEmpty() ? "default" : cameraProfile;
    }

    private int clamp(int value) {
        return Math.max(0, Math.min(255, value));
    }

    private void applyBrightness(BufferedImage image, float factor) {
        for (int y = 0; y < image.getHeight(); y++) {
            for (int x = 0; x < image.getWidth(); x++) {
                int rgb = image.getRGB(x, y);
                int r = clamp(Math.round(((rgb >> 16) & 0xFF) * factor));
                int g = clamp(Math.round(((rgb >> 8) & 0xFF) * factor));
                int b = clamp(Math.round((rgb & 0xFF) * factor));
                int adjustedRgb = (r << 16) | (g << 8) | b;
                image.setRGB(x, y, adjustedRgb);
            }
        }
    }

    private boolean isRunningFromIde() {
        String classPath = System.getProperty("java.class.path", "").toLowerCase();
        String command = System.getProperty("sun.java.command", "").toLowerCase();

        return classPath.contains("idea_rt.jar")
                || classPath.contains("eclipse")
                || classPath.contains("junit")
                || command.contains("com.intellij")
                || command.contains("org.eclipse");
    }

    protected String resolveScanQrCodeFile(Scenario scenario) {
        if (scenario.getSourceTagNames().contains("@offlineScan")) {
            return "src/test/resources/QRCodes/QRCode.png";
        }
        if (scenario.getSourceTagNames().contains("@browser_back_navigation")) {
            return createNavigationOnlyScanSourceImage().getAbsolutePath();
        }
        if (scenario.getSourceTagNames().contains("@qr_valid")) {
            return generateRuntimeScanQrCodeFromInsuranceCredential();
        }
        if (scenario.getSourceTagNames().contains("@qr_half")) {
            return "src/test/resources/QRCodes/HalfQrCode.png";
        }
        if (scenario.getSourceTagNames().contains("@qr_invalid")) {
            return "src/test/resources/QRCodes/Invalid.png";
        }
        if (scenario.getSourceTagNames().contains("@qr_expired")) {
            return "src/test/resources/QRCodes/Expired_QRCode.png";
        }
        if (scenario.getSourceTagNames().contains("@qr_visual_effect")) {
            return "src/test/resources/QRCodes/blur.PNG";
        }
        throw new RuntimeException("Missing scan QR tag for scenario: " + scenario.getName());
    }

    private String generateRuntimeScanQrCodeFromInsuranceCredential() {
        refreshRuntimeScanImages();
        File runtimeQrImage = new File(getRuntimeMediaDirectory(), RUNTIME_SCAN_IMAGE_NAME);
        return runtimeQrImage.getAbsolutePath();
    }

    private File getRuntimeMediaDirectory() {
        String scenarioRuntimeDir = BaseTest.getScenarioRuntimeDir();
        File outputDir = scenarioRuntimeDir == null || scenarioRuntimeDir.trim().isEmpty()
                ? new File(System.getProperty("user.dir") + File.separator + RUNTIME_MEDIA_DIR)
                : new File(scenarioRuntimeDir);
        if (!outputDir.exists() && !outputDir.mkdirs()) {
            throw new RuntimeException("Unable to create runtime media directory: " + outputDir.getAbsolutePath());
        }
        return outputDir;
    }

    private File resolveSharedScanVideo(File sourceImage, String cameraProfile) throws IOException {
        File sharedDir = new File(System.getProperty("user.dir"), SHARED_SCAN_MEDIA_DIR);
        Files.createDirectories(sharedDir.toPath());

        String cacheKey = buildScanVideoCacheKey(sourceImage, cameraProfile);
        String cachedPath = generatedSharedScanVideos.get(cacheKey);
        if (cachedPath != null) {
            File cachedFile = new File(cachedPath);
            if (cachedFile.exists() && cachedFile.length() > 0) {
                return cachedFile;
            }
            generatedSharedScanVideos.remove(cacheKey);
        }

        synchronized (sharedScanMediaLock) {
            cachedPath = generatedSharedScanVideos.get(cacheKey);
            if (cachedPath != null) {
                File cachedFile = new File(cachedPath);
                if (cachedFile.exists() && cachedFile.length() > 0) {
                    return cachedFile;
                }
                generatedSharedScanVideos.remove(cacheKey);
            }

            String safeBaseName = sourceImage.getName().replace('.', '_') + "-" + cameraProfile + "-" + Integer.toHexString(cacheKey.hashCode());
            File targetFile = new File(sharedDir, safeBaseName + ".y4m");
            if (!targetFile.exists() || targetFile.length() == 0) {
                File tempFile = new File(sharedDir, safeBaseName + ".tmp");
                writeY4mFile(sourceImage, tempFile, cameraProfile);
                Files.move(tempFile.toPath(), targetFile.toPath(), StandardCopyOption.REPLACE_EXISTING, StandardCopyOption.ATOMIC_MOVE);
                logger.info("Generated shared Y4M file for scan mode: {}", targetFile.getAbsolutePath());
            }

            generatedSharedScanVideos.put(cacheKey, targetFile.getAbsolutePath());
            return targetFile;
        }
    }

    private String buildScanVideoCacheKey(File sourceImage, String cameraProfile) {
        return sourceImage.getAbsolutePath() + "|" + sourceImage.lastModified() + "|" + sourceImage.length() + "|" + cameraProfile;
    }

    private File findPrimaryInsuranceCredentialImage(File outputDir) {
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

        return createPlaceholderScanSourceImage(outputDir);
    }

    private File createPlaceholderScanSourceImage(File outputDir) {
        File placeholderFile = new File(outputDir, RUNTIME_SCAN_IMAGE_NAME);
        return createBlankScanSourceImage(placeholderFile,
                "Created placeholder scan source image until insurance artifacts are prepared: {}");
    }

    private File createNavigationOnlyScanSourceImage() {
        File outputDir = getRuntimeMediaDirectory();
        File blankFile = new File(outputDir, "ScanQrCode-navigation-only.png");
        return createBlankScanSourceImage(blankFile,
                "Created blank scan source image for browser-back navigation scenario: {}");
    }

    private File createBlankScanSourceImage(File outputFile, String logMessage) {
        BufferedImage placeholderImage = new BufferedImage(1080, 1080, BufferedImage.TYPE_INT_RGB);
        Graphics2D graphics = placeholderImage.createGraphics();
        try {
            graphics.setColor(java.awt.Color.WHITE);
            graphics.fillRect(0, 0, placeholderImage.getWidth(), placeholderImage.getHeight());
        } finally {
            graphics.dispose();
        }

        try {
            ImageIO.write(placeholderImage, "png", outputFile);
            logger.info(logMessage, outputFile.getAbsolutePath());
            return outputFile;
        } catch (IOException e) {
            throw new RuntimeException("Unable to create blank scan source image: "
                    + outputFile.getAbsolutePath(), e);
        } finally {
            placeholderImage.flush();
        }
    }

    private void writeRuntimeScanQrCodeImage(File sourceImage, File targetImage) throws IOException {
        BufferedImage sourceBufferedImage = ImageIO.read(sourceImage);
        if (sourceBufferedImage == null) {
            throw new IOException("Unsupported image format: " + sourceImage.getAbsolutePath());
        }

        BufferedImage copiedImage = new BufferedImage(
                sourceBufferedImage.getWidth(),
                sourceBufferedImage.getHeight(),
                BufferedImage.TYPE_INT_RGB);
        Graphics2D graphics = copiedImage.createGraphics();
        try {
            graphics.setColor(java.awt.Color.WHITE);
            graphics.fillRect(0, 0, copiedImage.getWidth(), copiedImage.getHeight());
            graphics.drawImage(sourceBufferedImage, 0, 0, null);
        } finally {
            graphics.dispose();
            sourceBufferedImage.flush();
        }

        File tempFile = new File(targetImage.getParentFile(), targetImage.getName() + ".tmp");
        try {
            ImageIO.write(copiedImage, "png", tempFile);
            java.nio.file.Files.move(
                    tempFile.toPath(),
                    targetImage.toPath(),
                    java.nio.file.StandardCopyOption.REPLACE_EXISTING,
                    java.nio.file.StandardCopyOption.ATOMIC_MOVE
            );
        } finally {
            if (tempFile.exists()) {
                tempFile.delete();
            }
            copiedImage.flush();
        }
    }

    private void writeRuntimeScanQrCodePreviewImage(File sourceImage, File targetImage) throws IOException {
        BufferedImage sourceBufferedImage = ImageIO.read(sourceImage);
        if (sourceBufferedImage == null) {
            throw new IOException("Unsupported image format: " + sourceImage.getAbsolutePath());
        }

        int[] qrBounds = detectQrBounds(sourceBufferedImage);
        BufferedImage previewImage = new BufferedImage(qrBounds[2], qrBounds[3], BufferedImage.TYPE_INT_RGB);
        Graphics2D graphics = previewImage.createGraphics();
        try {
            graphics.setColor(java.awt.Color.WHITE);
            graphics.fillRect(0, 0, qrBounds[2], qrBounds[3]);
            graphics.drawImage(sourceBufferedImage, 0, 0, qrBounds[2], qrBounds[3],
                    qrBounds[0], qrBounds[1], qrBounds[0] + qrBounds[2], qrBounds[1] + qrBounds[3], null);
        } finally {
            graphics.dispose();
            sourceBufferedImage.flush();
        }

        File tempFile = new File(targetImage.getParentFile(), targetImage.getName() + ".tmp");
        try {
            ImageIO.write(previewImage, "png", tempFile);
            java.nio.file.Files.move(
                    tempFile.toPath(),
                    targetImage.toPath(),
                    java.nio.file.StandardCopyOption.REPLACE_EXISTING
            );
        } finally {
            if (tempFile.exists()) {
                tempFile.delete();
            }
            previewImage.flush();
        }
    }

    private boolean isReadableImage(File file) {
        if (file == null || !file.exists() || !file.isFile() || file.length() <= 0) {
            return false;
        }
        try {
            BufferedImage image = ImageIO.read(file);
            if (image != null) {
                image.flush();
                return true;
            }
        } catch (IOException ignored) {
        }
        return false;
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
                (int) Math.round(image.getHeight() * 0.34)
        );
        cropSize = Math.max(100, cropSize);
        cropSize = Math.min(cropSize, Math.min(image.getWidth() - cropX, image.getHeight() - cropY));
        return new int[] { cropX, cropY, cropSize, cropSize };
    }
    public static String getEnvName() {
        String baseUrl = InjiVerifyConfigManager.getproperty("injiverify");
        String host = URI.create(baseUrl).getHost();
        String[] parts = host.split("\\.");

        logger.info("--- ApplnURI --- {}", BaseTestCase.ApplnURI);
        BaseTestCase.ApplnURI = System.getProperty("env.endpoint");

        String envName = "";
        if (parts.length >= 3) {
            envName = parts[1];
        }

        return envName;
    }

}
