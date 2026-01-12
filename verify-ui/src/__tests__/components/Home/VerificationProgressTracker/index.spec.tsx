import React from "react";
import { render, screen } from "@testing-library/react";
import VerificationProgressTracker from "../../../../components/Home/VerificationProgressTracker";
import configureMockStore from "redux-mock-store";
import { Provider } from "react-redux";

// Mock isMobileDevice
jest.mock("../../../../utils/config", () => ({
  isMobileDevice: jest.fn(),
}));

// Mock MobileStepper + DesktopStepper
jest.mock(
  "../../../../components/Home/VerificationProgressTracker/MobileStepper",
  () => () => <div data-testid="mobile-stepper" />
);

jest.mock(
  "../../../../components/Home/VerificationProgressTracker/DesktopStepper",
  () => () => <div data-testid="desktop-stepper" />
);

const mockStore = configureMockStore();
const store = mockStore({});

describe("VerificationProgressTracker", () => {
  const { isMobileDevice } = require("../../../../utils/config");

  test("renders MobileStepper when isMobileDevice returns true", () => {
    isMobileDevice.mockReturnValue(true);

    render(
      <Provider store={store}>
        <VerificationProgressTracker />
      </Provider>
    );

    expect(screen.getByTestId("mobile-stepper")).toBeInTheDocument();
  });

  test("renders DesktopStepper when isMobileDevice returns false", () => {
    isMobileDevice.mockReturnValue(false);

    render(
      <Provider store={store}>
        <VerificationProgressTracker />
      </Provider>
    );

    expect(screen.getByTestId("desktop-stepper")).toBeInTheDocument();
  });
});
