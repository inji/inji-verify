Feature: Inji Verify scan qr code camera testing

  Background:
    Given User gets the title of the page
    Then Validate the title of the page
    When click on scan qr code tab

  @smoke @verifyScanValidQrCode @withoutBrowserstack @scan @qr_valid @needsInsuranceArtifacts
  Scenario: Verify the scan for valid qr code
    And Get current URL of scan QR code tab
    And click on scan qr code button
    Then validate that QR code is scanned successfully
    And validate the step 3 after scanning the QR code
    And validate the step 4 after scanning the QR code
    And Verify user is still on the scan QR code page

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

  @negative @verifyScanQrCodeUnreadableErrorPopup @withoutBrowserstack @scan @qr_visual_effect
  Scenario: Verify the error popup appears when the QR code cannot be read after the camera is left idle
    Then verify scan qr code button is clickable
    And click on scan qr code button
    And wait for 60 seconds
    Then validate scan does not work for visually modified qr code
    And verify scan qr code button is clickable

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

  @negative @offlineScan @withoutBrowserstack @scan
  Scenario: Verify the scan for valid qr code when internet is unavailable
    And turn off internet connection
    And click on scan qr code button
    Then validate the error message for no internet connection
    And validate that try again button is visible
    When click on try again button
    Then validate the error message for no internet connection

  @smoke @withoutBrowserstack @scan @browser_back_navigation
  Scenario: Verify browser back from scan page returns user to upload screen
    And click on scan qr code button
    When Click browser back button
    Then Verify that Upload button visible

  @smoke @withoutBrowserstack @scan @back_button @qr_visual_effect
  Scenario: Verify back button navigates back to scan button
    And click on scan qr code button
    Then verify back button is visible
    And verify click on back button
    Then verify scan qr code button

  @smoke @verifyScanVcData @withoutBrowserstack @scan @qr_health_insurence @needsInsuranceArtifacts
  Scenario: Verify VC data is displayed correctly after scanning a valid QR code
    And click on scan qr code button
    Then validate that QR code is scanned successfully
    And verify tick icon is visible on successful verification
    And verify congratulations message on successful verification
    And verify full name matches expected value
    And verify policy number matches expected value
    And verify policy name matches expected value
    And verify policy issued on matches expected value
    And verify policy expires on matches expected value
    And verify benefits matches expected value
    And verify mobile matches expected value
    And verify email matches expected value
    And verify gender matches expected value
    And verify dob matches expected value
    And Verify verify another qr code button on successful verification

  @smoke @withoutBrowserstack @scan  @qr_valid @browser_back_during_scan @needsInsuranceArtifacts
  Scenario: Verify scan page resets cleanly after navigating back during validation and VC validates on re-scan
    And click on scan qr code button
    When Click browser back button
    When click on scan qr code tab
    Then verify scan qr code button is clickable
    And verify no VC result is visible on scan page
    And click on scan qr code button
    Then validate that QR code is scanned successfully
