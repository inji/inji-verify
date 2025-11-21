import i18n from "i18next";
import { initReactI18next } from "react-i18next";
import en from "../locales/en.json";
import fr from "../locales/fr.json";
import ta from "../locales/ta.json";
import hi from "../locales/hi.json";
import kn from "../locales/kn.json";
import ar from "../locales/ar.json";
import pt from "../locales/pt.json";
import es from "../locales/es.json";
import km from "../locales/km.json";
import { storage } from "./storage";
import { LanguageObject } from "../types/data-types";

/**
 * Build resource object for a language so keys can be resolved whether
 * components use:
 *   - t('Common.Button.upload')
 *   - t('Common:Button.upload')
 *   - t('Button.upload')
 *
 * Approach:
 *  - always register `translation` namespace containing the whole JSON
 *  - register each top-level key as its own namespace
 *  - register each second-level key (if object) as its own namespace
 */
const buildResource = (langObj: Record<string, any>) => {
  const res: Record<string, any> = {
    translation: langObj,
  };

  // register top-level namespaces (e.g. Common, Accepted, Rejected)
  Object.keys(langObj).forEach((topKey) => {
    const topVal = langObj[topKey];
    if (typeof topVal === "object" && topVal !== null) {
      res[topKey] = topVal;

      // register second-level namespaces (e.g. Button) pointing to the subtree
      Object.keys(topVal).forEach((secondKey) => {
        const secondVal = topVal[secondKey];
        if (typeof secondVal === "object" && secondVal !== null) {
          // Only add if not colliding with an already added namespace
          if (!res[secondKey]) {
            res[secondKey] = secondVal;
          }
        }
      });
    }
  });

  return res;
};

// Build resources for all languages using the helper above
const resources = {
  en: buildResource(en),
  ta: buildResource(ta),
  kn: buildResource(kn),
  hi: buildResource(hi),
  fr: buildResource(fr),
  ar: buildResource(ar),
  pt: buildResource(pt),
  es: buildResource(es),
  km: buildResource(km),
};

export const LanguagesSupported: LanguageObject[] = [
  { label: "English", value: "en" },
  { label: "Português", value: "pt" },
  { label: "தமிழ்", value: "ta" },
  { label: "ಕನ್ನಡ", value: "kn" },
  { label: "हिंदी", value: "hi" },
  { label: "Français", value: "fr" },
  { label: "عربي", value: "ar" },
  { label: "español", value: "es" },
  { label: "ខ្មែរ", value: "km" },
];

export const defaultLanguage = window._env_.DEFAULT_LANG;

export const selected_language = storage.getItem(storage.SELECTED_LANGUAGE);

i18n.use(initReactI18next).init({
  resources,
  lng: selected_language ? selected_language : defaultLanguage,
  fallbackLng: defaultLanguage,
  interpolation: {
    escapeValue: false,
  },
});

export const switchLanguage = async (language: string) => {
  storage.setItem(storage.SELECTED_LANGUAGE, language);
  await i18n.changeLanguage(language);
};

export const isRTL = (language: string) => {
  return language === "ar";
};

export const getDirCurrentLanguage = (language: string) => {
  return isRTL(language) ? "rtl" : "ltr";
};
