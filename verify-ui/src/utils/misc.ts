// match fot the occurrence of an uppercase letter
import i18next from "i18next";
import {VerificationMethod, VerificationStep, VerificationStepsContentType} from "../types/data-types";
import {
  InternetConnectivityCheckTimeout,
  InternetConnectivityCheckEndpoint,
  getVerificationStepsContent,
    VerificationStepWithStatus
} from "./config";

const splitCamelCaseRegex: RegExp = /([A-Z][a-z]+)/g;

// match if the first char is lower case
const lowercaseStartRegex: RegExp = /^([a-z])/;

export const convertToTitleCase = (text: string): string => {
    if (!text) return "";
    return text
        // Once match is found, split the words by adding space at the beginning of the natch and ensure the first letter is capital
        .replaceAll(splitCamelCaseRegex, (match) => ` ${match.charAt(0).toUpperCase()}${match.slice(1)}`)
        // convert the first char of 'text' to capital case
        .replace(lowercaseStartRegex, (match) => match.toUpperCase());
};

export const getDisplayValue = (data: any): string => {
    if (Array.isArray(data) && data?.length > 0) {
        let displayValue = "";
        data.forEach(value => {
            displayValue += `${value}, `;
        });
        return displayValue.slice(0, displayValue.length - 2);
    }
    return data?.toString();
}

export const fetchVerificationSteps = (
    method: VerificationMethod,
    isPartiallyShared: boolean,
    flowType?: "sameDevice" | "crossDevice",
    activeScreen: number = 1
): VerificationStepWithStatus[] => {
    const verificationContent: VerificationStepsContentType = getVerificationStepsContent();
    let selectedSteps: VerificationStep[] = [];

    if (method === "UPLOAD") {
        selectedSteps = verificationContent.UPLOAD;
    } else if (method === "SCAN") {
        selectedSteps = verificationContent.SCAN;
    } else if (method === "VERIFY") {
        const verifySteps = verificationContent.VERIFY;
        const stepMap = Object.fromEntries(verifySteps.map((step) => [step.label, step]));

        selectedSteps.push(stepMap[i18next.t("VerificationStepsContent:VERIFY.InitiateVpRequest.label")]!);

        if (isPartiallyShared) {
            selectedSteps.push(stepMap[i18next.t("VerificationStepsContent:VERIFY.RequestMissingCredential.label")]!);
        } else {
            selectedSteps.push(stepMap[i18next.t("VerificationStepsContent:VERIFY.SelectCredential.label")]!);
        }

        selectedSteps.push(stepMap[i18next.t("VerificationStepsContent:VERIFY.ShareVerifiableCredentials.label")]!);

        selectedSteps.push(stepMap[i18next.t("VerificationStepsContent:VERIFY.DisplayResult.label")]!);
    }

    return selectedSteps
        .filter(Boolean)
        .map((step, index) => ({
            ...step,
            stepNumber: index + 1,
            isCompleted: index + 1 < activeScreen,
            isActive: index + 1 === activeScreen,
        }));
};


    export const getRangeOfNumbers = (length: number): number[] => {
    return Array.from(new Array(length), (x, i) => i + 1);
}

export const getFileExtension = (fileName: string) => fileName.slice(
    ((fileName.lastIndexOf('.') - 1) >>> 0) + 2
);

export const checkInternetStatus = async (): Promise<boolean> => {
    if (!window.navigator.onLine) return false;
    const controller = new AbortController();
    const timeoutId = setTimeout(() => {
        controller.abort()
    }, InternetConnectivityCheckTimeout);
    try {
        // Try making an api call if the window.navigator.onLine is true
        await fetch(InternetConnectivityCheckEndpoint, {
            method: 'HEAD',
            mode: 'no-cors',
            cache: 'no-cache',
            headers: {
                'Content-Type': 'text/plain'
            },
            signal: controller.signal
        }); // Use a reliable external endpoint
        return true;
    } catch (error) {
        console.error("Error occurred while checking for internet connectivity: ", error);
        return false; // Network request failed, assume offline
    } finally {
        clearTimeout(timeoutId);
    }
}

export const convertToId = (content: string) => content.toLowerCase().replaceAll(" ", "-");

export const saveData = async (vc: any) => {
  const myData = vc;
  const fileName = vc.type ? vc?.type[1] : "Inji_Verify_Credential_Data";
  const json = JSON.stringify(myData, null, 2);
  const blob = new Blob([json], { type: "application/json" });
  const href = URL.createObjectURL(blob);
  const link = document.createElement("a");
  link.href = href;
  link.download = fileName + ".json";
  document.body.appendChild(link);
  link.click();
  document.body.removeChild(link);
  URL.revokeObjectURL(href);
};
