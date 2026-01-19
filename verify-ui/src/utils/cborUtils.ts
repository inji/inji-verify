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
    bytes[i / 2] = parseInt(hex.slice(i, i + 2), 16);
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
