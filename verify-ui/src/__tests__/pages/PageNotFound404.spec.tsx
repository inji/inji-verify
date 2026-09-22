import React from "react";
import { render } from "@testing-library/react";
import { MemoryRouter } from "react-router-dom";
import PageNotFound404 from "../../pages/PageNotFound404";

const mockNavigate = jest.fn();
const mockDispatch = jest.fn();

jest.mock("react-router-dom", () => ({
  ...jest.requireActual("react-router-dom"),
  useNavigate: () => mockNavigate,
}));

jest.mock("../../redux/hooks", () => ({
  useAppDispatch: () => mockDispatch,
}));

describe("PageNotFound404", () => {
  beforeEach(() => {
    mockNavigate.mockClear();
    mockDispatch.mockClear();
  });

  test("redirects to home and raises an alert", () => {
    const { container } = render(
      <MemoryRouter>
        <PageNotFound404 />
      </MemoryRouter>,
    );

    expect(container.firstChild).toBeNull();
    expect(mockNavigate).toHaveBeenCalledWith("/");
    expect(mockDispatch).toHaveBeenCalled();
  });
});
