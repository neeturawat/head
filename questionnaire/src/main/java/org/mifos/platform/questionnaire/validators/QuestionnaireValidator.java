/*
 * Copyright (c) 2005-2010 Grameen Foundation USA
 *  All rights reserved.
 *
 *  Licensed under the Apache License, Version 2.0 (the "License");
 *  you may not use this file except in compliance with the License.
 *  You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or
 *  implied. See the License for the specific language governing
 *  permissions and limitations under the License.
 *
 *  See also http://www.apache.org/licenses/LICENSE-2.0.html for an
 *  explanation of the license and how it is applied.
 */

package org.mifos.platform.questionnaire.validators;

import org.mifos.framework.exceptions.SystemException;
import org.mifos.platform.questionnaire.service.dtos.EventSourceDto;
import org.mifos.platform.questionnaire.service.QuestionDetail;
import org.mifos.platform.questionnaire.service.QuestionGroupDetail;
import org.mifos.platform.questionnaire.service.dtos.QuestionDto;
import org.mifos.platform.questionnaire.service.dtos.QuestionGroupDto;

import java.util.List;


public interface QuestionnaireValidator {
    void validateForDefineQuestion(QuestionDetail questionDetail) throws SystemException;

    void validateForDefineQuestionGroup(QuestionGroupDetail questionGroupDetail) throws SystemException;

    void validateForEventSource(EventSourceDto eventSourceDto) throws SystemException;

    void validateForQuestionGroupResponses(List<QuestionGroupDetail> questionGroupDetails);

    void validateForDefineQuestionGroup(QuestionGroupDto questionGroupDto, boolean withDuplicateQuestionTextCheck);
    void validateForDefineQuestionGroup(QuestionGroupDto questionGroupDto);

    void validateForDefineQuestion(QuestionDto questionDto);

}
