import { vpSessionRequest } from "../../src/utils/api";

describe("vp request api errors", () => {
  beforeEach(() => {
    global.fetch = jest.fn();
  });

  it("vpSessionRequest includes responseCodeValidationRequired when requested", async () => {
    (global.fetch as jest.Mock).mockResolvedValue({
      status: 201,
      json: async () => ({ requestId: "req-1", transactionId: "txn-1" }),
    });

    await vpSessionRequest(
      "https://verify.example.com",
      { credentials: [{ id: "id-1", format: "dc+sd-jwt", meta: {} }] },
      "client-1",
      undefined,
      true,
    );

    expect(global.fetch).toHaveBeenCalledTimes(1);
    const [, options] = (global.fetch as jest.Mock).mock.calls[0];
    const body = JSON.parse(options.body);
    expect(body.responseCodeValidationRequired).toBe(true);
    expect(body.clientId).toBe("client-1");
  });

  it("vpSessionRequest omits responseCodeValidationRequired for cross-device", async () => {
    (global.fetch as jest.Mock).mockResolvedValue({
      status: 201,
      json: async () => ({ requestId: "req-1", transactionId: "txn-1" }),
    });

    await vpSessionRequest(
      "https://verify.example.com",
      { credentials: [{ id: "id-1", format: "dc+sd-jwt", meta: {} }] },
      "client-1",
    );

    const [, options] = (global.fetch as jest.Mock).mock.calls[0];
    const body = JSON.parse(options.body);
    expect(body.responseCodeValidationRequired).toBeUndefined();
  });

  it("vpSessionRequest surfaces verify-service 400 error body", async () => {
    (global.fetch as jest.Mock).mockResolvedValue({
      status: 400,
      json: async () => ({
        errorCode: "dcql_query.credentials",
        errorMessage:
          "Credential id must contain only alphanumeric characters, underscores, and hyphens.",
      }),
    });

    await expect(
      vpSessionRequest(
        "https://verify.example.com",
        { credentials: [{ id: "bad id", format: "dc+sd-jwt", meta: {} }] },
        "client-1",
      ),
    ).rejects.toEqual({
      errorCode: "dcql_query.credentials",
      errorMessage:
        "Credential id must contain only alphanumeric characters, underscores, and hyphens.",
    });
  });
});
