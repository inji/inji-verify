import { useState } from "react";
import { useAppSelector } from "../../../redux/hooks";
import { GradientUploadIcon, WhiteUploadIcon } from "../../../utils/theme-utils";
import { RootState } from "../../../redux/store";
import { isRTL } from "../../../utils/i18n";

const UploadButton = ({ displayMessage }: Readonly<{ displayMessage: string }>) => {
  const [isHover, setIsHover] = useState(false);
  const UploadIcon = isHover ? WhiteUploadIcon : GradientUploadIcon;
  const language = useAppSelector(
    (state: RootState) => state.common.language
  );
  const rtl = isRTL(language);

  return (
    <div
      className={`bg-${globalThis._env_.DEFAULT_THEME}-gradient hover:text-white p-px bg-no-repeat rounded-[5px] w-[180px] mt-10`}
    >
      <div
        onMouseEnter={() => setIsHover(true)}
        onMouseLeave={() => setIsHover(false)}
        onTouchStart={() => setIsHover(true)}
        className={`group bg-white hover:bg-${globalThis._env_.DEFAULT_THEME}-gradient font-bold h-[40px] rounded-[5px] flex content-center justify-center text-lgNormalTextSize pt-2 cursor-pointer`}
      >
        <span className={`${rtl ? "ml-1.5" : "mr-1.5"}`}>
          <UploadIcon />
        </span>
        <span
          id="upload-qr-code-button"
          className={`bg-${globalThis._env_.DEFAULT_THEME}-gradient bg-clip-text text-transparent group-hover:text-white`}
        >
          {displayMessage}
        </span>
      </div>
    </div>
  );
};

export const UploadQrCode = ({
  displayMessage,
  className,
}: {
  displayMessage: string;
  className?: string;
}) => {
  return (
    <div
      className={`mx-auto my-1.5 flex content-center justify-center ${className}`}
    >
      <UploadButton displayMessage={displayMessage} />
    </div>
  );
};
