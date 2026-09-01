import React from "react";
import { render, screen, fireEvent, waitFor } from "@testing-library/react";
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
        requestFailedGeneric: { message: "Generic request failed" },
    })),
    getWalletErrorAlert: jest.fn((code?: string) =>
        code === "access_denied"
            ? { message: "Wallet access denied", severity: "error" }
            : undefined
    ),
}));

jest.mock("@injistack/react-inji-verify-sdk", () => {
    const React = require("react");
    return {
        OpenID4VPVerification: (props: any) =>
            React.createElement(
                "div",
                {
                    "data-testid": "openid-verification-sdk",
                    "data-client-id": props.clientId,
                    "data-enable-dc-api": String(!!props.enableDcApi),
                    "data-web-wallet-base-url": props.webWalletBaseUrl ?? "",
                    onClick: () => {
                        props.onVPProcessed?.([
                            {
                                vc: { type: ["VerifiableCredential", "TestCredential"] },
                                verificationResponse: {
                                    vcResults: [
                                        {
                                            vc: { type: ["VerifiableCredential", "TestCredential"] },
                                            vcStatus: "SUCCESS",
                                        },
                                    ],
                                    vpResultStatus: "SUCCESS",
                                },
                            },
                        ]);
                    },
                    onDoubleClick: () => {
                        props.onError?.({
                            errorCode: "access_denied",
                            errorMessage: "user cancelled",
                            transactionId: "tx-1",
                        });
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
                dcqlQuery: { credentials: [] },
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

    test("disables enableDcApi when a web wallet base URL is selected", () => {
        Object.assign((window as any)._env_, { ENABLE_DC_API: "true" });
        mockState({
            flowType: "sameDevice",
            selectedWalletBaseUrl: "https://wallet.example.org",
        });
        render(<VpVerification />);
        const sdk = screen.getByTestId("openid-verification-sdk");
        expect(sdk).toHaveAttribute("data-enable-dc-api", "false");
        expect(sdk).toHaveAttribute(
            "data-web-wallet-base-url",
            "https://wallet.example.org"
        );
    });

    test("enables enableDcApi on same-device when no web wallet is selected", () => {
        Object.assign((window as any)._env_, { ENABLE_DC_API: "true" });
        mockState({
            flowType: "sameDevice",
            selectedWalletBaseUrl: undefined,
        });
        render(<VpVerification />);
        expect(screen.getByTestId("openid-verification-sdk")).toHaveAttribute(
            "data-enable-dc-api",
            "true"
        );
    });

    test("passes CLIENT_ID_X509 when selected credential uses x509_san_dns", () => {
        mockState({
            flowType: "crossDevice",
            selectedCredentials: [{
                name: "EU Personal ID (SD-JWT)",
                clientIdPrefix: "x509_san_dns",
                dcqlQuery: { credentials: [] },
            }],
        });
        Object.assign((window as any)._env_, {
            CLIENT_ID: "pre_registered:client",
            CLIENT_ID_DID: "decentralized_identifier:did:web:test.example.com",
            CLIENT_ID_X509: "x509_san_dns:test.example.com",
        });

        render(<VpVerification />);
        expect(screen.getByTestId("openid-verification-sdk")).toHaveAttribute(
            "data-client-id",
            "x509_san_dns:test.example.com"
        );
    });

    test("handles SDK processed event", async () => {
        mockState({ isShowResult: false, flowType: "crossDevice" });
        render(<VpVerification />);

        fireEvent.click(screen.getByTestId("openid-verification-sdk"));

        await waitFor(() =>
            expect(mockDispatch).toHaveBeenCalledWith(expect.objectContaining({
                type: "vpVerification/verificationSubmissionComplete"
            }))
        );
    });

    test("maps wallet access_denied error to localized alert message", async () => {
        const { raiseAlert } = jest.requireMock("../../../../redux/features/alerts/alerts.slice");
        raiseAlert.mockImplementation((payload: any) => ({ type: "alerts/raiseAlert", payload }));

        mockState({ isShowResult: false, flowType: "crossDevice" });
        render(<VpVerification />);

        fireEvent.doubleClick(screen.getByTestId("openid-verification-sdk"));

        await waitFor(() =>
            expect(raiseAlert).toHaveBeenCalledWith(
                expect.objectContaining({
                    message: "Wallet access denied",
                    errorCode: "access_denied",
                    errorReason: "user cancelled",
                    open: true,
                })
            )
        );
    });
});
