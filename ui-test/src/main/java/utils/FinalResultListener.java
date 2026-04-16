package utils;

import io.cucumber.testng.PickleWrapper;
import org.apache.log4j.Logger;
import org.testng.ISuite;
import org.testng.ISuiteListener;
import org.testng.ITestListener;
import org.testng.ITestResult;

import java.util.concurrent.atomic.AtomicInteger;

public class FinalResultListener implements ITestListener, ISuiteListener {

    private static final Logger LOGGER = Logger.getLogger(FinalResultListener.class);
    private static final AtomicInteger passedCount = new AtomicInteger(0);
    private static final AtomicInteger failedCount = new AtomicInteger(0);
    private static final AtomicInteger skippedCount = new AtomicInteger(0);
    private static final AtomicInteger totalCount = new AtomicInteger(0);

    @Override
    public void onStart(ISuite suite) {
        reset();
        BaseTest.clearScenarioHookStatuses();
    }

    @Override
    public void onTestSuccess(ITestResult result) {
        if (!isCucumberScenario(result)) {
            return;
        }
        passedCount.incrementAndGet();
        totalCount.incrementAndGet();
        logMismatch(result, "PASSED");
    }

    @Override
    public void onTestFailure(ITestResult result) {
        if (!isCucumberScenario(result)) {
            return;
        }
        failedCount.incrementAndGet();
        totalCount.incrementAndGet();
        logMismatch(result, "FAILED");
    }

    @Override
    public void onTestSkipped(ITestResult result) {
        if (!isCucumberScenario(result)) {
            return;
        }
        skippedCount.incrementAndGet();
        totalCount.incrementAndGet();
        logMismatch(result, "SKIPPED");
    }

    private void logMismatch(ITestResult result, String finalStatus) {
        String scenarioName = extractScenarioName(result);
        if (scenarioName == null) {
            return;
        }

        String hookStatus = BaseTest.getScenarioHookStatus(scenarioName);
        if (hookStatus != null && !hookStatus.equalsIgnoreCase(finalStatus)) {
            LOGGER.error("Scenario status mismatch detected. Scenario='" + scenarioName
                    + "', hookStatus='" + hookStatus
                    + "', finalTestNgStatus='" + finalStatus + "'.");
        }
    }

    private boolean isCucumberScenario(ITestResult result) {
        return result != null
                && result.getMethod() != null
                && "runScenario".equals(result.getMethod().getMethodName());
    }

    private String extractScenarioName(ITestResult result) {
        if (result == null || result.getParameters() == null || result.getParameters().length == 0) {
            return null;
        }
        Object firstParameter = result.getParameters()[0];
        if (firstParameter instanceof PickleWrapper) {
            return ((PickleWrapper) firstParameter).getPickle().getName();
        }
        return null;
    }

    private static void reset() {
        passedCount.set(0);
        failedCount.set(0);
        skippedCount.set(0);
        totalCount.set(0);
    }

    public static boolean hasRecordedResults() {
        return totalCount.get() > 0;
    }

    public static int getPassedCount() {
        return passedCount.get();
    }

    public static int getFailedCount() {
        return failedCount.get();
    }

    public static int getTotalCount() {
        return totalCount.get();
    }
}
