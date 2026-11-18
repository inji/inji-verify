import * as React from "react";
import { CameraAccessDeniedIcon } from "../../../utils/theme-utils";
import { Button } from "./commons/Button";
import { useTranslation } from "react-i18next";

interface ModalProps {
  isOpen: boolean;
  onClose: () => void;
  children: React.ReactNode;
}

const Modal = ({ isOpen, onClose, children }: ModalProps) => {
  if (!isOpen) return null;

  return (
    <div className="fixed inset-0 z-50 flex items-center justify-center">
      <button
        type="button"
        className="absolute inset-0 bg-black/50 transition-opacity duration-300"
        onClick={onClose}
        aria-label="Close modal"
      />
      <div
        className="relative z-10 transform transition-all duration-300 scale-100 opacity-100"
        role="dialog"
        aria-modal="true"
      >
        {children}
      </div>
    </div>
  );
};

const CameraAccessDenied = ({open, handleClose}: {open: boolean; handleClose: () => void;}) => {
  const { t } = useTranslation("CameraAccessDenied");

  return (
    <Modal isOpen={open} onClose={handleClose}>
      <div
        className="
          w-[570px]
          max-w-[95vw] lg:max-w-md
          bg-white
          grid
          place-items-center
          rounded-[20px]
          p-[38px]
          shadow-lg
          text-center
          fill-primary
          animate-scaleIn
        "
      >
        <CameraAccessDeniedIcon />

        <p
          id="camera-access-denied"
          className="font-bold text-mediumTextSize text-cameraDeniedLabel my-3 mx-auto"
        >
          {t("header")}
        </p>

        <p
          id="camera-access-denied-description"
          className="font-normal text-lgNormalTextSize text-cameraDeniedDescription my-3 mx-auto"
        >
          {t("description")}
        </p>

        <Button
          id="camera-access-denied-okay-button"
          title={t("okay")}
          onClick={handleClose}
          className="w-[180px] my-1.5 text-lgNormalTextSize inline-flex"
          data-testid="camera-access-denied-okay"
        />
      </div>
    </Modal>
  );
};

export default CameraAccessDenied;
