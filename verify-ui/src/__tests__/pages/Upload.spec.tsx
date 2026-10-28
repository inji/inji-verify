import React from "react";
import { render, waitFor } from "@testing-library/react";
import { Upload } from "../../pages/Upload";

const mockDispatch = jest.fn();
type QrError = Error | { errorMessage: string; errorCode?: string; transactionId?: string | null };

let onError: ((error: QrError) => Promise<void>) | undefined;

jest.mock("../../redux/hooks", () => ({
  useAppDispatch: () => mockDispatch,
}));

jest.mock("../../utils/misc", () => ({
  checkInternetStatus: jest.fn().mockResolvedValue(true),
}));

jest.mock("../../utils/commonUtils", () => ({
  getClientId: jest.fn().mockReturnValue("client-id"),
  isVPSubmissionSupported: jest.fn().mockReturnValue(false),
  vcVerificationV2Request: jest.fn(),
}));

jest.mock("@injistack/react-inji-verify-sdk", () => ({
  QRCodeVerification: ({ onError: errorHandler }: { onError: (error: QrError) => Promise<void> }) => {
    onError = errorHandler;
    return <div data-testid="qr-code-verification" />;
  },
}));

describe("Upload", () => {
  beforeEach(() => {
    mockDispatch.mockClear();
    onError = undefined;
  });

  test("shows the multiple QR message when the SDK detects multiple valid QR codes", async () => {
    render(<Upload />);

    expect(onError).toBeDefined();
    await onError!(Object.assign(new Error("Multiple QR codes found"), { name: "MULTIPLE_QR_FOUND" }));

    await waitFor(() => {
      expect(mockDispatch).toHaveBeenCalledWith(
        expect.objectContaining({
          payload: expect.objectContaining({
            message: "Multiple QR codes detected, please retry with an image containing a single QR code.",
          }),
        }),
      );
    });
  });

  test("shows the VC Verify error code and message", async () => {
    render(<Upload />);

    await onError!({
      errorCode: "resource_not_found",
      errorMessage: "The requested resource expired.",
    });

    await waitFor(() => {
      expect(mockDispatch).toHaveBeenCalledWith(
        expect.objectContaining({
          payload: expect.objectContaining({
            errorCode: "resource_not_found",
            errorReason: "The requested resource expired.",
            message: "The requested resource expired.",
          }),
        }),
      );
    });
  });
});
