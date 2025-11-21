import React from 'react';
import Verification from "./Verification";
import Result from "./Result";
import {VerificationSteps} from "../../../utils/config";
import {useVerificationFlowSelector} from "../../../redux/features/verification/verification.selector";
import { ScanQrCode } from './ScanQrCode';
import { Upload } from '../../../pages/Upload';

const DisplayActiveStep = () => {
    const {activeScreen, method} = useVerificationFlowSelector(state => ({activeScreen: state.activeScreen, qrData: state.qrReadResult?.qrData, method: state.method}));
    
    switch (activeScreen) {
        case VerificationSteps[method].QrCodePrompt:
            return method ==='SCAN' ? <ScanQrCode/> : <Upload/>
        case VerificationSteps[method].ActivateCamera:
        case VerificationSteps[method].Verifying:
            return (<Verification/>);
        case VerificationSteps[method].DisplayResult:
            return (<Result/>);
        default:
            return (<></>);
    }
}

const VerificationSection = () => {
    return (
        <>
            <DisplayActiveStep/>
        </>
    );
}

export default VerificationSection;