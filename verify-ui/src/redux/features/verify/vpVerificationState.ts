import { createSlice } from "@reduxjs/toolkit";
import { getVerifiableClaims, VerificationSteps } from "../../../utils/config";
import { VCShareType, VerifyState, claim } from "../../../types/data-types";
import {calculateUnverifiedClaims, calculateVerifiedClaims, getCredentialType} from "../../../utils/commonUtils";

export const OVP_SESSION_SELECTED_CLAIMS_KEY = "ovp_selectedClaims";

const restoreClaimsFromSession = (): claim[] => {
  try {
    const saved = sessionStorage.getItem(OVP_SESSION_SELECTED_CLAIMS_KEY);
    if (saved) {
      const parsed = JSON.parse(saved);
      if (Array.isArray(parsed) && parsed.length > 0) return parsed;
    }
  } catch {
    // ignore
  }
  return getVerifiableClaims()?.filter((claim) => claim.essential) ?? [];
};

const PreloadedState: VerifyState = {
  isLoading: false,
  flowType: "crossDevice",
  method: "VERIFY",
  activeScreen: VerificationSteps["VERIFY"].InitiateVpRequest,
  SelectionPanel: false,
  verificationSubmissionResult: [],
  selectedClaims: restoreClaimsFromSession(),
  originalSelectedClaims: restoreClaimsFromSession(),
  unVerifiedClaims: [],
  sharingType: VCShareType.SINGLE,
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

const vpVerificationState = createSlice({
  name: "vpVerification",
  initialState: PreloadedState,
  reducers: {
    setSelectCredential: (state) => {
      state.activeScreen = VerificationSteps[state.method].SelectCredential;
      state.selectedClaims = getVerifiableClaims().filter((claim) => claim.essential );
      state.originalSelectedClaims = [...state.selectedClaims];
      state.sharingType = state.selectedClaims.length > 1 ? VCShareType.MULTIPLE : VCShareType.SINGLE;
      const inputDescriptors = state.selectedClaims.flatMap((claim) => claim.definition.input_descriptors);
      state.presentationDefinition.input_descriptors = [...inputDescriptors];
      state.SelectionPanel = true;
      state.SelectWalletPanel = false;
      state.verificationSubmissionResult = [];
      state.unVerifiedClaims = [];
      state.isShowResult = false;
    },
    setSelectedClaims: (state, actions) => {
      state.selectedClaims = [...actions.payload.selectedClaims];
      state.sharingType = state.selectedClaims.length > 1 ? VCShareType.MULTIPLE : VCShareType.SINGLE;
      const inputDescriptors = state.selectedClaims.flatMap((claim) => claim.definition.input_descriptors);
      state.presentationDefinition.input_descriptors = [...inputDescriptors];
      state.verificationSubmissionResult = [];
      state.originalSelectedClaims = [...state.selectedClaims];
    },
    setFlowType: (state) => {
      state.SelectWalletPanel = false;
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
      state.selectedClaims = [...state.unVerifiedClaims];
      state.sharingType = state.selectedClaims.length > 1 ? VCShareType.MULTIPLE : VCShareType.SINGLE;
      const inputDescriptors = state.selectedClaims.flatMap((claim) => claim.definition.input_descriptors);
      state.presentationDefinition.input_descriptors = [...inputDescriptors];
      state.isShowResult = false;

      // If initial flow was web wallet (sameDevice), show wallet selector directly
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
    getVpRequest: (state, actions) => {
      if (state.isPartiallyShared && state.unVerifiedClaims.length > 0) {
        state.selectedClaims = [...state.unVerifiedClaims];
      } else {
        state.selectedClaims = [...actions.payload.selectedClaims];
        state.originalSelectedClaims = [...actions.payload.selectedClaims];
      }
      const inputDescriptors = state.selectedClaims.flatMap((claim) => claim.definition.input_descriptors);
      state.presentationDefinition.input_descriptors = [...inputDescriptors];
      state.SelectionPanel = false;
      state.SelectWalletPanel = false;
      state.isShowResult = false;
      state.activeScreen = VerificationSteps[state.method].ScanQrCode;
      state.unVerifiedClaims = [];
    },
    verificationSubmissionComplete: (state, action) => {
      const newlyVerified = calculateVerifiedClaims([...state.selectedClaims], action.payload.verificationResult);

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
      state.unVerifiedClaims = calculateUnverifiedClaims([...state.originalSelectedClaims], state.verificationSubmissionResult);
      state.isPartiallyShared = state.unVerifiedClaims.length > 0;
      state.activeScreen = state.isPartiallyShared
        ? VerificationSteps[state.method].RequestMissingCredential
        : VerificationSteps[state.method].DisplayResult;
      state.flowType = state.isPartiallyShared && state.flowType === "sameDevice" ? "sameDevice" : "crossDevice";
    },
    resetVpRequest: (state) => {
      const prevSdkKey = state.sdkInstanceKey;
      sessionStorage.removeItem(OVP_SESSION_SELECTED_CLAIMS_KEY);
      Object.assign(state, PreloadedState);
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
  setSelectedClaims,
  setShowWalletSelector,
  setSelectedWallet,
} = vpVerificationState.actions;

export default vpVerificationState.reducer;
