import { claim, LdpVc, VcStatus} from "../types/data-types";
import { EXCLUDE_KEYS_SD_JWT_VC, getVCRenderOrders } from "./config";
import { getLanguageCodes } from "./i18n";

const isSafeKey = (key: string) =>
  key && key !== "__proto__" && key !== "constructor" && key !== "prototype";

type CredentialValue = string | string[] | undefined;

const getValue = (credentialElement: any, currentLanguage: string): CredentialValue => {
  if (credentialElement === null || credentialElement === undefined) {
    return undefined;
  }

  if (typeof credentialElement === "boolean") {
    return credentialElement ? "true" : "false";
  }

  const languageAliases = getLanguageCodes(currentLanguage);
  const fallbackAliases = getLanguageCodes("en");

  if (Array.isArray(credentialElement)) {
    const languageEntry = credentialElement.find(
      (el) =>
        languageAliases.includes(el?.["@language"]) ||
        languageAliases.includes(el?.language)
    );
    if (languageEntry) {
      return languageEntry["@value"] ?? languageEntry.value;
    }

    const fallbackEntry = credentialElement.find(
      (el) =>
        fallbackAliases.includes(el?.["@language"]) ||
        fallbackAliases.includes(el?.language)
    );
    if (fallbackEntry) {
      return fallbackEntry["@value"] ?? fallbackEntry.value;
    }
  }
  if (typeof credentialElement === "object") {
    if ("value" in credentialElement) {
      return getValue(credentialElement.value, currentLanguage);
    }
    let finalValue: string[] = [];
    for (const key of Object.keys(credentialElement)) {
      if (!isSafeKey(key)) continue;
      const nestedValue = getValue(credentialElement[key], currentLanguage);
      if (nestedValue !== undefined) {
        if (Array.isArray(nestedValue)) {
          finalValue.push(...nestedValue);
        } else {
          finalValue.push(nestedValue);
        }
      }
    }
    return finalValue.length > 0 ? finalValue : undefined;
  }

  return String(credentialElement);
};

function createKeyValueEntry(key: string, rawValue: any, currentLanguage: string) {
  if (rawValue === undefined || rawValue === null) return null;

  const value = getValue(rawValue, currentLanguage);

  if (
    value === undefined ||
    value === null ||
    value === "" ||
    (Array.isArray(value) && value.length === 0)
  ) {
    return null;
  }

  return { key, value };
}

function processFields(order: string[], credential: any, currentLanguage: string): { key: string; value: any }[] {
  return order
    .map((key) => isSafeKey(key) ? createKeyValueEntry(key, credential?.[key], currentLanguage) : null)
    .filter((entry): entry is { key: string; value: any } => entry !== null);
}

function processFarmerLandCredential(credential: any, currentLanguage: string): { key: string; value: any }[] {
  return getVCRenderOrders().farmerLandCredentialRenderOrder
    .flatMap((keyEntry: any) => {
      if (typeof keyEntry === "string" && isSafeKey(keyEntry)) {
        const value = getValue(credential[keyEntry], currentLanguage);
        return value ? { key: keyEntry, value } : null;
      }

      if (typeof keyEntry === "object" && keyEntry !== null) {
        const [farmKey, farmOrder] = Object.entries(keyEntry)[0];
        if (!isSafeKey(farmKey)) return null;
        const farmObj = credential[farmKey];

        if (!farmObj) return null;

        return (farmOrder as string[])
          .map((farmField) => {
            if (!isSafeKey(farmField)) return null;
            const value = getValue(farmObj[farmField], currentLanguage);
            return value ? { key: farmField, value } : null;
          })
          .filter(
            (entry): entry is { key: string; value: any } => entry !== null
          );
      }

      return null;
    })
    .filter((entry: { key: string; value: any }) => entry !== null);
}

export const getDetailsOrder = (vc: any, currentLanguage: string): { key: string; value: any }[] => {
  // Validate and parse VC if it's a string
  let parsedVc = vc;

  if (typeof vc === "string") {
    try {
      parsedVc = JSON.parse(vc);
    } catch (e) {
      console.error("Failed to parse VC string:", e);
      return [];
    }
  }

  // Check if VC is null, undefined, or empty
  if (!parsedVc || (typeof parsedVc === "object" && Object.keys(parsedVc).length === 0)) {
    return [];
  }

  // Ensure parsedVc is a non-null object (not an array)
  if (Array.isArray(parsedVc)) {
    console.error("Invalid VC format: expected an object");
    return [];
  }
  if (typeof parsedVc !== "object" || parsedVc === null) {
    console.error("Invalid VC format: expected an object");
    return [];
  }

  const credential =
    parsedVc?.regularClaims && parsedVc?.disclosedClaims
      ? { ...parsedVc.regularClaims, ...parsedVc.disclosedClaims }
      : parsedVc?.credentialSubject ?? parsedVc;

  const type =
    parsedVc?.regularClaims && parsedVc?.disclosedClaims
      ? "SdJwtVC"
      : parsedVc?.type?.find((t: string) => t !== "VerifiableCredential");

  switch (type) {
    case "InsuranceCredential":
    case "LifeInsuranceCredential":
      return processFields(
        getVCRenderOrders().InsuranceCredentialRenderOrder,
        credential,
        currentLanguage
      );

    case "FarmerCredential":
      return processFields(
        getVCRenderOrders().farmerCredentialRenderOrder,
        credential,
        currentLanguage
      );

    case "MOSIPVerifiableCredential":
    case "MockVerifiableCredential":
      return processFields(
        getVCRenderOrders().MosipVerifiableCredentialRenderOrder,
        credential,
        currentLanguage
      );

    case "IncomeTaxAccountCredential":
      return processFields(
        getVCRenderOrders().IncomeTaxAccountCredentialRenderOrder,
        credential,
        currentLanguage
      );

    case "farmer":
      return processFarmerLandCredential(credential, currentLanguage);

    case "SdJwtVC":
      return Object.keys(credential)
        .filter(
          (key) =>
            key !== "id" &&
            isSafeKey(key) &&
            credential[key] !== null &&
            credential[key] !== undefined &&
            credential[key] !== "" &&
            !EXCLUDE_KEYS_SD_JWT_VC.includes(key.toLowerCase())
        )
        .map((key) => ({
          key,
          value: getValue(credential[key], currentLanguage),
        }));

    default:
      // Filter out unwanted keys and parse nested objects
      return Object.keys(credential)
        .filter(
          (key) =>
            key !== "id" &&
            isSafeKey(key) &&
            credential[key] != null &&
            credential[key] !== undefined &&
            credential[key] !== ""
        )
        .map((key) =>
          createKeyValueEntry(key, credential[key], currentLanguage),
        )
        .filter(
          (entry): entry is { key: string; value: any } => entry !== null,
        );
  }
};

export const calculateVerifiedClaims = (
  selectedClaims: claim[],
  verificationSubmissionResult: { vc: LdpVc | object; vcStatus: VcStatus }[]
) => {
  return verificationSubmissionResult.filter((vc) =>
    selectedClaims.some((claim) => getCredentialType(vc.vc) === claim.type)
  );
};

export const calculateUnverifiedClaims = (
  originalSelectedClaims: claim[],
  verificationSubmissionResult: { vc: LdpVc | object; vcStatus: VcStatus }[]
): claim[] => {
  return originalSelectedClaims.filter((claim) => {
    return !verificationSubmissionResult.some(
      (vcResult) => getCredentialType(vcResult.vc) === claim.type
    );
  });
};

const extractType = (type: any): string | undefined => {
  if (!type) return undefined;
  if (typeof type === "string")
    return type !== "VerifiableCredential" ? type : undefined;
  if (typeof type === "object" && "_value" in type) {
    return type._value !== "VerifiableCredential" ? type._value : undefined;
  }
  return String(type);
};

const findType = (types: any[]): string | undefined =>
  types?.map((type) => extractType(type)).find((t) => t !== undefined);

export const getCredentialType = (credential: any): string => {
  const sdType = credential?.regularClaims?.vct || credential?.regularClaims?.type;

  if (sdType) {
    if (Array.isArray(sdType)) {
      const type = findType(sdType);
      if (type) return type;
    } else {
      const type = extractType(sdType);
      if (type) return type;
    }
  }

  if (Array.isArray(credential?.type)) {
    const type = findType(credential.type);
    if (type) return type;
  }

  return "verifiableCredential";
};

export const getClientId = () => window._env_?.CLIENT_ID;

export const isVPSubmissionSupported = () => {
  const value = window._env_?.VP_SUBMISSION_SUPPORTED;
  return value?.toLowerCase() === "true";
};

export const vcVerificationV2Request = {
    skipStatusChecks: false,
    statusCheckFilters: ["revocation"],
    includeClaims: true
};
export const vpVerificationV2Request = {
    skipStatusChecks: false,
    statusCheckFilters: ["revocation"],
    includeClaims: true
};

