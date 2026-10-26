import { decodeMultiple, decode } from "cbor-x";
import { decodeMappedData } from "@injistack/pixelpass";

export function uint8ArrayToHex(uint8Array: Uint8Array): string {
  return Array.from(uint8Array)
    .map((b) => b.toString(16).padStart(2, "0"))
    .join("");
}

/**
 * Converts a hex string to Uint8Array
 * @throws Error if hex string is invalid
 */
function hexToBytes(hex: string): Uint8Array {
  if (hex.length % 2 !== 0) {
    throw new Error("Invalid hex string: odd length");
  }

  if (!/^[0-9a-fA-F]*$/.test(hex)) {
    throw new Error("Invalid hex string: contains non-hex characters");
  }

  const bytes = new Uint8Array(hex.length / 2);
  for (let i = 0; i < hex.length; i += 2) {
    bytes[i / 2] = Number.parseInt(hex.slice(i, i + 2), 16);
  }
  return bytes;
}

/**
 * Checks if the given value is a valid CWT (CBOR Web Token)
 * Accepts Uint8Array, ArrayBuffer, or hex string format
 */
export function isCWT(vc: unknown): boolean {
  try {
    // Check if already in binary format
    if (vc instanceof Uint8Array || vc instanceof ArrayBuffer) {
      return true;
    }

    // Check if it's a valid hex string
    if (typeof vc === "string" && vc.length > 0) {
      hexToBytes(vc);
      return true;
    }

    return false;
  } catch (err) {
    console.warn(
      "Failed to validate CWT format:",
      err instanceof Error ? err.message : String(err),
    );
    return false;
  }
}

/**
 * Extract a numeric claim from a verified CWT payload and return mapped JSON.
 * @param cwtHex Hex string of the CWT
 * @param claimNumber Numeric claim number (e.g., 169)
 * @returns Decoded and mapped claim JSON
 */
export function extractMappedClaim(
  cwtHex: string,
  claimNumber: number,
): object {
  const bytes = hexToBytes(cwtHex);

  const iterable = decodeMultiple(bytes);
  if (!iterable) throw new Error("CBOR decodeMultiple returned nothing");

  const items: any[] = [];
  for (const item of iterable) items.push(item);
  if (items.length === 0) throw new Error("No CBOR items found");

  let tagged = items[0];
  if (!tagged || tagged.tag !== 61) {
    throw new Error("Expected CWT (CBOR tag 61) at top level");
  }

  const taggedValue = tagged.value;
  const coseSign1 = taggedValue.value;

  if (!Array.isArray(coseSign1) || coseSign1.length !== 4) {
    throw new Error("Expected COSE_Sign1 array with 4 elements");
  }

  const payloadBytes = coseSign1[2];
  if (!payloadBytes) throw new Error("CWT payload is missing");

  const claims = decode(payloadBytes);

  const claimData = claims[claimNumber];
  if (!claimData) throw new Error(`Claim ${claimNumber} not found in CWT`);

  const claimHex = uint8ArrayToHex(claimData);
  const decodedMappedData = decodeMappedData(claimHex);

  return decodedMappedData;
}

function base64UrlToBytes(value: string): Uint8Array {
  const padded = value.replace(/-/g, "+").replace(/_/g, "/");
  const padLength = (4 - (padded.length % 4)) % 4;
  const binary = atob(padded + "=".repeat(padLength));
  const bytes = new Uint8Array(binary.length);
  for (let i = 0; i < binary.length; i++) {
    bytes[i] = binary.charCodeAt(i);
  }
  return bytes;
}

function isCborTag(value: unknown): value is { tag: number; value: unknown } {
  return (
    typeof value === "object" &&
    value !== null &&
    "tag" in value &&
    "value" in value &&
    typeof (value as { tag: unknown }).tag === "number"
  );
}

/** An ISO mdoc is base64url CBOR with an issuer signature, not a JWT. */
export function isMsoMdoc(value: unknown): boolean {
  if (typeof value !== "string" || value.length === 0 || value.includes(".") || value.startsWith("{") || value.startsWith("[")) {
    return false;
  }
  try {
    const root = decode(base64UrlToBytes(value)) as Record<string, unknown>;
    return Boolean(root && (root.issuerAuth || root.issuerSigned || root.documents));
  } catch {
    return false;
  }
}

function renderMdocValue(value: unknown): unknown {
  if (value == null || typeof value === "string" || typeof value === "number" || typeof value === "boolean") {
    return value;
  }
  if (value instanceof Uint8Array) {
    if (value.length > 64) {
      return `image (${value.length} bytes)`;
    }
    return uint8ArrayToHex(value);
  }
  if (isCborTag(value)) {
    return renderMdocValue(value.value);
  }
  if (Array.isArray(value)) {
    return value.map((entry) => renderMdocValue(entry));
  }
  if (value instanceof Date) {
    return value.toISOString();
  }
  if (typeof value === "object") {
    return Object.fromEntries(
      Object.entries(value as Record<string, unknown>).map(([key, entry]) => [key, renderMdocValue(entry)])
    );
  }
  return String(value);
}

function unwrapMdocItem(item: unknown): Record<string, unknown> | null {
  let current = item;
  for (let attempt = 0; attempt < 3; attempt++) {
    if (current instanceof Uint8Array) {
      current = decode(current);
      continue;
    }
    if (isCborTag(current) && current.tag === 24) {
      current = current.value instanceof Uint8Array ? decode(current.value) : current.value;
      continue;
    }
    break;
  }
  if (current && typeof current === "object" && "elementIdentifier" in (current as object)) {
    return current as Record<string, unknown>;
  }
  return null;
}

function mdocNamespaces(root: Record<string, any>): Record<string, unknown> | null {
  if (root.nameSpaces) return root.nameSpaces;
  if (root.issuerSigned?.nameSpaces) return root.issuerSigned.nameSpaces;
  const document = Array.isArray(root.documents) ? root.documents[0] : undefined;
  if (document?.issuerSigned?.nameSpaces) return document.issuerSigned.nameSpaces;
  return null;
}

function mdocType(root: Record<string, any>): string | undefined {
  const direct = root.docType ?? root.issuerSigned?.docType ?? root.documents?.[0]?.docType;
  if (typeof direct === "string") return direct;
  try {
    const issuerAuth = root.issuerAuth ?? root.issuerSigned?.issuerAuth ?? root.documents?.[0]?.issuerSigned?.issuerAuth;
    const payload = issuerAuth?.[2];
    if (!(payload instanceof Uint8Array)) return undefined;
    let mobileSecurityObject = decode(payload) as any;
    if (isCborTag(mobileSecurityObject) && mobileSecurityObject.tag === 24 && mobileSecurityObject.value instanceof Uint8Array) {
      mobileSecurityObject = decode(mobileSecurityObject.value);
    }
    return typeof mobileSecurityObject?.docType === "string" ? mobileSecurityObject.docType : undefined;
  } catch {
    return undefined;
  }
}

/** Flattens issuer-signed mdoc data elements for the result screen. */
export function extractMdocDisplayClaims(mdoc: string): Record<string, unknown> {
  const root = decode(base64UrlToBytes(mdoc)) as Record<string, any>;
  const claims: Record<string, unknown> = {};
  const docType = mdocType(root);
  if (docType) {
    claims.docType = docType;
  }
  const namespaces = mdocNamespaces(root) ?? {};
  const claimNamespaces: Record<string, string> = {};
  Object.entries(namespaces).forEach(([namespace, items]) => {
    if (!Array.isArray(items)) {
      throw new Error("Invalid mdoc namespace: expected an item array");
    }
    items.forEach((item) => {
      const decoded = unwrapMdocItem(item);
      if (!decoded || typeof decoded.elementIdentifier !== "string" || !("elementValue" in decoded)) {
        throw new Error("Invalid mdoc issuer-signed item");
      }
      assignMdocClaim(claims, claimNamespaces, namespace, decoded.elementIdentifier, renderMdocValue(decoded.elementValue));
    });
  });
  return claims;
}

function assignMdocClaim(
  claims: Record<string, unknown>,
  claimNamespaces: Record<string, string>,
  namespace: string,
  identifier: string,
  value: unknown,
) {
  if (identifier === "docType") {
    claims[`${namespace}.${identifier}`] = value;
    return;
  }
  if (Object.prototype.hasOwnProperty.call(claims, identifier)) {
    const previousNamespace = claimNamespaces[identifier];
    if (previousNamespace) {
      claims[`${previousNamespace}.${identifier}`] = claims[identifier];
      delete claims[identifier];
      delete claimNamespaces[identifier];
    }
    claims[`${namespace}.${identifier}`] = value;
    return;
  }
  claims[identifier] = value;
  claimNamespaces[identifier] = namespace;
}
