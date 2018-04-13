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

package com.bbva.arq.devops.ae.mirrorgate.collectors.jira.support;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;

import com.bbva.arq.devops.ae.mirrorgate.collectors.jira.dto.SprintDTO;
import com.bbva.arq.devops.ae.mirrorgate.collectors.jira.service.IssueTypeMapService;
import com.bbva.arq.devops.ae.mirrorgate.collectors.jira.service.StatusMapService;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import org.joda.time.DateTime;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.test.context.junit4.SpringRunner;

@RunWith(SpringRunner.class)
@SpringBootTest
public class JiraIssueUtilsTest {

    @Autowired
    private List<String> keywordsFields;

    @Autowired
    private Map<JiraIssueFields, String> jiraFields;

    @Autowired
    private StatusMapService statusMapService;

    @MockBean
    private IssueTypeMapService issueTypeMapService;

    private JiraIssueUtils issueUtils;

    @Before
    public void init(){
        issueUtils = new JiraIssueUtils(keywordsFields, jiraFields, statusMapService, issueTypeMapService);
    }

    @Test
    public void itShouldParseSprintsCorrectly() {
        String in = "com.atlassian.greenhopper.service.sprint.Sprint@20d5ab81[id=1003,rapidViewId=879,state=CLOSED,name=SOME_SPRINT,startDate=2017-02-22T07:00:27.314+01:00,endDate=2017-03-07T19:00:00.000+01:00,completeDate=2017-03-08T10:29:26.122+01:00,sequence=1003]";
        SprintDTO out = issueUtils.parseSprint(in);

        assertEquals(out.getId(), "1003");
        assertEquals(out.getStatus(), SprintStatus.CLOSED);
        assertEquals(out.getName(), "SOME_SPRINT");
        assertEquals(out.getStartDate(), DateTime.parse("2017-02-22T07:00:27.314+01:00").toDate());
        assertEquals(out.getEndDate(), DateTime.parse("2017-03-07T19:00:00.000+01:00").toDate());
        assertEquals(out.getCompleteDate(), DateTime.parse("2017-03-08T10:29:26.122+01:00").toDate());

    }

    @Test
    public void itShouldGetTheMoreRecentActiveSprint() {
        SprintDTO sprint = issueUtils.getPriorSprint(Arrays.asList(
                "com.atlassian.greenhopper.service.sprint.Sprint@19057bee[id=1879,rapidViewId=879,state=CLOSED,name=MIRRORGATE_PI03_2017_SP2,startDate=2017-06-14T11:59:37.474+02:00,endDate=2017-06-27T18:59:00.000+02:00,completeDate=2017-06-28T10:20:49.865+02:00,sequence=1879]",
                "com.atlassian.greenhopper.service.sprint.Sprint@81010b9[id=1941,rapidViewId=879,state=ACTIVE,name=MIRRORGATE_PI03_2017_SP3,startDate=2017-06-28T09:00:15.296+02:00,endDate=2017-07-11T21:00:00.000+02:00,completeDate=<null>,sequence=1941]")
        );

        assertEquals(sprint.getName(), "MIRRORGATE_PI03_2017_SP3");
    }

    @Test
    public void itShouldGetTheMoreRecentActiveSprintEvenWhenSooner() {
        SprintDTO sprint = issueUtils.getPriorSprint(Arrays.asList(
                "com.atlassian.greenhopper.service.sprint.Sprint@19057bee[id=1879,rapidViewId=879,state=CLOSED,name=MIRRORGATE_PI03_2017_SP2,startDate=2017-06-14T11:59:37.474+02:00,endDate=2018-06-27T18:59:00.000+02:00,completeDate=2017-06-28T10:20:49.865+02:00,sequence=1879]",
                "com.atlassian.greenhopper.service.sprint.Sprint@81010b9[id=1941,rapidViewId=879,state=ACTIVE,name=MIRRORGATE_PI03_2017_SP3,startDate=2017-06-28T09:00:15.296+02:00,endDate=2017-07-11T21:00:00.000+02:00,completeDate=<null>,sequence=1941]")
        );

        assertEquals(sprint.getName(), "MIRRORGATE_PI03_2017_SP3");
    }

    @Test
    public void itShouldGetActiveSprintAndNotFailOnMissingData() {
        SprintDTO sprint = issueUtils.getPriorSprint(Arrays.asList(
                "com.atlassian.greenhopper.service.sprint.Sprint@19057bee[id=1879,rapidViewId=879,state=CLOSED,name=MIRRORGATE_PI03_2017_SP2,startDate=2017-06-14T11:59:37.474+02:00,endDate=<null>,completeDate=2017-06-28T10:20:49.865+02:00,sequence=1879]",
                "com.atlassian.greenhopper.service.sprint.Sprint@81010b9[id=1941,rapidViewId=879,state=CLOSED,name=MIRRORGATE_PI03_2017_SP3,startDate=2017-06-28T09:00:15.296+02:00,endDate=<null>,completeDate=<null>,sequence=1941]")
        );

        assertNotNull(sprint.getName());
    }

    @Test
    public void itShouldNotFailWhenMissingFields() {
        String in = "com.atlassian.greenhopper.service.sprint.Sprint@20d5ab81[id=1003]";
        SprintDTO out = issueUtils.parseSprint(in);

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
        SprintDTO out = issueUtils.parseSprint(in);

        assertEquals(out, null);

    }

}
