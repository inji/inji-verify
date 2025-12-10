package io.inji.verify.services;

import io.inji.verify.dto.verification.VCVerificationStatusDto;
import io.inji.verify.exception.CredentialStatusCheckException;

public interface VCVerificationService {

    VCVerificationStatusDto verify(String vc, String contentType) throws CredentialStatusCheckException;
}
