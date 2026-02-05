import { useEffect, useState, useMemo } from "react";
import { useTranslation } from "react-i18next";
import { useNavigate, useSearchParams } from "react-router-dom";
import relyingPartyService from "../services/relyingPartyService";
import clientDetails from "../constants/clientDetails";
import Loader from "../../../components/commons/Loader";

// Helper function to get claim details with verified status
const getClaimDetails = (userInfo, fieldName) => {
  let result = { value: null, verified: false };
  if (userInfo?.[fieldName]) {
    result.value = userInfo[fieldName];
  }
  if (userInfo?.verified_claims) {
    for (let verifiedClaim of userInfo.verified_claims) {
      if (verifiedClaim?.claims?.[fieldName]) {
        result.value = verifiedClaim.claims[fieldName];
        result.verified = true;
        break;
      }
    }
  }
  return result;
};

const AccountOpening = () => {
  const { t } = useTranslation("account_page");
  const navigate = useNavigate();
  const [searchParams, setSearchParams] = useSearchParams();
  const [successBanner, setSuccessBanner] = useState(true);
  const [rawUserInfo, setRawUserInfo] = useState(null);

  const { post_fetchUserInfo } = {
    ...relyingPartyService,
  };

  // Process userInfo to extract claims with verified status
  const userInfo = useMemo(() => {
    if (!rawUserInfo) return null;
    return {
      name: getClaimDetails(rawUserInfo, "name"),
      given_name: getClaimDetails(rawUserInfo, "given_name"),
      family_name: getClaimDetails(rawUserInfo, "family_name"),
      email: getClaimDetails(rawUserInfo, "email"),
      phone_number: getClaimDetails(rawUserInfo, "phone_number"),
    };
  }, [rawUserInfo]);

  useEffect(() => {
    setSuccessBanner(true);
  }, []);

  const handleCross = () => {
    setSuccessBanner(false);
  };

  const navigateToLogin = (errorCode, errorDescription) => {
    let params = "?";
    if (errorDescription) {
      params = params + "error_description=" + errorDescription + "&";
    }

    //REQUIRED
    params = params + "error=" + errorCode;

    navigate(process.env.PUBLIC_URL + "/" + params, { replace: true });
  };

  useEffect(() => {
    const getSearchParams = async () => {
      let authCode = searchParams.get("code");
      let errorCode = searchParams.get("error");
      let error_desc = searchParams.get("error_description");

      if (errorCode) {
        navigateToLogin(errorCode, error_desc);
        return;
      }

      if (authCode) {
        getUserDetails(authCode);
      } else {
        return;
      }
    };
    getSearchParams();
  }, []);

  const getUserDetails = async (authCode) => {
    try {
      let client_id = clientDetails.clientId;
      let redirect_uri = clientDetails.redirect_uri_userprofile;
      let grant_type = clientDetails.grant_type;

      var userInformation = await post_fetchUserInfo(
        authCode,
        client_id,
        redirect_uri,
        grant_type
      );
      setRawUserInfo(userInformation);
      localStorage.setItem(
        "userInfo",
        window.btoa(JSON.stringify(userInformation))
      );
    } catch (errormsg) {
      console.error(errormsg);
    }
  };

  const handleOpenAccount = () => {
    if (userInfo) {
      navigate("/application");
    }
  };

  const steps = [
    t("step_open_account"),
    t("step_verify"),
    t("step_fill_details"),
    t("step_complete"),
  ];

  return userInfo ? (
    <div>
      {successBanner && (
        <div className="bg-[#1F9F60] py-2 flex justify-between">
          <span className="text-white font-normal xl:pl-[10rem] pl-4">
            {t("success_message")}
          </span>
          <img
            src="assets/images/close_icon.svg"
            alt="close"
            className="xl:mr-[5rem] hover:cursor-pointer mr-4"
            onClick={handleCross}
          />
        </div>
      )}
      <div className="pt-2 lg:flex">
        <div className="relative lg:pr-[4rem] px-4 py-2">
          {window.screen.availWidth >= 1024 ? (
            <img
              src="assets/images/mask.svg"
              className="relative max-w-[105%]"
              alt="mask"
            />
          ) : (
            <img
              src="assets/images/large_mask.svg"
              className="relative w-full"
              alt="large_mask"
            />
          )}
          <img
            src="assets/images/bank.svg"
            alt="bank"
            className="absolute top-[7%] left-[15%] lg:top-[13%] lg:left-[3%] xl:w-auto lg:w-[550px] w-[70vw]"
          />
        </div>
        <div className="lg:px-[4rem] m-auto lg:py-0 pb-6 pt-2 px-4">
          <div className="text-[2.25rem] xl:w-[100%] m-auto xl:m-0 font-bold md:flex block">
            <span>{t("get_started")}</span>
            <p className="md:mx-4 break-words">{userInfo?.name?.value}</p>
          </div>
          <p className="my-3 font-semibold text-[1.75rem]">{t("subtext")}</p>
          <div className="my-4">
            {steps.map((item, index) => {
              return (
                <div className="flex my-2 text-sm font-semibold items-start">
                  <span className="ml-0 mr-1 border-2 border-[#7F56D9] rounded-[25px] w-5 text-center text-[#7F56D9] text-xs font-bold relative top-[1px]">
                    {index + 1}
                  </span>
                  <span className="ml-1 mr-3 items-center flex text-[#514A68]">
                    {item}
                  </span>
                </div>
              );
            })}
          </div>
          <div className="mt-6 mb-[1rem] text-[#5A7184] font-semibold">
            {t("open_account")}
          </div>
          <button
            type="button"
            className="bg-[#7F56D9] py-3 sm:w-80 w-full rounded-md text-white"
            onClick={handleOpenAccount}
          >
            {t("open_account_button")}
          </button>
        </div>
      </div>
    </div>
  ) : (
    <div className="align-loading-center">
      <Loader />
    </div>
  );
};

export default AccountOpening;
