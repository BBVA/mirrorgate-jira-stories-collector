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

import static com.bbva.arq.devops.ae.mirrorgate.collectors.jira.config.Config.JIRA_TYPES;

import com.atlassian.jira.rest.client.api.RestClientException;
import com.atlassian.jira.rest.client.api.SearchRestClient;
import com.atlassian.jira.rest.client.api.domain.SearchResult;
import com.bbva.arq.devops.ae.mirrorgate.collectors.jira.dto.IssueDTO;
import com.bbva.arq.devops.ae.mirrorgate.collectors.jira.support.Counter;
import com.bbva.arq.devops.ae.mirrorgate.collectors.jira.support.JiraIssueUtils;
import com.bbva.arq.devops.ae.mirrorgate.collectors.jira.support.Pageable;

import java.util.*;
import java.util.stream.Collectors;
import java.util.stream.StreamSupport;

import io.atlassian.util.concurrent.Promise;
import org.joda.time.DateTimeZone;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Component;

@Component
public class JiraIssuesServiceImpl implements IssuesService {

    private static final Logger LOGGER = LoggerFactory.getLogger(JiraIssuesServiceImpl.class);

    private static final String ISSUES_QUERY_PATTERN="updatedDate>='%s' AND issueType in(%s) ORDER BY updated ASC";
    private static final String ISSUES_BY_ID_QUERY_PATTERN="id IN (%s)";
    private static final int PAGE_SIZE=10;

    private final String issueTypes;

    private final SearchRestClient client;
    private final CollectorStatusService collectorStatusService;
    private final JiraIssueUtils utils;
    private final TimeZone jiraTimeZone;

    @Autowired
    public JiraIssuesServiceImpl(@Qualifier(JIRA_TYPES) String issueTypes,
                                 SearchRestClient jiraRestClient,
                                 CollectorStatusService collectorStatusService,
                                 JiraIssueUtils jiraIssueUtils,
                                 TimeZone jiraTimeZone
    ) {
        this.issueTypes = issueTypes;
        this.client = jiraRestClient;
        this.collectorStatusService = collectorStatusService;
        this.utils = jiraIssueUtils;
        this.jiraTimeZone = jiraTimeZone;
    }

    @Override
    public Pageable<IssueDTO> getRecentIssues() {
        final Counter page = new Counter(PAGE_SIZE);

        String date =
                collectorStatusService.getLastExecutionDate()
                        .toDateTime(DateTimeZone.forTimeZone(jiraTimeZone))
                        .toString("yyyy-MM-dd HH:mm");
        String query = String.format(ISSUES_QUERY_PATTERN,
                date,
                issueTypes);

        LOGGER.info("-> Running Jira Query: {}", query);

        return (() -> {

            Promise<SearchResult> results = client.searchJql(query,PAGE_SIZE,page.inc(),null);

            return StreamSupport.stream(results.claim().getIssues().spliterator(),false)
                    .map(utils::map).collect(Collectors.toList());
        });
    }

    @Override
    public Pageable<IssueDTO> getById(List<Long> ids) {
        final StringBuilder sb = new StringBuilder(200);
        final Counter counter = new Counter();

        return (() -> {
            int firstItem = counter.get();
            if(counter.get() >= ids.size()) {
                return new ArrayList<>();
            }
            for(int i = 0; i < PAGE_SIZE && counter.get() < ids.size(); counter.inc(), i++) {
                if(i > 0) {
                    sb.append(',');
                }
                sb.append(ids.get(counter.get()));
            }
            String query = String.format(ISSUES_BY_ID_QUERY_PATTERN, sb.toString());
            sb.delete(0,sb.length());

            LOGGER.info("-> Running Jira Query: {}", query);
            try {
                Promise<SearchResult> results = client.searchJql(query);
                return StreamSupport.stream(results.claim().getIssues().spliterator(),false)
                        .map(utils::map).collect(Collectors.toList());
            }  catch (RestClientException e) {
                LOGGER.warn("Exception", e);
                int statusCode = e.getStatusCode().isPresent() ? e.getStatusCode().get() : 0;
                if (statusCode == 401 ) {
                    LOGGER.error("Error 401 connecting to JIRA server, your credentials are probably wrong. Note: Ensure you are using JIRA user name not your email address.");
                    throw e;
                } else if(statusCode == 400) {
                    if(ids.size() == 1) {
                        return new ArrayList<>();
                    } else {
                        LOGGER.warn("Error 400 - Some issues where not found {}, keep on", ids);
                        LOGGER.warn(e.getMessage());
                        //Falling back to per issue invocation if one was not found... Why Jira o why...
                        List<IssueDTO> result = new ArrayList<>(PAGE_SIZE);
                        for (int i = firstItem; i < counter.get(); i++) {
                            result.addAll(getById(Collections.singletonList(ids.get(i))).nextPage());
                        }
                        return result;
                    }
                } else {
                    LOGGER.error("No result was available from Jira unexpectedly - defaulting to blank response. The reason for this fault is the following:" + e.getCause());
                    throw e;
                }
            }
        });

    }
}
