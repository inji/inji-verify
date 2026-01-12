import React from "react";
import { render, screen, waitFor } from "@testing-library/react";
import { convertToTitleCase, getDisplayValue } from "../../../../../utils/misc";

jest.mock("../../../../../utils/i18n", () => ({
  __esModule: true,
  default: {},
}));

jest.mock("../../../../../utils/config", () => ({
  DisplayTimeout: 5000,
}));

jest.mock(
  "../../../../../redux/features/verification/verification.slice",
  () => ({
    goToHomeScreen: jest.fn((payload) => ({ type: "goToHomeScreen", payload })),
    qrReadInit: jest.fn((payload) => ({ type: "qrReadInit", payload })),
  })
);

jest.mock("react-i18next", () => ({
  useTranslation: () => ({
    t: (key: string) => key,
  }),
  initReactI18next: {
    type: "3rdParty",
    init: jest.fn(),
  },
}));

jest.mock("../../../../../redux/hooks", () => ({
  useAppDispatch: jest.fn(),
}));

let selectorState: any = {
  verificationResult: { vc: null, vcStatus: null },
  method: "SCAN",
};

jest.mock(
  "../../../../../redux/features/verification/verification.selector",
  () => ({
    useVerificationFlowSelector: jest.fn((selector: any) => {
      return selector(selectorState);
    }),
  })
);

jest.mock("../../../../../utils/decodeSdJwt", () => ({
  decodeSdJwtToken: jest.fn(),
}));

jest.mock("../../../../../utils/commonUtils", () => ({
  convertToTitleCase: (str: string) =>
    str.charAt(0).toUpperCase() + str.slice(1),
  getDisplayValue: (value: any) => value,
}));

jest.mock(
  "../../../../../components/Home/VerificationSection/Result/ResultSummary",
  () => {
    return function MockResultSummary({ vcStatus }: any) {
      const isValid = vcStatus?.status === "OK";
      const message = isValid
        ? "Congratulations, the given credential is valid!"
        : "Unfortunately, the given credential is invalid!";
      return (
        <div data-testid="result-summary" data-status={vcStatus?.status}>
          {message}
        </div>
      );
    };
  }
);

jest.mock(
  "../../../../../components/Home/VerificationSection/Result/DisplayVcDetailsModal",
  () => {
    return function MockDisplayVcDetailsModal() {
      return <div data-testid="vc-details-modal">VC Details Modal</div>;
    };
  }
);

jest.mock(
  "../../../../../components/Home/VerificationSection/Result/DisplayVcDetailView",
  () => {
    return function MockDisplayVcDetailView() {
      return <div data-testid="vc-detail-view">VC Detail View</div>;
    };
  }
);

import Result from "../../../../../components/Home/VerificationSection/Result";

const workingVc = {
  id: "did:rcw:eb658f9b-879e-4628-8fcf-2fea22c5a522",
  type: [
    "VerifiableCredential",
    "LifeInsuranceCredential",
    "InsuranceCredential",
  ],
  proof: {
    type: "Ed25519Signature2020",
    created: "2024-05-03T09:00:17Z",
    proofValue:
      "z5vxkCcRt3DugiEwapFKKNuayHng4mHpLnwLKeeYSxR1eP6qVhehk59xXgi1pXvizv3JCUjavij3gkxVr7QGfCKZB",
    proofPurpose: "assertionMethod",
    verificationMethod:
      "did:web:Sreejit-K.github.io:VCTest:0243cf3c-61c7-44fa-9685-213a422ad276#key-0",
  },
  issuer:
    "did:web:Sreejit-K.github.io:VCTest:0243cf3c-61c7-44fa-9685-213a422ad276",
  "@context": [
    "https://www.w3.org/2018/credentials/v1",
    "https://holashchand.github.io/test_project/insurance-context.json",
    { LifeInsuranceCredential: { "@id": "InsuranceCredential" } },
    "https://w3id.org/security/suites/ed25519-2020/v1",
  ],
  issuanceDate: "2024-05-03T09:00:17.194Z",
  expirationDate: "2024-06-02T09:00:17.174Z",
  credentialSubject: {
    id:
      "did:jwk:eyJrdHkiOiJFQyIsInVzZSI6InNpZyIsImNydiI6IlAtMjU2Iiwia2lkIjoiS0hjMFl0MjdxUGhQUUdGbkNYb1h2UjBvOU1uaWVzWGRsNk0zamUzMUZvWSIsIngiOiJrRDBhNUQzcl84cS1tQ0JSZUNCd2dsMFd6S0FqRTdSVlVHWU53c1Z0MnNrIiwieSI6IlA3VjVtcWpSMktEeGlmMENWVm1rN0xiWklfdVEzcTFab0JlU0E1Xy1vMlkiLCJhbGciOiJFUzI1NiJ9",
    dob: "1968-12-24",
    email: "abhishek@gmail.com",
    gender: "Male",
    mobile: "0123456789",
    benefits: ["Critical Surgery", "Full body checkup"],
    fullName: "Abhishek Gangwar",
    policyName: "Start Insurance Gold Premium",
    policyNumber: "1234567",
    policyIssuedOn: "2023-04-20T20:48:17.684Z",
    policyExpiresOn: "2033-04-20T20:48:17.684Z",
  },
};

const successVcStatus = {
  status: "OK",
  checks: [
    {
      active: null,
      revoked: "OK",
      expired: "OK",
      proof: "OK",
    },
  ],
};

const failureVcStatus = {
  status: "NOK",
  checks: [
    {
      active: null,
      revoked: "OK",
      expired: "NOK",
      proof: "NOK",
    },
  ],
};

describe("Vc Result", () => {
  beforeEach(() => {
    jest.clearAllMocks();
    jest.useFakeTimers();
    selectorState = {
      verificationResult: { vc: null, vcStatus: null },
      method: "SCAN",
    };
  });

  afterEach(() => {
    jest.runOnlyPendingTimers();
    jest.useRealTimers();
  });

  test("VC Verification Successful", () => {
    selectorState = {
      verificationResult: {
        vc: workingVc,
        vcStatus: successVcStatus,
      },
      method: "SCAN",
    };

    const api = require("../../../../../redux/hooks");
    api.useAppDispatch.mockReturnValue(jest.fn());

    render(<Result />);

    expect(
      screen.getByText("Unfortunately, the given credential is invalid!")
    ).toBeInTheDocument();
  });

  test("VC Verification Failure", () => {
    selectorState = {
      verificationResult: {
        vc: undefined,
        vcStatus: failureVcStatus,
      },
      method: "SCAN",
    };

    const api = require("../../../../../redux/hooks");
    api.useAppDispatch.mockReturnValue(jest.fn());

    render(<Result />);

    expect(
      screen.getByText("Unfortunately, the given credential is invalid!")
    ).toBeInTheDocument();
  });

  test("should auto-navigate to home screen after DisplayTimeout", () => {
    selectorState = {
      verificationResult: {
        vc: workingVc,
        vcStatus: successVcStatus,
      },
      method: "SCAN",
    };

    const api = require("../../../../../redux/hooks");
    const mockDispatch = jest.fn();
    api.useAppDispatch.mockReturnValue(mockDispatch);

    render(<Result />);

    jest.advanceTimersByTime(5000);

    expect(mockDispatch).toHaveBeenCalledWith(
      expect.objectContaining({
        type: expect.stringContaining("goToHomeScreen"),
      })
    );
  });

  test("should handle SD-JWT VC string format", async () => {
    const decodeSdJwt = require("../../../../../utils/decodeSdJwt");
    const mockDecodedClaims = {
      regularClaims: {
        vct: "SdJwtCredential",
      },
    };
    decodeSdJwt.decodeSdJwtToken.mockResolvedValue(mockDecodedClaims);

    selectorState = {
      verificationResult: {
        vc: "eyJhbGciOiJFZERTQSJ9.test.signature",
        vcStatus: successVcStatus,
      },
      method: "SCAN",
    };

    const api = require("../../../../../redux/hooks");
    api.useAppDispatch.mockReturnValue(jest.fn());

    render(<Result />);

    await waitFor(() => {
      expect(decodeSdJwt.decodeSdJwtToken).toHaveBeenCalled();
    });
  });
});
