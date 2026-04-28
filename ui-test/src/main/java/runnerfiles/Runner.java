package runnerfiles;

import api.InjiVerifyConfigManager;
import io.cucumber.junit.Cucumber;
import io.cucumber.testng.AbstractTestNGCucumberTests;
import io.cucumber.testng.CucumberOptions;
import io.cucumber.testng.CucumberOptions.SnippetType;
import io.cucumber.testng.FeatureWrapper;
import io.cucumber.testng.PickleWrapper;
import io.mosip.testrig.apirig.testrunner.BaseTestCase;
import io.mosip.testrig.apirig.testrunner.ExtractResource;
import io.mosip.testrig.apirig.utils.*;
import org.apache.log4j.Logger;
import org.junit.runner.RunWith;
import org.testng.ITestResult;
import org.testng.TestNG;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.DataProvider;
import org.testng.annotations.Test;

import java.io.BufferedReader;
import java.io.File;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@RunWith(Cucumber.class)
@CucumberOptions(
		features = {},
		dryRun = !true,
		glue = {"stepdefinitions", "utils"},
		snippets = SnippetType.CAMELCASE,
		monochrome = true,
		plugin = {"pretty",
				"html:reports",
				"html:target/cucumber.html", "json:target/cucumber.json",
				"summary","com.aventstack.extentreports.cucumber.adapter.ExtentCucumberAdapter:"}
)

public class Runner extends AbstractTestNGCucumberTests{

	private static final Logger LOGGER = Logger.getLogger(Runner.class);
	private static String cachedPath = null;

	public static String jarUrl = Runner.class.getProtectionDomain().getCodeSource().getLocation().getPath();
	public static List<String> languageList = new ArrayList<>();
	public static boolean skipAll = false;

	// ── Known Issues ──────────────────────────────────────────────────────────────
	public static Map<String, String> knownIssues = new ConcurrentHashMap<>();

	static {
		updateFeaturesPath();  
		loadKnownIssues(); 
	}
	// ─────────────────────────────────────────────────────────────────────────────


	public static void main(String[] args) {
		try {
			LOGGER.info("** ------------- Inji verify ui Run Started for prerequisite creation---------------------------- **");

			BaseTestCase.setRunContext(getRunType(), jarUrl);
			ExtractResource.removeOldMosipTestTestResource();
			if (getRunType().equalsIgnoreCase("JAR")) {
				ExtractResource.extractCommonResourceFromJar();
			} else {
				ExtractResource.copyCommonResources();
			}
			AdminTestUtil.init();
			InjiVerifyConfigManager.init();

			suiteSetup(getRunType());
			setLogLevels();

			 KeycloakUserManager.removeUser();
			 KeycloakUserManager.createUsers();
			 KeycloakUserManager.closeKeycloakInstance();
			 AdminTestUtil.getRequiredField();

			startTestRunner();
		} catch (Exception e) {
			LOGGER.error("Exception " + e.getMessage());
		}
		System.exit(0);
	}

	public static void suiteSetup(String runType) {
		BaseTestCase.initialize();
		utils.BaseTest.resetPrerequisiteState();
		LOGGER.info("Done with BeforeSuite and test case setup! su TEST EXECUTION!\n\n");

		if (!runType.equalsIgnoreCase("JAR")) {
			AuthTestsUtil.removeOldMosipTempTestResource();
		}

		BaseTestCase.currentModule = "injiverify";
		BaseTestCase.certsForModule = "injiverify";
		BaseTestCase.copymoduleSpecificAndConfigFile("injiverify");
	}

	public static void startTestRunner() {
		File homeDir = null;
		String os = System.getProperty("os.name");
		LOGGER.info(os);
		if (getRunType().contains("IDE") || os.toLowerCase().contains("windows")) {
			homeDir = new File(System.getProperty("user.dir") + "/testNgXmlFiles");
			LOGGER.info("IDE :" + homeDir);
		} else {
			File dir = new File(System.getProperty("user.dir"));
			homeDir = new File(dir.getParent() + "/inji/testNgXmlFiles");
			LOGGER.info("ELSE :" + homeDir);
		}
		File[] files = homeDir.listFiles();
		if (files != null) {
			for (File file : files) {
				TestNG runner = new TestNG();
				List<String> suitefiles = new ArrayList<>();
				if (file.getName().toLowerCase().contains("mastertestsuite")) {
					BaseTestCase.setReportName("injiweb");
					suitefiles.add(file.getAbsolutePath());
					runner.setTestSuites(suitefiles);
					System.getProperties().setProperty("testng.outpur.dir", "testng-report");
					runner.setOutputDirectory("testng-report");
					runner.run();
				}
			}
		} else {
			LOGGER.error("No files found in directory: " + homeDir);
		}
	}

	public static String getRunType() {
		if (Runner.class.getResource("Runner.class").getPath().contains(".jar"))
			return "JAR";
		else
			return "IDE";
	}

	@Override
	@DataProvider(parallel = true)
	public Object[][] scenarios() {
		Object[][] scenarios = super.scenarios();
		System.out.println("Number of scenarios provided: " + scenarios.length);

		for (Object[] scenario : scenarios) {
			if (scenario.length > 0 && scenario[0] instanceof PickleWrapper) {
				System.out.println("Scenario Name: " + ((PickleWrapper) scenario[0]).getPickle().getName());
			} else {
				System.out.println("Scenario data is not as expected!");
			}
		}

		return scenarios;
	}

	@BeforeMethod
	public void setTestName(ITestResult result) {
		result.getMethod().setDescription("Running Scenario: " + result.getMethod().getMethodName());
	}

	@Test(dataProvider = "scenarios")
	public void runScenario(PickleWrapper pickle, FeatureWrapper feature) {
		System.out.println("Running Scenario: " + pickle.getPickle().getName());
		Thread.currentThread().setName(pickle.getPickle().getName());
		super.runScenario(pickle, feature);
	}

	// ── Known Issues ──────────────────────────────────────────────────────────────
	/**
	 * Resets all scenario counters between runs (e.g. multi-language loops).
	 * knownIssueCount is kept in BaseTest alongside the other counters.
	 */
	public static void resetCounters() {
		utils.BaseTest.passedCount.set(0);
		utils.BaseTest.failedCount.set(0);
		utils.BaseTest.totalCount.set(0);
		utils.BaseTest.knownIssueCount.set(0);
	}

	/**
	 * Loads known issues from {@code src/test/resources/Known_Issues.txt}.
	 *
	 * <p>Each non-blank, non-comment line must follow the format:
	 * <pre>
	 *   BUGID------Scenario Name
	 * </pre>
	 * Lines starting with {@code #} are treated as comments and skipped.
	 */
	private static void loadKnownIssues() {
		InputStream knownIssuesStream = Runner.class.getClassLoader()
				.getResourceAsStream("config/Known_Issues.txt");

		if (knownIssuesStream == null) {
			LOGGER.warn("Known issues file not found in classpath: config/Known_Issues.txt");
			return;
		}

		try (BufferedReader br = new BufferedReader(
				new InputStreamReader(knownIssuesStream, StandardCharsets.UTF_8))) {

			String line;
			while ((line = br.readLine()) != null) {
				line = line.trim();

				// Skip blank lines and comments
				if (line.isEmpty() || line.startsWith("#") || !line.contains("------")) {
					continue;
				}

				String[] parts = line.split("------", 2);
				if (parts.length == 2) {
					String bugId        = parts[0].trim();
					String scenarioName = parts[1].trim().replaceAll("\\s+", " ");
					knownIssues.put(scenarioName, bugId);
				}
			}

			LOGGER.info("Known Issues Loaded: " + knownIssues);

		} catch (Exception e) {
			LOGGER.warn("Error reading Known_Issues.txt: " + e.getMessage());
		}
	}
	// ─────────────────────────────────────────────────────────────────────────────


	public static String getGlobalResourcePath() {
		if (cachedPath != null) {
			return cachedPath;
		}

		String path = null;
		if (getRunType().equalsIgnoreCase("JAR")) {
			path = new File(jarUrl).getParentFile().getAbsolutePath() + "/MosipTestResource/MosipTemporaryTestResource";
		} else if (getRunType().equalsIgnoreCase("IDE")) {
			path = new File(Runner.class.getClassLoader().getResource("").getPath()).getAbsolutePath()
					+ "/";
			if (path.contains(GlobalConstants.TESTCLASSES))
				path = path.replace(GlobalConstants.TESTCLASSES, "classes");
		}

		if (path != null) {
			cachedPath = path;
			return path;
		} else {
			return "Global Resource File Path Not Found";
		}
	}

	public static String getResourcePath() {
		return getGlobalResourcePath();
	}

	private static void setLogLevels() {
		AdminTestUtil.setLogLevel();
		OutputValidationUtil.setLogLevel();
		PartnerRegistration.setLogLevel();
		KeyCloakUserAndAPIKeyGeneration.setLogLevel();
		MispPartnerAndLicenseKeyGeneration.setLogLevel();
		JWKKeyUtil.setLogLevel();
		CertsUtil.setLogLevel();
	}

	   public static void updateFeaturesPath() {
        String existingFeatures = System.getProperty("cucumber.features");
        if (existingFeatures != null && !existingFeatures.trim().isEmpty()) {
            LOGGER.info("cucumber.features already set by caller, skipping override: " + existingFeatures);
            return;
        }

        CucumberOptions cucumberOptions = Runner.class.getAnnotation(CucumberOptions.class);

        if (cucumberOptions != null) {
            String[] annotatedFeatures = cucumberOptions.features();
            if (annotatedFeatures != null) {
                for (String feature : annotatedFeatures) {
                    if (feature != null && !feature.trim().isEmpty()) {
                        LOGGER.info("Using @CucumberOptions feature path: " + feature.trim());
                        return;
                    }
                }
            }
        }

        String os = System.getProperty("os.name").toLowerCase();
        String featuresPath = os.contains("windows")
                ? "src\\test\\resources\\featurefiles\\"
                : "/home/inji/featurefiles/";

        System.setProperty("cucumber.features", featuresPath);
        LOGGER.info("No feature path in @CucumberOptions. cucumber.features set to: " + featuresPath);
    }
}

