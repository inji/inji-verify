import React, { useEffect, useRef, useState } from "react";
import { isRTL, LanguagesSupported, switchLanguage } from "../../utils/i18n";
import { storeLanguage } from "../../redux/features/common/commonSlice";
import { useAppDispatch, useAppSelector } from "../../redux/hooks";
import { RootState } from "../../redux/store";
import { renderGradientText } from "../../utils/builder";
import { ArrowDown, ArrowUp, Check, GlobeIcon } from "../../utils/theme-utils";

interface DropdownItem {
  label: string;
  value: string;
}

export const LanguageSelector: React.FC = () => {
  const dispatch = useAppDispatch();
  let language = useAppSelector((state: RootState) => state.common.language);
  language = language ?? globalThis._env_.DEFAULT_LANG;
  const rtl = isRTL(language);
  const containerRef = useRef<HTMLDivElement>(null);
  const triggerRef = useRef<HTMLButtonElement>(null);

  const [isOpen, setIsOpen] = useState(false);

  const handleChange = (item: DropdownItem) => {
    setIsOpen(false);
    void switchLanguage(item.value);
    dispatch(storeLanguage({ language: item.value }));
    triggerRef.current?.focus();
  };

  useEffect(() => {
    if (isOpen) {
      containerRef.current?.querySelector<HTMLButtonElement>('[role="menuitemradio"][aria-checked="true"]')?.focus();
    }
  }, [isOpen]);

  const handleMenuKeyDown = (event: React.KeyboardEvent<HTMLUListElement>) => {
    if (event.key === "Escape") {
      setIsOpen(false);
      triggerRef.current?.focus();
      return;
    }
    if (!["ArrowDown", "ArrowUp", "Home", "End"].includes(event.key)) return;
    event.preventDefault();
    const buttons = Array.from(event.currentTarget.querySelectorAll<HTMLButtonElement>('[role="menuitemradio"]'));
    const currentIndex = buttons.indexOf(document.activeElement as HTMLButtonElement);
    let nextIndex = 0;
    if (event.key === "End") nextIndex = buttons.length - 1;
    if (event.key === "ArrowDown") nextIndex = (currentIndex + 1) % buttons.length;
    if (event.key === "ArrowUp") nextIndex = (currentIndex - 1 + buttons.length) % buttons.length;
    buttons[nextIndex]?.focus();
  };

  useEffect(() => {
    function handleClickOutside(e: MouseEvent) {
      if (isOpen && !containerRef.current?.contains(e.target as Node)) {
        setIsOpen(false);
      }
    }
    document.addEventListener("mousedown", handleClickOutside);
    return () => document.removeEventListener("mousedown", handleClickOutside);
  }, [isOpen]);

  return (
    <div
      className="flex flex-row justify-center items-center"
      data-testid="LanguageSelector-Outer-Div"
      ref={containerRef}
    >
      <GlobeIcon />

      <div className="relative inline-block ms-1">
        <button
          type="button"
          className="inline-flex items-center"
          data-testid="Language-Selector-Button"
          onClick={() => setIsOpen(!isOpen)}
          ref={triggerRef}
          aria-haspopup="menu"
          onKeyDown={(event) => {
            if (event.key === "ArrowDown" || event.key === "ArrowUp") {
              event.preventDefault();
              setIsOpen(true);
            }
          }}
          aria-expanded={isOpen}
        >
          <p
            data-testid={`Language-Selector-Selected-DropDown-${language}`}
            className="text-md font-bold"
          >
            {LanguagesSupported.find((lang) => lang.value === language)?.label}
          </p>
          {isOpen ? (
            <div className="px-1">
              <ArrowUp />
            </div>
          ) : (
            <div className="px-1">
              <ArrowDown />
            </div>
          )}
        </button>

        {isOpen && (
          <div
            className={`absolute top-10 w-[167px] lg:w-60 z-40 ${
              rtl ? "left-1 lg:left-0" : "right-1 lg:right-0"
            } mt-3 rounded-md shadow-lg bg-background overflow-hidden font-normal border border-gray-200`}
          >
            <ul role="menu" aria-label="Languages" onKeyDown={handleMenuKeyDown} className="divide-y divide-gray-200">
              {LanguagesSupported.map((item) => (
                <li
                  role="none"
                  key={item.value}
                  data-testid={`Language-Selector-DropDown-Item-${item.value}`}
                >
                  <button
                    type="button"
                    className="w-full px-4 py-2 bg-white text-left text-sm hover:bg-gray-100 flex items-center justify-between flex-row"
                    onClick={(event) => {
                      event.stopPropagation();
                      handleChange(item);
                    }}
                    role="menuitemradio"
                    tabIndex={-1}
                    aria-checked={language === item.value}
                  >
                    {language === item.value
                      ? renderGradientText(item.label)
                      : item.label}
                    {language === item.value && <Check />}
                  </button>
                </li>
              ))}
            </ul>
          </div>
        )}
      </div>
    </div>
  );
};
