import { Dispatch } from "redux";
import { checkInternetStatus } from "./misc";
import { SupportedFileTypes } from "./config";
import { updateInternetConnectionStatus } from "../redux/features/application-state/application-state.slice";

export const acceptedFileTypes = SupportedFileTypes.map((ext) => `.${ext}`).join(", ");


const doInternetCheck = async (dispatch: Dispatch) => {
  dispatch(
    updateInternetConnectionStatus({ internetConnectionStatus: "LOADING" })
  );
  let isOnline = await checkInternetStatus();
  dispatch(
    updateInternetConnectionStatus({
      internetConnectionStatus: isOnline ? "ONLINE" : "OFFLINE",
    })
  );
  return isOnline;
};

