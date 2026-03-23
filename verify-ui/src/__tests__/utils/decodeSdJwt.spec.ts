import { decodeSdJwtToken } from "../../utils/decodeSdJwt";
import { decodeSdJwt, getClaims } from "@sd-jwt/decode";

jest.mock("@sd-jwt/decode", () => ({
    decodeSdJwt: jest.fn(),
    getClaims: jest.fn(),
}));

jest.mock("@sd-jwt/crypto-browser", () => ({
    digest: jest.fn(),
}));

describe("decodeSdJwtToken", () => {
    beforeEach(() => {
        jest.clearAllMocks();
    });

    test("should decode SD-JWT and separate claims", async () => {
        const mockSdJwt = "mock.sd.jwt";
        const mockDecoded = {
            disclosures: [
                { key: "given_name", value: "John" },
                { key: "family_name", value: "Doe" },
                { value: "hidden_value" }, // No key, should be ignored
            ],
            jwt: {
                payload: {
                    iss: "issuer",
                    sub: "subject",
                    credentialSubject: {
                        degree: "Bachelor",
                    }
                }
            }
        };

        (decodeSdJwt as jest.Mock).mockResolvedValue(mockDecoded);
        (getClaims as jest.Mock).mockResolvedValue({
            iss: "issuer",
            sub: "subject",
            given_name: "John",
            family_name: "Doe",
            degree: "Bachelor",
        });

        const result = await decodeSdJwtToken(mockSdJwt);

        expect(result.disclosedClaims).toEqual({
            given_name: "John",
            family_name: "Doe",
        });

        expect(result.regularClaims).toEqual({
            iss: "issuer",
            sub: "subject",
            degree: "Bachelor",
        });
    });

    test("should handle credentialSubject regular claims", async () => {
        (decodeSdJwt as jest.Mock).mockResolvedValue({
            disclosures: [],
            jwt: { payload: {} }
        });
        (getClaims as jest.Mock).mockResolvedValue({
            credentialSubject: {
                foo: "bar"
            },
            normal: "value"
        });

        const result = await decodeSdJwtToken("token");

        expect(result.regularClaims).toEqual({
            foo: "bar",
            normal: "value"
        });
    });

    test("should handle errors in disclosures", async () => {
        const mockDecoded = {
            disclosures: [
                null, // Will cause error when accessing .key
                { key: "valid", value: "val" }
            ],
            jwt: { payload: {} }
        };

        (decodeSdJwt as jest.Mock).mockResolvedValue(mockDecoded);
        (getClaims as jest.Mock).mockResolvedValue({
            valid: "val"
        });

        const result = await decodeSdJwtToken("token");

        expect(result.disclosedClaims).toEqual({
            valid: "val",
        });
    });
});
