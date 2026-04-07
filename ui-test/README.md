# Inji Verify UI Test

UI automation suite for Inji Verify web using Selenium, Cucumber, TestNG, and Extent Reports.

## Overview

This project supports:

- Local Chrome execution
- BrowserStack desktop browser execution
- IDE execution through `runnerfiles.Runner`
- JAR execution for Linux/container environments such as Rancher
- Parallel scenario execution
- Runtime generation of QR/image/video test data for scan and upload scenarios

## Tech Stack

- Java 21
- Maven 3.6+
- Selenium 4
- Cucumber 7
- TestNG
- Extent Reports
- BrowserStack

## Project Entry Points

- Main runner: [`runnerfiles.Runner`](src/main/java/runnerfiles/Runner.java)
- Base test setup: [`BaseTest.java`](src/main/java/utils/BaseTest.java)
- Local/scan media setup: [`BaseTestUtil.java`](src/main/java/utils/BaseTestUtil.java)
- Main step definitions: [`StepDef.java`](src/main/java/stepdefinitions/StepDef.java)
- Scan steps: [`ScanQrCodeSteps.java`](src/main/java/stepdefinitions/ScanQrCodeSteps.java)

## Prerequisites

- JDK 21
- Maven 3.6 or later
- Google Chrome installed for local execution
- BrowserStack account for cloud execution

## Configuration

Update the following files before execution.

### 1. Main environment config

File: [`injiVerify.properties`](src/main/resources/config/injiVerify.properties)

Typical properties:

```properties
apiEnvUser=<api env user>
apiInternalEndPoint=<api internal endpoint>
injiverify=<inji verify url>
injiweb=<inji web url>
InsuranceUrl=<insurance registry url>
actuatorMimotoEndpoint=<mimoto actuator url>
eSignetbaseurl=<esignet url>
stayProtectedIssuer=<issuer display name>
stayProtectedIssuerCredentialType=<credential type display name>
browserstack_username=<browserstack username>
browserstack_access_key=<browserstack access key>
runOnBrowserStack=<true|false>
browserstack_max_sessions=<number>
```

### 2. Test data config

File: [`config.properties`](src/test/resources/config.properties)

Typical values:

```properties
issuerSearchText=National Identity Department (Released)
issuerSearchTextforSunbird=StayProtected Insurance
```

Update these according to the target environment labels shown in UI.

### 3. BrowserStack config

File: [`browserstack.yml`](browserstack.yml)

Update:

- credentials if required by your setup
- platform/browser entries
- build/session metadata if needed

## Execution Modes

### Local Chrome

Use local execution when:

- testing scan scenarios
- testing offline/local CDP scenarios
- debugging from IDE

Current behavior:

- scan scenarios run locally
- BrowserStack is not used for `@scan`
- scenarios tagged `@withoutBrowserstack` always stay local

### BrowserStack

Use BrowserStack for upload and normal browser validation scenarios.

Current behavior:

- BrowserStack is used only when `runOnBrowserStack=true`
- scenarios tagged `@withoutBrowserstack` stay local
- scan scenarios are forced local by framework logic

## Running From IDE

Use [`runnerfiles.Runner`](src/main/java/runnerfiles/Runner.java) as the main class.

### IntelliJ / Eclipse setup

1. Import the project as a Maven project.
2. Set JDK to 21.
3. Run `Maven -> Reload Project` or equivalent.
4. Create a Java run configuration with main class `runnerfiles.Runner`.

### Recommended VM options

For a normal IDE run:

```text
-Dmodules=ui-test
-Denv.user=api-internal.dev
-Denv.endpoint=https://api-internal.dev.mosip.net
-Denv.testLevel=smokeAndRegression
```

Notes:

- Local IDE execution runs Chrome in headed mode.
- If launched outside IDE, local Chrome runs headless by default.
- BrowserStack credentials for this repo are read from [`injiVerify.properties`](src/main/resources/config/injiVerify.properties), not from mandatory VM args.

## Running With Maven

From the `ui-test` directory:

Compile only:

```powershell
mvn -q -DskipTests compile
```

Run all tests:

```powershell
mvn test
```

Run by tag:

```powershell
mvn test -Dcucumber.filter.tags="@offlineUpload"
```

Examples:

```powershell
mvn test -Dcucumber.filter.tags="@offlineUpload and @negative"
mvn test -Dcucumber.filter.tags="@scan and @camera_8mp"
mvn test -Dcucumber.filter.tags="@verifyuploadBoundaryMaxSizeqrcode"
```

Run by scenario name:

```powershell
mvn test -Dcucumber.filter.name="Verify upload qr code when internet is unavailable"
```

## Running As JAR

Build:

```powershell
mvn clean package -DskipTests
```

The shaded jar uses `runnerfiles.Runner` as main class.

Example:

```powershell
java -Dmodules=ui-test ^
     -Denv.user=api-internal.dev ^
     -Denv.endpoint=https://api-internal.dev.mosip.net ^
     -Denv.testLevel=smokeAndRegression ^
     -jar target\\<generated-jar-name>.jar
```

With BrowserStack:

```powershell
java -Dmodules=ui-test ^
     -Denv.user=api-internal.dev ^
     -Denv.endpoint=https://api-internal.dev.mosip.net ^
     -Denv.testLevel=smokeAndRegression ^
     -jar target\\<generated-jar-name>.jar
```

## Running In Rancher

Rancher typically runs the JAR in a Linux container.

Important current behavior:

- when no explicit feature path is set, the runner sets `cucumber.features` to `/home/inji/featurefiles/` on Linux
- this is handled in [`Runner.updateFeaturesPath()`](src/main/java/runnerfiles/Runner.java)

### Recommended Rancher/container requirements

- mount or copy feature files to `/home/inji/featurefiles/` if you rely on external feature path
- provide all required `-Denv.*` properties
- provide BrowserStack credentials only if BrowserStack execution is required
- ensure Chrome dependencies exist only if local browser execution is intended inside container

### Typical Rancher command

```bash
java \
  -Dmodules=ui-test \
  -Denv.user=api-internal.dev \
  -Denv.endpoint=https://api-internal.dev.mosip.net \
  -Denv.testLevel=smokeAndRegression \
  -jar /app/uitest-injiverify.jar
```

With BrowserStack:

```bash
java \
  -Dmodules=ui-test \
  -Denv.user=api-internal.dev \
  -Denv.endpoint=https://api-internal.dev.mosip.net \
  -Denv.testLevel=smokeAndRegression \
  -jar /app/uitest-injiverify.jar
```

## Runtime Test Data Generation

This suite now generates several assets during execution instead of depending only on committed files.

### Upload runtime files

Generated under:

[`test-output/runtime-media`](test-output/runtime-media)

Examples:

- `QRCode_10KB.jpg`
- `QRCode_5MB.png`

These are generated from insurance credential runtime artifacts.

### Scan runtime files

Generated under:

[`test-output/runtime-media`](test-output/runtime-media)

Examples:

- `ScanQrCode-runtime.png`
- `ScanQrCode-runtime-preview.png`
- `ScanQrCode-runtime-preview_png-8mp-runtime.y4m`
- `ScanQrCode-runtime-preview_png-15mp-runtime.y4m`

### Shared insurance credential artifacts

Generated once per run under:

[`test-output/runtime-media/shared-insurance-artifacts`](test-output/runtime-media/shared-insurance-artifacts)

Examples:

- `InsuranceCredential.pdf`
- `InsuranceCredential0.png`
- `InsuranceCredential0.jpg`
- `InsuranceCredential0.jpeg`

These are reused by upload and scan scenarios that depend on insurance QR content.

## Tags And Execution Notes

Examples of important tags:

- `@withoutBrowserstack`: always local
- `@scan`: scan-mode scenario
- `@qr_valid`, `@qr_invalid`, `@qr_expired`, `@qr_half`
- `@camera_2mp`, `@camera_8mp`, `@camera_15mp`, `@camera_low_light`
- `@offlineUpload`

Important notes:

- BrowserStack desktop sessions do not support offline network simulation on Windows/Mac
- offline upload/scan scenarios should remain local unless explicitly reworked
- scan fake-camera input is generated as Y4M at runtime

## Reports

Primary outputs:

- Extent report: [`test-output`](test-output)
- Cucumber HTML/JSON: [`target`](target)
- HTML report folder: [`reports`](reports)

Current report behavior:

- local failed scenarios attach screenshots
- BrowserStack failed scenarios attach a clickable BrowserStack video/session link
- known issues from [`Known_Issues.txt`](src/main/resources/config/Known_Issues.txt) are marked as skipped-known-issue

## Known Issues Support

Known issues are loaded from:

[`Known_Issues.txt`](src/main/resources/config/Known_Issues.txt)

Format:

```text
BUGID------Scenario Name
```

Example:

```text
INJI-123------Verify upload qr code when internet is unavailable
```

## Troubleshooting

### BrowserStack file upload issue

If native file chooser behavior is inconsistent on BrowserStack:

- the framework uploads through the file input directly
- it does not rely on validating OS-native file chooser windows

### Missing scan runtime media

Check:

- `test-output/runtime-media`
- shared insurance artifacts were generated
- the scenario is using the expected scan tag

### No screenshot in report

Current expected behavior:

- local failure -> screenshot should be attached
- BrowserStack failure -> BrowserStack video/session link should be attached

If not, verify the failure happened after browser/driver initialization.

### Single-scenario debugging

Use:

```powershell
mvn test -Dcucumber.filter.name="Exact scenario name"
```

or:

```powershell
mvn test -Dcucumber.filter.tags="@tagName"
```