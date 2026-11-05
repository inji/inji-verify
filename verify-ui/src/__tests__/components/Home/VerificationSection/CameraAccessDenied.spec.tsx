import React from "react";
import { afterAll, beforeAll, beforeEach, describe, expect, jest, test } from "@jest/globals";
import { fireEvent, render, screen } from "@testing-library/react";
import CameraAccessDenied from "../../../../components/Home/VerificationSection/CameraAccessDenied";

// jsdom does not implement the native dialog lifecycle.
const originalShowModal = Object.getOwnPropertyDescriptor(HTMLDialogElement.prototype, "showModal");
const originalClose = Object.getOwnPropertyDescriptor(HTMLDialogElement.prototype, "close");
const showModal = jest.fn(function (this: HTMLDialogElement) { this.open = true; });
const close = jest.fn(function (this: HTMLDialogElement) { this.open = false; });

beforeAll(() => {
  Object.defineProperty(HTMLDialogElement.prototype, "showModal", { configurable: true, value: showModal });
  Object.defineProperty(HTMLDialogElement.prototype, "close", { configurable: true, value: close });
});

afterAll(() => {
  if (originalShowModal) Object.defineProperty(HTMLDialogElement.prototype, "showModal", originalShowModal);
  else delete (HTMLDialogElement.prototype as Partial<HTMLDialogElement>).showModal;
  if (originalClose) Object.defineProperty(HTMLDialogElement.prototype, "close", originalClose);
  else delete (HTMLDialogElement.prototype as Partial<HTMLDialogElement>).close;
});

beforeEach(() => {
  showModal.mockClear();
  close.mockClear();
});

describe("Camera access denied", () => {
  test("opens a named native modal and closes it on unmount", () => {
    const { unmount } = render(<CameraAccessDenied open handleClose={jest.fn()} />);
    const dialog = screen.getByRole("dialog");
    expect(dialog).toHaveAttribute("aria-labelledby", "camera-access-denied");
    expect(dialog).toHaveAttribute("aria-describedby", "camera-access-denied-description");
    expect(showModal).toHaveBeenCalledTimes(1);
    unmount();
    expect(close).toHaveBeenCalledTimes(1);
  });

  test("returns focus to the opener when React removes the modal", () => {
    const opener = document.createElement("button");
    document.body.appendChild(opener);
    opener.focus();
    const { unmount } = render(<CameraAccessDenied open handleClose={jest.fn()} />);
    screen.getByTestId("camera-access-denied-okay").focus();
    unmount();
    expect(opener).toHaveFocus();
    opener.remove();
  });

  test("does not open a dialog when closed", () => {
    render(<CameraAccessDenied open={false} handleClose={jest.fn()} />);
    expect(screen.queryByRole("dialog")).not.toBeInTheDocument();
    expect(showModal).not.toHaveBeenCalled();
  });

  test("handles native Escape cancellation", () => {
    const handleClose = jest.fn();
    render(<CameraAccessDenied open handleClose={handleClose} />);
    fireEvent(screen.getByRole("dialog"), new Event("cancel", { cancelable: true }));
    expect(handleClose).toHaveBeenCalledTimes(1);
  });

  test("closes on backdrop clicks but keeps content clicks inside the modal", () => {
    const handleClose = jest.fn();
    render(<CameraAccessDenied open handleClose={handleClose} />);
    const dialog = screen.getByRole("dialog");
    fireEvent.click(document.getElementById("camera-access-denied")!);
    expect(handleClose).not.toHaveBeenCalled();
    fireEvent.click(dialog);
    expect(handleClose).toHaveBeenCalledTimes(1);
  });
});
