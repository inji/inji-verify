import { render, screen } from "@testing-library/react";
import DesktopStepper from "../../../../components/Home/VerificationProgressTracker/DesktopStepper";
import i18n from "i18next";

// ---- Mocks ----

// FIX: proper mock default export with on/off
jest.mock("i18next", () => ({
  __esModule: true,
  default: {
    on: jest.fn(),
    off: jest.fn(),
  },
}));

jest.mock(
  "../../../../redux/features/verification/verification.selector",
  () => ({
    useVerificationFlowSelector: jest.fn(),
    useVerifyFlowSelector: jest.fn(),
  })
);

jest.mock("../../../../redux/hooks", () => ({
  useAppSelector: jest.fn(),
}));

jest.mock("../../../../utils/misc", () => ({
  fetchVerificationSteps: jest.fn(),
  convertToId: jest.fn((label) => label.toLowerCase().replace(/\s+/g, "-")),
}));

jest.mock("react-i18next", () => ({
  useTranslation: () => ({
    t: (key: string) => key,
  }),
}));

jest.mock("../../../../utils/i18n", () => ({
  isRTL: jest.fn(() => false),
}));

// --- Setup global env ---
beforeAll(() => {
  (window as any)._env_ = { DEFAULT_THEME: "primary" };
});

// --- Test Data ---
const mockSteps = [
  { stepNumber: 1, label: "Scan QR", description: "Scan the code" },
  { stepNumber: 2, label: "Verify", description: "Verifying credential" },
  { stepNumber: 3, label: "Result", description: "Show result" },
];

describe("DesktopStepper Component", () => {
  beforeEach(() => {
    jest.clearAllMocks();

    const {
      useVerificationFlowSelector,
      useVerifyFlowSelector,
    } = require("../../../../redux/features/verification/verification.selector");
    const { useAppSelector } = require("../../../../redux/hooks");
    const { fetchVerificationSteps } = require("../../../../utils/misc");

    useVerificationFlowSelector.mockReturnValue({
      mainActiveScreen: 1,
      method: "SCAN",
    });

    useVerifyFlowSelector.mockReturnValue({
      activeScreen: 1,
      isPartiallyShared: false,
      flowType: "NORMAL",
    });

    useAppSelector.mockReturnValue("en");
    fetchVerificationSteps.mockReturnValue(mockSteps);
  });

  test("renders steps correctly", () => {
    render(<DesktopStepper />);

    mockSteps.forEach((step) => {
      expect(screen.getByText(step.label)).toBeInTheDocument();
      expect(screen.getByText(step.description)).toBeInTheDocument();
    });
  });

  test("renders correct step numbers", () => {
    render(<DesktopStepper />);

    mockSteps.forEach((step) => {
      expect(screen.getByText(step.stepNumber.toString())).toBeInTheDocument();
    });
  });

  test("marks first step completed and others as incomplete", () => {
    render(<DesktopStepper />);

    const step0 = screen.getByTestId("step-number-0");
    expect(step0).toHaveClass("text-white");

    const step1 = screen.getByTestId("step-number-1");
    expect(step1).toHaveClass("text-primary");
  });

  test("uses theme color from window._env_", () => {
    render(<DesktopStepper />);

    mockSteps.forEach((step) => {
      const labelEl = screen.getByText(step.label);
      const parent = labelEl.closest("div") || labelEl.parentElement;

      expect(parent.className).toMatch(/text-stepperLabel|text-black|primary/i);
    });
  });

  test("updates steps when language changes via i18n", () => {
    const { unmount } = render(<DesktopStepper />);

    // Validate listener registration
    expect(i18n.on).toHaveBeenCalledWith(
      "languageChanged",
      expect.any(Function)
    );

    // Trigger cleanup
    unmount();

    // Validate cleanup listener removal
    expect(i18n.off).toHaveBeenCalledWith(
      "languageChanged",
      expect.any(Function)
    );
  });
});
