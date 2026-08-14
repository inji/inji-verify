package io.inji.verify.dto.submission;

import io.inji.verify.dto.result.VCResultDto;
import io.inji.verify.enums.VPResultStatus;
import io.mosip.vercred.vcverifier.data.VerificationStatus;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class VPTokenResultDtoTest {
    @Test
    public void shouldTestConstructor() {
        String transactionId = "tx123";
        VPResultStatus vpResultStatus = VPResultStatus.SUCCESS;
        List<VCResultDto> vcResults = new ArrayList<>();
        vcResults.add(new VCResultDto("vc1", VerificationStatus.SUCCESS));
        vcResults.add(new VCResultDto("vc2", VerificationStatus.INVALID));

        VPTokenResultDto resultDto = new VPTokenResultDto(transactionId, vpResultStatus, vcResults);

        assertEquals(transactionId, resultDto.getTransactionId());
        assertEquals(vpResultStatus, resultDto.getVpResultStatus());
        assertEquals(vcResults, resultDto.getVcResults());
    }
}