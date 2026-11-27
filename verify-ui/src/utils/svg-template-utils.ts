import { AnyVc, LdpVc } from "../types/data-types";

function hasRenderMethod(vc: AnyVc): vc is LdpVc {
  return (
    typeof vc === "object" &&
    vc !== null &&
    "renderMethod" in vc &&
    (vc as LdpVc).renderMethod !== undefined
  );
}

export const getTemplateUrl = (vc: AnyVc): string | undefined => {
  if (!hasRenderMethod(vc)) return undefined;

  const methods = Array.isArray(vc.renderMethod)
    ? vc.renderMethod
    : typeof vc.renderMethod === "object" && vc.renderMethod !== null
      ? [vc.renderMethod]
      : [];

  const renderMethod = methods.find(
    (r: any) =>
      r?.renderSuite === "svg-mustache" &&
      r?.template?.mediaType === "image/svg+xml"
  );

  return renderMethod?.template?.id;
};

export const fetchSvgTemplate = async (url: string): Promise<string> => {
  try {
    const res = await fetch(url);
    if (!res.ok) {
      console.error("Failed to fetch SVG template:", res.statusText);
      return "";
    }
    return await res.text();
  } catch (err) {
    console.error("Error fetching SVG template:", err);
    return "";
  }
};
