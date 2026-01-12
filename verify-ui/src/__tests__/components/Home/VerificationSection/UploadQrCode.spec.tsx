import React from "react";
import { render, screen, fireEvent } from "@testing-library/react";
import { UploadQrCode } from "../../../../components/Home/VerificationSection/UploadQrCode";

// Mock Redux selector
jest.mock("../../../../redux/hooks", () => ({
  useAppSelector: jest.fn().mockReturnValue("en"),
}));

// Mock theme icons
jest.mock("../../../../utils/theme-utils", () => ({
  GradientUploadIcon: () => <div data-testid="gradient-icon" />,
  WhiteUploadIcon: () => <div data-testid="white-icon" />,
}));

// Mock RTL helper
jest.mock("../../../../utils/i18n", () => ({
  isRTL: jest.fn().mockReturnValue(false),
}));

describe("UploadQrCode Component", () => {
  beforeEach(() => {
    (global as any).window._env_ = { DEFAULT_THEME: "default_theme" };
  });

  test("renders display message", () => {
    render(<UploadQrCode displayMessage="Upload QR" />);
    expect(screen.getByText("Upload QR")).toBeInTheDocument();
  });

  test("applies theme gradient on wrapper", () => {
    render(<UploadQrCode displayMessage="Upload QR" />);

    const wrapper = screen.getByTestId("upload-container");
    expect(wrapper).toHaveClass("bg-default_theme-gradient");
  });

  test("renders gradient icon initially", () => {
    render(<UploadQrCode displayMessage="Upload QR" />);
    expect(screen.getByTestId("gradient-icon")).toBeInTheDocument();
    expect(screen.queryByTestId("white-icon")).not.toBeInTheDocument();
  });

  test("swaps to white icon on hover", () => {
    render(<UploadQrCode displayMessage="Upload QR" />);

    const label = screen.getByTestId("upload-label");
    fireEvent.mouseEnter(label);

    expect(screen.getByTestId("white-icon")).toBeInTheDocument();
    expect(screen.queryByTestId("gradient-icon")).not.toBeInTheDocument();
  });

  test("has button id upload-qr-code-button", () => {
    render(<UploadQrCode displayMessage="Upload QR" />);
    expect(screen.getByText("Upload QR")).toHaveAttribute(
      "id",
      "upload-qr-code-button"
    );
  });

  test("uses LTR margin when not RTL", () => {
    render(<UploadQrCode displayMessage="Upload QR" />);

    const iconWrapper = screen.getByTestId("upload-icon-wrapper");
    expect(iconWrapper.className).toContain("mr-1.5");
  });
});
