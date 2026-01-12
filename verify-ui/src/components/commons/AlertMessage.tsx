import React, { useEffect } from "react";
import { useAppDispatch } from "../../redux/hooks";
import { closeAlert } from "../../redux/features/alerts/alerts.slice";
import { CloseIcon } from "../../utils/theme-utils";
import { useAlertsSelector } from "../../redux/features/alerts/alerts.selector";

const backgroundColorMapping: any = {
  warning: "bg-warningAlert",
  error: "bg-errorAlert",
  success: "bg-successAlert",
};

const AlertMessage = (props: any) => {
  const alertInfo = useAlertsSelector();
  const dispatch = useAppDispatch();
  const { isRtl } = props;
  const handleClose = () => dispatch(closeAlert({}));

  useEffect(() => {
    if (alertInfo.open) {
      const timer = setTimeout(() => {
        dispatch(closeAlert({}));
      }, alertInfo.autoHideDuration ?? 5000);
      return () => clearTimeout(timer);
    }
  }, [alertInfo, dispatch]);

  return (
    <>
      <div
        data-testid="alert-wrapper"
        className={`fixed top-[70px] ${
          isRtl ? "left-1 lg:left-[2]" : "right-1 lg:right-[2]"
        } py-[22px] px-[18px] text-white rounded-[12px] shadow-lg ${
          backgroundColorMapping[alertInfo.severity ?? "success"]
        } ${alertInfo.open ? "" : "hidden"} z-10`}
      >
        {alertInfo.errorCode ||
        alertInfo.errorReason ||
        alertInfo.referenceId ? (
          <div className="flex items-center">
            <div>
              <header className="flex items-center justify-between mb-4">
                <h3 className="text-lg font-semibold">{alertInfo.title}</h3>
                <button
                  type="button"
                  onClick={handleClose}
                  className={`${isRtl ? "pr-4" : "pl-4"} cursor-pointer`}
                  aria-label="Close"
                >
                  <CloseIcon />
                </button>
              </header>
              <p id="alert-message">{alertInfo.message}</p>
              <div className="mt-4 py-4 px-4 rounded-[12px] bg-white bg-opacity-20">
                {alertInfo.errorCode && (
                  <p className="mt-1">
                    <span className="text-lg font-semibold">Error Code: </span>
                    {alertInfo.errorCode}
                  </p>
                )}
                {alertInfo.errorReason && (
                  <p className="mt-1">
                    <span className="text-lg font-semibold">Reason: </span>
                    {alertInfo.errorReason}
                  </p>
                )}
                {alertInfo.referenceId && (
                  <p className="mt-1">
                    <span className="text-lg font-semibold">
                      Reference ID:{" "}
                    </span>
                    {alertInfo.referenceId}
                  </p>
                )}
              </div>
            </div>
          </div>
        ) : (
          <div className="flex items-center">
            <p id="alert-message">{alertInfo.message}</p>
            <button
              type="button"
              onClick={handleClose}
              className={`${isRtl ? "pr-4" : "pl-4"} cursor-pointer`}
              aria-label="Close"
            >
              <CloseIcon />
            </button>
          </div>
        )}
      </div>
    </>
  );
};

export default AlertMessage;
