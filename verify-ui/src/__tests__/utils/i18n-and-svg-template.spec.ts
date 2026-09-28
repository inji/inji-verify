import {
  getDirCurrentLanguage,
  getLanguageCodes,
  isRTL,
  normalizeLanguageCode,
  switchLanguage,
} from "../../utils/i18n";
import { fetchSvgTemplate, getTemplateUrl } from "../../utils/svg-template-utils";

describe("language helpers", () => {
  test.each([
    ["en", "en"],
    ["ENG", "en"],
    ["pt", "pt"],
    ["unknown", "en"],
    [null, "en"],
  ])("normalizes %p", (value, expected) => {
    expect(normalizeLanguageCode(value as any)).toBe(expected);
  });

  test("returns ISO aliases and direction", () => {
    expect(getLanguageCodes("eng")).toEqual(["en", "eng"]);
    expect(getLanguageCodes("unknown")).toEqual(["en", "eng"]);
    expect(isRTL("ar")).toBe(true);
    expect(isRTL("en")).toBe(false);
    expect(getDirCurrentLanguage("ar")).toBe("rtl");
    expect(getDirCurrentLanguage("en")).toBe("ltr");
  });

  test("switches language and persists the normalized value", async () => {
    await switchLanguage("fra");
    expect(window.localStorage.getItem("selected_language")).toBe(JSON.stringify("fr"));
  });
});

describe("SVG template helpers", () => {
  test("finds an SVG mustache render method", () => {
    expect(getTemplateUrl({
      renderMethod: [
        { renderSuite: "other", template: { mediaType: "image/svg+xml", id: "no" } },
        { renderSuite: "svg-mustache", template: { mediaType: "image/svg+xml", id: "yes" } },
      ],
    } as any)).toBe("yes");
    expect(getTemplateUrl({ renderMethod: [] } as any)).toBeUndefined();
    expect(getTemplateUrl({} as any)).toBeUndefined();
  });

  test("returns response text and handles HTTP and network failures", async () => {
    (global.fetch as jest.Mock).mockResolvedValueOnce({ ok: true, text: async () => "<svg />" });
    expect(await fetchSvgTemplate("/ok.svg")).toBe("<svg />");
    (global.fetch as jest.Mock).mockResolvedValueOnce({ ok: false, statusText: "Not Found" });
    expect(await fetchSvgTemplate("/missing.svg")).toBe("");
    (global.fetch as jest.Mock).mockRejectedValueOnce(new Error("offline"));
    expect(await fetchSvgTemplate("/offline.svg")).toBe("");
  });
});
