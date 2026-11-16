export {};

let misc: typeof import("../../utils/misc");

describe("misc utilities", () => {
    beforeEach(() => {
        jest.resetModules();

        (window as any)._env_ = {
            INTERNET_CONNECTIVITY_CHECK_ENDPOINT: "https://example.com",
            INTERNET_CONNECTIVITY_CHECK_TIMEOUT: "1000",
            DISPLAY_TIMEOUT: "1000",
            OVP_QR_HEADER: "header",
        };

        jest.doMock("i18next", () => ({
            __esModule: true,
            default: {
                use: jest.fn().mockReturnThis(),
                init: jest.fn().mockReturnThis(),
                t: jest.fn((key: string) => {
                    const map: Record<string, string> = {
                        "VerificationStepsContent:VERIFY.InitiateVpRequest.label": "Initiate VP Request",
                        "VerificationStepsContent:VERIFY.RequestMissingCredential.label": "Request Missing",
                        "VerificationStepsContent:VERIFY.SelectCredential.label": "Select Credential",
                        "VerificationStepsContent:VERIFY.ShareVerifiableCredentials.label": "Share Verifiable Credentials from Wallet",
                        "VerificationStepsContent:VERIFY.DisplayResult.label": "Display Result",
                    };
                    return map[key] || key;
                }),
            },
        }));

        jest.doMock("../../utils/config", () => ({
            __esModule: true,
            InternetConnectivityCheckTimeout: 1000,
            InternetConnectivityCheckEndpoint: "https://example.com",
            getVerificationStepsContent: jest.fn(() => ({
                UPLOAD: [{label: "Upload Step 1", description: "Upload Desc 1"}],
                SCAN: [{label: "Scan Step 1", description: "Scan Desc 1"}],
                VERIFY: [{label: "Initiate VP Request", description: "desc 1"},
                    {label: "Select Credential", description: "desc 2"},
                    {label: "Request Missing", description: "desc 3"},
                    {label: "Share Verifiable Credentials from Wallet", description: "desc 4"},
                    {label: "Display Result", description: "desc 5"}], TO_BE_SELECTED: [],
            })),
        }));

        misc = require("../../utils/misc");
    });

    afterEach(() => {
        jest.clearAllMocks();
        jest.resetModules();
    });

    describe("convertToTitleCase", () => {
        test("converts camelCase to Title Case", () => {
            expect(misc.convertToTitleCase("camelCaseText")).toBe("Camel Case Text");
        });
        test("returns empty string for empty input", () => {
            expect(misc.convertToTitleCase("")).toBe("");
        });
    });

    describe("getDisplayValue", () => {
        test("joins array with commas", () => {
            expect(misc.getDisplayValue(["A", "B"])).toBe("A, B");
        });
        test("returns string representation for non-array", () => {
            expect(misc.getDisplayValue(123)).toBe("123");
        });
        test("returns undefined for null", () => {
            expect(misc.getDisplayValue(null)).toBeUndefined();
        });
    });

    describe("fetchVerificationSteps", () => {
        test("returns UPLOAD steps", () => {
            const steps = misc.fetchVerificationSteps("UPLOAD", false);
            expect(steps).toHaveLength(1);
            expect(steps[0].label).toBe("Upload Step 1");
            expect(steps[0].stepNumber).toBe(1);
            expect(steps[0].isActive).toBe(true);
        });

        test("returns SCAN steps", () => {
            const steps = misc.fetchVerificationSteps("SCAN", false);
            expect(steps).toHaveLength(1);
            expect(steps[0].label).toBe("Scan Step 1");
            expect(steps[0].stepNumber).toBe(1);
            expect(steps[0].isActive).toBe(true);
        });

        test("returns VERIFY steps for sameDevice partially shared", () => {
            const steps = misc.fetchVerificationSteps("VERIFY", true, "sameDevice", 2);

            expect(steps).toHaveLength(4);
            expect(steps.map((s: any) => s.label)).toEqual([
                "Initiate VP Request",
                "Request Missing",
                "Share Verifiable Credentials from Wallet",
                "Display Result",
            ]);
            expect(steps[0].isCompleted).toBe(true);
            expect(steps[1].isActive).toBe(true);
        });

        test("returns VERIFY steps for crossDevice not partially shared", () => {
            const steps = misc.fetchVerificationSteps("VERIFY", false, "crossDevice");
            expect(steps).toHaveLength(4);
            expect(steps.map((s: any) => s.label)).toEqual([
                "Initiate VP Request",
                "Select Credential",
                "Share Verifiable Credentials from Wallet",
                "Display Result",
            ]);
        });
    });

    describe("getRangeOfNumbers", () => {
        test("returns array of numbers", () => {
            expect(misc.getRangeOfNumbers(3)).toEqual([1, 2, 3]);
        });
    });

    describe("getFileExtension", () => {
        test("returns extension", () => {
            expect(misc.getFileExtension("test.pdf")).toBe("pdf");
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
            jest.clearAllMocks();
        });

        test("returns false if navigator.onLine is false", async () => {
            Object.defineProperty(window.navigator, "onLine", {value: false, configurable: true,});
            await expect(misc.checkInternetStatus()).resolves.toBe(false);
        });

        test("returns true if fetch succeeds", async () => {
            Object.defineProperty(window.navigator, "onLine", {value: true, configurable: true,});
            (global.fetch as jest.Mock).mockResolvedValueOnce({ ok: true });
            await expect(misc.checkInternetStatus()).resolves.toBe(true);
        });

        test("returns false if fetch fails", async () => {
            Object.defineProperty(window.navigator, "onLine", {value: true, configurable: true,});
            (global.fetch as jest.Mock).mockRejectedValueOnce(new Error("Fail"));
            await expect(misc.checkInternetStatus()).resolves.toBe(false);
        });
    });

    describe("convertToId", () => {
        test("converts string to kebab-case id", () => {
            expect(misc.convertToId("Some Content")).toBe("some-content");
        });
    });

    describe("getDownloadFileName", () => {
        test("resolves credential type from multi-type array", () => {
            expect(misc.getDownloadFileName({ type: ["VerifiableCredential", "InsuranceCredential"] })).toBe("InsuranceCredential");
        });

        test("resolves credential type from single-item array", () => {
            expect(misc.getDownloadFileName({ type: ["InsuranceCredential"] })).toBe("InsuranceCredential");
        });

        test("resolves credential type from string type", () => {
            expect(misc.getDownloadFileName({ type: "InsuranceCredential" })).toBe("InsuranceCredential");
        });

        test("resolves credential type from SD-JWT regularClaims", () => {
            expect(misc.getDownloadFileName({ regularClaims: { vct: "MockIdentityCredential" } })).toBe("MockIdentityCredential");
        });

        test("resolves credential type from object with _value", () => {
            expect(misc.getDownloadFileName({ type: [{ _value: "InsuranceCredential" }] })).toBe("InsuranceCredential");
        });

        test("extracts clean type name from IRI with hash", () => {
            expect(misc.getDownloadFileName({ type: ["VerifiableCredential", "https://example.com/context#InsuranceCredential"] })).toBe("InsuranceCredential");
        });

        test("extracts clean type name from IRI with slash", () => {
            expect(misc.getDownloadFileName({ type: ["VerifiableCredential", "https://example.com/context/InsuranceCredential"] })).toBe("InsuranceCredential");
        });

        test("falls back to default filename when only VerifiableCredential is provided", () => {
            expect(misc.getDownloadFileName({ type: ["VerifiableCredential"] })).toBe("Inji_Verify_Credential_Data");
        });

        test("falls back to default filename when credential has no type", () => {
            expect(misc.getDownloadFileName({})).toBe("Inji_Verify_Credential_Data");
        });
    });

    describe("saveData", () => {
        const originalCreateObjectURL = URL.createObjectURL;
        const originalRevokeObjectURL = URL.revokeObjectURL;

        let mockLink: any;
        let mockCreateObjectURL: jest.Mock;
        let mockRevokeObjectURL: jest.Mock;

        beforeEach(() => {
            mockCreateObjectURL = jest.fn(() => "blob:url");
            mockRevokeObjectURL = jest.fn();
            URL.createObjectURL = mockCreateObjectURL;
            URL.revokeObjectURL = mockRevokeObjectURL;

            mockLink = {
                href: "",
                download: "",
                click: jest.fn(),
            };

            jest.spyOn(document, "createElement").mockReturnValue(mockLink as any);
            jest.spyOn(document.body, "appendChild").mockImplementation(() => mockLink as any);
            jest.spyOn(document.body, "removeChild").mockImplementation(() => mockLink as any);
        });

        afterEach(() => {
            URL.createObjectURL = originalCreateObjectURL;
            URL.revokeObjectURL = originalRevokeObjectURL;
            jest.restoreAllMocks();
        });

        test("triggers download", async () => {
            await misc.saveData({ type: ["VerifiableCredential", "MyType"], data: "test" });

            expect(mockLink.download).toBe("MyType.json");
            expect(mockLink.click).toHaveBeenCalled();
            expect(mockCreateObjectURL).toHaveBeenCalled();
            expect(mockRevokeObjectURL).toHaveBeenCalledWith("blob:url");
        });

        test("correctly names file for single-item type array", async () => {
            await misc.saveData({ type: ["InsuranceCredential"], data: "test" });
            expect(mockLink.download).toBe("InsuranceCredential.json");
        });

        test("correctly names file for string type", async () => {
            await misc.saveData({ type: "InsuranceCredential", data: "test" });
            expect(mockLink.download).toBe("InsuranceCredential.json");
        });

        test("correctly names file for SD-JWT credential", async () => {
            await misc.saveData({ regularClaims: { vct: "MockIdentityCredential" }, data: "test" });
            expect(mockLink.download).toBe("MockIdentityCredential.json");
        });

        test("uses default filename if type is missing", async () => {
            await misc.saveData({ data: "test" });
            expect(mockLink.download).toBe("Inji_Verify_Credential_Data.json");
            expect(mockLink.click).toHaveBeenCalled();
            expect(mockCreateObjectURL).toHaveBeenCalled();
            expect(mockRevokeObjectURL).toHaveBeenCalledWith("blob:url");
        });
    });
});