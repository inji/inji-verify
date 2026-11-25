import React from "react";
import { render, screen, waitFor, fireEvent } from "@testing-library/react";
import DisplayVcCardView from "../../../../../components/Home/VerificationSection/Result/DisplayVcCardView";
import { decodeSdJwtToken } from "../../../../../utils/decodeSdJwt";

jest.mock("../../../../../utils/decodeSdJwt", () => ({
  decodeSdJwtToken: jest.fn(),
}));

jest.mock("../../../../../components/Home/VerificationSection/Result/DisplayVcDetailView", () => {
  return function MockDisplayVcDetailView() {
    return <div data-testid="vc-detail-view">Detail View</div>;
  };
});

jest.mock("../../../../../components/Home/VerificationSection/Result/DisplayVcDetailsModal", () => {
  return function MockDisplayVcDetailsModal() {
    return <div data-testid="vc-details-modal">Modal View</div>;
  };
});

describe("DisplayVcCardView", () => {
  let consoleErrorSpy: jest.SpyInstance;

  beforeEach(() => {
    consoleErrorSpy = jest.spyOn(console, "error").mockImplementation(() => {});
  });

  afterEach(() => {
    consoleErrorSpy.mockRestore();
    jest.clearAllMocks();
  });

  test("renders card with SD-JWT credential when vct is present", async () => {
    (decodeSdJwtToken as jest.Mock).mockResolvedValueOnce({
      regularClaims: { vct: "InsuranceCredential" },
      disclosedClaims: {},
    });

    render(
      <DisplayVcCardView
        vc="valid.sd.jwt"
        vcStatus="SUCCESS"
        view={false}
      />
    );

    await waitFor(() => {
      expect(screen.getByText("Insurance Credential")).toBeInTheDocument();
    });
  });

  test("does not crash and resolves type when SD-JWT regularClaims uses type instead of vct", async () => {
    (decodeSdJwtToken as jest.Mock).mockResolvedValueOnce({
      regularClaims: { type: ["VerifiableCredential", "FarmerCredential"] },
      disclosedClaims: {},
    });

    render(
      <DisplayVcCardView
        vc="valid.sd.jwt.without.vct"
        vcStatus="SUCCESS"
        view={false}
      />
    );

    await waitFor(() => {
      expect(screen.getByText("Farmer Credential")).toBeInTheDocument();
    });
  });

  test("handles SD-JWT decoding error gracefully without crashing", async () => {
    (decodeSdJwtToken as jest.Mock).mockRejectedValueOnce(
      new Error("Malformed SD-JWT")
    );

    render(
      <DisplayVcCardView
        vc="invalid.sd.jwt"
        vcStatus="INVALID"
        view={false}
      />
    );

    await waitFor(() => {
      expect(consoleErrorSpy).toHaveBeenCalledWith(
        "Failed to decode credential claims:",
        expect.any(Error)
      );
    });
  });

  test("renders standard object VC and toggles detail view on click", async () => {
    const vc = {
      type: ["VerifiableCredential", "MockVerifiableCredential"],
      credentialSubject: { name: "Alice" },
    };

    render(
      <DisplayVcCardView
        vc={vc}
        vcStatus="SUCCESS"
        view={false}
      />
    );

    await waitFor(() => {
      expect(screen.getByText("Mock Verifiable Credential")).toBeInTheDocument();
    });

    const button = screen.getByRole("button");
    fireEvent.click(button);

    expect(screen.getByTestId("vc-detail-view")).toBeInTheDocument();
  });
});
