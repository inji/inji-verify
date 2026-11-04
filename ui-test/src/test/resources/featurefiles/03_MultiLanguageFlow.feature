Feature: Inji Verify Multilanguage testing

  @smoke @verifyingMultiLanguage
  Scenario: Verify the Multilanguage VC verification
    And Verify browser refresh
    And Verify upload QR code step2 label
    And Verify upload QR code step3 label
    And Verify that Upload button visible
    And Verify Upload multilanguage VC
    And Verify message for valid QR code
    And Verify click on language dropdown
    And Verify select language "French"
    And Verify click on language dropdown
    Then Verify "French" language is highlighted
    And Verify click on language dropdown
    And Verify if name value is present in french
    And Verify if gender value is present in french
    And Verify congratulations message in french
    And Verify click on language dropdown
    And Verify select language "Arabic"
    And Verify click on language dropdown
    Then Verify "Arabic" language is highlighted
    And Verify click on language dropdown
    And Verify if name value is present in arabic
    And Verify if gender value is present in arabic
    And Verify congratulations message in arabic
    And Verify click on language dropdown
    And Verify select language "French"
    And Verify click on language dropdown
    Then Verify "French" language is highlighted
    And Verify click on language dropdown
    And Verify if name value is present in french
    And Verify if gender value is present in french
    And Verify congratulations message in french
    And Verify click on language dropdown
    And Verify select language "English"
    And Verify click on language dropdown
    Then Verify "English" language is highlighted
    And Verify click on language dropdown
    And Verify if name value is present in english
    And Verify if gender value is present in english
    And Verify congratulations message in english

  @smoke @verifyingMultiLanguage
  Scenario: Verify VC details fall back to English when Spanish is selected
    And Verify browser refresh
    And Verify that Upload button visible
    And Verify Upload multilanguage VC
    And Verify message for valid QR code
    And Verify click on language dropdown
    And Verify select language "Spanish"
    And Verify if name value is present in english
    And Verify if gender value is present in english
    And Verify congratulations message in spanish

  @negative @offlineScan @withoutBrowserstack @verifyingMultiLanguage
  Scenario: Verify no internet message and try again button appear in French on scan page
    And click on scan qr code tab
    And turn off internet connection
    And click on scan qr code button
    Then validate the error message for no internet connection
    And validate that try again button is visible
    And Verify click on language dropdown
    And Verify select language "French"
    Then validate the error message for no internet connection in french
    And validate that try again button text is in french

  @smoke @verifyButtonLayoutMultiLanguage
  Scenario: Verify button layout adapts for French, Arabic and Spanish across all pages without text overflow
    Given User gets the title of the page
    And Verify that Upload button visible
    And Verify click on language dropdown
    And Verify select language "French"
    And Verify all buttons fit their layout without text overflow
    And Verify click on language dropdown
    And Verify select language "Arabic"
    And Verify all buttons fit their layout without text overflow
    And Verify click on language dropdown
    And Verify select language "Spanish"
    And Verify all buttons fit their layout without text overflow
    And click on scan qr code tab
    And Verify click on language dropdown
    And Verify select language "French"
    And Verify all buttons fit their layout without text overflow
    And Verify click on language dropdown
    And Verify select language "Arabic"
    And Verify all buttons fit their layout without text overflow
    And Verify click on language dropdown
    And Verify select language "Spanish"
    And Verify all buttons fit their layout without text overflow
    And Click on vp verification tab
    And Verify click on language dropdown
    And Verify select language "French"
    And Verify all buttons fit their layout without text overflow
    And Verify click on language dropdown
    And Verify select language "Arabic"
    And Verify all buttons fit their layout without text overflow
    And Verify click on language dropdown
    And Verify select language "Spanish"
    And Verify all buttons fit their layout without text overflow

  @smoke @verifyingMultiLanguage
  Scenario: Verify blur QR code error message is shown in the selected language
    And Verify browser refresh
    And Verify upload QR code step2 label
    And Verify upload QR code step3 label
    And Verify that Upload button visible
    And Upload blur QR code file
    And Verify small or blur alert message
    And Verify click on language dropdown
    And Verify select language "French"
    And Verify that Upload button visible
    And Upload blur QR code file
    And Verify small or blur alert message in "French"
    And Verify click on language dropdown
    And Verify select language "Arabic"
    And Verify that Upload button visible
    And Upload blur QR code file
    And Verify small or blur alert message in "Arabic"
    And Verify click on language dropdown
    And Verify select language "Spanish"
    And Verify that Upload button visible
    And Upload blur QR code file
    And Verify small or blur alert message in "Spanish"

  @smoke @verifyingMultiLanguage
  Scenario: Verify English fallback when an invalid language code is set
    When selecting an invalid language
    And Verify that header is displayed
    And Verify that sub header is displayed
    And Verify that upload QR Code tab is visible
    And Verify that scan QR Code tab is visible
    And Verify upload QR code step1 label
    And Verify upload QR code step2 label
    And Verify upload QR code step3 label
    And Verify that Upload button visible
    And Verify Upload multilanguage VC
    And Verify message for valid QR code
    And Verify if name value is present in english
    And Verify if gender value is present in english
    And Verify congratulations message in english

