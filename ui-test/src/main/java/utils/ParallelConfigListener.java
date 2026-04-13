package utils;

import org.apache.log4j.Logger;
import org.testng.IAlterSuiteListener;
import org.testng.xml.XmlSuite;

import java.io.InputStream;
import java.util.List;
import java.util.Properties;

public class ParallelConfigListener implements IAlterSuiteListener {

    private static final Logger LOGGER = Logger.getLogger(ParallelConfigListener.class);

    private static final String CUCUMBER_SUITE_NAME = "Cucumber Test Suite";

    @Override
    public void alter(List<XmlSuite> suites) {
        int threadCount = readThreadCount();
        for (XmlSuite suite : suites) {
            if (CUCUMBER_SUITE_NAME.equals(suite.getName())) {
                suite.setDataProviderThreadCount(threadCount);
                LOGGER.info("Parallel thread count set to " + threadCount + " for suite: " + suite.getName());
            } else {
                LOGGER.info("Skipping parallel config for suite: " + suite.getName());
            }
        }
    }

    private int readThreadCount() {
        try (InputStream is = getClass().getClassLoader()
                .getResourceAsStream("config/injiVerify.properties")) {
            if (is != null) {
                Properties props = new Properties();
                props.load(is);
                String value = props.getProperty("parallel_thread_count", "8").trim();
                return Integer.parseInt(value);
            }
        } catch (Exception e) {
            LOGGER.warn("Could not read parallel_thread_count from injiVerify.properties, using default 8: " + e.getMessage());
        }
        return 8;
    }
}