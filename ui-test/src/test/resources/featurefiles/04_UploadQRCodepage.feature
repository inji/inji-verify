Feature: Inji Verify upload qr code testing

  @smoke @verifyUploadFileControl
  Scenario: Verify upload file control is available
    And Verify that Upload button visible
    And Verify upload file input is present and enabled

  @smoke @verifyuploadqrcode @needsInsuranceArtifacts
  Scenario Outline: Verify upload qr code with png
    Given User gets the title of the page
    Then Validate the title of the page
    And Verify that upload QR Code tab is visible
    And Get current URL of upload QR code tab
    And Verify upload QR code step2 label
    And Verify upload QR code step3 label
    And Verify that Upload button visible
    And Upload QR code file png
    And verify upload QR code step2 description after
    And verify upload QR code step3 description after
    And verify policy issued on value
    And verify policy issued on date is in human readable format
    And verify policy expires on value
    And verify policy expires on date is in human readable format
    And verify full name value
    And verify tick icon is visible on successful verification
    And verify congratulations message on successful verification
    And Verify user is still on the upload QR code page
    And Verify verify another qr code button on successful verification
    And Verify click on another qr code button
    And Verify upload QR code step2 label
    And Verify upload QR code step3 label
    And Verify that Upload button visible
    And Upload QR code file png
    And verify upload QR code step2 description after
    And verify upload QR code step3 description after
    And verify tick icon is visible on successful verification
    And verify congratulations message on successful verification
    And Verify user is still on the upload QR code page
    And Verify verify another qr code button on successful verification
    And Verify click on another qr code button
    And Upload another QR code file png
    And Verify message for valid QR code
    And Verify user is still on the upload QR code page
    Examples:
      | policy number | full name | date of birth |
      | 9991    | hello     | 01-01-2025   |

  @smoke @verifyuploadqrcode @needsInsuranceArtifacts
  Scenario Outline: Verify upload qr code with PDF
    Given User gets the title of the page
    Then Validate the title of the page
    And Verify that upload QR Code tab is visible
    And Verify upload QR code step2 label
    And Verify upload QR code step3 label
    And Verify that Upload button visible
    And Upload QR code file PDF
    And verify upload QR code step2 description after
    And verify upload QR code step3 description after
    And verify tick icon is visible on successful verification
    And verify congratulations message on successful verification
    And Verify verify another qr code button on successful verification
    And Verify click on another qr code button
    And Verify upload QR code step2 label
    And Verify upload QR code step3 label
    And Verify that Upload button visible
    And Upload QR code file PDF
    And verify upload QR code step2 description after
    And verify upload QR code step3 description after
    And verify tick icon is visible on successful verification
    And verify congratulations message on successful verification
    And Verify verify another qr code button on successful verification
    And Verify click on another qr code button
    And Upload another QR code file PDF
    And Verify message for valid QR code
    Examples:
      | policy number | full name | date of birth |
      | 9991    | hello     | 01-01-2025   |

  @smoke @verifyuploadqrcode @needsInsuranceArtifacts
  Scenario Outline: Verify upload qr code with jpg
    Given User gets the title of the page
    Then Validate the title of the page
    And Verify that upload QR Code tab is visible
    And Verify upload QR code step2 label
    And Verify upload QR code step3 label
    And Verify that Upload button visible
    And Upload QR code file JPG
    And verify upload QR code step2 description after
    And verify upload QR code step3 description after
    And verify tick icon is visible on successful verification
    And verify congratulations message on successful verification
    And Verify verify another qr code button on successful verification
    And Verify click on another qr code button
    And Verify upload QR code step2 label
    And Verify upload QR code step3 label
    And Verify that Upload button visible
    And Upload QR code file JPG
    And verify upload QR code step2 description after
    And verify upload QR code step3 description after
    And verify tick icon is visible on successful verification
    And verify congratulations message on successful verification
    And Verify verify another qr code button on successful verification
    And Verify click on another qr code button
    And Upload another QR code file JPG
    And Verify message for valid QR code
    Examples:
      | policy number | full name | date of birth |
      | 9991    | hello     | 01-01-2025   |


  @smoke @verifyuploadqrcode @needsInsuranceArtifacts
  Scenario Outline: Verify upload qr code with jpeg
    Given User gets the title of the page
    Then Validate the title of the page
    And Verify that upload QR Code tab is visible
    And Verify upload QR code step2 label
    And Verify upload QR code step3 label
    And Verify that Upload button visible
    And Upload QR code file JPEG
    And verify upload QR code step2 description after
    And verify upload QR code step3 description after
    And verify tick icon is visible on successful verification
    And verify congratulations message on successful verification
    And Verify verify another qr code button on successful verification
    And Verify click on another qr code button
    And Verify upload QR code step2 label
    And Verify upload QR code step3 label
    And Verify that Upload button visible
    And Upload QR code file JPEG
    And verify upload QR code step2 description after
    And verify upload QR code step3 description after
    And verify tick icon is visible on successful verification
    And verify congratulations message on successful verification
    And Verify verify another qr code button on successful verification
    And Verify click on another qr code button
    And Upload another QR code file JPEG
    And Verify message for valid QR code
    Examples:
      | policy number | full name | date of birth |
      | 9991    | hello     | 01-01-2025   |

  @smoke @verifyuploadInValidqrcode
  Scenario: Verify upload qr code for invalid
    And Verify browser refresh
    And Verify upload QR code step2 label
    And Verify upload QR code step3 label
    And Verify that Upload button visible
    And Upload QR code file Expired png
    And Verify message for expired QR code
    And Verify verify another qr code button on successful verification
    And Verify click on another qr code button
    And Upload QR code file Expired jpg
    And Verify message for expired QR code
    And Verify verify another qr code button on successful verification
    And Verify click on another qr code button
    And Upload QR code file Expired jpeg
    And Verify message for expired QR code
    And Verify verify another qr code button on successful verification
    And Verify click on another qr code button
    And Upload QR code file Expired pdf
    And Verify message for expired QR code
    And Verify verify another qr code button on successful verification
    And Verify click on another qr code button
    And Upload QR code unsupported file HTML
    And Verify Error message

  @smoke @verifyV2EndpointIntegration
  Scenario: Verify verify v2 endpoint integration for SVG qr code
    And Verify browser refresh
    And Verify upload QR code step2 label
    And Verify upload QR code step3 label
    And Verify that Upload button visible
    And Upload SVG rendered VC
    And Verify vc-verification api call in network tab with url
    And Verify message for valid QR code

  @smoke @verifyuploadValidqrcodeDownloadedByMobileApp @needsInsuranceArtifacts
  Scenario: Verify upload qr code for Valid
    Given User gets the title of the page
    Then Validate the title of the page
    And Verify that upload QR Code tab is visible
    And Verify upload QR code step2 label
    And Verify upload QR code step3 label
    And Verify that Upload button visible
    And Upload QR code file JPEG
    And Verify message for valid QR code

  @smoke @verifyuploadLargeSizeqrcode
  Scenario: Verify upload large size QR code
    And Verify browser refresh
    And Verify upload QR code step2 label
    And Verify upload QR code step3 label
    And Verify that Upload button visible
    And Upload Large size not supported QR code file
    And Verify Large size alert message

  @smoke @verifyuploadSmallSizeqrcode
  Scenario: Verify upload small size QR code
    And Verify that Upload button visible
    And Upload small size not supported QR code file
    And Verify Large size alert message

  @smoke @verifyuploadBoundaryMinSizeqrcode @needsInsuranceArtifacts
  Scenario: Verify upload qr code having size 10KB
    And Verify that Upload button visible
    And Upload 10KB QR code file
    And verify tick icon is visible on successful verification
    And verify congratulations message on successful verification

  @smoke @verifyuploadBoundaryMaxSizeqrcode @needsInsuranceArtifacts
  Scenario: Verify upload qr code having size 5MB
    And Verify that Upload button visible
    And Upload 5MB QR code file
    And verify tick icon is visible on successful verification
    And verify congratulations message on successful verification

  @negative @offlineUpload @withoutBrowserstack @needsInsuranceArtifacts
  Scenario: Verify upload qr code when internet is unavailable
    And Verify that Upload button visible
    And turn off internet connection
    And Upload QR code file JPEG offline
    Then Validate offline upload error message with please try again button
    And Verify click on please try again button
    Then Validate offline upload error message with please try again button

   @smoke @verifyuploadblurqrcode
  Scenario: Verify upload blur QR code
    And Verify browser refresh
    And Verify upload QR code step2 label
    And Verify upload QR code step3 label
    And Verify that Upload button visible
    And Upload blur QR code file
    And Verify small or blur alert message

  @smoke @verifyuploadmultipleqrcodeinoneimage
  Scenario: Verify multiple qr code in one image
    And Verify browser refresh
    And Verify upload QR code step2 label
    And Verify upload QR code step3 label
    And Verify that Upload button visible
    And Upload multiple qr code in one image file
    And Verify Multiple Qr code alert message

  @smoke @verifyinvalidpdf
  Scenario: Verify upload invalid pdf
    And Verify browser refresh
    And Verify upload QR code step2 label
    And Verify upload QR code step3 label
    And Verify that Upload button visible
    And Upload invalid pdf
    And Verify MultiFormat alert message

  @smoke @verifyuploadValidSDJWTqrcode
  Scenario: Verify upload valid SD-Jwt qr code
    And Verify browser refresh
    And Verify upload QR code step2 label
    And Verify upload QR code step3 label
    And Verify that Upload button visible
    And Upload SD-Jwt QR code
    And Verify message for valid QR code

  @smoke @verifyuploadValidSvgqrcode
  Scenario: Verify upload valid SVG qr code
    And Verify browser refresh
    And Verify upload QR code step2 label
    And Verify upload QR code step3 label
    And Verify that Upload button visible
    And Upload SVG rendered VC
    And Verify message for valid QR code

  @smoke @verifyuploadValidClaim169qrcode
  Scenario: Verify upload valid claim 169 qr code
    And Verify browser refresh
    And Verify upload QR code step2 label
    And Verify upload QR code step3 label
    And Verify that Upload button visible
    And Upload claim 169 VC
    And Verify message for valid QR code

  @smoke @verifyVcVerificationStatusExpiry @needsInsuranceArtifacts
  Scenario: Verify valid vc verification status expire after countdown
    Given User gets the title of the page
    Then Validate the title of the page
    And Verify that upload QR Code tab is visible
    And Verify upload QR code step2 label
    And Verify upload QR code step3 label
    And Verify that Upload button visible
    And Upload QR code file JPEG
    And Verify message for valid QR code
    And Verify the Upload button after 2 mins idle

  @smoke @verifyuploadOnetimeQRCode @needsOnetimeInsuranceArtifacts
  Scenario: Verify upload of one time valid qr code
    Given User gets the title of the page
    Then Validate the title of the page
    And Upload one time use QR code file
    And verify congratulations message on successful verification
    And Verify click on 'Verify Another QR Code' button
    And Upload one time use QR code file
    And Verify message when vc ran out of verification chances

  @smoke @verifyUploadButtonHoverSvg
  Scenario: Verify SVG upload icon visibility on repeated hover and hover off
    And Verify that Upload button visible
    And Verify gradient SVG upload icon is visible
    And Hover over the Upload QR Code button
    And Verify SVG upload icon is visible
    And Hover off the Upload QR Code button
    And Verify gradient SVG upload icon is visible
    And Hover over the Upload QR Code button
    And Verify SVG upload icon is visible
    And Hover off the Upload QR Code button
    And Verify gradient SVG upload icon is visible
    And Hover over the Upload QR Code button
    And Verify SVG upload icon is visible
    And Hover off the Upload QR Code button
    And Verify gradient SVG upload icon is visible

  @smoke @verifyInvalidVcVerificationStatusExpiry
  Scenario: Verify invalid vc verification status expire after countdown
    And Verify browser refresh
    And Verify upload QR code step2 label
    And Verify upload QR code step3 label
    And Verify that Upload button visible
    And Upload invalid pdf
    And Verify MultiFormat alert message
    And Verify the Upload button after 2 mins idle

  @smoke @keyboardAccessibilityUpload @needsInsuranceArtifacts
  Scenario: Verify keyboard accessibility for upload control
    Given User gets the title of the page
    Then Validate the title of the page
    And Verify that upload QR Code tab is visible
    And Navigate to upload button using Tab key
    And Verify upload button has keyboard focus
    And Upload QR code file JPEG via keyboard
    And verify tick icon is visible on successful verification
    And verify congratulations message on successful verification


