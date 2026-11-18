import { Button } from "../../../../components/Home/VerificationSection/commons/Button";
import React from 'react';
import { render, screen } from "@testing-library/react";
import { UploadQrCode } from "../../../../components/Home/VerificationSection/UploadQrCode";

jest.mock("iso-639-3", () => ({
    iso6393: [],
}));

jest.mock("../../../../redux/hooks", () => ({
    useAppDispatch: jest.fn(),
    useAppSelector: jest.fn().mockImplementation((selector) => selector({ common: { language: 'en' } }))
}));

jest.mock("@injistack/pixelpass", () => ({
    decode: jest.fn()
}))

describe("UploadQrCode", () => {
    test("renders without a native file-input label", () => {
        const { container } = render(<UploadQrCode displayMessage="Upload Qr Code" />)
        expect(screen.getByText("Upload Qr Code")).toBeInTheDocument()
        expect(container.querySelector("label")).not.toBeInTheDocument()
    })
})



describe("shared button styles", () => {
  test.each(["fill", "outline", "clear"] as const)("preserves enabled and disabled %s styles", (variant) => {
    const { rerender } = render(<Button title="Action" variant={variant} />);
    const button = screen.getByRole("button", { name: "Action" });
    expect(button).toHaveAttribute("type", "button");
    const backgrounds = { fill: "text-white", outline: "bg-white", clear: "bg-transparent" };
    expect(button).toHaveClass(backgrounds[variant]);
    rerender(<Button title="Action" variant={variant} disabled />);
    expect(button).toBeDisabled();
    expect(button).toHaveClass("bg-disabledButtonBg", "text-white");
  });

  test("preserves explicit submit type", () => {
    render(<Button title="Submit" type="submit" />);
    expect(screen.getByRole("button", { name: "Submit" })).toHaveAttribute("type", "submit");
  });
});
