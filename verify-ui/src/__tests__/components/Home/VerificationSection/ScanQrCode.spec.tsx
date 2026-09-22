import React from "react";
import { fireEvent, render, waitFor } from "@testing-library/react";
import { ScanQrCode } from "../../../../components/Home/VerificationSection/ScanQrCode";

const dispatch = jest.fn();
jest.mock("../../../../redux/hooks", () => ({
  useAppDispatch: () => dispatch,
}));
jest.mock("../../../../utils/misc", () => ({
  checkInternetStatus: jest.fn().mockResolvedValue(true),
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

  test("starts scanning when internet is available", async () => {
    const { container } = render(<ScanQrCode />);
    fireEvent.click(container.querySelector("#scan-button")!);
    await waitFor(() => expect(dispatch).toHaveBeenCalled());
    fireEvent.click(container.querySelector("#trigger-scan")!);
    expect(dispatch).toHaveBeenCalledWith({ type: "VcVerification/qrReadInit", payload: { method: "SCAN" } });
  });

  test("opens the scanner from a response code URL", () => {
    window.history.pushState({}, "", "/scan?response_code=test");
    localStorage.setItem("path", "/scan");
    render(<ScanQrCode />);
    expect(dispatch).toHaveBeenCalled();
    window.history.pushState({}, "", "/");
    localStorage.clear();
  });
});
