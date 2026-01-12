import { decode, decodeBinary } from "@injistack/pixelpass";
import {
  HEADER_DELIMITER,
  SUPPORTED_QR_HEADERS,
  ZIP_HEADER,
  OvpQrHeader,
} from "./constants";

export const decodeQrData = async (qrData: any) => {
  try {
    if (!qrData) throw new Error("No QR data provided");
    let encodedData = qrData;

    if (HEADER_DELIMITER) {
      const splitQrData = qrData.split(HEADER_DELIMITER);
      const header = splitQrData[0];

      if (!SUPPORTED_QR_HEADERS.includes(header)) {
        throw new Error("Unsupported QR header");
      }
      if (splitQrData.length !== 2) {
        throw new Error("Invalid QR format");
      }
      encodedData = splitQrData[1];
    }

    const decodedData = new TextDecoder("utf-8").decode(encodedData);
    if (decodedData.startsWith(ZIP_HEADER)) {
      return await decodeBinary(encodedData);
    }
    return decode(decodedData);
  } catch (error) {
    throw error instanceof Error
      ? new Error(error.message)
      : new Error(String(error));
  }
};

export const extractRedirectUrlFromQrData = (qrData: string): string | null => {
  // qr data format = OVP://payload:text-content
  if (!qrData.startsWith(OvpQrHeader)) return null;
  return qrData.substring(OvpQrHeader.length);
};
