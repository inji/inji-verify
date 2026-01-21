import {createSlice} from "@reduxjs/toolkit";
import {VerificationState} from "../../../types/data-types";
import {VerificationSteps} from "../../../utils/config";

export const PreloadedState: VerificationState = {
    alert: {},
    qrReadResult: {status: "NOT_READ"},
    method: 'UPLOAD',
    activeScreen: VerificationSteps["UPLOAD"].QrCodePrompt,
    verificationResult: {vc: undefined, vcStatus: undefined},
    ovp: {}
};

const verificationSlice = createSlice({
    reducers: {
        selectMethod: (state, action) => {
            state.method = action.payload.method;
        },
        qrReadInit: (state, action) => {
            const method = action.payload.method;
            state.activeScreen = method === "SCAN" ? VerificationSteps[state.method].ActivateCamera : VerificationSteps[method].Verifying;
            state.method = method;
        },
        verificationInit: (state, action) => {
            state.activeScreen = VerificationSteps[state.method].Verifying;
            state.qrReadResult = action.payload.qrReadResult;
            state.ovp = action.payload;
        },
        verificationComplete: (state, action) => {
            state.activeScreen = VerificationSteps[state.method].DisplayResult;
            state.verificationResult = action.payload.verificationResult;
        },
        goToHomeScreen: (state, action) => {
            const method = action.payload.method ?? state.method;
            state.method = method;
            state.qrReadResult = {status: "NOT_READ"};
            state.activeScreen =  method === "VERIFY" ? VerificationSteps[state.method].InitiateVpRequest : VerificationSteps[state.method].QrCodePrompt ;
            state.verificationResult = {vc: undefined, vcStatus: undefined};
            state.ovp = {};
        }
    },
    name: "VcVerification",
    initialState: PreloadedState
})

export const {
    qrReadInit, verificationInit,
    verificationComplete, goToHomeScreen, selectMethod
} = verificationSlice.actions;

export default verificationSlice.reducer;
