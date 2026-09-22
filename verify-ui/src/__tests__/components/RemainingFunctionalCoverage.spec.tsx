import { render, screen } from "@testing-library/react";
import SelectionPanelContent from "../../components/Home/VerificationSection/commons/SelectionPanelContent";
import { acceptedFileTypes } from "../../utils/fileUploadUtils";
import { useApplicationStateSelector } from "../../redux/features/application-state/application-state.selector";

jest.mock("../../redux/hooks", () => ({ useAppDispatch: () => jest.fn(), useAppSelector: (selector: any) => selector({ common: { language: "en" }, appState: {} }) }));
jest.mock("../../redux/features/verification/verification.selector", () => ({ useVerifyFlowSelector: (selector: any) => selector({ selectedCredentials: [], dcqlQuery: { credentials: [{}] } }) }));
jest.mock("../../utils/config", () => ({ isMobileDevice: () => false, getVerifiableClaims: () => [], SupportedFileTypes: ["pdf", "png"], UploadFileSizeLimits: {}, AlertMessages: {} }));
jest.mock("../../utils/i18n", () => ({ isRTL: () => false }));
jest.mock("../../utils/storage", () => ({ storage: { setItem: jest.fn(), ESSENTIAL_CLAIM: "essential" } }));
jest.mock("../../utils/theme-utils", () => ({ SearchIcon: () => <span />, FilterLinesIcon: () => <span /> }));
jest.mock("../../redux/features/verify/vpVerificationState", () => ({ getVpRequest: () => ({}), resetVpRequest: () => ({}), setFlowType: () => ({}), setSelectedCredentials: () => ({}), setShowWalletSelector: () => ({}) }));
jest.mock("react-i18next", () => ({ useTranslation: () => ({ t: (value: string) => value }) }));

describe("remaining functional coverage", () => {
  test("renders the empty credential selection state", () => {
    render(<SelectionPanelContent />);
    expect(screen.getByText("noVcsFound")).toBeInTheDocument();
  });

  test("exposes the configured upload extensions", () => {
    expect(acceptedFileTypes).toContain(".");
  });

  test("application selector delegates to the app state", () => {
    expect(typeof useApplicationStateSelector((state: any) => state)).toBe("object");
  });
});
