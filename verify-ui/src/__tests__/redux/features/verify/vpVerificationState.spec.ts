import vpVerificationReducer, {
    setSelectCredential,
    setSelectedCredentials,
    setFlowType,
    getVpRequest,
    verificationSubmissionComplete,
    resetVpRequest
} from "../../../../redux/features/verify/vpVerificationState";
import { VCShareType } from "../../../../types/data-types";
import { VerificationSteps } from "../../../../utils/config";

jest.mock("../../../../utils/config", () => ({
    ...jest.requireActual("../../../../utils/config"),
    getVerifiableClaims: jest.fn(() => [
        { id: "1", type: "Type1", essential: true, definition: { input_descriptors: [{ id: "desc1" }] } },
        { id: "2", type: "Type2", essential: false, definition: { input_descriptors: [{ id: "desc2" }] } }
    ])
}));

jest.mock("../../../../utils/commonUtils", () => ({
    calculateUnverifiedClaims: jest.fn(() => []),
    calculateVerifiedClaims: jest.fn(() => []),
    getCredentialType: jest.fn((vc) => vc?.type || "unknown")
}));

describe("vpVerification slice", () => {
    test("should handle setSelectCredential", () => {
        const state = vpVerificationReducer(undefined, setSelectCredential());
        expect(state.activeScreen).toBe(VerificationSteps.VERIFY.SelectCredential);
        expect(state.SelectionPanel).toBe(true);
        expect(state.SelectWalletPanel).toBe(false);
        expect(state.selectedCredentials).toHaveLength(1); // essential only
    });

    test("should handle setSelectCredential with SelectWalletPanel open", () => {
        const initialState = {
            SelectWalletPanel: true,
            SelectionPanel: false,
            method: "VERIFY",
            presentationDefinition: { id: "test", input_descriptors: [] }
        } as any;
        const state = vpVerificationReducer(initialState, setSelectCredential());
        expect(state.SelectionPanel).toBe(true);
        expect(state.SelectWalletPanel).toBe(false);
    });

    test("should handle setSelectedCredentials", () => {
        const selectedCredentials = [{ id: "2", type: "Type2", definition: { input_descriptors: [] } }] as any;
        const state = vpVerificationReducer(undefined, setSelectedCredentials({ selectedCredentials }));
        expect(state.selectedCredentials).toHaveLength(1);
        expect(state.sharingType).toBe(VCShareType.SINGLE);
    });

    test("should handle setFlowType", () => {
        const state = vpVerificationReducer(undefined, setFlowType());
        // flowType is the runtime discriminator: "sameDevice" → wallet-selector path,
        // "crossDevice" → QR-code path. Both paths share activeScreen === 3, so
        // flowType must be asserted alongside activeScreen to make the intent unambiguous.
        expect(state.flowType).toBe("sameDevice");
        expect(state.activeScreen).toBe(VerificationSteps.VERIFY.SelectWallet);
        expect(state.SelectWalletPanel).toBe(false);
    });

    test("should handle setFlowType with SelectWalletPanel open", () => {
        const initialState = { SelectWalletPanel: true, method: "VERIFY", flowType: "crossDevice" } as any;
        const state = vpVerificationReducer(initialState, setFlowType());
        expect(state.SelectWalletPanel).toBe(false);
        // flowType === "sameDevice" is the runtime discriminator distinguishing this
        // state from ScanQrCode, since SelectWallet and ScanQrCode share activeScreen === 3.
        expect(state.flowType).toBe("sameDevice");
        expect(state.activeScreen).toBe(VerificationSteps.VERIFY.SelectWallet);
    });

    test("should handle getVpRequest", () => {
        const selectedCredentials = [{ id: "1", type: "Type1", definition: { input_descriptors: [] } }] as any;
        const state = vpVerificationReducer(undefined, getVpRequest({ selectedCredentials }));
        expect(state.activeScreen).toBe(VerificationSteps.VERIFY.ScanQrCode);
        expect(state.selectedCredentials).toHaveLength(1);
        // flowType is unchanged by getVpRequest; "crossDevice" here distinguishes this
        // ScanQrCode state from the SelectWallet state produced by setFlowType, since
        // both steps share the numeric value 3.
        expect(state.flowType).toBe("crossDevice");
    });

    test("should handle verificationSubmissionComplete (full success)", () => {
        const initialState = {
            method: "VERIFY",
            selectedCredentials: [{ id: "1", type: "Type1", definition: { input_descriptors: [] } }],
            originalSelectedCredentials: [{ id: "1", type: "Type1", definition: { input_descriptors: [] } }],
            verificationSubmissionResult: [],
            isPartiallyShared: false,
            flowType: "crossDevice"
        } as any;
        const action = verificationSubmissionComplete({ verificationResult: [] });
        const state = vpVerificationReducer(initialState, action);
        expect(state.isShowResult).toBe(true);
        expect(state.activeScreen).toBe(VerificationSteps.VERIFY.DisplayResult);
    });

    test("should handle resetVpRequest", () => {
        const initialState = { sdkInstanceKey: 5 } as any;
        const state = vpVerificationReducer(initialState, resetVpRequest());
        expect(state.sdkInstanceKey).toBe(6);
        expect(state.method).toBe("VERIFY");
    });
});
