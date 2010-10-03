/*
 * Copyright (c) 2005-2010 Grameen Foundation USA
 * All rights reserved.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or
 * implied. See the License for the specific language governing
 * permissions and limitations under the License.
 *
 * See also http://www.apache.org/licenses/LICENSE-2.0.html for an
 * explanation of the license and how it is applied.
 */

package org.mifos.accounts.savings.struts.action;

import static org.mifos.accounts.loan.util.helpers.LoanConstants.METHODCALLED;

import java.io.UnsupportedEncodingException;
import java.net.URLEncoder;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.apache.commons.lang.StringUtils;
import org.apache.struts.action.ActionErrors;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.apache.struts.action.ActionForm;
import org.apache.struts.action.ActionForward;
import org.apache.struts.action.ActionMapping;
import org.mifos.accounts.business.AccountActionDateEntity;
import org.mifos.accounts.business.AccountFlagMapping;
import org.mifos.accounts.business.AccountPaymentEntity;
import org.mifos.accounts.business.AccountStatusChangeHistoryEntity;
import org.mifos.accounts.business.AccountTrxnEntity;
import org.mifos.accounts.exceptions.AccountException;
import org.mifos.accounts.financial.business.FinancialTransactionBO;
import org.mifos.accounts.productdefinition.business.InterestCalcTypeEntity;
import org.mifos.accounts.productdefinition.business.RecommendedAmntUnitEntity;
import org.mifos.accounts.productdefinition.business.SavingsOfferingBO;
import org.mifos.accounts.productdefinition.business.SavingsTypeEntity;
import org.mifos.accounts.productdefinition.business.service.SavingsPrdBusinessService;
import org.mifos.accounts.savings.business.SavingsBO;
import org.mifos.accounts.savings.business.SavingsTransactionHistoryDto;
import org.mifos.accounts.savings.business.SavingsTrxnDetailEntity;
import org.mifos.accounts.savings.business.service.SavingsBusinessService;
import org.mifos.accounts.savings.struts.actionforms.SavingsActionForm;
import org.mifos.accounts.savings.util.helpers.SavingsConstants;
import org.mifos.accounts.struts.action.AccountAppAction;
import org.mifos.accounts.util.helpers.AccountConstants;
import org.mifos.accounts.util.helpers.AccountState;
import org.mifos.accounts.util.helpers.WaiveEnum;
import org.mifos.application.master.business.CustomFieldDefinitionEntity;
import org.mifos.application.master.business.CustomFieldType;
import org.mifos.application.master.business.service.MasterDataService;
import org.mifos.application.master.util.helpers.MasterConstants;
import org.mifos.application.questionnaire.struts.QuestionnaireFlowAdapter;
import org.mifos.application.questionnaire.struts.QuestionnaireServiceFacadeLocator;
import org.mifos.application.util.helpers.ActionForwards;
import org.mifos.config.ProcessFlowRules;
import org.mifos.customers.business.CustomerBO;
import org.mifos.customers.util.helpers.CustomerConstants;
import org.mifos.dto.domain.CustomFieldDto;
import org.mifos.dto.domain.PrdOfferingDto;
import org.mifos.framework.business.service.BusinessService;
import org.mifos.framework.exceptions.ApplicationException;
import org.mifos.framework.exceptions.PageExpiredException;
import org.mifos.framework.exceptions.ServiceException;
import org.mifos.framework.util.helpers.CloseSession;
import org.mifos.framework.util.helpers.Constants;
import org.mifos.framework.util.helpers.DateUtils;
import org.mifos.framework.util.helpers.Money;
import org.mifos.framework.util.helpers.SessionUtils;
import org.mifos.framework.util.helpers.TransactionDemarcate;
import org.mifos.platform.questionnaire.service.QuestionGroupInstanceDetail;
import org.mifos.platform.questionnaire.service.QuestionnaireServiceFacade;
import org.mifos.security.authorization.AuthorizationManager;
import org.mifos.security.util.ActionSecurity;
import org.mifos.security.util.ActivityContext;
import org.mifos.security.util.ActivityMapper;
import org.mifos.security.util.SecurityConstants;
import org.mifos.security.util.UserContext;
import org.mifos.service.MifosServiceFactory;

public class SavingsAction extends AccountAppAction {

    private SavingsBusinessService savingsService;

    private MasterDataService masterDataService;

    private SavingsPrdBusinessService savingsPrdService;

    private static final Logger logger = LoggerFactory.getLogger(SavingsAction.class);

    public static ActionSecurity getSecurity() {
        ActionSecurity security = new ActionSecurity("savingsAction");

        security.allow("getPrdOfferings", SecurityConstants.VIEW);
        security.allow("load", SecurityConstants.VIEW);
        security.allow("reLoad", SecurityConstants.VIEW);
        security.allow("preview", SecurityConstants.VIEW);
        security.allow("previous", SecurityConstants.VIEW);
        security.allow("create", SecurityConstants.VIEW);
        security.allow("get", SecurityConstants.VIEW);
        security.allow("getStatusHistory", SecurityConstants.VIEW);
        security.allow("edit", SecurityConstants.SAVINGS_UPDATE_SAVINGS);
        security.allow("editPreview", SecurityConstants.SAVINGS_UPDATE_SAVINGS);
        security.allow("editPrevious", SecurityConstants.SAVINGS_UPDATE_SAVINGS);
        security.allow("update", SecurityConstants.SAVINGS_UPDATE_SAVINGS);
        security.allow("getRecentActivity", SecurityConstants.VIEW);
        security.allow("getTransactionHistory", SecurityConstants.VIEW);
        security.allow("getDepositDueDetails", SecurityConstants.VIEW);
        security.allow("waiveAmountDue", SecurityConstants.SAVINGS_CANWAIVE_DUEAMOUNT);
        security.allow("waiveAmountOverDue", SecurityConstants.SAVINGS_CANWAIVE_OVERDUEAMOUNT);
        security.allow("loadChangeLog", SecurityConstants.VIEW);
        security.allow("cancelChangeLog", SecurityConstants.VIEW);
        security.allow("captureQuestionResponses", SecurityConstants.VIEW);
        security.allow("editQuestionResponses", SecurityConstants.VIEW);
        return security;
    }

    public SavingsAction() throws Exception {
        savingsService = new SavingsBusinessService();
        masterDataService = new MasterDataService();
        savingsPrdService = new SavingsPrdBusinessService();
    }

    @Override
    protected BusinessService getService() {
        return savingsService;
    }

    @Override
    protected boolean skipActionFormToBusinessObjectConversion(String method) {
        return true;
    }

    @TransactionDemarcate(saveToken = true)
    public ActionForward getPrdOfferings(ActionMapping mapping, ActionForm form, HttpServletRequest request,
            HttpServletResponse response) throws Exception {
        SavingsActionForm savingsActionForm = ((SavingsActionForm) form);
        logger.debug(" customerId: " + savingsActionForm.getCustomerId());
        doCleanUp(savingsActionForm, request);
        CustomerBO customer = getCustomer(getIntegerValue(savingsActionForm.getCustomerId()));
        SessionUtils.setAttribute(SavingsConstants.CLIENT, customer, request);
        List<PrdOfferingDto> savingPrds = savingsService.getSavingProducts(customer.getOffice(), customer
                .getCustomerLevel(), SavingsConstants.SAVINGS_ALL);
        SessionUtils.setCollectionAttribute(SavingsConstants.SAVINGS_PRD_OFFERINGS, savingPrds, request);
        logger.info(" Retrieved " + savingPrds.size() + " Products for customerId: " + customer.getCustomerId());
        return mapping.findForward(AccountConstants.GET_PRDOFFERINGS_SUCCESS);
    }

    @TransactionDemarcate(joinToken = true)
    public ActionForward load(ActionMapping mapping, ActionForm form, HttpServletRequest request,
            HttpServletResponse response) throws Exception {
        SavingsActionForm savingsActionForm = ((SavingsActionForm) form);
        logger.debug(" selectedPrdOfferingId: " + savingsActionForm.getSelectedPrdOfferingId());
        loadMasterData(savingsActionForm, request);
        loadPrdoffering(savingsActionForm, request);
        logger.info(" Data loaded successfully ");
        return mapping.findForward("load_success");
    }

    private void loadMasterData(SavingsActionForm actionForm, HttpServletRequest request) throws Exception {
        UserContext uc = (UserContext) SessionUtils.getAttribute(Constants.USER_CONTEXT_KEY, request.getSession());
        SessionUtils.setCollectionAttribute(MasterConstants.INTEREST_CAL_TYPES, masterDataService
                .retrieveMasterEntities(InterestCalcTypeEntity.class, uc.getLocaleId()), request);
        loadMasterDataPartial(actionForm, request);
        loadCustomFieldsForCreate(actionForm, request);
    }

    private void loadMasterDataPartial(SavingsActionForm actionForm, HttpServletRequest request) throws Exception {
        UserContext uc = (UserContext) SessionUtils.getAttribute(Constants.USER_CONTEXT_KEY, request.getSession());
        SessionUtils.setCollectionAttribute(MasterConstants.SAVINGS_TYPE, masterDataService.retrieveMasterEntities(
                SavingsTypeEntity.class, uc.getLocaleId()), request);
        SessionUtils.setCollectionAttribute(MasterConstants.RECOMMENDED_AMOUNT_UNIT, masterDataService
                .retrieveMasterEntities(RecommendedAmntUnitEntity.class, uc.getLocaleId()), request);
        SessionUtils.setCollectionAttribute(SavingsConstants.CUSTOM_FIELDS, savingsService
                .retrieveCustomFieldsDefinition(), request);
    }

    private void loadCustomFieldsForCreate(SavingsActionForm actionForm, HttpServletRequest request) throws Exception {
        // Set Default values for custom fields
        List<CustomFieldDefinitionEntity> customFieldDefs = (List<CustomFieldDefinitionEntity>) SessionUtils
                .getAttribute(CustomerConstants.CUSTOM_FIELDS_LIST, request);
        List<CustomFieldDto> customFields = new ArrayList<CustomFieldDto>();

        for (CustomFieldDefinitionEntity fieldDef : customFieldDefs) {
            if (StringUtils.isNotBlank(fieldDef.getDefaultValue())
                    && fieldDef.getFieldType().equals(CustomFieldType.DATE.getValue())) {
                customFields.add(new CustomFieldDto(fieldDef.getFieldId(), DateUtils.getUserLocaleDate(getUserContext(
                        request).getPreferredLocale(), fieldDef.getDefaultValue()), fieldDef.getFieldType()));
            } else {
                customFields.add(new CustomFieldDto(fieldDef.getFieldId(), fieldDef.getDefaultValue(), fieldDef
                        .getFieldType()));
            }
        }
        actionForm.setAccountCustomFieldSet(customFields);
    }

    private void loadPrdoffering(SavingsActionForm savingsActionForm, HttpServletRequest request)
            throws ServiceException, PageExpiredException {

        SavingsOfferingBO savingsOfferingBO = savingsPrdService.getSavingsProduct(getShortValue(savingsActionForm
                .getSelectedPrdOfferingId()));
        SessionUtils.setAttribute(SavingsConstants.PRDOFFCERING, savingsOfferingBO, request);

        savingsActionForm.setRecommendedAmount(savingsOfferingBO.getRecommendedAmount().toString());

    }

    @TransactionDemarcate(joinToken = true)
    public ActionForward reLoad(ActionMapping mapping, ActionForm form, HttpServletRequest request,
            HttpServletResponse response) throws Exception {
        SavingsActionForm savingsActionForm = ((SavingsActionForm) form);
        logger.debug(" selectedPrdOfferingId: " + (savingsActionForm).getSelectedPrdOfferingId());
        loadPrdoffering(savingsActionForm, request);
        logger.info("Data loaded successfully ");
        return mapping.findForward("load_success");
    }

    @TransactionDemarcate(joinToken = true)
    public ActionForward preview(ActionMapping mapping, ActionForm form, HttpServletRequest request,
            HttpServletResponse response) throws Exception {
        SavingsActionForm savingActionForm = (SavingsActionForm) form;
        logger.debug("In SavingsAction::preview()");
        CustomerBO customer = (CustomerBO) SessionUtils.getAttribute(SavingsConstants.CLIENT, request);
        customer = getCustomer(customer.getCustomerId());
        SessionUtils.setAttribute(SavingsConstants.IS_PENDING_APPROVAL, ProcessFlowRules
                .isSavingsPendingApprovalStateEnabled(), request.getSession());
        return createGroupQuestionnaire.fetchAppliedQuestions(mapping, savingActionForm, request, ActionForwards.preview_success);
    }

    @TransactionDemarcate(joinToken = true)
    public ActionForward previous(ActionMapping mapping, ActionForm form, HttpServletRequest request,
            HttpServletResponse response) throws Exception {
        logger.debug("In SavingsAction:previous()");
        return mapping.findForward("previous_success");
    }

    @TransactionDemarcate(validateAndResetToken = true)
    public ActionForward create(ActionMapping mapping, ActionForm form, HttpServletRequest request,
            HttpServletResponse response) throws Exception {
        SavingsActionForm savingsActionForm = ((SavingsActionForm) form);
        logger.debug("In SavingsAction::create(), accountStateId: " + savingsActionForm.getStateSelected());
        UserContext uc = (UserContext) SessionUtils.getAttribute(Constants.USER_CONTEXT_KEY, request.getSession());
        CustomerBO customer = (CustomerBO) SessionUtils.getAttribute(SavingsConstants.CLIENT, request);
        Integer version = customer.getVersionNo();
        customer = getCustomer(customer.getCustomerId());
        customer.setVersionNo(version);

        SavingsOfferingBO savingsOfferingBO = (SavingsOfferingBO) SessionUtils.getAttribute(
                SavingsConstants.PRDOFFCERING, request);
        version = savingsOfferingBO.getVersionNo();
        savingsOfferingBO = savingsPrdService.getSavingsProduct(savingsOfferingBO.getPrdOfferingId());
        savingsOfferingBO.setVersionNo(version);

        checkPermissionForCreate(getShortValue(savingsActionForm.getStateSelected()), uc, null, customer.getOffice()
                .getOfficeId(), customer.getPersonnel().getPersonnelId());

        List<CustomFieldDto> customFields = savingsActionForm.getAccountCustomFieldSet();

        SavingsBO saving = new SavingsBO(uc, savingsOfferingBO, customer, AccountState
                .fromShort(getShortValue(savingsActionForm.getStateSelected())),
                new Money(savingsOfferingBO.getCurrency(), savingsActionForm
                .getRecommendedAmount()), customFields);
        this.savingsService.persistSavings(saving);

        createGroupQuestionnaire.saveResponses(request, savingsActionForm, saving.getAccountId());

        request.setAttribute(SavingsConstants.GLOBALACCOUNTNUM, saving.getGlobalAccountNum());
        request.setAttribute(SavingsConstants.RECORD_OFFICE_ID, saving.getOffice().getOfficeId());
        request.setAttribute(SavingsConstants.CLIENT_NAME, customer.getDisplayName());
        request.setAttribute(SavingsConstants.CLIENT_ID, customer.getCustomerId());
        request.setAttribute(SavingsConstants.CLIENT_LEVEL, customer.getCustomerLevel().getId());

        logger.info("In SavingsAction::create(), Savings object saved successfully ");
        return mapping.findForward("create_success");
    }

    @TransactionDemarcate(saveToken = true)
    public ActionForward get(ActionMapping mapping, ActionForm form, HttpServletRequest request,
            HttpServletResponse response) throws Exception {

        SavingsActionForm actionForm = (SavingsActionForm) form;
        actionForm.setInput(null);
        logger.debug(" Retrieving for globalAccountNum: " + actionForm.getGlobalAccountNum());
        SavingsBO savings = savingsService.findBySystemId(actionForm.getGlobalAccountNum());
        savings.getSavingPerformanceHistory();

        UserContext uc = (UserContext) SessionUtils.getAttribute(Constants.USER_CONTEXT_KEY, request.getSession());
        savings.getAccountState().setLocaleId(uc.getLocaleId());
        for (AccountFlagMapping accountFlagMapping : savings.getAccountFlags()) {
            accountFlagMapping.getFlag().setLocaleId(uc.getLocaleId());
        }
        savings.setUserContext(uc);
        SessionUtils.setAttribute(Constants.BUSINESS_KEY, savings, request);

        loadMasterDataPartial(actionForm, request);

        SessionUtils.setAttribute(SavingsConstants.PRDOFFCERING, savings.getSavingsOffering(), request);
        actionForm.setRecommendedAmount(savings.getSavingsOffering().getRecommendedAmount().toString());

        actionForm.clear();

        SessionUtils.setCollectionAttribute(SavingsConstants.RECENTY_ACTIVITY_DETAIL_PAGE, savings
                .getRecentAccountActivity(3), request);
        SessionUtils.setCollectionAttribute(SavingsConstants.NOTES, savings.getRecentAccountNotes(), request);
        logger.info(" Savings object retrieved successfully");

        setCurrentPageUrl(request, savings);
        setQuestionGroupInstances(request, savings);

        return mapping.findForward("get_success");
    }
    private void setQuestionGroupInstances(HttpServletRequest request, SavingsBO savingsBO) throws PageExpiredException {
        QuestionnaireServiceFacade questionnaireServiceFacade = MifosServiceFactory.getQuestionnaireServiceFacade(request);
        if (questionnaireServiceFacade == null) {
            return;
        }
        setQuestionGroupInstances(questionnaireServiceFacade, request, savingsBO.getAccountId());
    }

    // Intentionally made public to aid testing !
    public void setQuestionGroupInstances(QuestionnaireServiceFacade questionnaireServiceFacade, HttpServletRequest request, Integer savingsAccountId) throws PageExpiredException {
        List<QuestionGroupInstanceDetail> instanceDetails = questionnaireServiceFacade.getQuestionGroupInstances(savingsAccountId, "View", "Savings");
        SessionUtils.setCollectionAttribute("questionGroupInstances", instanceDetails, request);
    }

    private void setCurrentPageUrl(HttpServletRequest request, SavingsBO savingsBO) throws PageExpiredException, UnsupportedEncodingException {
        SessionUtils.removeThenSetAttribute("currentPageUrl", constructCurrentPageUrl(request, savingsBO), request);
    }

    private String constructCurrentPageUrl(HttpServletRequest request, SavingsBO savingsBO) throws UnsupportedEncodingException {
        String globalAccountNum = request.getParameter("globalAccountNum");
        String officerId = request.getParameter("recordOfficeId");
        String loanOfficerId = request.getParameter("recordLoanOfficerId");
        String url = String.format("savingsAction.do?globalAccountNum=%s&customerId=%s&recordOfficeId=%s&recordLoanOfficerId=%s",
                globalAccountNum, Integer.toString(savingsBO.getAccountId()), officerId, loanOfficerId);
        return URLEncoder.encode(url, "UTF-8");
    }

    @TransactionDemarcate(joinToken = true)
    public ActionForward edit(ActionMapping mapping, ActionForm form, HttpServletRequest request,
            HttpServletResponse response) throws Exception {
        logger.debug("In SavingsAction::edit()");
        SavingsBO savings = (SavingsBO) SessionUtils.getAttribute(Constants.BUSINESS_KEY, request);
        SavingsActionForm actionForm = (SavingsActionForm) form;
        actionForm.setRecommendedAmount(savings.getRecommendedAmount().toString());
        actionForm.setAccountCustomFieldSet(createCustomFieldViewsForEdit(savings.getAccountCustomFields(), request));
        return mapping.findForward("edit_success");
    }

    @TransactionDemarcate(joinToken = true)
    public ActionForward editPreview(ActionMapping mapping, ActionForm form, HttpServletRequest request,
            HttpServletResponse response) throws Exception {
        logger.debug("In SavingsAction::editPreview()");
        return mapping.findForward("editPreview_success");
    }

    @TransactionDemarcate(joinToken = true)
    public ActionForward editPrevious(ActionMapping mapping, ActionForm form, HttpServletRequest request,
            HttpServletResponse response) throws Exception {
        logger.debug("In SavingsAction::editPrevious()");
        return mapping.findForward("editPrevious_success");
    }

    @CloseSession
    @TransactionDemarcate(validateAndResetToken = true)
    public ActionForward update(ActionMapping mapping, ActionForm form, HttpServletRequest request,
            HttpServletResponse response) throws Exception {
        logger.debug("In SavingsAction::update()");
        SavingsActionForm actionForm = (SavingsActionForm) form;
        SavingsBO savings = (SavingsBO) SessionUtils.getAttribute(Constants.BUSINESS_KEY, request);
        UserContext uc = (UserContext) SessionUtils.getAttribute(Constants.USER_CONTEXT_KEY, request.getSession());
        Integer version = savings.getVersionNo();
        savings = new SavingsBusinessService().findById(savings.getAccountId());
        checkVersionMismatch(version, savings.getVersionNo());
        savings.setVersionNo(version);
        savings.setUserContext(uc);
        setInitialObjectForAuditLogging(savings);
        savings.update(new Money(savings.getCurrency(), actionForm.getRecommendedAmount()), actionForm.getAccountCustomFieldSet());
        request.setAttribute(SavingsConstants.GLOBALACCOUNTNUM, savings.getGlobalAccountNum());
        logger.info("In SavingsAction::update(), Savings object updated successfully");

        doCleanUp(actionForm, request);
        return mapping.findForward("update_success");
    }

    @TransactionDemarcate(joinToken = true)
    public ActionForward getRecentActivity(ActionMapping mapping, ActionForm form, HttpServletRequest request,
            HttpServletResponse response) throws Exception {
        logger.debug("In SavingsAction::getRecentActivity()");
        // Check for order-by clause in AccountBO.hbm.xml and
        // AccountPayment.hbm.xml for accountPaymentSet and
        // accountTrxnSet. It should be set for the primay key column desc in
        // both. If stated is not there, the code
        // below will behave abnormally.
        UserContext uc = (UserContext) SessionUtils.getAttribute(Constants.USER_CONTEXT_KEY, request.getSession());
        SavingsBO savings = (SavingsBO) SessionUtils.getAttribute(Constants.BUSINESS_KEY, request);
        savings = savingsService.findById(savings.getAccountId());
        savings.setUserContext(uc);
        SessionUtils.setCollectionAttribute(SavingsConstants.RECENTY_ACTIVITY_LIST, savings
                .getRecentAccountActivity(null), request);
        return mapping.findForward("getRecentActivity_success");
    }

    @TransactionDemarcate(joinToken = true)
    public ActionForward getTransactionHistory(ActionMapping mapping, ActionForm form, HttpServletRequest request,
            HttpServletResponse response) throws Exception {
        logger.debug("In SavingsAction::getRecentActivity()");
        String globalAccountNum = request.getParameter("globalAccountNum");
        SavingsBO savings = savingsService.findBySystemId(globalAccountNum);
        List<SavingsTransactionHistoryDto> savingsTransactionHistoryViewList = new ArrayList<SavingsTransactionHistoryDto>();
        // Check for order-by clause in AccountBO.hbm.xml,
        // AccountPayment.hbm.xml and AccountTrxnEntity.hbm.xml for
        // accountPaymentSet ,
        // accountTrxnSet and financialBoSet. They all should be set for their
        // primay key column desc in both. If stated is not there, the code
        // below will behave abnormally.
        List<AccountPaymentEntity> accountPaymentSet = savings.getAccountPayments();
        for (AccountPaymentEntity accountPaymentEntity : accountPaymentSet) {
            Set<AccountTrxnEntity> accountTrxnEntitySet = accountPaymentEntity.getAccountTrxns();
            for (AccountTrxnEntity accountTrxnEntity : accountTrxnEntitySet) {
                Set<FinancialTransactionBO> financialTransactionBOSet = accountTrxnEntity.getFinancialTransactions();
                for (FinancialTransactionBO financialTransactionBO : financialTransactionBOSet) {
                    SavingsTransactionHistoryDto savingsTransactionHistoryDto = new SavingsTransactionHistoryDto();
                    savingsTransactionHistoryDto.setTransactionDate(financialTransactionBO.getActionDate());
                    savingsTransactionHistoryDto.setPaymentId(accountTrxnEntity.getAccountPayment().getPaymentId());
                    savingsTransactionHistoryDto.setAccountTrxnId(accountTrxnEntity.getAccountTrxnId());
                    savingsTransactionHistoryDto.setType(financialTransactionBO.getFinancialAction().getName());
                    savingsTransactionHistoryDto.setGlcode(financialTransactionBO.getGlcode().getGlcode());
                    if (financialTransactionBO.isDebitEntry()) {
                        savingsTransactionHistoryDto.setDebit(String.valueOf(removeSign(financialTransactionBO
                                .getPostedAmount())));
                    } else if (financialTransactionBO.isCreditEntry()) {
                        savingsTransactionHistoryDto.setCredit(String.valueOf(removeSign(financialTransactionBO
                                .getPostedAmount())));
                    }
                    savingsTransactionHistoryDto.setBalance(String
                            .valueOf(removeSign(((SavingsTrxnDetailEntity) accountTrxnEntity).getBalance())));
                    savingsTransactionHistoryDto.setClientName(accountTrxnEntity.getCustomer().getDisplayName());
                    savingsTransactionHistoryDto.setPostedDate(financialTransactionBO.getPostedDate());
                    if (accountTrxnEntity.getPersonnel() != null) {
                        savingsTransactionHistoryDto.setPostedBy(accountTrxnEntity.getPersonnel().getDisplayName());
                    }
                    if (financialTransactionBO.getNotes() != null && !financialTransactionBO.getNotes().equals("")) {
                        savingsTransactionHistoryDto.setNotes(financialTransactionBO.getNotes());
                    }
                    savingsTransactionHistoryViewList.add(savingsTransactionHistoryDto);
                }
            }
        }
        SessionUtils.setCollectionAttribute(SavingsConstants.TRXN_HISTORY_LIST, savingsTransactionHistoryViewList,
                request);
        return mapping.findForward("getTransactionHistory_success");
    }

    @TransactionDemarcate(joinToken = true)
    public ActionForward getStatusHistory(ActionMapping mapping, ActionForm form, HttpServletRequest request,
            HttpServletResponse response) throws Exception {
        logger.debug("In SavingsAction::getRecentActivity()");
        String globalAccountNum = request.getParameter("globalAccountNum");
        SavingsBO savings = savingsService.findBySystemId(globalAccountNum);
        savingsService.initialize(savings.getAccountStatusChangeHistory());
        savings.setUserContext((UserContext) SessionUtils
                .getAttribute(Constants.USER_CONTEXT_KEY, request.getSession()));
        List<AccountStatusChangeHistoryEntity> savingsStatusHistoryViewList = new ArrayList<AccountStatusChangeHistoryEntity>(
                savings.getAccountStatusChangeHistory());
        SessionUtils.setCollectionAttribute(SavingsConstants.STATUS_CHANGE_HISTORY_LIST, savingsStatusHistoryViewList,
                request);

        return mapping.findForward("getStatusHistory_success");
    }

    @TransactionDemarcate(joinToken = true)
    public ActionForward validate(ActionMapping mapping, ActionForm form, HttpServletRequest request,
            HttpServletResponse response) throws Exception {
        String method = (String) request.getAttribute("methodCalled");
        logger.debug("In SavingsAction::validate(), method: " + method);
        String forward = null;
        if (method != null) {
            if (method.equals("preview")) {
                forward = "preview_faliure";
            } else if (method.equals("editPreview")) {
                forward = "editPreview_faliure";
            } else if (method.equals("load")) {
                forward = "load_faliure";
            } else if (method.equals("reLoad")) {
                forward = "reLoad_faliure";
            }
        }
        return mapping.findForward(forward);
    }

    @TransactionDemarcate(joinToken = true)
    public ActionForward getDepositDueDetails(ActionMapping mapping, ActionForm form, HttpServletRequest request,
            HttpServletResponse response) throws Exception {
        logger.debug("In SavingsAction::getDepositDueDetails()");
        SessionUtils.removeAttribute(Constants.BUSINESS_KEY, request);
        SavingsBO savings = savingsService.findBySystemId(((SavingsActionForm) form).getGlobalAccountNum());
        for (AccountActionDateEntity actionDate : savings.getAccountActionDates()) {
            savingsService.initialize(actionDate);
        }

        savingsService.initialize(savings.getAccountNotes());
        UserContext uc = (UserContext) SessionUtils.getAttribute(Constants.USER_CONTEXT_KEY, request.getSession());
        for (AccountFlagMapping accountFlagMapping : savings.getAccountFlags()) {
            savingsService.initialize(accountFlagMapping.getFlag());
            accountFlagMapping.getFlag().setLocaleId(uc.getLocaleId());
        }
        savingsService.initialize(savings.getAccountFlags());
        savings.getAccountState().setLocaleId(uc.getLocaleId());
        savings.setUserContext(uc);
        SessionUtils.setAttribute(Constants.BUSINESS_KEY, savings, request);
        return mapping.findForward("depositduedetails_success");
    }

    @TransactionDemarcate(joinToken = true)
    public ActionForward waiveAmountDue(ActionMapping mapping, ActionForm form, HttpServletRequest request,
            HttpServletResponse response) throws Exception {
        logger.debug("In SavingsAction::waiveAmountDue()");
        UserContext uc = (UserContext) SessionUtils.getAttribute(Constants.USER_CONTEXT_KEY, request.getSession());
        SavingsBO savings = (SavingsBO) SessionUtils.getAttribute(Constants.BUSINESS_KEY, request);
        Integer versionNum = savings.getVersionNo();
        savings = savingsService.findBySystemId(((SavingsActionForm) form).getGlobalAccountNum());
        checkVersionMismatch(versionNum, savings.getVersionNo());
        savings.setUserContext(uc);
        savings.waiveAmountDue(WaiveEnum.ALL);
        return mapping.findForward("waiveAmount_success");
    }

    @TransactionDemarcate(joinToken = true)
    public ActionForward waiveAmountOverDue(ActionMapping mapping, ActionForm form, HttpServletRequest request,
            HttpServletResponse response) throws Exception {
        logger.debug("In SavingsAction::waiveAmountOverDue()");
        UserContext uc = (UserContext) SessionUtils.getAttribute(Constants.USER_CONTEXT_KEY, request.getSession());
        SavingsBO savings = (SavingsBO) SessionUtils.getAttribute(Constants.BUSINESS_KEY, request);
        Integer versionNum = savings.getVersionNo();
        savings = savingsService.findBySystemId(((SavingsActionForm) form).getGlobalAccountNum());
        checkVersionMismatch(versionNum, savings.getVersionNo());
        savings.setUserContext(uc);
        savings.waiveAmountOverDue();
        return mapping.findForward("waiveAmount_success");
    }

    private void doCleanUp(SavingsActionForm savingsActionForm, HttpServletRequest request) throws Exception {
        savingsActionForm.clear();
        // remove old savings object
        SessionUtils.removeAttribute(Constants.BUSINESS_KEY, request);
    }

    private String removeSign(Money amount) {
        if (amount.isLessThanZero()) {
            return amount.negate().toString();
        } else {
            return amount.toString();
        }
    }

    protected void checkPermissionForCreate(Short newState, UserContext userContext, Short flagSelected,
            Short officeId, Short loanOfficerId) throws ApplicationException {
        if (!isPermissionAllowed(newState, userContext, officeId, loanOfficerId, true)) {
            throw new AccountException(SecurityConstants.KEY_ACTIVITY_NOT_ALLOWED);
        }
    }

    private boolean isPermissionAllowed(Short newSate, UserContext userContext, Short officeId, Short loanOfficerId,
            boolean saveFlag) {
        return AuthorizationManager.getInstance().isActivityAllowed(
                userContext,
                new ActivityContext(ActivityMapper.getInstance().getActivityIdForState(newSate), officeId,
                        loanOfficerId));
    }

    @TransactionDemarcate(joinToken = true)
    public ActionForward captureQuestionResponses(
            final ActionMapping mapping, final ActionForm form, final HttpServletRequest request,
            @SuppressWarnings("unused") final HttpServletResponse response) throws Exception {
        request.setAttribute(METHODCALLED, "captureQuestionResponses");
        ActionErrors errors = createGroupQuestionnaire.validateResponses(request, (SavingsActionForm) form);
        if (errors != null && !errors.isEmpty()) {
            addErrors(request, errors);
            return mapping.findForward(ActionForwards.captureQuestionResponses.toString());
        }
        return createGroupQuestionnaire.rejoinFlow(mapping);
    }

    @TransactionDemarcate(joinToken = true)
    public ActionForward editQuestionResponses(
            final ActionMapping mapping, final ActionForm form,
            final HttpServletRequest request, @SuppressWarnings("unused") final HttpServletResponse response) throws Exception {
        request.setAttribute(METHODCALLED, "editQuestionResponses");
        return createGroupQuestionnaire.editResponses(mapping, request, (SavingsActionForm) form);
    }

    private QuestionnaireFlowAdapter createGroupQuestionnaire =
        new QuestionnaireFlowAdapter("Create", "Savings",
                ActionForwards.preview_success,
                "custSearchAction.do?method=loadMainSearch",
                new QuestionnaireServiceFacadeLocator() {
                    @Override
                    public QuestionnaireServiceFacade getService(HttpServletRequest request) {
                        return MifosServiceFactory.getQuestionnaireServiceFacade(request);
                    }
                });


}
