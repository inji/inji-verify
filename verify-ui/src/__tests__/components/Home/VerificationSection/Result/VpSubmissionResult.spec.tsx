import { fireEvent, render, screen } from "@testing-library/react";
import VpSubmissionResult from "../../../../../components/Home/VerificationSection/Result/VpSubmissionResult";

jest.mock("react-i18next", () => ({ useTranslation: () => ({ t: (value: string) => value }) }));
jest.mock("../../../../../redux/hooks", () => ({ useAppDispatch: () => jest.fn() }));
jest.mock("../../../../../redux/features/verification/verification.selector", () => ({
  useVerifyFlowSelector: (selector: (state: any) => any) => selector({
    originalSelectedCredentials: [], isPartiallyShared: true, isShowResult: true,
  }),
}));
jest.mock("../../../../../redux/features/verify/vpVerificationState", () => ({ resetVpRequest: () => ({ type: "reset" }) }));
jest.mock("../../../../../utils/config", () => ({ DisplayTimeout: 100000 }));
jest.mock("../../../../../components/Home/VerificationSection/Result/ResultSummary", () => ({ __esModule: true, default: () => <div data-testid="summary" /> }));
jest.mock("../../../../../components/Home/VerificationSection/Result/VpVerifyResultSummary", () => ({ __esModule: true, default: () => <div data-testid="vp-summary" /> }));
jest.mock("../../../../../components/Home/VerificationSection/Result/DisplayVcCardView", () => ({ __esModule: true, default: () => <div data-testid="vc-card" /> }));
jest.mock("../../../../../components/Home/VerificationSection/Result/DisplayUnVerifiedVc", () => ({ __esModule: true, default: () => <div data-testid="unverified" /> }));

jest.mock("../../../../../components/Home/VerificationSection/commons/Button", () => ({
  Button: ({ id, title, onClick }: any) => <button id={id} onClick={onClick}>{title}</button>,
}));

describe("VpSubmissionResult", () => {
  test("renders partial-share actions and invokes callbacks", () => {
    const requestMissingCredentials = jest.fn();
    const restart = jest.fn();
    render(<VpSubmissionResult verifiedVcs={[]} unverifiedCredentials={[{} as any]} requestCredentials={jest.fn()} requestMissingCredentials={requestMissingCredentials} restart={restart} isSingleVc={false} />);
    fireEvent.click(screen.getByRole("button", { name: "missingCredentials" }));
    fireEvent.click(screen.getByRole("button", { name: "restartProcess" }));
    expect(requestMissingCredentials).toHaveBeenCalled();
    expect(restart).toHaveBeenCalled();
    expect(screen.getByTestId("unverified")).toBeInTheDocument();
  });
});
