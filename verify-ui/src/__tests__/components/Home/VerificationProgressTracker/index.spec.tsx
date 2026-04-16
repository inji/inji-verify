import React from "react";
import { render, screen } from "@testing-library/react";
import { Provider } from "react-redux";
import configureMockStore from "redux-mock-store";
import VerificationProgressTracker from "../../../../components/Home/VerificationProgressTracker";
import { VerificationSteps } from "../../../../utils/config";

jest.mock(
    "../../../../components/Home/VerificationProgressTracker/DesktopStepper",
    () => {
        const React = require("react");
        return function MockDesktopStepper() {
            return React.createElement(
                "div",
                { "data-testid": "desktop-stepper" },
                "DesktopStepper"
            );
        };
    }
);

jest.mock(
    "../../../../components/Home/VerificationProgressTracker/MobileStepper",
    () => {
        const React = require("react");
        return function MockMobileStepper() {
            return React.createElement(
                "div",
                { "data-testid": "mobile-stepper" },
                "MobileStepper"
            );
        };
    }
);

const mockStore = configureMockStore([]);

describe("VerificationProgressTracker", () => {
    it("renders the progress tracker", () => {
        const store = mockStore({
            verification: {
                activeScreen: VerificationSteps.SCAN.QrCodePrompt,
                method: "SCAN",
            },
            verify: {
                isPartiallyShared: false,
                flowType: "sameDevice",
            },
            common: {
                language: "en",
            },
        });

        render(
            <Provider store={store}>
                <VerificationProgressTracker />
            </Provider>
        );

        expect(screen.getByTestId("desktop-stepper")).toBeInTheDocument();
    });
});