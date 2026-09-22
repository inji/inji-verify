import { fireEvent, render, screen } from "@testing-library/react";
import MobileStepper from "../../components/Home/VerificationProgressTracker/MobileStepper";
import SelectWallet from "../../components/Home/VerificationSection/commons/SelectWallet";
import SelectionPanel from "../../components/Home/VerificationSection/commons/SelectionPanel";
import CheckingForInternetConnectivity from "../../components/misc/CheckingForInternetConnectivity";

jest.mock("../../redux/features/verification/verification.selector", () => ({
  useVerificationFlowSelector: (selector: any) => selector({ activeScreen: "SCAN", method: "SCAN" }),
  useVerifyFlowSelector: (selector: any) => selector({ activeScreen: "SCAN", flowType: "sameDevice", isPartiallyShared: false, dcqlQuery: { credentials: [{}] }, selectedCredentials: [] }),
}));
jest.mock("../../utils/misc", () => ({
  convertToId: (value: string) => value.toLowerCase().replace(/\s+/g, "-"),
  fetchVerificationSteps: () => [{ stepNumber: 1, label: "Scan", description: "Scan: your credential", isActive: true, isCompleted: false }],
}));
jest.mock("../../utils/config", () => ({
  isMobileDevice: () => false,
  getWebWallets: () => [],
}));
jest.mock("../../redux/hooks", () => ({ useAppDispatch: () => jest.fn() }));
jest.mock("../../redux/features/verify/vpVerificationState", () => ({ resetVpRequest: () => ({ type: "reset" }), setSelectedWallet: () => ({ type: "wallet" }), setFlowType: () => ({ type: "flow" }), getVpRequest: () => ({ type: "request" }) }));
jest.mock("../../components/Home/VerificationSection/commons/SelectionPanelContent", () => ({ __esModule: true, default: () => <div data-testid="selection-content" /> }));
jest.mock("../../components/commons/LoaderWithBackdrop", () => ({ __esModule: true, default: () => <div data-testid="loader" /> }));
jest.mock("../../redux/features/application-state/application-state.selector", () => ({ useApplicationStateSelector: (selector: any) => selector({ internetConnectionStatus: "LOADING" }) }));
jest.mock("react-i18next", () => ({ useTranslation: () => ({ t: (value: string) => value }) }));
jest.mock("react-router-dom", () => ({ useNavigate: () => jest.fn() }));

describe("remaining coverage paths", () => {
  test("renders the mobile verification stepper", () => {
    render(<MobileStepper />);
    expect(screen.getByText("Scan")).toBeInTheDocument();
    expect(screen.getByText(/your credential/)).toBeInTheDocument();
  });

  test("renders the desktop selection panel and wallet selector", () => {
    render(<SelectionPanel />);
    expect(screen.getByTestId("selection-content")).toBeInTheDocument();
    render(<SelectWallet />);
    expect(screen.getByText("walletSelectorTitle")).toBeInTheDocument();
  });

  test("shows the connectivity loader while status is loading", () => {
    render(<CheckingForInternetConnectivity />);
    expect(screen.getByTestId("loader")).toBeInTheDocument();
  });
});
