export {};

declare global {
    interface Window {
        encodeData: (data: string) => void;
        _env_: {
            DEFAULT_LANG: string;
            DEFAULT_TITLE: string;
            DEFAULT_THEME: string;
            DEFAULT_FONT_URL: string;
            DEFAULT_FONT_FAMILY: string;
            OVP_QR_HEADER: string
            INTERNET_CONNECTIVITY_CHECK_ENDPOINT: string,
            INTERNET_CONNECTIVITY_CHECK_TIMEOUT: string,
            VERIFY_SERVICE_API_URL: string,
            VERIFIABLE_CLAIMS_CONFIG_URL: string,
            VP_SUBMISSION_SUPPORTED: string,
            CLIENT_ID: string;
            CLIENT_ID_DID: string;
            DISPLAY_TIMEOUT: string;
        }
    }
}