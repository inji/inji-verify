import vpVerificationReducer, {
    setSelectCredential,
    setSelectedCredentials,
    setFlowType,
    getVpRequest,
    verificationSubmissionComplete,
    resetVpRequest
} from "../../../../redux/features/verify/vpVerificationState";
import { VCShareType } from "../../../../types/data-types";
import {getVerifiableClaims, VerificationSteps} from "../../../../utils/config";
import {calculateUnverifiedClaims, calculateVerifiedClaims, getCredentialType} from "../../../../utils/commonUtils";

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
    getCredentialType: jest.fn((vc) =>
        Array.isArray(vc?.type) ? vc.type[0] : vc?.type || "unknown"
    ),
}));

describe("vpVerification slice", () => {
    test("should handle setSelectedCredentials", () => {
        const selectedCredentials = [
            {
                id: "2",
                type: "Type2",
                essential: false,
                definition: {
                    input_descriptors: [{ id: "desc2" }],
                },
            },
        ] as any;

        const initialState = {
            ...vpVerificationReducer(undefined, { type: "@@INIT" }),
            selectedCredentials: [],
            originalSelectedCredentials: [],
            unVerifiedCredentials: [],
            presentationDefinition: {
                id: "test",
                input_descriptors: [{ id: "desc1" }, { id: "desc2" }],
            },
        } as any;

        const state = vpVerificationReducer(
            initialState,
            setSelectedCredentials({ selectedCredentials })
        );

        expect(state.selectedCredentials).toHaveLength(1);
        expect(state.sharingType).toBe(VCShareType.SINGLE);
    });

    test("should handle setSelectCredential with SelectWalletPanel open", () => {
        (getVerifiableClaims as jest.Mock).mockReturnValue([
            {
                id: "1",
                type: "Type1",
                essential: true,
                definition: {
                    input_descriptors: [{ id: "desc1" }],
                },
            },
            {
                id: "2",
                type: "Type2",
                essential: true,
                definition: {
                    input_descriptors: [{ id: "desc2" }],
                },
            },
        ]);

        const baseState = vpVerificationReducer(undefined, { type: "@@INIT" });
        const walletState = vpVerificationReducer(baseState, setFlowType());

        const preparedState = {
            ...walletState,
            method: "VERIFY",
            selectedCredentials: [],
            originalSelectedCredentials: [],
            verificationSubmissionResult: [],
            unVerifiedCredentials: [],
            presentationDefinition: {
                id: "test",
                input_descriptors: [{ id: "desc1" }, { id: "desc2" }],
            },
        } as any;

        const state = vpVerificationReducer(preparedState, setSelectCredential());

        expect(state.SelectionPanel).toBe(true);
        expect(state.SelectWalletPanel).toBe(false);
        expect(state.flowType).toBe("sameDevice");
        expect(state.activeScreen).toBe(VerificationSteps.VERIFY.SelectCredential);
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
        const selectedCredentials = [
            {
                id: "1",
                type: "Type1",
                essential: true,
                definition: { input_descriptors: [{ id: "desc1" }] },
            },
        ] as any;

        const initialState = {
            ...vpVerificationReducer(undefined, { type: "@@INIT" }),
            presentationDefinition: {
                id: "test",
                input_descriptors: [{ id: "desc1" }],
            },
        } as any;

        const state = vpVerificationReducer(
            initialState,
            getVpRequest({ selectedCredentials })
        );

        expect(state.activeScreen).toBe(VerificationSteps.VERIFY.ScanQrCode);
        expect(state.selectedCredentials).toHaveLength(1);
        expect(state.flowType).toBe("crossDevice");
    });

    test("should handle verificationSubmissionComplete (full success)", () => {
        (calculateVerifiedClaims as jest.Mock).mockReturnValue([
            {
                credentialId: "1",
                credentialType: "Type1",
                vc: {
                    id: "1",
                    type: ["VerifiableCredential", "Type1"],
                },
                vcStatus: "SUCCESS",
            },
        ]);

        (calculateUnverifiedClaims as jest.Mock).mockReturnValue([]);

        (getCredentialType as jest.Mock).mockImplementation((vc: any) => {
            return vc?.type?.[1] || vc?.type || "";
        });

        const initialState = {
            ...vpVerificationReducer(undefined, { type: "@@INIT" }),
            method: "VERIFY",
            selectedCredentials: [
                {
                    id: "1",
                    type: "Type1",
                    essential: true,
                    definition: { input_descriptors: [{ id: "desc1" }] },
                },
            ],
            originalSelectedCredentials: [
                {
                    id: "1",
                    type: "Type1",
                    essential: true,
                    definition: { input_descriptors: [{ id: "desc1" }] },
                },
            ],
            verificationSubmissionResult: [],
            unVerifiedCredentials: [],
            isPartiallyShared: false,
            flowType: "crossDevice",
            presentationDefinition: {
                id: "test",
                input_descriptors: [{ id: "desc1" }],
            },
        } as any;

        const action = verificationSubmissionComplete({
            verificationResult: [
                {
                    credentialId: "1",
                    credentialType: "Type1",
                    vc: {
                        id: "1",
                        type: ["VerifiableCredential", "Type1"],
                    },
                    vcStatus: "SUCCESS",
                },
            ],
        } as any);

        const state = vpVerificationReducer(initialState, action);

        expect(state.isShowResult).toBe(true);
        expect(state.isPartiallyShared).toBe(false);
        expect(state.unVerifiedCredentials).toEqual([]);
        expect(state.activeScreen).toBe(VerificationSteps.VERIFY.DisplayResult);
        expect(state.verificationSubmissionResult).toEqual([
            {
                credentialId: "1",
                credentialType: "Type1",
                vc: {
                    id: "1",
                    type: ["VerifiableCredential", "Type1"],
                },
                vcStatus: "SUCCESS",
            },
        ]);
    });

    test("should handle resetVpRequest", () => {
        const initialState = { sdkInstanceKey: 5 } as any;
        const state = vpVerificationReducer(initialState, resetVpRequest());
        expect(state.sdkInstanceKey).toBe(6);
        expect(state.method).toBe("VERIFY");
    });
});
