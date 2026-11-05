/**
 * @jest-environment-options {"customExportConditions": ["node", "require"]}
 */
// jsdom resolves cbor-x to its ES module entry, which Jest cannot load; the node condition
// picks the CommonJS build of the same release.
import { decode, encode, Tag } from "cbor-x";
// Imported for its side effect: it registers the tag 0 extension with cbor-x.
import "../../utils/cborUtils";

jest.mock("@injistack/pixelpass", () => ({ decodeMappedData: jest.fn() }));

// Uses the real cbor-x (cborUtils.spec.ts mocks it) to check the tag 0 extension against the
// cbor-x version pinned in package.json.
describe("cborUtils tdate extension with the real cbor-x", () => {
  it("decodes a tdate (tag 0) to the text that was sent, keeping its offset", () => {
    expect(decode(encode(new Tag("2024-05-03T23:30:00-05:00", 0)))).toBe("2024-05-03T23:30:00-05:00");
  });

  it("decodes an unparseable tdate to its text", () => {
    expect(decode(encode(new Tag("not-a-date", 0)))).toBe("not-a-date");
  });

  it("decodes a tdate inside an element value map", () => {
    expect(decode(encode({ elementValue: new Tag("1985-04-29T00:00:00+05:30", 0) }))).toEqual({
      elementValue: "1985-04-29T00:00:00+05:30",
    });
  });
});
