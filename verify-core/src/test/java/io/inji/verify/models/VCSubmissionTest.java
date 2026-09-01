package io.inji.verify.models;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

public class VCSubmissionTest {

    private VCSubmission vcSubmission;
    private static final String TEST_TRANSACTION_ID = "txn_123456789";
    private static final String TEST_VC_JSON = """
        {
            "@context": [
                "https://www.w3.org/2018/credentials/v1",
                "https://www.w3.org/2018/credentials/examples/v1"
            ],
            "id": "http://example.edu/credentials/3732",
            "type": ["VerifiableCredential", "UniversityDegreeCredential"],
            "issuer": "https://example.edu/issuers/14",
            "issuanceDate": "2010-01-01T19:23:24Z",
            "credentialSubject": {
                "id": "did:example:ebfeb1f712ebc6f1c276e12ec21",
                "degree": {
                    "type": "BachelorDegree",
                    "name": "Bachelor of Science and Arts"
                }
            }
        }""";

    @BeforeEach
    void setUp() {
        vcSubmission = new VCSubmission();
    }

    @Test
    @DisplayName("Should create VCSubmission with no-args constructor")
    void shouldCreateWithNoArgsConstructor() {
        VCSubmission submission = new VCSubmission();

        assertNotNull(submission, "VCSubmission should be created successfully");
        assertNull(submission.getTransactionId(), "Transaction ID should be null initially");
        assertNull(submission.getVc(), "VC should be null initially");
    }

    @Test
    @DisplayName("Should create VCSubmission with all-args constructor")
    void shouldCreateWithAllArgsConstructor() {
        VCSubmission submission = new VCSubmission(TEST_TRANSACTION_ID, TEST_VC_JSON);

        assertNotNull(submission, "VCSubmission should be created successfully");
        assertEquals(TEST_TRANSACTION_ID, submission.getTransactionId(), "Transaction ID should match constructor parameter");
        assertEquals(TEST_VC_JSON, submission.getVc(), "VC should match constructor parameter");
    }

    @Test
    @DisplayName("Should set and get transaction ID")
    void shouldSetAndGetTransactionId() {
        String transactionId = "test_transaction_123";

        vcSubmission.setTransactionId(transactionId);

        assertEquals(transactionId, vcSubmission.getTransactionId(), "Transaction ID should be set and retrieved correctly");
    }

    @Test
    @DisplayName("Should set and get VC content")
    void shouldSetAndGetVc() {
        String vcContent = "test vc content";

        vcSubmission.setVc(vcContent);

        assertEquals(vcContent, vcSubmission.getVc(), "VC content should be set and retrieved correctly");
    }

    @Test
    @DisplayName("Should handle null transaction ID")
    void shouldHandleNullTransactionId() {
        vcSubmission.setTransactionId(null);

        assertNull(vcSubmission.getTransactionId(), "Should handle null transaction ID");
    }

    @Test
    @DisplayName("Should handle null VC content")
    void shouldHandleNullVc() {
        vcSubmission.setVc(null);

        assertNull(vcSubmission.getVc(), "Should handle null VC content");
    }

    @Test
    @DisplayName("Should handle empty transaction ID")
    void shouldHandleEmptyTransactionId() {
        String emptyTransactionId = "";

        vcSubmission.setTransactionId(emptyTransactionId);

        assertEquals(emptyTransactionId, vcSubmission.getTransactionId(), "Should handle empty transaction ID");
    }

    @Test
    @DisplayName("Should handle empty VC content")
    void shouldHandleEmptyVc() {
        String emptyVc = "";

        vcSubmission.setVc(emptyVc);

        assertEquals(emptyVc, vcSubmission.getVc(), "Should handle empty VC content");
    }

    @Test
    @DisplayName("Should handle large VC content (CLOB behavior)")
    void shouldHandleLargeVcContent() {
        StringBuilder largeVc = new StringBuilder();
        for (int i = 0; i < 10000; i++) {
            largeVc.append("This is a test line for large VC content. Line number: ").append(i).append("\n");
        }
        String largeVcContent = largeVc.toString();

        vcSubmission.setVc(largeVcContent);

        assertEquals(largeVcContent, vcSubmission.getVc(), "Should handle large VC content correctly");
        assertTrue(largeVcContent.length() > 1000, "Test content should be sufficiently large");
    }

    @Test
    @DisplayName("Should handle complex JSON VC content")
    void shouldHandleComplexJsonVc() {
        vcSubmission.setVc(TEST_VC_JSON);

        assertEquals(TEST_VC_JSON, vcSubmission.getVc(), "Should handle complex JSON VC content correctly");
        assertTrue(vcSubmission.getVc().contains("VerifiableCredential"), "VC should contain expected content");
        assertTrue(vcSubmission.getVc().contains("credentialSubject"), "VC should contain credential subject");
    }

    @Test
    @DisplayName("Should handle special characters in transaction ID")
    void shouldHandleSpecialCharactersInTransactionId() {
        String specialTransactionId = "txn_123-456@test#2023$%^&*()";

        vcSubmission.setTransactionId(specialTransactionId);

        assertEquals(specialTransactionId, vcSubmission.getTransactionId(), "Should handle special characters in transaction ID");
    }

    @Test
    @DisplayName("Should handle special characters and newlines in VC content")
    void shouldHandleSpecialCharactersInVc() {
        String specialVc = "VC with special chars: àáâãäå, ñ, ü, emoji: 🎓, newlines:\n\nand tabs:\t\ttab content";

        vcSubmission.setVc(specialVc);

        assertEquals(specialVc, vcSubmission.getVc(), "Should handle special characters and formatting in VC content");
    }

    @Test
    @DisplayName("Should maintain data integrity after multiple operations")
    void shouldMaintainDataIntegrity() {
        vcSubmission.setTransactionId(TEST_TRANSACTION_ID);
        vcSubmission.setVc(TEST_VC_JSON);

        assertEquals(TEST_TRANSACTION_ID, vcSubmission.getTransactionId());
        assertEquals(TEST_VC_JSON, vcSubmission.getVc());

        String newTransactionId = "new_txn_987654321";
        String newVc = "new vc content";

        vcSubmission.setTransactionId(newTransactionId);
        vcSubmission.setVc(newVc);

        assertEquals(newTransactionId, vcSubmission.getTransactionId(), "Transaction ID should be updated");
        assertEquals(newVc, vcSubmission.getVc(), "VC should be updated");
    }

    @Test
    @DisplayName("Should support object equality comparison")
    void shouldSupportObjectComparison() {
        VCSubmission submission1 = new VCSubmission(TEST_TRANSACTION_ID, TEST_VC_JSON);
        VCSubmission submission2 = new VCSubmission(TEST_TRANSACTION_ID, TEST_VC_JSON);
        VCSubmission submission3 = new VCSubmission("different_id", TEST_VC_JSON);

        assertNotEquals(submission1, submission2, "Without @Data/@EqualsAndHashCode, objects are not equal by value");
        assertNotEquals(submission1, submission3, "Objects with different data should not be equal");
        assertSame(submission1, submission1, "Object should be the same reference as itself");
    }

    @Test
    @DisplayName("Should have proper toString representation")
    void shouldHaveProperToStringRepresentation() {
        vcSubmission.setTransactionId(TEST_TRANSACTION_ID);
        vcSubmission.setVc("test vc");

        String toString = vcSubmission.toString();

        assertNotNull(toString, "toString should not return null");
        assertTrue(toString.length() > 0, "toString should return a non-empty string");
    }

    @Test
    @DisplayName("Should handle concurrent access to fields")
    void shouldHandleConcurrentFieldAccess() {
        vcSubmission.setTransactionId("concurrent_test");
        vcSubmission.setVc("concurrent vc content");

        for (int i = 0; i < 100; i++) {
            assertEquals("concurrent_test", vcSubmission.getTransactionId());
            assertEquals("concurrent vc content", vcSubmission.getVc());
        }
    }
}