import React from "react";
import { render } from "@testing-library/react";
import { MemoryRouter } from "react-router-dom";
import Copyrights from "../../../../components/PageTemplate/Copyrights";
import { Pages } from "../../../../utils/config";

describe("Copyrights", () => {
  test("renders copyrights content element", () => {
    const { container } = render(
      <MemoryRouter>
        <Copyrights />
      </MemoryRouter>
    );

    expect(
      container.querySelector("#copyrights-content")
    ).toBeInTheDocument();
  });

  test("uses fixed positioning on non-offline pages", () => {
    const { container } = render(
      <MemoryRouter initialEntries={[Pages.Home]}>
        <Copyrights />
      </MemoryRouter>
    );

    expect(container.firstChild).toHaveClass("fixed", "bottom-0");
  });

  test("uses normal document flow on offline page", () => {
    const { container } = render(
      <MemoryRouter initialEntries={[Pages.Offline]}>
        <Copyrights />
      </MemoryRouter>
    );

    expect(container.firstChild).not.toHaveClass("fixed");
    expect(container.firstChild).not.toHaveClass("bottom-0");
  });
});
