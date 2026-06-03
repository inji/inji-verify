package io.inji.verify.validator;

import io.inji.verify.dto.dcql.*;
import io.inji.verify.enums.ErrorCode;
import io.inji.verify.exception.VPRequestValidationException;
import org.springframework.stereotype.Component;

import java.util.HashSet;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;

@Component
public class DcqlValidator {

    private static final Set<String> SUPPORTED_FORMATS = Set.of(
            "dc+sd-jwt",
            "vc+sd-jwt",
            "ldp_vc"
    );

    public void validate(DCQLQueryDto query) {
        validateCredentialIds(query);
        validateCredentialSets(query);

        for (CredentialQueryDto credential : query.getCredentials()) {
            validateCredentialFormat(credential);
            validateCredentialMeta(credential);
            validateMetaValues(credential.getMeta());
            validateClaimIds(credential);
            validateClaimSets(credential);
        }
    }

    private void validateCredentialIds(DCQLQueryDto query) {
        Set<String> credentialIds = new HashSet<>();

        for (CredentialQueryDto credential : query.getCredentials()) {
            if (!credentialIds.add(credential.getId())) {
                throw new VPRequestValidationException(ErrorCode.DCQL_DUPLICATE_CREDENTIAL_ID);
            }
        }
    }

    private void validateCredentialSets(DCQLQueryDto query) {
        if (query.getCredentialSets() == null) {
            return;
        }

        Set<String> credentialIds = query.getCredentials()
                .stream()
                .map(CredentialQueryDto::getId)
                .collect(Collectors.toSet());

        for (CredentialSetQueryDto credentialSet : query.getCredentialSets()) {
            for (List<String> option : credentialSet.getOptions()) {
                for (String credentialId : option) {
                    if (!credentialIds.contains(credentialId)) {
                        throw new VPRequestValidationException(
                                ErrorCode.DCQL_INVALID_CREDENTIAL_SET);
                    }
                }
            }
        }

        for (CredentialSetQueryDto credentialSet : query.getCredentialSets()) {
            for (List<String> option : credentialSet.getOptions()) {

                Set<String> uniqueIds = new HashSet<>();

                for (String credentialId : option) {
                    if (!uniqueIds.add(credentialId)) {
                        throw new VPRequestValidationException(
                                ErrorCode.DCQL_DUPLICATE_CREDENTIAL_ID);
                    }
                }
            }
        }
    }

    private void validateClaimIds(CredentialQueryDto credential) {
        if (credential.getClaims() == null) {
            return;
        }

        Set<String> claimIds = new HashSet<>();

        if (credential.getClaimSets() != null) {
            for (ClaimQueryDto claim : credential.getClaims()) {
                if (claim.getId() == null || claim.getId().isBlank()) {
                    throw new VPRequestValidationException(
                            ErrorCode.DCQL_MISSING_CLAIM_ID);
                }
            }
        }

        for (ClaimQueryDto claim : credential.getClaims()) {
            String claimId = claim.getId();

            if (claimId == null || claimId.isBlank()) {
                continue;
            }

            if (!claimIds.add(claimId)) {
                throw new VPRequestValidationException(ErrorCode.DCQL_DUPLICATE_CLAIM_ID);
            }
        }
    }

    private void validateClaimSets(CredentialQueryDto credential) {
        if (credential.getClaimSets() == null) {
            return;
        }

        if (credential.getClaims() == null) {
            throw new VPRequestValidationException(ErrorCode.DCQL_INVALID_CLAIM_SET);
        }

       Set<String> claimIds = credential.getClaims()
                .stream()
                .map(ClaimQueryDto::getId)
                .filter(Objects::nonNull)
                .filter(id -> !id.isBlank())
                .collect(Collectors.toSet());

        for (List<String> claimSet : credential.getClaimSets()) {
            for (String claimId : claimSet) {
                if (!claimIds.contains(claimId)) {
                    throw new VPRequestValidationException(ErrorCode.DCQL_INVALID_CLAIM_SET);
                }
            }
        }
        //reject claim ids that are duplicated within a single claim set
        for (List<String> claimSet : credential.getClaimSets()) {
            Set<String> uniqueIds = new HashSet<>();

            for (String claimId : claimSet) {
                if (!uniqueIds.add(claimId)) {
                    throw new VPRequestValidationException(
                            ErrorCode.DCQL_DUPLICATE_CLAIM_ID);
                }
            }
        }
    }

    private void validateCredentialFormat(CredentialQueryDto credential) {
        if (!SUPPORTED_FORMATS.contains(credential.getFormat())) {
            throw new VPRequestValidationException(ErrorCode.DCQL_CREDENTIAL_FORMAT_INVALID);
        }
    }

    private void validateCredentialMeta(CredentialQueryDto credential) {
        CredentialMetaDto meta = credential.getMeta();

        switch (credential.getFormat()) {
            case "dc+sd-jwt":
            case "vc+sd-jwt":
                if (meta.getTypeValues() != null) {
                    throw new VPRequestValidationException(
                            ErrorCode.DCQL_META_NOT_MATCHING_FORMAT);
                }
                break;

            case "ldp_vc":
                if (meta.getVctValues() != null) {
                    throw new VPRequestValidationException(
                            ErrorCode.DCQL_META_NOT_MATCHING_FORMAT);
                }
                break;
        }
    }

    private void validateMetaValues(CredentialMetaDto meta) {
        if (meta.getVctValues() != null &&
                meta.getVctValues().size() != new HashSet<>(meta.getVctValues()).size()) {
            throw new VPRequestValidationException(
                    ErrorCode.DCQL_META_DUPLICATES);
        }

        if (meta.getTypeValues() != null &&
                meta.getTypeValues().size() != new HashSet<>(meta.getTypeValues()).size()) {
            throw new VPRequestValidationException(
                    ErrorCode.DCQL_META_DUPLICATES);
        }
    }
}
