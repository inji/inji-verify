import React from "react";
import { render, screen, fireEvent } from "@testing-library/react";
import CameraAccessDenied from "../../../../components/Home/VerificationSection/CameraAccessDenied";

// mock icon to avoid SVG issues
jest.mock("../../../../utils/theme-utils", () => ({
  CameraAccessDeniedIcon: () => <div data-testid="camera-access-denied-icon" />,
}));

// mock button (optional; keeps test light)
jest.mock(
  "../../../../components/Home/VerificationSection/commons/Button",
  () => ({
    Button: ({ title, onClick, ...props }: any) => (
      <button onClick={onClick} {...props}>
        {title}
      </button>
    ),
  })
);

// mock translations
jest.mock("react-i18next", () => ({
  useTranslation: () => ({
    t: (key: string) => {
      const dict: Record<string, string> = {
        header: "Camera access denied",
        description: "Please allow camera permissions in your browser.",
        okay: "OK",
      };
      return dict[key] || key;
    },
  }),
}));

describe("CameraAccessDenied Component", () => {
  test("does not render when open=false", () => {
    render(<CameraAccessDenied open={false} handleClose={jest.fn()} />);
    // modal should not exist in the document
    expect(screen.queryByText("Camera access denied")).not.toBeInTheDocument();
    expect(
      screen.queryByTestId("camera-access-denied-icon")
    ).not.toBeInTheDocument();
  });

  test("renders modal with correct content when open=true", () => {
    render(<CameraAccessDenied open={true} handleClose={jest.fn()} />);

    expect(screen.getByTestId("camera-access-denied-icon")).toBeInTheDocument();
    expect(screen.getByText("Camera access denied")).toBeInTheDocument();
    expect(
      screen.getByText("Please allow camera permissions in your browser.")
    ).toBeInTheDocument();
    expect(screen.getByRole("button", { name: "OK" })).toBeInTheDocument();
  });

  test("calls handleClose when OK button is clicked", () => {
    const mockClose = jest.fn();
    render(<CameraAccessDenied open={true} handleClose={mockClose} />);

    fireEvent.click(screen.getByRole("button", { name: "OK" }));
    expect(mockClose).toHaveBeenCalledTimes(1);
  });

  test("calls handleClose when clicking overlay", () => {
    const mockClose = jest.fn();
    render(<CameraAccessDenied open={true} handleClose={mockClose} />);

    // overlay is the button with aria-label
    fireEvent.click(screen.getByLabelText("Close modal"));
    expect(mockClose).toHaveBeenCalledTimes(1);
  });
});
