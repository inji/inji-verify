import React from "react";
import { render } from "@testing-library/react";
import ResultSummary from "../../../../../components/Home/VerificationSection/Result/ResultSummary";

describe("Result Summary", () => {
  test("renders non-empty message for SUCCESS status", () => {
    const { container } = render(<ResultSummary status="SUCCESS" />);

    const message = container.querySelector("#vc-result-display-message");
    expect(message).toBeInTheDocument();
    expect(message?.textContent).toBeTruthy();
  });

  test("renders non-empty message for INVALID status", () => {
    const { container } = render(<ResultSummary status="INVALID" />);

    const message = container.querySelector("#vc-result-display-message");
    expect(message).toBeInTheDocument();
    expect(message?.textContent).toBeTruthy();
  });
});
