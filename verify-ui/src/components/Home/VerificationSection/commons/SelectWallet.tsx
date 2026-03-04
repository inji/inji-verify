import React, { useState } from "react";
import MobileStepper from "../../VerificationProgressTracker/MobileStepper";
import { useAppDispatch } from "../../../../redux/hooks";
import { useVerifyFlowSelector } from "../../../../redux/features/verification/verification.selector";
import {
  resetVpRequest,
  setFlowType,
  setSelectedWallet,
  getVpRequest,
} from "../../../../redux/features/verify/vpVerificationState";
import { useTranslation } from "react-i18next";
import { getWebWallets, isMobileDevice } from "../../../../utils/config";
import { Button } from "./Button";
import { SearchIcon } from "../../../../utils/theme-utils";
import { WebWallet } from "../../../../types/data-types";

const DesktopModal: React.FC<{ children: React.ReactNode }> = ({
  children,
}) => (
  <div className="fixed z-50 inset-0 flex items-center justify-center">
    <div className="absolute inset-0 bg-black opacity-50"></div>
    <div className="relative bg-white max-w-[80vw] p-6 rounded-lg shadow-xl">
      {children}
    </div>
  </div>
);

const SlideModal: React.FC<{ children: React.ReactNode }> = ({ children }) => (
  <div className="fixed inset-0 z-50 overflow-hidden">
    <div className="absolute inset-0 bg-black opacity-50"></div>
    <div className="absolute bottom-0 w-full h-[60vh]">
      <div className="slide-up-container">{children}</div>
    </div>
  </div>
);

const SelectWalletContent: React.FC = () => {
  const { t } = useTranslation("Verify");
  const dispatch = useAppDispatch();
  const webWallets = getWebWallets();
  const [search, setSearch] = useState("");
  const [selectedWalletId, setSelectedWalletId] = useState<string | null>(null);
  const presentationDefinition = useVerifyFlowSelector(
    (state) => state.presentationDefinition,
  );
  const selectedCredentials = useVerifyFlowSelector((state) => state.selectedCredentials);

  const handleSearchChange = (event: React.ChangeEvent<HTMLInputElement>) => {
    setSearch(event.target.value);
  };

  const filteredWallets: WebWallet[] = webWallets.filter((wallet) =>
    wallet.name.toLowerCase().includes(search.toLowerCase()),
  );

  const handleCancel = () => {
    dispatch(resetVpRequest());
  };

  const handleProceed = () => {
    if (!selectedWalletId) {
      return;
    }

    const selectedWallet = webWallets.find(
      (wallet) => wallet.id === selectedWalletId,
    );
    if (!selectedWallet) {
      return;
    }

    dispatch(
      setSelectedWallet({
        walletId: selectedWallet.id,
        walletBaseUrl: selectedWallet.walletBaseUrl,
      }),
    );

    // Switch to same-device flow and kick off VP request so the SDK can redirect
    dispatch(setFlowType());
    dispatch(getVpRequest({ selectedCredentials }));
  };

  return (
    <div className="fill-primary grid gap-6 p-3 rounded max-h-[80vh] overflow-y-auto">
      <div className="lg:block sm:text-left">
        <h1 className="font-bold text-smallTextSize lg:text-lg sm:text-xl text-selectorPanelTitle">
          {t("walletSelectorTitle")}
        </h1>
        <p className="text-smallTextSize lg:text-sm text-selectorPanelSubTitle">
          {t("walletSelectorDescription")}
        </p>
      </div>

      <div className="flex justify-around lg:gap-4 box-border">
        <div className="w-fit lg:w-[483px] flex items-center border-[2px] border-searchBorder rounded-lg p-2">
          <SearchIcon />
          <input
            type="text"
            className="ml-2 outline-none w-full text-smallTextSize lg:text-sm placeholder:text-[14px] lg:placeholder:text-sm"
            placeholder={t("walletSearchPlaceholder")}
            value={search}
            onChange={handleSearchChange}
          />
        </div>
      </div>
      <hr className="border-t border-gray-200 -mx-3" />


      <div className="grid grid-cols-2 lg:grid-cols-3 gap-4 mt-2">
        {filteredWallets.length === 0 && search && (
          <p className="col-span-2 lg:col-span-3 text-center text-sm text-gray-400 py-6">
            {t("walletSearchNoResults", { search })}
          </p>
        )}
        {filteredWallets.map((wallet) => {
          const isSelected = wallet.id === selectedWalletId;
          return (
            <button
              key={wallet.id}
              type="button"
              onClick={() => setSelectedWalletId(wallet.id)}
              className={`grid rounded-2xl transition-shadow duration-150 ${isSelected
                  ? `bg-${window._env_.DEFAULT_THEME || "primary"}-lighter-gradient bg-no-repeat p-[2px] shadow-md`
                  : "border border-gray-200 p-[2px] bg-white shadow-sm"
                }`}
            >
              <div
                className={`flex flex-col items-center justify-between rounded-xl py-4 px-4 relative ${isSelected ? "" : "bg-white"
                  }`}
              >
                {isSelected && (
                  <div
                    className={`absolute inset-0 rounded-xl bg-${window._env_.DEFAULT_THEME || "primary"}-lighter-gradient opacity-25`}
                  />
                )}
                <div className="relative z-10 flex flex-col items-center justify-between w-full">
                  <div className="flex items-center justify-center w-16 h-16 rounded-lg bg-white border border-gray-200 mb-3">
                    <img
                      src={wallet.iconUrl}
                      alt={wallet.name}
                      onError={(e) => {
                        (e.currentTarget as HTMLImageElement).onerror = null;
                        (e.currentTarget as HTMLImageElement).src = "/icons/default-wallet.svg";
                      }}
                      className="w-10 h-10 object-contain"
                    />
                  </div>
                  <span className="text-smallTextSize lg:text-sm text-gray-800 text-center truncate w-full">
                    {wallet.name}
                  </span>
                </div>
              </div>
            </button>
          );
        })}
      </div>

      <hr className="border-t border-gray-200 -mx-3" />
      <div className="flex justify-between gap-4 mt-4">
        <Button
          id="wallet-selector-cancel-button"
          title={t("walletSelectorCancel")}
          className="w-1/2 text-smallTextSize lg:text-sm"
          onClick={handleCancel}
          variant="outline"
        />
        <Button
          id="wallet-selector-proceed-button"
          title={t("walletSelectorProceed")}
          className="w-1/2 text-smallTextSize lg:text-sm"
          onClick={handleProceed}
          disabled={
            !selectedWalletId ||
            (presentationDefinition?.input_descriptors?.length ?? 0) === 0
          }
          variant="fill"
        />
      </div>
    </div>
  );
};

const SelectWallet: React.FC = () => {
  const dispatch = useAppDispatch();

  const handleBack = () => {
    dispatch(resetVpRequest());
  };

  return (
    <div>
      {isMobileDevice() && (
        <div className="block">
          <SlideModal>
            <div className="flex justify-center">
              <button
                type="button"
                onClick={handleBack}
                className="w-10 h-1 rounded-full my-2 cursor-pointer bg-sortByBorder"
                aria-label="Go back"
              />
            </div>
            <div className="pt-2">
              <MobileStepper />
            </div>
            <SelectWalletContent />
          </SlideModal>
        </div>
      )}
      {!isMobileDevice() && (
        <div className="lg:block">
          <DesktopModal>
            <SelectWalletContent />
          </DesktopModal>
        </div>
      )}
    </div>
  );
};

export default SelectWallet;
