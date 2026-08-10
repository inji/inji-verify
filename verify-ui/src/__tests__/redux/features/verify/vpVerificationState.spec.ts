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
import {calculateUnverifiedClaims} from "../../../../utils/commonUtils";

const mockDcqlQuery = {
    credentials: [{ id: "desc1", format: "dc+sd-jwt", meta: {} }],
};

jest.mock("../../../../utils/config", () => ({
    ...jest.requireActual("../../../../utils/config"),
    getVerifiableClaims: jest.fn(() => [
        { id: "1", type: "Type1", essential: true, dcqlQuery: mockDcqlQuery },
        { id: "2", type: "Type2", essential: false, dcqlQuery: mockDcqlQuery },
    ])
}));

jest.mock("../../../../utils/commonUtils", () => ({
    calculateUnverifiedClaims: jest.fn(() => []),
}));

describe("vpVerification slice", () => {
    test("should handle setSelectedCredentials", () => {
        const selectedCredentials = [
            {
                id: "2",
                type: "Type2",
                essential: false,
                dcqlQuery: {
                    credentials: [{ id: "desc2", format: "dc+sd-jwt", meta: {} }],
                },
            },
        ] as any;

        const initialState = {
            ...vpVerificationReducer(undefined, { type: "@@INIT" }),
            selectedCredentials: [],
            originalSelectedCredentials: [],
            unVerifiedCredentials: [],
            dcqlQuery: mockDcqlQuery,
        } as any;

        const state = vpVerificationReducer(
            initialState,
            setSelectedCredentials({ selectedCredentials })
        );

        expect(state.selectedCredentials).toHaveLength(1);
        expect(state.sharingType).toBe(VCShareType.SINGLE);
    });

    test("should merge credential_sets from selected credentials into dcqlQuery", () => {
        const firstCredentialSets = [
            {
                options: [["mosip_verifiable_credential_id"], ["life_insurance_credential_id"]],
            },
        ];
        const secondCredentialSets = [
            {
                options: [["health_insurance_credential_id"]],
            },
        ];

        const selectedCredentials = [
            {
                id: "1",
                type: "Type1",
                essential: true,
                dcqlQuery: {
                    credentials: [{ id: "mosip_verifiable_credential_id", format: "ldp_vc", meta: {} }],
                    credential_sets: firstCredentialSets,
                },
            },
            {
                id: "2",
                type: "Type2",
                essential: false,
                dcqlQuery: {
                    credentials: [{ id: "life_insurance_credential_id", format: "ldp_vc", meta: {} }],
                    credential_sets: secondCredentialSets,
                },
            },
        ] as any;

        const initialState = {
            ...vpVerificationReducer(undefined, { type: "@@INIT" }),
            selectedCredentials: [],
            originalSelectedCredentials: [],
            unVerifiedCredentials: [],
            dcqlQuery: mockDcqlQuery,
        } as any;

        const state = vpVerificationReducer(
            initialState,
            setSelectedCredentials({ selectedCredentials })
        );

        expect(state.dcqlQuery.credentials).toHaveLength(2);
        expect(state.dcqlQuery.credential_sets).toEqual([
            ...firstCredentialSets,
            ...secondCredentialSets,
        ]);
    });

    test("should omit credential_sets from dcqlQuery when none of the selected credentials define it", () => {
        const selectedCredentials = [
            {
                id: "2",
                type: "Type2",
                essential: false,
                dcqlQuery: {
                    credentials: [{ id: "desc2", format: "dc+sd-jwt", meta: {} }],
                },
            },
        ] as any;

        const initialState = {
            ...vpVerificationReducer(undefined, { type: "@@INIT" }),
            selectedCredentials: [],
            originalSelectedCredentials: [],
            unVerifiedCredentials: [],
            dcqlQuery: mockDcqlQuery,
        } as any;

        const state = vpVerificationReducer(
            initialState,
            setSelectedCredentials({ selectedCredentials })
        );

        expect(state.dcqlQuery).not.toHaveProperty("credential_sets");
    });

    test("should handle setSelectCredential with SelectWalletPanel open", () => {
        (getVerifiableClaims as jest.Mock).mockReturnValue([
            {
                id: "1",
                type: "Type1",
                essential: true,
                dcqlQuery: {
                    credentials: [{ id: "desc1", format: "dc+sd-jwt", meta: {} }],
                },
            },
            {
                id: "2",
                type: "Type2",
                essential: true,
                dcqlQuery: {
                    credentials: [{ id: "desc2", format: "dc+sd-jwt", meta: {} }],
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
            dcqlQuery: mockDcqlQuery,
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
                dcqlQuery: mockDcqlQuery,
            },
        ] as any;

        const initialState = {
            ...vpVerificationReducer(undefined, { type: "@@INIT" }),
            dcqlQuery: mockDcqlQuery,
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
        (calculateUnverifiedClaims as jest.Mock).mockReturnValue([]);

        const verificationResult = [
            {
                vc: {
                    id: "1",
                    type: ["VerifiableCredential", "Type1"],
                },
                vcStatus: "SUCCESS",
            },
        ];

        const initialState = {
            ...vpVerificationReducer(undefined, { type: "@@INIT" }),
            method: "VERIFY",
            selectedCredentials: [
                {
                    id: "1",
                    type: "Type1",
                    essential: true,
                    dcqlQuery: mockDcqlQuery,
                },
            ],
            originalSelectedCredentials: [
                {
                    id: "1",
                    type: "Type1",
                    essential: true,
                    dcqlQuery: mockDcqlQuery,
                },
            ],
            verificationSubmissionResult: [],
            unVerifiedCredentials: [],
            isPartiallyShared: false,
            flowType: "crossDevice",
            dcqlQuery: mockDcqlQuery,
        } as any;

        const action = verificationSubmissionComplete({
            verificationResult,
        } as any);

        const state = vpVerificationReducer(initialState, action);

        expect(state.isShowResult).toBe(true);
        expect(state.isPartiallyShared).toBe(false);
        expect(state.unVerifiedCredentials).toEqual([]);
        expect(state.activeScreen).toBe(VerificationSteps.VERIFY.DisplayResult);
        expect(state.verificationSubmissionResult).toEqual(verificationResult);
    });

    test("should append all service credentials without deduplicating by type", () => {
        (calculateUnverifiedClaims as jest.Mock).mockReturnValue([]);

        const firstSubmission = [
            {
                vc: {
                    id: "1",
                    type: ["VerifiableCredential", "InsuranceCredential"],
                },
                vcStatus: "SUCCESS",
            },
        ];
        const secondSubmission = [
            {
                vc: {
                    id: "2",
                    type: ["VerifiableCredential", "InsuranceCredential"],
                },
                vcStatus: "SUCCESS",
            },
        ];

        const initialState = {
            ...vpVerificationReducer(undefined, { type: "@@INIT" }),
            method: "VERIFY",
            selectedCredentials: [],
            originalSelectedCredentials: [],
            verificationSubmissionResult: firstSubmission,
            unVerifiedCredentials: [],
            isPartiallyShared: false,
            flowType: "crossDevice",
            dcqlQuery: mockDcqlQuery,
        } as any;

        const state = vpVerificationReducer(
            initialState,
            verificationSubmissionComplete({ verificationResult: secondSubmission } as any)
        );

        expect(state.verificationSubmissionResult).toEqual([
            ...firstSubmission,
            ...secondSubmission,
        ]);
    });

    test("should handle resetVpRequest", () => {
        const initialState = { sdkInstanceKey: 5 } as any;
        const state = vpVerificationReducer(initialState, resetVpRequest());
        expect(state.sdkInstanceKey).toBe(6);
        expect(state.method).toBe("VERIFY");
    });
});
