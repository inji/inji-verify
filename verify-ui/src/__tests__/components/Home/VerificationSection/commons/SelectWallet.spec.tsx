import React from "react";
import { fireEvent, render, screen } from "@testing-library/react";
import SelectWallet from "../../../../../components/Home/VerificationSection/commons/SelectWallet";
import { getWebWallets, isMobileDevice } from "../../../../../utils/config";

const dispatch = jest.fn();

jest.mock("../../../../../redux/hooks", () => ({
  useAppDispatch: () => dispatch,
}));
jest.mock("../../../../../redux/features/verification/verification.selector", () => ({
  useVerifyFlowSelector: (selector: any) => selector({
    dcqlQuery: { credentials: [{ id: "credential" }] },
    selectedCredentials: [{ id: "selected" }],
  }),
}));
jest.mock("../../../../../utils/config", () => ({
  getWebWallets: jest.fn(),
  isMobileDevice: jest.fn(),
}));
jest.mock("../../../../../utils/theme-utils", () => ({ SearchIcon: () => <span /> }));
jest.mock("../../../../../components/Home/VerificationProgressTracker/MobileStepper", () => () => <div />);
jest.mock("react-i18next", () => ({ useTranslation: () => ({ t: (value: string) => value }) }));
jest.mock("../../../../../redux/features/verify/vpVerificationState", () => ({
  resetVpRequest: () => ({ type: "reset" }),
  setFlowType: () => ({ type: "flow" }),
  setSelectedWallet: (payload: unknown) => ({ type: "wallet", payload }),
  getVpRequest: (payload: unknown) => ({ type: "request", payload }),
}));

const wallets = [
  { id: "one", name: "Alpha Wallet", iconUrl: "/alpha.svg", walletBaseUrl: "https://alpha" },
  { id: "two", name: "Beta Wallet", iconUrl: "/beta.svg", walletBaseUrl: "https://beta" },
];

describe("SelectWallet", () => {
  beforeEach(() => {
    dispatch.mockClear();
    (getWebWallets as jest.Mock).mockReturnValue(wallets);
    (isMobileDevice as jest.Mock).mockReturnValue(false);
  });

  test("filters wallets, selects one, and proceeds", () => {
    render(<SelectWallet />);
    expect(screen.getByText("Alpha Wallet")).toBeInTheDocument();
    fireEvent.change(screen.getByPlaceholderText("walletSearchPlaceholder"), {
      target: { value: "beta" },
    });
    expect(screen.queryByText("Alpha Wallet")).not.toBeInTheDocument();
    fireEvent.click(screen.getByText("Beta Wallet"));
    fireEvent.click(screen.getByRole("button", { name: "walletSelectorProceed" }));
    expect(dispatch).toHaveBeenCalledWith({
      type: "wallet",
      payload: { walletId: "two", walletBaseUrl: "https://beta" },
    });
    expect(dispatch).toHaveBeenCalledWith({ type: "flow" });
    expect(dispatch).toHaveBeenCalledWith({
      type: "request",
      payload: { selectedCredentials: [{ id: "selected" }] },
    });
  });

  test("shows no-results text and handles cancel", () => {
    render(<SelectWallet />);
    fireEvent.change(screen.getByPlaceholderText("walletSearchPlaceholder"), {
      target: { value: "missing" },
    });
    expect(screen.getByText("walletSearchNoResults")).toBeInTheDocument();
    fireEvent.click(screen.getByRole("button", { name: "walletSelectorCancel" }));
    expect(dispatch).toHaveBeenCalledWith({ type: "reset" });
  });

  test("renders the mobile slide modal and supports back", () => {
    (isMobileDevice as jest.Mock).mockReturnValue(true);
    render(<SelectWallet />);
    fireEvent.click(screen.getByRole("button", { name: "Go back" }));
    expect(dispatch).toHaveBeenCalledWith({ type: "reset" });
  });
});
