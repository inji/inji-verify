import { getTemplateUrl, fetchSvgTemplate } from "../../utils/svg-template-utils";
import { AnyVc } from "../../types/data-types";

describe("svg-template-utils", () => {
  const originalFetch = global.fetch;

  afterEach(() => {
    jest.clearAllMocks();
    global.fetch = originalFetch;
  });

  describe("getTemplateUrl", () => {
    test("returns undefined safely when vc is null, undefined, or a string", () => {
      expect(getTemplateUrl(null as unknown as AnyVc)).toBeUndefined();
      expect(getTemplateUrl(undefined as unknown as AnyVc)).toBeUndefined();
      expect(getTemplateUrl("string-vc" as unknown as AnyVc)).toBeUndefined();
    });

    test("returns undefined when vc has no renderMethod", () => {
      const vc = {
        id: "urn:uuid:123",
        type: ["VerifiableCredential"],
      } as AnyVc;

      expect(getTemplateUrl(vc)).toBeUndefined();
    });

    test("extracts template URL when renderMethod is an array of objects", () => {
      const vc = {
        id: "urn:uuid:123",
        type: ["VerifiableCredential"],
        renderMethod: [
          {
            renderSuite: "other-suite",
            template: { id: "https://example.com/other", mediaType: "text/html" },
          },
          {
            renderSuite: "svg-mustache",
            template: {
              id: "https://example.com/template.svg",
              mediaType: "image/svg+xml",
            },
          },
        ],
      } as unknown as AnyVc;

      expect(getTemplateUrl(vc)).toBe("https://example.com/template.svg");
    });

    test("extracts template URL when renderMethod is a single object conforming to W3C VC spec", () => {
      const vc = {
        id: "urn:uuid:123",
        type: ["VerifiableCredential"],
        renderMethod: {
          id: "https://example.com/renderMethod/1",
          type: "SvgMustacheTemplate",
          renderSuite: "svg-mustache",
          template: {
            id: "https://example.com/single-template.svg",
            mediaType: "image/svg+xml",
          },
        },
      } as unknown as AnyVc;

      expect(getTemplateUrl(vc)).toBe("https://example.com/single-template.svg");
    });

    test("returns undefined when renderSuite is not svg-mustache", () => {
      const vc = {
        id: "urn:uuid:123",
        type: ["VerifiableCredential"],
        renderMethod: {
          renderSuite: "html-mustache",
          template: {
            id: "https://example.com/template.html",
            mediaType: "image/svg+xml",
          },
        },
      } as unknown as AnyVc;

      expect(getTemplateUrl(vc)).toBeUndefined();
    });

    test("returns undefined when mediaType is not image/svg+xml", () => {
      const vc = {
        id: "urn:uuid:123",
        type: ["VerifiableCredential"],
        renderMethod: {
          renderSuite: "svg-mustache",
          template: {
            id: "https://example.com/template.svg",
            mediaType: "text/plain",
          },
        },
      } as unknown as AnyVc;

      expect(getTemplateUrl(vc)).toBeUndefined();
    });
  });

  describe("fetchSvgTemplate", () => {
    test("returns template string when fetch succeeds", async () => {
      const mockSvg = "<svg><text>Credential</text></svg>";
      global.fetch = jest.fn().mockResolvedValueOnce({
        ok: true,
        text: jest.fn().mockResolvedValueOnce(mockSvg),
      } as unknown as Response);

      const result = await fetchSvgTemplate("https://example.com/template.svg");
      expect(result).toBe(mockSvg);
      expect(global.fetch).toHaveBeenCalledWith("https://example.com/template.svg");
    });

    test("returns empty string and logs error when response is not ok", async () => {
      const consoleErrorSpy = jest.spyOn(console, "error").mockImplementation(() => {});
      global.fetch = jest.fn().mockResolvedValueOnce({
        ok: false,
        statusText: "Not Found",
      } as unknown as Response);

      const result = await fetchSvgTemplate("https://example.com/not-found.svg");
      expect(result).toBe("");
      expect(consoleErrorSpy).toHaveBeenCalledWith(
        "Failed to fetch SVG template:",
        "Not Found"
      );
      consoleErrorSpy.mockRestore();
    });

    test("returns empty string and logs error when fetch encounters network failure", async () => {
      const consoleErrorSpy = jest.spyOn(console, "error").mockImplementation(() => {});
      const networkError = new Error("Network offline");
      global.fetch = jest.fn().mockRejectedValueOnce(networkError);

      const result = await fetchSvgTemplate("https://example.com/template.svg");
      expect(result).toBe("");
      expect(consoleErrorSpy).toHaveBeenCalledWith(
        "Error fetching SVG template:",
        networkError
      );
      consoleErrorSpy.mockRestore();
    });
  });
});
