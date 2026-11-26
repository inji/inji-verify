import React from "react";
import { render } from "@testing-library/react";
import ResultSummary from "../../../../../components/Home/VerificationSection/Result/ResultSummary";

describe("Result Summary", () => {
  test("renders non-empty message and success background for SUCCESS status", () => {
    const { container } = render(<ResultSummary status="SUCCESS" />);

    const message = container.querySelector("#vc-result-display-message");
    expect(message).toBeInTheDocument();
    expect(message?.textContent).toBeTruthy();
    expect(container.querySelector("#success_message_icon")).toBeInTheDocument();
    expect(container.firstChild).toHaveClass("bg-successText");
  });

  test("renders non-empty message and invalid background for INVALID status", () => {
    const { container } = render(<ResultSummary status="INVALID" />);

    const message = container.querySelector("#vc-result-display-message");
    expect(message).toBeInTheDocument();
    expect(message?.textContent).toBeTruthy();
    expect(container.querySelector("#success_message_icon")).not.toBeInTheDocument();
    expect(container.firstChild).toHaveClass("bg-invalidText");
  });

  test("renders non-empty message and expired background for EXPIRED status", () => {
    const { container } = render(<ResultSummary status="EXPIRED" />);

    const message = container.querySelector("#vc-result-display-message");
    expect(message).toBeInTheDocument();
    expect(message?.textContent).toBeTruthy();
    expect(container.firstChild).toHaveClass("bg-expiredText");
  });

  test("renders non-empty message and revoked background for REVOKED status", () => {
    const { container } = render(<ResultSummary status="REVOKED" />);

    const message = container.querySelector("#vc-result-display-message");
    expect(message).toBeInTheDocument();
    expect(message?.textContent).toBeTruthy();
    expect(container.firstChild).toHaveClass("bg-revokedText");
  });

  test("renders non-empty message and expired background for TIMEOUT status without undefined class", () => {
    const { container } = render(<ResultSummary status="TIMEOUT" />);

    const message = container.querySelector("#vc-result-display-message");
    expect(message).toBeInTheDocument();
    expect(message?.textContent).toBeTruthy();
    expect(container.firstChild).toHaveClass("bg-expiredText");
    expect((container.firstChild as HTMLElement).className).not.toContain("undefined");
  });

  test("falls back to invalid background for unknown status", () => {
    const { container } = render(<ResultSummary status={"UNKNOWN" as any} />);

    expect(container.firstChild).toHaveClass("bg-invalidText");
  });

  test("returns null when status is null or undefined", () => {
    const { container: containerNull } = render(<ResultSummary status={null} />);
    expect(containerNull.firstChild).toBeNull();

    const { container: containerUndefined } = render(<ResultSummary status={undefined} />);
    expect(containerUndefined.firstChild).toBeNull();
  });
});
