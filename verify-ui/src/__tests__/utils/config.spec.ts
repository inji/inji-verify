import {
    getVerificationStepsContent,
    AlertMessages,
    initializeClaims,
    isMobileDevice,
    getStepConfig,
    resolveWalletBaseUrl,
    getWebWallets,
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

    describe("resolveWalletBaseUrl", () => {
        test("returns URL unchanged when no trailing slash", () => {
            expect(resolveWalletBaseUrl("http://wallet.example.org")).toBe("http://wallet.example.org");
            expect(resolveWalletBaseUrl("https://injiweb.dev-int-inji.mosip.net")).toBe(
                "https://injiweb.dev-int-inji.mosip.net"
            );
            expect(resolveWalletBaseUrl("/wallet")).toBe("/wallet");
        });

        test("removes trailing slash", () => {
            expect(resolveWalletBaseUrl("https://injiweb.dev-int-inji.mosip.net/")).toBe(
                "https://injiweb.dev-int-inji.mosip.net"
            );
            expect(resolveWalletBaseUrl("/wallet/")).toBe("/wallet");
        });
    });

    describe("initializeClaims", () => {
        const origin = "https://verify.example.org";

        beforeEach(() => {
            Object.defineProperty(window, "location", {
                value: { origin },
                writable: true,
            });
        });

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

        test("resolves absolute wallet URLs unchanged", async () => {
            const mockData = {
                verifiableClaims: [],
                VCRenderOrders: {},
                WebWallets: [
                    { id: "w1", name: "External", iconUrl: "/icon.svg", walletBaseUrl: "https://injiweb.dev-int-inji.mosip.net" },
                ],
            };
            global.fetch = jest.fn().mockResolvedValueOnce({ ok: true, json: () => Promise.resolve(mockData) });
            await initializeClaims();
            expect(getWebWallets()[0].walletBaseUrl).toBe("https://injiweb.dev-int-inji.mosip.net");
        });

        test("trims trailing slash from wallet URLs", async () => {
            const mockData = {
                verifiableClaims: [],
                VCRenderOrders: {},
                WebWallets: [
                    { id: "w2", name: "Same-env", iconUrl: "/icon.svg", walletBaseUrl: "/wallet/" },
                ],
            };
            global.fetch = jest.fn().mockResolvedValueOnce({ ok: true, json: () => Promise.resolve(mockData) });
            await initializeClaims();
            expect(getWebWallets()[0].walletBaseUrl).toBe("/wallet");
        });

        test("filters out wallets with empty walletBaseUrl", async () => {
            const mockData = {
                verifiableClaims: [],
                VCRenderOrders: {},
                WebWallets: [
                    { id: "w3", name: "Unconfigured", iconUrl: "/icon.svg", walletBaseUrl: "" },
                    { id: "w4", name: "Configured", iconUrl: "/icon.svg", walletBaseUrl: "https://wallet.example.org" },
                ],
            };
            global.fetch = jest.fn().mockResolvedValueOnce({ ok: true, json: () => Promise.resolve(mockData) });
            await initializeClaims();
            const wallets = getWebWallets();
            expect(wallets).toHaveLength(1);
            expect(wallets[0].id).toBe("w4");
        });

        test("handles fetch error", async () => {
            const consoleErrorSpy = jest.spyOn(console, "error").mockImplementation();
            global.fetch = jest.fn().mockResolvedValueOnce({ ok: false, status: 500 });
            await initializeClaims();
            expect(consoleErrorSpy).toHaveBeenCalled();
            consoleErrorSpy.mockRestore();
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
