Feature: Inji Verify scan qr code page testing

  @smoke @verifyscanqrcodepage
  Scenario: Verify the scan qr code page flow
    Given User gets the title of the page
    Then Validate the title of the page
    And click on scan qr code tab
    And Verify scan qr code step1 label
    And Verify scan qr code step1 description
    And Verify scan qr code step2 label
    And Verify scan qr code step2 description
    And Verify scan qr code step3 label
    And Verify scan qr code step3 description
    And verify scan qr code area
    And verify scan qr code icon
    And verify scan qr code button
    And click on scan qr code button
    And Verify scan qr code step2 label after
    And verify scan qr code button
    And Click on Home button
    And Verify that Upload button visible
    And click on scan qr code tab
    And Click on vp verification tab
    And click on scan qr code tab
    And verify click on scan qr code button
