import { createSlice } from "@reduxjs/toolkit";
import { getVerifiableClaims, VerificationSteps } from "../../../utils/config";
import { VCShareType, VerifyState, claim } from "../../../types/data-types";
import { calculateUnverifiedClaims, calculateVerifiedClaims, getCredentialType } from "../../../utils/commonUtils";

export const OVP_SESSION_SELECTED_CREDENTIALS_KEY = "ovp_selectedCredentials";

const DEFAULT_CREDENTIALS = (): claim[] =>
  getVerifiableClaims()?.filter((c) => c.essential) ?? [];

const hasValidCredentialStructure = (item: unknown): item is claim => {
  if (!item || typeof item !== "object") return false;
  const c = item as Record<string, unknown>;
  const def = c.definition;
  if (!def || typeof def !== "object") return false;
  const descriptors = (def as Record<string, unknown>).input_descriptors;
  if (!Array.isArray(descriptors)) return false;
  const type = c.type;
  return typeof type === "string" && !!type;
};

const restoreCredentialsFromSession = (): claim[] => {
  try {
    const saved = localStorage.getItem(OVP_SESSION_SELECTED_CREDENTIALS_KEY);
    if (!saved) return DEFAULT_CREDENTIALS();
    const parsed: unknown = JSON.parse(saved);
    if (!Array.isArray(parsed) || parsed.length === 0) return DEFAULT_CREDENTIALS();
    const knownTypes = new Set(getVerifiableClaims()?.map((c) => c.type) ?? []);
    const valid = parsed.filter((item) => {
      if (!hasValidCredentialStructure(item)) return false;
      if (knownTypes.size > 0) return knownTypes.has((item as claim).type);
      return true;
    });
    if (valid.length === 0) return DEFAULT_CREDENTIALS();
    return valid as claim[];
  } catch {
    return DEFAULT_CREDENTIALS();
  }
};

const createPreloadedState = (): VerifyState => {
  const initialCredentials = restoreCredentialsFromSession();

  return {
    isLoading: false,
    flowType: "crossDevice",
    method: "VERIFY",
    activeScreen: VerificationSteps["VERIFY"].InitiateVpRequest,
    SelectionPanel: false,
    verificationSubmissionResult: [],
    selectedCredentials: [...initialCredentials],
    originalSelectedCredentials: [...initialCredentials],
    unVerifiedCredentials: [],
    sharingType:
      initialCredentials.length > 1 ? VCShareType.MULTIPLE : VCShareType.SINGLE,
    isPartiallyShared: false,
    isShowResult: false,
    presentationDefinition: {
      id: "c4822b58-7fb4-454e-b827-f8758fe27f9a",
      purpose:
        "Relying party is requesting your digital ID for the purpose of Self-Authentication",
      input_descriptors: [] as any[],
    },
    sdkInstanceKey: 0,
    SelectWalletPanel: false,
    selectedWalletId: undefined,
    selectedWalletBaseUrl: undefined,
  };
};

const PreloadedState: VerifyState = createPreloadedState();

const vpVerificationState = createSlice({
  name: "vpVerification",
  initialState: PreloadedState,
  reducers: {
    setSelectCredential: (state) => {
      state.activeScreen = VerificationSteps[state.method].SelectCredential;
      state.selectedCredentials = getVerifiableClaims().filter((c) => c.essential);
      state.originalSelectedCredentials = [...state.selectedCredentials];
      state.sharingType = state.selectedCredentials.length > 1 ? VCShareType.MULTIPLE : VCShareType.SINGLE;
      const inputDescriptors = state.selectedCredentials.flatMap((c) => c.definition.input_descriptors);
      state.presentationDefinition.input_descriptors = [...inputDescriptors];
      state.SelectionPanel = true;
      state.SelectWalletPanel = false;
      state.verificationSubmissionResult = [];
      state.unVerifiedCredentials = [];
      state.isShowResult = false;
    },
    setSelectedCredentials: (state, action) => {
      state.selectedCredentials = [...action.payload.selectedCredentials];
      state.sharingType = state.selectedCredentials.length > 1 ? VCShareType.MULTIPLE : VCShareType.SINGLE;
      const inputDescriptors = state.selectedCredentials.flatMap((c) => c.definition.input_descriptors);
      state.presentationDefinition.input_descriptors = [...inputDescriptors];
      state.verificationSubmissionResult = [];
      state.originalSelectedCredentials = [...state.selectedCredentials];
    },
    setFlowType: (state) => {
      state.SelectWalletPanel = false;
      state.SelectionPanel = false;
      state.flowType = "sameDevice";
      state.activeScreen = VerificationSteps[state.method].SelectWallet;
    },
    setSelectedWallet: (state, action) => {
      state.selectedWalletId = action.payload.walletId;
      state.selectedWalletBaseUrl = action.payload.walletBaseUrl;
    },
    setShowWalletSelector: (state) => {
      state.SelectionPanel = false;
      state.SelectWalletPanel = true;
      state.flowType = "sameDevice";
      state.activeScreen = VerificationSteps[state.method].SelectWallet;
    },
    showMissingCredentialOptions: (state) => {
      state.selectedCredentials = [...state.unVerifiedCredentials];
      state.sharingType = state.selectedCredentials.length > 1 ? VCShareType.MULTIPLE : VCShareType.SINGLE;
      const inputDescriptors = state.selectedCredentials.flatMap((c) => c.definition.input_descriptors);
      state.presentationDefinition.input_descriptors = [...inputDescriptors];
      state.isShowResult = false;

      if (state.flowType === "sameDevice") {
        state.SelectWalletPanel = true;
        state.SelectionPanel = false;
        state.activeScreen = VerificationSteps[state.method].SelectWallet;
      } else {
        state.SelectWalletPanel = false;
        state.SelectionPanel = true;
        state.activeScreen = VerificationSteps[state.method].SelectCredential;
      }
    },
    getVpRequest: (state, action) => {
      if (state.isPartiallyShared && state.unVerifiedCredentials.length > 0) {
        state.selectedCredentials = [...state.unVerifiedCredentials];
      } else {
        state.selectedCredentials = [...action.payload.selectedCredentials];
        state.originalSelectedCredentials = [...action.payload.selectedCredentials];
      }
      const inputDescriptors = state.selectedCredentials.flatMap((c) => c.definition.input_descriptors);
      state.presentationDefinition.input_descriptors = [...inputDescriptors];
      state.SelectionPanel = false;
      state.SelectWalletPanel = false;
      state.isShowResult = false;
      state.activeScreen = VerificationSteps[state.method].ScanQrCode;
      state.unVerifiedCredentials = [];
    },
    verificationSubmissionComplete: (state, action) => {
      const newlyVerified = calculateVerifiedClaims([...state.selectedCredentials], action.payload.verificationResult);

      const uniqueResult = [
        ...state.verificationSubmissionResult,
        ...newlyVerified.filter(
          (vc) =>
            !state.verificationSubmissionResult.some(
              (existing) => getCredentialType(existing.vc) === getCredentialType(vc.vc))
        ),
      ];
      state.verificationSubmissionResult = uniqueResult;
      state.isShowResult = true;
      state.unVerifiedCredentials = calculateUnverifiedClaims([...state.originalSelectedCredentials], state.verificationSubmissionResult);
      state.isPartiallyShared = state.unVerifiedCredentials.length > 0;
      state.activeScreen = state.isPartiallyShared
        ? VerificationSteps[state.method].RequestMissingCredential
        : VerificationSteps[state.method].DisplayResult;
      state.flowType = state.isPartiallyShared && state.flowType === "sameDevice" ? "sameDevice" : "crossDevice";
    },
    resetVpRequest: (state) => {
      const prevSdkKey = state.sdkInstanceKey;
      try {
        localStorage.removeItem(OVP_SESSION_SELECTED_CREDENTIALS_KEY);
      } catch {
        // Ignore storage errors; state reset must still proceed
      }
      Object.assign(state, createPreloadedState());
      state.sdkInstanceKey = prevSdkKey + 1;
    },
  },
});

export const {
  getVpRequest,
  setSelectCredential,
  showMissingCredentialOptions,
  setFlowType,
  resetVpRequest,
  verificationSubmissionComplete,
  setSelectedCredentials,
  setShowWalletSelector,
  setSelectedWallet,
} = vpVerificationState.actions;

export default vpVerificationState.reducer;
