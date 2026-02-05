import React, { useState } from "react";
import Form from "../components/Form";
import Loading from "../components/Loading";
import Result from "../components/Result";
import Stepper from "@keyvaluesystems/react-stepper";
import { useTranslation } from "react-i18next";

const Application = (props) => {
  const [isChild, setChild] = useState(0);
  const [accountData, setAccountData] = useState(null);
  const { t } = useTranslation("application");

  const handleFormSubmit = (data) => {
    setAccountData(data);
    setChild(1);
  };

  const desktopAccountSteps = [
    {
      stepLabel: t("details"),
      stepDescription: t("details_description"),
      isCompleted: isChild === 0,
    },
    {
      stepLabel: t("eligible_check"),
      stepDescription: t("eligible_check_description"),
      isCompleted: isChild === 1,
    },
    {
      stepLabel: t("result"),
      stepDescription: t("result_description"),
      isCompleted: isChild === 2,
    },
  ];

  const mobileAccountSteps = desktopAccountSteps.map((item) => {
    return { ...item, stepLabel: "", stepDescription: "" };
  });

  return (
    <div>
      <div className="lg:flex block">
        <div className="lg:w-[30%] lg:min-w-[320px] lg:max-w-[400px] bg-[#53389E] text-white lg:flex w-full block">
          <div className="self-center m-auto w-full px-4 py-6">
            {window.screen.availWidth < 1024 && (
              <p className="text-center pt-4 pb-2 font-semibold text-lg">
                {t("header")}
              </p>
            )}
            <Stepper
              steps={
                window.screen.availWidth >= 1024
                  ? desktopAccountSteps
                  : mobileAccountSteps
              }
              labelPosition="right"
              showDescriptionsForAllSteps
              currentStepIndex={isChild}
              orientation={
                window.screen.availWidth >= 1024 ? "vertical" : "horizontal"
              }
              renderNode={(step, stepIndex) => {
                if (
                  stepIndex < isChild ||
                  (desktopAccountSteps.length === stepIndex + 1 &&
                    stepIndex === isChild)
                ) {
                  return (
                    <div key={stepIndex}>
                      <img
                        src="assets/images/check.svg"
                        className="fill-[#fff] brightness-125"
                        alt="check"
                      />
                    </div>
                  );
                } else if (stepIndex === isChild) {
                  return (
                    <div key={stepIndex}>
                      <img
                        src="assets/images/dot.svg"
                        className="rounded-[25px] bg-[#7F56D9] brightness-125 ring-1 ring-white"
                        alt="active_dot"
                      />
                    </div>
                  );
                } else {
                  return (
                    <div key={stepIndex}>
                      <img src="assets/images/dot.svg" alt="dot" />
                    </div>
                  );
                }
              }}
            />
            {window.screen.availWidth < 1024 && (
              <div>
                {desktopAccountSteps.map((item, index) => {
                  return (
                    isChild === index && (
                      <div>
                        <p className="text-center">{item.stepLabel}</p>
                        <p className="text-center pb-6 text-[#E9D7FE]">
                          {item.stepDescription}
                        </p>
                      </div>
                    )
                  );
                })}
              </div>
            )}
          </div>
        </div>
        <div className="lg:w-[70%] bg-[#F9F5FF] lg:p-[3rem] w-full p-3">
          {isChild === 0 && <Form onSubmit={handleFormSubmit} />}
          {isChild === 1 && <Loading child={(value) => setChild(value)} />}
          {isChild === 2 && <Result accountData={accountData} />}
        </div>
      </div>
    </div>
  );
};

export default Application;
