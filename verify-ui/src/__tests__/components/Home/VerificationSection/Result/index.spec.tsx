import React from "react";
import { render, waitFor } from "@testing-library/react";
import Result from "../../../../../components/Home/VerificationSection/Result";
import { useVerificationFlowSelector } from "../../../../../redux/features/verification/verification.selector";
import { VerificationResult } from "../../../../../types/data-types";
import { useAppDispatch } from "../../../../../redux/hooks";
import { raiseAlert } from "../../../../../redux/features/alerts/alerts.slice";
import { decodeSdJwtToken } from "../../../../../utils/decodeSdJwt";
import { isCWT, extractMappedClaim } from "../../../../../utils/cborUtils";

jest.mock("iso-639-3", () => ({
    iso6393: [],
}));

jest.mock("../../../../../redux/hooks", () => ({
    useAppDispatch: jest.fn(),
}));

jest.mock("../../../../../redux/features/verification/verification.selector", () => ({
    useVerificationFlowSelector: jest.fn(),
}));

jest.mock("../../../../../utils/decodeSdJwt", () => ({
    decodeSdJwtToken: jest.fn(),
}));

jest.mock("../../../../../utils/cborUtils", () => ({
    isCWT: jest.fn(() => false),
    uint8ArrayToHex: () => "",
    extractMappedClaim: jest.fn(() => ({})),
}));

jest.mock(
    "../../../../../components/Home/VerificationSection/Result/DisplayVcDetailView",
    () => {
        const React = require("react");
        return {
            __esModule: true,
            default: () =>
                React.createElement("div", {
                    "data-testid": "vc-detail-view-mock",
                }),
        };
    }
);

jest.mock(
    "../../../../../components/Home/VerificationSection/Result/DisplayVcDetailsModal",
    () => {
        const React = require("react");
        return {
            __esModule: true,
            default: () =>
                React.createElement("div", {
                    "data-testid": "vc-detail-modal-mock",
                }),
        };
    }
);

const mockVerificationSelector = (verificationResult: VerificationResult) => {
    (useVerificationFlowSelector as jest.Mock).mockImplementation(
        (selector: (state: any) => any) =>
            selector({
                method: "SCAN",
                activeScreen: 1,
                qrReadResult: { status: "SUCCESS" },
                verificationResult,
                alert: {},
                ovp: {},
            })
    );
};

const workingVc: any = {
  id: "did:example:123",
  type: ["VerifiableCredential", "LifeInsuranceCredential", "InsuranceCredential"],
  "@context": [],
  issuanceDate: "2024-05-03T09:00:17.194Z",
  expirationDate: "2024-06-02T09:00:17.174Z",
  issuer: "did:example:issuer",
  proof: {
    type: "Ed25519Signature2020",
    created: "2024-05-03T09:00:17Z",
    proofValue: "z-proof",
    proofPurpose: "assertionMethod",
    verificationMethod: "did:example:issuer#key-0",
  },
  credentialSubject: {
    email: "abhishek@gmail.com",
  },
};

describe("Vc Result", () => {
  const mockDispatch = jest.fn();

  beforeEach(() => {
    jest.clearAllMocks();
    (useAppDispatch as jest.Mock).mockReturnValue(mockDispatch);
  });

  test("VC Verification Successful", () => {
    mockVerificationSelector({ vc: workingVc, vcStatus: "SUCCESS" });

    const { container } = render(<Result />);

    const message = container.querySelector("#vc-result-display-message");
    expect(message).toBeInTheDocument();
    expect(message?.textContent).toBeTruthy();
  });

  test("VC Verification Failure", () => {
    mockVerificationSelector({ vc: workingVc, vcStatus: "INVALID" });

    const { container } = render(<Result />);

    const message = container.querySelector("#vc-result-display-message");
    expect(message).toBeInTheDocument();
    expect(message?.textContent).toBeTruthy();
  });

  test("dispatches raiseAlert with severity 'error' when SD-JWT decoding fails", async () => {
    (decodeSdJwtToken as jest.Mock).mockRejectedValueOnce(new Error("Invalid SD-JWT"));

    mockVerificationSelector({ vc: "invalid.sdjwt.token", vcStatus: "SUCCESS" });

    render(<Result />);

    await waitFor(() => {
      expect(mockDispatch).toHaveBeenCalledWith(
        raiseAlert({
          message: "Invalid SD-JWT",
          severity: "error",
        })
      );
    });
  });

  test("dispatches raiseAlert with severity 'error' when CWT decoding fails", async () => {
    (isCWT as jest.Mock).mockReturnValueOnce(true);
    (extractMappedClaim as jest.Mock).mockImplementationOnce(() => {
      throw new Error("Invalid CWT");
    });

    mockVerificationSelector({ vc: new Uint8Array([1, 2, 3]), vcStatus: "SUCCESS" });

    render(<Result />);

    await waitFor(() => {
      expect(mockDispatch).toHaveBeenCalledWith(
        raiseAlert({
          message: "Invalid CWT",
          severity: "error",
        })
      );
    });
  });
});
