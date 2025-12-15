import React, { useEffect } from "react";
import "./App.css";
import Home from "./pages/Home";
import Offline from "./pages/Offline";
import { Scan } from "./pages/Scan";
import { RouterProvider, createBrowserRouter } from "react-router-dom";
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

// ✅ Import new standalone pages
import TruckPassLogin from "./pages/Login";
import OtpPage from "./pages/OtpPage";

function switchToVerificationMethod(method: VerificationMethod) {
  store.dispatch(goToHomeScreen({ method }));
  return null;
}

// ✅ Updated router: "/" is Login, app pages still use existing URLs (Pages.*)
const router = createBrowserRouter([
  // Root → Login
  {
    path: "/",
    element: <TruckPassLogin />,
  },

  // Explicit login route (alias, if someone hits /login directly)
  {
    path: "/login",
    element: <TruckPassLogin />,
  },

  // OTP page
  {
    path: "/otp",
    element: <OtpPage />,
  },

  // Layout route with PageTemplate wrapping all app pages
  // No `path` here → children control their own URLs via Pages.*
  {
    element: <PageTemplate />,
    children: [
      {
        path: Pages.Home,
        element: <Home />,
        loader: () => switchToVerificationMethod("UPLOAD"),
      },
      {
        path: Pages.Scan,
        element: <Scan />,
        loader: () => switchToVerificationMethod("SCAN"),
      },
      {
        path: Pages.VerifyCredentials,
        element: <Verify />,
        loader: () => switchToVerificationMethod("VERIFY"),
      },
      {
        path: Pages.Offline,
        element: <Offline />,
      },
      {
        path: Pages.PageNotFound,
        element: <PageNotFound404 />,
      },
    ],
  },
]);

function App() {
  const language = useAppSelector((state: RootState) => state.common.language);
  const rtl = isRTL(language);
  const preloadImages = [
    "/assets/images/under_construction.svg",
    "/assets/images/inji-logo.svg",
  ];

  useEffect(() => {
    document.body.classList.toggle("rtl", rtl);
    document.documentElement.classList.add("default_theme");
  }, [rtl]);

  return (
    <div className="font-base">
      <RouterProvider router={router} />
      <AlertMessage isRtl={rtl} />
      <PreloadImages imageUrls={preloadImages} />
    </div>
  );
}

export default App;
