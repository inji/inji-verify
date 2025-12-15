import React from "react";
import { useTranslation } from "react-i18next";

type StatusPopupProps = {
  isOpen: boolean;
  onClose: () => void;

  // These will be i18n keys like "Accepted.Accept.title"
  title: string;
  description: string;
  buttonLabel: string;

  gifSrc?: string;
  gifAlt?: string;
};

const StatusPopup: React.FC<StatusPopupProps> = ({
  isOpen,
  onClose,
  title,
  description,
  buttonLabel,
  gifSrc,
  gifAlt = "status",
}) => {
  const { t } = useTranslation();

  if (!isOpen) return null;

  const translatedTitle = t(title);
  const translatedDescription = t(description);
  const translatedButtonLabel = t(buttonLabel);

  return (
    <div className="fixed inset-0 bg-black bg-opacity-40 flex items-center justify-center z-50">
      <div className="bg-white rounded-xl p-8 w-[480px] max-w-[90vw] text-center shadow-xl">
        {gifSrc && (
          <img src={gifSrc} alt={gifAlt} className="mx-auto w-28 mb-4" />
        )}

        <h2 className="text-2xl font-semibold mb-2">{translatedTitle}</h2>

        <p className="text-gray-700 mb-6">{translatedDescription}</p>

        <button
          onClick={onClose}
          className="px-6 py-3 rounded-lg bg-[#006DE7] text-white font-semibold w-full"
        >
          {translatedButtonLabel}
        </button>
      </div>
    </div>
  );
};

export default StatusPopup;
