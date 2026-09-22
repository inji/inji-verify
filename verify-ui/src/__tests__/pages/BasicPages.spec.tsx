import React from "react";
import { render, screen } from "@testing-library/react";
import { MemoryRouter } from "react-router-dom";
import Home from "../../pages/Home";
import { Scan } from "../../pages/Scan";
import Offline from "../../pages/Offline";

jest.mock("../../components/Home/VerificationProgressTracker", () => () => (
  <div data-testid="verification-progress-tracker" />
));

jest.mock("../../components/Home/VerificationSection", () => () => (
  <div data-testid="verification-section" />
));

jest.mock("../../components/SomethingWentWrong", () => () => (
  <div data-testid="something-went-wrong" />
));

jest.mock("../../utils/theme-utils", () => ({
  Logo: () => <div data-testid="logo" />,
}));

jest.mock("../../redux/hooks", () => ({
  useAppDispatch: () => jest.fn(),
}));

describe("basic pages", () => {
  test.each([
    ["Home", <Home />],
    ["Scan", <Scan />],
  ])("renders the $0 page sections", (_name, page) => {
    render(page);
    expect(screen.getByTestId("verification-progress-tracker")).toBeInTheDocument();
    expect(screen.getByTestId("verification-section")).toBeInTheDocument();
  });

  test("renders the offline page", () => {
    render(
      <MemoryRouter>
        <Offline />
      </MemoryRouter>,
    );
    expect(screen.getByTestId("logo")).toBeInTheDocument();
    expect(screen.getByTestId("something-went-wrong")).toBeInTheDocument();
  });
});
