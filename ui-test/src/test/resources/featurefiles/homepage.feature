Feature: Inji Verify homepage testing

  @smoke @verifyingHomepage
  Scenario: Verify the Inji Verify homepage
    Given User gets the title of the page
    Then Validate the title of the page
    And Verify that header is displayed
    And Verify that sub header is displayed
    And Verify that home button is displayed
    And Verify that Credentials button is displayed
    And Verify that Help button is displayed
    And Verify that expansion button is displayed before expansion
    And Verify click on Help button
    And Verify that links are valid under help
    And Verify minimize help option
    And Verify that upload QR Code tab is visible
    And Verify that scan QR Code tab is visible
    And Verify that VP Verification tab is visible
    And Verify that BLE tab is visible
    And Verify copyright text
    And Verify upload QR code step1 label
    And Verify upload QR code step1 description
    And Verify upload QR code step2 label
    And Verify upload QR code step2 description
    And Verify upload QR code step3 label
    And Verify upload QR code step3 label
    And Verify upload QR code step3 description
    And Verify that scan element is visible
    And Verify that Upload icon visible
    And Verify that Upload button visible
    And Verify file format constraints text