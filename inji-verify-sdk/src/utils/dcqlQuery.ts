import type { PresentationDefinition } from "../components/openid4vp-verification/OpenID4VPVerification.types";

const SUPPORTED_DESCRIPTOR_FORMAT_KEYS = new Set(["dc+sd-jwt", "vc+sd-jwt"]);

const assertDescriptorFormatSupported = (
  descriptor: PresentationDefinition["input_descriptors"][number],
) => {
  if (!descriptor.format) {
    return;
  }
  const unsupported = Object.keys(descriptor.format).filter(
    (key) => !SUPPORTED_DESCRIPTOR_FORMAT_KEYS.has(key),
  );
  if (unsupported.length > 0) {
    throw new Error(
      `Input descriptor "${descriptor.id}" format is not supported for DCQL conversion (${unsupported.join(", ")}). Supported formats: ${[...SUPPORTED_DESCRIPTOR_FORMAT_KEYS].join(", ")}.`,
    );
  }
};

export const buildDcqlQueryFromPresentationDefinition = (
  presentationDefinition: PresentationDefinition,
) => {
  const credentials = presentationDefinition.input_descriptors.map(
    (descriptor) => {
      assertDescriptorFormatSupported(descriptor);

      const claims =
        descriptor.constraints?.fields?.map((field, index) => {
          const firstPath = field.path?.[0]?.replace(/^\$\./, "") ?? "";
          const segments = firstPath ? firstPath.split(".") : [];
          return {
            id: firstPath ? firstPath.replace(/\./g, "_") : `claim_${index}`,
            path: segments,
          };
        }) || [];

      const vctField = descriptor.constraints?.fields?.find(
        (field) => field.filter?.pattern,
      );

      const descriptorFormat = descriptor.format
        ? Object.keys(descriptor.format).find((k) =>
            SUPPORTED_DESCRIPTOR_FORMAT_KEYS.has(k),
          )
        : undefined;

      return {
        id: descriptor.id,
        format: descriptorFormat ?? "dc+sd-jwt",
        meta: vctField?.filter?.pattern
          ? {
              vct_values: [vctField.filter.pattern],
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
