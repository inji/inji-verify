import React from "react";
import { render, screen } from "@testing-library/react";
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
      />
    );

    expect(
      screen.getByRole("button", { name: "Verify VC" })
    ).toBeInTheDocument();
  });

  test("throws when verifyServiceUrl is missing", () => {
    expect(() =>
      render(
        <QRCodeVerification
          {...baseProps}
          verifyServiceUrl={""}
          scannerActive={false}
          isEnableScan={false}
        />
      )
    ).toThrow("verifyServiceUrl is required.");
  });

  test("throws when both scan and upload are disabled", () => {
    expect(() =>
      render(
        <QRCodeVerification
          {...baseProps}
          scannerActive={false}
          isEnableScan={false}
          isEnableUpload={false}
        />
      )
    ).toThrow("Either scan or upload must be enabled.");
  });

  test("throws when both callbacks are provided", () => {
    expect(() =>
      render(
        <QRCodeVerification
          {...baseProps}
          onVCReceived={jest.fn()}
          onVCProcessed={jest.fn()}
          scannerActive={false}
          isEnableScan={false}
        />
      )
    ).toThrow("Only one of onVCReceived or onVCProcessed can be provided.");
  });
});
