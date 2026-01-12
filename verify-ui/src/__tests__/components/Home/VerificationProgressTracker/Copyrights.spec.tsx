import { render, screen } from "@testing-library/react";
import Copyrights from "../../../../components/PageTemplate/Copyrights";

// Mock i18next translation hook
jest.mock("react-i18next", () => ({
  useTranslation: () => ({
    t: (key: string) =>
      key === "content" ? "© DriveZen All rights reserved" : key,
  }),
}));

describe("Copyrights Component", () => {
  test("renders translation text from i18n", () => {
    render(<Copyrights />);

    // ensure the text is displayed
    expect(
      screen.getByText("© DriveZen All rights reserved")
    ).toBeInTheDocument();
  });

  test("renders with correct id for content paragraph", () => {
    render(<Copyrights />);

    const el = screen.getByText("© DriveZen All rights reserved");
    expect(el).toBeInTheDocument();
    expect(el).toHaveAttribute("id", "copyrights-content");
  });
});
