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
    const results = await readBarcodes(arrayBuffer);

    if (results.length === 0) {
        if (!isPDF) {
            const error = new Error(
                "Unable to detect the QR code. It may be too small or low quality. Please retry with a clear QR"
            );
            error.name = "QR_DECODE_FAILED";
            throw error;
        }
    } else {
        return results[0].text;
    }
};

const readQRcodeFromPdf = async (file: File, format: string) => {
    const pdfData = await file.arrayBuffer();
    const pdf = await pdfjsLib.getDocument({data: pdfData}).promise;

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
            const qrCode = await readQRcodeFromImageFile(fileFromBlob, format, true);
            if (qrCode) {
                return qrCode;
            }
        }
    }
    throw new Error(`No ${format} found`);

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