// jest-dom adds custom jest matchers for asserting on DOM nodes.
// allows you to do things like:
// expect(element).toHaveTextContent(/react/i)
// learn more: https://github.com/testing-library/jest-dom
import '@testing-library/jest-dom';

(window as any)._env_ = {
    INTERNET_CONNECTIVITY_CHECK_ENDPOINT: "https://dns.google/",
    INTERNET_CONNECTIVITY_CHECK_TIMEOUT: "3000",
    DISPLAY_TIMEOUT: "5000",
    VERIFIABLE_CLAIMS_CONFIG_URL: "https://claims.api",
    OVP_QR_HEADER: "ovp-qr-header",
    DEFAULT_THEME: "orange",
    VERIFY_SERVICE_API_URL: "/verify",
};


global.fetch = jest.fn().mockImplementation(() =>
    Promise.resolve({
        ok: true,
        json: () => Promise.resolve({ verifiableClaims: [], VCRenderOrders: {} }),
    })
) as jest.Mock;

