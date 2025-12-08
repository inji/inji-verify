import React, { useState } from "react";
import { useNavigate } from "react-router-dom";
import { useTranslation } from "react-i18next";

import logo from "../assets/truckpassTheme/TruckpassVerifyLogo.svg";
import banner from "../assets/truckpassTheme/WelcomeBanner.svg";
import truckIcon from "../assets/truckpassTheme/truckpassTruckLogo.svg";
import { LanguageSelector } from "../components/commons/LanguageSelector";

export default function TruckPassLogin() {
  const [email, setEmail] = useState<string>("");
  const [error, setError] = useState<string>("");
  const [loading, setLoading] = useState<boolean>(false);
  const navigate = useNavigate();

  // 🔹 Use separate namespaces
  const { t: tNavbar } = useTranslation("Navbar");
  const { t: tLogin } = useTranslation("LoginPage");

  function validateEmail(value: string): boolean {
    return /\S+@\S+\.\S+/.test(value);
  }

  const handleEmailSubmit = async (
    e: React.FormEvent<HTMLFormElement>
  ): Promise<void> => {
    e.preventDefault();
    setError("");

    if (!validateEmail(email)) {
      setError(tLogin("errors.invalidEmail"));
      return;
    }

    try {
      setLoading(true);
      await new Promise((r) => setTimeout(r, 500));
      navigate("/otp", { state: { email } });
    } catch {
      setError(tLogin("errors.generic"));
    } finally {
      setLoading(false);
    }
  };

  return (
    <div className="min-h-screen flex flex-col">
      {/* Header */}
      <div className="flex items-center justify-between px-10 py-3 w-full">
        <div className="flex items-center gap-2">
          <img src={truckIcon} alt="TruckPass Icon" className="h-6 w-auto" />
          <img src={logo} alt="TruckPass Verify Logo" className="h-8 w-auto" />
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

      {/* Main content */}
      <main className="flex-1 grid grid-cols-1 lg:grid-cols-2">
        <div className="hidden lg:flex items-center justify-center bg-gray-50">
          <img
            src={banner}
            alt="TruckPass Verify Banner"
            className="w-full h-full object-cover"
          />
        </div>

        <section className="flex items-center justify-center py-16 px-8 bg-white">
          <div className="w-full max-w-md">
            <div className="mb-8">
              <h2 className="text-2xl font-semibold">
                {tLogin("title")}
              </h2>
            </div>

            <form onSubmit={handleEmailSubmit} className="space-y-4">
              <div>
                <input
                  id="email"
                  type="email"
                  value={email}
                  onChange={(e) => setEmail(e.target.value)}
                  placeholder={tLogin("emailPlaceholder")}
                  className="w-full border border-gray-200 rounded-md px-4 py-3 focus:outline-none focus:ring-2 focus:ring-blue-400"
                  aria-label="Email address"
                />
              </div>

              {error && <div className="text-sm text-red-600">{error}</div>}

              <div>
                <button
                  type="submit"
                  className="w-full inline-flex items-center justify-center gap-2 bg-blue-600 hover:bg-blue-700 text-white font-medium py-3 rounded-md shadow-sm disabled:opacity-60"
                  disabled={loading}
                >
                  {loading && (
                    <svg className="animate-spin h-5 w-5" viewBox="0 0 24 24">
                      <circle
                        className="opacity-25"
                        cx="12"
                        cy="12"
                        r="10"
                        stroke="currentColor"
                        strokeWidth="4"
                        fill="none"
                      />
                      <path
                        className="opacity-75"
                        fill="currentColor"
                        d="M4 12a8 8 0 018-8v8z"
                      />
                    </svg>
                  )}
                  <span>{tLogin("buttons.continue")}</span>
                </button>
              </div>
            </form>
          </div>
        </section>
      </main>
    </div>
  );
}
