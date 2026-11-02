import {useEffect} from 'react';
import {useNavigate} from "react-router-dom";
import {useAppDispatch} from "../redux/hooks";
import {raiseAlert} from "../redux/features/alerts/alerts.slice";
import {AlertMessages, Pages} from "../utils/config";

function PageNotFound404(props: any) {
    const navigate = useNavigate();
    const dispatch = useAppDispatch();

    useEffect(() => {
        navigate(Pages.Home);
        dispatch(raiseAlert({...AlertMessages().pageNotFound}));
    }, [navigate, dispatch]);

    return null;
}

export default PageNotFound404;