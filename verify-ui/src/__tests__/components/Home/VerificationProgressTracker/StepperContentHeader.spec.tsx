import React from "react";
import { render, screen } from "@testing-library/react";
import Header from "../../../../components/Home/Header";
import configureMockStore from "redux-mock-store";
import { Provider } from "react-redux";
import { MemoryRouter } from "react-router-dom";

jest.mock("iso-639-3", () => ({
    iso6393: [],
}));

const mockStore = configureMockStore();
const store = mockStore({
  verification: { method: "UPLOAD", activeScreen: 1 },
  verify: {
    isPartiallyShared: false,
    flowType: "sameDevice",
    activeScreen: 1,
  },
  common: { language: "en" },
});

describe("Stepper Content Header", () => {
  test("renders verification heading", () => {
    render(
      <Provider store={store}>
        <MemoryRouter>
          <Header />
        </MemoryRouter>
      </Provider>
    );

    const heading = screen.getByText((_, element) => {
      return element?.id === "verify-credentials-heading";
    });

    expect(heading).toBeInTheDocument();
  });
});