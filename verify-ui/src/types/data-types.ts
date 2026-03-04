export type QrScanResult = {
  data: string | null;
  error: string | null;
  status: QrReadStatus;
};

export type QrReadStatus = "SUCCESS" | "NOT_READ" | "FAILED";

export type VcStatus = "SUCCESS" | "INVALID" | "EXPIRED" | "REVOKED";

export type VerificationStep = {
  label: string;
  description: string;
};
export type AlertSeverity =
  | "success"
  | "info"
  | "warning"
  | "error"
  | undefined;

export type AlertInfo = {
  title?: string;
  errorCode?: string;
  errorReason?: string;
  referenceId?: string;
  message?: string;
  severity?: AlertSeverity;
  open?: boolean;
  autoHideDuration?: number;
};

export type VerificationMethod = "SCAN" | "UPLOAD" | "VERIFY";

export type InternetConnectionStatus =
  | "ONLINE"
  | "OFFLINE"
  | "LOADING"
  | "UNKNOWN";

export type ApplicationState = {
  internetConnectionStatus: InternetConnectionStatus;
};

export type VerificationState = {
  method: VerificationMethod;
  activeScreen: number; // Verification steps
  qrReadResult?: QrReadResult | undefined;
  verificationResult?: VerificationResult;
  alert?: AlertInfo;
  ovp?: OvpFlowData;
};

export type QrReadResult = {
  alert?: AlertInfo;
  qrData?: object;
  status: QrReadStatus;
};

export type OvpFlowData = {
  presentationSubmission?: any;
  vpToken?: any;
};
export type VerificationResult = {
  vc?: AnyVc;
  vcStatus?: VcStatus;
};

export type LanguageObject = {
  label: string;
  value: string;
};

export interface VerificationStepsContentType {
  SCAN: VerificationStep[];
  UPLOAD: VerificationStep[];
  VERIFY: VerificationStep[];
  TO_BE_SELECTED: VerificationStep[];
}
export interface claim {
  name: string;
  type: string;
  logo: string;
  essential?: boolean;
  definition: PresentationDefinition;
  clientIdScheme?: string;
}

interface InputDescriptor {
  id: string;
  format?: {
    ldp_vc: {
      proof_type: string[];
    };
  };
  constraints?: {};
}

export interface PresentationDefinition {
  id?: string;
  purpose: string;
  format?: {
    ldp_vc: {
      proof_type: string[];
    };
  };
  input_descriptors: InputDescriptor[];
}

export type VpSubmissionResultInt = {
  vc: LdpVc | object;
  vcStatus: VcStatus;
  view?: boolean;
};

export type VerifyState = {
  isLoading: boolean;
  flowType: "crossDevice" | "sameDevice";
  method: string;
  activeScreen: number;
  verificationSubmissionResult: VpSubmissionResultInt[];
  SelectionPanel: boolean;
  isShowResult: boolean;
  selectedCredentials: claim[];
  originalSelectedCredentials: claim[];
  unVerifiedCredentials: claim[];
  sharingType: VCShareType;
  isPartiallyShared: boolean;
  presentationDefinition: PresentationDefinition;
  sdkInstanceKey: number;
  SelectWalletPanel: boolean;
  selectedWalletId?: string;
  selectedWalletBaseUrl?: string;
};

export enum VCShareType {
  SINGLE = "single",
  MULTIPLE = "multiple",
}

export type QrCodeProps = {
  title: string;
  data: string;
  size: number;
  footer?: string;
  status: "SUCCESS" | "EXPIRED" | "INVALID";
};

export type RenderMethod = {
  renderSuite: string;
  template: {
    id: string;
    mediaType: string;
  };
  type: string;
};

export type LdpVc = {
  "@context": string[];
  credentialSubject: credentialSubject;
  expirationDate: string;
  id: string;
  issuanceDate: string;
  issuer: string;
  proof: {
    proofValue: string;
    created: string;
    proofPurpose: string;
    type: string;
    verificationMethod: string;
  };
  type: string[];
  renderMethod?: RenderMethod[];
};

export type SdJwtVc = {
  regularClaims: Record<string, any>;
  disclosedClaims: Record<string, any>;
};

export type AnyVc = LdpVc | SdJwtVc;
export type credentialSubject = {
  benefits: string[];
  gender: string;
  policyName: string;
  dob: string;
  mobile: string;
  policyNumber: string;
  fullName: string;
  policyIssuedOn: string;
  id: string;
  email: string;
  policyExpiresOn: string;
};

export type WebWallet = {
  id: string;
  name: string;
  iconUrl: string;
  walletBaseUrl: string;
};
export interface ValidationCheck {
    purpose?: string;
    valid: boolean;
    error?: {
        errorCode?: string;
        errorMessage?: string;
    } | null;
}

export interface VCVerificationV2Response {
    allChecksSuccessful: boolean;
    schemaAndSignatureCheck: ValidationCheck;
    expiryCheck: ValidationCheck;
    statusCheck: ValidationCheck[];
    claims?: Record<string, any>;
}

export interface CredentialResult {
    verifiableCredential: string | object;
    allChecksSuccessful: boolean;
    holderProofCheck?: {
        valid: boolean;
        error: any;
    } | null;
    schemaAndSignatureCheck?: {
        valid: boolean;
        error: any;
    };
    expiryCheck?: {
        valid: boolean;
    };
    statusChecks?: {
        purpose: string;
        valid: boolean;
        error: any;
    }[];
    claims?: Record<string, any>;
}
