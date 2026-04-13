# Inji Verify UI Test

UI automation suite for Inji Verify web flows. The suite uses Selenium, Cucumber, TestNG, Extent Reports, BrowserStack, and MOSIP API test-rig utilities.

## What This Project Covers

- Local Chrome execution in headed or headless mode
- BrowserStack desktop browser execution for supported scenarios
- Cucumber feature execution through TestNG
- IDE execution through `runnerfiles.Runner`
- Executable shaded JAR execution for Linux/container environments such as Rancher
- Pre-requisite and post-requisite API setup through TestNG XML suites
- Runtime QR/image/video test data generation for upload and scan scenarios
- Known-issue mapping in reports

## Tech Stack

| Tool | Version |
|------|---------|
| Java | 21 |
| Maven | 3.6+ |
| Selenium | 4.17.0 |
| Cucumber | 7.x |
| TestNG | Provided by dependencies |
| Extent Reports | 5.1.0 |
| BrowserStack SDK | 1.31.1 |

## Project Structure

```text
ui-test/
|-- src/
|   |-- main/
|   |   |-- java/
|   |   |   |-- runnerfiles/Runner.java        # Main entry point and TestNG/Cucumber runner
|   |   |   |-- stepdefinitions/               # Cucumber step definitions
|   |   |   |-- pages/                         # Page objects
|   |   |   |-- utils/                         # Driver setup, reports, wait/screenshot utilities
|   |   |   `-- api/                           # Inji Verify config/API helpers
|   |   `-- resources/
|   |       `-- config/
|   |           |-- injiVerify.properties      # Environment, browser, BrowserStack, and test data config
|   |           `-- Known_Issues.txt           # Known issue mappings
|   `-- test/
|       `-- resources/
|           |-- cucumber.properties
|           |-- extent.properties
|           |-- featurefiles/                  # Cucumber feature files
|           `-- QRCodes/                       # Static QR test assets
|-- testNgXmlFiles/                            # TestNG suite files
|-- browserstack.yml                           # BrowserStack SDK config
|-- Dockerfile
|-- entrypoint.sh
|-- pom.xml
`-- README.md
```

## Main Entry Points

- Runner: `src/main/java/runnerfiles/Runner.java`
- TestNG master suite: `testNgXmlFiles/MasterTestSuite.xml`
- Cucumber suite: `testNgXmlFiles/TestNg.xml`
- Environment & test data config: `src/main/resources/config/injiVerify.properties`
- Feature files: `src/test/resources/featurefiles/`
- Known issues: `src/main/resources/config/Known_Issues.txt`

## Quick Start

### First-Time Setup

1. **Clone the repository and navigate to ui-test:**
   ```bash
   cd ui-test
   ```

2. **Update configuration** - Edit `src/main/resources/config/injiVerify.properties`:
   - Set your environment URLs (injiverify, injiweb, etc.)
   - Set issuer names to match your target UI
   - Add BrowserStack credentials if running on cloud
   - Configure issuer search text values for test data

3. **Build the project:**
   ```bash
   mvn clean compile
   ```

4. **Run tests from IDE or Maven:**
   - **From IntelliJ/Eclipse/VS Code:** Run `runnerfiles.Runner` as the main class
   - **From Maven:** `mvn -DskipTests=false -Dsurefire.suiteXmlFiles=testNgXmlFiles/TestNg.xml test`

5. **View test reports:**
   - Extent HTML Report: `test-output/SparkReport/index.html`
   - TestNG Report: `testng-report/index.html`
   - Screenshots: `screenshots/FailureScreenshot_*.png`

## Prerequisites

- JDK 21
- Maven 3.6 or later
- Google Chrome for local browser execution
- BrowserStack account for cloud execution, if `runOnBrowserStack=true`
- Access to the configured MOSIP/Inji environment and required secrets

## Configuration

Update environment-specific values before running the suite. Do not commit real credentials.

### Main Environment Config

File: `src/main/resources/config/injiVerify.properties`

Important properties:

```properties
# Target environment URLs
apiEnvUser=api-internal.released
apiInternalEndPoint=https://api-internal.released.mosip.net
injiverify=https://injiverify.dev-int-inji.mosip.net/
injiweb=https://injiweb.dev-int-inji.mosip.net/issuers
InsuranceUrl=https://registry.released.mosip.net/api/v1/Insurance
actuatorMimotoEndpoint=/v1/mimoto/actuator/env
eSignetbaseurl=https://esignet-mosipid.released.mosip.net

# Issuer display names. These must match labels in the target UI.
stayProtectedIssuer=StayProtected Insurance
stayProtectedIssuerCredentialType=Health Insurance

# Browser and execution settings
explicitWaitTimeout=30
runOnBrowserStack=true
headless=true
scanVerificationTimeoutMultiplier=6

# Parallel execution
browserstack_max_sessions=5
parallel_thread_count=4

# BrowserStack credentials
browserstack_username=
browserstack_access_key=
```

Notes:

- `runOnBrowserStack=false` forces local Chrome where framework logic permits it.
- `headless=false` is useful for local debugging.
- `scanVerificationTimeoutMultiplier` multiplies `explicitWaitTimeout` for scan result waiting.
- Runtime media generation is thread-safe and includes retries for intermittent Windows file move failures.
- Keep `parallel_thread_count` conservative when running scan scenarios because large Y4M files are generated at runtime.

### Issuer Search Text Configuration

Issuer search text properties are now configured directly in `injiVerify.properties`:

```properties
issuerSearchText=National Identity Department (Released)
issuerSearchTextforSunbird=StayProtected Insurance
```

Update these values to match the issuer labels shown in the target environment UI. These properties are accessed via `InjiVerifyConfigManager.getIssuerSearchText()` and `InjiVerifyConfigManager.getIssuerSearchTextForSunbird()`.

### Configuration Priority and Environment Variables

BrowserStack credentials can be provided through multiple sources with the following priority:

1. **BrowserStack SDK Environment Variables** (highest priority - recommended for CI/CD):
   ```bash
   export BROWSERSTACK_USERNAME=your_username
   export BROWSERSTACK_ACCESS_KEY=your_access_key
   java -jar uitest-injiverify.jar
   ```
   These environment variables are read by the BrowserStack SDK directly from `browserstack.yml`.

2. **System properties** (medium priority):
   ```bash
   java -DbrowserstackUsername=your_username -DbrowserstackAccessKey=your_access_key -jar uitest-injiverify.jar
   ```

3. **injiVerify.properties file** (lowest priority - for local development):
   ```properties
   browserstack_username=your_username
   browserstack_access_key=your_access_key
   ```
   - Default values are loaded from `src/main/resources/config/injiVerify.properties`
   - Updates to this file require recompilation to take effect in JAR

**Important Notes:**
- `browserstack.yml` file (used by BrowserStack SDK) looks for environment variables: `BROWSERSTACK_USERNAME` and `BROWSERSTACK_ACCESS_KEY` (uppercase)
- `injiVerify.properties` file uses lowercase property names: `browserstack_username` and `browserstack_access_key`
- For **CI/CD pipelines and Docker containers**, use environment variables `BROWSERSTACK_USERNAME` and `BROWSERSTACK_ACCESS_KEY`
- For **local development**, update the properties in `injiVerify.properties`
- Never commit real credentials to version control

**Best Practice:** Use environment variables in CI/CD pipelines and do not commit sensitive data to version control.

### BrowserStack Config

File: `browserstack.yml`

Update these values as needed:

- `userName` and `accessKey`, or provide `BROWSERSTACK_USERNAME` and `BROWSERSTACK_ACCESS_KEY` as environment variables
- `projectName`, `buildName`, and `buildIdentifier`
- `platforms`
- `parallelsPerPlatform`
- `browserstackLocal`
- debug, network log, console log, and observability settings

## Execution Model

The primary executable entry point is `runnerfiles.Runner`.

When `Runner.main()` starts, it:

1. Initializes MOSIP test-rig resources.
2. Initializes Inji Verify config.
3. Creates required users and prerequisite data.
4. Runs `testNgXmlFiles/MasterTestSuite.xml` through TestNG.
5. Executes pre-requisite API tests, Cucumber UI scenarios, and post-requisite cleanup.

The Maven Surefire plugin is configured with `skipTests=true` in `pom.xml`, so plain `mvn test` does not run the suite unless you explicitly override that property.

## Important Notes

### Configuration Management

- **Centralized Configuration**: All environment-specific settings and test data are centralized in `injiVerify.properties`
- **ConfigManager Pattern**: Properties are accessed through `InjiVerifyConfigManager` for type-safe access
- **No File I/O**: Direct file reading is eliminated; use ConfigManager methods instead
- **Docker Ready**: Configuration is automatically available in Docker containers via volume mounts

### Java Version and Compatibility

- The project requires **Java 21**
- Ensure your JDK matches this version to avoid compatibility issues
- The Docker image uses `eclipse-temurin:21-jdk`

### Parallel Execution and Performance

- **Thread Safety**: Runtime media generation is thread-safe with 5-attempt retry for file operations
- **Large File Handling**: Scan scenarios generate Y4M files (8MP ≈ 170MB, 15MP ≈ 660MB per scenario)
- **Recommended Thread Count**:
  - 8 threads for suites with scan scenarios
  - 5 threads for BrowserStack-only or non-scan scenarios
- **Disk Space**: Ensure several gigabytes of free disk space when running scan scenarios in parallel

### BrowserStack Sessions

- Default limit: 5 concurrent sessions
- If more threads are configured, excess threads execute locally
- Adjust `browserstack_max_sessions` in `injiVerify.properties` if your plan supports more

### Credentials and Security

- **Never commit credentials** to version control
- Use environment variables for sensitive data in CI/CD pipelines and Docker containers

**BrowserStack Credentials:**
  - Environment Variables (recommended for CI/CD): `BROWSERSTACK_USERNAME`, `BROWSERSTACK_ACCESS_KEY` (uppercase, used by BrowserStack SDK)
  - Property File (for local development): `browserstack_username`, `browserstack_access_key` in `injiVerify.properties` (lowercase)
  - `browserstack.yml` file expects environment variables: `BROWSERSTACK_USERNAME` and `BROWSERSTACK_ACCESS_KEY`

**Other Credentials:**
  - Database passwords: Add as properties in `injiVerify.properties` with corresponding environment variable overrides
  - API credentials: Set via environment variables with corresponding property names
  - Property file shows empty placeholders that must be filled before running

**Example - Docker with Environment Variables:**
```bash
docker run \
  -e BROWSERSTACK_USERNAME=your_username \
  -e BROWSERSTACK_ACCESS_KEY=your_access_key \
  inji-verify-ui-test:latest
```

**Example - Local Development:**
Edit `src/main/resources/config/injiVerify.properties`:
```properties
browserstack_username=your_username
browserstack_access_key=your_access_key
```

## Running From IntelliJ IDEA

1. Open the `ui-test` directory as a Maven project.
2. Set the project SDK to JDK 21.
3. Reload Maven dependencies.
4. Create an Application run configuration:

| Field | Value |
|-------|-------|
| Name | `Inji Verify UI Tests` |
| Module classpath | `uitest-injiverify` or the detected project module |
| Main class | `runnerfiles.Runner` |

5. Add VM options if your environment requires them:

```text
-Dmodules=ui-test
-Denv.user=api-internal.released
-Denv.endpoint=https://api-internal.released.mosip.net
-Denv.testLevel=smokeAndRegression
```

6. Run the configuration.

Notes:

- IDE runs are treated as `IDE` run type by `Runner.getRunType()`.
- On Windows and IDE runs, the runner loads TestNG XML files from `testNgXmlFiles/` under the project directory.
- BrowserStack credentials are read from `injiVerify.properties` and/or BrowserStack SDK environment variables, depending on your setup.

## Running From Eclipse

1. Import the project using `File > Import > Maven > Existing Maven Projects`.
2. Select the `ui-test` directory.
3. Ensure the JRE System Library is JDK 21.
4. Run `Maven > Update Project` if dependencies are not resolved.
5. Create a Java Application run configuration for `src/main/java/runnerfiles/Runner.java`.
6. Use `runnerfiles.Runner` as the main class.
7. Add the same VM options shown in the IntelliJ section if required.

## Running From VS Code

1. Install `Extension Pack for Java`.
2. Install a Cucumber/Gherkin extension if you want feature-file syntax highlighting.
3. Open the `ui-test` folder.
4. Confirm JDK 21 is selected using `Java: Configure Java Runtime`.
5. Create `.vscode/launch.json` if needed:

```json
{
  "version": "0.2.0",
  "configurations": [
    {
      "type": "java",
      "name": "Inji Verify UI Tests",
      "request": "launch",
      "mainClass": "runnerfiles.Runner",
      "projectName": "uitest-injiverify",
      "vmArgs": "-Dmodules=ui-test -Denv.user=api-internal.released -Denv.endpoint=https://api-internal.released.mosip.net -Denv.testLevel=smokeAndRegression"
    }
  ]
}
```

## Running With Maven

Compile only:

```powershell
mvn -q -DskipTests compile
```

Package the executable shaded JAR:

```powershell
mvn clean package -DskipTests
```

Run TestNG suites through Maven Surefire by overriding the configured skip flag:

```powershell
mvn -DskipTests=false -Dsurefire.suiteXmlFiles=testNgXmlFiles/MasterTestSuite.xml test
```

Run only the Cucumber suite through TestNG:

```powershell
mvn -DskipTests=false -Dsurefire.suiteXmlFiles=testNgXmlFiles/TestNg.xml test
```

Filter Cucumber scenarios by tag:

```powershell
mvn -DskipTests=false -Dsurefire.suiteXmlFiles=testNgXmlFiles/TestNg.xml -Dcucumber.filter.tags="@offlineUpload" test
```

Filter by scenario name:

```powershell
mvn -DskipTests=false -Dsurefire.suiteXmlFiles=testNgXmlFiles/TestNg.xml -Dcucumber.filter.name="Verify upload qr code when internet is unavailable" test
```

## Running As JAR

Build:

```powershell
mvn clean package -DskipTests
```

Run:

```powershell
java -Dmodules=ui-test ^
     -Denv.user=api-internal.released ^
     -Denv.endpoint=https://api-internal.released.mosip.net ^
     -Denv.testLevel=smokeAndRegression ^
     -jar target\<generated-jar-name>.jar
```

The shaded JAR uses `runnerfiles.Runner` as the main class.

## Running In Rancher Or Linux Containers

The Dockerfile copies these runtime inputs into `/home/inji/`:

- `target/`
- generated JAR from `target/*.jar`
- `application.properties`
- `browserstack.yml`
- `src/main/resources/` (includes injiVerify.properties and Known_Issues.txt)
- `src/test/resources/featurefiles/`
- `testNgXmlFiles/`
- QR/static resource folders used by the suite

Important runtime behavior:

- If `cucumber.features` is not set, `Runner.updateFeaturesPath()` sets it to `/home/inji/featurefiles/` on Linux.
- The TestNG XML directory is resolved relative to the container layout for non-Windows, non-IDE runs.
- Chrome and browser dependencies are needed only if local browser execution is intended inside the container.
- BrowserStack credentials are needed only for BrowserStack execution.
- All configuration including issuer search text is loaded from `injiVerify.properties`.

Typical command:

```bash
java \
  -Dmodules=ui-test \
  -Denv.user=api-internal.released \
  -Denv.endpoint=https://api-internal.released.mosip.net \
  -Denv.testLevel=smokeAndRegression \
  -jar /home/inji/<generated-jar-name>.jar
```

## Execution Modes

### Local Chrome

Use local Chrome for:

- scan scenarios
- offline/local CDP scenarios
- headed debugging from an IDE
- scenarios tagged with `@withoutBrowserstack`

Current framework behavior:

- Scan scenarios are forced to local execution.
- `@withoutBrowserstack` scenarios stay local.
- IDE runs use headed Chrome behavior where framework logic enforces it.
- JAR/CI runs generally respect the `headless` property.

### BrowserStack

Use BrowserStack for upload and normal browser validation scenarios.

Current framework behavior:

- BrowserStack is used only when `runOnBrowserStack=true` and the scenario is eligible for BrowserStack execution.
- Scan scenarios are not run on BrowserStack.
- `@withoutBrowserstack` scenarios are not run on BrowserStack.
- BrowserStack desktop sessions do not support every local-only behavior, such as some offline network simulation cases.

## Runtime Test Data Generation

The suite generates runtime artifacts under `test-output/runtime-media`.

Upload runtime files include:

- `QRCode_10KB.jpg`
- `QRCode_5MB.png`

Scan runtime files include:

- `ScanQrCode-runtime.png`
- `ScanQrCode-runtime-preview.png`
- `ScanQrCode-runtime-preview_png-8mp-runtime.y4m`
- `ScanQrCode-runtime-preview_png-15mp-runtime.y4m`

Shared insurance credential artifacts are generated under `test-output/runtime-media/shared-insurance-artifacts`:

- `InsuranceCredential.pdf`
- `InsuranceCredential0.png`
- `InsuranceCredential0.jpg`
- `InsuranceCredential0.jpeg`

Notes:

- Runtime media generation includes a 5-attempt retry mechanism for file move operations to handle intermittent Windows `AccessDeniedException` during parallel execution.
- 15MP Y4M files are approximately 660 MB per scenario.
- 8MP Y4M files are approximately 170 MB per scenario.
- Keep enough free disk space when running scan scenarios in parallel.

## Tags And Execution Notes

| Tag | Description |
|-----|-------------|
| `@withoutBrowserstack` | Always runs locally, never on BrowserStack |
| `@scan` | Scan-mode scenario, always local |
| `@qr_valid` | Valid QR code scenario |
| `@qr_invalid` | Invalid QR code scenario |
| `@qr_expired` | Expired QR code scenario |
| `@qr_half` | Partial/half QR code scenario |
| `@camera_2mp` | 2MP fake camera resolution |
| `@camera_8mp` | 8MP fake camera resolution |
| `@camera_15mp` | 15MP fake camera resolution |
| `@camera_low_light` | Low-light camera simulation |
| `@offlineUpload` | Upload flow without internet |

Use tag filters with `cucumber.filter.tags`, for example:

```powershell
mvn -DskipTests=false -Dsurefire.suiteXmlFiles=testNgXmlFiles/TestNg.xml -Dcucumber.filter.tags="@scan and @camera_8mp" test
```

## Reports And Outputs

| Output | Location |
|--------|----------|
| Extent Spark config | `src/test/resources/extent.properties` |
| Extent output folder | `test-output/` |
| Extent screenshots | `test-output/SparkReport/screenshots/` |
| Cucumber HTML folder | `reports` |
| Cucumber HTML/JSON | `target/cucumber.html`, `target/cucumber.json` |
| TestNG report | `testng-report/` |
| Runtime media | `test-output/runtime-media/` |
| Local screenshots | `screenshots/` |

Report behavior:

- Local failed scenarios attach screenshots where browser initialization has completed.
- BrowserStack failed scenarios attach a clickable BrowserStack video/session link where available.
- Known issues from `Known_Issues.txt` are marked as `skipped-known-issue`.

## Known Issues Support

Known issues are loaded from:

```text
src/main/resources/config/Known_Issues.txt
```

Format:

```text
BUGID------Scenario Name
```

Example:

```text
INJI-123------Verify upload qr code when internet is unavailable
```

Rules:

- Blank lines are ignored.
- Lines starting with `#` are ignored.
- Scenario names are normalized for whitespace before matching.

## Troubleshooting

### Plain `mvn test` Does Not Run Tests

`pom.xml` configures Surefire with `skipTests=true`. Use one of these commands instead:

```powershell
mvn -DskipTests=false -Dsurefire.suiteXmlFiles=testNgXmlFiles/MasterTestSuite.xml test
```

```powershell
mvn -DskipTests=false -Dsurefire.suiteXmlFiles=testNgXmlFiles/TestNg.xml test
```

### BrowserStack File Upload Issue

The framework uploads through the file input element directly instead of relying on OS-native file chooser window validation.

### Missing Scan Runtime Media

Check that:

- `test-output/runtime-media` exists after the run.
- Shared insurance artifacts were generated successfully.
- The scenario is using the expected scan/camera tags.
- There is enough free disk space for generated Y4M files.

### Intermittent Camera Access Denied Popup

The framework includes a one-time recovery mechanism if the "Camera access denied" popup appears unexpectedly despite permissions being granted. The test refreshes the page and retries scan initiation once before failing.

### No Screenshot In Report

Expected behavior:

- Local failure: screenshot should be attached.
- BrowserStack failure: BrowserStack video/session link should be attached where available.

If an attachment is missing, verify the failure occurred after browser/driver initialization.

### IntelliJ Dependencies Not Resolved

Open the Maven tool window and run `Reload All Maven Projects`.

### Eclipse Red Markers After Import

Run `Maven > Update Project` and enable `Force Update of Snapshots/Releases` if needed.

### VS Code Imports Not Resolving

Run `Java: Clean Java Language Server Workspace` from the Command Palette, then reload VS Code.

### Headless Mode Issues

- Set `headless=false` in `injiVerify.properties` for local debugging.
- Keep `headless=true` for JAR/CI execution unless visual debugging is required.
- Scan scenarios may need local browser/camera-specific setup regardless of the global BrowserStack flag.

