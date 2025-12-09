
export interface QrData {
  transactionId: string;
  requestId: string;
  authorizationDetails?: {
    responseType: string;
    responseMode: string;
    clientId: string;
    presentationDefinition: Record<string, unknown>;
    presentationDefinitionUri?: string;
    responseUri: string;
    nonce: string;
    iat: number;
  };
  expiresAt: number;
  requestUri?: string;
}