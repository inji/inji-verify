import React, { useMemo } from "react";
import { useTranslation } from "react-i18next";
import { useNavigate } from "react-router-dom";

// Generate a random masked account number
const generateAccountNumber = () => {
  const lastFour = Math.floor(1000 + Math.random() * 9000);
  return `XXXX-XXXX-${lastFour}`;
};

const Result = ({ accountData }) => {
  const { t } = useTranslation("account_result");
  const { t: tForm } = useTranslation("form");
  const navigate = useNavigate();

  // Generate account number once and memoize it
  const accountNumber = useMemo(() => generateAccountNumber(), []);

  // Format transaction limit with currency
  const formattedLimit = accountData?.transactionLimit 
    ? `$${parseFloat(accountData.transactionLimit).toLocaleString()}`
    : t("transaction_limit_value");

  const list = [
    {
      label: t("account_type"),
      value: accountData?.accountType ? tForm(accountData.accountType) : t("account_type_value"),
    },
    {
      label: t("account_number"),
      value: accountNumber,
    },
    {
      label: t("transaction_limit"),
      value: formattedLimit,
    },
  ];

  const handleGoHome = () => {
    navigate("/verification");
  };

  return (
    <div className="rounded-xl bg-white shadow-md pb-6 result text-[#42307D]">
      <div className="m-auto w-full">
        <img
          src="assets/gifs/success.gif"
          alt="success"
          className="block m-auto h-[250px]"
        />
        <p className="text-center font-semibold md:text-[36px] text-[32px] pb-4">
          {t("congrats")}
        </p>
        <p className="text-center text-[20px] my-4 px-2">
          {t("account_created_message")}
        </p>
        <div className="mt-6 mb-[1rem] flex flex-col xl:w-[50%] md:w-[50%] mx-2 sm:mx-auto">
          {list.map((item, idx) => (
            <div key={idx} className="my-4 flex justify-center items-center">
              <img
                src="assets/images/arrow_right.svg"
                alt="arrow_right"
                className="inline relative"
              />
              <span className="mx-4 w-48 text-left">{item.label}:</span>
              <span className="w-32 md:w-48">
                <span className="text-[#14A35C] font-semibold">{item.value}</span>
              </span>
            </div>
          ))}
        </div>
        <div className="flex justify-center mt-8">
          <button
            type="button"
            className="bg-[#7F56D9] px-8 py-3 text-white rounded-md hover:bg-[#6941C6] transition-colors"
            onClick={handleGoHome}
          >
            {t("go_home")}
          </button>
        </div>
      </div>
    </div>
  );
};

export default Result;
