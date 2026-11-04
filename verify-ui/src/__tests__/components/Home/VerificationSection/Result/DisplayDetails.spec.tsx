import { fireEvent, render, screen } from "@testing-library/react";
import VcDetailsGrid from "../../../../../components/Home/VerificationSection/Result/VcDetailsGrid";
import { getMdocDetailsOrder, MdocDetailsSections } from "../../../../../components/Home/VerificationSection/Result/VcDetailsGrid";
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
  test("does not load remote URLs as biometric images", () => {
    const { container } = render(
      <VcDetailsGrid orderedDetails={[{ key: "portrait", value: "https://tracker.example/p.png" }]} />
    );
    expect(container.querySelector("img")).toBeNull();
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
      { key: "org.iso.18013.5.1.family_name", value: "Cooper", label: "Family Name", namespace: "org.iso.18013.5.1" },
      { key: "org.iso.18013.5.2.age_over_18", value: true, label: "Age Over 18", namespace: "org.iso.18013.5.2" },
    ]);

    render(<VcDetailsGrid orderedDetails={orderedDetails} />);
    // Labels show the element name, not the namespace-qualified key.
    expect(screen.getByText("Family Name")).toBeInTheDocument();
    expect(screen.queryByText("org.iso.18013.5.1.family_name")).toBeNull();
    expect(screen.getByText("Cooper")).toBeInTheDocument();
    expect(screen.getByText("Age Over 18")).toBeInTheDocument();
    expect(screen.getByText("true")).toBeInTheDocument();
  });

  test("shows each mdoc namespace as a section holding its own claims", () => {
    const { container } = render(
      <MdocDetailsSections
        claims={{
          "org.iso.18013.5.1": { family_name: "Cooper", given_name: "Gorge" },
          "org.example.custom": { family_name: "Second" },
        }}
      />,
    );

    const sections = Array.from(container.querySelectorAll("section"));
    expect(sections.map((section) => section.querySelector("p")?.textContent)).toEqual([
      "org.iso.18013.5.1",
      "org.example.custom",
    ]);
    // The same element name in two namespaces stays under its own namespace heading.
    expect(sections[0].textContent).toContain("Cooper");
    expect(sections[0].textContent).toContain("Gorge");
    expect(sections[1].textContent).toContain("Second");
    expect(sections[1].textContent).not.toContain("Cooper");
  });

  test("lists mdoc claims by namespace only when the details view is flagged as mdoc", () => {
    const mdocClaims = { "org.iso.18013.5.1": { family_name: "Cooper" } } as any;

    const { unmount } = render(<DisplayVcDetailView vc={mdocClaims} isMdoc onExpand={jest.fn()} />);
    expect(screen.getByText("Family Name")).toBeInTheDocument();
    // The card holds the namespace sections only; the title is in the result header.
    expect(screen.queryByText("Mobile Document")).toBeNull();
    unmount();

    // Without the flag the same shape goes through the generic ordering.
    render(<DisplayVcDetailView vc={mdocClaims} onExpand={jest.fn()} />);
    expect(screen.queryByText("org.iso.18013.5.1.family_name")).toBeNull();
    expect(screen.getByText("org.iso.18013.5.1")).toBeInTheDocument();
  });

  test("renders details view and invokes expand/download actions", () => {
    const onExpand = jest.fn();
    render(<DisplayVcDetailView vc={{ name: "Sandra" } as any} onExpand={onExpand} />);

    fireEvent.click(screen.getByRole("button", { name: "expand" }));
    fireEvent.click(screen.getByRole("button", { name: "download" }));
    expect(onExpand).toHaveBeenCalledTimes(1);
  });
});
