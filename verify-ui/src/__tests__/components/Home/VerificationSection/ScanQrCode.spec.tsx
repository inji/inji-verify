import React from "react";
import { render, screen, fireEvent, waitFor } from "@testing-library/react";
import { ScanQrCode } from "../../../../components/Home/VerificationSection/ScanQrCode";
import * as reduxHooks from "../../../../redux/hooks";
import * as verificationSlice from "../../../../redux/features/verification/verification.slice";
import * as appStateSlice from "../../../../redux/features/application-state/application-state.slice";
import * as miscUtils from "../../../../utils/misc";

// ---- Mocks ----

// mock window env
beforeAll(() => {
  (window as any)._env_ = {
    DEFAULT_THEME: "default_theme",
  };
});

// mock redux dispatch
const mockDispatch = jest.fn();
jest.spyOn(reduxHooks, "useAppDispatch").mockReturnValue(mockDispatch);

// mock i18n translations
jest.mock("react-i18next", () => ({
  useTranslation: () => ({
    t: (key: string) => key, // return key literal
  }),
}));

// mock selectors & actions
jest
  .spyOn(verificationSlice, "qrReadInit")
  .mockReturnValue({ type: "qrReadInit" } as any);
jest
  .spyOn(appStateSlice, "updateInternetConnectionStatus")
  .mockReturnValue({ type: "updateNet" } as any);

// mock misc checkInternetStatus
jest.spyOn(miscUtils, "checkInternetStatus").mockResolvedValue(true);

describe("ScanQrCode Component", () => {
  test("renders UI correctly", () => {
    render(<ScanQrCode />);
    expect(screen.getByTestId("scan-button")).toBeInTheDocument();
    expect(screen.getByTestId("trigger-scan")).toBeInTheDocument();
  });

  test("fires internet check + triggers scan on click", async () => {
    render(<ScanQrCode />);

    const scanButton = screen.getByTestId("scan-button");
    const triggerBtn = screen.getByTestId("trigger-scan");

    const triggerClickSpy = jest.spyOn(triggerBtn, "click");

    fireEvent.click(scanButton);

    await waitFor(() => {
      expect(mockDispatch).toHaveBeenCalledTimes(3);
    });

    expect(triggerClickSpy).toHaveBeenCalled();
  });
});
