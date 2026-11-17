import React from "react";
import { render, screen } from "@testing-library/react";
import i18n from "i18next";
import "../../../../../utils/i18n";
import DisplayVcDetailView from "../../../../../components/Home/VerificationSection/Result/DisplayVcDetailView";
import { raiseAlert } from "../../../../../redux/features/alerts/alerts.slice";
import { fetchSvgTemplate } from "../../../../../utils/svg-template-utils";
import en from "../../../../../locales/en.json";
import fr from "../../../../../locales/fr.json";
import ta from "../../../../../locales/ta.json";
import hi from "../../../../../locales/hi.json";
import kn from "../../../../../locales/kn.json";
import ar from "../../../../../locales/ar.json";
import pt from "../../../../../locales/pt.json";
import es from "../../../../../locales/es.json";
import km from "../../../../../locales/km.json";

// "stub": VcSvgTemplate is replaced by a stub that raises mockStubError via onError.
// "real": the real VcSvgTemplate runs against a mocked fetchSvgTemplate.
let mockTemplateMode: "stub" | "real" = "stub";
let mockStubError: Error = new Error("Failed to load credential template");

jest.mock("../../../../../components/Home/VerificationSection/Result/VcSvgTemplate", () => {
  const ReactActual = jest.requireActual("react");
  const { act } = jest.requireActual("@testing-library/react");
  const Actual = jest.requireActual(
    "../../../../../components/Home/VerificationSection/Result/VcSvgTemplate"
  ).default;
  const Stub = ({ onError }: { onError?: (error: Error) => void }) => {
    // Raise asynchronously, like the real component does after its fetch settles.
    ReactActual.useEffect(() => {
      void Promise.resolve().then(() =>
        act(() => {
          onError?.(mockStubError);
        })
      );
    }, []);
    return <div data-testid="svg-template" />;
  };
  return {
    __esModule: true,
    default: (props: any) => (mockTemplateMode === "real" ? <Actual {...props} /> : <Stub {...props} />),
  };
});

jest.mock("../../../../../utils/svg-template-utils", () => ({
  getTemplateUrl: () => "https://example.test/template.svg",
  fetchSvgTemplate: jest.fn(),
}));

jest.mock("../../../../../components/Home/VerificationSection/Result/VcDetailsGrid", () => ({
  __esModule: true,
  default: () => <div data-testid="details-grid" />,
}));

jest.mock("../../../../../utils/commonUtils", () => ({
  getDetailsOrder: () => [{ key: "name", value: "Sandra" }],
}));

jest.mock("../../../../../utils/misc", () => ({ saveData: jest.fn() }));

jest.mock("../../../../../utils/theme-utils", () => ({
  SharableLink: () => <span />,
  VectorDownload: () => <span />,
  VectorExpand: () => <span />,
  DocumentIcon: () => <span data-testid="document-icon" />,
}));

jest.mock("../../../../../components/Home/VerificationSection/commons/ActionButton", () => ({
  __esModule: true,
  default: ({ label }: { label: string }) => <button>{label}</button>,
}));

jest.mock("../../../../../components/commons/Loader", () => ({
  __esModule: true,
  default: () => <div data-testid="template-loader" />,
}));

jest.mock("qrcode", () => ({
  __esModule: true,
  default: { toDataURL: jest.fn(() => Promise.resolve("data:image/png;base64,qr")) },
}));

const mockDispatch = jest.fn();
jest.mock("../../../../../redux/hooks", () => ({
  useAppDispatch: () => mockDispatch,
}));

const mockedFetch = fetchSvgTemplate as jest.MockedFunction<typeof fetchSvgTemplate>;
const LOCALES: Record<string, any> = { en, fr, ta, hi, kn, ar, pt, es, km };
const vc = { credentialSubject: { name: "Sandra" } } as any;

const alertMessages = () =>
  mockDispatch.mock.calls
    .map(([action]) => action)
    .filter((action) => action?.type === raiseAlert.type)
    .map((action) => action.payload.message);

describe("DisplayVcDetailView after an SVG template error", () => {
  let consoleSpy: jest.SpyInstance;

  beforeEach(() => {
    jest.clearAllMocks();
    consoleSpy = jest.spyOn(console, "error").mockImplementation(() => {});
    mockTemplateMode = "stub";
    mockStubError = new Error("Failed to load credential template");
  });

  afterEach(async () => {
    consoleSpy.mockRestore();
    await i18n.changeLanguage("en");
  });

  // AC-4: fallback to the details grid is unchanged.
  test("falls back to the details grid and raises an error alert with the template error text", async () => {
    render(<DisplayVcDetailView vc={vc} onExpand={jest.fn()} />);

    expect(await screen.findByTestId("details-grid")).toBeInTheDocument();
    expect(screen.queryByTestId("svg-template")).not.toBeInTheDocument();
    expect(mockDispatch).toHaveBeenCalledWith(
      raiseAlert({ message: "Failed to load credential template", severity: "error", open: true })
    );
  });

  // Pass-through only: the stub supplies the locale text, so this proves the alert does not
  // rewrite the template's message. Translation itself is proven by the real-component tests.
  test.each(Object.keys(LOCALES))(
    "%s: the alert passes the template error message through unchanged",
    async (lang) => {
      await i18n.changeLanguage(lang);
      const text = LOCALES[lang].AlertMessages.svgTemplateLoadFailed;
      expect(text).toBeTruthy();
      mockStubError = new Error(text);

      render(<DisplayVcDetailView vc={vc} onExpand={jest.fn()} />);

      expect(await screen.findByTestId("details-grid")).toBeInTheDocument();
      expect(alertMessages()).toEqual([text]);
    }
  );

  // AC-1 + AC-3 + AC-4 end to end with the real template component.
  test.each(Object.keys(LOCALES))(
    "%s: a failed template fetch alerts in the selected language, hides the raw error and shows the grid",
    async (lang) => {
      mockTemplateMode = "real";
      await i18n.changeLanguage(lang);
      mockedFetch.mockRejectedValue(new Error("network failure"));

      render(<DisplayVcDetailView vc={vc} onExpand={jest.fn()} />);

      expect(await screen.findByTestId("details-grid")).toBeInTheDocument();
      const messages = alertMessages();
      expect(messages).toHaveLength(1);
      expect(messages[0]).not.toContain("network failure");
      const expected = LOCALES[lang].AlertMessages.svgTemplateLoadFailed;
      expect(expected).toBeTruthy();
      expect(messages[0]).toBe(expected);
    }
  );
});
