package io.inji.verify.services.impl;

import io.inji.verify.repository.DcqlQueryScopeRepository;
import io.inji.verify.services.DcqlQueryScopeService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

@Service
@Slf4j
public class DcqlQueryScopeServiceImpl implements DcqlQueryScopeService {

    final DcqlQueryScopeRepository dcqlQueryScopeRepository;

    public DcqlQueryScopeServiceImpl(DcqlQueryScopeRepository dcqlQueryScopeRepository) {
        this.dcqlQueryScopeRepository = dcqlQueryScopeRepository;
    }

    @Override
    public String getDcqlQuery(String scope) {
        return dcqlQueryScopeRepository.findById(scope)
                .map(dcqlQueryScope -> dcqlQueryScope.getDcqlQuery())
                .orElse(null);
    }
}
