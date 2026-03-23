import React, {useEffect, useState} from "react";
import { useVerificationFlowSelector, useVerifyFlowSelector } from "../../../redux/features/verification/verification.selector";
import { convertToId, fetchVerificationSteps } from "../../../utils/misc";
import { VerificationMethod } from "../../../types/data-types";
import i18n from "i18next";

const Step = ({ stepNumber, isCompleted, isActive }: { stepNumber: number; isCompleted: boolean; isActive: boolean }) => {
  const theme = window._env_.DEFAULT_THEME;
  const activeOrCompleted = isCompleted || isActive;
  const base = "flex items-center";
  const activeClass = `rounded-full bg-${theme}-gradient bg-no-repeat text-white`;
  const upcomingClass = `bg-${theme}-gradient bg-no-repeat rounded-full text-primary p-[2px]`;
  const stepperBackgroundTheme = activeOrCompleted ? `bg-${theme}-gradient` : "bg-white";
  const stepperCircle = `${stepperBackgroundTheme} bg-no-repeat flex items-center justify-center w-9 h-9 rounded-full border-[1px] border-transparent`;

  return (
    <div className={`${base} ${activeOrCompleted ? activeClass : upcomingClass}`} >
      <div className={stepperCircle}>{stepNumber}</div>
    </div>
  );
};

function MobileStepper() {
  const { mainActiveScreen, method } = useVerificationFlowSelector((state) => ({
    mainActiveScreen: state.activeScreen,
    method: state.method,
  }));
  const VerifyActiveScreen = useVerifyFlowSelector((state) => state.activeScreen);
  const flow = useVerifyFlowSelector((state) => state.flowType);
  const isPartiallyShared = useVerifyFlowSelector((state) => state.isPartiallyShared);
  const activeScreen = (method === "VERIFY") ? VerifyActiveScreen : mainActiveScreen;

  const [VerificationStepsContent, setVerificationStepsContent] = useState(() =>
    fetchVerificationSteps(
      method as VerificationMethod,
      isPartiallyShared,
      flow,
      activeScreen
    )
  );

  useEffect(() => {
    if (!flow) return;

  const updateSteps = () => {
    const updatedSteps = fetchVerificationSteps(
      method as VerificationMethod,
      isPartiallyShared,
      flow,
      activeScreen
    );
    setVerificationStepsContent(updatedSteps);
  };

  updateSteps();
  i18n.on("languageChanged", updateSteps);
  return () => i18n.off("languageChanged", updateSteps);
  }, [method, isPartiallyShared, flow, activeScreen]);

  const theme = window._env_.DEFAULT_THEME;
  const activeStep = VerificationStepsContent.find((step) => step.isActive);
  const label = activeStep?.label as string;
  const description = activeStep?.description as string;

  return (
    <div className="grid grid-cols-13 flex-column mx-auto items-center">
      <div
        className="col-start-1 col-end-13 flex items-center mx-auto p-4"
        id="stepper"
      >
        {VerificationStepsContent.map((step, index) => (
          <div key={index} className="flex flex-column items-center">
            <Step
              stepNumber={step.stepNumber}
              isCompleted={step.isCompleted}
              isActive={step.isActive}
            />
            {index < VerificationStepsContent.length - 1 && (
              <div className={`bg-${theme}-gradient p-[1px] w-[44px] h-[1px] ${ !VerificationStepsContent[index].isCompleted ? "opacity-20" : "" }`} >
                <div className="flex-grow border-t-2 border-transparent" />
              </div>
            )}
          </div>
        ))}
      </div>
      <div className="col-start-1 col-end-13 text-center px-4">
        <p id={convertToId(label)} className={`font-bold md:text-smallTextSize text-normalTextSize my-1 ${activeStep?.isActive ? "text-black" : "text-stepperLabel"}`}>
          {label}
        </p>

          <div
              id={`${convertToId(label)}-description`}
              className="text-stepperDescription text-smallTextSize md:text-normalTextSize text-left"
          >
              {(description || "").split("\n").map((line: string, index: number) => {
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
      </div>
    </div>
  );
}

export default MobileStepper;
