import React from "react";
import { fireEvent, render, screen } from "@testing-library/react";
import Navbar from "../../../components/PageTemplate/Navbar";

jest.mock("react-router-dom", () => ({
  Link: ({ children, ...props }: any) => <a {...props}>{children}</a>,
}));

jest.mock("react-i18next", () => ({
  useTranslation: () => ({ t: (key: string) => key }),
}));

jest.mock("../../../utils/i18n", () => ({
  isRTL: () => false,
}));

jest.mock("../../../redux/hooks", () => ({
  useAppDispatch: () => jest.fn(),
  useAppSelector: (selector: any) => selector({ common: { language: "en" } }),
}));

jest.mock("../../../components/commons/LanguageSelector", () => ({
  LanguageSelector: () => <div data-testid="language-selector" />,
}));

jest.mock("../../../utils/theme-utils", () => ({
  Logo: (props: any) => <svg data-testid="logo" {...props} />,
  MenuIcon: (props: any) => <svg data-testid="menu-icon" {...props} />,
  NewTabIcon: () => <svg />,
}));

describe("Navbar", () => {
  it("opens and closes the mobile navigation menu", () => {
    render(<Navbar />);

    fireEvent.click(screen.getByRole("button", { name: "" }));
    expect(screen.getAllByText("home")).toHaveLength(2);
    expect(screen.getAllByText("verifyCredentials")).toHaveLength(2);

    fireEvent.click(screen.getAllByText("home")[1]);
    expect(screen.getAllByText("verifyCredentials")).toHaveLength(1);
  });

  it("shows the help submenu on desktop", () => {
    render(<Navbar />);

    const helpButtons = screen.getAllByText("help");
    fireEvent.click(helpButtons[helpButtons.length - 1]);

    expect(screen.getByText("contactUs")).toBeInTheDocument();
    expect(screen.getByText("documentation")).toBeInTheDocument();
    expect(screen.getByText("faqs")).toBeInTheDocument();
  });
});
