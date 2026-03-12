import React, { useEffect, useState } from "react";
import { useVerificationFlowSelector, useVerifyFlowSelector } from "../../../redux/features/verification/verification.selector";
import { convertToId, fetchVerificationSteps } from "../../../utils/misc";
import i18n from "i18next";
import { useAppSelector } from "../../../redux/hooks";
import { VerificationMethod } from "../../../types/data-types";
import { isRTL } from "../../../utils/i18n";

const DesktopStepper: React.FC = () => {
  const { mainActiveScreen, method } = useVerificationFlowSelector((state) => ({
    mainActiveScreen: state.activeScreen,
    method: state.method,
  }));
  const verifyState = useVerifyFlowSelector((state) => state);
  const activeScreen = method === "VERIFY" ? verifyState.activeScreen : mainActiveScreen;
  const language = useAppSelector((state) => state.common.language);
  const rtl = isRTL(language);

  const [steps, setSteps] = useState(() =>
    fetchVerificationSteps(
      method as VerificationMethod,
      verifyState.isPartiallyShared,
      verifyState.flowType,
      activeScreen
    )
  );

  useEffect(() => {
    const updateSteps = () => {
      const newSteps = fetchVerificationSteps(
        method as VerificationMethod,
        verifyState.isPartiallyShared,
        verifyState.flowType,
        activeScreen
      );
      setSteps(newSteps);
    };
    updateSteps();
    i18n.on("languageChanged", updateSteps);
    return () => i18n.off("languageChanged", updateSteps);
  }, [method, verifyState.isPartiallyShared, verifyState.flowType, activeScreen]);

  const isLastStep = (index: number) => steps.length - 1 === index;
  const isStepCompleted = (index: number) => activeScreen > index;

  return (
    <div className="pr-[60px] pl-[76px] flex flex-col items-start justify-start ml-0 mt-9 max-h-[100%]">
      <div className="flex flex-col items-start space-y-2">
        {steps.map((step, index) => {
          const lastStepMargin = rtl ? "mr-9" : "ml-9";
          const stepMargin = rtl ? "mr-[10px]" : "ml-[10px]";
          const marginClass = isLastStep(index) ? lastStepMargin : stepMargin;

          return (
            <div key={index}>
              <div className="flex items-center">
                <div className={`bg-${window._env_.DEFAULT_THEME}-gradient rounded-full bg-no-repeat p-[2px] flex items-center justify-center`}>
                  <div
                    className={`text-center rounded-full w-6 h-6 flex items-center justify-center font-normal text-smallTextSize leading-5 bg-no-repeat  ${
                      isStepCompleted(index)
                        ? `bg-${window._env_.DEFAULT_THEME}-gradient text-white border-1 border-transparent`
                        : "bg-white text-primary border-[1px] border-transparent"
                    }`}
                  >
                    {step.stepNumber}
                  </div>
                </div>
                <div
                  id={convertToId(step.label)}
                  className={`${stepMargin} text-lgNormalTextSize font-bold ${
                    isStepCompleted(index) ? "text-black" : "text-stepperLabel"
                  }`}
                >
                  {step.label}
                </div>
              </div>
              <div className="grid items-start">
                <div className="grid items-center m-0">
                  {!isLastStep(index) && (
                    <div className={`w-6 h-full col-end-2 ${isStepCompleted(index + 1) ? "" : "opacity-20"}`}>
                      <div className={`bg-${window._env_.DEFAULT_THEME}-gradient mt-1 w-[1px] h-full m-auto`}>
                        <div className="border-transparent border-l-transparent border-[1px]" />
                      </div>
                    </div>
                  )}
                  <div
                    id={`${convertToId(step.label)}-description`}
                    className={`${marginClass} text-normalTextSize text-stepperDescription font-normal col-end-13`}
                  >
                      {(step.description || "").split("\n").map((line: string, index: number) => {
                          const containsColon = line.includes(":");

                          if (containsColon) {
                              const [title, ...rest] = line.split(":");

                              return (
                                  <ul key={index} className="list-disc pl-6 mt-1">
                                      <li>
                                          <strong>{title}:</strong> {rest.join(":")}
                                      </li>
                                  </ul>
                              );
                          }

                          return <p key={index}>{line}</p>;
                      })}
                  </div>
                  {!isLastStep(index) && (
                    <div className={`col-end-2 ${isStepCompleted(index + 1) ? "" : "opacity-20"}`}>
                      <div className={`bg-${window._env_.DEFAULT_THEME}-gradient w-[1px] h-8 m-auto`}>
                        <div className="border-transparent border-l-primary border-[1px]" />
                      </div>
                    </div>
                  )}
                </div>
              </div>
            </div>
          );
        })}
      </div>
    </div>
  );
};

export default DesktopStepper;
