import {
  AppError,
  PresentationDefinition,
  VPRequestBody,
  VPVerificationV2Request,
} from "../components/openid4vp-verification/OpenID4VPVerification.types";
import {
  vcSubmissionBody,
  VCVerificationV2Request,
  VCVerificationV2Response
} from "../components/qrcode-verification/QRCodeVerification.types";
import { QrData } from "../types/OVPSchemeQrData";
import { isCWT } from "./cborUtils";

const generateNonce = (): string => {
  return btoa(Date.now().toString());
};

export const vcVerificationV2 = async (credential: unknown, url: string, config?: VCVerificationV2Request): Promise<VCVerificationV2Response> => {
    const vcString = isCWT(credential)
        ? (credential as string)
        : typeof credential === "string" ? credential : JSON.stringify(credential);

    const requestBody = {
        verifiableCredential: vcString,
        skipStatusChecks: config?.skipStatusChecks ?? false,
        statusCheckFilters: config?.statusCheckFilters ?? [],
        includeClaims: config?.includeClaims ?? false,
    };

    try {
        const response = await fetch(`${url}/v2/vc-verification`, {
            method: "POST",
            headers: { "Content-Type": "application/json" },
            body: JSON.stringify(requestBody),
        });
        const data = await response.json();

        if (!response.ok) {
            throw new Error(data?.message || data?.error || `Verification failed with status ${response.status}`);
        }
        if (!data) {
            throw new Error("Verification response was empty or invalid JSON");
        }
        return data as VCVerificationV2Response;
    } catch (error) {
        console.error("V2 Verification Error:", error);
        throw error instanceof Error ? error : new Error("An unknown error occurred during verification");
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
  presentationDefinition?: PresentationDefinition,
  acceptVPWithoutHolderProof?: boolean,
  presentationFlow?: string
) => {
  const requestBody: VPRequestBody = {
    clientId: clientId,
    nonce: generateNonce(),
    acceptVPWithoutHolderProof: acceptVPWithoutHolderProof,
    presentationFlow: presentationFlow,
  };

  if (txnId) requestBody.transactionId = txnId;
  if (presentationDefinitionId)
    requestBody.presentationDefinitionId = presentationDefinitionId;
  if (presentationDefinition)
    requestBody.presentationDefinition = presentationDefinition;

  const requestOptions: RequestInit = {
    method: "POST",
    headers: {
      "Content-Type": "application/json",
    },
    credentials: "include",
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

export const vpRequestStatus = async (url: string, reqId: string, abortSignal = false) => {
  try {
    const response = await fetch(url + `/vp-request/${reqId}/status`, {
      signal: abortSignal ? AbortSignal.timeout(5000) : undefined
    });
    if (response.status !== 200) throw new Error("Failed to fetch status");
    return await response.json();
  } catch (error) {
    console.error(error);
    if (error instanceof Error) {
      if (error.name === "TimeoutError") return error;
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

export const vpResult = async (url: string, transactionId: string, config?: VPVerificationV2Request)  => {
    if (!transactionId) {
        throw new Error("Transaction ID is required for VP verification");
    }
    const requestBody = {
        skipStatusChecks: config?.skipStatusChecks ?? false,
        statusCheckFilters: config?.statusCheckFilters ?? [],
        includeClaims: config?.includeClaims ?? false,
    };

    try {
        const baseUrl = new URL(`${url}/v2/vp-results/${transactionId}`);
        const response = await fetch(baseUrl.toString(), {
            method: "POST",
            headers: {
                "Content-Type": "application/json",
            },
            body: JSON.stringify(requestBody),
        });
        if (!response.ok) {
            const errorData = await response.json().catch(() => ({}));
            throw {
                errorCode: errorData.errorCode,
                errorMessage: errorData.errorMessage || errorData.error || "Unknown error",
                transactionId,
            } as AppError;
        }
      return await response.json();
    } catch (error) {
        if (isAppError(error)) {
            throw error as AppError;
        }
        throw error;
    }
};

export const vpResultUsingResponseCode = async (url: string, responseCode: string, config?: VPVerificationV2Request) => {
  if (!responseCode) {
    throw new Error("Response Code is required for VP verification");
  }
  const requestBody = {
    skipStatusChecks: config?.skipStatusChecks ?? false,
    statusCheckFilters: config?.statusCheckFilters ?? [],
    includeClaims: config?.includeClaims ?? false,
  };

  try {
    const baseUrl = new URL(`${url}/vp-results`);
    baseUrl.searchParams.append("response_code", responseCode);

    const response = await fetch(baseUrl.toString(), {
      method: "POST",
      headers: {
        "Content-Type": "application/json",
      },
      credentials: "include",
      body: JSON.stringify(requestBody),
    });

    if (!response.ok) {
      const errorData = await response.json();
      throw {
        errorCode: errorData.errorCode,
        errorMessage: errorData.errorMessage || errorData.error || "Unknown error",
      } as AppError;
    }
    return await response.json();
  } catch (error) {
    if (isAppError(error)) {
      throw error as AppError;
    }
    throw error;
  }
}