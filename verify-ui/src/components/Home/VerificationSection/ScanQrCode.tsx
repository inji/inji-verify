import React, { useState } from 'react';
import { GradientScanIcon, QrIcon, WhiteScanIcon } from '../../../utils/theme-utils';
import { Button } from "./commons/Button";
import {useAppDispatch} from "../../../redux/hooks";
import {qrReadInit} from "../../../redux/features/verification/verification.slice";
import {checkInternetStatus} from "../../../utils/misc";
import {updateInternetConnectionStatus} from "../../../redux/features/application-state/application-state.slice";
import { ScanOutline } from '../../../utils/theme-utils';
import { useTranslation } from 'react-i18next';

const Scan = () => {
  const dispatch = useAppDispatch();
  const [isHover, setHover] = useState(false);
  const ScanIcon = isHover ? WhiteScanIcon : GradientScanIcon;
  const {t} = useTranslation();

  return (
    <>
      <div
        className={`bg-${window._env_.DEFAULT_THEME}-gradient p-[2px] rounded-full w-[180px] absolute top-[160px] left-1/2 -translate-x-1/2 lg:top-[231px]`}
      >
        <Button
          id="scan-button"
          title={t("Common:Button.scan")}
          icon={<ScanIcon />}
          variant="outline"
          onMouseEnter={() => setHover(true)}
          onMouseLeave={() => setHover(false)}
          className={`group bg-white hover:bg-${window._env_.DEFAULT_THEME}-gradient font-bold h-[40px] rounded-full flex items-center justify-center text-lgNormalTextSize cursor-pointer w-full`}
          style={{ borderRadius: 9999 }}
          onClick={async () => {
            dispatch(
              updateInternetConnectionStatus({
                internetConnectionStatus: "LOADING",
              })
            );
            let isOnline = await checkInternetStatus();
            dispatch(
              updateInternetConnectionStatus({
                internetConnectionStatus: isOnline ? "ONLINE" : "OFFLINE",
              })
            );
            if (isOnline) {
              document.getElementById("trigger-scan")?.click();
            }
          }}
        />
      </div>

      <button
        id="trigger-scan"
        className="hidden"
        onClick={() => {
          dispatch(qrReadInit({ method: "SCAN" }));
        }}
      />
    </>
  );
};

export const ScanQrCode = () => {
  return (
    <div className="flex flex-col pt-0 pb-[100px] lg:py-[42px] px-0 lg:px-[104px] text-center content-center justify-center">
      <div className="xs:col-end-13">
        <div
          className={`relative grid content-center justify-center w-[275px] h-auto lg:w-[360px] aspect-square my-1.5 mx-auto bg-cover`}
          style={{ backgroundImage: `url(${ScanOutline})` }}
        >
          <div className={`grid bg-${window._env_.DEFAULT_THEME}-lighter-gradient rounded-[12px] w-[250px] lg:w-[320px] aspect-square content-center justify-center`}></div>
          <div className="absolute top-[58px] left-[98px] lg:top-[165px] lg:left-[50%] lg:translate-x-[-50%] lg:translate-y-[-50%]">
            <QrIcon className="w-[78px] lg:w-[100px]" />
          </div>
          <Scan />
        </div>
      </div>
    </div>
  );
};
