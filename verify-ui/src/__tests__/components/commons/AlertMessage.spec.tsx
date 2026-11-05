import React from "react";
import AlertMessage from "../../../components/commons/AlertMessage";
import { render, screen } from "@testing-library/react";
import { Provider } from "react-redux";
import { configureStore } from "@reduxjs/toolkit";
import { PreloadedState } from "../../../redux/features/verification/verification.slice";
import { AlertMessages } from "../../../utils/config";

jest.mock("react-i18next", () => ({
    useTranslation: () => ({
        t: (key: string) => key,
        i18n: {changeLanguage: jest.fn(), language: "en"}
    }),
    initReactI18next: {
        type: "3rdParty",
        init: jest.fn(),
    },
}));

const createTestStore = (state: any) => configureStore({ reducer: () => state });
describe("AlertMessage", () => {
    test("renders alert message component", () => {
        const store = createTestStore({
            verification: PreloadedState,
            alert: { ...AlertMessages().qrUploadSuccess, open: true },
        });

        render(
            <Provider store={store}>
                <AlertMessage />
            </Provider>
        );

        expect(screen.getByRole("button", { name: /close/i })).toBeInTheDocument();
    });
});
