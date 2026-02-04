import React, { useEffect, useState } from "react";
import "./App.css";
import Home from "./pages/Home";
import Offline from "./pages/Offline";
import { RouterProvider, createBrowserRouter } from "react-router-dom";
import AlertMessage from "./components/commons/AlertMessage";

import PreloadImages from "./components/commons/PreloadImages";
import OvpRedirect from "./pages/OvpRedirect";
import PageNotFound404 from "./pages/PageNotFound404";
import { Pages } from "./utils/config";
import LandingPage from "./portals/banking/pages/LandingPage";
import langConfigService from "./portals/banking/services/langConfigService";
import AccountOpening from "./portals/banking/pages/AccountOpening";
import Application from "./portals/banking/pages/Application";
import NavHeader from "./portals/banking/components/NavHeader";
import RedirectOnReload from "./portals/banking/components/RedirectOnReload";

const preloadImages = [
  "/assets/images/under_construction.svg",
  "/assets/images/inji-logo.svg",
];

function App() {
  const [langOptions, setLangOptions] = useState([]);
  var navLinks: any = ["home", "help"];

  //Loading rtlLangs
  useEffect(() => {
    try {
      langConfigService.getLocaleConfiguration().then((response) => {
        let lookup: any = {};
        let supportedLanguages: any = response.languages;
        let langData: any = [];
        for (let lang in supportedLanguages) {
          //check to avoid duplication language labels
          if (!(supportedLanguages[lang] in lookup)) {
            lookup[supportedLanguages[lang]] = 1;
            langData.push({
              label: supportedLanguages[lang],
              value: lang,
            });
          }
        }
        setLangOptions(langData);
      });
    } catch (error) {
      console.error("Failed to load rtl languages!");
    }
  }, []);

  useEffect(() => {
    if (window.location.pathname === "/") {
      localStorage.removeItem("ui_locales");
    }
  }, []);

  const router = createBrowserRouter([
    {
      path: Pages.Home,
      element: <Home />,
    },
    {
      path: Pages.Redirect,
      element: <OvpRedirect />,
    },
    {
      path: Pages.Offline,
      element: <Offline />,
    },
    {
      path: Pages.PageNotFound,
      element: <PageNotFound404 />,
    },
    {
      path: Pages.LandingPage,
      element: <LandingPage />,
    },
    {
      path: Pages.AccountOpeningPage,
      element: <AccountOpening />,
    },
    {
      path: Pages.Application,
      element: <Application />,
    },
  ]);

  const userInfo = localStorage.getItem("userInfo");

  return (
    <div className="font-base">
      <RedirectOnReload />
      {!(
        !userInfo &&
        (window.location.pathname === "/verification" ||
          window.location.pathname === "/application")
      ) && (
        <>
          <NavHeader langOptions={langOptions} navLinks={navLinks} />
          <RouterProvider router={router} />
          <AlertMessage />
          <PreloadImages imageUrls={preloadImages} />
        </>
      )}
    </div>
  );
}

export default App;
