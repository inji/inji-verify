package io.inji.verify.models;

import com.fasterxml.jackson.annotation.JsonIgnore;
import io.inji.verify.shared.Constants;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.Instant;

@Table(name = "dcql_query_scope")
@Getter
@Entity
@AllArgsConstructor
@NoArgsConstructor
public class DcqlQueryScope {
    @Id
    private String scope;

    @Column(name = "dcql_query", nullable = false)
    private String dcqlQuery;

    @Column(name = "created_at")
    private Instant createdAt;

    @JsonIgnore
    public String getURL() {
        return Constants.DCQL_QUERY_URI + this.scope;
    }
}
