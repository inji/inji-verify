Feature: Inji Verify BLE verification testing

  @smoke @verifyingVpVerification
  Scenario: Verify the Inji Verify homepage

    Given User gets the title of the page
    Then Validate the title of the page
    And Click on ble tab
    And verify information message on ble verification