jest.mock("iso-639-3", () => ({
    iso6393: [],
    Language: {},
}));

import React from "react";
import { render, screen } from "@testing-library/react";
import DesktopStepper from "../../../../components/Home/VerificationProgressTracker/DesktopStepper";
import { getVerificationStepsContent } from "../../../../utils/config";
import { Provider } from "react-redux";
import configureMockStore from "redux-mock-store";

const mockStore = configureMockStore();
const store = mockStore({
  verification: { activeScreen: 1, method: "SCAN" },
  verify: { isPartiallyShared: false, flowType: "sameDevice", activeScreen: 1 },
  common: { language: "en" },
});

describe("Inji Stepper", () => {
  test("renders all SCAN step labels and descriptions", () => {
    render(
      <Provider store={store}>
        <DesktopStepper />
      </Provider>
    );

    const steps = getVerificationStepsContent().SCAN;

    steps.forEach((step) => {
      expect(screen.getByText(step.label)).toBeInTheDocument();
      if (typeof step.description === "string") {
        expect(screen.getByText(step.description)).toBeInTheDocument();
      }
    });
  });
});
