import {
    getVerificationStepsContent,
    AlertMessages,
    initializeClaims,
    isMobileDevice,
    getStepConfig,
    Pages,
    SupportedFileTypes,
    VerificationSteps,
    backgroundColorMapping,
    borderColorMapping,
    textColorMapping
} from "../../utils/config";
import i18next from "i18next";

jest.mock("i18next", () => ({
    t: jest.fn((key) => key),
}));

describe("config utilities", () => {
    beforeEach(() => {
        jest.clearAllMocks();
        // Mock window._env_
        (window as any)._env_ = {
            INTERNET_CONNECTIVITY_CHECK_ENDPOINT: "https://dns.google/",
            INTERNET_CONNECTIVITY_CHECK_TIMEOUT: "3000",
            DISPLAY_TIMEOUT: "5000",
            VERIFIABLE_CLAIMS_CONFIG_URL: "https://claims.api",
        };
    });

    test("getVerificationStepsContent returns structured content", () => {
        const content = getVerificationStepsContent();
        expect(content).toHaveProperty("SCAN");
        expect(content).toHaveProperty("UPLOAD");
        expect(content).toHaveProperty("VERIFY");
        expect(i18next.t).toHaveBeenCalled();
    });

    test("AlertMessages returns alert info objects", () => {
        const alerts = AlertMessages();
        expect(alerts.qrUploadSuccess.severity).toBe("success");
        expect(alerts.sessionExpired.severity).toBe("error");
    });

    describe("initializeClaims", () => {
        test("calls fetch and updates claims", async () => {
            const mockData = {
                verifiableClaims: [{ id: "1" }],
                VCRenderOrders: { "1": "order" }
            };
            global.fetch = jest.fn().mockResolvedValueOnce({
                ok: true,
                json: () => Promise.resolve(mockData)
            });

            await initializeClaims();
            expect(global.fetch).toHaveBeenCalledWith(window._env_.VERIFIABLE_CLAIMS_CONFIG_URL);
        });

        test("handles fetch error", async () => {
            console.error = jest.fn();
            global.fetch = jest.fn().mockResolvedValueOnce({ ok: false, status: 500 });
            await initializeClaims();
            expect(console.error).toHaveBeenCalled();
        });
    });

    describe("isMobileDevice", () => {
        const originalUserAgent = navigator.userAgent;

        afterEach(() => {
            Object.defineProperty(navigator, 'userAgent', { value: originalUserAgent, configurable: true });
        });

        test("returns true for iPhone", () => {
            Object.defineProperty(navigator, 'userAgent', { value: "iPhone", configurable: true });
            expect(isMobileDevice()).toBe(true);
        });

        test("returns false for Desktop", () => {
            Object.defineProperty(navigator, 'userAgent', { value: "Mozilla/5.0 (Windows NT 10.0; Win64; x64)", configurable: true });
            expect(isMobileDevice()).toBe(false);
        });
    });

    describe("getStepConfig", () => {
        test("returns config for valid method", () => {
            expect(getStepConfig("SCAN")).toEqual(VerificationSteps.SCAN);
        });
        test("returns null for invalid method", () => {
            expect(getStepConfig("INVALID")).toBeNull();
        });
    });

    test("mappings are correctly defined", () => {
        expect(backgroundColorMapping.SUCCESS).toBe("bg-success");
        expect(textColorMapping.EXPIRED).toBe("text-expiredText");
        expect(borderColorMapping.INVALID).toBe("border-invalidBorder");
    });
});
