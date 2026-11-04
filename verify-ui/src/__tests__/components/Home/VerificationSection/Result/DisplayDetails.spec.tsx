import { fireEvent, render, screen } from "@testing-library/react";
import VcDetailsGrid from "../../../../../components/Home/VerificationSection/Result/VcDetailsGrid";
import {
  getMdocDetailsOrder,
  hasMdocNamespace,
} from "../../../../../components/Home/VerificationSection/Result/VcDetailsGrid";
import DisplayVcDetailView from "../../../../../components/Home/VerificationSection/Result/DisplayVcDetailView";

jest.mock("../../../../../utils/misc", () => ({
  convertToId: (value: string) => value.toLowerCase().replace(/\s+/g, "-"),
  convertToTitleCase: (value: string) => value,
  getDisplayValue: (value: unknown) => String(value),
  getDetailsOrder: (claims: Record<string, unknown>) =>
    Object.entries(claims).map(([key, value]) => ({ key, value })),
  saveData: jest.fn(),
}));

jest.mock("../../../../../utils/svg-template-utils", () => ({
  getTemplateUrl: () => undefined,
}));

jest.mock("../../../../../utils/i18n", () => ({
  getLanguageCodes: () => ["en"],
  isRTL: () => false,
}));

jest.mock("react-i18next", () => ({
  useTranslation: () => ({ t: (value: string) => value, i18n: { language: "en" } }),
}));

jest.mock("../../../../../utils/theme-utils", () => ({
  SharableLink: () => <span data-testid="share-icon" />,
  VectorDownload: () => <span />,
  VectorExpand: () => <span />,
  DocumentIcon: () => <span data-testid="document-icon" />,
}));

jest.mock("../../../../../components/Home/VerificationSection/commons/ActionButton", () => ({
  __esModule: true,
  default: ({ label, onClick }: { label: string; onClick: () => void }) => (
    <button onClick={onClick}>{label}</button>
  ),
}));

jest.mock("../../../../../redux/hooks", () => ({
  useAppDispatch: () => jest.fn(),
}));

describe("credential details display", () => {
  test("detects mdoc claims by their namespace", () => {
    expect(hasMdocNamespace({ "org.iso.18013.5.1": { family_name: "Cooper" } })).toBe(true);
    expect(hasMdocNamespace({ family_name: "Cooper" })).toBe(false);
  });

  test("renders disclosed claims and biometric values", () => {
    render(
      <VcDetailsGrid
        orderedDetails={[
          { key: "name", value: "Sandra" },
          { key: "face", value: ["data:image/png;base64,abc"] },
        ]}
        vc={{ disclosedClaims: { name: "Sandra" } } as any}
      />,
    );

    expect(screen.getByText("name")).toBeInTheDocument();
    expect(screen.getByTestId("share-icon")).toBeInTheDocument();
    expect(screen.getByAltText("face")).toBeInTheDocument();
  });

  test("converts grouped mdoc claims into namespace-qualified details for the existing grid", () => {
    const orderedDetails = getMdocDetailsOrder({
      "org.iso.18013.5.1": { family_name: "Cooper" },
      "org.iso.18013.5.2": { age_over_18: true },
    });

    expect(orderedDetails).toEqual([
      { key: "org.iso.18013.5.1.family_name", value: "Cooper" },
      { key: "org.iso.18013.5.2.age_over_18", value: true },
    ]);
    expect(
      getMdocDetailsOrder({
        docType: "Mobile Document",
        "org.iso.18013.5.1": { family_name: "Cooper" },
      } as any),
    ).toEqual([
      { key: "docType", value: "Mobile Document" },
      { key: "org.iso.18013.5.1.family_name", value: "Cooper" },
    ]);

    render(<VcDetailsGrid orderedDetails={orderedDetails} />);
    expect(screen.getByText("org.iso.18013.5.1.family_name")).toBeInTheDocument();
    expect(screen.getByText("Cooper")).toBeInTheDocument();
    expect(screen.getByText("org.iso.18013.5.2.age_over_18")).toBeInTheDocument();
    expect(screen.getByText("true")).toBeInTheDocument();
  });

  test("renders details view and invokes expand/download actions", () => {
    const onExpand = jest.fn();
    render(<DisplayVcDetailView vc={{ name: "Sandra" } as any} onExpand={onExpand} />);

    fireEvent.click(screen.getByRole("button", { name: "expand" }));
    fireEvent.click(screen.getByRole("button", { name: "download" }));
    expect(onExpand).toHaveBeenCalledTimes(1);
  });
});
