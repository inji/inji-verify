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
    And Verify click on FAQ button
    And Verify that FAQ info message is displayed
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

  @negative @verifyingHomePageWithWrongURL
  Scenario: Verify the Inji Verify with wrong URL
    Given User enters wrong URL in the address bar
    Then Verify error message for wrong URL

  @smoke @verifyingPageURLs
  Scenario: Verify that upload page, scan page, and VP verification page URLs are unique
    Given User gets the title of the page
    And Verify that Upload button visible
    And Get current URL of upload QR code tab
    And click on scan qr code tab
    And verify scan qr code button
    And Get current URL of scan QR code tab
    And Click on vp verification tab
    And verify request verifiable credentials button
    And Get current URL of VP verification tab
    Then Verify that upload scan and VP verification page URLs are unique

  @smoke @verifyingPageURLPersistence
  Scenario: Verify that each tab page persists after browser reload
    Given User gets the title of the page
    And Verify that Upload button visible
    And Reload the page
    And Verify that Upload button visible
    And click on scan qr code tab
    And verify scan qr code button
    And Reload the page
    And verify scan qr code button
    And Click on vp verification tab
    And verify request verifiable credentials button
    And Reload the page
    Then verify request verifiable credentials button

  @smoke @verifySeamlessNavigationFromUploadPage
  Scenario: Verify navigation from upload page via logo and home button is seamless without page reload
    Given User gets the title of the page
    And Verify that Upload button visible
    And Inject no-reload page sentinel
    And Click on Inji Verify logo
    And Verify page navigation was seamless without reload
    And Verify that Upload button visible
    And Inject no-reload page sentinel
    And Click on Home button
    And Verify page navigation was seamless without reload
    And Verify that Upload button visible

  @smoke @verifySeamlessNavigationFromVpPage
  Scenario: Verify navigation from VP verification page via logo and home button is seamless without page reload
    Given User gets the title of the page
    And Click on vp verification tab
    And Verify that VP Verification tab is visible
    And Inject no-reload page sentinel
    And Click on Inji Verify logo
    And Verify page navigation was seamless without reload
    And Verify that Upload button visible
    And Click on vp verification tab
    And Verify that VP Verification tab is visible
    And Inject no-reload page sentinel
    And Click on Home button
    And Verify page navigation was seamless without reload
    And Verify that Upload button visible

  @smoke @verifySeamlessNavigationFromScanPage
  Scenario: Verify navigation from scan page via logo and home button is seamless without page reload
    Given User gets the title of the page
    And click on scan qr code tab
    And verify scan qr code button
    And Inject no-reload page sentinel
    And Click on Inji Verify logo
    And Verify page navigation was seamless without reload
    And Verify that Upload button visible
    And click on scan qr code tab
    And verify scan qr code button
    And Inject no-reload page sentinel
    And Click on Home button
    And Verify page navigation was seamless without reload
    And Verify that Upload button visible

  @smoke @verifyHelpDropdownClosesOnOutsideClick
  Scenario: Verify help dropdown closes when clicking outside of it
    Given User gets the title of the page
    And Verify that Help button is displayed
    And Verify that expansion button is displayed before expansion
    And Verify click on Help button
    And Verify that expansion button is displayed after expansion
    And Click outside the help dropdown
    And Verify that expansion button is displayed before expansion
