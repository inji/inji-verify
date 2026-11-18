import React from 'react';
import {render, screen} from "@testing-library/react";
import VcDisplayCard from "../../../../../components/Home/VerificationSection/Result/VcDisplayCard";
import {convertToTitleCase, getDisplayValue} from "../../../../../utils/misc";

jest.mock("../../../../../redux/hooks", () => ({
    useAppDispatch: jest.fn()
}))

let workingVc: any = {
    "id": "did:rcw:eb658f9b-879e-4628-8fcf-2fea22c5a522",
    "type": ["VerifiableCredential", "LifeInsuranceCredential", "InsuranceCredential"],
    "proof": {
        "type": "Ed25519Signature2020",
        "created": "2024-05-03T09:00:17Z",
        "proofValue": "z5vxkCcRt3DugiEwapFKKNuayHng4mHpLnwLKeeYSxR1eP6qVhehk59xXgi1pXvizv3JCUjavij3gkxVr7QGfCKZB",
        "proofPurpose": "assertionMethod",
        "verificationMethod": "did:web:Sreejit-K.github.io:VCTest:0243cf3c-61c7-44fa-9685-213a422ad276#key-0"
    },
    "issuer": "did:web:Sreejit-K.github.io:VCTest:0243cf3c-61c7-44fa-9685-213a422ad276",
    "@context": ["https://www.w3.org/2018/credentials/v1", "https://holashchand.github.io/test_project/insurance-context.json", {"LifeInsuranceCredential": {"@id": "InsuranceCredential"}}, "https://w3id.org/security/suites/ed25519-2020/v1"],
    "issuanceDate": "2024-05-03T09:00:17.194Z",
    "expirationDate": "2024-06-02T09:00:17.174Z",
    "credentialSubject": {
        "id": "did:jwk:eyJrdHkiOiJFQyIsInVzZSI6InNpZyIsImNydiI6IlAtMjU2Iiwia2lkIjoiS0hjMFl0MjdxUGhQUUdGbkNYb1h2UjBvOU1uaWVzWGRsNk0zamUzMUZvWSIsIngiOiJrRDBhNUQzcl84cS1tQ0JSZUNCd2dsMFd6S0FqRTdSVlVHWU53c1Z0MnNrIiwieSI6IlA3VjVtcWpSMktEeGlmMENWVm1rN0xiWklfdVEzcTFab0JlU0E1Xy1vMlkiLCJhbGciOiJFUzI1NiJ9",
        "dob": "1968-12-24",
        "email": "abhishek@gmail.com",
        "gender": "Male",
        "mobile": "0123456789",
        "benefits": ["Critical Surgery", "Full body checkup"],
        "fullName": "Abhishek Gangwar",
        "policyName": "Start Insurance Gold Premium",
        "policyNumber": "1234567",
        "policyIssuedOn": "2023-04-20T20:48:17.684Z",
        "policyExpiresOn": "2033-04-20T20:48:17.684Z"
    }
};

describe("Vc Card Display", () => {

    test("Test rendering of VC Properties", () => {
        render(<VcDisplayCard vc={workingVc}/>)
        Object.keys(workingVc.credentialSubject)
            .filter(key => key !== "id" && key !== "type")
            .forEach(key => {
                expect(screen.getByText(convertToTitleCase(key)))
                expect(screen.getByText(getDisplayValue(workingVc.credentialSubject[key])))
            })
    })
})

