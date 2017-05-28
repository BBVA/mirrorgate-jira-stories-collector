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

package com.bbva.arq.devops.ae.mirrorgate.collectors.jira.service;

import com.atlassian.jira.rest.client.api.SearchRestClient;
import com.atlassian.jira.rest.client.api.domain.SearchResult;
import com.atlassian.util.concurrent.Promise;
import com.bbva.arq.devops.ae.mirrorgate.collectors.jira.api.CollectorService;
import com.bbva.arq.devops.ae.mirrorgate.collectors.jira.model.Project;
import com.bbva.arq.devops.ae.mirrorgate.collectors.jira.support.Counter;
import com.bbva.arq.devops.ae.mirrorgate.collectors.jira.support.JiraIssueFields;
import com.bbva.arq.devops.ae.mirrorgate.collectors.jira.model.Issue;
import com.bbva.arq.devops.ae.mirrorgate.collectors.jira.support.JiraIssueUtils;
import com.bbva.arq.devops.ae.mirrorgate.collectors.jira.support.Pageable;
import org.joda.time.DateTime;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Function;
import java.util.stream.Collectors;
import java.util.stream.StreamSupport;

/**
 * Created by alfonso on 26/05/17.
 */
@Component
public class JiraIssuesServiceImpl implements IssuesService {

    private static final Logger LOGGER = LoggerFactory.getLogger(JiraIssuesServiceImpl.class);

    private static final String ISSUES_QUERY_PATTERN="updatedDate>='%s' AND issueType in(%s) ORDER BY updated ASC";
    private static final String ISSUES_BY_ID_QUERY_PATTERN="id IN (%s)";
    private static final int PAGE_SIZE=10;

    @Value("${jira.issue.types:Epic,Feature,Story,Bug,Task}")
    private String issueTypes;

    private SearchRestClient client;
    private CollectorStatusService collectorStatusService;
    private StatusMapService statusMapService;
    private JiraIssueUtils utils;

    @Autowired
    public JiraIssuesServiceImpl(SearchRestClient jiraRestClient,
                                 CollectorStatusService collectorStatusService,
                                 StatusMapService statusMapService,
                                 JiraIssueUtils jiraIssueUtils
    ) {
        this.client = jiraRestClient;
        this.collectorStatusService = collectorStatusService;
        this.statusMapService = statusMapService;
        this.utils = jiraIssueUtils;
    }

    @Override
    public Pageable<Issue> getRecentIssues() {
        final Counter page = new Counter(PAGE_SIZE);

        String query = String.format(ISSUES_QUERY_PATTERN,
                collectorStatusService.getLastExecutionDate().toString("yyyy-MM-dd HH:mm"),
                issueTypes);

        return (() -> {

            Promise<SearchResult> results = client.searchJql(query,PAGE_SIZE,page.inc(),null);

            return StreamSupport.stream(results.claim().getIssues().spliterator(),false)
                    .map(getIssueMapper()).collect(Collectors.toList());
        });
    }

    public Pageable<Issue> getById(List<Long> ids) {
        final StringBuilder sb = new StringBuilder(200);
        final Counter counter = new Counter();

        return (() -> {
            if(counter.get() >= ids.size()) {
                return new ArrayList<>();
            }
            for(int i = 0; i < PAGE_SIZE && counter.get() < ids.size(); counter.inc(), i++) {
                if(i > 0) {
                    sb.append(',');
                }
                sb.append(ids.get(i));
            }
            String query = String.format(ISSUES_BY_ID_QUERY_PATTERN, sb.toString());
            sb.delete(0,sb.length());

            LOGGER.info("-> Running Jira Query: " + query);

            Promise<SearchResult> results = client.searchJql(query);

            return StreamSupport.stream(results.claim().getIssues().spliterator(),false)
                    .map(getIssueMapper()).collect(Collectors.toList());
        });

    }

    private Function<com.atlassian.jira.rest.client.api.domain.Issue, Issue> getIssueMapper() {
        return (issue) ->
                new Issue()
                        .setId(issue.getId())
                        .setName(issue.getSummary())
                        .setEstimate(utils.getField(issue, JiraIssueFields.STORY_POINTS, Double.class).get())
                        .setType(issue.getIssueType().getName())
                        .setStatus(statusMapService.getStatusMappings().get(issue.getStatus().getName()))
                        .setSprint(utils.getPriorSprint(utils.getField(issue, JiraIssueFields.SPRINT).get()))
                        .setType(issue.getIssueType().getName())
                        .setUpdatedDate(issue.getUpdateDate().toDate())
                        .setProject(issue.getProject() == null ? null :
                                new Project()
                                        .setId(issue.getProject().getId())
                                        .setName(issue.getProject().getName())
                                        .setKey(issue.getProject().getKey())
                        )
                        .setKeywords(utils.buildKeywords(issue));
    }
}
