import { createSlice } from "@reduxjs/toolkit";
import { getVerifiableClaims, VerificationSteps } from "../../../utils/config";
import { DcqlQuery, VCShareType, VerifyState, claim } from "../../../types/data-types";
import { calculateUnverifiedClaims } from "../../../utils/commonUtils";

export const OVP_SESSION_SELECTED_CREDENTIALS_KEY = "ovp_selectedCredentials";

const DEFAULT_CREDENTIALS = (): claim[] =>
  getVerifiableClaims()?.filter((c) => c.essential) ?? [];

const mergeDcqlFromCredentials = (credentials: claim[]): DcqlQuery => {
  const credentialSets = credentials.flatMap(
    (c) => c.dcqlQuery?.credential_sets ?? []
  );
  return {
    credentials: credentials.flatMap((c) => c.dcqlQuery?.credentials ?? []),
    ...(credentialSets.length > 0 ? { credential_sets: credentialSets } : {}),
  };
};

const hasValidCredentialStructure = (item: unknown): item is claim => {
  if (!item || typeof item !== "object") return false;
  const c = item as Record<string, unknown>;
  const dcqlQuery = c.dcqlQuery;
  if (!dcqlQuery || typeof dcqlQuery !== "object") return false;
  const credentials = (dcqlQuery as DcqlQuery).credentials;
  if (
    !Array.isArray(credentials) ||
    credentials.length === 0 ||
    !credentials.every((credential) => {
      return (
        !!credential &&
        typeof credential === "object" &&
        typeof credential.id === "string" &&
        typeof credential.format === "string" &&
        !!credential.meta &&
        typeof credential.meta === "object"
      );
    })
  ) {
    return false;
  }
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
    dcqlQuery: mergeDcqlFromCredentials(initialCredentials),
    sdkInstanceKey: 0,
    SelectWalletPanel: false,
    selectedWalletId: undefined,
    selectedWalletBaseUrl: undefined,
  };
};

const PreloadedState: VerifyState = createPreloadedState();

const updateAggregatedDcqlQuery = (state: VerifyState) => {
  state.dcqlQuery = mergeDcqlFromCredentials(state.selectedCredentials);
};

const vpVerificationState = createSlice({
  name: "vpVerification",
  initialState: PreloadedState,
  reducers: {
    setSelectCredential: (state) => {
      state.activeScreen = VerificationSteps[state.method].SelectCredential;
      state.selectedCredentials = getVerifiableClaims().filter((c) => c.essential);
      state.originalSelectedCredentials = [...state.selectedCredentials];
      state.sharingType = state.selectedCredentials.length > 1 ? VCShareType.MULTIPLE : VCShareType.SINGLE;
      updateAggregatedDcqlQuery(state);
      state.SelectionPanel = true;
      state.SelectWalletPanel = false;
      state.verificationSubmissionResult = [];
      state.unVerifiedCredentials = [];
      state.isShowResult = false;
    },
    setSelectedCredentials: (state, action) => {
      state.selectedCredentials = [...action.payload.selectedCredentials];
      state.sharingType = state.selectedCredentials.length > 1 ? VCShareType.MULTIPLE : VCShareType.SINGLE;
      updateAggregatedDcqlQuery(state);
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
      updateAggregatedDcqlQuery(state);
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
      updateAggregatedDcqlQuery(state);
      state.SelectionPanel = false;
      state.SelectWalletPanel = false;
      state.isShowResult = false;
      state.activeScreen = VerificationSteps[state.method].ScanQrCode;
      state.unVerifiedCredentials = [];
    },
    verificationSubmissionComplete: (state, action) => {
      state.verificationSubmissionResult = [
        ...state.verificationSubmissionResult,
        ...action.payload.verificationResult,
      ];
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
