package io.mosip.testrig.apirig.injiverify.testrunner;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Properties;

import org.apache.log4j.Level;
import org.apache.log4j.Logger;
import org.testng.TestNG;

import io.mosip.testrig.apirig.injiverify.utils.InjiVerifyConfigManager;
import io.mosip.testrig.apirig.injiverify.utils.InjiVerifyUtil;
import io.mosip.testrig.apirig.testrunner.BaseTestCase;
import io.mosip.testrig.apirig.testrunner.ExtractResource;
import io.mosip.testrig.apirig.testrunner.HealthChecker;
import io.mosip.testrig.apirig.utils.AdminTestUtil;
import io.mosip.testrig.apirig.utils.AuthTestsUtil;
import io.mosip.testrig.apirig.utils.CertsUtil;
import io.mosip.testrig.apirig.utils.DependencyResolver;
import io.mosip.testrig.apirig.utils.GlobalConstants;
import io.mosip.testrig.apirig.utils.GlobalMethods;
import io.mosip.testrig.apirig.utils.JWKKeyUtil;
import io.mosip.testrig.apirig.utils.KeyCloakUserAndAPIKeyGeneration;
import io.mosip.testrig.apirig.utils.MispPartnerAndLicenseKeyGeneration;
import io.mosip.testrig.apirig.utils.OutputValidationUtil;
import io.mosip.testrig.apirig.utils.PartnerRegistration;
import io.mosip.testrig.apirig.utils.SkipTestCaseHandler;

/**
 * Class to initiate mosip api test execution
 * 
 * @author Mohan
 *
 */
public class MosipTestRunner {
	private static final Logger LOGGER = Logger.getLogger(MosipTestRunner.class);
	private static String cachedPath = null;

	public static String jarUrl = MosipTestRunner.class.getProtectionDomain().getCodeSource().getLocation().getPath();
	public static List<String> languageList = new ArrayList<>();
	public static boolean skipAll = false;

	/**
	 * C Main method to start mosip test execution
	 * 
	 * @param arg
	 */
	public static void main(String[] arg) {

		try {

			LOGGER.info("** ------------- Started Inji Verify API Testrig--------------------------------- **");
			setLogLevels();
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
			SkipTestCaseHandler.loadTestcaseToBeSkippedList("testCaseSkippedList.txt");
			GlobalMethods.setModuleNameAndReCompilePattern(InjiVerifyConfigManager.getproperty("moduleNamePattern"));
			

				HealthChecker healthcheck = new HealthChecker();
				healthcheck.setCurrentRunningModule(BaseTestCase.currentModule);
				Thread trigger = new Thread(healthcheck);
				trigger.start();

			BaseTestCase.getLanguageList();
			
			String testCasesToExecuteString = InjiVerifyConfigManager.getproperty("testCasesToExecute");

			DependencyResolver.loadDependencies(getGlobalResourcePath() + "/" + "config/testCaseInterDependency.json");
			if (!testCasesToExecuteString.isBlank()) {
				InjiVerifyUtil.testCasesInRunScope = DependencyResolver.getDependencies(testCasesToExecuteString);
			}

			startTestRunner();
		} catch (Exception e) {
			LOGGER.error("Exception " + e.getMessage());
		}

		HealthChecker.bTerminate = true;
		
		// Used for generating the test case interdependency JSON file
		// AdminTestUtil.generateTestCaseInterDependencies(getGlobalResourcePath() + "/config/testCaseInterDependency.json");
		System.exit(0);

	}

	public static void suiteSetup(String runType) {
		if (InjiVerifyConfigManager.IsDebugEnabled())
			LOGGER.setLevel(Level.ALL);
		else
			LOGGER.info("Test Framework for Mosip api Initialized");
		BaseTestCase.initialize();
		LOGGER.info("Done with BeforeSuite and test case setup! su TEST EXECUTION!\n\n");

		if (!runType.equalsIgnoreCase("JAR")) {
			AuthTestsUtil.removeOldMosipTempTestResource();
		}
		BaseTestCase.currentModule = GlobalConstants.INJIVERIFY;
		BaseTestCase.certsForModule = GlobalConstants.INJIVERIFY;
		AdminTestUtil.copymoduleSpecificAndConfigFile(GlobalConstants.INJIVERIFY);
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

	/**
	 * The method to start mosip testng execution
	 * 
	 * @throws IOException
	 */
	public static void startTestRunner() {
		File homeDir = null;
		String os = System.getProperty("os.name");
		LOGGER.info(os);
		if (getRunType().contains("IDE") || os.toLowerCase().contains("windows")) {
			homeDir = new File(System.getProperty("user.dir") + "/testNgXmlFiles");
			LOGGER.info("IDE :" + homeDir);
		} else {
			File dir = new File(System.getProperty("user.dir"));
			homeDir = new File(dir.getParent() + "/mosip/testNgXmlFiles");
			LOGGER.info("ELSE :" + homeDir);
		}
		File[] files = homeDir.listFiles();
		if (files != null) {
			for (File file : files) {
				TestNG runner = new TestNG();
				List<String> suitefiles = new ArrayList<>();
				if (file.getName().toLowerCase().contains("mastertestsuite")) {
					BaseTestCase.setReportName(GlobalConstants.INJIVERIFY);
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

	/**
	 * The method to return class loader resource path
	 * 
	 * @return String
	 * @throws IOException
	 */

	public static String getGlobalResourcePath() {
		if (cachedPath != null) {
			return cachedPath;
		}

		String path = null;
		if (getRunType().equalsIgnoreCase("JAR")) {
			path = new File(jarUrl).getParentFile().getAbsolutePath() + "/MosipTestResource/MosipTemporaryTestResource";
		} else if (getRunType().equalsIgnoreCase("IDE")) {
			path = new File(MosipTestRunner.class.getClassLoader().getResource("").getPath()).getAbsolutePath()
					+ "/MosipTestResource/MosipTemporaryTestResource";
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

	public static Properties getproperty(String path) {
		Properties prop = new Properties();
		FileInputStream inputStream = null;
		try {
			File file = new File(path);
			inputStream = new FileInputStream(file);
			prop.load(inputStream);
		} catch (Exception e) {
			LOGGER.error(GlobalConstants.EXCEPTION_STRING_2 + e.getMessage());
		} finally {
			AdminTestUtil.closeInputStream(inputStream);
		}
		return prop;
	}

	/**
	 * The method will return mode of application started either from jar or eclipse
	 * ide
	 * 
	 * @return
	 */
	public static String getRunType() {
		if (MosipTestRunner.class.getResource("MosipTestRunner.class").getPath().contains(".jar"))
			return "JAR";
		else
			return "IDE";
	}

}