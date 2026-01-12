import React from "react";
import { render, screen } from "@testing-library/react";
import Header from "../../../../components/Home/Header";

// mock theme
beforeAll(() => {
  (window as any)._env_ = { DEFAULT_THEME: "primary" };
});

// mock translation
jest.mock("react-i18next", () => ({
  useTranslation: () => ({
    t: (key: string) => {
      const dict: Record<string, string> = {
        heading: "Verify",
        headingHighlight: " Credentials",
        description: "Scan or upload to verify instantly",
      };
      return dict[key] || key;
    },
  }),
}));

// mock child component
jest.mock(
  "../../../../components/Home/Header/VerificationMethodTabs",
  () => () => <div data-testid="method-tabs" />
);

describe("Header Component", () => {
  test("renders heading with highlighted part", () => {
    render(<Header />);

    expect(
      screen.getByText((_, el) => el?.textContent === "Verify Credentials")
    ).toBeInTheDocument();
  });

  test("renders description text", () => {
    render(<Header />);
    expect(
      screen.getByText("Scan or upload to verify instantly")
    ).toBeInTheDocument();
  });

  test("renders VerificationMethodTabs", () => {
    render(<Header />);
    expect(screen.getByTestId("method-tabs")).toBeInTheDocument();
  });

  test("applies theme gradient on header container", () => {
    render(<Header />);
    const container = screen.getByTestId("header-container");

    expect(container).toBeInTheDocument();
    expect(container.className).toContain("bg-primary-lighter-gradient");
  });
});
