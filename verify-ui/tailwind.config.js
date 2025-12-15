/** @type {import('tailwindcss').Config} */

module.exports = {
  content: ["./src/**/**/*.{js,jsx,ts,tsx}"],
  theme: {
    extend: {
      fontFamily: {
        base: "var(--iv-font-base)",
      },
      fontSize: {
        lgBoldTextSize: "26px",
        boldTextSize: "19px",
        lgMediumTextSize: "24px",
        mediumTextSize: "20px",
        lgNormalTextSize: "16px",
        normalTextSize: "14px",
        smallTextSize: "12px",
        verySmallTextSize: "11px",
      },
      maxHeight: {
        0: "0",
        "1/4": "25%",
        "1/2": "50%",
        "3/4": "75%",
        full: "100%",
      },
      colors: {
        primary: "var(--iv-primary-color)",
        logoStart: "var(--iv-logo-start-color)",
        background: "var(--iv-background-color)",
        whiteText: "var(--iv-white-text)",
        headerLabelText: "var(--iv-header-label-text)",
        headerDescriptionText: "var(--iv-header-description-text)",
        offlineLabel: "var(--iv-offline-label-text)",
        offlineDescription: "var(--iv-offline-description-text)",
        tabsBackGround: "var(--iv-verificationMethodTabs-bg-color)",
        horizontalLine: "var(--iv-horizontal-line)",
        headerBackGround: "var(--iv-header-background-color)",
        pageBackGroundColor: "var(--iv-page-background-color)",
        success: "var(--iv-success)",
        invalid: "var(--iv-invalid)",
        expired: "var(--iv-expired)",
        successText: "var(--iv-successText)",
        invalidText: "var(--iv-invalidText)",
        expiredText: "var(--iv-expiredText)",
        successBorder: "var( --iv-successBorder)",
        invalidBorder: "var(--iv-invalidBorder)",
        expiredBorder: "var(--iv-expiredBorder)",
        successAlert: "var(--iv-success-alert)",
        warningAlert: "var(--iv-warning-alert)",
        errorAlert: "var(--iv-error-alert)",
        arrowBackGround: "var(--iv-arrow-background)",
        stepperBackGround: "var(--iv-stepper-background)",
        stepperLabel: "var(--iv-stepper-label)",
        stepperDescription: "var(--iv-stepper-description)",
        cameraDeniedLabel: "var(--iv-camera-denied-label-color)",
        cameraDeniedDescription: "var(--iv-camera-denied-description-color)",
        uploadDescription: "var(--iv-upload-description)",
        copyRightsText: "var(--iv-copyrights-text)",
        copyRightsBorder: "var(--iv-copyrights-border)",
        activeTabText: "var(--iv-active-tab-text)",
        activeTabBackground: "var(--iv-active-tab-background)",
        inactiveTabText: "var(--iv-inactive-tab-text)",
        inactiveTabBackground: "var(--iv-inactive-tab-background)",
        disableTabBackground: "var(--iv-disabledTab-bg)",
        disableTabText: "var(--iv-disabledTab-text)",
        documentIcon: "var(--iv-document-icon)",
        disabledButtonBg: "var(--iv-disabled-button-bg)",
        selectorBorder: "var(--iv-selector-border)",
        sortByBorder: "var(--iv-sortBy-border)",
        sortByText: "var(--iv-sortBy-text)",
        selectorPannelTitle: "var(--iv-selector-pannel-title)",
        selectorPannelSubTitle: "var(--iv-selector-pannel-sub-title)",
        qrCodeTimer: "var(--iv-qr-code-timer)",

        /* --- TruckPass minimal solid color keys (added) --- */
        truckpassPrimary: "#006DE7",
        truckpassBackground: "#F5FAFF",
        truckpassLine: "#006DE7",
      },
      backgroundImage: {
        "default_theme-gradient":
          "linear-gradient(90deg, #FF5300 0%, #FB5103 16%, #F04C0F 31%, #DE4322 46%, #C5363C 61%, #A4265F 75%, #7C1389 90%, #5B03AD 100%)",
        "default_theme-lighter-gradient":
          "linear-gradient(90deg, rgba(255, 83, 0, 0.08) 0%, rgba(251, 81, 3, 0.08) 16%, rgba(240, 76, 15, 0.08) 31%, rgba(222, 67, 34, 0.08) 46%, rgba(197, 54, 60, 0.08) 61%, rgba(164, 38, 95, 0.08) 75%, rgba(124, 19, 137, 0.08) 90%, rgba(91, 3, 173, 0.08) 100%)",
        "purple_theme-gradient":
          "linear-gradient(90deg, #5a31ee 0%, #8f69ec 100%)",
        "purple_theme-lighter-gradient":
          "linear-gradient(83.99deg, #f9fafc 38.27%, #F8FBFF 93.3%)",
        "car_theme-gradient":
          "linear-gradient(90deg, #52AE32 0%, #006535 100%)",
        "car_theme-lighter-gradient":
          "linear-gradient(83.99deg, rgba(211, 246, 199, 0.08) 38.27%, rgba(82, 174, 50, 0.08) 93.3%)",

        /* --- TruckPass theme kept as gradients but start & end identical (solid look) --- */
        "truckpass_theme-gradient":
          "linear-gradient(90deg, #006DE7 0%, #006DE7 100%)",
        "truckpass_theme-lighter-gradient":
          "linear-gradient(90deg, #F5FAFF 0%, #F5FAFF 100%)",
      },
    },
  },
  safelist: [
    "bg-default_theme-gradient",
    "bg-default_theme-lighter-gradient",
    "hover:bg-default_theme-gradient",
    "bg-purple_theme-gradient",
    "bg-purple_theme-lighter-gradient",
    "hover:bg-purple_theme-gradient",
    "bg-car_theme-gradient",
    "bg-car_theme-lighter-gradient",
    "hover:bg-car_theme-gradient",
    "bg-truckpass_theme-gradient",
    "bg-truckpass_theme-lighter-gradient",
    "hover:bg-truckpass_theme-gradient",
  ], //Listing possible class names in the safelist, ensures Tailwind generates those styles.
  plugins: [],
};
