export type VerificationStatus = "SUCCESS" | "INVALID" | "EXPIRED" | "REVOKED";
export type OverallVPStatus = "SUCCESS" | "INVALID";
export interface VerificationResult {
    /**

     Verified credential data (structured per implementation).
     */
    vc: Record<string, unknown>;

    /**

     Full verification result, including per-check outcomes and optional claims.
     */
    verificationResponse: CredentialResult | VpSummarisedVerificationResponse;
}

export type VerificationResults = VerificationResult[];

export interface VpSummarisedVerificationResponse {
    vcResults: {
        vc: Record<string, unknown>;
        vcStatus: VerificationStatus;
    }[];
    vpResultStatus: OverallVPStatus;
}

/**
 * A single claim requested from a credential.
 */
export interface DcqlClaimQuery {
  /** Required if claim_sets is used. Used to reference the claim in claim_sets. */
  id?: string;
  /** Path pointer to navigate the credential structure (JSON pointer segments). */
  path: string[];
  /** Array of allowed values. Claim is returned only if its value matches one of these. */
  values?: unknown[];
}

/**
 * Trusted authority filter for credential issuers.
 */
export interface DcqlTrustedAuthority {
  /** Authority filter type (e.g. aki, etsi_tl, openid_federation; extensible per DCQL). */
  type: string;
  values: string[];
}

/**
 * Format-specific metadata constraints for a credential query.
 */
export interface DcqlCredentialMeta {
  /** SD-JWT VC: allowed credential type identifiers. */
  vct_values?: string[];
  /** W3C VC (JSON-LD): expanded type values. */
  type_values?: string[][];
}

/**
 * A single credential query describing what the Verifier is requesting.
 */
export interface DcqlCredentialQuery {
  /** Unique identifier for this credential within the request and response. */
  id: string;
  /** Credential format (e.g., "dc+sd-jwt", "vc+sd-jwt"). */
  format: string;
  /** Whether multiple credentials of this type can be returned. Defaults to false. */
  multiple?: boolean;
  /** Format-specific constraints (required, can be empty). */
  meta: DcqlCredentialMeta;
  /** Trusted issuer authorities filter. */
  trusted_authorities?: DcqlTrustedAuthority[];
  /** Whether proof of possession is required. Defaults to true. */
  require_cryptographic_holder_binding?: boolean;
  /** Individual data points requested from the credential. */
  claims?: DcqlClaimQuery[];
  /**
   * Acceptable combinations of claims (arrays of claim ids).
   * Each inner array represents one valid combination.
   * Wallet evaluates in order and returns the first satisfiable set.
   */
  claim_sets?: string[][];
}

/**
 * Credential set query defining logical combinations of requested credentials.
 */
export interface DcqlCredentialSetQuery {
  /**
   * Array of arrays of credential ids.
   * Each inner array = one valid combination (AND within, OR across).
   */
  options: string[][];
  /** Whether this set is required. Defaults to true. */
  required?: boolean;
}

/**
 * Top-level DCQL (Digital Credentials Query Language) query object.
 * Used by a Verifier to request specific credentials from a Wallet.
 */
export interface DcqlQuery {
  /** List of credential queries describing what is being requested. */
  credentials: DcqlCredentialQuery[];
  /**
   * Rules about acceptable credential combinations.
   * If omitted, all credentials in the `credentials` array are required.
   */
  credential_sets?: DcqlCredentialSetQuery[];
}

export interface VPRequestBody {
  clientId: string;
  nonce: string;
  transactionId?: string;
  dcqlQuery: DcqlQuery;
  /**
   * When true, the verifier backend will generate a short-lived single-use `response_code`
   * and return it via redirect for same-device web-wallet flows.
   *
   * Must be omitted/false for cross-device and same-device mobile-wallet (deeplink) flows.
   */
  responseCodeValidationRequired?: boolean;
}

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

export type OpenID4VPVerificationProps = ExclusiveCallbacks & {
  /**
   * DCQL query object sent to the verifier backend for OpenID4VP 1.0.
   * Must contain a `credentials` array describing the requested credentials.
   */
  dcqlQuery: DcqlQuery;

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
    vpVerificationRequest?: VPVerificationRequest;

    /*This attribute will decide the format of the response from SDK*/

    summariseResults?: boolean;
};

export interface SessionState {
  requestId: string;
}

export type AppError = {
  errorMessage: string;
  errorCode?: string;
  transactionId?: string | null;
};
export interface VPVerificationRequest {
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
    statusCheck?: {
        purpose: string;
        valid: boolean;
        error: any;
    }[];
    claims?: Record<string, any>;
}



