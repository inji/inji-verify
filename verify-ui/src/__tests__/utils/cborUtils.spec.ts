import { decode, decodeMultiple, Tag } from "cbor-x";
import { decodeMappedData } from "@injistack/pixelpass";
import { extractMappedClaim, extractMdocDisplayClaims, isCWT, isMsoMdoc, uint8ArrayToHex } from "../../utils/cborUtils";

jest.mock("cbor-x", () => ({
  decode: jest.fn(),
  decodeMultiple: jest.fn(),
  Tag: class Tag {
    constructor(public value: unknown, public tag: number) {}
  },
}));

jest.mock("@injistack/pixelpass", () => ({
  decodeMappedData: jest.fn(),
}));

const mockDecode = decode as jest.MockedFunction<typeof decode>;
const mockDecodeMultiple = decodeMultiple as jest.MockedFunction<typeof decodeMultiple>;
const mockDecodeMappedData = decodeMappedData as jest.MockedFunction<typeof decodeMappedData>;

describe("cborUtils", () => {
  let consoleWarnSpy: jest.SpyInstance;

  beforeEach(() => {
    jest.clearAllMocks();
    consoleWarnSpy = jest.spyOn(console, "warn").mockImplementation(() => undefined);
  });

  afterEach(() => consoleWarnSpy.mockRestore());

  it("converts bytes to lowercase hexadecimal", () => {
    expect(uint8ArrayToHex(new Uint8Array([0, 15, 16, 255]))).toBe("000f10ff");
  });

  it("recognises binary and valid hexadecimal CWT values", () => {
    expect(isCWT(new Uint8Array([1, 2]))).toBe(true);
    expect(isCWT(new Uint8Array([1, 2]).buffer)).toBe(true);
    expect(isCWT("A0ff")).toBe(true);
  });

  it("groups mdoc data elements by namespace for the result screen", () => {
    mockDecode.mockReturnValue({
      docType: "org.iso.18013.5.1.mDL",
      issuerSigned: {
        nameSpaces: {
          "org.iso.18013.5.1": [
            { elementIdentifier: "family_name", elementValue: "Cooper" },
            { elementIdentifier: "birth_date", elementValue: new Tag("1985-04-29", 1004) },
          ],
        },
      },
    } as never);

    expect(isMsoMdoc("omppc3N1ZXJBdXRo")).toBe(true);
    mockDecode.mockClear();
    expect(extractMdocDisplayClaims("omppc3N1ZXJBdXRo")).toEqual({
      "org.iso.18013.5.1": {
        family_name: "Cooper",
        birth_date: "1985-04-29",
      },
    });
    expect(mockDecode).toHaveBeenCalledTimes(1);
    expect(isMsoMdoc("header.payload.signature")).toBe(false);
  });

  it("keeps plain map values with tag and value keys and rejects duplicate identifiers", () => {
    mockDecode.mockReturnValue({
      issuerSigned: {
        nameSpaces: {
          "org.iso.18013.5.1": [
            { elementIdentifier: "note", elementValue: { tag: 1, value: "x", label: "important" } },
          ],
        },
      },
    } as never);
    expect(extractMdocDisplayClaims("omppc3N1ZXJBdXRo")).toEqual({
      "org.iso.18013.5.1": { note: { tag: 1, value: "x", label: "important" } },
    });

    mockDecode.mockReturnValue({
      issuerSigned: {
        nameSpaces: {
          "org.iso.18013.5.1": [
            { elementIdentifier: "family_name", elementValue: "Cooper" },
            { elementIdentifier: "family_name", elementValue: "Smith" },
          ],
        },
      },
    } as never);
    expect(() => extractMdocDisplayClaims("omppc3N1ZXJBdXRo")).toThrow(
      "Duplicate mdoc element identifier: org.iso.18013.5.1.family_name",
    );
  });

  it("recognizes a corrupt mdoc and reports an mdoc decoding error", () => {
    const raw = new Uint8Array([
      0xa1,
      0x6c,
      ...Array.from("issuerSigned", (character) => character.charCodeAt(0)),
    ]);
    const corruptMdoc = btoa(String.fromCharCode(...raw))
      .replace(/\+/g, "-")
      .replace(/\//g, "_")
      .replace(/=+$/, "");
    mockDecode.mockImplementation(() => {
      throw new Error("Invalid CBOR");
    });

    expect(isMsoMdoc(corruptMdoc)).toBe(true);
    expect(() => extractMdocDisplayClaims(corruptMdoc)).toThrow(
      "Failed to decode mdoc credential",
    );
  });

  it("uses document claims before issuerSigned and direct namespaces in mixed input", () => {
    mockDecode.mockReturnValue({
      documents: [{
        docType: "verified-document",
        issuerSigned: {
          nameSpaces: {
            verified: [{ elementIdentifier: "family_name", elementValue: "Verified" }],
          },
        },
      }],
      issuerSigned: {
        nameSpaces: {
          unverified: [{ elementIdentifier: "family_name", elementValue: "Unverified issuerSigned" }],
        },
      },
      issuerAuth: [],
      nameSpaces: {
        unverified: [{ elementIdentifier: "family_name", elementValue: "Unverified direct" }],
      },
    } as never);

    expect(extractMdocDisplayClaims("omppc3N1ZXJBdXRo")).toEqual({
      verified: { family_name: "Verified" },
    });
  });

  it("renders a decoded CBOR date instead of an empty object", () => {
    const birth = new Date("1985-04-29T00:00:00.000Z");
    mockDecode.mockReturnValue({
      docType: "org.iso.18013.5.1.mDL",
      issuerAuth: [],
      nameSpaces: {
        "org.iso.18013.5.1": [
          { elementIdentifier: "birth_date", elementValue: birth },
        ],
      },
    } as never);

    expect(extractMdocDisplayClaims("omppc3N1ZXJBdXRo")["org.iso.18013.5.1"].birth_date).toBe(
      birth.toISOString(),
    );
  });

  it("keeps colliding element identifiers in separate namespaces and omits docType", () => {
    mockDecode.mockReturnValue({
      docType: "org.iso.18013.5.1.mDL",
      issuerAuth: [],
      nameSpaces: {
        "org.iso.18013.5.1": [
          { elementIdentifier: "family_name", elementValue: "First" },
        ],
        "org.iso.18013.5.1.custom": [
          { elementIdentifier: "family_name", elementValue: "Second" },
        ],
      },
    } as never);

    expect(extractMdocDisplayClaims("omppc3N1ZXJBdXRo")).toEqual({
      "org.iso.18013.5.1": { family_name: "First" },
      "org.iso.18013.5.1.custom": { family_name: "Second" },
    });
  });

  it("encodes binary values as Base64 and labels only image fields", () => {
    mockDecode.mockReturnValue({
      issuerAuth: [],
      nameSpaces: {
        "org.iso.18013.5.1": [
          { elementIdentifier: "portrait", elementValue: new Uint8Array([1, 2, 3]) },
          { elementIdentifier: "signature_usual_mark", elementValue: new Uint8Array([4, 5]) },
          { elementIdentifier: "other_bytes", elementValue: new Uint8Array([6, 7, 8]) },
        ],
      },
    } as never);

    expect(extractMdocDisplayClaims("omppc3N1ZXJBdXRo")).toEqual({
      "org.iso.18013.5.1": {
        portrait: "image (3 bytes)",
        signature_usual_mark: "image (2 bytes)",
        other_bytes: btoa(String.fromCharCode(6, 7, 8)),
      },
    });
  });

  it("rejects malformed mdoc namespaces and items", () => {
    mockDecode.mockReturnValue({
      docType: "org.iso.18013.5.1.mDL",
      issuerAuth: [],
      nameSpaces: {
        "org.iso.18013.5.1": { not: "an-array" },
      },
    } as never);
    expect(() => extractMdocDisplayClaims("omppc3N1ZXJBdXRo")).toThrow(
      "Invalid mdoc namespace: expected an item array",
    );

    mockDecode.mockReturnValue({
      docType: "org.iso.18013.5.1.mDL",
      issuerAuth: [],
      nameSpaces: {
        "org.iso.18013.5.1": [{ elementValue: "missing-identifier" }],
      },
    } as never);
    expect(() => extractMdocDisplayClaims("omppc3N1ZXJBdXRo")).toThrow(
      "Invalid mdoc issuer-signed item",
    );
  });

  it("rejects invalid CWT values", () => {
    expect(isCWT("")).toBe(false);
    expect(isCWT("abc")).toBe(false);
    expect(isCWT("zz")).toBe(false);
    expect(isCWT({})).toBe(false);
    expect(consoleWarnSpy).toHaveBeenCalledTimes(2);
  });

  it("extracts and maps a claim from a valid CWT", () => {
    const claimBytes = new Uint8Array([202, 254]);
    mockDecodeMultiple.mockReturnValue([
      { tag: 61, value: { value: ["protected", "unprotected", new Uint8Array([1]), "signature"] } },
    ] as never);
    mockDecode.mockReturnValue({ 169: claimBytes } as never);
    mockDecodeMappedData.mockReturnValue({ fullName: "Test User" } as never);

    expect(extractMappedClaim("a0", 169)).toEqual({ fullName: "Test User" });
    expect(mockDecodeMappedData).toHaveBeenCalledWith("cafe");
  });

  it.each([
    ["has no CBOR items", []],
    ["does not contain CWT tag 61", [{ tag: 1, value: {} }]],
    ["does not contain a valid COSE Sign1 array", [{ tag: 61, value: { value: [] } }]],
    ["does not contain a payload", [{ tag: 61, value: { value: [1, 2, null, 4] } }]],
  ])("throws when the CWT %s", (_description, decodedItems) => {
    mockDecodeMultiple.mockReturnValue(decodedItems as never);

    expect(() => extractMappedClaim("a0", 169)).toThrow();
  });

  it("throws when the requested claim is absent", () => {
    mockDecodeMultiple.mockReturnValue([
      { tag: 61, value: { value: [1, 2, new Uint8Array([1]), 4] } },
    ] as never);
    mockDecode.mockReturnValue({} as never);

    expect(() => extractMappedClaim("a0", 169)).toThrow("Claim 169 not found in CWT");
  });
});
