import React, { useRef } from "react";
import { useAppDispatch } from "../../../../redux/hooks";
import { useVerificationFlowSelector } from "../../../../redux/features/verification/verification.selector";
import { goToHomeScreen } from "../../../../redux/features/verification/verification.slice";
import { VerificationMethod } from "../../../../types/data-types";
import { raiseAlert } from "../../../../redux/features/alerts/alerts.slice";
import { AlertMessages, Pages } from "../../../../utils/config";
import { MdArrowForwardIos } from "react-icons/md";
import { MdArrowBackIos } from "react-icons/md";
import { useTranslation } from 'react-i18next';
import { useNavigate } from "react-router-dom";
import { resetVpRequest } from "../../../../redux/features/verify/vpVerificationState";

const Tab = ({
  id,
  active,
  label,
  disabled,
  onClick,
}: {
  id: string;
  active: boolean;
  label: string;
  disabled?: boolean;
  onClick?: () => void;
}) => {
  const activeTab =
    `bg-${window._env_.DEFAULT_THEME}-gradient border-t-[6px] border-y-transparent text-activeTabText`;
  const inactiveTab = "bg-inactiveTabBackground text-inactiveTabText";
  const disabledTab = "text-disableTabText bg-disableTabBackground";
  const enabledTab = active ? activeTab : inactiveTab;
  return (
      <button
        id={id}
        className={`min-w-[214px] py-4 px-4 focus:outline-none self-end rounded-t-xl shadow-xl shadow-[0_2px_6px_0] ${
          disabled ? disabledTab : enabledTab
        }`}
        onClick={onClick}
      >
        {label}
      </button>
  );
};

function VerificationMethodTabs(props: any) {
  const dispatch = useAppDispatch();
  const navigate = useNavigate();
  const method = useVerificationFlowSelector((state) => state.method);
  const {t} = useTranslation('Tab')
  const carouselRef: any = useRef<HTMLDivElement>();

  function switchToVerificationMethod(method: VerificationMethod) {
    if (!localStorage.getItem("path")) localStorage.setItem("path", "/");
    localStorage.setItem(
      "path",
      method === "UPLOAD" ? Pages.Home : method === "SCAN" ? Pages.Scan : Pages.VerifyCredentials
    );
    dispatch(goToHomeScreen({ method }));
    dispatch(resetVpRequest());
  }

  function showAlert() {
    dispatch(
      raiseAlert({ ...AlertMessages().verificationMethodComingSoon, open: true })
    );
  }

  const handlePrevious = () => {
    if (carouselRef.current) {
      carouselRef.current.scrollBy({ left: -200, behavior: "smooth" });
    }
  };

  const handleNext = () => {
    if (carouselRef.current) {
      carouselRef.current.scrollBy({ left: 200, behavior: "smooth" });
    }
  };

  return (
    <div className={`container mx-auto w-[100%] bg-${window._env_.DEFAULT_THEME}-lighter-gradient  max-w-[100vw] overflow-x-hidden lg:overflow-x-auto`}>
      <div className="flex flex-row items-center mx-auto justify-center relative">
        <div className="absolute left-0 h-full w-12 bg-light-gradient md:hidden grid items-center">
          <button
            id="tabs-carousel-left-icon"
            onClick={handlePrevious}
            className="focus:outline-none"
          >
            <>
              <MdArrowBackIos className="mx-auto" />
            </>
          </button>
        </div>
        <div
          className="flex w-[calc(100vw-96px)] md:w-full mx-auto overflow-x-scroll md:overflow-x-auto"
          ref={carouselRef}
        >
          <div className="flex space-x-0.5 border-gray-200 font-bold items-end mx-auto lg:justify-center">
            <Tab
              id="upload-qr-code-tab"
              active={method === "UPLOAD"}
              label={t('upload')}
              onClick={() => {
                switchToVerificationMethod("UPLOAD");
                navigate(Pages.Home);
              }}
            />
            <Tab
              id="scan-qr-code-tab"
              active={method === "SCAN"}
              label={t('scan')}
              onClick={() => {
                switchToVerificationMethod("SCAN");
                navigate(Pages.Scan);
              }}
            />
            <Tab
              id="vp-verification-tab"
              active={method === "VERIFY"}
              label={t('VP_Verification')}
              onClick={() => {
                switchToVerificationMethod("VERIFY");
                navigate(Pages.VerifyCredentials);
              }}
            />
            <Tab
              id="ble-tab"
              active={false}
              label={t('ble')}
              disabled
              onClick={showAlert}
            />
          </div>
        </div>
        <div className="absolute right-0 h-full w-12 bg-light-gradient md:hidden grid items-center">
          <button
            id="tabs-carousel-right-icon"
            onClick={handleNext}
            className="focus:outline-none"
          >
            <>
              <MdArrowForwardIos className="mx-auto" />
            </>
          </button>
        </div>
      </div>
      <div
        id="horizontalLine"
        className={`bg-${window._env_.DEFAULT_THEME}-gradient h-[3px] border-b-2 border-b-transparent`}
      />
    </div>
  );
}

export default VerificationMethodTabs;
