import React, {useEffect} from 'react';
import LoaderWithBackdrop from "../commons/LoaderWithBackdrop";
import {useApplicationStateSelector} from "../../redux/features/application-state/application-state.selector";
import {useNavigate} from "react-router-dom";

function CheckingForInternetConnectivity(props: any) {
    const internetStatus = useApplicationStateSelector(state => state.internetConnectionStatus);
    const navigate = useNavigate();
    useEffect(() => {
        if (internetStatus === "OFFLINE") {
            navigate("/offline");
        }
    }, [internetStatus, navigate]);
    return internetStatus === "LOADING" ? (
        <LoaderWithBackdrop/>
    ) : null;
}

export default CheckingForInternetConnectivity;
