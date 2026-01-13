import React from 'react';
import {render, screen} from "@testing-library/react";
import ScanQrCode from "../../../../components/Home/VerificationSection/ScanQrCode";
import {useVerificationFlowSelector} from "../../../../redux/features/verification/verification.selector";

jest.mock("../../../../redux/hooks", () => ({
    useAppDispatch: jest.fn(),
}));

jest.mock("../../../../redux/features/verification/verification.selector", () => ({
    useVerificationFlowSelector: jest.fn(),
}));

jest.mock("@injistack/pixelpass", () => ({
    decode: jest.fn()
}))

describe("Scan Qr Code", () => {
    test("Test rendering", () => {
        const api = require("../../../../redux/features/verification/verification.selector");
        api.useVerificationFlowSelector.mockReturnValue("NOT_READ");

        render(<ScanQrCode/>);
        expect(screen.getByText("Scan QR Code or Upload an Image")).toBeInTheDocument();
        expect(screen.getByText("Scan QR Code")).toBeInTheDocument();
        expect(screen.getByText("Upload QR Code")).toBeInTheDocument();
    })

    test("Test rendering - scan failed", () => {
        const api = require("../../../../redux/features/verification/verification.selector");
        api.useVerificationFlowSelector.mockReturnValue("FAILED");
        render(<ScanQrCode/>);
        expect(screen.getByText("Upload Another QR Code")).toBeInTheDocument();
    })
})
