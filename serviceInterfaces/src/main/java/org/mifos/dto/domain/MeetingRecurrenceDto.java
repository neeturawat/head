/*
 * Copyright (c) 2005-2011 Grameen Foundation USA
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

package org.mifos.dto.domain;

import java.io.Serializable;

@SuppressWarnings("PMD")
@edu.umd.cs.findbugs.annotations.SuppressWarnings(value="SE_NO_SERIALVERSIONID", justification="should disable at filter level and also for pmd - not important for us")
public class MeetingRecurrenceDto implements Serializable {

    private final Integer dayNumber;
    private final Integer weekOfMonth;
    private final Integer dayOfWeek;

    public MeetingRecurrenceDto(Integer dayNumber, Integer weekOfMonth, Integer dayOfWeek) {
        this.dayNumber = dayNumber;
        this.weekOfMonth = weekOfMonth;
        this.dayOfWeek = dayOfWeek;
    }

    public Integer getDayNumber() {
        return this.dayNumber;
    }

    public Integer getWeekOfMonth() {
        return this.weekOfMonth;
    }

    public Integer getDayOfWeek() {
        return this.dayOfWeek;
    }
}