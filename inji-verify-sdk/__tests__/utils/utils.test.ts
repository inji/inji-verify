import {
  isDcApiSupported,
  isMobileDevice,
  isSignedRequestScheme,
  isChromeDcApiSecurityVersionMet,
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
