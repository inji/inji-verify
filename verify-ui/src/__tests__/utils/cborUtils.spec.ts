import { decode, decodeMultiple } from "cbor-x";
import { decodeMappedData } from "@injistack/pixelpass";
import { extractMappedClaim, isCWT, uint8ArrayToHex } from "../../utils/cborUtils";

jest.mock("cbor-x", () => ({
  decode: jest.fn(),
  decodeMultiple: jest.fn(),
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
