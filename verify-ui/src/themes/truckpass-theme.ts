// src/themes/truckpass-theme.ts
// TruckPass theme - uses available SVGs and falls back to defaultTheme for missing icons

// Inline React components (for usage like <Icon />)
import { ReactComponent as NavLogo } from "../assets/truckpassTheme/TruckpassVerifyLogo.svg";
import { ReactComponent as GradientScanFillIcon } from "../assets/truckpassTheme/gradient-scan-icon.svg";
import { ReactComponent as GradientUploadIcon } from "../assets/truckpassTheme/gradient-upload-icon.svg";
import { ReactComponent as LanguageWorldIcon } from "../assets/truckpassTheme/LanguageSelectionWorld.svg";
import { ReactComponent as QrCodeIcon } from "../assets/truckpassTheme/qr-code-icon.svg";
import { ReactComponent as QrCodeOutlineSvg } from "../assets/truckpassTheme/qr-code-outline.svg";
import { ReactComponent as WelcomeBanner } from "../assets/truckpassTheme/WelcomeBanner.svg";
import { ReactComponent as ArrowUpIcon } from "../assets/truckpassTheme/arrow-up.svg";
import { ReactComponent as CameraAccessDeniedIcon } from "../assets/truckpassTheme/camera-access-denied-icon.svg";
import { ReactComponent as CheckIcon } from "../assets/truckpassTheme/check.svg";
import { ReactComponent as DocumentIconSvg } from "../assets/truckpassTheme/document.svg";
import { ReactComponent as DownloadIconSvg } from "../assets/truckpassTheme/download.svg";
import { ReactComponent as HamburgerMenuIcon } from "../assets/truckpassTheme/hamburger-menu.svg";
import { ReactComponent as ReGenerateIconSvg } from "../assets/truckpassTheme/re-generate.svg";
import { ReactComponent as VectorCollapseSvg } from "../assets/truckpassTheme/vector-collapse.svg";
import { ReactComponent as VectorExpandSvg } from "../assets/truckpassTheme/vector-expand.svg";
import { ReactComponent as VectorOutlineSvg } from "../assets/truckpassTheme/vector-icon-outline.svg";

// Truckpass-specific icons (filenames adjusted to actual assets in folder)
import { ReactComponent as WhiteScanIcon } from "../assets/truckpassTheme/white-scan-icon.svg"; // exists
// Note: white-upload.svg not present in truckpass assets — will fallback to defaultTheme
import { ReactComponent as WhiteDownloadIcon } from "../assets/truckpassTheme/white-download.svg"; // exists
import { ReactComponent as VectorDownload } from "../assets/truckpassTheme/vector-download.svg"; // exists
import { ReactComponent as VerificationSuccessIcon } from "../assets/truckpassTheme/verification-success-icon.svg"; // actual filename has -icons
import { ReactComponent as VerificationFailedIcon } from "../assets/truckpassTheme/verification-failed-icon.svg";
import { ReactComponent as SearchIcon } from "../assets/truckpassTheme/search.svg"; // renamed
import { ReactComponent as FilterLinesIcon } from "../assets/truckpassTheme/filter-lines.svg"; // renamed
import { ReactComponent as WhiteReGenerateIcon } from "../assets/truckpassTheme/white-regenerate.svg"; // renamed
import { ReactComponent as CloseIcon } from "../assets/truckpassTheme/close_icon.svg"; // underscore in filename
// new-tab-icon not present in truckpass assets; we'll keep using defaultTheme.NewTabIcon
import { ReactComponent as UnderConstruction } from "../assets/truckpassTheme/under-construction.svg";
import { ReactComponent as SharableLink } from "../assets/truckpassTheme/sharable-link.svg";

// Default imports (URL strings) — useful when components use `style={{ backgroundImage: `url(${...})` }}`
import QrCodeOutlineUrl from "../assets/truckpassTheme/qr-code-outline.svg";
import GradientScanFillIconUrl from "../assets/truckpassTheme/gradient-scan-icon.svg";
import GradientUploadIconUrl from "../assets/truckpassTheme/gradient-upload-icon.svg";
import TruckpassLogoUrl from "../assets/truckpassTheme/TruckpassVerifyLogo.svg";
import WelcomeBannerUrl from "../assets/truckpassTheme/WelcomeBanner.svg";

// fallback: reuse existing theme icons (defaultTheme) for anything still missing
import defaultTheme from "./default-theme";

const truckpassTheme = {
  // primary logos / images
  Logo: NavLogo,
  LogoUrl: TruckpassLogoUrl,
  InjiLogo: defaultTheme.InjiLogo, // still using default for Inji logo (not in truckpass assets)
  WelcomeBanner: WelcomeBanner,
  WelcomeBannerUrl,

  // scan / upload icons (both inline svg components and url strings available)
  GradientScanIcon: GradientScanFillIcon,
  GradientScanFillIconUrl: GradientScanFillIconUrl,
  WhiteScanIcon: WhiteScanIcon,

  GradientUploadIcon: GradientUploadIcon,
  GradientUploadIconUrl,
  WhiteUploadIcon: defaultTheme.WhiteUploadIcon, // no truckpass white-upload asset, fallback to default

  // QR icons / outlines
  QrIcon: QrCodeIcon,
  QrCodeOutLine: QrCodeOutlineSvg, // inline component
  QrCodeOutLineUrl: QrCodeOutlineUrl, // URL for background-image
  ScanOutline: QrCodeOutlineUrl, // many components expect ScanOutline URL for bg

  // language/world icon
  GlobeIcon: LanguageWorldIcon,

  // arrow up/down: use the new arrow-up for both up and down per your request
  ArrowUp: ArrowUpIcon,
  ArrowDown: ArrowUpIcon,

  // small icons you provided
  CameraAccessDeniedIcon: CameraAccessDeniedIcon,
  Check: CheckIcon,
  DocumentIcon: DocumentIconSvg,
  DownloadIcon: DownloadIconSvg,
  WhiteDownloadIcon: WhiteDownloadIcon, // prefer truckpass asset
  MenuIcon: HamburgerMenuIcon,
  ReGenerateIcon: ReGenerateIconSvg,

  // vector icons
  VectorUp: ArrowUpIcon,
  VectorDown: ArrowUpIcon,
  VectorOutline: VectorOutlineSvg,
  VectorExpand: VectorExpandSvg,
  VectorCollapse: VectorCollapseSvg,
  VectorDownload: VectorDownload,

  // use truckpass versions where available, else fallback to defaultTheme
  VerificationSuccessIcon: VerificationSuccessIcon,
  VerificationFailedIcon: VerificationFailedIcon,
  SearchIcon: SearchIcon,
  FilterLinesIcon: FilterLinesIcon,
  WhiteReGenerateIcon: WhiteReGenerateIcon,
  CloseIcon: CloseIcon,
  NewTabIcon: defaultTheme.NewTabIcon, // no truckpass new-tab asset
  UnderConstruction: UnderConstruction,
  SharableLink: SharableLink,
};

export default truckpassTheme;
