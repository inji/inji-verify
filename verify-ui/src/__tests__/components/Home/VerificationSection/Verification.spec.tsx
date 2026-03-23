import React from "react";
import { render } from "@testing-library/react";
import Verification from "../../../../components/Home/VerificationSection/Verification";

const mockDispatch = jest.fn();

jest.mock("iso-639-3", () => ({
    iso6393: [],
}));

jest.mock("../../../../redux/hooks", () => ({
    useAppDispatch: () => mockDispatch,
}));

jest.mock("@injistack/react-inji-verify-sdk", () => {
    const React = require("react");
    return {
        QRCodeVerification: () =>
            React.createElement(
                "div",
                { "data-testid": "qr-code-verification-mock" },
                "QR CODE SDK MOCK"
            ),
    };
});

describe("Verification component", () => {
  test("renders back button", () => {
    const { container } = render(<Verification />);

    expect(
      container.querySelector("#verification-back-button")
    ).toBeInTheDocument();
  });
});
