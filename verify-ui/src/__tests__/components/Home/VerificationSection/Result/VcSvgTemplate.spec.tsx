import React from "react";
import { render, screen, waitFor } from "@testing-library/react";
import VcSvgTemplate from "../../../../../components/Home/VerificationSection/Result/VcSvgTemplate";
import { fetchSvgTemplate } from "../../../../../utils/svg-template-utils";
import QRCode from "qrcode";

jest.mock("../../../../../utils/svg-template-utils", () => ({
  fetchSvgTemplate: jest.fn(),
}));

jest.mock("qrcode", () => ({
  toDataURL: jest.fn().mockResolvedValue("data:image/png;base64,mockqr"),
}));

const mockVc = {
  id: "urn:uuid:credential-1",
  type: ["VerifiableCredential", "InsuranceCredential"],
  credentialSubject: {
    fullName: "Alice Smith",
    policyNumber: "POL-12345",
    isActive: true,
  },
};

describe("VcSvgTemplate Component", () => {
  beforeEach(() => {
    jest.clearAllMocks();
  });

  test("renders template containing standard Mustache section closing tags without unclosed section error", async () => {
    const templateWithSections = `
      <svg xmlns="http://www.w3.org/2000/svg" viewBox="0 0 400 200">
        {{#credentialSubject}}
          <text id="full-name">{{fullName}}</text>
        {{/credentialSubject}}
      </svg>
    `;
    (fetchSvgTemplate as jest.Mock).mockResolvedValueOnce(templateWithSections);

    const onError = jest.fn();
    const { container } = render(
      <VcSvgTemplate
        vc={mockVc}
        templateUrl="https://example.com/template-with-sections.svg"
        onError={onError}
      />
    );

    await waitFor(() => {
      expect(container.querySelector("#full-name")).toBeInTheDocument();
    });

    expect(container.querySelector("#full-name")?.textContent).toBe("Alice Smith");
    expect(onError).not.toHaveBeenCalled();
  });

  test("converts multi-segment JSON Pointer expressions to dot notation while preserving section tags", async () => {
    const templateWithJsonPointerAndSections = `
      <svg xmlns="http://www.w3.org/2000/svg" viewBox="0 0 400 200">
        <text id="policy">{{/credentialSubject/policyNumber}}</text>
        {{#credentialSubject}}
          <text id="subject-name">{{fullName}}</text>
        {{/credentialSubject}}
      </svg>
    `;
    (fetchSvgTemplate as jest.Mock).mockResolvedValueOnce(templateWithJsonPointerAndSections);

    const onError = jest.fn();
    const { container } = render(
      <VcSvgTemplate
        vc={mockVc}
        templateUrl="https://example.com/template-pointer.svg"
        onError={onError}
      />
    );

    await waitFor(() => {
      expect(container.querySelector("#policy")).toBeInTheDocument();
    });

    expect(container.querySelector("#policy")?.textContent).toBe("POL-12345");
    expect(container.querySelector("#subject-name")?.textContent).toBe("Alice Smith");
    expect(onError).not.toHaveBeenCalled();
  });

  test("triggers onError callback when template fetch fails", async () => {
    (fetchSvgTemplate as jest.Mock).mockResolvedValueOnce("");

    const onError = jest.fn();
    render(
      <VcSvgTemplate
        vc={mockVc}
        templateUrl="https://example.com/invalid-template.svg"
        onError={onError}
      />
    );

    await waitFor(() => {
      expect(onError).toHaveBeenCalledWith(
        expect.objectContaining({
          message: "Failed to load credential template",
        })
      );
    });
  });

  test("returns null and calls onError when Mustache render encounters invalid template syntax", async () => {
    // Malformed template with an unmatched opening tag
    const malformedTemplate = `
      <svg xmlns="http://www.w3.org/2000/svg">
        {{#unclosedTag}}
          <text>broken</text>
      </svg>
    `;
    (fetchSvgTemplate as jest.Mock).mockResolvedValueOnce(malformedTemplate);

    const onError = jest.fn();
    render(
      <VcSvgTemplate
        vc={mockVc}
        templateUrl="https://example.com/malformed.svg"
        onError={onError}
      />
    );

    await waitFor(() => {
      expect(onError).toHaveBeenCalled();
    });
  });
});
