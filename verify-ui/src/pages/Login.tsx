import React, { useState } from "react";
import { useNavigate } from "react-router-dom";
import logo from "../assets/truckpassTheme/TruckpassVerifyLogo.png";
import banner from "../assets/truckpassTheme/WelcomeBanner.png";
import world from "../assets/truckpassTheme/LanguageSelectionWorld.png";

export default function TruckPassLogin() {
  const [email, setEmail] = useState<string>("");
  const [error, setError] = useState<string>("");
  const [loading, setLoading] = useState<boolean>(false);
  const navigate = useNavigate();

  function validateEmail(value: string): boolean {
    return /\S+@\S+\.\S+/.test(value);
  }

  const handleEmailSubmit = async (e: React.FormEvent<HTMLFormElement>): Promise<void> => {
    e.preventDefault();
    setError("");

    if (!validateEmail(email)) {
      setError("Please enter a valid email address.");
      return;
    }

    try {
      setLoading(true);
      // simulate short delay
      await new Promise((r) => setTimeout(r, 500));
      // redirect to OTP page
      navigate("/otp");
    } catch {
      setError("Something went wrong — try again.");
    } finally {
      setLoading(false);
    }
  };

  return (
    <div className="min-h-screen flex flex-col">
      {/* Header */}
        <div className="flex items-center justify-between px-10 py-3 w-full">
          {/* Left: Logo */}
          <div className="flex items-center">
            <img src={logo} alt="TruckPass Verify Logo" className="h-8 w-auto" />
          </div>

          {/* Right: Navigation */}
          <nav className="flex items-center gap-8">
            <a href="#" className="text-sm font-medium text-gray-800 hover:underline">
              Home
            </a>
            <a href="#" className="text-sm font-medium text-gray-800 hover:underline">
              Help
            </a>

            {/* Language Selector */}
            <button
              type="button"
              className="flex items-center gap-2 text-sm font-medium text-gray-800 hover:underline focus:outline-none"
            >
              <img src={world} alt="Language Selection" className="h-5 w-5 object-contain" />
              <span>English</span>
              <svg
                xmlns="http://www.w3.org/2000/svg"
                className="h-3 w-3 text-gray-600"
                viewBox="0 0 20 20"
                fill="currentColor"
              >
                <path
                  fillRule="evenodd"
                  d="M5.23 7.21a.75.75 0 011.06.02L10 10.94l3.71-3.71a.75.75 0 111.06 1.06l-4.24 4.24a.75.75 0 01-1.06 0L5.21 8.29a.75.75 0 01.02-1.06z"
                  clipRule="evenodd"
                />
              </svg>
            </button>
          </nav>
        </div>

      {/* Main content */}
      <main className="flex-1 grid grid-cols-1 lg:grid-cols-2">
        {/* Left: Banner */}
        <div className="hidden lg:flex items-center justify-center bg-gray-50">
          <img src={banner} alt="TruckPass Verify Banner" className="w-full h-full object-cover" />
        </div>

        {/* Right: Email Form */}
        <section className="flex items-center justify-center py-16 px-8 bg-white">
          <div className="w-full max-w-md">
            <div className="mb-8">
              <h2 className="text-2xl font-semibold">Log In</h2>
            </div>

            <form onSubmit={handleEmailSubmit} className="space-y-4">
              <div>
                <input
                  id="email"
                  type="email"
                  value={email}
                  onChange={(e) => setEmail(e.target.value)}
                  placeholder="Enter Your Email ID"
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
                  <span>Continue</span>
                </button>
              </div>
            </form>

          </div>
        </section>
      </main>
    </div>
  );
}
