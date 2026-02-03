import vpVerificationReducer, {
    setSelectCredential,
    setSelectedClaims,
    setFlowType,
    getVpRequest,
    verificationSubmissionComplete,
    resetVpRequest
} from "../../../../redux/features/verify/vpVerificationState";
import { VCShareType } from "../../../../types/data-types";
import { VerificationSteps } from "../../../../utils/config";

jest.mock("../../../../utils/config", () => ({
    getVerifiableClaims: jest.fn(() => [
        { id: "1", essential: true, definition: { input_descriptors: [{ id: "desc1" }] } },
        { id: "2", essential: false, definition: { input_descriptors: [{ id: "desc2" }] } }
    ]),
    VerificationSteps: {
        VERIFY: {
            InitiateVpRequest: 1,
            SelectCredential: 2,
            SelectWallet: 3,
            ScanQrCode: 4,
            RequestMissingCredential: 5,
            DisplayResult: 6
        }
    }
}));

jest.mock("../../../../utils/commonUtils", () => ({
    calculateUnverifiedClaims: jest.fn(() => []),
    calculateVerifiedClaims: jest.fn(() => []),
    getCredentialType: jest.fn((vc) => vc?.type || "unknown")
}));

describe("vpVerification slice", () => {
    test("should handle setSelectCredential", () => {
        const state = vpVerificationReducer(undefined, setSelectCredential());
        expect(state.activeScreen).toBe(2);
        expect(state.SelectionPanel).toBe(true);
        expect(state.selectedClaims).toHaveLength(1); // essential only
    });

    test("should handle setSelectedClaims", () => {
        const selectedClaims = [{ id: "2", definition: { input_descriptors: [] } }] as any;
        const state = vpVerificationReducer(undefined, setSelectedClaims({ selectedClaims }));
        expect(state.selectedClaims).toHaveLength(1);
        expect(state.sharingType).toBe(VCShareType.SINGLE);
    });

    test("should handle setFlowType", () => {
        const state = vpVerificationReducer(undefined, setFlowType());
        expect(state.flowType).toBe("sameDevice");
        expect(state.activeScreen).toBe(3);
    });

    test("should handle getVpRequest", () => {
        const selectedClaims = [{ id: "1", definition: { input_descriptors: [] } }] as any;
        const state = vpVerificationReducer(undefined, getVpRequest({ selectedClaims }));
        expect(state.activeScreen).toBe(4);
        expect(state.selectedClaims).toHaveLength(1);
    });

    test("should handle verificationSubmissionComplete (full success)", () => {
        const initialState = {
            method: "VERIFY",
            selectedClaims: [{ id: "1", definition: { input_descriptors: [] } }],
            originalSelectedClaims: [{ id: "1", definition: { input_descriptors: [] } }],
            verificationSubmissionResult: [],
            isPartiallyShared: false,
            flowType: "crossDevice"
        } as any;
        const action = verificationSubmissionComplete({ verificationResult: [] });
        const state = vpVerificationReducer(initialState, action);
        expect(state.isShowResult).toBe(true);
        expect(state.activeScreen).toBe(6);
    });

    test("should handle resetVpRequest", () => {
        const initialState = { sdkInstanceKey: 5 } as any;
        const state = vpVerificationReducer(initialState, resetVpRequest());
        expect(state.sdkInstanceKey).toBe(6);
        expect(state.method).toBe("VERIFY");
    });
});
