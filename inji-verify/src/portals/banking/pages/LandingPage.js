import { useEffect, useState } from "react";
import { useTranslation } from "react-i18next";
import clientDetails from "../constants/clientDetails";
import clientDetailsForeigner from "../constants/clientDetailsForeigner";
import { useExternalScript } from "../hooks/useExternalScript";
import { Error } from "../commons/Errors";
import { useSearchParams } from "react-router-dom";
import Loader from "../../../components/commons/Loader";

const LandingPage = () => {
  const { i18n, t } = useTranslation("landing_page");
  const signInButtonScript = window._env_.SIGN_IN_BUTTON_PLUGIN_URL;
  const state = useExternalScript(signInButtonScript);
  const [searchParams, setSearchParams] = useSearchParams();
  const [error, setError] = useState(null);
  
  // Feature flag to control foreigner sign-in button visibility
  const isForeignerSignInEnabled = window._env_.ENABLE_FOREIGNER_SIGNIN === "true";

  useEffect(() => {
    const getSearchParams = async () => {
      let errorCode = searchParams.get("error");
      let error_desc = searchParams.get("error_description");

      if (errorCode) {
        setError({
          errorCode: errorCode,
          errorMsg: error_desc,
          showToast: true,
        });
      }
    };
    getSearchParams();

    renderSignInButton();

    i18n.on("languageChanged", function (lng) {
      renderSignInButton();
    });
  }, [state]);

  const renderSignInButton = () => {
    // OIDC configuration for National ID button
    const oidcConfig = {
      authorizeUri: clientDetails.uibaseUrl + clientDetails.authorizeEndpoint,
      redirect_uri: clientDetails.redirect_uri_userprofile,
      client_id: clientDetails.clientId,
      scope: clientDetails.scopeUserProfile,
      nonce: clientDetails.nonce,
      state: clientDetails.state,
      acr_values: clientDetails.acr_values,
      claims_locales: clientDetails.claims_locales,
      display: clientDetails.display,
      prompt: clientDetails.prompt,
      max_age: clientDetails.max_age,
      ui_locales: i18n.language,
      claims: JSON.parse(decodeURIComponent(clientDetails.userProfileClaims)),
    };

    // OIDC configuration for Foreigner button
    const oidcConfigForeigner = {
      authorizeUri: clientDetailsForeigner.uibaseUrl + clientDetailsForeigner.authorizeEndpoint,
      redirect_uri: clientDetailsForeigner.redirect_uri_userprofile,
      client_id: clientDetailsForeigner.clientId,
      scope: clientDetailsForeigner.scopeUserProfile,
      nonce: clientDetailsForeigner.nonce,
      state: clientDetailsForeigner.state,
      acr_values: clientDetailsForeigner.acr_values,
      claims_locales: clientDetailsForeigner.claims_locales,
      display: clientDetailsForeigner.display,
      prompt: clientDetailsForeigner.prompt,
      max_age: clientDetailsForeigner.max_age,
      ui_locales: i18n.language,
      claims: JSON.parse(decodeURIComponent(clientDetailsForeigner.userProfileClaims)),
    };

    // Initialize National ID button
    window.SignInWithEsignetButton?.init({
      oidcConfig: oidcConfig,
      buttonConfig: {
        shape: "soft_edges",
        labelText: t("sign_in_with_esignet"),
        width: "100%",
      },
      signInElement: document.getElementById("sign-in-with-esignet"),
    });

    // Initialize Foreigner button with separate configuration (if enabled)
    if (isForeignerSignInEnabled) {
      window.SignInWithEsignetButton?.init({
        oidcConfig: oidcConfigForeigner,
        buttonConfig: {
          shape: "soft_edges",
          labelText: t("sign_in_as_foreigner"),
          width: "100%",
        },
        signInElement: document.getElementById("sign-in-as-foreigner"),
      });
    }
  };

  localStorage.removeItem("userInfo");

  return (
    <div>
      {document?.getElementById("selectedLang")?.textContent ? (
        <div>
          {error && (
            <Error
              errorCode={error.errorCode}
              errorMsg={error.errorMsg}
              showToast={error.showToast}
            />
          )}
          <div className="pt-2 lg:flex font-semibold">
            <div className="relative lg:pr-[4rem] px-4 py-2">
              {window.screen.availWidth >= 1024 ? (
                <img
                  src="assets/images/mask.svg"
                  className="relative"
                  alt="mask"
                />
              ) : (
                <img
                  src="assets/images/full_mask.svg"
                  className="relative w-full"
                  alt="full_mask"
                />
              )}
              <img
                src="assets/images/bank.svg"
                alt="bank"
                className="absolute top-[9%] left-[25%] lg:top-[13%] lg:left-[3%] xl:w-auto lg:w-[550px] w-[48vw]"
              />
            </div>
            <div className="xl:pl-[4rem] xl:pt-[2rem] lg:pt-0 px-4 lg:m-0 m-auto pt-4">
              <p className="font-bold text-[36px] ">
                {t("sign_in_to_banking_portal")}
              </p>
              <div className="lg:w-full lg:pt-[1rem] lg:m-0 m-auto">
                <form className="my-6">
                  <label className="my-3 block text-[#5A7184]">
                    {t("username")}
                  </label>
                  <input
                    type="text"
                    className="input-box py-2 px-4 w-full mb-6"
                    placeholder={t("username_placeholder")}
                  />
                  <label className="mb-3 block mt-6 text-[#5A7184]">
                    {t("password")}
                  </label>
                  <input
                    type="text"
                    className="input-box py-2 px-4 w-full"
                    placeholder={t("password_placeholder")}
                  />
                  <div className="flex justify-between my-[1.75rem] text-sm">
                    <div>
                      <input
                        type="checkbox"
                        id="signin"
                        className="text-md scale-125 relative top-[1.5px] mr-1 hover:cursor-pointer"
                      />
                      <label for="signin" className="mx-0 text-[#5A7184]">
                        {" "}
                        {t("keep_me_sign_in")}{" "}
                      </label>
                    </div>
                    <a href="" className="text-[#7F56D9]">
                      {t("forgot_password")}
                    </a>
                  </div>
                  <button
                    type="submit"
                    className="bg-[#7F56D9] text-white w-full py-3 rounded-md mb-3 font-semibold"
                  >
                    {t("submit")}
                  </button>
                  <div className="flex my-4">
                    <img
                      src="assets/images/line.svg"
                      className="sm:w-[50%] w-[45%]"
                      alt="line"
                    />
                    <span className="mx-1">{t("or")}</span>
                    <img
                      src="assets/images/line.svg"
                      className="sm:w-[50%] w-[45%]"
                      alt="line"
                    />
                  </div>
                </form>
                <div>
                  {state === "ready" && (
                    <>
                      <div id="sign-in-with-esignet" className="w-full mb-3"></div>
                      {isForeignerSignInEnabled && (
                        <div id="sign-in-as-foreigner" className="w-full"></div>
                      )}
                    </>
                  )}
                </div>
                <p className="text-sm my-[1.75rem]">
                  <span className="text-[#5A7184] mr-2">{t("no_account")}</span>
                  <a href="" className="text-[#7F56D9]">
                    {t("sign_up")}
                  </a>
                </p>
              </div>
            </div>
          </div>
        </div>
      ) : (
        <div className="align-loading-center">
          <Loader />
        </div>
      )}
    </div>
  );
};

export default LandingPage;
