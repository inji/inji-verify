package io.inji.verify.controller;

import com.nimbusds.jose.util.JSONObjectUtils;
import io.inji.verify.dto.core.ErrorDto;
import io.inji.verify.services.DcqlQueryScopeService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.text.ParseException;

@RequestMapping("/dcql-query")
@RestController
@Slf4j
public class DcqlQueryScopeController {

    final DcqlQueryScopeService dcqlQueryScopeService;

    public DcqlQueryScopeController(DcqlQueryScopeService dcqlQueryScopeService) {
        this.dcqlQueryScopeService = dcqlQueryScopeService;
    }

    @GetMapping(path = "/{scope}", produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<Object> getDcqlQueryFor(@PathVariable String scope) {
        String dcqlQuery = dcqlQueryScopeService.getDcqlQuery(scope);
        if (dcqlQuery == null) {
            return new ResponseEntity<>(null, HttpStatus.NOT_FOUND);
        }
        try {
            return new ResponseEntity<>(JSONObjectUtils.parse(dcqlQuery), HttpStatus.OK);
        } catch (ParseException e) {
            log.error("Error parsing stored DCQL query for scope {}: {}", scope, e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(new ErrorDto("MALFORMED_DCQL_QUERY", "Stored DCQL query is malformed and could not be parsed."));
        }
    }
}
