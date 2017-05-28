/*
 * Copyright 2017 Banco Bilbao Vizcaya Argentaria, S.A.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.bbva.arq.devops.ae.mirrorgate.collectors.jira;

import static org.junit.Assert.*;

import com.bbva.arq.devops.ae.mirrorgate.collectors.jira.model.Sprint;
import com.bbva.arq.devops.ae.mirrorgate.collectors.jira.model.SprintStatus;
import com.bbva.arq.devops.ae.mirrorgate.collectors.jira.support.JiraIssueUtils;
import org.joda.time.DateTime;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.JUnit4;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.PropertySources;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringRunner;


/**
 * Created by alfonso on 27/05/17.
 */

@RunWith(SpringRunner.class)
@SpringBootTest
public class SprintParserTests {

    @Autowired
    JiraIssueUtils issueUtils;

    @Test
    public void itShouldParseSprintsCorrectly() {
        String in = "com.atlassian.greenhopper.service.sprint.Sprint@20d5ab81[id=1003,rapidViewId=879,state=CLOSED,name=SOME_SPRINT,startDate=2017-02-22T07:00:27.314+01:00,endDate=2017-03-07T19:00:00.000+01:00,completeDate=2017-03-08T10:29:26.122+01:00,sequence=1003]";
        Sprint out = issueUtils.parseSprint(in);

        assertEquals(out.getId(), "1003");
        assertEquals(out.getStatus(), SprintStatus.CLOSED);
        assertEquals(out.getName(), "SOME_SPRINT");
        assertEquals(out.getStartDate(), DateTime.parse("2017-02-22T07:00:27.314+01:00").toDate());
        assertEquals(out.getEndDate(), DateTime.parse("2017-03-07T19:00:00.000+01:00").toDate());
        assertEquals(out.getCompleteDate(), DateTime.parse("2017-03-08T10:29:26.122+01:00").toDate());

    }

    @Test
    public void itShouldNotFailWhenMissingFields() {
        String in = "com.atlassian.greenhopper.service.sprint.Sprint@20d5ab81[id=1003]";
        Sprint out = issueUtils.parseSprint(in);

        assertEquals(out.getId(), "1003");
        assertEquals(out.getStatus(), null);
        assertEquals(out.getName(), null);
        assertEquals(out.getStartDate(), null);
        assertEquals(out.getEndDate(), null);
        assertEquals(out.getCompleteDate(), null);

    }

    @Test
    public void itShouldNotResturnSprintWhenDoesntMatch() {
        String in = "someother.class@20d5ab81[id=1003]";
        Sprint out = issueUtils.parseSprint(in);

        assertEquals(out, null);

    }

}
