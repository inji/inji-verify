import React from "react";
import { render, screen } from "@testing-library/react";
import { MemoryRouter } from "react-router-dom";
import PageTemplate from "../../components/PageTemplate";

jest.mock("../../components/PageTemplate/Navbar", () => () => <div data-testid="navbar" />);
jest.mock("../../components/PageTemplate/Copyrights", () => () => <div data-testid="copyrights" />);
jest.mock("../../components/misc/CheckingForInternetConnectivity", () => () => (
  <div data-testid="connectivity-check" />
));
jest.mock("../../components/Home/Header", () => () => <div data-testid="header" />);
jest.mock("react-router-dom", () => ({
  ...jest.requireActual("react-router-dom"),
  Outlet: () => <div data-testid="outlet" />,
}));

describe("PageTemplate", () => {
  test("renders the shared page sections", () => {
    render(
      <MemoryRouter>
        <PageTemplate />
      </MemoryRouter>,
    );

    expect(screen.getByTestId("navbar")).toBeInTheDocument();
    expect(screen.getByTestId("header")).toBeInTheDocument();
    expect(screen.getByTestId("outlet")).toBeInTheDocument();
    expect(screen.getByTestId("copyrights")).toBeInTheDocument();
    expect(screen.getByTestId("connectivity-check")).toBeInTheDocument();
  });
});
