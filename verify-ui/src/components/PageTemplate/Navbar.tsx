import React, { useCallback, useEffect, useState } from 'react';
import { MdArrowForwardIos } from "react-icons/md";
import { MdExpandLess } from "react-icons/md";
import { Pages } from "../../utils/config";
import { LanguageSelector } from '../commons/LanguageSelector';
import { useTranslation } from 'react-i18next';
import { useAppSelector } from '../../redux/hooks';
import { RootState } from '../../redux/store';
import { isRTL } from '../../utils/i18n';
import { Logo, MenuIcon, NewTabIcon } from '../../utils/theme-utils';

// 🚚 TruckPass truck logo (truckpassTheme asset)
import truckpassTruckLogo from "../../assets/truckpassTheme/truckpassTruckLogo.svg";

const SubMenu = () => {
  const { t } = useTranslation("Navbar");

  return (
    <div
      id="help-submenu"
      className="absolute top-[36px] left-[-12px] mt-2 w-[100vw] lg:w-[250px] lg:top-[24px] lg:left-[-190px] bg-white rounded-md py-1 ring-1 ring-black ring-opacity-5 lg:py-5 lg:shadow-lg z-60"
    >
      <a
        id="contact-us"
        href="https://community.mosip.io/"
        target="_blank"
        rel="noreferrer"
        className="inline-flex items-center w-full px-[26px] py-2 text-sm lg:px-4"
      >
        {t("contactUs")} <NewTabIcon className="mx-1.5" />
      </a>
      <a
        id="documentation"
        href="https://docs.mosip.io/inji/inji-verify/overview"
        target="_blank"
        rel="noreferrer"
        className="inline-flex items-center w-full px-[26px] py-2 text-sm lg:px-4"
      >
        {t("documentation")} <NewTabIcon className="mx-1.5" />
      </a>
      <button
        id="faq"
        disabled
        className="inline-flex items-center w-full px-[26px] py-2 text-sm lg:px-4"
      >
        {t("faqs")}
      </button>
    </div>
  );
};

const MobileDropDownMenu = ({
  showMenu,
  setShowMenu,
}: {
  showMenu: boolean;
  setShowMenu: React.Dispatch<React.SetStateAction<boolean>>;
}) => {
  const [showSubMenu, setShowSubMenu] = useState(false);
  const { t } = useTranslation("Navbar");

  const handleClickOutside = useCallback(
    (event: MouseEvent) => {
      const target = event.target as HTMLElement;
      if (
        showMenu &&
        target.closest("#menu") === null &&
        target.closest("#hamburger") === null
      ) {
        setShowMenu(false);
        setShowSubMenu(false);
      }
    },
    [showMenu, setShowMenu]
  );

  useEffect(() => {
    document.addEventListener("click", handleClickOutside);
    return () => document.removeEventListener("click", handleClickOutside);
  }, [handleClickOutside]);

  return (
    <div className="lg:hidden">
      {showMenu && (
        <div
          id="menu"
          className="absolute right-0 top-[68px] w-[100vw] bg-white rounded-md shadow-lg p-3 ring-1 ring-black ring-opacity-5 font-bold text-[14px] z-[1000]"
        >
          <a
            id="home-button"
            href={Pages.Home}
            className="block px-1 py-2 text-sm text-gray-700 hover:bg-gray-100"
          >
            {t("home")}
          </a>
          <a
            id="verify-credentials-button"
            href={Pages.Home}
            className={`block px-1 py-2 font-bold text-sm bg-${window._env_.DEFAULT_THEME}-gradient bg-clip-text text-transparent`}
          >
            {t("verifyCredentials")}
          </a>
          <div className="relative">
            <button
              id="submenu-button"
              className="inline-flex items-center w-full text-left px-1 py-3 text-sm text-gray-700 hover:bg-gray-100"
              onClick={() => setShowSubMenu((show) => !show)}
            >
              {t("help")}{" "}
              <MdArrowForwardIos
                className={`mx-1.5 ${showSubMenu ? "rotate-90" : ""}`}
              />
            </button>
            {showSubMenu && <SubMenu />}
          </div>
        </div>
      )}
    </div>
  );
};

const DesktopMenu = () => {
  const [showHelp, setShowHelp] = useState(false);
  const { t } = useTranslation("Navbar");
  const language = useAppSelector((state: RootState) => state.common.language);
  const rtl = isRTL(language);
  return (
    <div className="hidden lg:block w-full lg:w-auto" id="navbar-default">
      <ul
        className={`hidden mt-4 lg:flex ${
          rtl ? "lg:space-x-reverse lg:space-x-10" : "lg:space-x-10"
        } lg:mt-0 lg:text-sm lg:font-medium`}
      >
        <li>
          <a
            id="home-button"
            href={Pages.Home}
            className="block py-2 rounded text-black"
            aria-current="page"
          >
            {t("home")}
          </a>
        </li>
        <li>
          <a
            id="verify-credentials-button"
            href={Pages.VerifyCredentials}
            className={`block py-2 font-bold rounded bg-${window._env_.DEFAULT_THEME}-gradient bg-clip-text text-transparent`}
          >
            {t("verifyCredentials")}
          </a>
        </li>
        <li className="relative">
          <button
            id="help-button"
            onClick={() => setShowHelp((show) => !show)}
            className="inline-flex items-center cursor-pointer py-2 rounded text-black"
          >
            {t("help")}{" "}
            <MdExpandLess
              className={`mx-1.5 ${showHelp ? "" : "rotate-180"}`}
            />
          </button>
          {showHelp && <SubMenu />}
        </li>
      </ul>
    </div>
  );
};

function Navbar(props: any) {
  const [showMenu, setShowMenu] = useState(false);

  // Only show the truck icon when the active theme is truckpass
  const isTruckpass =
    (window as any)._env_?.DEFAULT_THEME === "truckpass_theme";

  return (
    <nav className="bg-background border-gray-200 xs:px-4 lg:px-20 py-3.5 rounded drop-shadow-md z-10 relative">
      <div className="container flex flex-wrap justify-between items-center h-[40px] mx-0">
        <div className="flex items-center">
          <button
            data-collapse-toggle="navbar-default"
            type="button"
            className={`${
              showMenu
                ? `bg-${window._env_.DEFAULT_THEME}-lighter-gradient`
                : "bg-background"
            } p-3 ml-2 mr-1 lg:hidden`}
            aria-controls="navbar-default"
            aria-expanded="false"
            id="hamburger"
            onClick={() => setShowMenu(!showMenu)}
          >
            <MenuIcon id="menu-icon" style={{ width: "25px", height: "19px" }} />
          </button>

          <a href={Pages.Home} className="flex items-center gap-2">
            {isTruckpass && (
              <img
                src={truckpassTruckLogo}
                alt="TruckPass Icon"
                className="h-[20px] w-auto"
              />
            )}
            <Logo className="w-[150px] sm:w-[184px] h-[32px] cursor-pointer" />
          </a>
        </div>

        <DesktopMenu />
        <MobileDropDownMenu showMenu={showMenu} setShowMenu={setShowMenu} />

        <div id="language-selector" className="relative">
          <LanguageSelector />
        </div>
      </div>
    </nav>
  );
}

export default Navbar;
