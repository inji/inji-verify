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

const getButtonStyle = (variant: ButtonVariant, disabled: boolean, gradient: string, textGradient: string) => {
  if (disabled) {
    return { wrapper: "bg-disabledButtonBg", button: "bg-disabledButtonBg text-white", hover: "", text: "text-white" };
  }
  switch (variant) {
    case "fill":
      return { wrapper: gradient, button: `${gradient} text-white`, hover: "", text: "text-white" };
    case "outline":
      return { wrapper: gradient, button: "bg-white", hover: `hover:${gradient} hover:text-white`, text: `${textGradient} group-hover:text-white` };
    case "clear":
      return { wrapper: "", button: "bg-transparent", hover: `hover:${gradient} hover:text-white`, text: `${textGradient} group-hover:text-white` };
    default:
      return { wrapper: "", button: "bg-transparent", hover: "", text: textGradient };
  }
};

export const Button = ({
  title,
  icon,
  variant = "fill",
  disabled = false,
  className = "",
  id,
  type: buttonType = "button",
  ...rest
}: ButtonProps) => {
  const theme = window._env_?.DEFAULT_THEME || "primary";
  const gradient = `bg-${theme}-gradient`;
  const textGradient = `bg-${theme}-gradient bg-clip-text text-transparent`;

  const style = getButtonStyle(variant, disabled, gradient, textGradient);
  const wrapperClass = ["rounded-[5px]", "transition-all duration-200", style.wrapper, className].filter(Boolean).join(" ");

  const buttonBaseClass = [
    "min-h-[40px]",
    "py-2",
    "w-full",
    "rounded-[5px]",
    "flex",
    "items-center",
    "justify-center",
    "group",
  ];

  const textClass = ["font-bold normal-case transition-all duration-200", style.text].join(" ");

  return (
    <div className={wrapperClass}>
      <button
        {...rest}
        type={buttonType}
        id={id}
        disabled={disabled}
        className={[...buttonBaseClass, style.button, style.hover].join(" ")}
      >
        {icon && <span className="mr-1.5">{icon}</span>}
        <span className={textClass}>{title}</span>
      </button>
    </div>
  );
};
