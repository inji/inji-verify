export type VerificationStatus = "SUCCESS" | "INVALID" | "EXPIRED" | "REVOKED";

export interface VerificationResult {
    /**

     Verified credential data (structured per implementation).
     */
    vc: Record<string, unknown>;

    /**

     Full verification result, including per-check outcomes and optional claims.
     */
    verificationResponse: CredentialResult;
}

export type VerificationResults = VerificationResult[];

export interface VPVerificationSummaryVcResult {
  vc: Record<string, unknown>;
  vcStatus: VerificationStatus;
}

export interface VPVerificationSummaryResponse {
  vcResults: VPVerificationSummaryVcResult[];
  vpResultStatus: VerificationStatus;
}

export interface VPRequestBody {
  clientId: string;
  nonce: string;
  transactionId?: string;
  presentationDefinitionId?: string;
  presentationDefinition?: PresentationDefinition;
  acceptVPWithoutHolderProof?: boolean;
  /**
   * When true, the verifier backend will generate a short-lived single-use `response_code`
   * and return it via redirect for same-device web-wallet flows.
   *
   * Must be omitted/false for cross-device and same-device mobile-wallet (deeplink) flows.
   */
  responseCodeValidationRequired?: boolean;
}

type ExclusivePresentationDefinition =
  /**
   * ID of the presentation definition used for verification.
   * Required for some verification flows.
   */
  | { presentationDefinitionId: string; presentationDefinition?: never }
  /**
   * The full presentation definition JSON string.
   * If provided, it will be used instead of fetching from the backend.
   */
  | {
      presentationDefinition?: PresentationDefinition;
      presentationDefinitionId?: never;
    };

type ExclusiveCallbacks =
  /**
   * Callback triggered when the verification presentation (VP) is received.
   * Provides the associated transaction ID.
   */
  | { onVPReceived: (transactionId: string) => void; onVPProcessed?: never }
  /**
   * Callback triggered when the VP is successfully processed.
   * Provides the verification result data.
   */
  | {
      onVPProcessed: (VPResult: VerificationResults) => void;
      onVPReceived?: never;
    };

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

export type OpenID4VPVerificationProps = ExclusivePresentationDefinition &
  ExclusiveCallbacks & {
  /**
   React element that triggers the verification process (e.g., a button).
   If not provided, the component may automatically start the process.
   */
  triggerElement?: React.ReactNode;

  /**
   The backend service URL where the verification request will be sent.
   */
  verifyServiceUrl: string;

  /**
   The client identifier for relaying party.
   */
  clientId: string;

  /**
   The protocol being used for verification (e.g., OpenID4VP).
   */
  protocol?: string;

  /**
   A unique identifier for the transaction.
   */
  transactionId?: string;

  /**
   Indicates whether the same device flow is enabled.
   Defaults to true, allowing verification on the same device.
   */
  isSameDeviceFlowEnabled?: boolean;

  /**
   Styling options for the QR code.
   */
  qrCodeStyles?: {
    size?: number; // Default: 200px
    level?: "L" | "M" | "Q" | "H"; // Default: "L"
    bgColor?: string; // Default: "#ffffff"
    fgColor?: string; // Default: "#000000"
    margin?: number; // Default: 10px
    borderRadius?: number; // Default: 10px
  };

  /**
   * Callback triggered when the QR code expires before verification is completed.
   */
  onQrCodeExpired: () => void;

  /**
   * Callback triggered when an error occurs during the verification process.
   * This is a required field to ensure proper error handling.
   */
  onError: (error: AppError) => void;

    /**
     Indicates whether to accept VP submissions without holder proof.
     When true, allows unsigned VPs (VPs without proof).
     */
    acceptVPWithoutHolderProof?: boolean;

    /**
     The base URL of the wallet.
     */
    webWalletBaseUrl?: string;

    /**
     * Configuration object used to control VP verification behaviour.
     *
     * Allows enabling/disabling specific verification checks such as:
     * - Schema & signature validation
     * - Expiry validation
     * - Status checks (e.g., revocation)
     */
    vpVerificationV2Request?: VPVerificationV2Request;
};

export interface SessionState {
  requestId: string;
}

export type AppError = {
  errorMessage: string;
  errorCode?: string;
  transactionId?: string | null;
};
export interface VPVerificationV2Request {
    skipStatusChecks?: boolean;
    statusCheckFilters?: string[];
    includeClaims?: boolean;
}

export interface VPVerificationV2Response {
    transactionId: string;
    allChecksSuccessful: boolean;
    credentialResults: CredentialResult[];
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



