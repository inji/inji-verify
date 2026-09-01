declare global {
  interface DigitalCredentialRequest {
    protocol: string;
    data: unknown;
  }

  interface DigitalCredentialRequestOptions {
    requests: DigitalCredentialRequest[];
  }

  interface CredentialRequestOptions {
    digital?: DigitalCredentialRequestOptions;
  }

  interface DigitalCredential extends Credential {
    readonly protocol: string;
    readonly data: unknown;
  }

  var DigitalCredential: {
    prototype: DigitalCredential;
    userAgentAllowsProtocol(protocol: string): boolean;
  };

  interface Window {
    DigitalCredential?: typeof DigitalCredential;
  }

  interface CredentialsContainer {
    get(
      options: CredentialRequestOptions & {
        digital: DigitalCredentialRequestOptions;
      }
    ): Promise<DigitalCredential | null>;
  }
}

export {};
