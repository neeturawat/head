package org.mifos.application.holiday.struts.action;

import java.util.Calendar;
import java.util.List;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.apache.struts.action.ActionForm;
import org.apache.struts.action.ActionForward;
import org.apache.struts.action.ActionMapping;
import org.mifos.application.holiday.business.HolidayBO;
import org.mifos.application.holiday.business.HolidayPK;
import org.mifos.application.holiday.business.RepaymentRuleEntity;
import org.mifos.application.holiday.business.service.HolidayBusinessService;
import org.mifos.application.holiday.struts.actionforms.HolidayActionForm;
import org.mifos.application.holiday.util.resources.HolidayConstants;
import org.mifos.application.util.helpers.ActionForwards;
import org.mifos.framework.business.service.BusinessService;
import org.mifos.framework.business.service.ServiceFactory;
import org.mifos.framework.exceptions.ServiceException;
import org.mifos.framework.security.util.UserContext;
import org.mifos.framework.struts.action.BaseAction;
import org.mifos.framework.util.helpers.BusinessServiceName;
import org.mifos.framework.util.helpers.Constants;
import org.mifos.framework.util.helpers.Flow;
import org.mifos.framework.util.helpers.FlowManager;
import org.mifos.framework.util.helpers.SessionUtils;
import org.mifos.framework.util.helpers.TransactionDemarcate;

public class HolidayAction extends BaseAction {

	
	@Override
	protected BusinessService getService() throws ServiceException {
		return new HolidayBusinessService();
	}

	@Override
	protected boolean skipActionFormToBusinessObjectConversion(String method) {
		return true;
	}

	@TransactionDemarcate(saveToken = true)
	public ActionForward load(ActionMapping mapping, ActionForm form,
			HttpServletRequest request, HttpServletResponse response)
			throws Exception {
		
		UserContext userContext = (UserContext) SessionUtils.getAttribute(
				Constants.USER_CONTEXT_KEY, request.getSession());
		
		doCleanUp(request);
		
		request.getSession().setAttribute("HolidayActionForm", null);
		
		SessionUtils.setCollectionAttribute(HolidayConstants.REPAYMENTRULETYPES, 
				getRepaymentRuleTypes(userContext.getLocaleId()),request);		
				
		return mapping.findForward(ActionForwards.load_success.toString());
	}
	
	public ActionForward getHolidays(ActionMapping mapping, ActionForm form,
			HttpServletRequest request, HttpServletResponse response)
			throws Exception {
		
		UserContext userContext = (UserContext) SessionUtils.getAttribute(
				Constants.USER_CONTEXT_KEY, request.getSession());
		
		SessionUtils.setCollectionAttribute(HolidayConstants.HOLIDAYLIST1, getHolidays(Calendar.getInstance().get(Calendar.YEAR), userContext.getLocaleId()),request);
		SessionUtils.setCollectionAttribute(HolidayConstants.HOLIDAYLIST2, getHolidays(Calendar.getInstance().get(Calendar.YEAR)+1, userContext.getLocaleId()),request);
		
		return mapping.findForward("view_organizational_holidays");
	}
	
	private List<HolidayBO> getHolidays(int year, int localeId) throws Exception{
		return getHolidayBizService().getHolidays(year, localeId);
	}
	
	private List<RepaymentRuleEntity> getRepaymentRuleTypes(int localId) throws Exception{
		return getHolidayBizService().getRepaymentRuleTypes(localId);
	}
	
	public ActionForward addHoliday(ActionMapping mapping, ActionForm form,
			HttpServletRequest request, HttpServletResponse response)
			throws Exception {
		
		UserContext userContext = (UserContext) SessionUtils.getAttribute(
				Constants.USER_CONTEXT_KEY, request.getSession());
		
		SessionUtils.setCollectionAttribute(HolidayConstants.REPAYMENTRULETYPES, 
				getRepaymentRuleTypes(userContext.getLocaleId()),request);		
		
		return mapping.findForward("create_office_holiday");
	}
		
	private HolidayBusinessService getHolidayBizService() {
		return (HolidayBusinessService) ServiceFactory.getInstance().getBusinessService(BusinessServiceName.Holiday);
	}
		
	private void doCleanUp(HttpServletRequest request) {
		SessionUtils.setAttribute(HolidayConstants.HOLIDAY_ACTIONFORM, null,request.getSession());
	}
	
	@TransactionDemarcate(joinToken = true)
	public ActionForward preview(ActionMapping mapping, ActionForm form,
			HttpServletRequest request, HttpServletResponse response)
			throws Exception {

		UserContext uc = (UserContext) SessionUtils.getAttribute(
				Constants.USER_CONTEXT_KEY, request.getSession());
				
		SessionUtils.setCollectionAttribute(HolidayConstants.REPAYMENTRULETYPES, 
				getRepaymentRuleTypes(uc.getLocaleId()),request);
		
		return mapping.findForward(ActionForwards.preview_success.toString());
	}
	
	
	@TransactionDemarcate(joinToken = true)
	public ActionForward previous(ActionMapping mapping, ActionForm form,
			HttpServletRequest request, HttpServletResponse response)
			throws Exception {
		
		UserContext userContext = (UserContext) SessionUtils.getAttribute(
				Constants.USER_CONTEXT_KEY, request.getSession());
		
		SessionUtils.setCollectionAttribute(HolidayConstants.REPAYMENTRULETYPES, 
				getRepaymentRuleTypes(userContext.getLocaleId()),request);
		
		return mapping.findForward(ActionForwards.previous_success.toString());
	}
	
	@TransactionDemarcate(validateAndResetToken = true)
	public ActionForward cancelCreate(ActionMapping mapping, ActionForm form,
			HttpServletRequest request, HttpServletResponse response)
			throws Exception {
		return mapping.findForward(ActionForwards.cancelCreate_success
				.toString());
	}

	@TransactionDemarcate(saveToken = true)
	public ActionForward get(ActionMapping mapping, ActionForm form,
			HttpServletRequest request, HttpServletResponse response)
			throws Exception {
		
		UserContext userContext = (UserContext) SessionUtils.getAttribute(
				Constants.USER_CONTEXT_KEY, request.getSession());
		
		SessionUtils.setCollectionAttribute(HolidayConstants.HOLIDAYLIST1, getHolidays(Calendar.getInstance().get(Calendar.YEAR), userContext.getLocaleId()),request);
		SessionUtils.setCollectionAttribute(HolidayConstants.HOLIDAYLIST2, getHolidays(Calendar.getInstance().get(Calendar.YEAR)+1, userContext.getLocaleId()),request);
				
		return mapping.findForward(ActionForwards.get_success.toString());
	}

	@TransactionDemarcate(joinToken = true)
	public ActionForward getEditStates(ActionMapping mapping, ActionForm form,
			HttpServletRequest request, HttpServletResponse response)
			throws Exception {		
		return mapping.findForward(ActionForwards.manage_success.toString());
	}

	@TransactionDemarcate(joinToken = true)
	public ActionForward managePreview(ActionMapping mapping, ActionForm form,
			HttpServletRequest request, HttpServletResponse response)
			throws Exception {

		return mapping.findForward(ActionForwards.managepreview_success
				.toString());
	}

	@TransactionDemarcate(joinToken = true)
	public ActionForward managePrevious(ActionMapping mapping, ActionForm form,
			HttpServletRequest request, HttpServletResponse response)
			throws Exception {
		return mapping.findForward(ActionForwards.manageprevious_success
				.toString());
	}
	

	// @CloseSession
	@TransactionDemarcate(validateAndResetToken = true)
	public ActionForward update(ActionMapping mapping, ActionForm form,
			HttpServletRequest request, HttpServletResponse response)
			throws Exception {
		
		HolidayActionForm holidayActionForm = (HolidayActionForm) form;
		
		HolidayPK holidayPK = new HolidayPK((short)1,holidayActionForm.getFromDate());
		
		HolidayBO accountHoliday = new HolidayBO(holidayPK, 
				holidayActionForm.getThruDate(), 
				holidayActionForm.getHolidayName(), 
				getUserContext(request).getLocaleId(), 
				Short.parseShort(holidayActionForm.getRepaymentRuleId()), "");

		accountHoliday.update(holidayPK, 
							  holidayActionForm.getThruDate(), 
							  holidayActionForm.getHolidayName(), 
							  getUserContext(request).getLocaleId());
		if (null != request.getParameter(Constants.CURRENTFLOWKEY))
			request.setAttribute(Constants.CURRENTFLOWKEY, request.getParameter("currentFlowKey"));
		
		FlowManager flowManager = new FlowManager(); 
		Flow flow = new Flow();
		flow.addObjectToSession(Constants.CURRENTFLOWKEY, request.getParameter("currentFlowKey"));
		flowManager.addFLow(Constants.CURRENTFLOWKEY, flow, this.clazz.getName());
		request.setAttribute(Constants.CURRENTFLOWKEY, request.getParameter("currentFlowKey"));
		
		return mapping.findForward(ActionForwards.update_success.toString());
	}

	
	@TransactionDemarcate(validateAndResetToken = true)
	public ActionForward cancelManage(ActionMapping mapping, ActionForm form,
			HttpServletRequest request, HttpServletResponse response)
			throws Exception {
		return mapping
				.findForward(ActionForwards.cancelEdit_success.toString());
	}

	@TransactionDemarcate(joinToken = true)
	public ActionForward validate(ActionMapping mapping, ActionForm form,
			HttpServletRequest request, HttpServletResponse httpservletresponse)
			throws Exception {
		
		String method = (String) request.getAttribute("methodCalled");
		return mapping.findForward(method + "_failure");
	}
}
