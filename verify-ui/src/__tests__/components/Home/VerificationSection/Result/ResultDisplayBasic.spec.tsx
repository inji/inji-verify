import React from "react";
import { render, screen } from "@testing-library/react";
import DisplayUnVerifiedVc from "../../../../../components/Home/VerificationSection/Result/DisplayUnVerifiedVc";
import DisplayVcDetailsModal from "../../../../../components/Home/VerificationSection/Result/DisplayVcDetailsModal";

jest.mock("../../../../../utils/theme-utils", () => ({
  VectorCollapse: () => <span data-testid="collapse-icon" />,
  VectorDownload: () => <span data-testid="download-icon" />,
}));

jest.mock("../../../../../components/Home/VerificationSection/Result/VcDetailsGrid", () => () => (
  <div data-testid="vc-details-grid" />
));

jest.mock("../../../../../components/Home/VerificationSection/Result/VcSvgTemplate", () => () => (
  <div data-testid="vc-svg-template" />
));

jest.mock("../../../../../components/Home/VerificationSection/commons/ActionButton", () => ({
  label,
  onClick,
}: { label: string; onClick: () => void }) => (
  <button onClick={onClick}>{label}</button>
));

describe("result display components", () => {
  test("shows an unverified credential", () => {
    render(<DisplayUnVerifiedVc claim={{ name: "Health Insurance", logo: "logo.png" } as any} />);
    expect(screen.getByText("Health Insurance")).toBeInTheDocument();
  });

  test("does not render the details modal when closed", () => {
    const { container } = render(
      <DisplayVcDetailsModal
        isOpen={false}
        onClose={jest.fn()}
        vc={{} as any}
        status={"SUCCESS" as any}
        vcType="Health Insurance"
      />,
    );
    expect(container.firstChild).toBeNull();
  });
});
