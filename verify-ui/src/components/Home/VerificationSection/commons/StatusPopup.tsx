import React from "react";

type StatusPopupProps = {
  isOpen: boolean;
  onClose: () => void;

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
  if (!isOpen) return null;

  return (
    <div className="fixed inset-0 bg-black bg-opacity-40 flex items-center justify-center z-50">
      <div className="bg-white rounded-xl p-8 w-[480px] max-w-[90vw] text-center shadow-xl">
        {gifSrc && (
          <img src={gifSrc} alt={gifAlt} className="mx-auto w-28 mb-4" />
        )}

        <h2 className="text-2xl font-semibold mb-2">{title}</h2>

        <p className="text-gray-700 mb-6">{description}</p>

        <button
          onClick={onClose}
          className="px-6 py-3 rounded-lg bg-[#006DE7] text-white font-semibold w-full"
        >
          {buttonLabel}
        </button>
      </div>
    </div>
  );
};

export default StatusPopup;
