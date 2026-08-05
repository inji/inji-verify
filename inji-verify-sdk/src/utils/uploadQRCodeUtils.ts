import {scanResult} from "../components/qrcode-verification/QRCodeVerification.types";
import {
    OvpQrHeader,
    SupportedFileTypes,
    UploadFileSizeLimits,
} from "./constants";
import {readBarcodes} from "zxing-wasm/full";
import * as pdfjsLib from "pdfjs-dist";
import workerCode from "pdfjs-dist/build/pdf.worker.mjs";

const blob = new Blob([workerCode], {type: "application/javascript"});
const workerBlobUrl = URL.createObjectURL(blob);

pdfjsLib.GlobalWorkerOptions.workerSrc = workerBlobUrl;

const createDecodeFailedError = () => {
    const error = new Error(
        "QR size too small/low quality, please retry with a clear QR",
    );
    error.name = "QR_DECODE_FAILED";
    return error;
};

const createQrNotFoundError = (message = "No QR code found") => {
    const error = new Error(message);
    error.name = "QR_NOT_FOUND";
    return error;
};

const containsPotentialQrCode = async (file: File): Promise<boolean> => {
    const imageUrl = URL.createObjectURL(file);

    try {
        const image = await new Promise<HTMLImageElement>((resolve, reject) => {
            const loadedImage = new Image();
            loadedImage.onload = () => resolve(loadedImage);
            loadedImage.onerror = () => reject(new Error("Unable to inspect the uploaded image"));
            loadedImage.src = imageUrl;
        });
        const maximumDimension = 320;
        const scale = Math.min(1, maximumDimension / Math.max(image.width, image.height));
        const width = Math.max(1, Math.round(image.width * scale));
        const height = Math.max(1, Math.round(image.height * scale));
        const canvas = document.createElement("canvas");
        const context = canvas.getContext("2d", {willReadFrequently: true});

        if (!context) return false;

        canvas.width = width;
        canvas.height = height;
        context.drawImage(image, 0, 0, width, height);
        const pixels = context.getImageData(0, 0, width, height).data;
        const luminance = (x: number, y: number) => {
            const offset = (y * width + x) * 4;
            return pixels[offset] * 0.299 + pixels[offset + 1] * 0.587 + pixels[offset + 2] * 0.114;
        };

        for (let squareSize = 20; squareSize <= Math.min(width, height); squareSize += 8) {
            const step = Math.max(4, Math.floor(squareSize / 4));

            for (let y = 0; y <= height - squareSize; y += step) {
                for (let x = 0; x <= width - squareSize; x += step) {
                    let transitions = 0;
                    let comparisons = 0;

                    for (let row = y + 1; row < y + squareSize; row++) {
                        for (let column = x + 1; column < x + squareSize; column++) {
                            const current = luminance(column, row);
                            if (Math.abs(current - luminance(column - 1, row)) > 25) transitions++;
                            if (Math.abs(current - luminance(column, row - 1)) > 25) transitions++;
                            comparisons += 2;
                        }
                    }

                    if (transitions / comparisons >= 0.45) return true;
                }
            }
        }

        return false;
    } catch {
        return false;
    } finally {
        URL.revokeObjectURL(imageUrl);
    }
};

export const extractRedirectUrlFromQrData = (qrData: string) => {
    // qr data format = OVP://payload:text-content
    const regex = new RegExp(`^${OvpQrHeader}(.*)$`);
    const match = qrData.match(regex);
    return match ? match[1] : null;
};

export const readQRcodeFromImageFile = async (
    file: File,
    format: string,
    isPDF?: boolean
): Promise<string | undefined> => {
    const arrayBuffer = await file.arrayBuffer();
    const results = await readBarcodes(arrayBuffer, {
        formats: [format as "QRCode"],
        returnErrors: true,
        tryHarder: true,
        tryRotate: true,
        tryInvert: true,
        tryDownscale: false,
        tryDenoise: true,
    });

    const decodedQrCode = results.find((result) => result.isValid);
    if (decodedQrCode) {
        return decodedQrCode.text;
    }

    if (results.some((result) => result.format === "QRCode" && !result.isValid)) {
        throw createDecodeFailedError();
    }

    if (!isPDF && await containsPotentialQrCode(file)) {
        throw createDecodeFailedError();
    }

    if (!isPDF) {
        throw createQrNotFoundError();
    }
};

const readQRcodeFromPdf = async (file: File, format: string) => {
    const pdfData = await file.arrayBuffer();
    const pdf = await pdfjsLib.getDocument({data: pdfData}).promise;
    let decodeFailure: Error | undefined;

    for (let i = 1; i <= pdf.numPages; i++) {
        const page = await pdf.getPage(i);
        for (const scale of [2.0, 2.5, 3.0]) {
            const viewport = page.getViewport({scale});
            const canvas = document.createElement("canvas");
            const context = canvas.getContext("2d");
            if (!context) {
                throw new Error("Failed to get canvas 2D context");
            }
            canvas.height = viewport.height;
            canvas.width = viewport.width;
            const renderContext = {
                canvasContext: context,
                viewport: viewport,
            };
            await page.render(renderContext).promise;
            const dataURL = canvas.toDataURL();
            const blob = await (await fetch(dataURL)).blob();
            const fileFromBlob = new File([blob], "tempFileName", {type: blob.type});
            try {
                const qrCode = await readQRcodeFromImageFile(fileFromBlob, format, true);
                if (qrCode) {
                    return qrCode;
                }
            } catch (error) {
                if (error instanceof Error && error.name === "QR_DECODE_FAILED") {
                    decodeFailure ??= error;
                    continue;
                }
                throw error;
            }
        }
    }
    if (decodeFailure) {
        throw decodeFailure;
    }

    throw createQrNotFoundError(`No ${format} found`);

};

export const scanFilesForQr = async (
    selectedFile: File
): Promise<scanResult> => {
    const scanResult: scanResult = {data: null, error: null};
    const format: string = "QRCode";

    try {
        const fileType: string = selectedFile.type;

        if (fileType === "application/pdf") {
            scanResult.data = await readQRcodeFromPdf(selectedFile, format);
        } else {
            scanResult.data =
                (await readQRcodeFromImageFile(selectedFile, format)) ?? null;
        }
    } catch (error) {
        scanResult.error =
            error instanceof Error
                ? error
                : new Error("Unknown error");
    }

    return scanResult;
};

const getFileExtension = (fileName: string) =>
    fileName.slice(((fileName.lastIndexOf(".") - 1) >>> 0) + 2);

export const doFileChecks = (file: File | null): boolean => {
    if (!file) return false;
    let alert: string | null = null;

    // file format check
    const fileExtension = getFileExtension(file.name).toLowerCase();
    if (!SupportedFileTypes.includes(fileExtension)) {
        alert =
            "Unsupported file format. Allowed file formats are: png, jpeg, jpg, pdf.";
    }

    // file size check
    if (
        file.size < UploadFileSizeLimits.min ||
        file.size > UploadFileSizeLimits.max
    ) {
        alert =
            "File size not supported. The file size should be between 10 KB and 5 MB.";
    }

    if (alert) {
        throw new Error(alert);
    }
    return true;
};
