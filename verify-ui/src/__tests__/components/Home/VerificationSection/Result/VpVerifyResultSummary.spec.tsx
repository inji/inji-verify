import { render, screen } from "@testing-library/react";
import VpVerifyResultSummary from "../../../../../components/Home/VerificationSection/Result/VpVerifyResultSummary";

jest.mock("react-i18next", () => ({
  useTranslation: () => ({ t: (value: string) => value }),
}));

jest.mock("../../../../../redux/features/verification/verification.selector", () => ({
  useVerifyFlowSelector: () => ({ originalSelectedCredentials: [{ id: "one" }, { id: "two" }] }),
}));

jest.mock("../../../../../utils/commonUtils", () => ({
  getTotalCredentialCount: () => 2,
}));

describe("VpVerifyResultSummary", () => {
  test("shows requested credentials and unique verification statuses", () => {
    render(
      <VpVerifyResultSummary
        verifiedVcs={[
          { vcStatus: "SUCCESS" } as any,
          { vcStatus: "SUCCESS" } as any,
          { vcStatus: "INVALID" } as any,
        ]}
        unverifiedCredentials={[{ type: "HealthInsurance" } as any]}
      />,
    );

    expect(screen.getByText(/credentialsRequested/)).toBeInTheDocument();
    expect(screen.getByText(/SUCCESS/)).toBeInTheDocument();
    expect(screen.getByText(/INVALID/)).toBeInTheDocument();
    expect(screen.getByText(/notShared/)).toBeInTheDocument();
  });
});
