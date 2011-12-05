package org.mifos.platform.rest.approval.service;

import java.util.List;

import org.mifos.platform.rest.approval.domain.ApprovalMethod;
import org.mifos.platform.rest.approval.domain.RESTApprovalEntity;

public interface ApprovalService {

    List<RESTApprovalEntity> getWaitingForApproval();

    RESTApprovalEntity getDetails(Long id);

    void create(ApprovalMethod method) throws Exception;

    void approve(Long id) throws Exception;

    void reject(Long id);

}
