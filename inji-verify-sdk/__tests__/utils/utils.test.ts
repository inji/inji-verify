import {
  isDcApiSupported,
  isMobileDevice,
  isSignedRequestScheme,
  isChromeDcApiSecurityVersionMet,
  isDcqlVpToken,
  parseVpTokenFromFragment,
  extractVcFromVpToken,
  getRawHashParam,
} from "../../src/utils/utils";
import { DC_API_PROTOCOL } from "../../src/utils/constants";

const DID_CLIENT_ID = "decentralized_identifier:did:web:example.com";
const X509_CLIENT_ID = "x509_san_dns:verify.example.com";
const CHROME_SECURE_UA =
  "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/144.0.7559.59 Safari/537.36";
const CHROME_INSECURE_UA =
  "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/144.0.7559.58 Safari/537.36";

describe("isSignedRequestScheme", () => {
  it("accepts DID and x509_san_dns client ids", () => {
    expect(isSignedRequestScheme(DID_CLIENT_ID)).toBe(true);
    expect(isSignedRequestScheme(X509_CLIENT_ID)).toBe(true);
  });

  it("rejects pre-registered and missing client ids", () => {
    expect(isSignedRequestScheme("pre_registered:client")).toBe(false);
    expect(isSignedRequestScheme("x509_san_dnsfoo")).toBe(false);
    expect(isSignedRequestScheme(undefined)).toBe(false);
  });
});

describe("isChromeDcApiSecurityVersionMet", () => {
  it("allows non-Chromium user agents", () => {
    expect(
      isChromeDcApiSecurityVersionMet(
        "Mozilla/5.0 (iPhone; CPU iPhone OS 17_0 like Mac OS X) AppleWebKit/605.1.15 (KHTML, like Gecko) Version/17.0 Mobile/15E148 Safari/604.1",
      ),
    ).toBe(true);
  });

  it("rejects Chrome older than 144.0.7559.59", () => {
    expect(isChromeDcApiSecurityVersionMet(CHROME_INSECURE_UA)).toBe(false);
    expect(
      isChromeDcApiSecurityVersionMet(
        "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/141.0.0.0 Safari/537.36",
      ),
    ).toBe(false);
  });

  it("accepts Chrome 144.0.7559.59 and later", () => {
    expect(isChromeDcApiSecurityVersionMet(CHROME_SECURE_UA)).toBe(true);
    expect(
      isChromeDcApiSecurityVersionMet(
        "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/145.0.0.0 Safari/537.36",
      ),
    ).toBe(true);
  });
});

describe("isDcApiSupported", () => {
  const originalDigitalCredential = window.DigitalCredential;
  const originalUserAgent = navigator.userAgent;

  afterEach(() => {
    Object.defineProperty(navigator, "userAgent", {
      configurable: true,
      value: originalUserAgent,
    });
    if (originalDigitalCredential === undefined) {
      delete (window as { DigitalCredential?: unknown }).DigitalCredential;
    } else {
      window.DigitalCredential = originalDigitalCredential;
    }
  });

  const mockDcApi = () => {
    window.DigitalCredential = {
      userAgentAllowsProtocol: jest.fn((protocol: string) => protocol === DC_API_PROTOCOL),
    } as unknown as typeof DigitalCredential;
  };

  it("returns false when clientId is not a signed-request scheme", () => {
    mockDcApi();
    expect(isDcApiSupported("pre_registered:client")).toBe(false);
  });

  it("returns false when DigitalCredential is missing", () => {
    delete (window as { DigitalCredential?: unknown }).DigitalCredential;
    expect(isDcApiSupported(DID_CLIENT_ID)).toBe(false);
  });

  it("returns true when DID clientId and protocol are allowed", () => {
    mockDcApi();
    expect(isDcApiSupported(DID_CLIENT_ID)).toBe(true);
    expect(window.DigitalCredential.userAgentAllowsProtocol).toHaveBeenCalledWith(
      DC_API_PROTOCOL,
    );
  });

  it("returns true when x509_san_dns clientId and protocol are allowed", () => {
    mockDcApi();
    expect(isDcApiSupported(X509_CLIENT_ID)).toBe(true);
  });

  it("returns false on Chrome versions affected by CVE-2026-0904", () => {
    Object.defineProperty(navigator, "userAgent", {
      configurable: true,
      value: CHROME_INSECURE_UA,
    });
    mockDcApi();
    expect(isDcApiSupported(DID_CLIENT_ID)).toBe(false);
  });
});

describe("vp token redirect helpers", () => {
  it("detects DCQL vp_token shape", () => {
    expect(isDcqlVpToken({ "cred-id": [{}] })).toBe(true);
    // Shape alone cannot distinguish PE when query id is "verifiableCredential".
    expect(isDcqlVpToken({ verifiableCredential: [{}] })).toBe(true);
    expect(isDcqlVpToken(null)).toBe(false);
    expect(isDcqlVpToken([])).toBe(false);
    expect(isDcqlVpToken({ foo: "bar" })).toBe(false);
  });

  it("parses URL-encoded vp_token fragments", () => {
    const encoded = encodeURIComponent(
      JSON.stringify({ "cred-id": [{ type: ["VerifiableCredential"] }] })
    );
    expect(parseVpTokenFromFragment(encoded)).toEqual({
      "cred-id": [{ type: ["VerifiableCredential"] }],
    });
  });

  it("restores spaces from form-urlencoded + in vp_token fragments", () => {
    const payload = {
      "cred-id": [
        {
          type: ["VerifiableCredential"],
          credentialSubject: {
            fullName: "Alheri Bobby",
            phone: "+919427357934",
            region: "yuan wee 3",
          },
        },
      ],
    };
    // Java URLEncoder / form-urlencoded: space → +, literal + → %2B
    const formEncoded = encodeURIComponent(JSON.stringify(payload)).replace(
      /%20/g,
      "+"
    );
    expect(formEncoded).toContain("Alheri+Bobby");
    expect(formEncoded).toContain("%2B919427357934");
    expect(parseVpTokenFromFragment(formEncoded)).toEqual(payload);
  });

  it("parses plain JSON vp_token fragments", () => {
    const payload = {
      "cred-id": [
        {
          type: ["VerifiableCredential"],
          credentialSubject: { fullName: "José García", city: "München" },
        },
      ],
    };
    expect(parseVpTokenFromFragment(JSON.stringify(payload))).toEqual(payload);
  });

  it("preserves literal percent escapes in plain JSON vp_token fragments", () => {
    const payload = {
      "cred-id": [
        {
          type: ["VerifiableCredential"],
          credentialSubject: { note: "discount 50% off", code: "%7Bnot-encoded%7D" },
        },
      ],
    };
    expect(parseVpTokenFromFragment(JSON.stringify(payload))).toEqual(payload);
  });

  it("preserves literal percent escapes when reading vp_token from the hash", () => {
    const payload = {
      "cred-id": [
        {
          type: ["VerifiableCredential"],
          credentialSubject: { code: "%7Bnot-encoded%7D" },
        },
      ],
    };
    const hash = `#vp_token=${JSON.stringify(payload)}`;
    const raw = getRawHashParam(hash, "vp_token");
    expect(raw).toBe(JSON.stringify(payload));
    expect(parseVpTokenFromFragment(raw as string)).toEqual(payload);
    // URLSearchParams would decode the literal %7B / %7D sequences.
    expect(new URLSearchParams(hash.slice(1)).get("vp_token")).not.toBe(
      JSON.stringify(payload)
    );
  });

  it("extracts a VC from DCQL vp_token", () => {
    const vc = {
      type: ["VerifiableCredential"],
      credentialSubject: { id: "1" },
    };
    expect(
      extractVcFromVpToken({
        "6426c84e-88af-4a6a-bd47-007ba549e3c9": [vc],
      })
    ).toEqual(vc);
  });

  it("extracts a VC nested in a presentation from DCQL vp_token", () => {
    const vc = { type: ["VerifiableCredential"] };
    expect(
      extractVcFromVpToken({
        query_id: [{ type: ["VerifiablePresentation"], verifiableCredential: [vc] }],
      })
    ).toEqual(vc);
  });

  it("rejects null presentation entries in DCQL vp_token", () => {
    expect(() => extractVcFromVpToken({ id: [null] })).toThrow(
      "Empty credential entry in vp_token"
    );
  });

  it("rejects non-array-valued vp_token objects", () => {
    expect(() => extractVcFromVpToken({ foo: "bar" })).toThrow(
      "Unsupported vp_token format in redirect URL"
    );
  });
});

describe("isMobileDevice", () => {
  const originalUserAgent = navigator.userAgent;

  afterEach(() => {
    Object.defineProperty(navigator, "userAgent", {
      configurable: true,
      value: originalUserAgent,
    });
  });

  it("detects iPhone user agents", () => {
    Object.defineProperty(navigator, "userAgent", {
      configurable: true,
      value:
        "Mozilla/5.0 (iPhone; CPU iPhone OS 17_0 like Mac OS X) AppleWebKit/605.1.15 (KHTML, like Gecko) Version/17.0 Mobile/15E148 Safari/604.1",
    });
    expect(isMobileDevice()).toBe(true);
  });

  it("does not treat desktop Chrome as mobile", () => {
    Object.defineProperty(navigator, "userAgent", {
      configurable: true,
      value:
        "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/120.0.0.0 Safari/537.36",
    });
    expect(isMobileDevice()).toBe(false);
  });
});
