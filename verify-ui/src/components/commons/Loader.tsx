import React from "react";

const Loader = (props: { className?: string; innerBg?: string }) => {
  return (
    <div id="loader" className="flex justify-center items-center">
      <div
        className={`w-16 h-16 rounded-full relative flex items-center justify-center bg-${globalThis._env_.DEFAULT_THEME}-gradient animate-spin ${
          props.className ?? ""
        }`}
      >
        <div
          className={`${
            props.innerBg ?? "bg-white"
          } w-[85%] h-[85%] rounded-full`}
        />
      </div>
    </div>
  );
};

export default Loader;
