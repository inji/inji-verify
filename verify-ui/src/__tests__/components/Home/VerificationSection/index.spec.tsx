import React from "react";
import { render, screen } from "@testing-library/react";
import VerificationSection from "../../../../components/Home/VerificationSection";
import { useVerificationFlowSelector } from "../../../../redux/features/verification/verification.selector";

jest.mock("../../../../redux/features/verification/verification.selector", () => ({
  useVerificationFlowSelector: jest.fn(),
}));
jest.mock("../../../../components/Home/VerificationSection/Verification", () => () => <div data-testid="verification" />);
jest.mock("../../../../components/Home/VerificationSection/Result", () => () => <div data-testid="result" />);
jest.mock("../../../../components/Home/VerificationSection/ScanQrCode", () => ({ ScanQrCode: () => <div data-testid="scan" /> }));
jest.mock("../../../../pages/Upload", () => ({ Upload: () => <div data-testid="upload" /> }));

const selector = useVerificationFlowSelector as jest.Mock;

describe("VerificationSection", () => {
  it.each([
    ["UPLOAD", 1, "upload"],
    ["SCAN", 1, "scan"],
    ["UPLOAD", 2, "verification"],
    ["SCAN", 3, "verification"],
    ["UPLOAD", 3, "result"],
  ])("renders %s step %s", (method, activeScreen, expected) => {
    selector.mockImplementation((fn: (state: any) => any) => fn({ method, activeScreen }));
    render(<VerificationSection />);
    expect(screen.getByTestId(expected)).toBeInTheDocument();
  });

  it("renders nothing for an unknown step or method", () => {
    selector.mockImplementation((fn: (state: any) => any) => fn({ method: "UNKNOWN", activeScreen: 99 }));
    const { container } = render(<VerificationSection />);
    expect(container).toBeEmptyDOMElement();
  });
});
