import {
  calculateVerifiedClaims,
  calculateUnverifiedClaims,
  getClientId,
  getCredentialType,
  getDetailsOrder,
  getDcqlCredentialQueryCount,
  getTotalCredentialCount,
  isVPSubmissionSupported,
} from "../../utils/commonUtils";
import { claim, DcqlCredentialQuery, MatchingVc } from "../../types/data-types";

jest.mock("../../utils/i18n", () => ({
  getLanguageCodes: jest.fn(() => ["en"]),
}));

jest.mock("../../utils/config", () => ({
  EXCLUDE_KEYS_SD_JWT_VC: ["excluded"],
  getVCRenderOrders: jest.fn(() => ({
    InsuranceCredentialRenderOrder: ["name", "active", "nested", "empty"],
    farmerCredentialRenderOrder: ["name"],
    MosipVerifiableCredentialRenderOrder: ["name"],
    IncomeTaxAccountCredentialRenderOrder: ["name"],
    farmerLandCredentialRenderOrder: ["farmerName", { land: ["district", "acres", "empty"] }],
  })),
}));

const buildClaim = (
  name: string,
  type: string,
  credentials: DcqlCredentialQuery[]
): claim => ({
  name,
  type,
  logo: "/assets/cert.png",
  dcqlQuery: { credentials },
});

const ldpVc = (credentialType: string) => ({
  type: ["VerifiableCredential", credentialType],
});

const matchingResult = (
  vc: object,
  vcStatus: MatchingVc["vcStatus"] = "SUCCESS"
): MatchingVc => ({ vc, vcStatus });

describe("commonUtils credential matching", () => {
  describe("getCredentialType", () => {
    test("returns non-VerifiableCredential type from ldp_vc type array", () => {
      expect(getCredentialType(ldpVc("InsuranceCredential"))).toBe(
        "InsuranceCredential"
      );
    });

    test("returns full IRI when wallet submits absolute type identifier", () => {
      const iri =
        "https://inji.github.io/inji-config/contexts/insurance-context.json#InsuranceCredential";
      expect(getCredentialType(ldpVc(iri))).toBe(iri);
    });

    test("returns vct from SD-JWT regularClaims", () => {
      expect(
        getCredentialType({
          regularClaims: { vct: "MockVerifiableCredential_SD_JWT" },
        })
      ).toBe("MockVerifiableCredential_SD_JWT");
    });
  });

  describe("calculateVerifiedClaims", () => {
    test("matches submitted credential against IRI type_values using # separator", () => {
      const selectedClaim = buildClaim("Life Insurance", "InsuranceCredential", [
        {
          id: "life_insurance_credential_id",
          format: "ldp_vc",
          meta: {
            type_values: [
              [
                "https://inji.github.io/inji-config/contexts/insurance-context.json#InsuranceCredential",
              ],
            ],
          },
        },
      ]);

      const result = calculateVerifiedClaims(
        [selectedClaim],
        [matchingResult(ldpVc("InsuranceCredential"))]
      );

      expect(result).toHaveLength(1);
      expect(result[0].vcStatus).toBe("SUCCESS");
    });

    test("matches submitted credential against IRI type_values using / separator", () => {
      const selectedClaim = buildClaim("Life Insurance", "InsuranceCredential", [
        {
          id: "life_insurance_credential_id",
          format: "ldp_vc",
          meta: {
            type_values: [
              ["https://example.org/context/InsuranceCredential"],
            ],
          },
        },
      ]);

      const result = calculateVerifiedClaims(
        [selectedClaim],
        [matchingResult(ldpVc("InsuranceCredential"))]
      );

      expect(result).toHaveLength(1);
    });

    test("matches relative configured type against submitted absolute IRI", () => {
      const selectedClaim = buildClaim("Life Insurance", "InsuranceCredential", [
        {
          id: "life_insurance_credential_id",
          format: "ldp_vc",
          meta: {
            type_values: [["InsuranceCredential"]],
          },
        },
      ]);

      const result = calculateVerifiedClaims(
        [selectedClaim],
        [
          matchingResult(
            ldpVc(
              "https://example.org/context.json#InsuranceCredential"
            )
          ),
        ]
      );

      expect(result).toHaveLength(1);
    });

    test("matches when top-level claim type differs from dcql type_values", () => {
      const selectedClaim = buildClaim("Health Insurance", "HealthCredential", [
        {
          id: "health_insurance_credential_id",
          format: "ldp_vc",
          meta: {
            type_values: [
              [
                "https://inji.github.io/inji-config/contexts/insurance-context.json#InsuranceCredential",
              ],
            ],
          },
        },
      ]);

      const result = calculateVerifiedClaims(
        [selectedClaim],
        [matchingResult(ldpVc("InsuranceCredential"))]
      );

      expect(result).toHaveLength(1);
    });

    test("matches any credential definition within a multi-credential claim", () => {
      const selectedClaim = buildClaim(
        "MOSIP ID + Health Insurance",
        "MOSIPVerifiableCredential",
        [
          {
            id: "mosip_verifiable_credential_id",
            format: "ldp_vc",
            meta: {
              type_values: [
                [
                  "https://inji.github.io/inji-config/contexts/mosip-identity-context.json#MOSIPVerifiableCredential",
                ],
              ],
            },
          },
          {
            id: "health_insurance_credential_id",
            format: "ldp_vc",
            meta: {
              type_values: [
                [
                  "https://inji.github.io/inji-config/contexts/insurance-context.json#InsuranceCredential",
                ],
              ],
            },
          },
        ]
      );

      const mosipResult = calculateVerifiedClaims(
        [selectedClaim],
        [matchingResult(ldpVc("MOSIPVerifiableCredential"))]
      );
      const insuranceResult = calculateVerifiedClaims(
        [selectedClaim],
        [matchingResult(ldpVc("InsuranceCredential"))]
      );

      expect(mosipResult).toHaveLength(1);
      expect(insuranceResult).toHaveLength(1);
    });

    test("matches when any configured type value in a type_values group matches", () => {
      const selectedClaim = buildClaim("MOSIP ID", "MOSIPVerifiableCredential", [
        {
          id: "mosip_verifiable_credential_id",
          format: "ldp_vc",
          meta: {
            type_values: [
              [
                "https://www.w3.org/2018/credentials#VerifiableCredential",
                "MOSIPVerifiableCredential",
              ],
            ],
          },
        },
      ]);

      const result = calculateVerifiedClaims(
        [selectedClaim],
        [matchingResult(ldpVc("MOSIPVerifiableCredential"))]
      );

      expect(result).toHaveLength(1);
    });

    test("matches SD-JWT credentials using vct_values", () => {
      const selectedClaim = buildClaim(
        "Mock Identity (SD JWT)",
        "MockVerifiableCredential_SD_JWT",
        [
          {
            id: "mock_identity_sd_jwt_credential_id",
            format: "vc+sd-jwt",
            meta: {
              vct_values: ["MockVerifiableCredential_SD_JWT"],
            },
          },
        ]
      );

      const result = calculateVerifiedClaims(
        [selectedClaim],
        [
          matchingResult({
            regularClaims: { vct: "MockVerifiableCredential_SD_JWT" },
          }),
        ]
      );

      expect(result).toHaveLength(1);
    });

    test("returns only selected claims that have matching submissions", () => {
      const insuranceClaim = buildClaim("Life Insurance", "InsuranceCredential", [
        {
          id: "life_insurance_credential_id",
          format: "ldp_vc",
          meta: {
            type_values: [
              [
                "https://inji.github.io/inji-config/contexts/insurance-context.json#InsuranceCredential",
              ],
            ],
          },
        },
      ]);
      const mosipClaim = buildClaim("MOSIP ID", "MOSIPVerifiableCredential", [
        {
          id: "mosip_verifiable_credential_id",
          format: "ldp_vc",
          meta: {
            type_values: [
              [
                "https://inji.github.io/inji-config/contexts/mosip-identity-context.json#MOSIPVerifiableCredential",
              ],
            ],
          },
        },
      ]);

      const result = calculateVerifiedClaims(
        [insuranceClaim, mosipClaim],
        [matchingResult(ldpVc("InsuranceCredential"))]
      );

      expect(result).toHaveLength(1);
      expect(getCredentialType(result[0].vc)).toBe("InsuranceCredential");
    });

    test("returns all credentials from service without deduplicating by type", () => {
      const selectedClaim = buildClaim("Life Insurance", "InsuranceCredential", [
        {
          id: "life_insurance_credential_id",
          format: "ldp_vc",
          meta: {
            type_values: [["InsuranceCredential"]],
          },
        },
      ]);

      const submissions = [
        matchingResult(ldpVc("InsuranceCredential"), "INVALID"),
        matchingResult(ldpVc("InsuranceCredential"), "SUCCESS"),
      ];

      const result = calculateVerifiedClaims([selectedClaim], submissions);

      expect(result).toEqual(submissions);
    });

    test("returns all service credentials when dcql query has multiple: true", () => {
      const selectedClaim = buildClaim("Health Insurance", "InsuranceCredential", [
        {
          id: "health_insurance_credential_id",
          format: "ldp_vc",
          multiple: true,
          meta: {
            type_values: [
              [
                "https://inji.github.io/inji-config/contexts/insurance-context.json#InsuranceCredential",
              ],
            ],
          },
        },
      ]);

      const result = calculateVerifiedClaims(
        [selectedClaim],
        [
          matchingResult({
            id: "urn:uuid:policy-1",
            type: ["VerifiableCredential", "InsuranceCredential"],
          }),
          matchingResult({
            id: "urn:uuid:policy-2",
            type: ["VerifiableCredential", "InsuranceCredential"],
          }),
        ]
      );

      expect(result).toHaveLength(2);
    });

    test("passes through service credentials even when type does not match selected claim", () => {
      const selectedClaim = buildClaim("MOSIP ID", "MOSIPVerifiableCredential", [
        {
          id: "mosip_verifiable_credential_id",
          format: "ldp_vc",
          meta: {
            type_values: [["MOSIPVerifiableCredential"]],
          },
        },
      ]);

      const submissions = [matchingResult(ldpVc("InsuranceCredential"))];

      const result = calculateVerifiedClaims([selectedClaim], submissions);

      expect(result).toEqual(submissions);
    });

    test("passes through credentials whose type IRIs end with trailing delimiters", () => {
      const selectedClaim = buildClaim("Context A", "ContextA", [
        {
          id: "context_a_credential_id",
          format: "ldp_vc",
          meta: {
            type_values: [["https://example.org/context-a#"]],
          },
        },
      ]);

      const hashSubmissions = [matchingResult(ldpVc("https://example.org/context-b#"))];
      const slashSubmissions = [matchingResult(ldpVc("https://example.org/context-d/"))];

      const hashResult = calculateVerifiedClaims(
        [selectedClaim],
        hashSubmissions
      );
      const slashResult = calculateVerifiedClaims(
        [buildClaim("Context C", "ContextC", [
          {
            id: "context_c_credential_id",
            format: "ldp_vc",
            meta: {
              type_values: [["https://example.org/context-c/"]],
            },
          },
        ])],
        slashSubmissions
      );

      expect(hashResult).toEqual(hashSubmissions);
      expect(slashResult).toEqual(slashSubmissions);
    });

    test("passes through credentials when dcql type_values are absent", () => {
      const selectedClaim = buildClaim("MOSIP ID", "MOSIPVerifiableCredential", [
        {
          id: "mosip_verifiable_credential_id",
          format: "ldp_vc",
          meta: {},
        },
      ]);

      const submissions = [matchingResult(ldpVc("MOSIPVerifiableCredential"))];

      const result = calculateVerifiedClaims([selectedClaim], submissions);

      expect(result).toEqual(submissions);
    });
  });

  describe("calculateUnverifiedClaims", () => {
    test("returns claims with no matching submitted credential", () => {
      const insuranceClaim = buildClaim("Life Insurance", "InsuranceCredential", [
        {
          id: "life_insurance_credential_id",
          format: "ldp_vc",
          meta: {
            type_values: [["InsuranceCredential"]],
          },
        },
      ]);
      const mosipClaim = buildClaim("MOSIP ID", "MOSIPVerifiableCredential", [
        {
          id: "mosip_verifiable_credential_id",
          format: "ldp_vc",
          meta: {
            type_values: [["MOSIPVerifiableCredential"]],
          },
        },
      ]);

      const unverified = calculateUnverifiedClaims(
        [insuranceClaim, mosipClaim],
        [matchingResult(ldpVc("InsuranceCredential"))]
      );

      expect(unverified).toHaveLength(1);
      expect(unverified[0].name).toBe("MOSIP ID");
    });

    test("returns empty when every selected claim has a matching submission", () => {
      const insuranceClaim = buildClaim("Life Insurance", "InsuranceCredential", [
        {
          id: "life_insurance_credential_id",
          format: "ldp_vc",
          meta: {
            type_values: [["InsuranceCredential"]],
          },
        },
      ]);
      const mosipClaim = buildClaim("MOSIP ID", "MOSIPVerifiableCredential", [
        {
          id: "mosip_verifiable_credential_id",
          format: "ldp_vc",
          meta: {
            type_values: [["MOSIPVerifiableCredential"]],
          },
        },
      ]);

      const unverified = calculateUnverifiedClaims(
        [insuranceClaim, mosipClaim],
        [
          matchingResult(ldpVc("InsuranceCredential")),
          matchingResult(ldpVc("MOSIPVerifiableCredential")),
        ]
      );

      expect(unverified).toHaveLength(0);
    });

    test("treats claim as verified when dcql type_values match even if top-level type differs", () => {
      const healthClaim = buildClaim("Health Insurance", "HealthCredential", [
        {
          id: "health_insurance_credential_id",
          format: "ldp_vc",
          meta: {
            type_values: [
              [
                "https://inji.github.io/inji-config/contexts/insurance-context.json#InsuranceCredential",
              ],
            ],
          },
        },
      ]);

      const unverified = calculateUnverifiedClaims(
        [healthClaim],
        [matchingResult(ldpVc("InsuranceCredential"))]
      );

      expect(unverified).toHaveLength(0);
    });

    test("returns all claims when no submissions match configured dcql type values", () => {
      const insuranceClaim = buildClaim("Life Insurance", "InsuranceCredential", [
        {
          id: "life_insurance_credential_id",
          format: "ldp_vc",
          meta: {
            type_values: [["InsuranceCredential"]],
          },
        },
      ]);
      const mosipClaim = buildClaim("MOSIP ID", "MOSIPVerifiableCredential", [
        {
          id: "mosip_verifiable_credential_id",
          format: "ldp_vc",
          meta: {
            type_values: [["MOSIPVerifiableCredential"]],
          },
        },
      ]);

      const unverified = calculateUnverifiedClaims(
        [insuranceClaim, mosipClaim],
        [matchingResult(ldpVc("RegistrationReceiptCredential"))]
      );

      expect(unverified).toHaveLength(2);
    });
  });

  describe("getTotalCredentialCount", () => {
    test("uses dcql credentials array length across selected claims", () => {
      const insuranceClaim = buildClaim("Life Insurance", "InsuranceCredential", [
        {
          id: "life_insurance_credential_id",
          format: "ldp_vc",
          meta: { type_values: [["InsuranceCredential"]] },
        },
      ]);
      const mosipClaim = buildClaim("MOSIP ID", "MOSIPVerifiableCredential", [
        {
          id: "mosip_verifiable_credential_id",
          format: "ldp_vc",
          meta: { type_values: [["MOSIPVerifiableCredential"]] },
        },
      ]);

      expect(
        getTotalCredentialCount(
          [matchingResult(ldpVc("MOSIPVerifiableCredential"))],
          [insuranceClaim],
          [insuranceClaim, mosipClaim]
        )
      ).toBe(2);
    });

    test("counts multiple submitted credentials for a single multiple:true query", () => {
      const insuranceClaim = buildClaim("Health Insurance", "InsuranceCredential", [
        {
          id: "health_insurance_credential_id",
          format: "ldp_vc",
          multiple: true,
          meta: {
            type_values: [
              [
                "https://inji.github.io/inji-config/contexts/insurance-context.json#InsuranceCredential",
              ],
            ],
          },
        },
      ]);

      expect(
        getTotalCredentialCount(
          [
            matchingResult({
              id: "urn:uuid:policy-1",
              type: ["VerifiableCredential", "InsuranceCredential"],
            }),
            matchingResult({
              id: "urn:uuid:policy-2",
              type: ["VerifiableCredential", "InsuranceCredential"],
            }),
          ],
          [],
          [insuranceClaim]
        )
      ).toBe(2);
    });
  });

  describe("getDetailsOrder", () => {
    let consoleErrorSpy: jest.SpyInstance;

    beforeEach(() => {
      consoleErrorSpy = jest.spyOn(console, "error").mockImplementation(() => undefined);
    });

    afterEach(() => consoleErrorSpy.mockRestore());

    test("returns an empty list for invalid credential input", () => {
      expect(getDetailsOrder("not json", "en")).toEqual([]);
      expect(getDetailsOrder(null, "en")).toEqual([]);
      expect(getDetailsOrder([], "en")).toEqual([]);
      expect(getDetailsOrder("text", "en")).toEqual([]);
    });

    test("orders insurance fields and resolves language, booleans and nested values", () => {
      const vc = {
        type: ["VerifiableCredential", "InsuranceCredential"],
        credentialSubject: {
          name: [{ "@language": "en", "@value": "Test User" }],
          active: true,
          nested: { value: "Nested value" },
          empty: "",
        },
      };

      expect(getDetailsOrder(JSON.stringify(vc), "en")).toEqual([
        { key: "name", value: "Test User" },
        { key: "active", value: "true" },
        { key: "nested", value: "Nested value" },
      ]);
    });

    test("orders farmer land fields and ignores empty fields", () => {
      const vc = {
        type: ["VerifiableCredential", "farmer"],
        credentialSubject: {
          farmerName: "Ravi",
          land: { district: "Mysuru", acres: 2, empty: "" },
        },
      };

      expect(getDetailsOrder(vc, "en")).toEqual([
        { key: "farmerName", value: "Ravi" },
        { key: "district", value: "Mysuru" },
        { key: "acres", value: "2" },
      ]);
    });

    test("filters SD-JWT internal fields and keeps visible claims", () => {
      expect(
        getDetailsOrder(
          {
            regularClaims: { type: "MockVerifiableCredential" },
            disclosedClaims: { id: "hidden", excluded: "hidden", name: "Ravi", score: 5 },
          },
          "en",
        ),
      ).toEqual([
        { key: "type", value: "MockVerifiableCredential" },
        { key: "name", value: "Ravi" },
        { key: "score", value: "5" },
      ]);
    });

    test("uses default ordering for nested object values", () => {
      expect(
        getDetailsOrder(
          { credentialSubject: { id: "hidden", address: { city: "Bengaluru", state: "Karnataka" } } },
          "en",
        ),
      ).toEqual([{ key: "address", value: ["Bengaluru", "Karnataka"] }]);
    });
  });

  describe("small helper functions", () => {
    test("counts all configured DCQL credentials", () => {
      expect(
        getDcqlCredentialQueryCount([
          buildClaim("One", "OneCredential", [{ id: "one", format: "ldp_vc" }]),
          buildClaim("Two", "TwoCredential", [
            { id: "two", format: "ldp_vc" },
            { id: "three", format: "ldp_vc" },
          ]),
        ]),
      ).toBe(3);
    });

    test("reads client and VP submission settings from the environment", () => {
      (window as any)._env_ = { CLIENT_ID: "inji-verify", VP_SUBMISSION_SUPPORTED: "TRUE" };
      expect(getClientId()).toBe("inji-verify");
      expect(isVPSubmissionSupported()).toBe(true);

      (window as any)._env_.VP_SUBMISSION_SUPPORTED = "false";
      expect(isVPSubmissionSupported()).toBe(false);
    });
  });
});
