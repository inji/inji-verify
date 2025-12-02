import {
  AppError,
  PresentationDefinition,
  VPRequestBody,
} from "../components/openid4vp-verification/OpenID4VPVerification.types";
import {
  vcSubmissionBody
} from "../components/qrcode-verification/QRCodeVerification.types";
import { QrData } from "../types/OVPSchemeQrData";

const generateNonce = (): string => {
  return btoa(Date.now().toString());
};

export const vcVerification = async (credential: unknown, url: string) => {
  let body: string;
  let contentType: string;
  if (typeof credential === "string") {
    body = credential;
    contentType = "application/vc+sd-jwt";
  } else {
    body = JSON.stringify(credential);
    contentType = "application/vc+ld+json";
  }
  const requestOptions = {
    method: "POST",
    headers: {
      "Content-Type": contentType,
    },
    body: body,
  };

  try {
    const response = await fetch(url + "/vc-verification", requestOptions);
    const data = await response.json();
    if (response.status !== 200) throw new Error(`Failed VC Verification due to: ${ data.error || "Unknown Error" }`);
    return data.verificationStatus;
  } catch (error) {
    console.error(error);
    if (error instanceof Error) {
      throw Error(error.message);
    } else {
      throw new Error("An unknown error occurred");
    }
  }
};

export const vcSubmission = async (
  credential: unknown,
  url: string,
  txnId?: string
) => {
  const requestBody: vcSubmissionBody = {
    vc: JSON.stringify(credential),
  };
  if (txnId) requestBody.transactionId = txnId;
  const requestOptions = {
    method: "POST",
    headers: {
      "Content-Type": "application/json",
    },
    body: JSON.stringify(requestBody),
  };

  try {
    const response = await fetch(url + "/vc-submission", requestOptions);
    const data = await response.json();
    if (response.status !== 200) throw new Error(`Failed to Submit VC due to: ${ data.error || "Unknown Error" }`);
    return data.transactionId;
  } catch (error) {
    console.error(error);
    if (error instanceof Error) {
      throw Error(error.message);
    } else {
      throw new Error("An unknown error occurred");
    }
  }
};

export const vpRequest = async (
  url: string,
  clientId: string,
  txnId?: string,
  presentationDefinitionId?: string,
  presentationDefinition?: PresentationDefinition
) => {
  const requestBody: VPRequestBody = {
    clientId: clientId,
    nonce: generateNonce(),
  };

  if (txnId) requestBody.transactionId = txnId;
  if (presentationDefinitionId)
    requestBody.presentationDefinitionId = presentationDefinitionId;
  if (presentationDefinition)
    requestBody.presentationDefinition = presentationDefinition;

  const requestOptions = {
    method: "POST",
    headers: {
      "Content-Type": "application/json",
    },
    body: JSON.stringify(requestBody),
  };

  try {
    const response = await fetch(url + "/vp-request", requestOptions);
    if (response.status !== 201) throw new Error("Failed to create VP request");
    const data: QrData = await response.json();
    return data;
  } catch (error) {
    console.error(error);
    if (error instanceof Error) {
      throw Error(error.message);
    } else {
      throw new Error("An unknown error occurred");
    }
  }
};

export const vpRequestStatus = async (url: string, reqId: string) => {
  try {
    const response = await fetch(url + `/vp-request/${reqId}/status`);
    if (response.status !== 200) throw new Error("Failed to fetch status");
    const data = await response.json();
    return data;
  } catch (error) {
    console.error(error);
    if (error instanceof Error) {
      throw Error(error.message);
    } else {
      throw new Error("An unknown error occurred");
    }
  }
};

const isAppError = (error: unknown): error is AppError => (
  typeof error === 'object' &&
  error !== null &&
  'errorMessage' in error &&
  typeof (error as Record<string, unknown>).errorMessage === 'string'
);

export const vpResult = async (url: string, txnId: string) => {
  try {
    const response = await fetch(url + `/vp-result/${txnId}`);
    const data = await response.json();
    if (response.status !== 200) {
      throw {
        errorCode: data.errorCode,
        errorMessage: data.errorMessage,
        transactionId: txnId ?? null
      } as AppError;
    }
    return data.vcResults;
  } catch (error) {
    if (isAppError(error)) {
      throw error as AppError;
    } else {
      throw error;
    }
  }
};