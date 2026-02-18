package io.inji.verify.models;

import io.inji.verify.dto.submission.PresentationSubmissionDto;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.mock;

public class VPSubmissionTest {

    @Test
    public void testConstructorAndGetters() {
        String requestId = "request123";
        String vpToken = "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9...";
        PresentationSubmissionDto presentationSubmission = new PresentationSubmissionDto("id","dId",mock());

        VPSubmission vpSubmission = new VPSubmission(requestId, vpToken, presentationSubmission, null, null, null, null, null);
        assertEquals(requestId, vpSubmission.getRequestId());
        assertEquals(vpToken, vpSubmission.getVpToken());
        assertEquals(presentationSubmission, vpSubmission.getPresentationSubmission());
        assertNull(vpSubmission.getError());
        assertNull(vpSubmission.getErrorDescription());
    }


    @Test
    public void testEmptyConstructor() {
        VPSubmission vpSubmission = new VPSubmission();
        assertNull(vpSubmission.getRequestId());
        assertNull(vpSubmission.getVpToken());
        assertNull(vpSubmission.getPresentationSubmission());
    }

}