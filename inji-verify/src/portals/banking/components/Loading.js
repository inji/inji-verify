import React, { useEffect, useState } from "react";
import { useTranslation } from "react-i18next";

const Loading = (props) => {
  const { t } = useTranslation("loading");

  const list = [
    { label: t("identity_verification") },
    { label: t("address_verification") },
    { label: t("kyc_check") },
    { label: t("account_setup") },
  ];

  const [activeIndex, setActiveIndex] = useState(-1); // Initially, all have opacity 0.25

  useEffect(() => {
    const interval = setInterval(() => {
      setActiveIndex((prevIndex) => {
        if (prevIndex < list.length - 1) {
          return prevIndex + 1;
        } else {
          clearInterval(interval);
          props.child(2);
          return prevIndex;
        }
      });
    }, 1000); // Change every 1 second

    return () => clearInterval(interval);
  }, []);

  return (
    <div className="rounded-xl bg-white shadow-md">
      <p className="text-center text-[#42307D] text-[2.5rem] font-semibold mx-auto pt-[3rem] flex justify-center">
        {t("processing_application")}
      </p>
      <img
        src="assets/gifs/loading.gif"
        alt="loading"
        className="block m-auto h-[350px]"
      />
      <p className="text-center text-[#42307D] font-semibold text-[1.5rem] mx-auto">
        {t("please_wait")}
      </p>
      <div className="mt-6 mb-[1rem] flex flex-col xl:w-[35%] md:w-[50%] mx-2 sm:mx-auto pb-[3rem]">
        {list.map((item, idx) => (
          <div
            key={idx}
            className="my-2 flex justify-center items-center"
            style={{
              opacity: idx <= activeIndex ? 1 : 0.25,
              transition: "opacity 0.5s",
            }}
          >
            <img
              src="assets/images/default_arrow_right.svg"
              alt="default_arrow_right"
              className="inline relative"
            />
            <span className="mx-4 w-40 text-left">{item.label}</span>
            <span className="w-8">
              <img
                src="assets/images/success_circle.svg"
                alt="success_circle"
                className="inline relative"
              />
            </span>
          </div>
        ))}
      </div>
    </div>
  );
};

export default Loading;
