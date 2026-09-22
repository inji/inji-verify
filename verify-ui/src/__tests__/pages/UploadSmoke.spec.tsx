import { render, screen } from "@testing-library/react";
import { Upload } from "../../pages/Upload";

jest.mock("../../redux/hooks", () => ({ useAppDispatch: () => jest.fn() }));
jest.mock("../../utils/misc", () => ({ checkInternetStatus: jest.fn().mockResolvedValue(true) }));
jest.mock("../../utils/commonUtils", () => ({ getClientId: () => "client-id", isVPSubmissionSupported: () => false, vcVerificationV2Request: jest.fn() }));
jest.mock("@injistack/react-inji-verify-sdk", () => ({ QRCodeVerification: () => <div data-testid="qr-code-verification" /> }));

describe("Upload page", () => {
  test("renders the QR upload verification component", () => {
    render(<Upload />);
    expect(screen.getByTestId("qr-code-verification")).toBeInTheDocument();
    expect(screen.getByText(/Allowed file formats/)).toBeInTheDocument();
  });
});
