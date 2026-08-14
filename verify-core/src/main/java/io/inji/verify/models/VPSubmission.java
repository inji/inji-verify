package io.inji.verify.models;

import java.sql.Timestamp;

import io.inji.verify.dto.submission.PresentationSubmissionDto;
import io.inji.verify.serialization.impl.PresentationSubmissionDtoConverter;
import jakarta.persistence.*;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.nimbusds.jose.shaded.gson.annotations.SerializedName;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Table(name = "vp_submission")
@Getter
@Entity
@AllArgsConstructor
@NoArgsConstructor(force = true)
public class VPSubmission {
    @Id
    @JsonProperty("state")
    @SerializedName("state")
    private final String requestId;

    @JdbcTypeCode(SqlTypes.CLOB)
    private final String vpToken;

    @Convert(converter = PresentationSubmissionDtoConverter.class)
    @Lob
    @JdbcTypeCode(SqlTypes.LONGVARCHAR)
    private final PresentationSubmissionDto presentationSubmission;

    private final String error;

    private final String errorDescription;

    private final String responseCode;

    private final Timestamp responseCodeExpiryAt;

    private final boolean responseCodeUsed;
}