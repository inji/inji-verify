import React from "react";
import { render } from "@testing-library/react";
import Copyrights from "../../../../components/PageTemplate/Copyrights";

describe("Copyrights", () => {
  test("renders copyrights content element", () => {
    const { container } = render(<Copyrights />);

    expect(
      container.querySelector("#copyrights-content")
    ).toBeInTheDocument();
  });
});
