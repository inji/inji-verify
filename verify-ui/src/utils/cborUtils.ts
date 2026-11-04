import { decodeMultiple, decode, Tag } from "cbor-x";
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

const MDOC_KEY_ISSUER_AUTH = "issuerAuth";
const MDOC_KEY_NAMESPACES = "nameSpaces";
const MDOC_KEY_ISSUER_SIGNED = "issuerSigned";
const MDOC_KEY_DOCUMENTS = "documents";
const MDOC_KEY_ELEMENT_IDENTIFIER = "elementIdentifier";
const MDOC_KEY_ELEMENT_VALUE = "elementValue";
const MDOC_IMAGE_ELEMENTS = new Set(["portrait", "signature_usual_mark"]);

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

function bytesToBase64(value: Uint8Array): string {
  let binary = "";
  value.forEach((byte) => {
    binary += String.fromCharCode(byte);
  });
  return btoa(binary);
}

/** Only cbor-x's decoded Tag instances count; plain maps with tag/value keys are claim data. */
function isCborTag(value: unknown): value is Tag {
  return value instanceof Tag;
}

function containsCborTextKey(bytes: Uint8Array, key: string): boolean {
  const keyBytes = Uint8Array.from(key, (character) => character.charCodeAt(0));
  const header = 0x60 | keyBytes.length;
  for (let start = 0; start <= bytes.length - keyBytes.length - 1; start++) {
    if (bytes[start] !== header) continue;
    let offset = 0;
    while (offset < keyBytes.length && bytes[start + offset + 1] === keyBytes[offset]) {
      offset++;
    }
    if (offset === keyBytes.length) return true;
  }
  return false;
}

function containsMdocShapeKey(bytes: Uint8Array): boolean {
  return (
    containsCborTextKey(bytes, MDOC_KEY_DOCUMENTS) ||
    containsCborTextKey(bytes, MDOC_KEY_ISSUER_SIGNED) ||
    (containsCborTextKey(bytes, MDOC_KEY_ISSUER_AUTH) &&
      containsCborTextKey(bytes, MDOC_KEY_NAMESPACES))
  );
}

/** An ISO mdoc is base64url CBOR with an issuer signature, not a JWT. */
export function isMsoMdoc(value: unknown): boolean {
  if (typeof value !== "string" || value.length === 0 || value.includes(".") || value.startsWith("{") || value.startsWith("[")) {
    return false;
  }
  try {
    const root = decode(base64UrlToBytes(value)) as Record<string, unknown>;
    return Boolean(
      root &&
        (root[MDOC_KEY_DOCUMENTS] ||
          root[MDOC_KEY_ISSUER_SIGNED] ||
          (root[MDOC_KEY_ISSUER_AUTH] && root[MDOC_KEY_NAMESPACES])),
    );
  } catch {
    try {
      return containsMdocShapeKey(base64UrlToBytes(value));
    } catch {
      return false;
    }
  }
}

function renderMdocValue(value: unknown, elementIdentifier?: string): unknown {
  if (value == null || typeof value === "string" || typeof value === "number" || typeof value === "boolean") {
    return value;
  }
  if (value instanceof Uint8Array) {
    if (elementIdentifier && MDOC_IMAGE_ELEMENTS.has(elementIdentifier)) {
      return `image (${value.length} bytes)`;
    }
    return bytesToBase64(value);
  }
  if (isCborTag(value)) {
    return renderMdocValue(value.value, elementIdentifier);
  }
  if (Array.isArray(value)) {
    return value.map((entry) => renderMdocValue(entry, elementIdentifier));
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
  if (current && typeof current === "object" && MDOC_KEY_ELEMENT_IDENTIFIER in (current as object)) {
    return current as Record<string, unknown>;
  }
  return null;
}

function mdocNamespaces(root: Record<string, any>): Record<string, unknown> {
  if (Object.prototype.hasOwnProperty.call(root, MDOC_KEY_DOCUMENTS)) {
    if (!Array.isArray(root[MDOC_KEY_DOCUMENTS]) || root[MDOC_KEY_DOCUMENTS].length === 0) {
      throw new Error("mdoc contains no documents");
    }
    const namespaces = root[MDOC_KEY_DOCUMENTS][0]?.[MDOC_KEY_ISSUER_SIGNED]?.[MDOC_KEY_NAMESPACES];
    if (!namespaces) {
      throw new Error("Failed to extract mdoc claims");
    }
    return namespaces;
  }
  if (root[MDOC_KEY_ISSUER_SIGNED]?.[MDOC_KEY_NAMESPACES]) {
    return root[MDOC_KEY_ISSUER_SIGNED][MDOC_KEY_NAMESPACES];
  }
  if (root[MDOC_KEY_ISSUER_AUTH] && root[MDOC_KEY_NAMESPACES]) {
    return root[MDOC_KEY_NAMESPACES];
  }
  throw new Error("Failed to extract mdoc claims");
}

/**
 * Issuer-signed mdoc data elements for the result screen, grouped by namespace.
 * Shape matches the backend: `{ "org.iso.18013.5.1": { family_name: "..." } }`.
 * docType is not included; the UI uses a fixed title.
 */
export function extractMdocDisplayClaims(
  mdoc: string,
): Record<string, Record<string, unknown>> {
  let root: Record<string, any>;
  try {
    root = decode(base64UrlToBytes(mdoc)) as Record<string, any>;
  } catch {
    throw new Error("Failed to decode mdoc credential");
  }
  const namespaces = mdocNamespaces(root);
  const claims: Record<string, Record<string, unknown>> = {};
  Object.entries(namespaces).forEach(([namespace, items]) => {
    if (!Array.isArray(items)) {
      throw new Error("Invalid mdoc namespace: expected an item array");
    }
    const namespaceClaims: Record<string, unknown> = {};
    items.forEach((item) => {
      const decoded = unwrapMdocItem(item);
      if (!decoded || typeof decoded[MDOC_KEY_ELEMENT_IDENTIFIER] !== "string" || !(MDOC_KEY_ELEMENT_VALUE in decoded)) {
        throw new Error("Invalid mdoc issuer-signed item");
      }
      const elementIdentifier = decoded[MDOC_KEY_ELEMENT_IDENTIFIER] as string;
      if (Object.prototype.hasOwnProperty.call(namespaceClaims, elementIdentifier)) {
        throw new Error(`Duplicate mdoc element identifier: ${namespace}.${elementIdentifier}`);
      }
      namespaceClaims[elementIdentifier] = renderMdocValue(
        decoded[MDOC_KEY_ELEMENT_VALUE],
        elementIdentifier,
      );
    });
    claims[namespace] = namespaceClaims;
  });
  return claims;
}
