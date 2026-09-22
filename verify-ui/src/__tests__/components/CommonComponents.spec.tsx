import React from "react";
import { fireEvent, render, screen } from "@testing-library/react";
import Loader from "../../components/commons/Loader";
import LoaderWithBackdrop from "../../components/commons/LoaderWithBackdrop";
import PreloadImages from "../../components/commons/PreloadImages";
import { QrCode } from "../../components/commons/QrCode";
import ActionButton from "../../components/Home/VerificationSection/commons/ActionButton";

const mockDispatch = jest.fn();
const mockNavigate = jest.fn();

jest.mock("../../utils/theme-utils", () => ({
  QrCodeOutLine: "outline.png",
  VectorOutline: "vector.png",
}));

jest.mock("../../components/Home/VerificationSection/Result/ResultSummary", () => ({
  __esModule: true,
  default: ({ status }: { status: string }) => <div data-testid="result-summary">{status}</div>,
}));

jest.mock("qrcode.react", () => ({
  QRCodeSVG: ({ value }: { value: string }) => <div data-testid="qr-code">{value}</div>,
}));

jest.mock("../../redux/hooks", () => ({
  useAppDispatch: () => mockDispatch,
}));

jest.mock("react-router-dom", () => ({
  ...jest.requireActual("react-router-dom"),
  useNavigate: () => mockNavigate,
}));

jest.mock("react-i18next", () => ({
  useTranslation: () => ({ t: (key: string) => key }),
}));

describe("common UI components", () => {
  beforeEach(() => {
    mockDispatch.mockClear();
    mockNavigate.mockClear();
  });

  test("renders loader and backdrop loader", () => {
    const { rerender, container } = render(<Loader />);
    expect(container.querySelector("#loader")).toBeInTheDocument();
    rerender(<LoaderWithBackdrop />);
    expect(container.querySelector("#loader")).toBeInTheDocument();
  });

  test("preloads all provided images", () => {
    render(<PreloadImages imageUrls={["one.png", "two.png"]} />);
    expect(screen.getAllByRole("img")).toHaveLength(2);
  });

  test("renders a QR code and footer", () => {
    render(<QrCode data="payload" size={200} title="Scan" footer="Footer" status="ACTIVE" />);
    expect(screen.getByTestId("qr-code")).toHaveTextContent("payload");
    expect(screen.getByText("Footer")).toBeInTheDocument();
  });

  test("renders expired QR state", () => {
    render(<QrCode data="payload" size={200} title="Scan" footer="Footer" status="EXPIRED" />);
    expect(screen.getByTestId("result-summary")).toHaveTextContent("TIMEOUT");
  });

  test("calls the action button handler", () => {
    const onClick = jest.fn();
    render(<ActionButton label="Download" onClick={onClick} icon={<span />} positionClasses="bottom-0" />);
    fireEvent.click(screen.getByRole("button"));
    expect(onClick).toHaveBeenCalledTimes(1);
  });
});
