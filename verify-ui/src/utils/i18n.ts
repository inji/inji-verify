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
import { iso6393, Language } from "iso-639-3";

const resources = { en, ta, kn, hi, fr, ar, pt, es, km };

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

export function normalizeLanguageCode(lang: string | undefined | null): string {
  const defaultLang = window._env_?.DEFAULT_LANG || "en";
  if (!lang) return defaultLang;

  const code = lang.toLowerCase();

  if (code.length === 2) {
    const match = iso6393.find((entry) => entry.iso6391 === code);
    return match?.iso6391 ?? defaultLang;
  }

  if (code.length === 3) {
    const match = iso6393.find((entry) => entry.iso6393 === code);
    return match?.iso6391 ?? defaultLang;
  }

  return defaultLang;
}

export function getLanguageCodes(lang: string): string[] {
  const normalized = normalizeLanguageCode(lang);
  const entry = iso6393.find((e: Language) => e.iso6391 === normalized);

  if (!entry) return [normalized];

  const aliases = new Set<string>();

  if (entry.iso6391) aliases.add(entry.iso6391);
  aliases.add(entry.iso6393);

  return Array.from(aliases);
}

export const defaultLanguage = normalizeLanguageCode(window._env_?.DEFAULT_LANG) || "en";
export const selected_language = normalizeLanguageCode(storage.getItem(storage.SELECTED_LANGUAGE));

i18n.use(initReactI18next).init({
  resources,
  lng: selected_language || defaultLanguage,
  fallbackLng: defaultLanguage,
  interpolation: {
    escapeValue: false,
  },
});

export const switchLanguage = async (language: string) => {
  const normalized = normalizeLanguageCode(language);
  storage.setItem(storage.SELECTED_LANGUAGE, normalized);
  await i18n.changeLanguage(normalized);
};

export const isRTL = (language: string) => {
  return normalizeLanguageCode(language) === "ar";
};

export const getDirCurrentLanguage = (language: string) => {
  return isRTL(language) ? "rtl" : "ltr";
};
