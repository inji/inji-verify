import React from "react";
import { render } from "@testing-library/react";
import Verification from "../../../../components/Home/VerificationSection/Verification";

jest.mock("../../../../redux/hooks", () => ({
  useAppDispatch: () => jest.fn(),
}));

jest.mock("@injistack/react-inji-verify-sdk", () => ({
  QRCodeVerification: () => (
    <div data-testid="qr-code-verification-mock">QR CODE SDK MOCK</div>
  ),
}));

describe("Verification component", () => {
  test("renders back button", () => {
    const { container } = render(<Verification />);

    expect(
      container.querySelector("#verification-back-button")
    ).toBeInTheDocument();
  });
});
