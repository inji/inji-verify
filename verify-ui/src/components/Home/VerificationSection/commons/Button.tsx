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
  };

export const Button = ({
  title,
  icon,
  variant = "fill",
  disabled = false,
  className = "",
  id,
  ...rest
}: ButtonProps) => {
  const theme = window._env_?.DEFAULT_THEME || "primary";
  const gradient = `bg-${theme}-gradient`;
  const textGradient = `bg-${theme}-gradient bg-clip-text text-transparent`;

  const isOutline = variant === "outline";
  const isClear = variant === "clear";
  const isFill = variant === "fill";

  const wrapperClass = [
    "rounded-[5px]",
    "transition-all duration-200",
    disabled
      ? "bg-disabledButtonBg"
      : isOutline || isFill
      ? `${gradient}`
      : "", // clear → no border
    className,
  ]
    .filter(Boolean)
    .join(" ");

  const buttonBaseClass = [
    "h-[40px]",
    "w-full",
    "rounded-[5px]",
    "flex",
    "items-center",
    "justify-center",
    "group",
  ];

  const buttonStateClass = disabled
    ? "bg-disabledButtonBg text-white"
    : isFill
    ? `${gradient} text-white`
    : isOutline
    ? "bg-white"
    : "bg-transparent"; // clear

  const hoverClass = disabled
    ? ""
    : isOutline || isClear
    ? `hover:${gradient} hover:text-white`
    : "";

  const textClass = [
    "font-bold normal-case transition-all duration-200",
    disabled
      ? "text-white"
      : isFill
      ? "text-white"
      : textGradient, // for outline/clear
    (isOutline || isClear) && !disabled
      ? "group-hover:text-white"
      : "",
  ]
    .filter(Boolean)
    .join(" ");

  return (
    <div className={wrapperClass}>
      <button
        {...rest}
        id={id}
        disabled={disabled}
        className={[...buttonBaseClass, buttonStateClass, hoverClass].join(" ")}
      >
        {icon && <span className="mr-1.5">{icon}</span>}
        <span className={textClass}>{title}</span>
      </button>
    </div>
  );
};
