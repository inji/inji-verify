import * as React from "react";
import { CameraAccessDeniedIcon } from "../../../utils/theme-utils";
import { Button } from "./commons/Button";
import { useTranslation } from "react-i18next";

interface ModalProps {
  isOpen: boolean;
  onClose: () => void;
  children: React.ReactNode;
}

const Modal = ({ isOpen, onClose, children }: Readonly<ModalProps>) => {
  const dialogRef = React.useRef<HTMLDialogElement>(null);

  React.useEffect(() => {
    const dialog = dialogRef.current;
    if (!isOpen || !dialog) return;
    const previousFocus = document.activeElement;
    dialog.showModal();
    return () => {
      dialog.close();
      if (previousFocus instanceof HTMLElement && previousFocus.isConnected) {
        previousFocus.focus();
      }
    };
  }, [isOpen]);

  if (!isOpen) return null;

  return (
    <dialog
      ref={dialogRef}
      className="m-auto p-0 border-0 bg-transparent overflow-visible backdrop:bg-black/50"
      aria-labelledby="camera-access-denied"
      aria-describedby="camera-access-denied-description"
      onCancel={(event) => {
        event.preventDefault();
        onClose();
      }}
      onClick={(event) => {
        if (event.target === event.currentTarget) onClose();
      }}
    >
      {children}
    </dialog>
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
