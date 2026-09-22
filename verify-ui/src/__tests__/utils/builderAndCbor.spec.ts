import { render } from "@testing-library/react";
import { renderGradientText } from "../../utils/builder";

describe("builder utility", () => {
  test("renders gradient text using the active theme", () => {
    const { container } = render(renderGradientText("English"));
    expect(container).toHaveTextContent("English");
  });

});
