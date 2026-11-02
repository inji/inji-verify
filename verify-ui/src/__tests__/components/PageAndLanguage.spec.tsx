import React from "react";
import { fireEvent, render, screen } from "@testing-library/react";
import { MemoryRouter } from "react-router-dom";
import { LanguageSelector } from "../../components/commons/LanguageSelector";
import SomethingWentWrong from "../../components/SomethingWentWrong";

const mockDispatch = jest.fn();
let mockLanguage = "en";

jest.mock("../../redux/hooks", () => ({
  useAppDispatch: () => mockDispatch,
  useAppSelector: (selector: (state: any) => any) => selector({ common: { language: mockLanguage } }),
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

jest.mock("../../redux/store", () => ({
  getState: () => ({ verification: { method: "SCAN" } }),
}));

jest.mock("react-i18next", () => ({
  useTranslation: () => ({
    t: (key: string) => key,
  }),
}));

describe("language and retry components", () => {
  beforeEach(() => {
    mockDispatch.mockClear();
    mockLanguage = "en";
  });

  test("opens the language selector and changes language", () => {
    render(<LanguageSelector />);
    fireEvent.click(screen.getByTestId("Language-Selector-Button"));
    expect(screen.getByTestId("Language-Selector-DropDown-Item-hi")).toBeInTheDocument();
  });

  test("supports language menu keyboard navigation, selection, and escape", () => {
    render(<LanguageSelector />);
    const trigger = screen.getByTestId("Language-Selector-Button");
    fireEvent.keyDown(trigger, { key: "ArrowDown" });
    const english = screen.getByRole("menuitemradio", { name: "English" });
    const hindi = screen.getByRole("menuitemradio", { name: "Hindi" });
    expect(english).toHaveFocus();
    fireEvent.keyDown(english, { key: "ArrowDown" });
    expect(hindi).toHaveFocus();
    fireEvent.click(hindi);
    expect(mockDispatch).toHaveBeenCalledWith(expect.objectContaining({ payload: { language: "hi" } }));
    expect(trigger).toHaveFocus();
    fireEvent.click(trigger);
    fireEvent.keyDown(screen.getByRole("menu"), { key: "Escape" });
    expect(screen.queryByRole("menu")).not.toBeInTheDocument();
    expect(trigger).toHaveFocus();
  });

  test("closes the menu on Tab without trapping keyboard navigation", () => {
    render(<LanguageSelector />);
    fireEvent.click(screen.getByTestId("Language-Selector-Button"));
    fireEvent.keyDown(screen.getByRole("menuitemradio", { name: "English" }), { key: "Tab" });
    expect(screen.queryByRole("menu")).not.toBeInTheDocument();
  });

  test("focuses the first language when the current language is unavailable", () => {
    mockLanguage = "unsupported";
    render(<LanguageSelector />);
    fireEvent.click(screen.getByTestId("Language-Selector-Button"));
    expect(screen.getByRole("menuitemradio", { name: "English" })).toHaveFocus();
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
  });
});
