import React from "react";
import { render, screen } from "@testing-library/react";
import Header from "../../../../components/Home/Header";

jest.mock("react-i18next", () => ({
    useTranslation: () => ({
        t: (key: string) => {
            const translations: Record<string, string> = {
                heading: "Verify credentials ",
                headingHighlight: "with ease!",
                description: "Effortlessly verify credentials with Inji Verify!",
            };
            return translations[key] || key;
        },
        i18n: { changeLanguage: jest.fn(), language: "en" },
    }),
}));

jest.mock(
    "../../../../components/Home/Header/VerificationMethodTabs",
    () => {
        const React = require("react");
        return function MockVerificationMethodTabs() {
            return React.createElement(
                "div",
                { "data-testid": "verification-method-tabs" },
                "VerificationMethodTabs"
            );
        };
    }
);

describe("Header Component", () => {
    it("renders heading and description correctly", () => {
        const { container } = render(<Header />);

        const headingElement = container.querySelector("#verify-credentials-heading");
        expect(headingElement).toBeInTheDocument();
        expect(headingElement).toHaveTextContent("Verify credentials");

        const highlightElement = container.querySelector("#heading-highlighted-content");
        expect(highlightElement).toBeInTheDocument();
        expect(highlightElement).toHaveTextContent("with ease!");

        const descriptionElement = container.querySelector("#verify-credentials-description");
        expect(descriptionElement).toBeInTheDocument();
        expect(descriptionElement).toHaveTextContent("Effortlessly verify credentials with Inji Verify!");
    });

    it("renders the verification method tabs component", () => {
        render(<Header />);

        const tabsElement = screen.getByTestId("verification-method-tabs");
        expect(tabsElement).toBeInTheDocument();
    });
});
