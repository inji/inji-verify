import React from "react";
import { render } from "@testing-library/react";
import { ScanQrCode } from "../../../../components/Home/VerificationSection/ScanQrCode";

jest.mock("../../../../redux/hooks", () => ({
  useAppDispatch: jest.fn(),
}));

jest.mock("@injistack/pixelpass", () => ({
  decode: jest.fn(),
}));

describe("Scan Qr Code", () => {
  test("renders scan button and hidden trigger", () => {
    const { container } = render(<ScanQrCode />);

    expect(container.querySelector("#scan-button")).toBeInTheDocument();
    expect(container.querySelector("#trigger-scan")).toBeInTheDocument();
  });
});
