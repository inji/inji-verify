import React from "react";
import { render, screen } from "@testing-library/react";
import CameraAccessDenied from "../../../../components/Home/VerificationSection/CameraAccessDenied";

describe("Camera access denied", () => {
  test("renders header and description elements", () => {
    const handleCloseMock = jest.fn();
    render(<CameraAccessDenied open={true} handleClose={handleCloseMock} />);

    const header = screen.getByText((_, element) => {
      return element?.id === "camera-access-denied";
    });
    const description = screen.getByText((_, element) => {
      return element?.id === "camera-access-denied-description";
    });

    expect(header).toBeInTheDocument();
    expect(description).toBeInTheDocument();
  });
});
