import React from "react";
import { useTranslation } from "react-i18next";
import { useLocation } from "react-router-dom";
import { Pages } from "../../utils/config";

function Copyrights() {
    const {t} = useTranslation("CopyRight");
    const { pathname } = useLocation();
    const isOfflinePage = pathname === Pages.Offline;

    return (
        <div className={`grid w-[100vw] lg:w-[49vw] content-center justify-center bg-white ${isOfflinePage ? "" : "fixed bottom-0"}`}>
            <div className="xs:w-[90vw] lg:w-[40vw] mx-auto border-b-[1px] border-b-copyRightsBorder opacity-20"/>
            <p id="copyrights-content" className="py-4 px-0 w-[100%] text-center text-normalTextSize font-normal text-copyRightsText">
                {t('content')}
            </p>
        </div>
    );
}

export default Copyrights;