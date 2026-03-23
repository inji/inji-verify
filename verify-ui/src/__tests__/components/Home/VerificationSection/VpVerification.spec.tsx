import React, { act } from "react";
import { render, screen, fireEvent } from "@testing-library/react";
import { VpVerification } from "../../../../components/Home/VerificationSection/VpVerification";
import { useVerifyFlowSelector } from "../../../../redux/features/verification/verification.selector";
import { VCShareType } from "../../../../types/data-types";

jest.mock("iso-639-3", () => ({
    iso6393: [],
}));

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
    const showMissingCredentialOptions = jest.fn(() => ({ type: "vpVerification/showMissingCredentialOptions" }));

    (getVpRequest as any).type = "vpVerification/getVpRequest";
    (resetVpRequest as any).type = "vpVerification/resetVpRequest";
    (setSelectCredential as any).type = "vpVerification/setSelectCredential";
    (verificationSubmissionComplete as any).type = "vpVerification/verificationSubmissionComplete";
    (showMissingCredentialOptions as any).type = "vpVerification/showMissingCredentialOptions";

    return {
        getVpRequest,
        resetVpRequest,
        setSelectCredential,
        verificationSubmissionComplete,
        showMissingCredentialOptions,
        OVP_SESSION_SELECTED_CREDENTIALS_KEY: "ovp_selectedCredentials",
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
}));

jest.mock("@injistack/react-inji-verify-sdk", () => {
    const React = require("react");
    return {
        OpenID4VPVerification: (props: any) =>
            React.createElement(
                "div",
                {
                    "data-testid": "openid-verification-sdk",
                    onClick: () => {
                        props.onVPProcessed?.([]);
                    },
                },
                "SDK MOCK"
            ),
    };
});

jest.mock("../../../../utils/theme-utils", () => {
    const React = require("react");
    return {
        QrIcon: (props: any) =>
            React.createElement("div", {
                "data-testid": "qr-icon",
                ...props,
            }),
    };
});

jest.mock(
    "../../../../components/Home/VerificationSection/Result/VpSubmissionResult",
    () => {
        const React = require("react");
        return {
            __esModule: true,
            default: () =>
                React.createElement("div", null, "VpSubmissionResult Mock"),
        };
    }
);

jest.mock("../../../../components/commons/Loader", () => {
    const React = require("react");
    return {
        __esModule: true,
        default: () =>
            React.createElement(
                "div",
                { "data-testid": "loader" },
                "Loader Mock"
            ),
    };
});

const {verificationSubmissionComplete: mockVerificationSubmissionComplete} = jest.requireMock("../../../../redux/features/verify/vpVerificationState") as any;

describe("VpVerification Component", () => {
    beforeEach(() => {
        mockDispatch.mockClear();
        (useVerifyFlowSelector as any).mockClear();
        mockVerificationSubmissionComplete.mockImplementation(() => ({
            type: "vpVerification/verificationSubmissionComplete"}));
    });

    const mockState = (overrides = {}) => {
        (useVerifyFlowSelector as any).mockImplementation((selector: any) =>
            selector({
                isLoading: false,
                sharingType: VCShareType.SINGLE,
                selectedCredentials: [],
                originalSelectedCredentials: [],
                verificationSubmissionResult: [],
                unVerifiedCredentials: [],
                presentationDefinition: { input_descriptors: [] },
                activeScreen: 1,
                isShowResult: false,
                flowType: "crossDevice",
                SelectWalletPanel: false,
                selectedWalletBaseUrl: undefined,
                sdkInstanceKey: "key",
                ...overrides,
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

        await act(async () => {
            fireEvent.click(screen.getByTestId("openid-verification-sdk"));
        });

        expect(mockDispatch).toHaveBeenCalledWith(expect.objectContaining({
            type: "vpVerification/verificationSubmissionComplete"
        }));
    });
});
