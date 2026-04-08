import React from "react";
import { render, screen, waitFor } from "@testing-library/react";
import "@testing-library/jest-dom";
import QRCodeVerification from "../../../src/components/qrcode-verification/QRCodeVerification";

jest.mock("../../../src/utils/uploadQRCodeUtils", () => ({
  doFileChecks: jest.fn(() => true),
  scanFilesForQr: jest.fn(),
}));

jest.mock("../../../src/utils/dataProcessor", () => ({
  decodeQrData: jest.fn(),
  extractRedirectUrlFromQrData: jest.fn(),
}));

jest.mock("zxing-wasm/full", () => ({
  readBarcodes: jest.fn(),
}));

describe("QRCodeVerification", () => {
  const baseProps = {
    verifyServiceUrl: "https://example.com/verify",
    clientId: "test-client-id",
    onError: jest.fn(),
    onVCProcessed: jest.fn(),
    triggerElement: <button>Verify VC</button>,
  };

  beforeEach(() => {
    jest.clearAllMocks();
  });

  test("renders trigger element", () => {
    render(
      <QRCodeVerification
        {...baseProps}
        scannerActive={false}
        isEnableScan={false}
        isEnableUpload={true}
      />
    );
  
    expect(
      screen.getAllByRole("button", { name: "Verify VC" })[0]
    ).toBeInTheDocument();
  });

  test("throws when verifyServiceUrl is missing", () => {
    class ErrorBoundary extends React.Component<
      { children: React.ReactNode },
      { error: Error | null }
    > {
      state = { error: null };
      static getDerivedStateFromError(error: Error) {
        return { error };
      }
      render() {
        if (this.state.error) {
          return <div data-testid="error-message">{this.state.error.message}</div>;
        }
        return this.props.children;
      }
    }
    render(
      <ErrorBoundary>
        <QRCodeVerification
          {...baseProps}
          verifyServiceUrl={""}
          scannerActive={false}
          isEnableScan={false}
        />
      </ErrorBoundary>
    );
    expect(screen.getByTestId("error-message")).toHaveTextContent("verifyServiceUrl is required.");
  });

  test("throws when both scan and upload are disabled", () => {
    class ErrorBoundary extends React.Component<
      { children: React.ReactNode },
      { error: Error | null }
    > {
      state = { error: null };
      static getDerivedStateFromError(error: Error) {
        return { error };
      }
      render() {
        if (this.state.error) {
          return <div data-testid="error-message">{this.state.error.message}</div>;
        }
        return this.props.children;
      }
    }
    render(
      <ErrorBoundary>
        <QRCodeVerification
          {...baseProps}
          scannerActive={false}
          isEnableScan={false}
          isEnableUpload={false}
        />
      </ErrorBoundary>
    );
    expect(screen.getByTestId("error-message")).toHaveTextContent("Either scan or upload must be enabled.");
  });

  test("throws when both callbacks are provided", async () => {
    class ErrorBoundary extends React.Component<
      { children: React.ReactNode },
      { error: Error | null }
    > {
      state = { error: null };
      static getDerivedStateFromError(error: Error) {
        return { error };
      }
      render() {
        if (this.state.error) {
          return <div data-testid="error-message">{this.state.error.message}</div>;
        }
        return this.props.children;
      }
    }
    render(
      <ErrorBoundary>
        <QRCodeVerification
          {...baseProps}
          onVCReceived={jest.fn()}
          onVCProcessed={jest.fn()}
          scannerActive={false}
          isEnableScan={false}
        />
      </ErrorBoundary>
    );
    await waitFor(() => {
      expect(screen.getByTestId("error-message")).toHaveTextContent(
        "Only one of onVCReceived or onVCProcessed can be provided."
      );
    });
  });
});
