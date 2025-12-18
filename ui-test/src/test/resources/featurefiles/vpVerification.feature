Feature: Inji Verify vpVerification testing

  @smoke @verifyingVpVerification
  Scenario Outline: Verify the VP verification panel
    Given User gets the title of the page
    Then Validate the title of the page
    And Click on vp verification tab
    And Verify VP verification qr code step1 description
    And Verify VP verification qr code step1 label
    And Verify VP verification qr code step2 label
    And Verify VP verification qr code step2 description
    And Verify VP verification qr code step3 label
    And Verify VP verification qr code step3 description
    And Verify VP verification qr code step4 label
    And Verify VP verification qr code step4 description
    And Verify click on request verifiable credentials button
    And Verify Verifiable Credential Panel label
    And Verify click sort by button
    And Verify click Sort AtoZ button
    And Verify click sort by button
    And Verify click Sort ZtoA button
    And User enter the credential type "<credential type>"
    And Verify click Back button

    Examples:
      | credential type |
      | life            |


  @smoke @verifyingVpVerification
  Scenario: Verify if the VP verification QR code is generated

    Given User gets the title of the page
    Then Validate the title of the page
    And Click on vp verification tab
    And Verify click on request verifiable credentials button
    And Verify Verifiable Credential Panel label
    And Verify Click on Generate QR Code button
    And Verify QR code generated
#    And Click on vp verification tab
#    And Verify QR code is not precent
    And Click on vp verification tab
    And Verify click on request verifiable credentials button
    And Verify Verifiable Credential Panel label
    And Uncheck MOSIP ID
    And Select Health Insurance
    And Select Land Registry
    And Verify Click on Generate QR Code button
    And Verify QR code generated
    And Click on vp verification tab

@mobileView @verifyingVpVerification
Scenario: Verify VP verification same device flow

    Given User gets the title of the page
    Then Validate the title of the page
    And Click on right arrow
    And Click on vp verification tab
    And Verify click on request verifiable credentials button
    And Verify Click on open wallet button
    #Below lines are commented as the selecting wallet flow is descoped for the release
#    And Verify Click on cancel
#    And verify Transaction Terminated error message
#    And Verify click on request verifiable credentials button
#    And Verify Click on open wallet button
#    And Verify Click on wallet
#    And Verify Click on Proceed
    And verify loading screen

  @smoke @verifyingVpVerification
  Scenario Outline: Verify the VP verification QR code is generated

    Given User gets the title of the page
    Then Validate the title of the page
    And Click on vp verification tab
    And Verify click on request verifiable credentials button
    And Verify Verifiable Credential Panel label
    And User enter the credential type "<credential type>"
    And Select SD JWT VC
    And Verify Click on Generate QR Code button
    And Verify QR code generated
    And Click on vp verification tab
    And Verify click on request verifiable credentials button
    And Verify Verifiable Credential Panel label
    And Select Health Insurance
    And Verify Click on Generate QR Code button
    And Verify QR code generated
    
    Examples:
      | credential type |
      | SD JWT     |