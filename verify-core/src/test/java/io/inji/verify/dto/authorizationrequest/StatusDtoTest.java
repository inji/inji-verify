package io.inji.verify.dto.authorizationrequest;

import io.inji.verify.enums.VPRequestStatus;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.assertEquals;

public class StatusDtoTest {

    @Test
    public void testConstructor() {
        VPRequestStatus status = VPRequestStatus.VP_SUBMITTED;
        VPRequestStatusDto statusDto = new VPRequestStatusDto(status);
        assertEquals(status, statusDto.getStatus());
    }
}