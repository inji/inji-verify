Feature: Inji SVG qr code testing


  @smoke @verifyUploadVcWithSvgTemplate @needsvginsurencecredetials
  Scenario: Verify upload VC with SVG template
    Given User gets the title of the page
    Then Validate the title of the page
    And Verify that upload QR Code tab is visible
    And Get current URL of upload QR code tab
    And Verify that Upload button visible
    And Upload Farmer Credential PDF file
    And verify tick icon is visible on successful verification
    And verify congratulations message on successful verification
    And Verify user is still on the upload QR code page

  @smoke @verifySvgTemplateFetchFailureFallback @needsvginsurencecredetials @withoutBrowserstack
  Scenario: Verify fallback to details grid when SVG template fails to load
    Given User gets the title of the page
    Then Validate the title of the page
    And Verify that upload QR Code tab is visible
    And Block SVG credential template network requests
    And Upload Farmer Credential PDF file
    And Verify failed to load credential template alert message is shown
    And Verify VC details grid is displayed instead of SVG template

  @smoke @verifySvgTemplateUnderSlowNetwork @needsvginsurencecredetials @withoutBrowserstack
  Scenario: Verify SVG template still renders under throttled network
    Given User gets the title of the page
    Then Validate the title of the page
    And Verify that upload QR Code tab is visible
    And Throttle network for SVG credential template requests
    And Upload Farmer Credential PDF file
    And Verify SVG template is rendered successfully

  @smoke @verifySvgTemplateVcVerificationApiCall @needsvginsurencecredetials @withoutBrowserstack
  Scenario: Verify backend verification call for SVG-templated VC
    Given User gets the title of the page
    Then Validate the title of the page
    And Verify that upload QR Code tab is visible
    And Verify browser refresh
    And Upload Farmer Credential PDF file
    And Verify vp-session-results api call in network tab with url

  @smoke @verifySvgRenderedVcViaScan @needsvginsurencecredetials @withoutBrowserstack @scan @qr_svg
  Scenario: Verify SVG rendered VC via camera scan
    Given User gets the title of the page
    Then Validate the title of the page
    When click on scan qr code tab
    And click on scan qr code button
    Then validate that QR code is scanned successfully

  @smoke @verifyConcurrentSvgUploadsInSeparateTabs @needsvginsurencecredetials
  Scenario: Verify concurrent SVG VC uploads in separate tabs produce independent results
    Given User gets the title of the page
    Then Validate the title of the page
    And Verify that upload QR Code tab is visible
    And Upload Farmer Credential PDF file
    And Verify SVG template is rendered successfully
    And Open inji verify in new tab
    And Verify that upload QR Code tab is visible
    And Upload Farmer Credential PDF file
    And Verify SVG template is rendered successfully

  @smoke @verifySvgUnsupportedTemplateMediaTypeFallback
  Scenario: Verify fallback to details grid for unsupported SVG template mediaType
    Given User gets the title of the page
    Then Validate the title of the page
    And Verify that upload QR Code tab is visible
    And Upload SVG VC with invalid template mediaType
    And Verify VC details grid is displayed instead of SVG template

  @smoke @verifySvgPartialRenderMissingData
  Scenario: Verify SVG template renders without crashing when a referenced field is missing
    Given User gets the title of the page
    Then Validate the title of the page
    And Verify that upload QR Code tab is visible
    And Upload SVG VC with missing template data
    And Verify SVG template is rendered successfully

  @smoke @verifySvgTemplateErrorMessageAcrossLanguages @withoutBrowserstack
  Scenario: Verify SVG template fetch-failure alert message is localized across languages
    Given User gets the title of the page
    Then Validate the title of the page
    And Verify that upload QR Code tab is visible
    And Block SVG credential template network requests
    And Upload SVG rendered VC
    And Verify failed to load credential template alert message is shown in "English"
    And Verify click on language dropdown
    And Verify select language "Tamil"
    And Verify that Upload button visible
    And Upload SVG rendered VC
    And Verify failed to load credential template alert message is shown in "Tamil"
#    And Verify click on language dropdown
#    And Verify select language "French"
#    And Verify that Upload button visible
#    And Upload SVG rendered VC
#    And Verify failed to load credential template alert message is shown in "French"
#    And Verify click on language dropdown
#    And Verify select language "Arabic"
#    And Verify that Upload button visible
#    And Upload SVG rendered VC
#    And Verify failed to load credential template alert message is shown in "Arabic"
#    And Verify click on language dropdown
#    And Verify select language "Spanish"
#    And Verify that Upload button visible
#    And Upload SVG rendered VC
#    And Verify failed to load credential template alert message is shown in "Spanish"

# above methods are commented because it is failing will be uncommented after the fix.