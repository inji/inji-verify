import React from "react";
import { render, screen, fireEvent, act } from "@testing-library/react";
import AlertMessage from "../../../components/commons/AlertMessage";

// Mock CloseIcon
jest.mock("../../../utils/theme-utils", () => ({
  CloseIcon: () => <div data-testid="close-icon" />,
}));

// Mock Redux hooks
jest.mock("../../../redux/hooks", () => ({
  useAppDispatch: jest.fn(),
}));

jest.mock("../../../redux/features/alerts/alerts.selector", () => ({
  useAlertsSelector: jest.fn(),
}));

// Mock reducer action
jest.mock("../../../redux/features/alerts/alerts.slice", () => ({
  closeAlert: jest.fn(() => ({ type: "alerts/close" })),
}));

describe("AlertMessage Component", () => {
  const {
    useAlertsSelector,
  } = require("../../../redux/features/alerts/alerts.selector");
  const { useAppDispatch } = require("../../../redux/hooks");
  const { closeAlert } = require("../../../redux/features/alerts/alerts.slice");

  const mockDispatch = jest.fn();
  beforeEach(() => {
    jest.clearAllMocks();
    useAppDispatch.mockReturnValue(mockDispatch);
  });

  test("renders simple alert message when no detailed fields", () => {
    useAlertsSelector.mockReturnValue({
      open: true,
      severity: "success",
      message: "Operation completed",
      autoHideDuration: 3000,
    });

    render(<AlertMessage isRtl={false} />);

    expect(screen.getByText("Operation completed")).toBeInTheDocument();
  });

  test("renders detailed alert when errorCode, errorReason or referenceId exist", () => {
    useAlertsSelector.mockReturnValue({
      open: true,
      severity: "error",
      title: "Failed",
      message: "Request failed",
      errorCode: "500",
      errorReason: "Server error",
      referenceId: "REF123",
      autoHideDuration: 3000,
    });

    render(<AlertMessage isRtl={false} />);

    expect(screen.getByText("Failed")).toBeInTheDocument();
    expect(screen.getByText("Request failed")).toBeInTheDocument();
    expect(screen.getByText("Error Code:")).toBeInTheDocument();
    expect(screen.getByText("500")).toBeInTheDocument();
    expect(screen.getByText("Reason:")).toBeInTheDocument();
    expect(screen.getByText("Server error")).toBeInTheDocument();
    expect(screen.getByText("Reference ID:")).toBeInTheDocument();
    expect(screen.getByText("REF123")).toBeInTheDocument();
  });

  test("dispatches closeAlert when close button is clicked", () => {
    useAlertsSelector.mockReturnValue({
      open: true,
      message: "Closable alert",
    });

    render(<AlertMessage isRtl={false} />);

    const closeBtn = screen.getByRole("button", { name: /close/i });
    fireEvent.click(closeBtn);

    expect(mockDispatch).toHaveBeenCalledWith(closeAlert({}));
  });

  test("auto-hides alert after timeout", () => {
    jest.useFakeTimers();

    useAlertsSelector.mockReturnValue({
      open: true,
      message: "Timeout alert",
      autoHideDuration: 2000,
    });

    render(<AlertMessage isRtl={false} />);

    act(() => {
      jest.advanceTimersByTime(2000);
    });

    expect(mockDispatch).toHaveBeenCalledWith(closeAlert({}));

    jest.useRealTimers();
  });

  test("hides alert visually when open=false", () => {
    (useAlertsSelector as jest.Mock).mockReturnValue({
      open: false,
      message: "Hidden alert",
      severity: "success",
    });

    render(<AlertMessage isRtl={false} />);

    // message is still in the DOM
    expect(screen.getByText("Hidden alert")).toBeInTheDocument();

    // wrapper determines visual hiding
    expect(screen.getByTestId("alert-wrapper")).toHaveClass("hidden");
  });
});
