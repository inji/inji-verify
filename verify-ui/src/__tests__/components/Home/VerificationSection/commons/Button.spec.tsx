import React from "react";
import { render, screen, fireEvent } from "@testing-library/react";
import { Button } from "../../../../../components/Home/VerificationSection/commons/Button";

describe("Button Component", () => {
  beforeEach(() => {
    // Mock theme value
    (window as any)._env_ = { DEFAULT_THEME: "primary" };
  });

  test("renders with title text", () => {
    render(<Button title="Click Me" />);
    expect(screen.getByText("Click Me")).toBeInTheDocument();
  });

  test("renders with given id", () => {
    render(<Button title="Click" id="test-btn" />);
    expect(screen.getByRole("button")).toHaveAttribute("id", "test-btn");
  });

  test("calls onClick when enabled", () => {
    const onClick = jest.fn();
    render(<Button title="Click" onClick={onClick} />);
    fireEvent.click(screen.getByRole("button"));
    expect(onClick).toHaveBeenCalledTimes(1);
  });

  test("does not call onClick when disabled", () => {
    const onClick = jest.fn();
    render(<Button title="Click" onClick={onClick} disabled />);
    fireEvent.click(screen.getByRole("button"));
    expect(onClick).not.toHaveBeenCalled();
  });

  test("renders icon when provided", () => {
    const Icon = <span data-testid="icon">⭐</span>;
    render(<Button title="Star" icon={Icon} />);
    expect(screen.getByTestId("icon")).toBeInTheDocument();
  });

  test("applies fill variant styles by default", () => {
    render(<Button title="Fill Button" />);
    const btn = screen.getByRole("button");
    expect(btn.className).toContain("text-white");
  });

  test("applies outline variant styles", () => {
    render(<Button title="Outline Button" variant="outline" />);
    const btn = screen.getByRole("button");
    expect(btn.className).toContain("bg-white");
  });

  test("applies clear variant styles", () => {
    render(<Button title="Clear Button" variant="clear" />);
    const btn = screen.getByRole("button");
    expect(btn.className).toContain("bg-transparent");
  });

  test("applies disabled styles", () => {
    render(<Button title="Disabled" disabled />);
    const btn = screen.getByRole("button");
    expect(btn).toBeDisabled();
    expect(btn.className).toContain("bg-disabledButtonBg");
  });
});
