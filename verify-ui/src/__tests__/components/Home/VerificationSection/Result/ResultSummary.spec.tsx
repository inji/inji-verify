import React from "react";
import { render, screen } from "@testing-library/react";
import ResultSummary from "../../../../../components/Home/VerificationSection/Result/ResultSummary";

// Mock icons
jest.mock("../../../../../utils/theme-utils", () => ({
  VerificationSuccessIcon: (props: any) => (
    <div data-testid="success-icon" {...props} />
  ),
  VerificationFailedIcon: (props: any) => (
    <div data-testid="failed-icon" {...props} />
  ),
}));

// Mock translations
jest.mock("react-i18next", () => ({
  useTranslation: () => ({
    t: (key: string) => key, // echo translation keys
  }),
}));

describe("ResultSummary Component", () => {
  test("renders SUCCESS with success icon and correct background", () => {
    render(<ResultSummary status="SUCCESS" />);

    expect(screen.getByTestId("success-icon")).toBeInTheDocument();
    expect(screen.queryByTestId("failed-icon")).not.toBeInTheDocument();

    const container = screen.getByTestId("result-summary-container");
    expect(container.className).toContain("bg-successText");
  });

  test.each(["EXPIRED", "INVALID", "REVOKED"] as const)(
    "renders %s with failed icon and correct background",
    (status) => {
      const backgroundMap = {
        EXPIRED: "bg-expiredText",
        INVALID: "bg-invalidText",
        REVOKED: "bg-revokedText",
      } as const;

      render(<ResultSummary status={status} />);

      expect(screen.getByTestId("failed-icon")).toBeInTheDocument();
      expect(screen.queryByTestId("success-icon")).not.toBeInTheDocument();

      const container = screen.getByTestId("result-summary-container");
      expect(container.className).toContain(backgroundMap[status]);
    }
  );

  test("renders TIMEOUT with failed icon and no background class", () => {
    render(<ResultSummary status="TIMEOUT" />);

    expect(screen.getByTestId("failed-icon")).toBeInTheDocument();
    expect(screen.queryByTestId("success-icon")).not.toBeInTheDocument();

    const container = screen.getByTestId("result-summary-container");

    // Assert absence of `bg-*Text` classes
    expect(container.className).not.toMatch(/bg-.*Text/);
  });
});
