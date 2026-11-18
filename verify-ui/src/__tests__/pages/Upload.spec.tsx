import React from "react";
import { render, waitFor } from "@testing-library/react";
import i18next from "i18next";
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

  test.each([
    ["QR_NOT_FOUND", "No QRCode found", "Aucun code QR trouvé."],
    ["QR_DECODE_FAILED", "QR size too small/low quality, please retry with a clear QR",
      "Impossible de détecter le code QR. Il est peut-être trop petit ou de mauvaise qualité. Veuillez réessayer avec un code QR clair"],
    ["MULTIPLE_QR_FOUND", "Multiple QR codes found",
      "Plusieurs codes QR ont été détectés. Veuillez réessayer avec une image contenant un seul code QR."],
    ["Error", "File size not supported. The file size should be between 10 KB and 5 MB.",
      "Taille de fichier non prise en charge. "],
    ["Error", "Unsupported file format. Allowed file formats are: png, jpeg, jpg, pdf.",
      "Format de fichier non pris en charge. "],
  ])("translates the %s upload error in French", async (name, message, translatedMessage) => {
    const previousLanguage = i18next.language;
    await i18next.changeLanguage("fr");
    const { unmount } = render(<Upload />);

    try {
      await onError!(Object.assign(new Error(message), { name }));
      expect(mockDispatch).toHaveBeenCalledWith(
        expect.objectContaining({ payload: expect.objectContaining({ message: translatedMessage }) }),
      );
    } finally {
      unmount();
      await i18next.changeLanguage(previousLanguage);
    }
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
