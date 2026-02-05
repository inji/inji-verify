import React, { useState } from "react";
import StyledButton from "../../../components/Home/VerificationSection/commons/StyledButton";
import { useTranslation } from "react-i18next";

const ModalPopup = () => {
  const [isPopup, setPopup] = useState(true);
  const { t } = useTranslation("modal");

  const style = {
    position: "absolute",
    top: "50%",
    left: "50%",
    transform: "translate(-50%, -50%)",
    width: 570,
    backgroundColor: "white",
    display: "grid",
    placeItems: "center",
    borderRadius: "20px",
    outline: "none",
    padding: "38px",
    display: "block"
  };

  const Modal = ({ children }) => (
    <div className="fixed z-10 inset-0 overflow-y-auto">
      <div className="flex items-center justify-center min-h-screen">
        <div className="fixed inset-0 bg-black opacity-50"></div>
        <div className="relative bg-white max-w-[95vw] lg:max-w-md p-6 rounded-lg shadow-xl">
          {children}
        </div>
      </div>
    </div>
  );

  const Fade = ({ children }) => (
    <div
      x-show="isOpen"
      className="fixed inset-0 flex items-center justify-center"
    >
      <div className="fixed inset-0 transition-opacity">
        <div className="absolute inset-0 bg-black opacity-30"></div>
      </div>
      <div className="relative bg-white max-w-[95vw] lg:max-w-md p-6 rounded-lg shadow-xl">
        {children}
      </div>
    </div>
  );

  const handleClose = () => {
    setPopup(false);
  };

  return (
    isPopup && (
      <div>
        <Modal>
          <Fade>
            <div
              className="container grid justify-items-center items-center text-center sm:max-w-xl shadow-lg max-w-xs"
              style={style}
            >
              <p className="text-center font-semibold text-[36px] pb-4">
                {t("congrats")}
              </p>
              <img
                src="assets/images/cibil.svg"
                alt="verification_success"
                className="block m-auto"
              />
              <p className="text-center font-normal text-[16px] mb-4 sm:px-[4rem]">
                {t("verification_success")}
              </p>
              <StyledButton
                id="verification-success-close-button"
                onClick={handleClose}
                className="w-[180px] mx-auto my-[18px] !text-[#7F56D9] hover:!text-white !border-[#7F56D9] hover:!bg-[#7F56D9] !rounded-xl !px-[8rem] sm:!px-[10rem]"
                data-testid="verification-success-close"
              >
                {t("close")}
              </StyledButton>
            </div>
          </Fade>
        </Modal>
      </div>
    )
  );
};

export default ModalPopup;
