import React from 'react';
import { render, screen } from "@testing-library/react";
import VerificationProgressTracker from "../../../../components/Home/VerificationProgressTracker";
import configureMockStore from "redux-mock-store";
import { Provider } from "react-redux";
import { VerificationSteps, getVerificationStepsContent } from "../../../../utils/config";

const mockStore = configureMockStore();
const store = mockStore({
    verification: {
        activeScreen: VerificationSteps.SCAN.QrCodePrompt,
        method: 'SCAN'
    },
    verify: {
        isPartiallyShared: false,
        flowType: 'sameDevice'
    },
    common: {
        language: 'en'
    }
});

describe("Verification Progress Tracker", () => {
    test("Test rendering", () => {
        render(<Provider store={store}>
            <VerificationProgressTracker />
        </Provider>)
        expect(screen.getByText(getVerificationStepsContent().SCAN[0].label)).toBeInTheDocument();
    })
})
