import React, { useEffect, useState } from "react";
import { useAppDispatch, useAppSelector } from "../../../../redux/hooks";
import { claim } from "../../../../types/data-types";
import { RootState } from "../../../../redux/store";
import { useTranslation } from "react-i18next";
import { isRTL } from "../../../../utils/i18n";
import { isMobileDevice, getVerifiableClaims } from "../../../../utils/config";
import {
  getVpRequest,
  resetVpRequest,
  setFlowType,
  setSelectedCredentials,
  setShowWalletSelector,
} from "../../../../redux/features/verify/vpVerificationState";
import { storage } from "../../../../utils/storage";
import { Button } from "./Button";
import { FilterLinesIcon, SearchIcon } from "../../../../utils/theme-utils";
import { useVerifyFlowSelector } from "../../../../redux/features/verification/verification.selector";

function SelectionPanelContent() {
  const { t } = useTranslation("Verify");
  const [search, setSearch] = useState("");
  const [showMenu, setShowMenu] = useState(false);
  const [isAscending, setIsAscending] = useState(true);
  const dispatch = useAppDispatch();
  let language = useAppSelector((state: RootState) => state.common.language);
  language = language ?? window._env_.DEFAULT_LANG;
  const rtl = isRTL(language);
  const selectedCredentials = useVerifyFlowSelector((state) => state.selectedCredentials);
  const presentationDefinition = useVerifyFlowSelector((state) => state.presentationDefinition );
  const isMobile = isMobileDevice();

  const isCredentialSelected = (cred: claim) =>
    selectedCredentials.some((c: claim) => c.type === cred.type);

  const filteredClaims = getVerifiableClaims()
    .filter((claim) => claim.name.toLowerCase().includes(search.toLowerCase()))
    .sort((a, b) => {
      const aSelected = isCredentialSelected(a);
      const bSelected = isCredentialSelected(b);

      if (aSelected && !bSelected) return -1;
      if (!aSelected && bSelected) return 1;

      return isAscending
        ? a.name.localeCompare(b.name)
        : b.name.localeCompare(a.name);
    });

  const handleSearchChange = (event: React.ChangeEvent<HTMLInputElement>) => {
    setSearch(event.target.value);
  };

  const toggleClaimSelection = (cred: claim) => {
    if (isCredentialSelected(cred)) {
      dispatch(
        setSelectedCredentials({
          selectedCredentials: selectedCredentials.filter(
            (c: claim) => c.type !== cred.type
          ),
        })
      );
    } else {
      dispatch(
        setSelectedCredentials({ selectedCredentials: [...selectedCredentials, cred] })
      );
    }
  };

  const handleBack = () => {
    setSearch("");
    dispatch(resetVpRequest());
  };

  const handleGenerateQR = () => {
    setSearch("");
    dispatch(getVpRequest({ selectedCredentials }));
    const triggerElement = document.getElementById(
      "OpenID4VPVerification_trigger"
    );
    if (triggerElement) {
      const event = new MouseEvent("click", {
        bubbles: true,
        cancelable: true,
      });
      triggerElement.dispatchEvent(event);
    }
  };

  const handleOpenWallet = () => {
    dispatch(setFlowType());
  };

  const handleChooseWallet = () => {
    dispatch(setShowWalletSelector());
  };

  useEffect(() => {
    selectedCredentials.forEach((cred: claim) => {
      storage.setItem(storage.ESSENTIAL_CLAIM, cred);
    });
  }, [selectedCredentials]);

  return (
    <div className="fill-primary grid gap-6 p-3 rounded max-h-[80vh] overflow-y-auto">
      <div className="lg:block sm:text-left">
        <h1 className="font-bold text-smallTextSize lg:text-lg sm:text-xl text-selectorPanelTitle">
          {t("selectorTitle")}
        </h1>
        <p className="text-smallTextSize lg:text-sm text-selectorPanelSubTitle">
          {t("selectorSubTitle")}
        </p>
      </div>

      <div className="flex justify-around lg:gap-4 box-border">
        <div className="w-fit lg:w-[483px] flex items-center border-[2px] border-searchBorder rounded-lg p-2">
          <SearchIcon />
          <input
            type="text"
            className="ml-2 outline-none w-full text-smallTextSize lg:text-sm placeholder:text-[14px] lg:placeholder:text-sm"
            placeholder={t("searchPlaceholder")}
            onChange={handleSearchChange}
          />
        </div>
        <button
          type="button"
          onClick={() => setShowMenu(!showMenu)}
          className="w-[120px] relative flex items-center rounded-lg p-2 cursor-pointer border border-sortByBorder"
          aria-haspopup="menu"
          aria-expanded={showMenu}
        >
          <FilterLinesIcon />
          <span className="text-sortByText font-semibold text-smallTextSize ml-2">
            {t("sortBy")}
          </span>
          {showMenu && (
            <div
              role="menu"
              className={`absolute top-8 z-40 flex flex-col ${
                rtl ? "left-1 lg:left-0" : "right-px"
              } mt-3 rounded-md shadow-lg bg-background overflow-hidden font-normal border border-gray-200`}
            >
              <button
                role="menuitem"
                onClick={() => setIsAscending(true)}
                className="w-full h-[44px] text-sortByText text-left text-verySmallTextSize px-4 py-2 hover:bg-gray-100"
              >
                {t("sortAtoZ")}
              </button>
              <div className="w-full border-t-[2px] border-sortByBorder" />
              <button
                role="menuitem"
                onClick={() => setIsAscending(false)}
                className="w-full h-[44px] text-sortByText text-left text-verySmallTextSize px-4 py-2 hover:bg-gray-100"
              >
                {t("sortZtoA")}
              </button>
            </div>
          )}
        </button>
      </div>

      <div className="h-[210px] overflow-y-auto custom-scrollbar">
        <h2 className="text-smallTextSize lg:text-sm font-bold text-gray-700 mb-2">
          {t("listHeader")}
        </h2>
        {filteredClaims.length > 0 ? (
          <ul className="grid gap-4 max-h-[120px] lg:max-h-[250px] pr-4">
            {filteredClaims.map((claim, index) => {
              const isSelectedClaim = isCredentialSelected(claim);
              return (
                <li
                  id={`${claim.name}-ItemBox`}
                  key={index}
                  className={`grid mb-2 ${
                    isSelectedClaim
                      ? `bg-${window._env_.DEFAULT_THEME}-gradient bg-no-repeat rounded-[5px] p-px bg-opacity-95`
                      : ""
                  }`}
                >
                  <div
                    className={`flex items-center justify-between w-full p-2 lg:p-4 shadow rounded-md ${
                      isSelectedClaim ? `bg-red-50 bg-opacity-95` : ""
                    }`}
                  >
                    <div className="flex items-center gap-2">
                      <img
                        src={claim.logo}
                        alt={claim.type}
                        className="w-10 h-10 rounded"
                      />
                      <span data-testid={`ItemBox-Text-${index}`} className="truncate font-semibold text-smallTextSize lg:text-sm">
                        {claim.name}
                      </span>
                    </div>
                    <label
                      htmlFor={claim.name}
                      className="flex items-center cursor-pointer"
                    >
                      <input
                        type="checkbox"
                        id={claim.name}
                        name="claims"
                        className="hidden peer"
                        checked={isSelectedClaim}
                        onChange={() => toggleClaimSelection(claim)}
                      />
                      <div
                        className={`w-5 h-5 ${
                          isSelectedClaim
                            ? ` bg-${window._env_.DEFAULT_THEME}-gradient`
                            : "border-2 border-gray-300 bg-white"
                        } rounded-sm flex items-center justify-center p-px`}
                      >
                        <span
                          className={`${
                            isSelectedClaim ? "block" : "hidden"
                          } text-white font-bold`}
                        >
                          ✓
                        </span>
                      </div>
                      <span className="sr-only"> {claim.name} </span>
                    </label>
                  </div>
                </li>
              );
            })}
          </ul>
        ) : (
          <div className="text-gray-500 text-sm text-center py-4">
            {t("noVcsFound")}
          </div>
        )}
      </div>

      <div>
        <Button
          id="verification-generate-qr-code-button"
          title={t("generateQrCodeBtn")}
          onClick={handleGenerateQR}
          className="w-full text-smallTextSize lg:text-sm lg:mb-2"
          disabled={selectedCredentials.length <= 0}
          variant="fill"
        />
        {isMobile && (
          <Button
            id="verification-open-wallet-button"
            title={t("Common:Button.openWallet")}
            className="w-full text-smallTextSize lg:text-sm my-2"
            onClick={handleOpenWallet}
            disabled={presentationDefinition.input_descriptors.length === 0}
            variant="fill"
          />
        )}

        <Button
          id="verification-open-wallet-button"
          title={t("openWebWallets")}
          className="w-full text-smallTextSize lg:text-sm my-2"
          onClick={handleChooseWallet}
          disabled={presentationDefinition.input_descriptors.length === 0}
          variant="fill"
        />

        <Button
          id="selection-panel-back-button"
          className="w-full text-smallTextSize lg:text-sm"
          onClick={handleBack}
          title={t("goBack")}
          variant="clear"
        />
      </div>
    </div>
  );
}

export default SelectionPanelContent;
