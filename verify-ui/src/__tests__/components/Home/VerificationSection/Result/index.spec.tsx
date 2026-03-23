import React from "react";
import { render } from "@testing-library/react";
import Result from "../../../../../components/Home/VerificationSection/Result";
import { useVerificationFlowSelector } from "../../../../../redux/features/verification/verification.selector";
import { VerificationResult } from "../../../../../types/data-types";

jest.mock("../../../../../redux/hooks", () => ({
  useAppDispatch: jest.fn(),
}));

jest.mock("../../../../../redux/features/verification/verification.selector", () => ({
  useVerificationFlowSelector: jest.fn(),
}));

jest.mock("../../../../../utils/cborUtils", () => ({
  isCWT: () => false,
  uint8ArrayToHex: () => "",
  extractMappedClaim: () => ({}),
}));

jest.mock(
  "../../../../../components/Home/VerificationSection/Result/DisplayVcDetailView",
  () =>
    () =>
      <div data-testid="vc-detail-view-mock" />
);

jest.mock(
  "../../../../../components/Home/VerificationSection/Result/DisplayVcDetailsModal",
  () =>
    () =>
      <div data-testid="vc-detail-modal-mock" />
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
  beforeEach(() => {
    jest.clearAllMocks();
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
});
