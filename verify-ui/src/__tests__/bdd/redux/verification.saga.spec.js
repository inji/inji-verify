import { expectSaga } from 'redux-saga-test-plan';
import verificationSaga from "../../../redux/features/verification/verification.saga";
import verificationReducer, {verificationInit} from "../../../redux/features/verification/verification.slice";
import {VerificationSteps} from "../../../utils/config";
import '@injistack/pixelpass';
import {decode} from "@injistack/pixelpass";
import {verifyCredential} from "verification-sdk";

jest.mock('@injistack/pixelpass', () => ({
    decode: jest.fn()
}));
jest.mock('verification-sdk', () => ({
    verifyCredential: jest.fn()
}))

let encodedWorkingVc = "NCFK.LIXROSK042$4CT0PXY0CS07EO% 4 YROM2WGG3P6.PJTF2N20I 3/XHBJ564O8WS1M6TNR3.N2EV36U3/L+0COKH:QBTS2*P1I$6%BPBT6$-G1VE+M4I97ZOAMG3W49Q92W00QJ1+003-21CUIQKK3G%70JT6R76.LPI82WH9%CCXSQQQ6%ZT+CD/T5.7K79MK:7HZIKZAZ$KN266SOKB0NYPS1D %H EP3-8DZ0KDE 86MC7F/F6EO8X6%:QD:4T.C.OJ22ACSPX%9EYUJ 2QMBGM84293.RF7SPYEUKO-FFJZSMFM+%5IWMF2M6ZPK7V+9I.7SUBA9FV+1CWI8/%PMX3:QCWCRKRF7$R+UJ%8KXX7H%HXK7ZYDEEMU4TSWNWBV7BVRVLC.8CLGVL8.6L*KTW0K 5B88V8JP*HVYHU OPS81+3IIMI 4MLP1JRP$$67TQY*133A2OS1BFSEG$K0%NC-Y9LXQNBKGK3LV5/ET%P6W8GYV9X4G-YKR 6B0VSHKYUQ6AV2YAK2PVCA3LEVJS7PF67Q32PC-HL0MV0FJDM9LU9CD%WE4*756M*0U1IEK$B6TA TDA IAF2KD3MHEU5WVC6UWIE-7X9U*FLP.8Y$M3L9ZUS728U82+VBU74LJ5ZWVVNR1L65-7QTDQIDH3T3DLXILL%7W2E$GLKL4:KV.ZRTWCV9USEL.DFFRTDNA%W8YX6*7L4+VPM7% 6ITJ9NE5.FZ3PVQ8WUB87WSMOU+5OE9FHCQI7P4F3VMRJTG0QJQ15N75Y7L6JL/NKDTG:TRY29*V 3F804-DD8C4-UM IB41AZIAZHJ -9:02SG55IUXYNSP6T29615E3A57V$5O3QO2CEO0M$J3NXQQGMU6IKBO$2EGKRV:IOOR/DVL+ON:3465OPJ+ZQDTGTNU92O47PL27QF2KY7YQVALU8QFW$3B4ECVP/SV*PET5JW048Q7FH6 U6U8DLQF-1P9WCJ03PSO4$0/3QV:T28NU+3T8CRS89M9LLDF-RW+25SE6DU/T7LBNKX9M4MZPTM7WF/L2VEQJ2AVOV25AXKJ33WX3PFO8L6R64%:S$ GMZDTMIN2PQ46DYAULIRD5-81.:0EU8VQH4$K6*V9X9T:G0%H7OAEW6OX4155AT4BOE2UGL/OFIS7Q50YLYMT-VJ7TGCTO$000P04 D.IKLV8Q*G.%L73UCHF LG0%R$%4H5OKD5DAQ05IP1P*.3P 1W AE01+VH0ZMX-7LZDE2K:1K- F/4R$6ER*BM-8CIKM1LRWUVADQC7/S6-5L1QJBYM7JP-GDPMC3TD5$TM7OI5CI5";
let workingVc = {
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
let successVcStatus = {
    "status": "OK",
    "checks": [
        {
            "active": null,
            "revoked": "OK",
            "expired": "OK",
            "proof": "OK"
        }
    ]
}

let encodedInvalidVc = "NCF3-LMEBQSK +2LN5-$PO9L+-CEYAW*H%K7:AI9W0.33QF2J1MB2P-CC4WPELT$XB83GM69*EVAA9Y77ZCGP:F.-7+$U8P4TUITR8EJC- UWF80AT9$CLMFFG6B6KIX67SFG:GZWGP B%B6*.9 G0RQVOZT4.N9LQ-OUB91CVT*ZIU6A4TL9E2$JQUR9SSTPLQLW66IV2OTE1CSZA22JMNP:9U9DV8843$BCPEZ97Y%L RT-50W530EF6IMWG30D3SPRQCLV*C3P8620NKK+JTJJ4ZFCNL3*-U1LN8C7MW091CNVK1E8DP7XSUWUHZZJEY792VZ-EE+74CJ$DC/0K7.U65S095V6GP454NUPTF5N72RTC87/3PL:MQ67%LS$J7$3B-26**TUPKX3MJQPXV9.LJ:QL/XK9 J %HD96UYFYEVGKRQPS7 B4OK3A2YLI.B62CBB+3+A9:6F:M8O-53DL1CWMO02O4P%5QU43BQXX7OETQAL8TA$GD2XIS5QK6ADHL7+I/ZBZYIOXCEZDQ$4S2OCOI5OT+WVV6KWBDJ.1Q$2FWEUAP$3P*NB8UHRBP63T/8E%04$WVXALKLVRFP.W1/IF Y1Q8LP1H2 5QNUIA3AKPOS8 /7UZ71Z9MXFTEERK274A1V83VL.9VHGR$IPZQS9JF2LP8R08FJ4DQCPF2RC9/0C*V$U4Q7855IBIHHZAFCS XUA5NNWAA38NE55*F$/R$QF0:2WZ8AA9F1VF KZTDM2SIJB%OM$8J Z3R/84RN//SCY55X7R9JYXCTTDNQQZ2EGVRE.D 5EMA7ZVJ+ 7%K9 3F+ZL.RIBD2HTKHVAYF9K O8G75WLIQI$COEV65-NA:NOO5C50Z*V4JQUWHAKSW4U/K8-F4100JJ8P*7*4I5H6Z.D4*OTP7XWQQ11QS42TNM UJXODLQNK9-EOVXVN3JJ 3C26IU5LK5S%HZG5J 54F3KCDTZ0MA8PA6VJB.TALIT/LM3IBRH1-UAGQSICMI*F-*5X06.JMZZBOR0C7STNESXD5$B+N2H/9KJH2TOUYK*UBRU0$:D -DFYBB/RAMTS2P%M9GQM0042XC1M1XJ5.LKY020WNF/S$/LCX2COU*%DS4P MR+L52U2K-QH4WBTRF12SQ5::S9PF.WGVP7LQJBU5CKCXFD7Q4EKIX:OOKB-I9O1HM7RD%09HQH2BE6ET1O2QA9GPLUIQ2IFNBB83Z%O4R7GG862VTN6A.PA6DXF22.JZQFL-NPHOIKM/9S6-R59F+7JCLNLO8WKA094BK6MIOE7R.DPE 0Q12 ISKX1TNK/3M4CUP N9%TCCSY3OT%T3GQOS8AWR.AJ+E88R5R.UYYTWYNZ54GGT.+2OUVZOG+W83Q8U.LXQ9SXQPQI*ANI5GG2OM52V:RKKTI5TEYEH-61X8/LAT76291B1J%+OJY2*%U.57/7BTKB7TRJW3V EB78RWD9-3HNU 7I7W5A+OZ51+IAPR0383P$C.ZQJE1DF9/TV/YDGFCKLO/Z4*:BBK76C86 LWB38%T93SB$UJVDHAP*65-7DR*Q8J7CVR-M83ZG+T325J$W19V5DCL9XIZU1.-RITGF 4%HD.EK$ENAF1*DAYU9CO4:OLCOQT9ET.A+:9T/DC:I30RVOIK$H2MU.D1$JT9MV5WO.YP7%NCQL1VE2UFPX04.P8XU2-AM%NCOL1Z0-:UL21Q14K33X7M %D5P2:5C5LKQDEZ/AP:8/PPK8GRX7D:G836UTQ46WDOSVU2*:SAWIWWEV573MK.T6/ E*HS*NM7+8A4O650JAPY5M:KR.IE$4WS2P14MCZ5P/7XB0B$C61DV30E00NUDDSUE2QK97JXR$ U/IFZ1G/ZCHHPOBAT5UT%TN73EDTVN1TE7JK1%NHMOT8ALNEE5:G9A2SOI%83Z E0CE6CMJ+SA/6$ZV9-DSXAI168 L5-25 L%TT+VEC*71MU:9Q*N70$IQZ1+%C72EZF1.N5LXKX5CZI1KJ548JDYN%8OXYB78S4TQMCT:8F0$5+HM63SE9A I2 FU*FMG:JNKUDHCDVF-EO+OBN$KCGL:46/4GT/V-9PB001EK$1";
let invalidVc = {
    "id": "did:rcw:4b9f26f7-a356-4347-bcdb-5dcd6ef52059",
    "type": ["VerifiableCredential", "InsuranceCredential"],
    "proof": {
        "type": "Ed25519Signature2020",
        "created": "2024-03-26T10:00:56Z",
        "proofValue": "z3FHZma1nUJKeEry1TufZoAtUPMCJ9arDZuGUfuUc4bjogK5PzSajRVqK24nv4KQMP3HWgZsSN4Lhm1b7j6QpLKm6",
        "proofPurpose": "assertionMethod",
        "verificationMethod": "did:web:holashchand.github.io:test_project:32b08ca7-9979-4f42-aacc-1d73f3ac5322#key-0"
    },
    "issuer": "did:web:holashchand.github.io:test_project:32b08ca7-9979-4f42-aacc-1d73f3ac5322",
    "@context": ["https://www.w3.org/2018/credentials/v1", "https://holashchand.github.io/test_project/insurance-context.json", "https://w3id.org/security/suites/ed25519-2020/v1"],
    "issuanceDate": "2024-03-26T10:00:56.039Z",
    "expirationDate": "2024-04-25T10:00:56.024Z",
    "credentialSubject": {
        "id": "did:jwk:eyJrdHkiOiJSU0EiLCJlIjoiQVFBQiIsInVzZSI6InNpZyIsImFsZyI6IlJTMjU2IiwibiI6IndpSFVQVER5cFpnSlE0Uk1FQzFlSDNlNWROTDV5d1hHbC1OeVZQdWM0V0dlazk3YzREcmFGVTJudTZxb0U1V3NCbmhuUUs4ZjVFWTNzYUljQ1RSbHBlLUVsMVp0UGRLbUlGWE1RWWlUV2tlY3N1Mjh2NG1kb013V2NXUlFTRjBRTlBhSUN4dWxzSUl6MHAxVldnUjRfVmdSSlliN0x0bmFtNmlNU1NIVzFacHB3eGZTWjVIOWVLZ0dzbXZvTUlDRDRxU3hBaHpsMlkxc2FpbEZSTTBDTGRUWGJjcVIxdVZGOExjckhIUUwtalVzVlZKNGtTZlRRbHBDVy1ubEJCNzFJT2ljc2lrcVQ5RDI5REN1UjlYRVVZZTB6ck9GMDNVakgxQUpPM0JzUl9ySUt6cnZBNFBrb3ZZcHVmaFdVUU1rcEk4a0h0eE0xcEZaemlCcTNHSmdOMTFZTERJbW5TRVFJNXFydV9ab25wLXVPd0dZdzduWW5xZ0xwdVE3ODMweTZfMkVldzA1T2VRVmNuTTByMGZ6aHctRGR2Q1hmV01GMUdHRnA5Y0Y4U2stR3J6OUt5dFBQVHR5eThCQU0ybEdCY1JodHd0OEFZaEMzT2RNVnNTQUFTbzk1OGdQSkNMNXl4azNaRkJyUHNIVHItMFlReURBMGZ3MTNtVE5id3BNVnBNZWxtVWZ4V2hjWjUtcXNDVXlET0o4c3RnakZZRFlmR1N3WEtOUWg3aTZ2cG0tZ2pIZV9JRXhOeWN1SU9QU1Qtcl9oZ3NBMkJQSWFyTV9YU3RhOGprb3RjTk10azh5U1pLdDJjZ08xcXpzUndCaTRuRmRWRE5mREFna21ZTTBkdS1CcFlqZVZsSXgweC1FcGxXUWpGcURxcDl0UmYtcDVVQ1VfdzgycHNNIn0=",
        "dob": "2000-01-26",
        "email": "tgs@gmail.com",
        "gender": "Male",
        "mobile": "0123456789",
        "benefits": ["Critical PSUT", "Hepatitas PSUT"],
        "fullName": "TGSStudio",
        "policyName": "Sunbird Insurenc Policy",
        "policyNumber": "55555",
        "policyIssuedOn": "2023-04-20T20:48:17.684Z",
        "policyExpiresOn": "2033-04-20T20:48:17.684Z"
    }
}
let failureVcStatus = {
    "status": "NOK",
    "checks": [
        {
            "active": null,
            "revoked": "OK",
            "expired": "NOK",
            "proof": "NOK"
        }
    ]
}

describe("Verification Saga", () => {
    test("Successful VC Verification", () => {
        decode.mockImplementation(() => {
            return workingVc;
        });
        verifyCredential.mockImplementation(() => {
            return successVcStatus;
        });

        expectSaga(verificationSaga)
            .withReducer(verificationReducer)
            .dispatch(verificationInit({qrReadResult: {qrData: encodedWorkingVc, status: "SUCCESS"}, flow: "SCAN"}))
            .hasFinalState({
                qrReadResult: {status: "SUCCESS", qrData: encodedWorkingVc},
                flow: "SCAN",
                activeScreen: VerificationSteps[method].DisplayResult,
                verificationResult: {
                    vc: workingVc,
                    vcStatus: successVcStatus
                },
                alert: {message: "QR code uploaded successfully!", severity: "success", autoHideDuration: 1200, open: true}
            })
            .run();
    });

    test("Invalid VC", () => {
        decode.mockImplementation(() => {
            return invalidVc;
        });
        verifyCredential.mockImplementation(() => {
            return failureVcStatus;
        });

        expectSaga(verificationSaga)
            .withReducer(verificationReducer)
            .dispatch(verificationInit({qrReadResult: {qrData: encodedInvalidVc, status: "SUCCESS"}, flow: "SCAN"}))
            .hasFinalState({
                qrReadResult: {status: "SUCCESS", qrData: encodedInvalidVc},
                flow: "SCAN",
                activeScreen: VerificationSteps[method].DisplayResult,
                verificationResult: {
                    vc: invalidVc,
                    vcStatus: failureVcStatus
                },
                alert: {message: "QR code uploaded successfully!", severity: "success", autoHideDuration: 1200, open: true}
            })
            .run();
    });

    test("Decoding fails", () => {
        const error = new Error('Verification failed');
        decode.mockImplementation(() => {
            throw error;
        });
        verifyCredential.mockImplementation(() => {
            return {};
        });

        expectSaga(verificationSaga)
            .withReducer(verificationReducer)
            .dispatch(verificationInit({qrReadResult: {qrData: "sample unencoded data", status: "SUCCESS"}}))
            .hasFinalState({
                qrReadResult: {status: "NOT_READ"},
                flow: "TO_BE_SELECTED",
                activeScreen: VerificationSteps[method].QrCodePrompt,
                verificationResult: {vc: undefined, vcStatus: undefined},
                alert: {message: "QR code format is not supported.", severity: "error", open: true}
            })
            .run();
    })
})
