import React from "react";
import { fireEvent, render, screen } from "@testing-library/react";
import { MemoryRouter } from "react-router-dom";
import { LanguageSelector } from "../../components/commons/LanguageSelector";
import SomethingWentWrong from "../../components/SomethingWentWrong";

const mockDispatch = jest.fn();
const mockNavigate = jest.fn();

jest.mock("../../redux/hooks", () => ({
  useAppDispatch: () => mockDispatch,
  useAppSelector: (selector: (state: any) => any) => selector({ common: { language: "en" } }),
}));

jest.mock("../../utils/i18n", () => ({
  isRTL: () => false,
  LanguagesSupported: [
    { value: "en", label: "English" },
    { value: "hi", label: "Hindi" },
  ],
  switchLanguage: jest.fn(),
}));

jest.mock("../../utils/theme-utils", () => ({
  ArrowDown: () => <span />,
  ArrowUp: () => <span />,
  Check: () => <span />,
  GlobeIcon: () => <span />,
  UnderConstruction: () => <span />,
}));

jest.mock("../../utils/builder", () => ({
  renderGradientText: (label: string) => label,
}));

jest.mock("react-router-dom", () => ({
  ...jest.requireActual("react-router-dom"),
  useNavigate: () => mockNavigate,
}));

jest.mock("react-i18next", () => ({
  useTranslation: () => ({
    t: (key: string) => key,
  }),
}));

describe("language and retry components", () => {
  beforeEach(() => {
    mockDispatch.mockClear();
    mockNavigate.mockClear();
  });

  test("opens the language selector and changes language", () => {
    render(<LanguageSelector />);
    fireEvent.click(screen.getByTestId("Language-Selector-Button"));
    expect(screen.getByTestId("Language-Selector-DropDown-Item-hi")).toBeInTheDocument();
  });

  test("renders retry action on the error page", () => {
    render(
      <MemoryRouter>
        <SomethingWentWrong />
      </MemoryRouter>,
    );
    expect(screen.getByText("retry")).toBeInTheDocument();
    fireEvent.click(screen.getByRole("button"));
    expect(mockDispatch).toHaveBeenCalled();
    expect(mockNavigate).toHaveBeenCalled();
  });
});
