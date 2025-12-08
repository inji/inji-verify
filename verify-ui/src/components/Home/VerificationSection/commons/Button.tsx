import React, {
  HTMLAttributes,
  ReactElement,
  ButtonHTMLAttributes,
} from "react";

type ButtonVariant = "fill" | "outline" | "clear";

type ButtonProps = HTMLAttributes<HTMLButtonElement> &
  ButtonHTMLAttributes<HTMLButtonElement> & {
    title: string;
    icon?: ReactElement;
    variant?: ButtonVariant;
    disabled?: boolean;

    /* --- NEW OPTIONAL PROPS (Non-breaking) --- */
    bgColor?: string;              // solid bg override
    textColor?: string;            // solid text color override
    noGradient?: boolean;          // disable gradient behavior completely
    minWidth?: number | string;    // optional min width
    iconPosition?: "left" | "right"; // default: left
  };

export const Button = ({
  title,
  icon,
  variant = "fill",
  disabled = false,
  className = "",
  bgColor,
  textColor,
  minWidth,
  noGradient = false,
  iconPosition = "left",
  id,
  ...rest
}: ButtonProps) => {
  const theme = window._env_?.DEFAULT_THEME || "primary";
  const gradient = `bg-${theme}-gradient`;
  const textGradient = `bg-${theme}-gradient bg-clip-text text-transparent`;

  const isOutline = variant === "outline";
  const isClear = variant === "clear";
  const isFill = variant === "fill";

  /* ----------------------------------------
     Determine background behavior
     ---------------------------------------- */

  const usesCustomSolidColor = !!bgColor || noGradient;

  const computedBg =
    disabled
      ? "bg-disabledButtonBg"
      : usesCustomSolidColor
      ? bgColor
      : isFill
      ? gradient
      : isOutline
      ? "bg-white"
      : "bg-transparent";

  const computedTextColor =
    disabled
      ? "text-white"
      : textColor
      ? textColor
      : isFill && !usesCustomSolidColor
      ? "text-white"
      : !isFill && !noGradient
      ? textGradient
      : "text-black";

  /* Hover only applies when not disabled & not using custom color */
  const hoverClass =
    disabled || usesCustomSolidColor
      ? ""
      : isFill
      ? ""
      : `hover:${gradient} hover:text-white`;

  /* ---------------------------------------- */

  return (
    <div
      className={[
        "rounded-[5px]",
        "transition-all duration-200",
        computedBg,
        className,
      ]
        .filter(Boolean)
        .join(" ")}
      style={{ minWidth }}
    >
      <button
        {...rest}
        id={id}
        disabled={disabled}
        className={[
          "h-[40px]",
          "w-full",
          "rounded-[5px]",
          "flex",
          "items-center",
          "justify-center",
          "gap-2",
          computedBg,
          hoverClass,
          "group",
        ].join(" ")}
      >
        {icon && iconPosition === "left" && (
          <span className="flex items-center">{icon}</span>
        )}

        <span
          className={[
            "font-bold normal-case transition-all duration-200",
            computedTextColor,
            !disabled && (isOutline || isClear) && !usesCustomSolidColor
              ? "group-hover:text-white"
              : "",
          ].join(" ")}
        >
          {title}
        </span>

        {icon && iconPosition === "right" && (
          <span className="flex items-center">{icon}</span>
        )}
      </button>
    </div>
  );
};
