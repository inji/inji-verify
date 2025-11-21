import './setupEnvironment'; // Ensure this is the first import
import '../src/utils/i18n'; // Ensure the i18n configuration loads
import React from 'react';
import ReactDOM from 'react-dom/client';
import './index.css';
import App from './App';
import reportWebVitals from './reportWebVitals';
import { Provider } from "react-redux";
import store from "./redux/store";

// ---------- NEW: set HTML theme class + lang/dir BEFORE React mounts ----------
/**
 * We set the theme class on document.documentElement early (before React mounts)
 * so Tailwind / CSS variables tied to [class="..."] selectors are available
 * immediately and you avoid a flash-of-unstyled-theme.
 *
 * window._env_.DEFAULT_THEME is used across your codebase; fallback to "default_theme".
 *
 * Also set lang + dir using the i18n default language defined in ../src/utils/i18n.ts
 * so the page renders in the correct direction (ltr/rtl) before React renders.
 */
try {
  // get theme from environment (string like "truckpass_theme", "default_theme", etc.)
  const activeTheme = (window as any)._env_?.DEFAULT_THEME || "default_theme";

  // add that class to the html element (replace any previous theme classes)
  const htmlEl = document.documentElement;
  // remove any *_theme classes already present (helpful in dev when hot-reloading)
  // keep other classes intact.
  const existingClasses = htmlEl.className
    .split(/\s+/)
    .filter(Boolean)
    .filter((c) => !c.endsWith("_theme"));
  // add the selected theme
  htmlEl.className = [...existingClasses, activeTheme].join(" ");

  // set language & direction if i18n is configured. This mirrors i18n init defaults.
  // we import i18n in this file indirectly (via ../src/utils/i18n) so global i18n exists.
  // We avoid importing i18n directly here to keep file content minimal,
  // but we access the same default via window._env_ keys if present.
  const defaultLang = (window as any)._env_?.DEFAULT_LANG || "en";
  htmlEl.lang = defaultLang;

  // If you have a global helper for RTL, we mimic it: only 'ar' requires RTL here.
  htmlEl.dir = defaultLang === "ar" ? "rtl" : "ltr";
} catch (e) {
  // non-fatal; if anything goes wrong just continue to render React app.
  // Keep console message for easier debugging.
  // eslint-disable-next-line no-console
  console.warn("Failed to set initial theme/lang on <html>:", e);
}
// ---------------------------------------------------------------------------

const root = ReactDOM.createRoot(
  document.getElementById('root') as HTMLElement
);

root.render(
  <React.StrictMode>
    <Provider store={store}>
        <App/>
    </Provider>
  </React.StrictMode>
);

// If you want to start measuring performance in your app, pass a function
// to log results (for example: reportWebVitals(console.log))
// or send to an analytics endpoint. Learn more: https://bit.ly/CRA-vitals
reportWebVitals();
