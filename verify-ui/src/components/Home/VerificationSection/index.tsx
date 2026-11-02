import React from 'react';
import Verification from "./Verification";
import Result from "./Result";
import {getStepConfig} from "../../../utils/config";
import {useVerificationFlowSelector} from "../../../redux/features/verification/verification.selector";
import { ScanQrCode } from './ScanQrCode';
import { Upload } from '../../../pages/Upload';

const DisplayActiveStep = () => {
    const {activeScreen, method} = useVerificationFlowSelector(state => ({activeScreen: state.activeScreen, qrData: state.qrReadResult?.qrData, method: state.method}));
    const screen = getStepConfig(method);
    if (!screen) return null;

    switch (activeScreen) {
        case screen.QrCodePrompt:
            return method === "SCAN" ? <ScanQrCode /> : <Upload />;
        case screen.ActivateCamera:
        case screen.Verifying:
            return <Verification />;
        case screen.DisplayResult:
            return <Result />;
        default:
            return null;
    }
}

const VerificationSection = () => {
    return (
        <DisplayActiveStep/>
    );
}

export default VerificationSection;
