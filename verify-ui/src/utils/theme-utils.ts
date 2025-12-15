import defaultTheme from "../themes/default-theme";
import purpleTheme from "../themes/purple-theme";
import carTheme from "../themes/car-theme";
import truckpassTheme from "../themes/truckpass-theme"; // <-- NEW THEME IMPORT

let Theme;

// This value decides which theme is active
const ActiveTheme = window._env_.DEFAULT_THEME;

switch (ActiveTheme) {
  case "default_theme":
    Theme = defaultTheme;
    break;

  case "purple_theme":
    Theme = purpleTheme;
    break;

  case "car_theme":
    Theme = carTheme;
    break;

  case "truckpass_theme":         // <-- NEW CASE
    Theme = truckpassTheme;
    break;

  default:
    Theme = defaultTheme;
    break;
}

// EXPORT ALL THE ICONS/COMPONENTS FROM THE ACTIVE THEME

export const Logo = Theme.Logo;
export const InjiLogo = Theme.InjiLogo;
export const QrIcon = Theme.QrIcon;

export const GradientScanIcon = Theme.GradientScanIcon;
export const WhiteScanIcon = Theme.WhiteScanIcon;

export const GradientUploadIcon = Theme.GradientUploadIcon;
export const WhiteUploadIcon = Theme.WhiteUploadIcon;

export const CameraAccessDeniedIcon = Theme.CameraAccessDeniedIcon;
export const DocumentIcon = Theme.DocumentIcon;

export const VerificationSuccessIcon = Theme.VerificationSuccessIcon;
export const VerificationFailedIcon = Theme.VerificationFailedIcon;

export const ScanOutline = Theme.ScanOutline;
export const GlobeIcon = Theme.GlobeIcon;

export const ArrowDown = Theme.ArrowDown;
export const ArrowUp = Theme.ArrowUp;

export const Check = Theme.Check;
export const UnderConstruction = Theme.UnderConstruction;

export const DownloadIcon = Theme.DownloadIcon;
export const WhiteDownloadIcon = Theme.WhiteDownloadIcon;

export const SearchIcon = Theme.SearchIcon;
export const QrCodeOutLine = Theme.QrCodeOutLine;

export const FilterLinesIcon = Theme.FilterLinesIcon;

export const ReGenerateIcon = Theme.ReGenerateIcon;
export const WhiteReGenerateIcon = Theme.WhiteReGenerateIcon;

export const CloseIcon = Theme.CloseIcon;
export const MenuIcon = Theme.MenuIcon;

export const NewTabIcon = Theme.NewTabIcon;

export const VectorDown = Theme.VectorDown;
export const VectorUp = Theme.VectorUp;
export const VectorOutline = Theme.VectorOutline;

export const VectorExpand = Theme.VectorExpand;
export const VectorCollapse = Theme.VectorCollapse;
export const VectorDownload = Theme.VectorDownload;

export const SharableLink = Theme.SharableLink;
