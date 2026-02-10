import React from "react";
import { render, screen } from "@testing-library/react";
import { Button } from "../../../../../components/Home/VerificationSection/commons/Button";

describe("Styled Button", () => {
  test("renders button title", () => {
    render(<Button title="A Styled Button" />);
    expect(screen.getByText("A Styled Button")).toBeInTheDocument();
  });
});

