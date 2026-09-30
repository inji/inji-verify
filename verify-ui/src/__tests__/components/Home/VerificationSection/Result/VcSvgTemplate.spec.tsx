import React from "react";
import { render, screen, waitFor } from "@testing-library/react";
import VcSvgTemplate from "../../../../../components/Home/VerificationSection/Result/VcSvgTemplate";
import { fetchSvgTemplate } from "../../../../../utils/svg-template-utils";
import QRCode from "qrcode";
import Mustache from "mustache";

jest.mock("../../../../../utils/svg-template-utils", () => ({
  fetchSvgTemplate: jest.fn(),
}));

jest.mock("qrcode", () => ({
  __esModule: true,
  default: { toDataURL: jest.fn(() => Promise.resolve("data:image/png;base64,qr")) },
}));

jest.mock("mustache", () => ({
  __esModule: true,
  default: { render: jest.fn((template: string) => template) },
}));

jest.mock("../../../../../components/commons/Loader", () => ({
  __esModule: true,
  default: () => <div data-testid="template-loader" />,
}));

const mockedFetch = fetchSvgTemplate as jest.MockedFunction<typeof fetchSvgTemplate>;
const mockedQr = QRCode.toDataURL as jest.MockedFunction<typeof QRCode.toDataURL>;
const mockedMustache = Mustache.render as jest.MockedFunction<typeof Mustache.render>;

describe("VcSvgTemplate", () => {
  const vc = { credentialSubject: { name: "Sandra" } } as any;

  beforeEach(() => {
    jest.clearAllMocks();
    mockedQr.mockResolvedValue("data:image/png;base64,qr");
    mockedMustache.mockImplementation((template: string, view: any) =>
      template.replace("{{credentialSubject.name}}", view.credentialSubject.name)
    );
  });

  test("renders a fetched and sanitized SVG template", async () => {
    mockedFetch.mockResolvedValue('<svg><text>{{/credentialSubject/name}}</text><script>alert(1)</script></svg>');

    render(<VcSvgTemplate vc={vc} templateUrl="https://example.test/template.svg" />);

    await waitFor(() => expect(screen.getByText("Sandra")).toBeInTheDocument());
    expect(screen.queryByText("alert(1)")).not.toBeInTheDocument();
    expect(mockedFetch).toHaveBeenCalledWith("https://example.test/template.svg");
  });

  test("returns no content when a template URL is not provided", () => {
    const { container } = render(<VcSvgTemplate vc={vc} templateUrl="" />);
    expect(container.firstChild).toBeNull();
    expect(mockedFetch).not.toHaveBeenCalled();
  });

  test("reports template loading failures", async () => {
    const onError = jest.fn();
    mockedFetch.mockResolvedValue("");

    render(<VcSvgTemplate vc={vc} templateUrl="broken.svg" onError={onError} />);

    await waitFor(() => expect(onError).toHaveBeenCalledWith(expect.any(Error)));
  });

  test("reports fetch exceptions", async () => {
    const onError = jest.fn();
    mockedFetch.mockRejectedValue(new Error("network failure"));

    render(<VcSvgTemplate vc={vc} templateUrl="offline.svg" onError={onError} />);

    await waitFor(() => expect(onError).toHaveBeenCalledWith(expect.any(Error)));
  });

  test("reports rendering exceptions", async () => {
    const onError = jest.fn();
    mockedFetch.mockResolvedValue("<svg>{{name}}</svg>");
    mockedMustache.mockImplementationOnce(() => {
      throw new Error("invalid template");
    });

    render(<VcSvgTemplate vc={vc} templateUrl="invalid.svg" onError={onError} />);

    await waitFor(() => expect(onError).toHaveBeenCalledWith(expect.any(Error)));
  });
});
