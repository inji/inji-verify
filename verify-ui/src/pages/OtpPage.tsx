import React, { useRef, useState } from "react";
import { useLocation, useNavigate } from "react-router-dom";
import { useTranslation } from "react-i18next";

import logo from "../assets/truckpassTheme/TruckpassVerifyLogo.svg";
import truckIcon from "../assets/truckpassTheme/truckpassTruckLogo.svg";
import { LanguageSelector } from "../components/commons/LanguageSelector";
import Copyrights from "../components/PageTemplate/Copyrights";

export default function OtpPage(): JSX.Element {
  const navigate = useNavigate();
  const location = useLocation();

  // 🔹 Use namespaces like on Login page
  const { t: tNavbar } = useTranslation("Navbar");
  const { t: tOtp } = useTranslation("OtpPage");

  const passedEmail = (location.state as any)?.email as string | undefined;
  const email = passedEmail ?? "youremail@gmail.com";

  // ⭐ Updated masking: show only first 2 characters
  const getMaskedEmail = (rawEmail: string): string => {
    const [localPart, domain] = rawEmail.split("@");
    if (!domain) return rawEmail;

    const visible = localPart.slice(0, 2); // ← updated
    const hiddenLength = Math.max(localPart.length - 2, 0);
    const mask = hiddenLength > 0 ? "•".repeat(hiddenLength) : "";

    return `${visible}${mask}@${domain}`;
  };

  const maskedEmail = getMaskedEmail(email);

  const OTP_LENGTH = 6;
  const DEFAULT_OTP = "111111";
  const [values, setValues] = useState<string[]>(Array(OTP_LENGTH).fill(""));
  const inputsRef = useRef<Array<HTMLInputElement | null>>([]);
  const [activeIdx, setActiveIdx] = useState<number | null>(null);

  const focusAt = (idx: number) => {
    const el = inputsRef.current[idx];
    el?.focus();
    el?.select?.();
    setActiveIdx(idx);
  };

  const handleChange = (idx: number, v: string) => {
    const filtered = v.replace(/\D/g, "");
    if (!filtered) return;

    setValues((prev) => {
      const copy = [...prev];
      let i = idx;
      for (const ch of filtered.split("")) {
        if (i < OTP_LENGTH) {
          copy[i] = ch;
          i++;
        }
      }
      return copy;
    });

    setTimeout(() => {
      for (let j = idx + 1; j < OTP_LENGTH; j++) {
        const el = inputsRef.current[j];
        if (el && !(el.value && el.value.length > 0)) {
          focusAt(j);
          return;
        }
      }
      focusAt(Math.min(OTP_LENGTH - 1, idx + filtered.length));
    }, 0);
  };

  const handleKeyDown = (
    e: React.KeyboardEvent<HTMLInputElement>,
    idx: number
  ) => {
    const key = e.key;
    if (key === "Backspace") {
      e.preventDefault();
      setValues((prev) => {
        const copy = [...prev];
        if (copy[idx]) {
          copy[idx] = "";
          setTimeout(() => focusAt(idx), 0);
        } else if (idx > 0) {
          copy[idx - 1] = "";
          setTimeout(() => focusAt(idx - 1), 0);
        }
        return copy;
      });
    } else if (key === "ArrowLeft" && idx > 0) {
      e.preventDefault();
      focusAt(idx - 1);
    } else if (key === "ArrowRight" && idx < OTP_LENGTH - 1) {
      e.preventDefault();
      focusAt(idx + 1);
    }
  };

  const handlePaste = (
    e: React.ClipboardEvent<HTMLInputElement>,
    idx: number
  ) => {
    e.preventDefault();
    const paste = e.clipboardData.getData("Text").replace(/\D/g, "");
    if (!paste) return;
    setValues((prev) => {
      const copy = [...prev];
      let i = idx;
      for (const ch of paste.split("")) {
        if (i < OTP_LENGTH) {
          copy[i] = ch;
          i++;
        }
      }
      return copy;
    });
    const next = Math.min(OTP_LENGTH - 1, idx + paste.length);
    setTimeout(() => focusAt(next), 0);
  };

  const getOtpValue = () => values.join("");

  const verifyOtp = (e?: React.FormEvent) => {
    e?.preventDefault();
    const otp = getOtpValue();

    if (otp === DEFAULT_OTP) {
      navigate("/scan");
    } else {
      setValues(Array(OTP_LENGTH).fill(""));
      setTimeout(() => focusAt(0), 0);
    }
  };

  return (
    <>
      <div className="min-h-screen flex flex-col bg-white">
        {/* Header */}
        <div className="flex items-center justify-between px-10 py-3 w-full">
          <div className="flex items-center gap-2">
            <img src={truckIcon} alt="TruckPass Icon" className="h-6 w-auto" />
            <img
              src={logo}
              alt="TruckPass Verify Logo"
              className="h-8 w-auto"
            />
          </div>

          <nav className="flex items-center gap-8">
            <a
              href="#"
              className="text-sm font-medium text-gray-800 hover:underline"
            >
              {tNavbar("home")}
            </a>
            <a
              href="#"
              className="text-sm font-medium text-gray-800 hover:underline"
            >
              {tNavbar("help")}
            </a>

            <LanguageSelector />
          </nav>
        </div>

        {/* Content */}
        <main className="flex-1 flex items-start justify-center px-6 py-16">
          <div className="w-full max-w-2xl">
            <div className="text-center">
              <div className="mx-auto mb-4 w-12 h-12 rounded-md border border-gray-200 flex items-center justify-center text-blue-600">
                <svg
                  xmlns="http://www.w3.org/2000/svg"
                  className="h-6 w-6"
                  viewBox="0 0 24 24"
                  fill="none"
                  stroke="currentColor"
                >
                  <path
                    strokeLinecap="round"
                    strokeLinejoin="round"
                    strokeWidth="2"
                    d="M3 8l7.89 5.26a2 2 0 002.22 0L21 8M5 19h14a2 2 0 002-2V7a2 2 0 00-2-2H5a2 2 0 00-2 2v10a2 2 0 002 2z"
                  />
                </svg>
              </div>

              <h1 className="text-2xl font-bold mb-2">{tOtp("title")}</h1>
              <p className="text-sm text-gray-500 mb-6">
                {tOtp("subtitle")}{" "}
                <span className="font-medium text-gray-800">
                  {maskedEmail}
                </span>
              </p>
            </div>

            <form onSubmit={verifyOtp} className="flex flex-col items-center">
              <div className="flex items-center gap-3 mb-6">
                {Array.from({ length: OTP_LENGTH }).map((_, idx) => (
                  <React.Fragment key={idx}>
                    <div
                      style={{
                        position: "relative",
                        display: "inline-block",
                      }}
                    >
                      {activeIdx === idx && (
                        <div
                          aria-hidden
                          style={{
                            position: "absolute",
                            top: -5,
                            left: -5,
                            right: -5,
                            bottom: -5,
                            borderRadius: 10,
                            border: "3px solid rgba(29,78,216,0.9)",
                            background: "transparent",
                          }}
                        />
                      )}

                      <input
                        id={`otp-${idx}`}
                        ref={(el) => (inputsRef.current[idx] = el)}
                        type="text"
                        inputMode="numeric"
                        maxLength={1}
                        value={values[idx]}
                        onChange={(ev) => handleChange(idx, ev.target.value)}
                        onKeyDown={(ev) => handleKeyDown(ev, idx)}
                        onPaste={(ev) => handlePaste(ev as any, idx)}
                        onFocus={() => setActiveIdx(idx)}
                        onBlur={() =>
                          setActiveIdx((cur) => (cur === idx ? null : cur))
                        }
                        placeholder={tOtp("otpPlaceholder")}
                        className={`w-14 h-14 text-2xl text-center rounded-md border-2 font-semibold outline-none relative z-10
                        placeholder-gray-300
                        ${
                          values[idx]
                            ? "border-blue-500 text-blue-600"
                            : "border-gray-200 text-gray-800"
                        }`}
                        aria-label={`OTP digit ${idx + 1}`}
                      />
                    </div>

                    {idx === 2 && (
                      <span className="mx-2 text-gray-400 text-lg">-</span>
                    )}
                  </React.Fragment>
                ))}
              </div>

              <button
                type="submit"
                className="w-56 bg-blue-600 hover:bg-blue-700 text-white font-medium py-3 rounded-md shadow-sm mb-4"
              >
                {tOtp("buttons.verify")}
              </button>

              <button
                type="button"
                onClick={() => navigate("/login")}
                className="text-sm text-gray-600 hover:underline flex items-center gap-2"
              >
                <svg
                  xmlns="http://www.w3.org/2000/svg"
                  className="h-4 w-4"
                  viewBox="0 0 24 24"
                  fill="none"
                  stroke="currentColor"
                >
                  <path
                    strokeLinecap="round"
                    strokeLinejoin="round"
                    strokeWidth="2"
                    d="M15 19l-7-7 7-7"
                  />
                </svg>
                {tOtp("buttons.backToLogin")}
              </button>
            </form>
          </div>
        </main>
      </div>

      {/* Fixed footer */}
      <Copyrights />
    </>
  );
}
