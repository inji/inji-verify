package api;

import static org.testng.Assert.assertEquals;
import static org.testng.Assert.assertFalse;
import static org.testng.Assert.assertTrue;

import org.testng.annotations.AfterMethod;
import org.testng.annotations.Test;

public class VerifiableClaimsConfigManagerTest {

    @AfterMethod
    public void tearDown() {
        VerifiableClaimsConfigManager.resetForTests();
    }

    @Test
    public void essentialNameUsesEssentialClaimWhenDcqlIdIsReusedByCombinedClaim() throws Exception {
        VerifiableClaimsConfigManager.loadFromJsonForTests("""
                {
                  "verifiableClaims": [
                    {
                      "name": "MOSIP ID",
                      "essential": true,
                      "dcqlQuery": {
                        "credentials": [
                          { "id": "mosip_verifiable_credential_id", "format": "ldp_vc", "meta": {} }
                        ]
                      }
                    },
                    {
                      "name": "MOSIP ID + Life Insurance",
                      "dcqlQuery": {
                        "credentials": [
                          { "id": "mosip_verifiable_credential_id", "format": "ldp_vc", "meta": {} },
                          { "id": "life_insurance_credential_id", "format": "ldp_vc", "meta": {} }
                        ]
                      }
                    },
                    {
                      "name": "Life Insurance",
                      "dcqlQuery": {
                        "credentials": [
                          { "id": "life_insurance_credential_id", "format": "ldp_vc", "meta": {} }
                        ]
                      }
                    }
                  ]
                }
                """);

        assertEquals(VerifiableClaimsConfigManager.getEssentialCredentialName(), "MOSIP ID");
        assertEquals(VerifiableClaimsConfigManager.getCredentialNameById("mosip_verifiable_credential_id"),
                "MOSIP ID");
        assertEquals(VerifiableClaimsConfigManager.getCredentialNameById("life_insurance_credential_id"),
                "Life Insurance");
        assertTrue(VerifiableClaimsConfigManager.getNonEssentialCredentialNames()
                .contains("MOSIP ID + Life Insurance"));
        assertFalse(VerifiableClaimsConfigManager.getNonEssentialCredentialNames().contains("MOSIP ID"));
    }

    @Test
    public void standaloneClaimWinsWhenCombinedClaimAppearsFirst() throws Exception {
        VerifiableClaimsConfigManager.loadFromJsonForTests("""
                {
                  "verifiableClaims": [
                    {
                      "name": "MOSIP ID + Life Insurance",
                      "dcqlQuery": {
                        "credentials": [
                          { "id": "mosip_verifiable_credential_id", "format": "ldp_vc", "meta": {} },
                          { "id": "life_insurance_credential_id", "format": "ldp_vc", "meta": {} }
                        ]
                      }
                    },
                    {
                      "name": "MOSIP ID",
                      "essential": true,
                      "dcqlQuery": {
                        "credentials": [
                          { "id": "mosip_verifiable_credential_id", "format": "ldp_vc", "meta": {} }
                        ]
                      }
                    }
                  ]
                }
                """);

        assertEquals(VerifiableClaimsConfigManager.getEssentialCredentialName(), "MOSIP ID");
        assertEquals(VerifiableClaimsConfigManager.getCredentialNameById("mosip_verifiable_credential_id"),
                "MOSIP ID");
    }
}
