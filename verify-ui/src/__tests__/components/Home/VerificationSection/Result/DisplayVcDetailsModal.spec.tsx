import React from "react";
import { render, screen, fireEvent } from "@testing-library/react";
import DisplayVcDetailsModal from "../../../../../components/Home/VerificationSection/Result/DisplayVcDetailsModal";

jest.mock("react-i18next", () => ({
  useTranslation: () => ({
    t: (key: string) => key,
    i18n: { changeLanguage: jest.fn(), language: "en" },
  }),
  initReactI18next: {
    type: "3rdParty",
    init: jest.fn(),
  },
}));

jest.mock("../../../../../utils/commonUtils", () => ({
  getDetailsOrder: jest.fn(() => [{ key: "fullName", value: "Test User" }]),
}));

const mockGetTemplateUrl = jest.fn();
jest.mock("../../../../../utils/svg-template-utils", () => ({
  getTemplateUrl: (...args: any[]) => mockGetTemplateUrl(...args),
}));

jest.mock(
  "../../../../../components/Home/VerificationSection/Result/VcSvgTemplate",
  () => {
    const React = require("react");
    return {
      __esModule: true,
      default: ({ onError }: { onError?: (err: Error) => void }) =>
        React.createElement(
          "button",
          {
            "data-testid": "vc-svg-template",
            onClick: () => onError?.(new Error("Template load failed")),
          },
          "SVG Template"
        ),
    };
  }
);

jest.mock(
  "../../../../../components/Home/VerificationSection/Result/VcDetailsGrid",
  () => {
    const React = require("react");
    return {
      __esModule: true,
      default: () =>
        React.createElement("div", { "data-testid": "vc-details-grid" }, "Details Grid"),
    };
  }
);

const mockVc: any = {
  id: "did:example:123",
  type: ["VerifiableCredential", "InsuranceCredential"],
  credentialSubject: { fullName: "Test User" },
};

describe("DisplayVcDetailsModal", () => {
  beforeEach(() => {
    jest.clearAllMocks();
  });

  test("falls back to VcDetailsGrid when VcSvgTemplate triggers onError", () => {
    mockGetTemplateUrl.mockReturnValue("https://example.com/template.svg");

    render(
      <DisplayVcDetailsModal
        isOpen={true}
        onClose={jest.fn()}
        vc={mockVc}
        status="SUCCESS"
        vcType="InsuranceCredential"
      />
    );

    const svgTemplate = screen.getByTestId("vc-svg-template");
    expect(svgTemplate).toBeInTheDocument();

    fireEvent.click(svgTemplate);

    expect(screen.queryByTestId("vc-svg-template")).not.toBeInTheDocument();
    expect(screen.getByTestId("vc-details-grid")).toBeInTheDocument();
  });

  test("renders VcDetailsGrid directly when no templateUrl is present", () => {
    mockGetTemplateUrl.mockReturnValue(undefined);

    render(
      <DisplayVcDetailsModal
        isOpen={true}
        onClose={jest.fn()}
        vc={mockVc}
        status="SUCCESS"
        vcType="InsuranceCredential"
      />
    );

    expect(screen.getByTestId("vc-details-grid")).toBeInTheDocument();
  });
});
