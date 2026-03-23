package io.inji.verify.dto.submission;

import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class VPSubmissionDtoTest {

    @Test
    public void testConstructor() {
        String vpToken = "valid_vp_token";
        PresentationSubmissionDto presentationSubmission = new PresentationSubmissionDto("submission1", "pd1", List.of(new DescriptorMapDto("id1", "string", "path1", new PathNestedDto("parent", "child"))));
        String state = "PRESENTED";

        VPSubmissionDto vpSubmissionDto = new VPSubmissionDto(vpToken,
                presentationSubmission, state, null, null, null, null, false);

        assertEquals(vpToken, vpSubmissionDto.getVpToken());
        assertEquals(presentationSubmission, vpSubmissionDto.getPresentationSubmission());
        assertEquals(state, vpSubmissionDto.getState());
    }


}