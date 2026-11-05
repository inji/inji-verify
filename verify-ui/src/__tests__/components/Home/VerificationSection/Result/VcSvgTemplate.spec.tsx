import React from "react";
import { render, screen, waitFor } from "@testing-library/react";
import VcSvgTemplate from "../../../../../components/Home/VerificationSection/Result/VcSvgTemplate";
import { fetchSvgTemplate } from "../../../../../utils/svg-template-utils";
import QRCode from "qrcode";
import Mustache from "mustache";
import i18n from "i18next";
import "../../../../../utils/i18n";
import en from "../../../../../locales/en.json";
import fr from "../../../../../locales/fr.json";
import ta from "../../../../../locales/ta.json";
import hi from "../../../../../locales/hi.json";
import kn from "../../../../../locales/kn.json";
import ar from "../../../../../locales/ar.json";
import pt from "../../../../../locales/pt.json";
import es from "../../../../../locales/es.json";
import km from "../../../../../locales/km.json";

jest.mock("../../../../../utils/svg-template-utils", () => ({
  fetchSvgTemplate: jest.fn(),
}));

jest.mock("qrcode", () => ({
  __esModule: true,
  default: { toDataURL: jest.fn(() => Promise.resolve("data:image/png;base64,qr")) },
}));

jest.mock("mustache", () => ({
  __esModule: true,
  default: { render: jest.fn((template: string) => template) },
}));

jest.mock("../../../../../components/commons/Loader", () => ({
  __esModule: true,
  default: () => <div data-testid="template-loader" />,
}));

const mockedFetch = fetchSvgTemplate as jest.MockedFunction<typeof fetchSvgTemplate>;
const mockedQr = QRCode.toDataURL as jest.MockedFunction<typeof QRCode.toDataURL>;
const mockedMustache = Mustache.render as jest.MockedFunction<typeof Mustache.render>;

describe("VcSvgTemplate", () => {
  const vc = { credentialSubject: { name: "Sandra" } } as any;

  beforeEach(() => {
    jest.clearAllMocks();
    mockedQr.mockResolvedValue("data:image/png;base64,qr");
    mockedMustache.mockImplementation((template: string, view: any) =>
      template.replace("{{credentialSubject.name}}", view.credentialSubject.name)
    );
  });

  test("renders a fetched and sanitized SVG template", async () => {
    mockedFetch.mockResolvedValue('<svg><text>{{/credentialSubject/name}}</text><script>alert(1)</script></svg>');

    render(<VcSvgTemplate vc={vc} templateUrl="https://example.test/template.svg" />);

    expect(await screen.findByText("Sandra")).toBeInTheDocument();
    expect(screen.queryByText("alert(1)")).not.toBeInTheDocument();
    expect(mockedFetch).toHaveBeenCalledWith("https://example.test/template.svg");
  });

  test("returns no content when a template URL is not provided", () => {
    const { container } = render(<VcSvgTemplate vc={vc} templateUrl="" />);
    expect(container).toBeEmptyDOMElement();
    expect(mockedFetch).not.toHaveBeenCalled();
  });

  describe("template errors (en)", () => {
    let consoleSpy: jest.SpyInstance;
    beforeEach(async () => {
      consoleSpy = jest.spyOn(console, "error").mockImplementation(() => {});
      await i18n.changeLanguage("en");
    });
    afterEach(() => consoleSpy.mockRestore());

    // AC-4: English load-failure text is unchanged.
    test("reports template loading failures with the English load text", async () => {
      const onError = jest.fn();
      mockedFetch.mockResolvedValue("");

      render(<VcSvgTemplate vc={vc} templateUrl="broken.svg" onError={onError} />);

      await waitFor(() => expect(onError).toHaveBeenCalledWith(expect.any(Error)));
      expect(onError.mock.calls[0][0].message).toBe("Failed to load credential template");
    });

    // AC-3 (en): the raw fetch error is never surfaced.
    test("reports fetch exceptions with the English load text, not the raw error", async () => {
      const onError = jest.fn();
      mockedFetch.mockRejectedValue(new Error("network failure"));

      render(<VcSvgTemplate vc={vc} templateUrl="offline.svg" onError={onError} />);

      await waitFor(() => expect(onError).toHaveBeenCalledWith(expect.any(Error)));
      const message = onError.mock.calls[0][0].message;
      expect(message).not.toContain("network failure");
      expect(message).toBe("Failed to load credential template");
    });

    // AC-2 / AC-3 (en): render failures use the render text, not the Mustache error.
    test("reports rendering exceptions with the English render text, not the raw error", async () => {
      const onError = jest.fn();
      mockedFetch.mockResolvedValue("<svg>{{name}}</svg>");
      mockedMustache.mockImplementationOnce(() => {
        throw new Error("invalid template");
      });

      render(<VcSvgTemplate vc={vc} templateUrl="invalid.svg" onError={onError} />);

      await waitFor(() => expect(onError).toHaveBeenCalledWith(expect.any(Error)));
      const message = onError.mock.calls[0][0].message;
      expect(message).not.toContain("invalid template");
      expect(en.AlertMessages).toHaveProperty("svgTemplateRenderFailed");
      expect(message).toBe((en.AlertMessages as any).svgTemplateRenderFailed);
    });
  });

  describe("error messages follow the selected language", () => {
    const LOCALES: Record<string, any> = { en, fr, ta, hi, kn, ar, pt, es, km };
    const LANGS = Object.keys(LOCALES);
    const KEYS = ["svgTemplateLoadFailed", "svgTemplateRenderFailed"];
    const textFor = (lang: string, key: string) => {
      const text = LOCALES[lang].AlertMessages[key];
      expect(text).toBeTruthy();
      return text;
    };

    let consoleSpy: jest.SpyInstance;
    beforeEach(() => {
      consoleSpy = jest.spyOn(console, "error").mockImplementation(() => {});
    });
    afterEach(async () => {
      consoleSpy.mockRestore();
      await i18n.changeLanguage("en");
    });

    test.each(LANGS)("%s has text for each template error", (lang) => {
      KEYS.forEach((key) => {
        expect(LOCALES[lang].AlertMessages[key]).toBeTruthy();
      });
    });

    test.each(LANGS.filter((lang) => lang !== "en"))(
      "%s template error text differs from English",
      (lang) => {
        KEYS.forEach((key) => {
          expect(LOCALES[lang].AlertMessages[key]).toBeTruthy();
          expect(LOCALES[lang].AlertMessages[key]).not.toBe(LOCALES.en.AlertMessages[key]);
        });
      }
    );

    // AC-1
    test.each(LANGS)("%s: reports a template that cannot be loaded in the selected language", async (lang) => {
      await i18n.changeLanguage(lang);
      const onError = jest.fn();
      mockedFetch.mockResolvedValue("");

      render(<VcSvgTemplate vc={vc} templateUrl="broken.svg" onError={onError} />);

      await waitFor(() => expect(onError).toHaveBeenCalled());
      expect(onError.mock.calls[0][0].message).toBe(textFor(lang, "svgTemplateLoadFailed"));
    });

    // AC-1 + AC-3
    test.each(LANGS)("%s: reports a failed fetch in the selected language, not the raw error", async (lang) => {
      await i18n.changeLanguage(lang);
      const onError = jest.fn();
      mockedFetch.mockRejectedValue(new Error("network failure"));

      render(<VcSvgTemplate vc={vc} templateUrl="offline.svg" onError={onError} />);

      await waitFor(() => expect(onError).toHaveBeenCalled());
      const message = onError.mock.calls[0][0].message;
      expect(message).not.toContain("network failure");
      expect(message).toBe(textFor(lang, "svgTemplateLoadFailed"));
    });

    // AC-2 + AC-3
    test.each(LANGS)("%s: reports a template that cannot be rendered in the selected language", async (lang) => {
      await i18n.changeLanguage(lang);
      const onError = jest.fn();
      mockedFetch.mockResolvedValue("<svg>{{name}}</svg>");
      mockedMustache.mockImplementationOnce(() => {
        throw new Error("invalid template");
      });

      render(<VcSvgTemplate vc={vc} templateUrl="invalid.svg" onError={onError} />);

      await waitFor(() => expect(onError).toHaveBeenCalled());
      const message = onError.mock.calls[0][0].message;
      expect(message).not.toContain("invalid template");
      expect(message).toBe(textFor(lang, "svgTemplateRenderFailed"));
    });

    // AC-1: the language is read when the error happens, not when the module loads.
    test("uses the newly selected language for an error raised after a language switch", async () => {
      await i18n.changeLanguage("fr");
      const onError = jest.fn();
      mockedFetch.mockRejectedValue(new Error("network failure"));

      const { rerender } = render(<VcSvgTemplate vc={vc} templateUrl="first.svg" onError={onError} />);
      await waitFor(() => expect(onError).toHaveBeenCalledTimes(1));
      expect(onError.mock.calls[0][0].message).toBe(textFor("fr", "svgTemplateLoadFailed"));

      await i18n.changeLanguage("hi");
      rerender(<VcSvgTemplate vc={vc} templateUrl="second.svg" onError={onError} />);
      await waitFor(() => expect(onError).toHaveBeenCalledTimes(2));
      expect(onError.mock.calls[1][0].message).toBe(textFor("hi", "svgTemplateLoadFailed"));
    });

    // AC-1/AC-2/AC-3 for non-Error throws (the "Failed to fetch/render template" fallbacks).
    test.each(["en", "fr"])("%s: a non-Error fetch rejection reports the load text, never the thrown value", async (lang) => {
      await i18n.changeLanguage(lang);
      const onError = jest.fn();
      mockedFetch.mockRejectedValue("boom");

      render(<VcSvgTemplate vc={vc} templateUrl="offline.svg" onError={onError} />);

      await waitFor(() => expect(onError).toHaveBeenCalled());
      const message = onError.mock.calls[0][0].message;
      expect(message).not.toContain("boom");
      expect(message).toBe(textFor(lang, "svgTemplateLoadFailed"));
    });

    test.each(["en", "fr"])("%s: a non-Error render throw reports the render text, never the thrown value", async (lang) => {
      await i18n.changeLanguage(lang);
      const onError = jest.fn();
      mockedFetch.mockResolvedValue("<svg>{{name}}</svg>");
      mockedMustache.mockImplementationOnce(() => {
        // eslint-disable-next-line no-throw-literal -- intentional non-Error throw exercises the render fallback
        throw "boom";
      });

      render(<VcSvgTemplate vc={vc} templateUrl="invalid.svg" onError={onError} />);

      await waitFor(() => expect(onError).toHaveBeenCalled());
      const message = onError.mock.calls[0][0].message;
      expect(message).not.toContain("boom");
      expect(message).toBe(textFor(lang, "svgTemplateRenderFailed"));
    });

    test("keeps the English text of the load failure", () => {
      expect(LOCALES.en.AlertMessages.svgTemplateLoadFailed).toBe("Failed to load credential template");
    });
  });
});
