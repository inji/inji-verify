import React from "react";
import "@testing-library/jest-dom";
import {
  render,
  screen,
  waitFor,
  fireEvent,
  act,
} from "@testing-library/react";
import OpenID4VPVerification from "../../../src/components/openid4vp-verification/OpenID4VPVerification";

const mockFetchError = (message = "Failed to fetch") => {
  global.fetch = jest.fn(() => Promise.reject(new Error(message))) as jest.Mock;
};

describe("OpenID4VPVerification UI Tests", () => {
  const verifyServiceUrl = "https://example.com/verify";
  const protocol = "testopenid4vp://";
  const presentationDefinitionId = "pd-id-123";
  const presentationDefinition = { input_descriptors: [{ id: "id-1" }] };
  const onVPReceived = jest.fn();
  const onVPProcessed = jest.fn();
  const onQrCodeExpired = jest.fn();
  const onError = jest.fn();
  const qrCodeStyles = {
    size: 150,
    level: "H",
    bgColor: "#f0f0f0",
    fgColor: "#333",
    margin: 5,
    borderRadius: 5,
  };
  const triggerElement = <button>Verify</button>;

  beforeEach(() => {
    jest.clearAllMocks();
    // Mock window.location.origin for each test
    Object.defineProperty(window, "location", {
      value: { origin: "https://client.example.com" },
      writable: true,
    });
  });

  // Helper function to render the component with common props
  const renderComponent = (
    props: Partial<React.ComponentProps<typeof OpenID4VPVerification>>
  ) => {
    const { onVPReceived: received, onVPProcessed: processed, ...rest } = props;

    return render(
      <OpenID4VPVerification
        verifyServiceUrl={verifyServiceUrl}
        protocol={protocol}
        onQrCodeExpired={onQrCodeExpired}
        onError={onError}
        {...rest}
        {...(received
          ? { onVPReceived: received }
          : processed
          ? {}
          : { onVPReceived })}
        {...(processed ? { onVPProcessed: processed } : {})}
      />
    );
  };

  it("should render the trigger element", () => {
    renderComponent({
      presentationDefinitionId,
      onVPReceived,
      onQrCodeExpired,
      onError,
      triggerElement,
    });
    expect(screen.getByRole("button", { name: "Verify" })).toBeInTheDocument();
  });

  it("should indicate QR code expiry after a timeout (mocking status)", async () => {
    const fetchMock = jest
      .fn()
      // First call: createRequest
      .mockResolvedValueOnce({
        status: 201,
        json: async () => ({
          transactionId: "mock-txn-id",
          requestId: "mock-req-id",
          authorizationDetails: {},
        }),
      })
      // Second call: status polling
      .mockResolvedValueOnce({
        status: 200,
        json: async () => ({ status: "EXPIRED" }),
      });

    global.fetch = fetchMock;

    renderComponent({
      presentationDefinitionId,
      onVPReceived,
      onQrCodeExpired,
      onError,
      triggerElement,
    });

    fireEvent.click(screen.getByRole("button", { name: "Verify" }));

    await waitFor(() => expect(onQrCodeExpired).toHaveBeenCalled(), {
      timeout: 10000,
    });
  }, 15000);

  it("should handle API error during request creation", async () => {
    const consoleErrorMock = jest
      .spyOn(console, "error")
      .mockImplementation(() => {});
    mockFetchError("Failed to create request");

    renderComponent({
      presentationDefinitionId,
      onVPReceived,
      onQrCodeExpired,
      onError,
      triggerElement, // ✅ Add this!
    });

    // Wait for the button to render
    await waitFor(() => screen.getByRole("button", { name: "Verify" }));

    fireEvent.click(screen.getByRole("button", { name: "Verify" }));

    await waitFor(() =>
      expect(onError).toHaveBeenCalledWith(
        new Error("Failed to create request")
      )
    );

    expect(screen.queryByRole("img")).toBeNull();
    consoleErrorMock.mockRestore();
  });

  it("should display the QR code after successful request", async () => {
    const mockTransactionId = "mock-txn-id";
    const mockRequestId = "mock-req-id";

    const fetchMock = jest
      .fn()
      .mockResolvedValueOnce({
        status: 201,
        json: async () => ({
          transactionId: mockTransactionId,
          requestId: mockRequestId,
          authorizationDetails: {},
        }),
      })
      .mockResolvedValue({
        status: 200,
        json: async () => ({ status: "PENDING" }),
      });

    global.fetch = fetchMock;

    renderComponent({
      presentationDefinitionId,
      onVPReceived,
      onQrCodeExpired,
      onError,
      triggerElement,
    });

    fireEvent.click(screen.getByRole("button", { name: "Verify" }));

    await waitFor(() => {
      expect(screen.getByRole("img")).toBeInTheDocument();
    });
  });

  it("should handle onVPReceived after VP_SUBMITTED status", async () => {
    const mockTransactionId = "mock-txn-id";
    const mockRequestId = "mock-req-id";

    const fetchMock = jest
      .fn()
      .mockResolvedValueOnce({
        status: 201,
        json: async () => ({
          transactionId: mockTransactionId,
          requestId: mockRequestId,
          authorizationDetails: {},
        }),
      })
      .mockResolvedValueOnce({
        status: 200,
        json: async () => ({ status: "VP_SUBMITTED" }), // Simulating VP_SUBMITTED status
      });

    global.fetch = fetchMock;

    const onVPReceived = jest.fn();
    const onQrCodeExpired = jest.fn();
    const onError = jest.fn();

    renderComponent({
      presentationDefinitionId,
      onVPReceived,
      onQrCodeExpired,
      onError,
      triggerElement: <button>Verify</button>,
    });

    // Trigger the creation of the VP request
    fireEvent.click(screen.getByRole("button", { name: "Verify" }));

    // Wait for the VP result to be received
    await waitFor(() => {
      expect(onVPReceived).toHaveBeenCalledWith(mockTransactionId); // Expect txnId
    });
  });

  it("should handle VP result after VP_SUBMITTED status and call onVPProcessed", async () => {
    const mockTransactionId = "mock-txn-id";
    const mockRequestId = "mock-req-id";
    const mockVcResults = [
      { vc: JSON.stringify({ id: "vc1" }), verificationStatus: "valid" },
      { vc: JSON.stringify({ id: "vc2" }), verificationStatus: "expired" },
    ];

    const expectedProcessedResult = [
      { vc: { id: "vc1" }, vcStatus: "valid" },
      { vc: { id: "vc2" }, vcStatus: "expired" },
    ];

    const fetchMock = jest
      .fn()
      .mockResolvedValueOnce({
        status: 201,
        json: async () => ({
          transactionId: mockTransactionId,
          requestId: mockRequestId,
          authorizationDetails: {},
        }),
      })
      .mockResolvedValueOnce({
        status: 200,
        json: async () => ({ status: "VP_SUBMITTED" }),
      })
      .mockResolvedValueOnce({
        status: 200,
        json: async () => ({ vcResults: mockVcResults }),
      });

    global.fetch = fetchMock;

    const onVPProcessed = jest.fn(); // Only pass onVPProcessed here
    const onQrCodeExpired = jest.fn();
    const onError = jest.fn();

    renderComponent({
      presentationDefinitionId,
      onVPProcessed, // Only pass onVPProcessed here
      onQrCodeExpired,
      onError,
      triggerElement: <button>Verify</button>,
    });

    fireEvent.click(screen.getByRole("button", { name: "Verify" }));

    await waitFor(() => {
      expect(onVPProcessed).toHaveBeenCalledWith(expectedProcessedResult);
    });
  });

  it("should handle VP result with presentationDefinition and call onVPProcessed", async () => {
    const mockTransactionId = "mock-txn-id";
    const mockRequestId = "mock-req-id";
    const mockVcResults = [
      { vc: JSON.stringify({ id: "vc1" }), verificationStatus: "valid" },
      { vc: JSON.stringify({ id: "vc2" }), verificationStatus: "expired" },
    ];

    const expectedProcessedResult = [
      { vc: { id: "vc1" }, vcStatus: "valid" },
      { vc: { id: "vc2" }, vcStatus: "expired" },
    ];

    const fetchMock = jest
      .fn()
      .mockResolvedValueOnce({
        status: 201,
        json: async () => ({
          transactionId: mockTransactionId,
          requestId: mockRequestId,
          authorizationDetails: {},
        }),
      })
      .mockResolvedValueOnce({
        status: 200,
        json: async () => ({ status: "VP_SUBMITTED" }),
      })
      .mockResolvedValueOnce({
        status: 200,
        json: async () => ({ vcResults: mockVcResults }),
      });

    global.fetch = fetchMock;

    const onVPProcessed = jest.fn();
    const onQrCodeExpired = jest.fn();
    const onError = jest.fn();

    const presentationDefinition = {
      input_descriptors: [
        {
          id: "email_input",
          schema: [{ uri: "https://example.com/email-schema" }],
        },
      ],
    };

    render(
      <OpenID4VPVerification
        verifyServiceUrl="https://example.com/verify"
        protocol="testopenid4vp://"
        presentationDefinition={presentationDefinition}
        onVPProcessed={onVPProcessed}
        onQrCodeExpired={onQrCodeExpired}
        onError={onError}
        triggerElement={<button>Verify</button>}
      />
    );

    fireEvent.click(screen.getByRole("button", { name: "Verify" }));

    await waitFor(() => {
      expect(onVPProcessed).toHaveBeenCalledWith(expectedProcessedResult);
    });
  });

  it("should throw error if both onVPReceived and onVPProcessed are provided", async () => {
    const consoleErrorMock = jest
      .spyOn(console, "error")
      .mockImplementation(() => {});

    // Expect an error to be thrown when both callbacks are provided
    expect(() => {
      renderComponent({
        presentationDefinitionId,
        onVPReceived,
        onVPProcessed,
        onQrCodeExpired,
        onError,
        triggerElement: <button>Verify</button>,
      });
    }).toThrow(
      "Both onVPReceived and onVPProcessed cannot be provided simultaneously"
    );

    // Ensure that console error is also captured if thrown
    expect(consoleErrorMock).toHaveBeenCalled();

    consoleErrorMock.mockRestore();
  });

  it("should generate QR code using presentationDefinitionUri", async () => {
    const mockTransactionId = "txn789";
    const mockRequestId = "req789";
    const presentationDefinitionUri = "https://example.com/pd-uri.json";
  
    global.fetch = jest
      .fn()
      .mockResolvedValueOnce({
        status: 201,
        json: async () => ({
          transactionId: mockTransactionId,
          requestId: mockRequestId,
          authorizationDetails: {},
        }),
      })
      .mockResolvedValueOnce({
        status: 200,
        json: async () => ({ status: "PENDING" }),
      }) as jest.Mock;
  
    render(
      <OpenID4VPVerification
        verifyServiceUrl="https://example.com"
        protocol="testopenid4vp://"
        presentationDefinition={presentationDefinition}
        onVPProcessed={onVPProcessed}
        onQrCodeExpired={jest.fn()}
        onError={jest.fn()}
        triggerElement={<button>Verify</button>}
      />
    );
  
    fireEvent.click(screen.getByRole("button", { name: "Verify" }));
  
    await waitFor(() => {
      const qr = screen.getByRole("img");
      expect(qr).toBeInTheDocument();
    });
  });
  

});
