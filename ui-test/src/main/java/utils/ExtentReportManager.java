package utils;

import com.aventstack.extentreports.ExtentReports;
import com.aventstack.extentreports.ExtentTest;
import com.aventstack.extentreports.reporter.ExtentHtmlReporter;
import com.aventstack.extentreports.reporter.configuration.Theme;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.net.URI;
import java.net.URISyntaxException;

import api.InjiVerifyConfigManager;

public class ExtentReportManager {
    private static ExtentReports extent;
    private static final ThreadLocal<ExtentTest> testHolder = new ThreadLocal<>();
    private static long startTime;

    // ── Scenario counters ─────────────────────────────────────────────────────────
    private static int passedCount     = 0;
    private static int failedCount     = 0;
    private static int skippedCount    = 0;
    private static int knownIssueCount = 0;
    // ─────────────────────────────────────────────────────────────────────────────

    public static void initReport() {
        if (extent == null) {
            startTime = System.currentTimeMillis();

            ExtentHtmlReporter htmlReporter = new ExtentHtmlReporter("test-output/ExtentReport.html");
            htmlReporter.config().setTheme(Theme.DARK);
            htmlReporter.config().setDocumentTitle("Automation Report");

            try {
                String testUrl = BaseTest.url;
                String host = getHostFromUrl(testUrl);
                htmlReporter.config().setReportName(host);
            } catch (Exception e) {
                System.err.println("Could not set Report Name from URL: " + e.getMessage());
                htmlReporter.config().setReportName("Test Execution Report");
            }

            extent = new ExtentReports();
            extent.attachReporter(htmlReporter);

            addSystemInfo();
        }
    }

    private static void addSystemInfo() {
        String branch   = getGitBranch();
        String commitId = getGitCommitId();

        if (extent != null) {
            if (branch != null)   extent.setSystemInfo("Git Branch",    branch);
            if (commitId != null) extent.setSystemInfo("Git Commit ID", commitId);

            try {
                extent.setSystemInfo("Test URL", BaseTest.url);
            } catch (Exception e) {
                System.err.println("Could not fetch Test URL: " + e.getMessage());
            }

            try {
                String dependentUrl = InjiVerifyConfigManager.getInjiWebUi();
                extent.setSystemInfo("Dependent URL", dependentUrl);
            } catch (Exception e) {
                System.err.println("Could not fetch Dependent URL: " + e.getMessage());
            }

            String startTimeStr = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss").format(new Date(startTime));
            extent.setSystemInfo("Execution Start Time", startTimeStr);
        }
    }

    public static void createTest(String testName) {
        testHolder.set(extent.createTest(testName));
    }

    public static void logStep(String message) {
        ExtentTest test = testHolder.get();
        if (test != null) {
            test.info(message);
        }
    }

    public static void flushReport() {
        if (extent != null) {
            extent.flush();
        }
    }

    public static ExtentTest getTest() {
        return testHolder.get();
    }

    // ── Counter incrementers ──────────────────────────────────────────────────────
    public static synchronized void incrementPassed() {
        passedCount++;
    }

    public static synchronized void incrementFailed() {
        failedCount++;
    }

    public static synchronized void incrementSkipped() {
        skippedCount++;
    }

    public static synchronized void incrementKnownIssue() {
        knownIssueCount++;
    }
    // ─────────────────────────────────────────────────────────────────────────────

    // ── Counter getters ───────────────────────────────────────────────────────────
    public static int getPassedCount() {
        return passedCount;
    }

    public static int getFailedCount() {
        return failedCount;
    }

    public static int getSkippedCount() {
        return skippedCount;
    }

    public static int getKnownIssueCount() {
        return knownIssueCount;
    }

    public static int getTotalCount() {
        return passedCount + failedCount + skippedCount + knownIssueCount;
    }
    // ─────────────────────────────────────────────────────────────────────────────

    private static String getGitBranch() {
        try {
            Process process = Runtime.getRuntime().exec("git rev-parse --abbrev-ref HEAD");
            BufferedReader reader = new BufferedReader(new InputStreamReader(process.getInputStream()));
            return reader.readLine();
        } catch (IOException | NullPointerException e) {
            System.err.println("Failed to get Git branch: " + e.getMessage());
            return null;
        }
    }

    private static String getGitCommitId() {
        try {
            Process process = Runtime.getRuntime().exec("git rev-parse HEAD");
            BufferedReader reader = new BufferedReader(new InputStreamReader(process.getInputStream()));
            return reader.readLine();
        } catch (IOException | NullPointerException e) {
            System.err.println("Failed to get Git commit ID: " + e.getMessage());
            return null;
        }
    }

    private static String getHostFromUrl(String url) {
        try {
            URI uri = new URI(url);
            return uri.getHost();
        } catch (URISyntaxException e) {
            System.err.println("Invalid URL: " + url);
            return url;
        }
    }
}
