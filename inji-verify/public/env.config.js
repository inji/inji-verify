window._env_ = {
  INTERNET_CONNECTIVITY_CHECK_ENDPOINT: "https://dns.google/",
  INTERNET_CONNECTIVITY_CHECK_TIMEOUT: "10000",
  OVP_QR_HEADER: "INJI_OVP://",
  DEFAULT_TITLE: "Inji Verify",
  DEFAULT_THEME: "default_theme",
  DEFAULT_LANG: "en",
  DEFAULT_FONT_URL: "https://fonts.googleapis.com/css?family=Inter",
  ESIGNET_UI_BASE_URL: "http://localhost:3000",
  MOCK_RELYING_PARTY_SERVER_URL:
    "http://localhost:5000/mock-relying-party-server",
  REDIRECT_URI_USER_PROFILE: "http://localhost:5000/userprofile",
  REDIRECT_URI: "http://localhost:5000/userprofile",
  CLIENT_ID: "IIBIjANBgkqhkiG9w0BAQEFAAOCAQ8AMIIBCgKCAQEAsFWo7",
  ACRS: "mosip:idp:acr:generated-code%20mosip:idp:acr:biometrics%20mosip:idp:acr:linked-wallet",
  SCOPE_USER_PROFILE: "openid%20profile%20resident-service",
  SCOPE_REGISTRATION: "openid%20profile",
  CLAIMS_USER_PROFILE:
    "%7B%22userinfo%22:%7B%22given_name%22:%7B%22essential%22:true%7D,%22phone_number%22:%7B%22essential%22:false%7D,%22email%22:%7B%22essential%22:true%7D,%22picture%22:%7B%22essential%22:false%7D,%22gender%22:%7B%22essential%22:false%7D,%22birthdate%22:%7B%22essential%22:false%7D,%22address%22:%7B%22essential%22:false%7D%7D,%22id_token%22:%7B%7D%7D",
  CLAIMS_REGISTRATION:
    "%7B%22userinfo%22:%7B%22given_name%22:%7B%22essential%22:true%7D,%22phone_number%22:%7B%22essential%22:false%7D,%22email%22:%7B%22essential%22:true%7D,%22picture%22:%7B%22essential%22:false%7D,%22gender%22:%7B%22essential%22:false%7D,%22birthdate%22:%7B%22essential%22:false%7D,%22address%22:%7B%22essential%22:false%7D%7D,%22id_token%22:%7B%7D%7D",
  SIGN_IN_BUTTON_PLUGIN_URL:
    "http://127.0.0.1:5500/sign-in-with-esignet/dist/iife/index.js",
  DISPLAY: "page",
  PROMPT: "consent",
  GRANT_TYPE: "authorization_code",
  MAX_AGE: 21,
  CLAIMS_LOCALES: "en",
  DEFAULT_LANG: "en",
  FALLBACK_LANG: "%7B%22label%22%3A%22English%22%2C%22value%22%3A%22en%22%7D",

  // Feature flag to enable/disable foreigner sign-in button
  ENABLE_FOREIGNER_SIGNIN: "true",

  // Foreigner-specific authentication configurations
  ESIGNET_UI_BASE_URL_FOREIGNER: "http://localhost:3001",
  REDIRECT_URI_USER_PROFILE_FOREIGNER: "http://localhost:5000/userprofile",
  REDIRECT_URI_FOREIGNER: "http://localhost:5000/userprofile",
  CLIENT_ID_FOREIGNER: "IIBIjANBgkqhkiG9w0BAQEFAAOCAQ8AMIIBCgKCAQEAsFWo7",
  ACRS_FOREIGNER: "mosip:idp:acr:generated-code%20mosip:idp:acr:biometrics%20mosip:idp:acr:linked-wallet",
  SCOPE_USER_PROFILE_FOREIGNER: "openid%20profile%20resident-service",
  SCOPE_REGISTRATION_FOREIGNER: "openid%20profile",
  CLAIMS_USER_PROFILE_FOREIGNER:
    "%7B%22userinfo%22:%7B%22given_name%22:%7B%22essential%22:true%7D,%22phone_number%22:%7B%22essential%22:false%7D,%22email%22:%7B%22essential%22:true%7D,%22picture%22:%7B%22essential%22:false%7D,%22gender%22:%7B%22essential%22:false%7D,%22birthdate%22:%7B%22essential%22:false%7D,%22address%22:%7B%22essential%22:false%7D%7D,%22id_token%22:%7B%7D%7D",
  CLAIMS_REGISTRATION_FOREIGNER:
    "%7B%22userinfo%22:%7B%22given_name%22:%7B%22essential%22:true%7D,%22phone_number%22:%7B%22essential%22:false%7D,%22email%22:%7B%22essential%22:true%7D,%22picture%22:%7B%22essential%22:false%7D,%22gender%22:%7B%22essential%22:false%7D,%22birthdate%22:%7B%22essential%22:false%7D,%22address%22:%7B%22essential%22:false%7D%7D,%22id_token%22:%7B%7D%7D",
  DISPLAY_FOREIGNER: "page",
  PROMPT_FOREIGNER: "consent",
  GRANT_TYPE_FOREIGNER: "authorization_code",
  MAX_AGE_FOREIGNER: 21,
  CLAIMS_LOCALES_FOREIGNER: "en",
};
