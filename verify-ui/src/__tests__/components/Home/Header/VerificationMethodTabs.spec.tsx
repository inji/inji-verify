import React from "react";
import { fireEvent, render, screen } from "@testing-library/react";
import VerificationMethodTabs from "../../../../components/Home/Header/VerificationMethodTabs";

const dispatch = jest.fn();
const navigate = jest.fn();
const selector = jest.fn((fn: (state: any) => any) => fn({ method: "UPLOAD" }));

jest.mock("../../../../redux/hooks", () => ({ useAppDispatch: () => dispatch }));
jest.mock("../../../../redux/features/verification/verification.selector", () => ({
  useVerificationFlowSelector: (fn: (state: any) => any) => selector(fn),
}));
jest.mock("../../../../redux/features/verify/vpVerificationState", () => ({
  resetVpRequest: () => ({ type: "resetVpRequest" }),
}));
jest.mock("../../../../redux/features/verification/verification.slice", () => ({
  goToHomeScreen: (payload: any) => ({ type: "goToHomeScreen", payload }),
}));
jest.mock("react-router-dom", () => ({ useNavigate: () => navigate }));
jest.mock("react-i18next", () => ({ useTranslation: () => ({ t: (key: string) => key }) }));

describe("VerificationMethodTabs", () => {
  beforeEach(() => {
    jest.clearAllMocks();
    (window as any)._env_ = { DEFAULT_THEME: "orange" };
    localStorage.clear();
  });

  it("renders all methods and switches to scan and VP", () => {
    render(<VerificationMethodTabs />);
    expect(screen.getByRole("button", { name: "upload" })).toBeInTheDocument();
    fireEvent.click(screen.getByRole("button", { name: "scan" }));
    expect(dispatch).toHaveBeenCalledTimes(2);
    expect(navigate).toHaveBeenCalledWith("/scan");
    fireEvent.click(screen.getByRole("button", { name: "VP_Verification" }));
    expect(navigate).toHaveBeenCalledWith("/verify");
    expect(localStorage.getItem("path")).toBe("/verify");
  });

  it("shows the coming-soon alert for BLE and scrolls the carousel", () => {
    const { container } = render(<VerificationMethodTabs />);
    const carousel = container.querySelector(".overflow-x-scroll") as HTMLDivElement;
    carousel.scrollBy = jest.fn();
    fireEvent.click(screen.getByRole("button", { name: "ble" }));
    expect(dispatch).toHaveBeenCalled();
    fireEvent.click(container.querySelector("#tabs-carousel-left-icon")!);
    fireEvent.click(container.querySelector("#tabs-carousel-right-icon")!);
    expect(carousel.scrollBy).toHaveBeenCalledTimes(2);
  });
});
