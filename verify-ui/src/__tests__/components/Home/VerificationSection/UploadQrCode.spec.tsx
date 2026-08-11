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

