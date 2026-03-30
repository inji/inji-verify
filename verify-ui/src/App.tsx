import React, { useEffect } from "react";
import "./App.css";
import Home from "./pages/Home";
import Offline from "./pages/Offline";
import { Scan } from "./pages/Scan";
import { RouterProvider, createBrowserRouter, redirect } from "react-router-dom";
import AlertMessage from "./components/commons/AlertMessage";
import PreloadImages from "./components/commons/PreloadImages";
import PageNotFound404 from "./pages/PageNotFound404";
import { Pages } from "./utils/config";
import { useAppSelector } from "./redux/hooks";
import store, { RootState } from "./redux/store";
import { isRTL } from "./utils/i18n";
import { VerificationMethod } from "./types/data-types";
import { goToHomeScreen } from "./redux/features/verification/verification.slice";
import { Verify } from "./pages/Verify";
import PageTemplate from "./components/PageTemplate";

const methodToPath: Record<VerificationMethod, string> = {
  UPLOAD: Pages.Home,
  SCAN: Pages.Scan,
  VERIFY: Pages.VerifyCredentials,
};

function switchToVerificationMethod(method: VerificationMethod) {
  if (!localStorage.getItem("path")) {
    localStorage.setItem("path", "/");
  }
  localStorage.setItem("path", methodToPath[method]);
  store.dispatch(goToHomeScreen({ method }));
  return null;
}

// True only for the very first load of the Home route within this page session.
// Subsequent navigations to "/" (e.g. clicking the Upload tab) must NOT redirect.
let initialHomeLoad = true;

const router = createBrowserRouter([
  {
    path: "/",
    element: <PageTemplate />,
    children: [
      {
        path: Pages.Home,
        element: <Home />,
        loader: () => {
          if (initialHomeLoad) {
            initialHomeLoad = false;
            const searchParams = new URLSearchParams(window.location.search);
            const hashParams = new URLSearchParams(
              window.location.hash.startsWith("#")
                ? window.location.hash.slice(1)
                : window.location.hash
            );
            if (
              searchParams.has("response_code") ||
              hashParams.has("response_code")
            ) {
              const storedPath = localStorage.getItem("path");
              if (storedPath && storedPath !== "/") {
                return redirect(
                  `${storedPath}${window.location.search}${window.location.hash}`
                );
              }
            }
          }
          return switchToVerificationMethod("UPLOAD");
        },
      },
      {
        path: Pages.Scan,
        element: <Scan/>,
        loader: () => switchToVerificationMethod("SCAN"),
      },
      {
        path: Pages.VerifyCredentials,
        element: <Verify/>,
        loader: () => switchToVerificationMethod("VERIFY"),
      },
      {
        path: Pages.Offline,
        element: <Offline/>,
      },
      {
        path: Pages.PageNotFound,
        element: <PageNotFound404/>,
      },
    ]
  }
]);

function App() {
  const language = useAppSelector((state: RootState) => state.common.language);
  const rtl = isRTL(language);
  const preloadImages = ['/assets/images/under_construction.svg', '/assets/images/inji-logo.svg'];

  useEffect(() => {
    document.body.classList.toggle('rtl', rtl);
    document.documentElement.classList.add('default_theme');
  }, [rtl]);

  return (
    <div className="font-base">
      <RouterProvider router={router}/>
      <AlertMessage isRtl={rtl}/>
      <PreloadImages imageUrls={preloadImages}/>
    </div>
  );
}

export default App;