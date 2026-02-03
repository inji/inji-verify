import { render, screen, fireEvent, waitFor } from "@testing-library/react";
import { VpVerification } from "../../../../components/Home/VerificationSection/VpVerification";
import { useVerifyFlowSelector } from "../../../../redux/features/verification/verification.selector";
import { useAppDispatch } from "../../../../redux/hooks";
import { VCShareType } from "../../../../types/data-types";

// ---- Mocks ----
const mockDispatch = jest.fn();
jest.mock("../../../../redux/hooks", () => ({
    useAppDispatch: () => mockDispatch,
}));

jest.mock("../../../../redux/features/verification/verification.selector", () => ({
    useVerifyFlowSelector: jest.fn(),
}));

jest.mock("../../../../redux/features/verify/vpVerificationState", () => {
    const getVpRequest = jest.fn(() => ({ type: "vpVerification/getVpRequest" }));
    const resetVpRequest = jest.fn(() => ({ type: "vpVerification/resetVpRequest" }));
    const setSelectCredential = jest.fn(() => ({ type: "vpVerification/setSelectCredential" }));
    const verificationSubmissionComplete = jest.fn(() => ({ type: "vpVerification/verificationSubmissionComplete" }));

    (getVpRequest as any).type = "vpVerification/getVpRequest";
    (resetVpRequest as any).type = "vpVerification/resetVpRequest";
    (setSelectCredential as any).type = "vpVerification/setSelectCredential";
    (verificationSubmissionComplete as any).type = "vpVerification/verificationSubmissionComplete";

    return {
        getVpRequest,
        resetVpRequest,
        setSelectCredential,
        verificationSubmissionComplete,
    };
});

jest.mock("../../../../redux/features/alerts/alerts.slice", () => ({
    closeAlert: jest.fn(),
    raiseAlert: jest.fn(),
}));

jest.mock("../../../../utils/config", () => ({
    AlertMessages: jest.fn(() => ({
        sessionExpired: { title: "Session Expired" },
        incorrectCredential: { title: "Incorrect Credential" },
    })),
    VerificationSteps: {
        VERIFY: { DisplayResult: 3, InitiateVpRequest: 1 }
    }
}));

jest.mock("@injistack/react-inji-verify-sdk", () => ({
    OpenID4VPVerification: (props: any) => (
        <div
            data-testid="openid-verification-sdk"
            onClick={() => props.onVPProcessed?.([])}
        >
            SDK MOCK
        </div>
    ),
}));

jest.mock("../../../../utils/theme-utils", () => ({
    QrIcon: (props: any) => <div data-testid="qr-icon" {...props} />,
}));

jest.mock("react-i18next", () => ({
    useTranslation: () => ({
        t: (key: string) => key,
    }),
}));

describe("VpVerification Component", () => {
    beforeEach(() => {
        jest.clearAllMocks();
    });

    const mockState = (overrides = {}) => {
        (useVerifyFlowSelector as any).mockImplementation((selector: any) =>
            selector({
                isLoading: false,
                sharingType: VCShareType.SINGLE,
                selectedClaims: [],
                verificationSubmissionResult: [],
                unVerifiedClaims: [],
                presentationDefinition: { input_descriptors: [] },
                activeScreen: 1,
                isShowResult: false,
                flowType: "crossDevice",
                sdkInstanceKey: "key",
                ...overrides
            })
        );
    };

    test("renders Loader when isLoading is true", () => {
        mockState({ isLoading: true });
        render(<VpVerification />);
        expect(screen.getByTestId("loader")).toBeInTheDocument();
    });

    test("renders VpSubmissionResult when isShowResult is true", () => {
        mockState({ isShowResult: true });
        render(<VpVerification />);
        expect(screen.getByText("VpSubmissionResult Mock")).toBeInTheDocument();
    });

    test("renders SDK when flowType is crossDevice", () => {
        mockState({ flowType: "crossDevice" });
        render(<VpVerification />);
        expect(screen.getByTestId("openid-verification-sdk")).toBeInTheDocument();
    });

    test("renders SDK when flowType is sameDevice", () => {
        mockState({ flowType: "sameDevice" });
        render(<VpVerification />);
        expect(screen.getByTestId("openid-verification-sdk")).toBeInTheDocument();
    });

    test("handles SDK processed event", async () => {
        mockState({ isShowResult: false, flowType: "crossDevice" });
        render(<VpVerification />);
        const sdkElement = screen.getByTestId("openid-verification-sdk");
        fireEvent.click(sdkElement);

        await waitFor(() => {
            expect(mockDispatch).toHaveBeenCalledWith(expect.objectContaining({
                type: "vpVerification/verificationSubmissionComplete"
            }));
        });
    });
});

jest.mock("../../../../components/Home/VerificationSection/Result/VpSubmissionResult", () => () => <div>VpSubmissionResult Mock</div>);
jest.mock("../../../../components/commons/Loader", () => () => <div data-testid="loader">Loader Mock</div>);
