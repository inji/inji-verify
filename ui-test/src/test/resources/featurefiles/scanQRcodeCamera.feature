Feature: Inji Verify scan qr code camera testing

  Background:
    Given User gets the title of the page
    Then Validate the title of the page
    When click on scan qr code tab

  @smoke @verifyScanValidQrCode @withoutBrowserstack @scan @qr_valid @needsInsuranceArtifacts
  Scenario: Verify the scan for valid qr code
    And click on scan qr code button
    Then validate that QR code is scanned successfully
    And validate the step 3 after scanning the QR code
    And validate the step 4 after scanning the QR code

  @smoke @verifyFirstTimeScanQrCodePermissionPrompt @withoutBrowserstack @scan @qr_valid @needsInsuranceArtifacts
  Scenario: Verify first time scan qr code click triggers camera permission flow
    And reset camera access for scan qr code to prompt
    And click on scan qr code button
    Then validate camera permission prompt flow is triggered for scan qr code

  @smoke @verifyScanQrCodeWithAllowedCameraAccess @withoutBrowserstack @scan @qr_valid @needsInsuranceArtifacts
  Scenario: Verify scan qr code works without denied popup when camera access is allowed
    And allow camera access for scan qr code
    And click on scan qr code button
    Then validate scan qr code works without denied popup when camera access is allowed

  @negative @verifyScanHalfQrCode @withoutBrowserstack @scan @qr_half
  Scenario: Verify the scan for half qr code
    And click on scan qr code button
    Then validate the error message for half QR code
    And Verify close button on timeout message
    And click on close button on timeout message
    Then validate timeout message disappears within five seconds

  @negative @verifyScanInvalidQrCode @withoutBrowserstack @scan @qr_invalid
  Scenario: Verify the scan for invalid qr code
    And click on scan qr code button
    Then validate the error message for invalid QR code
    And validate scan another qr code option is visible after invalid scan

  @negative @verifyScanQrCodeWithVisualEffect @withoutBrowserstack @scan @qr_visual_effect
  Scenario: Verify the scan does not work for qr code with visual effects applied
    And click on scan qr code button
    Then validate scan does not work for visually modified qr code

  @negative @verifyExpiredScanQrCode @withoutBrowserstack @scan @qr_expired
  Scenario: Verify the scan for expired qr code
    And click on scan qr code button
    Then validate the error message for expired QR code

  @negative @verifyScanQrCodeCameraAccessDeniedAndRecover @withoutBrowserstack @scan @camera_denied @qr_valid @needsInsuranceArtifacts
  Scenario: Verify denied camera access popup and user can access camera again after dismissing it
    And deny camera access for scan qr code
    And click on scan qr code button
    Then validate the camera access denied popup
    When verify click on okay button
    And allow camera access for scan qr code
    And click on scan qr code button
    Then validate camera can be accessed again for scan qr code

  @smoke @verifyScanValidQrCode8Mp @withoutBrowserstack @scan @qr_valid @camera_8mp @needsInsuranceArtifacts
  Scenario: Verify the scan feature with camera 8mp
    And click on scan qr code button
    Then validate that QR code is scanned successfully

  @smoke @verifyScanValidQrCode15Mp @withoutBrowserstack @scan @qr_valid @camera_15mp @needsInsuranceArtifacts
  Scenario: Verify the scan feature with camera 15mp
    And click on scan qr code button
    Then validate that QR code is scanned successfully

  @smoke @verifyScanValidQrCodeAt2Mp @withoutBrowserstack @scan @qr_valid @camera_2mp @needsInsuranceArtifacts
  Scenario: Verify the scan feature with camera resolution of 2mp
    And click on scan qr code button
    Then validate that QR code is scanned successfully

  @smoke @verifyScanValidQrCodeInLowLight @withoutBrowserstack @scan @qr_valid @camera_low_light @needsInsuranceArtifacts
  Scenario: Verify the scan for valid qr code in low light
    And click on scan qr code button
    Then validate that QR code is scanned successfully

  @negative @offlineScan @withoutBrowserstack @scan @qr_valid @needsInsuranceArtifacts
  Scenario: Verify the scan for valid qr code when internet is unavailable
    And turn off internet connection
    And click on scan qr code button
    Then validate the error message for no internet connection
    And validate that try again button is visible
    When click on try again button
    Then validate the error message for no internet connection

  @smoke @withoutBrowserstack @scan @qr_valid @browser_back_navigation @needsInsuranceArtifacts
  Scenario: Verify browser back from scan page returns user to upload screen
    And click on scan qr code button
    When Click browser back button
    Then Verify that Upload button visible
