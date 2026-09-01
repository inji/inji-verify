Feature: Inji Verify vpVerification testing

  @smoke @verifyingVpVerification @prerequisiteVP
  Scenario: Verify OVP verification using inji-web wallet 
    When Open inji web in new tab
    And User performs token-based login to inji-web wallet
    And User unlocks inji-web wallet with configured passcode
    And User confirms inji-web wallet with configured passcode
    #And User click on submit button
    #And User opens the inji-web issuer catalog
    Then User search the issuers sunbird
    When User click on StayProtected Insurance credentials button
    When User click on health insurance by e-signet button
    #And User click on validity dropdown
    #And User click on no limit
    #And User click on proceed
    And User enter the policy number
    And User enter the full name
    And User enter the date of birth
    And User click on login button
    #Then User verify Download Success text displayed
    #And User verifies health insurance credential is stored in inji-web wallet
    And User switches back to inji-verify tab
    Given User gets the title of the page
    #Then Validate the title of the page
    #And Click on right arrow
    And Click on vp verification tab
    And Verify click on request verifiable credentials button
    And Uncheck credential "mosipId"
    And Select credential "healthInsurance"
    And Credential "healthInsurance" is selected and credential "mosipId" is not selected
    And Verify Click on open wallet button
    And User selects configured inji-web wallet in verifier
    And User clicks on Proceed button
    #And User is redirected to inji-web wallet presentation page
    And User click on trust verifier
    And User selects the credential for verification
    And User clicks consent and share button
    And VP result is posted back to inji-verify successfully

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
    And Uncheck credential "mosipId"
    And Select credential "healthInsurance"
    And Select credential "landRegistry"
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
    And Select credential "sdJwt"
    And Verify Click on Generate QR Code button
    And Verify QR code generated
    And Click on vp verification tab
    And Verify click on request verifiable credentials button
    And Verify Verifiable Credential Panel label
    And Select credential "healthInsurance"
    And Verify Click on Generate QR Code button
    And Verify QR code generated
    
    Examples:
      | credential type |
      | SD JWT     |
	  
	
	@smoke @verifyingVpVerificationNegative @dependsOnVP
  Scenario: Verify VP verification error when no matching credentials found
    Given User gets the title of the page
    And Click on vp verification tab
    And Verify click on request verifiable credentials button
    And Verify Click on open wallet button
    And User selects configured inji-web wallet in verifier
    And User clicks on Proceed button
    #And User is redirected to inji-web wallet presentation page
    And User performs token-based login to inji-web wallet
    And User unlocks inji-web wallet with configured passcode
    And User click on submit button
    And Then Verify error message is displayed
	
	@smoke @verifyingVpVerification @dependsOnVP
    Scenario: Verify multiple VC OVP verification using inji-web wallet 
    When Open inji web in new tab
    And User performs token-based login to inji-web wallet
    And User unlocks inji-web wallet with configured passcode
    #And User confirms inji-web wallet with configured passcode
    And User click on submit button
    And User opens the inji-web issuer catalog
    Then User search the issuers sunbird
    When User click on StayProtected Insurance credentials button
    When User click on health insurance by e-signet button
    And User enter the policy number
    And User enter the full name
    And User enter the date of birth
    And User click on login button
    And User Click on home button
    Then User search the issuers sunbird
    When User click on StayProtected Insurance credentials button
    When User click on health insurance by e-signet button
    And User enter the policy number
    And User enter the full name
    And User enter the date of birth
    And User click on login button
    And User switches back to inji-verify tab
    Given User gets the title of the page
    Then Click on vp verification tab
    And Verify click on request verifiable credentials button
    And Uncheck credential "mosipId"
    And Select credential "healthInsurance"
    And Credential "healthInsurance" is selected and credential "mosipId" is not selected
    And Verify Click on open wallet button
    And User selects configured inji-web wallet in verifier
    And User clicks on Proceed button
    #And User is redirected to inji-web wallet presentation page
    #And User click on trust verifier
    And User selects the credential for verification
    And User select the second credential for verification
    And User clicks consent and share button
    And VP result is posted back to inji-verify successfully

  @smoke @verifyingVpVerification @dependsOnVP
  Scenario: Verify partial sharing in OVP verification using inji-web wallet 
    When Open inji web in new tab
    And User performs token-based login to inji-web wallet
    And User unlocks inji-web wallet with configured passcode
    #And User confirms inji-web wallet with configured passcode
    And User click on submit button
    And User opens the inji-web issuer catalog
    Then User search the issuers sunbird
    When User click on StayProtected Insurance credentials button
    When User click on health insurance by e-signet button
    And User enter the policy number
    And User enter the full name
    And User enter the date of birth
    And User click on login button
    #And User verifies health insurance credential is stored in inji-web wallet
    And User switches back to inji-verify tab
    Given User gets the title of the page
    Then Click on vp verification tab
    And Verify click on request verifiable credentials button
    And Select credential "healthInsurance"
    #And Credential "healthInsurance" is selected and credential "mosipId" is not selected
    And Verify Click on open wallet button
    And User selects configured inji-web wallet in verifier
    And User clicks on Proceed button
    And User click on trust verifier
    And User selects the credential for verification
    And User clicks consent and share button
    And VP result for partial sharing

  @smoke @verifyingVpVerification @credentialAutoSelect
  Scenario: Verify essential credential from config.json is auto selected
    Given User gets the title of the page
    Then Validate the title of the page
    And Click on vp verification tab
    And Verify click on request verifiable credentials button
    And Verify Verifiable Credential Panel label
    Then Essential credential from config is auto selected
    And Non essential credentials from config are not auto selected

  @smoke @verifyingVpVerification @credentialAutoSelect
  Scenario: Verify QR code generation with auto selected essential credential
    Given User gets the title of the page
    Then Validate the title of the page
    And Click on vp verification tab
    And Verify click on request verifiable credentials button
    And Verify Verifiable Credential Panel label
    Then Essential credential from config is auto selected
    And Verify Click on Generate QR Code button
    And Verify QR code generated

  @smoke @verifyingVpVerification @credentialAutoSelect
  Scenario: Verify user can override auto selected credential from config.json
    Given User gets the title of the page
    Then Validate the title of the page
    And Click on vp verification tab
    And Verify click on request verifiable credentials button
    And Verify Verifiable Credential Panel label
    Then Essential credential from config is auto selected
    And Uncheck credential "mosipId"
    And Select credential "healthInsurance"
    And Credential "healthInsurance" is selected and credential "mosipId" is not selected
    And Verify Click on Generate QR Code button
    And Verify QR code generated

  @smoke @verifyingVpVerification @credentialAutoSelect
  Scenario: Verify multi credential selection on top of auto selected essential credential
    Given User gets the title of the page
    Then Validate the title of the page
    And Click on vp verification tab
    And Verify click on request verifiable credentials button
    And Verify Verifiable Credential Panel label
    Then Essential credential from config is auto selected
    And Select credential "healthInsurance"
    And Select credential "landRegistry"
    And Select credential "lifeInsurance"
    And Verify Click on Generate QR Code button
    And Verify QR code generated

