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
    And Verify select french language
    And Verify if name value is present in french
    And Verify if gender value is present in french
    And Verify click on language dropdown
    And Verify select arabic language
    And Verify if name value is present in arabic
    And Verify if gender value is present in arabic
    And Verify click on language dropdown
    And Verify select french language
    And Verify if name value is present in french
    And Verify if gender value is present in french


