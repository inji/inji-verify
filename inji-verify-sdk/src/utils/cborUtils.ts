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
      err instanceof Error ? err.message : String(err)
    );
    return false;
  }
}
