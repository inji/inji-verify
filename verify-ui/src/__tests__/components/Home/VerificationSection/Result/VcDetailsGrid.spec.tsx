import React from "react";
import { render, screen } from "@testing-library/react";
import VcDetailsGrid from "../../../../../components/Home/VerificationSection/Result/VcDetailsGrid";

jest.mock("../../../../../utils/theme-utils", () => ({
  SharableLink: () => <span data-testid="shareable-link" />,
}));

describe("VcDetailsGrid", () => {
  it("shows biometric details first and marks selectively shared claims", () => {
    const { container } = render(
      <VcDetailsGrid
        orderedDetails={[
          { key: "Full Name", value: "Asha" },
          { key: "Face", value: ["data:image/png;base64,image"] },
          { key: "Phone Number", value: "1234567890" },
        ]}
        vc={{ disclosedClaims: { "full name": true } } as any}
      />,
    );

    expect(screen.getByAltText("Face")).toHaveAttribute("src", "data:image/png;base64,image");
    expect(screen.getByText("Full Name")).toBeInTheDocument();
    expect(screen.getByText("Asha")).toBeInTheDocument();
    expect(screen.getByText("Phone Number")).toBeInTheDocument();
    expect(screen.getByTestId("shareable-link")).toBeInTheDocument();
    expect(container.querySelectorAll("img")[0]).toHaveAttribute("alt", "Face");
  });

  it("renders normal details when no selectively shared claims exist", () => {
    render(
      <VcDetailsGrid
        orderedDetails={[{ key: "Email", value: "asha@example.org" }]}
        vc={{} as any}
      />,
    );

    expect(screen.getByText("Email")).toBeInTheDocument();
    expect(screen.getByText("asha@example.org")).toBeInTheDocument();
    expect(screen.queryByTestId("shareable-link")).not.toBeInTheDocument();
  });
});
