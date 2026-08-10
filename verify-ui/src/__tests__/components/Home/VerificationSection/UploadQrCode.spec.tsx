import React from 'react';
import { fireEvent, render, screen } from "@testing-library/react";
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
    const renderUploadButton = () => {
        const fileInput = document.createElement("input");
        fileInput.id = "upload-qr";
        document.body.appendChild(fileInput);
        const clickSpy = jest.spyOn(fileInput, "click");

        render(<UploadQrCode displayMessage="Upload Qr Code" />);

        return { fileInput, clickSpy };
    };

    afterEach(() => {
        document.getElementById("upload-qr")?.remove();
    });

    test("opens the file picker when the upload button is clicked", () => {
        const { clickSpy } = renderUploadButton();

        fireEvent.click(screen.getByRole("button", { name: "Upload Qr Code" }));

        expect(clickSpy).toHaveBeenCalledTimes(1);
    });

    test("opens the file picker when the upload button is activated with the keyboard", () => {
        const { clickSpy } = renderUploadButton();

        fireEvent.keyDown(screen.getByRole("button", { name: "Upload Qr Code" }), { key: "Enter" });

        expect(clickSpy).toHaveBeenCalledTimes(1);
    });
})

