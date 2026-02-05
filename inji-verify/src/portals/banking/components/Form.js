import React, { useState, useEffect, useMemo } from "react";
import Select from "react-select";
import { useTranslation } from "react-i18next";
import ModalPopup from "./Modal";

const TRANSACTION_LIMIT_PER_CLAIM = 1000;
const TRANSACTION_LIMIT_REGEX = /^\d+\.?\d{0,2}$/;
const PHONE_REGEX = /^[0-9+\-\s()]*$/;
const PIN_REGEX = /^\d*$/;

// Helper function to calculate the number of unique verified claims
const getVerifiedClaimsCount = (userInfo) => {
  if (!userInfo?.verified_claims || !Array.isArray(userInfo.verified_claims)) {
    return 0;
  }

  const uniqueVerifiedClaims = new Set();
  
  for (let verifiedClaim of userInfo.verified_claims) {
    if (verifiedClaim?.claims) {
      Object.keys(verifiedClaim.claims).forEach(claimKey => {
        if (verifiedClaim.claims[claimKey] !== null && verifiedClaim.claims[claimKey] !== undefined) {
          uniqueVerifiedClaims.add(claimKey);
        }
      });
    }
  }
  
  return uniqueVerifiedClaims.size;
};

// Helper function to get claim details with verification status
const getClaimDetails = (userInfo, fieldName) => {
  let result = { value: null, verified: false };
  
  if (userInfo?.[fieldName]) {
    result.value = userInfo[fieldName];
  }

  if (userInfo?.verified_claims && Array.isArray(userInfo.verified_claims)) {
    for (let verifiedClaim of userInfo.verified_claims) {
      if (verifiedClaim?.claims?.[fieldName]) {
        result.value = verifiedClaim.claims[fieldName];
        result.verified = true;
        break;
      }
    }
  }
  return result;
};

// Helper function to normalize date to YYYY-MM-DD format
const normalizeDateToISO = (dateString) => {
  if (!dateString || typeof dateString !== 'string') return "";
  
  const trimmed = dateString.trim();
  
  // If already in YYYY-MM-DD format, return as is
  if (/^\d{4}-\d{2}-\d{2}$/.test(trimmed)) {
    return trimmed;
  }
  
  // Handle DD-MM-YYYY format
  const ddmmyyyyMatch = trimmed.match(/^(\d{1,2})[-\/](\d{1,2})[-\/](\d{4})$/);
  if (ddmmyyyyMatch) {
    const [, day, month, year] = ddmmyyyyMatch;
    return `${year}-${month.padStart(2, '0')}-${day.padStart(2, '0')}`;
  }
  
  // Handle YYYY-MM-DD or YYYY/MM/DD format
  const yyyymmddMatch = trimmed.match(/^(\d{4})[-\/](\d{1,2})[-\/](\d{1,2})$/);
  if (yyyymmddMatch) {
    const [, year, month, day] = yyyymmddMatch;
    return `${year}-${month.padStart(2, '0')}-${day.padStart(2, '0')}`;
  }
  
  // If format not recognized, return empty.
  console.warn('Date format not recognized:', trimmed);
  return "";
};

// Helper function to format address from various formats
const getFormattedAddress = (userAddress) => {
  if (!userAddress) return "";
  if (typeof userAddress === "string") return userAddress;
  
  const parts = [
    userAddress.formatted,
    userAddress.street_address,
    userAddress.addressLine1,
    userAddress.addressLine2,
    userAddress.addressLine3,
    userAddress.locality,
    userAddress.city,
    userAddress.province,
    userAddress.region,
    userAddress.postalCode ? `(${userAddress.postalCode})` : null,
    userAddress.country
  ].filter(Boolean);
  
  return parts.join(", ");
};

// Reusable Toggle component
const Toggle = ({ id, label, checked, onChange }) => (
  <div className="flex items-center justify-between py-2">
    <label htmlFor={id} className="text-gray-700">{label}</label>
    <button
      id={id}
      type="button"
      onClick={() => onChange(!checked)}
      className={`relative inline-flex h-6 w-11 items-center rounded-full transition-colors ${
        checked ? 'bg-[#7F56D9]' : 'bg-gray-300'
      }`}
    >
      <span className={`inline-block h-4 w-4 transform rounded-full bg-white transition-transform ${
        checked ? 'translate-x-6' : 'translate-x-1'
      }`} />
    </button>
  </div>
);

// Reusable VerifiedBadge component
const VerifiedBadge = ({ show, title }) => {
  if (!show) return null;
  return (
    <img
      src="assets/images/verification-symbol.svg"
      alt="verified"
      className="inline ml-1 w-4 h-4"
      title={title}
    />
  );
};

// Reusable Asterisk component
const RequiredAsterisk = () => (
  <img src="assets/images/asterisk.svg" alt="asterisk" className="inline relative bottom-1" />
);

// Reusable FormField component
const FormField = ({ label, value, onChange, placeholder, disabled, verified, required, verifiedTitle, className = "", type = "text" }) => (
  <div className={className}>
    <span>{label}</span>
    {verified !== undefined && <VerifiedBadge show={verified} title={verifiedTitle} />}
    {required && <RequiredAsterisk />}
    <input
      type={type}
      value={value || ""}
      onChange={onChange}
      placeholder={!disabled ? placeholder : ""}
      disabled={disabled}
      className={`p-3 w-full rounded-md mt-3 border ${
        disabled ? 'text-gray-500 bg-gray-100 border-gray-100' : 'border-gray-300'
      }`}
    />
  </div>
);

// Date input field component with validation
const DateField = ({ label, value, onChange, placeholder, disabled, verified, required, verifiedTitle, className = "" }) => {
  const dateInputRef = React.useRef(null);

  const handleWrapperClick = () => {
    if (disabled || !dateInputRef.current) return;
    try {
      dateInputRef.current.showPicker();
    } catch (err) {
      dateInputRef.current.focus();
    }
  };

  return (
    <div className={className}>
      <span>{label}</span>
      {verified !== undefined && <VerifiedBadge show={verified} title={verifiedTitle} />}
      {required && <RequiredAsterisk />}
      <div className="relative" onClick={handleWrapperClick}>
        {/* Display input (shows formatted date, read-only) */}
        <input
          type="text"
          value={value || ""}
          placeholder={!disabled ? placeholder : ""}
          readOnly
          disabled={disabled}
          className={`p-3 w-full rounded-md mt-3 border ${
            disabled ? 'text-gray-500 bg-gray-100 border-gray-100 cursor-not-allowed' : 'border-gray-300 cursor-pointer'
          }`}
        />
        {/* Calendar icon */}
        {!disabled && (
          <svg
            className="absolute right-3 top-1/2 transform -translate-y-1/2 mt-1.5 w-5 h-5 text-gray-400 pointer-events-none"
            fill="none"
            stroke="currentColor"
            viewBox="0 0 24 24"
          >
            <path
              strokeLinecap="round"
              strokeLinejoin="round"
              strokeWidth={2}
              d="M8 7V3m8 4V3m-9 8h10M5 21h14a2 2 0 002-2V7a2 2 0 00-2-2H5a2 2 0 00-2 2v12a2 2 0 002 2z"
            />
          </svg>
        )}
        <input
          ref={dateInputRef}
          type="date"
          value={value || ""}
          onChange={onChange}
          disabled={disabled}
          max={new Date().toISOString().split('T')[0]}
          className="absolute top-0 left-0 w-full h-full opacity-0 pointer-events-none"
          tabIndex={-1}
        />
      </div>
    </div>
  );
};

// Numeric input field component
const NumericField = ({ label, value, onChange, placeholder, disabled, verified, required, verifiedTitle, className = "", maxLength }) => (
  <div className={className}>
    <span>{label}</span>
    {verified !== undefined && <VerifiedBadge show={verified} title={verifiedTitle} />}
    {required && <RequiredAsterisk />}
    <input
      type="text"
      inputMode="numeric"
      pattern="[0-9]*"
      value={value || ""}
      onChange={onChange}
      placeholder={!disabled ? placeholder : ""}
      disabled={disabled}
      maxLength={maxLength}
      className={`p-3 w-full rounded-md mt-3 border ${
        disabled ? 'text-gray-500 bg-gray-100 border-gray-100' : 'border-gray-300'
      }`}
    />
  </div>
);

// Section Header component
const SectionHeader = ({ children }) => (
  <p className="font-semibold sm:text-[24px] text-[22px]">{children}</p>
);

const Form = (props) => {
  const { t, i18n } = useTranslation("form");
  
  const customStyles = {
    control: (base, state) => ({
      ...base,
      minHeight: '48px',
      height: '48px',
      boxShadow: 'none',
      borderColor: state.isFocused ? '#d1d5db' : '#d1d5db',
      '&:hover': {
        borderColor: '#d1d5db'
      }
    }),
    valueContainer: (base) => ({
      ...base,
      height: '48px',
      padding: '0 12px'
    }),
    input: (base) => ({
      ...base,
      margin: '0px',
    }),
    indicatorsContainer: (base) => ({
      ...base,
      height: '48px',
    }),
  };

  const accountTypes = useMemo(() => [
    { value: "savings_account", label: t("savings_account") },
    { value: "checking_account", label: t("checking_account") },
    { value: "salary_account", label: t("salary_account") },
    { value: "student_account", label: t("student_account") },
  ], [t]);

  const genderOptions = useMemo(() => [
    { value: "male", label: t("gender_male") },
    { value: "female", label: t("gender_female") },
    { value: "other", label: t("gender_other") },
  ], [t]);

  // Form state
  const [formState, setFormState] = useState({
    accountType: null,
    transactionLimit: "",
    hasTransactionLimitError: false,
    internetBanking: false,
    mobileBanking: false,
    atmDebitCard: false,
    domesticTransaction: false,
    internationalTransaction: false,
    isChecked: false,
  });

  const [editableFields, setEditableFields] = useState({
    name: "",
    email: "",
    phone_number: "",
    gender: "",
    birthdate: "",
    address: "",
    pin: "",
    city: "",
  });

  const rawUserInfo = useMemo(() => {
    const stored = localStorage.getItem("userInfo");
    if (!stored) return null;
    
    try {
      const decoded = window.atob(stored);
      return JSON.parse(decoded);
    } catch (error) {
      console.error("Failed to decode/parse userInfo:", error);
      localStorage.removeItem("userInfo");
      return null;
    }
  }, []);

  const transactionLimitMax = useMemo(() => {
    const verifiedClaimsCount = getVerifiedClaimsCount(rawUserInfo);
    return Math.max(1, verifiedClaimsCount) * TRANSACTION_LIMIT_PER_CLAIM;
  }, [rawUserInfo]);

  const userInfo = useMemo(() => {
    if (!rawUserInfo) return null;
    const addressDetails = getClaimDetails(rawUserInfo, "address");
    const birthdateDetails = getClaimDetails(rawUserInfo, "birthdate");
    
    return {
      name: getClaimDetails(rawUserInfo, "name"),
      email: getClaimDetails(rawUserInfo, "email"),
      phone_number: getClaimDetails(rawUserInfo, "phone_number"),
      gender: getClaimDetails(rawUserInfo, "gender"),
      address: { value: getFormattedAddress(addressDetails.value), verified: addressDetails.verified },
      birthdate: { value: normalizeDateToISO(birthdateDetails.value), verified: birthdateDetails.verified },
      picture: getClaimDetails(rawUserInfo, "picture"),
    };
  }, [rawUserInfo]);

  // Initialize editable fields from userInfo
  useEffect(() => {
    if (userInfo) {
      setEditableFields(prev => ({
        ...prev,
        name: userInfo.name?.value || "",
        email: userInfo.email?.value || "",
        phone_number: userInfo.phone_number?.value || "",
        gender: userInfo.gender?.value || "",
        birthdate: userInfo.birthdate?.value || "",
        address: userInfo.address?.value || "",
      }));
    }
  }, [userInfo]);

  // Helper to check if field has userInfo data
  const hasUserData = (field) => Boolean(userInfo?.[field]?.value);

  // Form state handlers
  const updateFormState = (key, value) => setFormState(prev => ({ ...prev, [key]: value }));
  const updateEditableField = (key, value) => setEditableFields(prev => ({ ...prev, [key]: value }));

  const handleTransactionLimitChange = (e) => {
    const value = e.target.value;
    if (value === "") {
      updateFormState("transactionLimit", "");
      updateFormState("hasTransactionLimitError", false);
      return;
    }
    if (!TRANSACTION_LIMIT_REGEX.test(value)) return;
    
    const parsedValue = parseFloat(value);
    if (!Number.isFinite(parsedValue)) {
      updateFormState("hasTransactionLimitError", true);
      return;
    }
    
    const hasError = parsedValue > transactionLimitMax;
    updateFormState("hasTransactionLimitError", hasError);
    updateFormState("transactionLimit", value);
  };

  // Handler for phone number with validation
  const handlePhoneChange = (e) => {
    const value = e.target.value;
    if (value === "" || PHONE_REGEX.test(value)) {
      updateEditableField("phone_number", value);
    }
  };

  // Handler for PIN with numeric validation
  const handlePinChange = (e) => {
    const value = e.target.value;
    if (value === "" || PIN_REGEX.test(value)) {
      updateEditableField("pin", value);
    }
  };

  // Handler for date validation
  const handleDateChange = (e) => {
    updateEditableField("birthdate", e.target.value);
  };

  // Handler for gender selection
  const handleGenderChange = (selectedOption) => {
    updateEditableField("gender", selectedOption?.value || "");
  };

  const handleProceed = () => {
    props.onSubmit({
      accountType: formState.accountType,
      transactionLimit: formState.transactionLimit,
      personalInfo: {
        name: editableFields.name,
        email: editableFields.email,
        phone_number: editableFields.phone_number,
        gender: editableFields.gender,
        birthdate: editableFields.birthdate,
        address: editableFields.address,
        pin: editableFields.pin,
        city: editableFields.city,
      },
      channelAccess: {
        internetBanking: formState.internetBanking,
        mobileBanking: formState.mobileBanking,
        atmDebitCard: formState.atmDebitCard,
      },
      paymentCapabilities: {
        domesticTransaction: formState.domesticTransaction,
        internationalTransaction: formState.internationalTransaction,
      },
    });
    localStorage.removeItem("userInfo");
  };

  // Form validation
  const isFormValid = formState.isChecked && 
    formState.accountType && 
    !formState.hasTransactionLimitError && 
    formState.transactionLimit !== "" &&
    editableFields.name.trim() !== "" &&
    editableFields.birthdate.trim() !== "" &&
    editableFields.email.trim() !== "" &&
    editableFields.pin.trim() !== "" &&
    editableFields.city.trim() !== "" &&
    editableFields.gender.trim() !== "" &&
    editableFields.phone_number.trim() !== "" &&
    editableFields.address.trim() !== "" &&
    (formState.internetBanking || formState.mobileBanking || formState.atmDebitCard) &&
    (formState.domesticTransaction || formState.internationalTransaction);

  if (!userInfo) return null;

  return (
    <>
      <ModalPopup />
      <div className="rounded-xl bg-white shadow-md">
        {/* Header */}
        <div className="md:p-[2rem] border-b border-b-[#D7D8E1] p-3">
          <SectionHeader>{t("fill_details")}</SectionHeader>
          <p className="text-[#2C3345] mt-3 mb-1">{t("details_description")}</p>
        </div>

        <div className="md:p-[2rem] border-b border-b-[#D7D8E1] scrollable-div p-3">
          {/* (1) Primary Information */}
          <div className="sm:mb-6 mb-3">
            <SectionHeader>{t("primary_information")}</SectionHeader>
            <div className="sm:flex mt-6 gap-4">
              <div className="w-full">
                <label className="block mb-3">
                  <span>{t("type_of_account")}</span>
                  <RequiredAsterisk />
                </label>
                <Select
                  styles={customStyles}
                  isSearchable={false}
                  options={accountTypes}
                  placeholder={t("select_option")}
                  onChange={(e) => updateFormState("accountType", e.value)}
                  value={formState.accountType ? { value: formState.accountType, label: t(formState.accountType) } : null}
                />
              </div>
              <div className="w-full mt-4 sm:mt-0">
                <label className="block mb-3">
                  <span>{t("monthly_transaction_limit")}</span>
                  <RequiredAsterisk />
                </label>
                <input
                  type="text"
                  value={formState.transactionLimit}
                  onChange={handleTransactionLimitChange}
                  placeholder={t("transaction_limit_placeholder", { limit: `$${transactionLimitMax.toLocaleString()}` })}
                  className={`p-3 w-full h-12 rounded-md border ${
                    formState.hasTransactionLimitError ? 'border-red-500' : 'border-gray-300'
                  }`}
                />
                {formState.hasTransactionLimitError && (
                  <p className="text-red-500 text-sm mt-1">
                    {t("transaction_limit_error", { limit: `$${transactionLimitMax.toLocaleString()}` })}
                  </p>
                )}
              </div>
            </div>
          </div>

          {/* (2) Channel Access */}
          <div className="sm:mt-[2.5rem] mt-6">
            <SectionHeader>
              {t("channel_access")}
              <RequiredAsterisk />
            </SectionHeader>
            <div className="sm:flex mt-4 gap-8 items-center">
              {[
                { id: "internet-banking", label: t("internet_banking"), key: "internetBanking" },
                { id: "mobile-banking", label: t("mobile_banking"), key: "mobileBanking" },
                { id: "atm-debit-card", label: t("atm_debit_card"), key: "atmDebitCard" },
              ].map(({ id, label, key }) => (
                <div key={id} className="w-full sm:w-1/3">
                  <Toggle id={id} label={label} checked={formState[key]} onChange={(val) => updateFormState(key, val)} />
                </div>
              ))}
            </div>
          </div>

          {/* (3) Payment Capabilities */}
          <div className="sm:mt-[2.5rem] mt-6">
            <SectionHeader>
              {t("payment_capabilities")}
              <RequiredAsterisk />
            </SectionHeader>
            <div className="sm:flex mt-4 gap-8">
              {[
                { id: "domestic-transaction", label: t("domestic_transaction"), key: "domesticTransaction" },
                { id: "international-transaction", label: t("international_transaction"), key: "internationalTransaction" },
              ].map(({ id, label, key }) => (
                <div key={id} className="w-full sm:w-1/2">
                  <Toggle id={id} label={label} checked={formState[key]} onChange={(val) => updateFormState(key, val)} />
                </div>
              ))}
            </div>
          </div>

          {/* (4) Transfer Capabilities */}
          <div className="sm:mt-[2.5rem] mt-6">
            <SectionHeader>{t("transfer_capabilities")}</SectionHeader>
            <p className="text-gray-500 mt-2 text-sm">{t("transfer_capabilities_description")}</p>
          </div>

          {/* (5) Personal Information */}
          <div className="sm:mt-[2.5rem] mt-6">
            <SectionHeader>{t("personal_information")}</SectionHeader>
            <div className="grid sm:my-6 my-4 grid-cols-2 sm:gap-x-8">
              <FormField
                label={t("full_name")}
                value={editableFields.name}
                onChange={(e) => updateEditableField("name", e.target.value)}
                placeholder={t("name_placeholder")}
                disabled={hasUserData("name")}
                verified={userInfo.name?.verified}
                verifiedTitle={t("verified")}
                required
                className="row-span sm:col-span-1 col-span-12 mb-4"
              />
              
              <div className="row-span-3 sm:col-span-1 col-span-12 mb-4">
                <span>{t("photo")}</span>
                <VerifiedBadge show={userInfo.picture?.verified} title={t("verified")} />
                <img
                  alt={t("profile_picture")}
                  className="h-[167px] w-[167px] my-4 rounded-lg object-cover"
                  src={userInfo.picture?.value || "assets/images/profile.svg"}
                />
              </div>

              <div className="row-span sm:col-span-1 col-span-12 mb-4">
                <label className="block mb-3">
                  <span>{t("gender")}</span>
                  <VerifiedBadge show={userInfo.gender?.verified} title={t("verified")} />
                  <RequiredAsterisk />
                </label>
                <Select
                  styles={customStyles}
                  isSearchable={false}
                  options={genderOptions}
                  placeholder={t("gender_placeholder")}
                  onChange={handleGenderChange}
                  value={editableFields.gender ? genderOptions.find(opt => opt.value === editableFields.gender) : null}
                  isDisabled={hasUserData("gender")}
                />
              </div>

              <DateField
                label={t("dob")}
                value={editableFields.birthdate}
                onChange={handleDateChange}
                placeholder={t("dob_placeholder")}
                disabled={hasUserData("birthdate")}
                verified={userInfo.birthdate?.verified}
                verifiedTitle={t("verified")}
                required
                className="row-span sm:col-span-1 col-span-12 mb-4"
              />

              <FormField
                label={t("email")}
                value={editableFields.email}
                onChange={(e) => updateEditableField("email", e.target.value)}
                placeholder={t("email_placeholder")}
                disabled={hasUserData("email")}
                verified={userInfo.email?.verified}
                verifiedTitle={t("verified")}
                required
                type="email"
                className="row-span sm:col-span-1 col-span-12 mb-4"
              />

              <FormField
                label={t("phone_number")}
                value={editableFields.phone_number}
                onChange={handlePhoneChange}
                placeholder={t("phone_placeholder")}
                disabled={hasUserData("phone_number")}
                verified={userInfo.phone_number?.verified}
                verifiedTitle={t("verified")}
                required
                type="tel"
                className="row-span sm:col-span-1 col-span-12"
              />
            </div>
          </div>

          {/* (6) Residential Information */}
          <div className="sm:mt-[2.5rem] mt-6">
            <SectionHeader>{t("residential_information")}</SectionHeader>
            <div className="grid sm:my-6 my-3 grid-cols-2 sm:gap-x-8">
              <FormField
                label={t("address")}
                value={editableFields.address}
                onChange={(e) => updateEditableField("address", e.target.value)}
                placeholder={t("address_placeholder")}
                disabled={hasUserData("address")}
                verified={userInfo.address?.verified}
                verifiedTitle={t("verified")}
                required
                className="sm:col-span-2 col-span-12"
              />

              <NumericField
                label={t("pin")}
                value={editableFields.pin}
                onChange={handlePinChange}
                placeholder={t("pin_placeholder")}
                disabled={false}
                required
                maxLength={6}
                className="sm:col-span-1 col-span-12 mt-4"
              />

              <FormField
                label={t("city")}
                value={editableFields.city}
                onChange={(e) => updateEditableField("city", e.target.value)}
                placeholder={t("city_placeholder")}
                disabled={false}
                required
                className="sm:col-span-1 col-span-12 mt-4"
              />
            </div>
          </div>

          {/* (7) Declaration */}
          <div className="sm:mt-[2.5rem] mt-6">
            <SectionHeader>{t("declarations")}</SectionHeader>
          </div>
          <div className="my-4 flex items-baseline">
            <input
              type="checkbox"
              id="declaration"
              className="text-md scale-150 relative top-[1.5px] mx-2 sm:mx-0 hover:cursor-pointer w-12"
              onChange={(e) => updateFormState("isChecked", e.target.checked)}
            />
            <label htmlFor="declaration" className="mx-0 font-[400]">
              {t("terms_and_conditions")}
            </label>
          </div>
        </div>

        {/* Submit Button */}
        <div className="md:p-[2rem] flex justify-end p-3">
          <button
            type="submit"
            className="bg-[#7F56D9] px-[3rem] py-3 text-white rounded-md hover:cursor-pointer m-auto sm:m-0 w-full sm:w-max disabled:opacity-50 disabled:cursor-not-allowed"
            onClick={handleProceed}
            disabled={!isFormValid}
          >
            {t("submit")}
          </button>
        </div>
      </div>
    </>
  );
};

export default Form;
