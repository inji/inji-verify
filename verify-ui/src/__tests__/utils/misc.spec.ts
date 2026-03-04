import {
    convertToTitleCase,
    getDisplayValue,
    fetchVerificationSteps,
    getRangeOfNumbers,
    getFileExtension,
    checkInternetStatus,
    convertToId,
    saveData
} from "../../utils/misc";
import i18next from "i18next";

jest.mock("i18next", () => ({
    t: jest.fn((key) => {
        if (key === "VerificationStepsContent:VERIFY.InitiateVpRequest.label") return "Initiate VP Request";
        if (key === "VerificationStepsContent:VERIFY.RequestMissingCredential.label") return "Request Missing";
        if (key === "VerificationStepsContent:VERIFY.SelectCredential.label") return "Select Credential";
        if (key === "VerificationStepsContent:VERIFY.ShareVerifiableCredentials.label") return "Share Verifiable Credentials from Wallet";
        if (key === "VerificationStepsContent:VERIFY.DisplayResult.label") return "Display Result";
        return key;
    }),
}));

jest.mock("../../utils/config", () => ({
    getVerificationStepsContent: jest.fn(() => ({
        SCAN: [{ label: "Scan Step 1" }],
        UPLOAD: [{ label: "Upload Step 1" }],
        VERIFY: [
            { label: "Initiate VP Request" },
            { label: "Request Missing" },
            { label: "Select Credential" },
            { label: "Share Verifiable Credentials from Wallet" },
            { label: "Display Result" }
        ]
    })),
    InternetConnectivityCheckTimeout: 1000,
    InternetConnectivityCheckEndpoint: "https://example.com"
}));

describe("misc utilities", () => {
    describe("convertToTitleCase", () => {
        test("converts camelCase to Title Case", () => {
            expect(convertToTitleCase("camelCaseText")).toBe("Camel Case Text");
        });
        test("returns empty string for empty input", () => {
            expect(convertToTitleCase("")).toBe("");
        });
    });

    describe("getDisplayValue", () => {
        test("joins array with commas", () => {
            expect(getDisplayValue(["A", "B"])).toBe("A, B");
        });
        test("returns string representation for non-array", () => {
            expect(getDisplayValue(123)).toBe("123");
        });
        test("returns undefined for null", () => {
            expect(getDisplayValue(null)).toBeUndefined();
        });
    });

    describe("fetchVerificationSteps", () => {
        test("returns UPLOAD steps", () => {
            const steps = fetchVerificationSteps("UPLOAD", false);
            expect(steps[0].label).toBe("Upload Step 1");
        });

        test("returns SCAN steps", () => {
            const steps = fetchVerificationSteps("SCAN", false);
            expect(steps[0].label).toBe("Scan Step 1");
        });

        test("returns VERIFY steps for sameDevice partially shared", () => {
            const steps = fetchVerificationSteps("VERIFY", true, "sameDevice", 2);
            expect(steps.some(s => s.label === "Request Missing")).toBeTruthy();
            expect(steps.some(s => s.label === "Share Verifiable Credentials from Wallet")).toBeTruthy();
            expect(steps[0].isCompleted).toBe(true);
            expect(steps[1].isActive).toBe(true);
        });

        test("returns VERIFY steps for crossDevice not partially shared", () => {
            const steps = fetchVerificationSteps("VERIFY", false, "crossDevice");
            expect(steps.some(s => s.label === "Select Credential")).toBeTruthy();
            expect(steps.some(s => s.label === "Share Verifiable Credentials from Wallet")).toBeTruthy();
        });
    });

    describe("getRangeOfNumbers", () => {
        test("returns array of numbers", () => {
            expect(getRangeOfNumbers(3)).toEqual([1, 2, 3]);
        });
    });

    describe("getFileExtension", () => {
        test("returns extension", () => {
            expect(getFileExtension("test.pdf")).toBe("pdf");
        });
    });

    describe("checkInternetStatus", () => {
        const originalFetch = global.fetch;
        const originalOnLine = Object.getOwnPropertyDescriptor(window.navigator, "onLine");

        beforeEach(() => {
            global.fetch = jest.fn();
        });

        afterEach(() => {
            global.fetch = originalFetch;
            if (originalOnLine) {
                Object.defineProperty(window.navigator, "onLine", originalOnLine);
            }
        });

        test("returns false if navigator.onLine is false", async () => {
            Object.defineProperty(window.navigator, 'onLine', { value: false, configurable: true });
            expect(await checkInternetStatus()).toBe(false);
        });

        test("returns true if fetch succeeds", async () => {
            Object.defineProperty(window.navigator, 'onLine', { value: true, configurable: true });
            (global.fetch as jest.Mock).mockResolvedValueOnce({ ok: true });
            expect(await checkInternetStatus()).toBe(true);
        });

        test("returns false if fetch fails", async () => {
            Object.defineProperty(window.navigator, 'onLine', { value: true, configurable: true });
            (global.fetch as jest.Mock).mockRejectedValueOnce(new Error("Fail"));
            expect(await checkInternetStatus()).toBe(false);
        });
    });

    describe("convertToId", () => {
        test("converts string to kebab-case id", () => {
            expect(convertToId("Some Content")).toBe("some-content");
        });
    });

    describe("saveData", () => {
        const originalCreateObjectURL = URL.createObjectURL;
        const originalRevokeObjectURL = URL.revokeObjectURL;

        afterEach(() => {
            URL.createObjectURL = originalCreateObjectURL;
            URL.revokeObjectURL = originalRevokeObjectURL;
            jest.restoreAllMocks();
        });

        test("triggers download", async () => {
            const mockCreateObjectURL = jest.fn(() => "blob:url");
            const mockRevokeObjectURL = jest.fn();
            URL.createObjectURL = mockCreateObjectURL;
            URL.revokeObjectURL = mockRevokeObjectURL;

            const mockLink = {
                href: "",
                download: "",
                click: jest.fn(),
            };
            const createElementSpy = jest.spyOn(document, "createElement").mockReturnValue(mockLink as any);
            jest.spyOn(document.body, "appendChild").mockImplementation();
            jest.spyOn(document.body, "removeChild").mockImplementation();

            await saveData({ type: ["VerifiableCredential", "MyType"], data: "test" });

            expect(mockLink.download).toBe("MyType.json");
            expect(mockLink.click).toHaveBeenCalled();
        });

        test("uses default filename if type is missing", async () => {
            URL.createObjectURL = jest.fn(() => "blob:url");
            URL.revokeObjectURL = jest.fn();

            const mockLink = { href: "", download: "", click: jest.fn() };
            jest.spyOn(document, "createElement").mockReturnValue(mockLink as any);
            jest.spyOn(document.body, "appendChild").mockImplementation();
            jest.spyOn(document.body, "removeChild").mockImplementation();

            await saveData({ data: "test" });
            expect(mockLink.download).toBe("Inji_Verify_Credential_Data.json");
        });
    });
});
