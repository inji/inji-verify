import { useEffect, useState } from "react";
import Mustache from "mustache";
import { AnyVc } from "../../../../types/data-types";
import { fetchSvgTemplate } from "../../../../utils/svg-template-utils";
import Loader from "../../../commons/Loader";
import DOMPurify from "dompurify";
import QRCode from "qrcode";

interface VcSvgTemplateProps {
  vc: AnyVc;
  templateUrl: string;
  onError?: (error:Error) => void;
}

const VcSvgTemplate = ({ vc, templateUrl, onError }: VcSvgTemplateProps) => {
  const [templateContent, setTemplateContent] = useState<string>("");
  const [loader, setLoader] = useState(false);
  const [qrCodeDataUrl, setQrCodeDataUrl] = useState<string>("");

  // Generate QR code
  useEffect(() => {
    QRCode.toDataURL("https://verify.example.com/credential/123", {
      width: 256,
      margin: 2,
      color: {
        dark: "#000000",
        light: "#FFFFFF"
      }
    })
    .then((url) => {
      setQrCodeDataUrl(url);
    })
    .catch((err) => {
      console.error("QR generation error:", err);
    });
  }, []);

  useEffect(() => {
    const loadTemplate = async () => {
      if (!templateUrl) {
        setLoader(false);
        return;
      }

      setLoader(true);
      try {
        const svgTemplate = await fetchSvgTemplate(templateUrl);
        if (svgTemplate) {
          setTemplateContent(svgTemplate);
          setLoader(false);
        } else {
          throw new Error("Failed to load credential template");
        }
      } catch (err) {
        console.error("Template fetch error:", err);
        onError?.(new Error(err instanceof Error ? err.message : "Failed to fetch template"));
      } finally {
        setLoader(false);
      }
    };
    loadTemplate();
  }, [onError, templateUrl]);

  if (loader) return <Loader innerBg="bg-white" className="w-5 h-5 mt-20" />;
  if (!templateContent) return null;

  try {
    const preprocessedTemplate = templateContent.replaceAll(
      /\{\{\/([^}]+)\}\}/g,
      (_, path) => {
        const trimmed = path.trim();
        return `{{${trimmed.replaceAll("/", ".")}}}`;
      }
    );

    const renderedVc = {
      ...vc,
      qrCodeImage: qrCodeDataUrl
    };
    
    let renderedSvg = Mustache.render(preprocessedTemplate, renderedVc);
    
    renderedSvg = DOMPurify.sanitize(renderedSvg, {
      USE_PROFILES: { svg: true, svgFilters: true },
      ADD_TAGS: ["use", "image"],
      ADD_ATTR: ["target", "href", "xlink:href", "preserveAspectRatio", "x", "y", "width", "height", "id"],
      ALLOW_DATA_ATTR: true,
      FORBID_TAGS: ["script", "iframe", "object", "embed"],
      FORBID_ATTR: ["onerror", "onload", "onclick", "onmouseover"],
    });
    
    return (
      <div className="w-full flex justify-center items-center">
        <div dangerouslySetInnerHTML={{ __html: renderedSvg }} />
      </div>
    );
  } catch (err) {
    console.error("Mustache render error:", err);
    onError?.(new Error(err instanceof Error ? err.message : "Failed to render template"));
    return null;
  }
};

export default VcSvgTemplate;
