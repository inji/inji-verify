import type { PresentationDefinition } from "../components/openid4vp-verification/OpenID4VPVerification.types";

export const buildDcqlQueryFromPresentationDefinition = (
  presentationDefinition: PresentationDefinition,
) => {
  const credentials = presentationDefinition.input_descriptors.map(
    (descriptor) => {
      const claims =
        descriptor.constraints?.fields?.map((field, index) => ({
          id:
            field.path?.[0]?.replace(/^\$\./, "")?.replace(/\./g, "_") ||
            `claim_${index}`,
          path:
            field.path?.map((path: string) => path.replace(/^\$\./, "")) || [],
        })) || [];

      const filter = descriptor.constraints?.fields?.[0]?.filter;

      return {
        id: descriptor.id,
        format: "dc+sd-jwt",
        meta: filter?.pattern
          ? {
              vct_values: [filter.pattern],
            }
          : undefined,
        claims,
      };
    },
  );

  return {
    credentials,
  };
};
