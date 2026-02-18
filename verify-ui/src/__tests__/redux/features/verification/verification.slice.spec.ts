import verificationReducer, {
    qrReadInit,
    verificationInit,
    verificationComplete,
    goToHomeScreen,
    selectMethod,
    PreloadedState
} from "../../../../redux/features/verification/verification.slice";
import { VerificationSteps } from "../../../../utils/config";

describe("verification slice", () => {
    test("should return logical initial state", () => {
        expect(verificationReducer(undefined, { type: "unknown" })).toEqual(PreloadedState);
    });

    test("should handle selectMethod", () => {
        const initialState = { ...PreloadedState, method: "SCAN" as const };
        const action = selectMethod({ method: "UPLOAD" });
        const state = verificationReducer(initialState, action);
        expect(state.method).toBe("UPLOAD");
    });

    test("should handle qrReadInit for SCAN", () => {
        const initialState = { ...PreloadedState, method: "SCAN" as const };
        const action = qrReadInit({ method: "SCAN" });
        const state = verificationReducer(initialState, action);
        expect(state.activeScreen).toBe(VerificationSteps.SCAN.ActivateCamera);
        expect(state.method).toBe("SCAN");
    });

    test("should handle qrReadInit for UPLOAD", () => {
        const initialState = { ...PreloadedState, method: "UPLOAD" as const };
        const action = qrReadInit({ method: "UPLOAD" });
        const state = verificationReducer(initialState, action);
        expect(state.activeScreen).toBe(VerificationSteps.UPLOAD.Verifying);
        expect(state.method).toBe("UPLOAD");
    });

    test("should handle verificationInit", () => {
        const initialState = { ...PreloadedState, method: "SCAN" as const };
        const action = verificationInit({ qrReadResult: { status: "SUCCESS" }, other: "data" });
        const state = verificationReducer(initialState, action);
        expect(state.activeScreen).toBe(VerificationSteps.SCAN.Verifying);
        expect(state.qrReadResult!.status).toBe("SUCCESS");
        expect(state.ovp).toHaveProperty("other", "data");
    });

    test("should handle verificationComplete", () => {
        const initialState = { ...PreloadedState, method: "SCAN" as const };
        const action = verificationComplete({ verificationResult: "result" });
        const state = verificationReducer(initialState, action);
        expect(state.activeScreen).toBe(VerificationSteps.SCAN.DisplayResult);
        expect(state.verificationResult).toBe("result");
    });

    test("should handle goToHomeScreen for VERIFY", () => {
        const initialState = { ...PreloadedState, method: "VERIFY" as const, activeScreen: 4 };
        const action = goToHomeScreen({ method: "VERIFY" });
        const state = verificationReducer(initialState, action);
        expect(state.activeScreen).toBe(VerificationSteps.VERIFY.InitiateVpRequest);
        expect(state.qrReadResult!.status).toBe("NOT_READ");
    });

    test("should handle goToHomeScreen for SCAN (default)", () => {
        const initialState = { ...PreloadedState, method: "SCAN" as const, activeScreen: 4 };
        const action = goToHomeScreen({});
        const state = verificationReducer(initialState, action);
        expect(state.activeScreen).toBe(VerificationSteps.SCAN.QrCodePrompt);
        expect(state.qrReadResult!.status).toBe("NOT_READ");
    });
});
