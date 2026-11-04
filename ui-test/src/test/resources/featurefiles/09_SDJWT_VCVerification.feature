Feature: Inji SD-JWT VC verification testing

  @negative @verifySDJWTwithExpiredCertificate
  Scenario: Verify SD-JWT QR code with expired certificate shows invalid credential message
    And Verify browser refresh
    And Verify upload QR code step2 label
    And Verify upload QR code step3 label
    And Verify that Upload button visible
    And Upload SD-JWT QR code with expired certificate
    And Verify Error message for invalid QR code

  @negative @verifySDJWTwithIncorrectDigestValue
  Scenario: Verify SD-JWT QR code with incorrect digest value shows invalid credential message
    And Verify browser refresh
    And Verify upload QR code step2 label
    And Verify upload QR code step3 label
    And Verify that Upload button visible
    And Upload SD-JWT QR code with incorrect digest value
    And Verify Error message for invalid QR code

  @negative @verifySDJWTwithIncorrectDisclosureValue
  Scenario: Verify SD-JWT QR code with incorrect disclosure value shows invalid credential message
    And Verify browser refresh
    And Verify upload QR code step2 label
    And Verify upload QR code step3 label
    And Verify that Upload button visible
    And Upload SD-JWT QR code with incorrect disclosure value
    And Verify Error message for invalid QR code

  @negative @verifySDJWTwithIncorrectSignature
  Scenario: Verify SD-JWT QR code with incorrect signature shows invalid credential message
    And Verify browser refresh
    And Verify upload QR code step2 label
    And Verify upload QR code step3 label
    And Verify that Upload button visible
    And Upload SD-JWT QR code with incorrect signature
    And Verify Error message for invalid QR code

  @negative @verifySDJWTwithInvalidDigestLength
  Scenario: Verify SD-JWT QR code with invalid digest length shows invalid credential message
    And Verify browser refresh
    And Verify upload QR code step2 label
    And Verify upload QR code step3 label
    And Verify that Upload button visible
    And Upload SD-JWT QR code with invalid digest length
    And Verify Error message for invalid QR code

  @negative @verifySDJWTwithMalformedQRCode
  Scenario: Verify SD-JWT malformed QR code shows invalid credential message
    And Verify browser refresh
    And Verify upload QR code step2 label
    And Verify upload QR code step3 label
    And Verify that Upload button visible
    And Upload SD-JWT QR code with malformed QR code
    And Verify Error message for invalid QR code

  @negative @verifySDJWTwithNBFsetToFuture
  Scenario: Verify SD-JWT QR code with NBF set to future shows invalid credential message
    And Verify browser refresh
    And Verify upload QR code step2 label
    And Verify upload QR code step3 label
    And Verify that Upload button visible
    And Upload SD-JWT QR code with NBF set to future
    And Verify Error message for invalid QR code

  @negative @verifySDJWTwithUnsupportedAlgorithm
  Scenario: Verify SD-JWT QR code with unsupported algorithm shows invalid credential message
    And Verify browser refresh
    And Verify upload QR code step2 label
    And Verify upload QR code step3 label
    And Verify that Upload button visible
    And Upload SD-JWT QR code with unsupported algorithm
    And Verify Error message for invalid QR code

  @negative @verifySDJWTwithMissingDigest
  Scenario: Verify SD-JWT QR code with missing digest shows invalid credential message
    And Verify browser refresh
    And Verify upload QR code step2 label
    And Verify upload QR code step3 label
    And Verify that Upload button visible
    And Upload SD-JWT QR code with missing digest
    And Verify Error message for invalid QR code

  @smoke @verifySDJWTwithSelectiveDisclosure
  Scenario: Verify SD-JWT QR code with selective disclosure shows firstname and hides lastname
    And Verify browser refresh
    And Verify upload QR code step2 label
    And Verify upload QR code step3 label
    And Verify that Upload button visible
    And Upload SD-JWT QR code with selective disclosure
    And verify congratulations message on successful verification
    And Verify SD-JWT firstname field displays value 'John'
    And Verify SD-JWT lastname field is not visible
