import { fireEvent, render, screen, waitFor } from "@testing-library/react";
import DisplayVcCardView from "../../../../../components/Home/VerificationSection/Result/DisplayVcCardView";

jest.mock("../../../../../utils/misc", () => ({
  convertToId: (value: string) => value.toLowerCase(),
  convertToTitleCase: (value: string) => value,
}));
jest.mock("../../../../../utils/theme-utils", () => ({
  VectorDown: () => <span data-testid="down" />,
  VectorUp: () => <span data-testid="up" />,
}));
jest.mock("../../../../../utils/decodeSdJwt", () => ({ decodeSdJwtToken: jest.fn() }));
jest.mock("../../../../../utils/commonUtils", () => ({ getCredentialType: () => "HealthInsurance" }));
jest.mock("../../../../../utils/config", () => ({
  backgroundColorMapping: { SUCCESS: "bg-green" },
  borderColorMapping: { SUCCESS: "border-green" },
  textColorMapping: { SUCCESS: "text-green" },
}));
jest.mock("react-i18next", () => ({ useTranslation: () => ({ t: (value: string) => value }) }));
jest.mock("../../../../../components/Home/VerificationSection/Result/DisplayVcDetailView", () => ({
  __esModule: true,
  default: () => <div data-testid="detail-view" />,
}));
jest.mock("../../../../../components/Home/VerificationSection/Result/DisplayVcDetailsModal", () => ({
  __esModule: true,
  default: ({ isOpen }: { isOpen: boolean }) => isOpen ? <div data-testid="details-modal" /> : null,
}));

describe("DisplayVcCardView", () => {
  test("expands a credential card and renders its details", async () => {
    render(<DisplayVcCardView vc={{ type: "HealthInsurance" } as any} vcStatus="SUCCESS" view={false} />);
    await waitFor(() => expect(screen.getByText("HealthInsurance")).toBeInTheDocument());
    fireEvent.click(screen.getByRole("button"));
    expect(screen.getByTestId("detail-view")).toBeInTheDocument();
  });

  test("renders an already-open card without a toggle icon", async () => {
    render(<DisplayVcCardView vc={{ type: "HealthInsurance" } as any} vcStatus="SUCCESS" view={true} />);
    await waitFor(() => expect(screen.getByTestId("detail-view")).toBeInTheDocument());
    expect(screen.queryByTestId("down")).not.toBeInTheDocument();
  });
});
