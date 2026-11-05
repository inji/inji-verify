import React from "react";
import { VectorOutline } from "../../../../utils/theme-utils";

interface ActionButtonProps {
  label: string;
  onClick: () => void;
  icon: React.ReactNode;
  positionClasses: string; // Classes to handle positioning
}

const ActionButton: React.FC<ActionButtonProps> = ({
  label,
  onClick,
  icon,
  positionClasses,
}) => {
  return (
    <div
      className={`group absolute flex items-center justify-center ${positionClasses} gap-2`}
    >
      <p
        className={`hidden group-hover:flex items-center justify-center text-smallTextSize w-[105px] bg-${globalThis._env_.DEFAULT_THEME}-gradient text-white p-px bg-no-repeat rounded-[5px]`}
      >
        {label}
      </p>

      <button
        type="button"
        onClick={onClick}
        className="flex items-center justify-center w-[40px] h-[40px] aspect-square bg-cover opacity-25 group-hover:opacity-100"
        style={{
          backgroundImage: `url(${VectorOutline})`,
        }}
      >
        {icon}
      </button>
    </div>
  );
};

export default ActionButton;
