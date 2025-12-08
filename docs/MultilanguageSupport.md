# Multilanguage Support

`Inji Verify` now supports `multilanguage rendering` of Verifiable Credential (VC) fields.
This allows issuers to embed localized values for attributes (such as gender, name, address, roles, and more), enabling the UI to display information in the **viewer’s preferred language**.

---

# Specifications supported

Inji Verify fully supports internationalized VC fields following the W3C Verifiable Credentials guidance on Language and Base Direction:
- [DataModel-1.1](https://www.w3.org/TR/vc-data-model-1.1/#language-and-base-direction)
- [DataModel-2.0](https://www.w3.org/TR/vc-data-model-2.0/#language-and-base-direction)

---

# Why Multilanguage Support?

Issuers often need to represent credential information in multiple languages for:

- International users  
- Government identity documents  
- Cross-border verification  
- Multilingual certificates or licenses  

To support this, Verifiable Credentials can include language-specific values using W3C-recommended structures.

---

# How Issuers Provide Multilanguage Fields

Issuers must represent localized fields using an array of objects containing:

- `language` – ISO language code  
- `value` – localized value  

Issuers may express human-readable fields using the standard language value object format:

```json
"property": {
  "value": "The string value",
  "lang": "LANGUAGE"
}
```

Or

```json
"property": {
  "@value": "The string value",
  "@language": "LANGUAGE"
}
```

## Example (Gender field in 2 languages)

```json
"gender": [
  { "lang": "en", "value": "Male" },
  { "@language": "ar", "@value": "ذكر" }
]
```
The issuer may include as many languages as needed.

---

# Supported Credential Formats

Inji Verify supports both patterns:

1. Array of language objects (as often found in JSON credentials)

```json
"gender": [
  { "language": "en", "value": "Male" },
  { "language": "fr", "value": "Homme" },
  { "language": "ar", "value": "ذكر" }
]
```

2. W3C JSON-LD language value objects

```json
"title": [
  { "@value": "HTML and CSS", "@language": "en" },
  { "@value": "HTML و CSS", "@language": "ar" }
]
```

Inji Verify normalizes both formats internally and renders them consistently.

---

# Functionality

# How Inji Verify Selects the Correct Language

After a VC is verified, the `Verify UI` reads the `user's selected language` (e.g., en, fr, ta, ar) and matches it against each `multilingual VC field`.

## Step-by-step:

### 1. Match selected UI language

If any VC entry has:

`@language` == `selectedLanguage`

→ That value is displayed.

**_Example_** :
Selected language = `ta`, 
Field contains `Tamil` → show the `Tamil` value:
`பெங்களூர்`

### 2. Fallback to English

If the `selected language` is `not available`:

`Inji Verify` falls back to `English (en)`.

This guarantees that every field always displays a value.

### 3. ↩️ RTL (Right-to-Left) Rendering

If the `selected UI` language is `RTL` (e.g., ar):

The entire `UI layout` switches to `right-to-left` mode.

Text alignment flips.

Key–value components reorder accordingly.

SVG Template rendering also applies RTL direction (if the template uses direction hints).

This ensures accurate and accessible Arabic/RTL presentation.