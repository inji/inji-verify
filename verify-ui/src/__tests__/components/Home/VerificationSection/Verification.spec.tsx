import { render, screen, fireEvent, act } from "@testing-library/react";
import Verification from "../../../../components/Home/VerificationSection/Verification";
import { VerificationSteps } from "../../../../utils/config";

// ---- Mock Child Components ----
jest.mock(
  "../../../../components/Home/VerificationSection/QrScanner",
  () => (props: any) => (
    <div data-testid="qr-scanner" {...props}>
      QR SCANNER MOCK
    </div>
  )
);

jest.mock("../../../../components/commons/Loader", () => (props: any) => (
  <div data-testid="loader" {...props}>
    LOADER MOCK
  </div>
));

// ---- Mock Redux hooks & actions ----
jest.mock("../../../../redux/hooks", () => ({
  useAppDispatch: jest.fn(),
}));

jest.mock(
  "../../../../redux/features/verification/verification.slice",
  () => ({
    goToHomeScreen: jest.fn(() => ({ type: "goToHomeScreen" })),
  })
);

jest.mock(
  "../../../../redux/features/verification/verification.selector",
  () => ({
    useVerificationFlowSelector: jest.fn(),
  })
);

// ---- Mock theme image ----
jest.mock("../../../../utils/theme-utils", () => ({
  ScanOutline: "mocked-scan-outline-url",
}));

// ---- Mock translations ----
jest.mock("react-i18next", () => ({
  useTranslation: () => ({
    t: (key: string) => {
      if (key === "Common:Button.back") return "Back";
      return key;
    },
  }),
}));

describe("Verification Component", () => {
  const {
    useVerificationFlowSelector,
  } = require("../../../../redux/features/verification/verification.selector");
  const { useAppDispatch } = require("../../../../redux/hooks");
  const {
    goToHomeScreen,
  } = require("../../../../redux/features/verification/verification.slice");

  const mockDispatch = jest.fn();

  beforeEach(() => {
    jest.clearAllMocks();
    jest.useFakeTimers();
    useAppDispatch.mockReturnValue(mockDispatch);
  });

  afterEach(() => {
    jest.useRealTimers();
  });

  test("renders QrScanner when activeScreen != Verifying", () => {
    useVerificationFlowSelector.mockReturnValue({
      activeScreen: 0,
      method: "SCAN",
    });

    render(<Verification />);

    expect(screen.getByTestId("qr-scanner")).toBeInTheDocument();
    expect(screen.queryByTestId("loader")).not.toBeInTheDocument();
  });

  test("renders Loader when activeScreen === Verifying", () => {
    useVerificationFlowSelector.mockReturnValue({
      activeScreen: VerificationSteps["SCAN"].Verifying,
      method: "SCAN",
    });

    render(<Verification />);

    expect(screen.getByTestId("loader")).toBeInTheDocument();
    expect(screen.queryByTestId("qr-scanner")).not.toBeInTheDocument();
  });

  test("renders Back button with translated label", () => {
    useVerificationFlowSelector.mockReturnValue({
      activeScreen: 0,
      method: "SCAN",
    });

    render(<Verification />);

    expect(screen.getByRole("button", { name: "Back" })).toBeInTheDocument();
  });

  test("clicking Back triggers dispatch(goToHomeScreen) after timeout", () => {
    useVerificationFlowSelector.mockReturnValue({
      activeScreen: 0,
      method: "SCAN",
    });

    render(<Verification />);

    const backButton = screen.getByRole("button", { name: "Back" });

    fireEvent.click(backButton);

    // fast-forward the timeout (80ms)
    act(() => {
      jest.advanceTimersByTime(80);
    });

    expect(mockDispatch).toHaveBeenCalledWith(goToHomeScreen({}));
  });
});
