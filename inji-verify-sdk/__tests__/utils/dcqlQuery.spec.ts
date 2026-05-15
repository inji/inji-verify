import { buildDcqlQueryFromPresentationDefinition } from "../../src/utils/dcqlQuery";

describe("buildDcqlQueryFromPresentationDefinition", () => {
  it("splits JSONPath dot segments into DCQL path array", () => {
    const dcql = buildDcqlQueryFromPresentationDefinition({
      purpose: "test",
      input_descriptors: [
        {
          id: "addr",
          constraints: {
            fields: [{ path: ["$.address.country"] }],
          },
        },
      ],
    });

    expect(dcql.credentials[0].claims).toEqual([
      { id: "address_country", path: ["address", "country"] },
    ]);
  });

  it("uses claim index when path is missing", () => {
    const dcql = buildDcqlQueryFromPresentationDefinition({
      purpose: "test",
      input_descriptors: [
        {
          id: "no-path",
          constraints: { fields: [{}] },
        },
      ],
    });

    expect(dcql.credentials[0].claims).toEqual([{ id: "claim_0", path: [] }]);
  });

  it("propagates vc+sd-jwt from the input descriptor format", () => {
    const dcql = buildDcqlQueryFromPresentationDefinition({
      purpose: "test",
      input_descriptors: [
        {
          id: "vc-cred",
          format: { "vc+sd-jwt": {} },
        },
      ],
    });

    expect(dcql.credentials[0].format).toBe("vc+sd-jwt");
  });

  it("defaults format to dc+sd-jwt when descriptor has no format", () => {
    const dcql = buildDcqlQueryFromPresentationDefinition({
      purpose: "test",
      input_descriptors: [{ id: "default-format" }],
    });

    expect(dcql.credentials[0].format).toBe("dc+sd-jwt");
  });
});
