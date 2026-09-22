import React from "react";
import { fireEvent, render, screen } from "@testing-library/react";
import { Verify } from "../../pages/Verify";

const mockDispatch = jest.fn();
let mockVerifyState: any;

jest.mock("../../redux/hooks", () => ({
  useAppDispatch: () => mockDispatch,
}));

jest.mock("../../redux/features/verification/verification.selector", () => ({
  useVerifyFlowSelector: (selector: (state: any) => any) => selector(mockVerifyState),
}));

jest.mock("react-i18next", () => ({
  initReactI18next: { type: "3rdParty", init: jest.fn() },
  useTranslation: () => ({ t: (key: string) => key }),
}));

jest.mock("../../components/Home/VerificationProgressTracker", () => () => (
  <div data-testid="progress-tracker" />
));

jest.mock("../../components/Home/VerificationSection/VpVerification", () => ({
  VpVerification: () => <div data-testid="vp-verification" />,
}));

jest.mock("../../components/Home/VerificationSection/commons/SelectionPanel", () => () => (
  <div data-testid="selection-panel" />
));

jest.mock("../../components/Home/VerificationSection/commons/SelectWallet", () => () => (
  <div data-testid="select-wallet" />
));

describe("Verify page", () => {
  beforeEach(() => {
    mockDispatch.mockClear();
    mockVerifyState = {
      SelectionPanel: false,
      SelectWalletPanel: false,
      unVerifiedCredentials: [],
      activeScreen: 1,
    };
  });

  test("renders the request-credentials action", () => {
    render(<Verify />);
    expect(screen.getByTestId("progress-tracker")).toBeInTheDocument();
    expect(screen.getByTestId("vp-verification")).toBeInTheDocument();
    expect(screen.getByRole("button", { name: "rqstButton" })).toBeInTheDocument();
  });

  test("dispatches when requesting credentials", () => {
    render(<Verify />);
    fireEvent.click(screen.getByRole("button", { name: "rqstButton" }));
    expect(mockDispatch).toHaveBeenCalled();
  });

  test("renders missing-credentials and restart actions", () => {
    mockVerifyState.unVerifiedCredentials = [{}];
    render(<Verify />);
    expect(screen.getByRole("button", { name: "missingCredentials" })).toBeInTheDocument();
    expect(screen.getByRole("button", { name: "restartProcess" })).toBeInTheDocument();
  });
});
