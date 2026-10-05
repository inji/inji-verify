import React from "react";
import { fireEvent, render, screen } from "@testing-library/react";
import SelectionPanelContent from "../../../../../components/Home/VerificationSection/commons/SelectionPanelContent";
import { getVerifiableClaims, isMobileDevice } from "../../../../../utils/config";

const dispatch = jest.fn();
const claims = [
  {
    id: "one",
    type: "TypeOne",
    name: "Alpha Credential",
    logo: "/alpha.svg",
    essential: true,
    dcqlQuery: { credentials: [{ id: "a", format: "ldp_vc", meta: {} }] },
  },
  {
    id: "two",
    type: "TypeTwo",
    name: "Beta Credential",
    logo: "/beta.svg",
    essential: false,
    dcqlQuery: { credentials: [{ id: "b", format: "ldp_vc", meta: {} }] },
  },
];

jest.mock("../../../../../redux/hooks", () => ({
  useAppDispatch: () => dispatch,
  useAppSelector: (selector: any) => selector({ common: { language: "en" } }),
}));
jest.mock("../../../../../redux/features/verification/verification.selector", () => ({
  useVerifyFlowSelector: (selector: any) => selector({
    selectedCredentials: [claims[0]],
    dcqlQuery: { credentials: [{ id: "a" }] },
  }),
}));
jest.mock("../../../../../utils/config", () => ({
  getVerifiableClaims: jest.fn(),
  isMobileDevice: jest.fn(),
}));
jest.mock("../../../../../utils/i18n", () => ({ isRTL: () => false }));
jest.mock("../../../../../utils/storage", () => ({
  storage: { setItem: jest.fn(), ESSENTIAL_CLAIM: "essential" },
}));
jest.mock("../../../../../utils/theme-utils", () => ({
  SearchIcon: () => <span />,
  FilterLinesIcon: () => <span />,
}));
jest.mock("../../../../../redux/features/verify/vpVerificationState", () => ({
  getVpRequest: (payload: unknown) => ({ type: "request", payload }),
  resetVpRequest: () => ({ type: "reset" }),
  setFlowType: () => ({ type: "flow" }),
  setSelectedCredentials: (payload: unknown) => ({ type: "selected", payload }),
  setShowWalletSelector: () => ({ type: "wallet" }),
}));
jest.mock("react-i18next", () => ({ useTranslation: () => ({ t: (value: string) => value }) }));

describe("SelectionPanelContent", () => {
  beforeEach(() => {
    dispatch.mockClear();
    (getVerifiableClaims as jest.Mock).mockReturnValue(claims);
    (isMobileDevice as jest.Mock).mockReturnValue(false);
    const trigger = document.createElement("button");
    trigger.id = "OpenID4VPVerification_trigger";
    document.body.appendChild(trigger);
  });

  afterEach(() => {
    document.getElementById("OpenID4VPVerification_trigger")?.remove();
  });

  test("searches, sorts, toggles credentials, and generates a request", () => {
    render(<SelectionPanelContent />);
    expect(screen.getByTestId("ItemBox-Text-0")).toHaveTextContent("Alpha Credential");
    fireEvent.change(screen.getByPlaceholderText("searchPlaceholder"), {
      target: { value: "beta" },
    });
    expect(screen.getByTestId("ItemBox-Text-0")).toHaveTextContent("Beta Credential");
    fireEvent.click(screen.getByRole("checkbox", { name: /Beta Credential/ }));
    expect(dispatch).toHaveBeenCalledWith({
      type: "selected",
      payload: { selectedCredentials: [claims[0], claims[1]] },
    });
    fireEvent.click(screen.getByRole("button", { name: "sortBy" }));
    fireEvent.click(screen.getByRole("menuitem", { name: "sortZtoA" }));
    fireEvent.click(screen.getByRole("button", { name: "generateQrCodeBtn" }));
    expect(dispatch).toHaveBeenCalledWith({
      type: "request",
      payload: { selectedCredentials: [claims[0]] },
    });
  });

  test("selects a credential when its card text is clicked", () => {
    render(<SelectionPanelContent />);

    fireEvent.click(screen.getByTestId("ItemBox-Text-1"));

    expect(dispatch).toHaveBeenCalledWith({
      type: "selected",
      payload: { selectedCredentials: [claims[0], claims[1]] },
    });
  });

  test("supports wallet, back, and mobile-wallet actions", () => {
    (isMobileDevice as jest.Mock).mockReturnValue(true);
    render(<SelectionPanelContent />);
    fireEvent.click(screen.getByRole("button", { name: "Common:Button.openWallet" }));
    fireEvent.click(screen.getByRole("button", { name: "openWebWallets" }));
    fireEvent.click(screen.getByRole("button", { name: "goBack" }));
    expect(dispatch).toHaveBeenCalledWith({ type: "flow" });
    expect(dispatch).toHaveBeenCalledWith({ type: "wallet" });
    expect(dispatch).toHaveBeenCalledWith({ type: "reset" });
  });
});
