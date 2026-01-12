import { render, screen, fireEvent } from "@testing-library/react";
import VcDisplayCard from "../../../../../components/Home/VerificationSection/Result/VcDisplayCard";

// --- MOCK UTILS ---
jest.mock("../../../../../utils/misc", () => ({
  convertToId: jest.fn((key) => key),
  convertToTitleCase: jest.fn((key) => key),
  getDisplayValue: jest.fn((v) => v),
  saveData: jest.fn(),
}));

// --- MOCK ICONS ---
jest.mock("../../../../../utils/theme-utils", () => ({
  DocumentIcon: () => <div data-testid="document-icon" />,
  VectorExpand: ({ onClick }: any) => (
    <button data-testid="expand-icon" onClick={onClick} />
  ),
  VectorDownload: ({ onClick }: any) => (
    <button data-testid="download-icon" onClick={onClick} />
  ),
  VectorOutline: "outline.png",
}));

describe("VcDisplayCard", () => {
  const vcWithData = {
    credentialSubject: {
      fullName: "John Doe",
      gender: "Male",
      dob: "1990-01-01",
      benefits: ["Benefit A"],
      policyName: "Health",
      policyNumber: "123",
      policyIssuedOn: "2021-01-01",
      policyExpiresOn: "2031-01-01",
      mobile: "1234567890",
      email: "test@example.com",
    },
  };

  test("renders all ordered details when credentialSubject exists", () => {
    render(<VcDisplayCard vc={vcWithData} onExpand={() => {}} />);

    const expectedKeys = [
      "fullName",
      "gender",
      "dob",
      "benefits",
      "policyName",
      "policyNumber",
      "policyIssuedOn",
      "policyExpiresOn",
      "mobile",
      "email",
    ] as const;

    type SubjectKey = typeof expectedKeys[number];

    expectedKeys.forEach((key: SubjectKey) => {
      expect(screen.getByText(key)).toBeInTheDocument();

      const value = vcWithData.credentialSubject[key];

      // Handle benefits array
      const displayValue = Array.isArray(value) ? value.join(", ") : value;

      expect(screen.getByText(displayValue)).toBeInTheDocument();
    });
  });

  test("renders DocumentIcon when credentialSubject is missing", () => {
    render(<VcDisplayCard vc={{}} onExpand={() => {}} />);

    expect(screen.getByTestId("document-icon")).toBeInTheDocument();
  });

  test("calls onExpand when expand icon is clicked", () => {
    const handleExpand = jest.fn();

    render(<VcDisplayCard vc={vcWithData} onExpand={handleExpand} />);

    fireEvent.click(screen.getByTestId("expand-icon"));

    expect(handleExpand).toHaveBeenCalledTimes(1);
  });

  test("calls saveData when download icon is clicked", () => {
    const { saveData } = require("../../../../../utils/misc");

    render(<VcDisplayCard vc={vcWithData} onExpand={() => {}} />);

    fireEvent.click(screen.getByTestId("download-icon"));

    expect(saveData).toHaveBeenCalledTimes(1);
    expect(saveData).toHaveBeenCalledWith(vcWithData);
  });

  test("handles null values by displaying 'N/A'", () => {
    const { getDisplayValue } = require("../../../../../utils/misc");
    getDisplayValue.mockImplementation((v: string) =>
      v ? (v as string) : "N/A"
    );

    const vcMissing = {
      credentialSubject: {
        fullName: null,
      },
    };

    render(<VcDisplayCard vc={vcMissing} onExpand={() => {}} />);

    // assert key visible
    expect(screen.getByText("fullName")).toBeInTheDocument();

    // assert one or more N/A rendered
    const naNodes = screen.getAllByText("N/A");
    expect(naNodes.length).toBeGreaterThan(0);
  });
});
