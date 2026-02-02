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
import { goToHomeScreen, selectMethod } from "./redux/features/verification/verification.slice";
import { Verify } from "./pages/Verify";
import PageTemplate from "./components/PageTemplate";
import { verificationInit } from "./redux/features/verification/verification.slice";

function switchToVerificationMethod(method: VerificationMethod) {
  const sessionStoragePath = sessionStorage.getItem('pathName');
  let methodPath = "";
  switch (method) {
    case "UPLOAD":
      methodPath = Pages.Home;
      break;
    case "SCAN":
      methodPath = Pages.Scan;
      break;
    case "VERIFY":
      methodPath = Pages.VerifyCredentials;
      break;
    default:
      methodPath = "";
  }
  if (sessionStoragePath && sessionStoragePath !== methodPath) {
    sessionStorage.removeItem("pathName");
    sessionStorage.removeItem("transactionId");
    sessionStorage.removeItem("requestId");
  }
  if (sessionStoragePath?.includes(Pages.Scan) && method === "SCAN") {
    store.dispatch(selectMethod({ method: "SCAN" }));
    store.dispatch(
      verificationInit({
        qrReadResult: {status: "READ"},
        ovp: {},
      })
    );
    return null;
  }
  store.dispatch(goToHomeScreen({ method }));
  return null;
}

const router = createBrowserRouter([
  {
    path: "/",
    element: <PageTemplate />,
    loader: ({ request }) => {
      const url = new URL(request.url);
      const sessionStoragePath = sessionStorage.getItem('pathName');
      if (url.pathname === Pages.Home && sessionStoragePath?.includes(Pages.Scan)) return redirect(Pages.Scan);
      return null;
    },
    children: [
      {
        path: Pages.Home,
        element: <Home/>,
        loader: () => switchToVerificationMethod("UPLOAD"),
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
