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

import com.atlassian.jira.rest.client.api.domain.Status;
import com.bbva.arq.devops.ae.mirrorgate.collectors.jira.config.Config;
import com.bbva.arq.devops.ae.mirrorgate.collectors.jira.support.IssueStatus;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Collectors;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestTemplate;

@Component
public class JiraStatusMapServiceImpl implements StatusMapService {

    private static final Logger LOGGER = LoggerFactory.getLogger(JiraStatusMapServiceImpl.class);

    private static final String SERVER_URI="/rest/api/2/status/";

    @Value("${jira.url}")
    private String jiraUrl;

    private final Map<String, IssueStatus> issueStatusMapping;

    private Map<Long, IssueStatus> statusCache;

    private final RestTemplate restTemplate;

    @Autowired
    public JiraStatusMapServiceImpl(
            @Qualifier(Config.JIRA_REST_TEMPLATE)
                    RestTemplate restTemplate,
            @Qualifier(Config.JIRA_STATUS_MAPPING)
                    Map<String, IssueStatus> issueStatusMapping
    ) {
        this.restTemplate = restTemplate;
        this.issueStatusMapping = issueStatusMapping;
    }

    private IssueStatus getStatus(Object status) {
        IssueStatus value = issueStatusMapping.get(getName(status));
        Object category = ((Map) status).get("statusCategory");
        if(value != null) {
            return value;
        }
        value = issueStatusMapping.get(getName(category));
        if(value != null) {
            return value;
        }
        value = issueStatusMapping.get(getField(category, "key"));
        return value != null ? value : IssueStatus.BACKLOG;
    }

    private static String getName(Object map) {
        return getField(map, "name");
    }

    private static String getField(Object map, String field) {
        return (String)((Map) map).get(field);
    }

    private synchronized Map<Long, IssueStatus> getStatusMappings() {

        if(statusCache == null) {
            List jsa = restTemplate.getForObject(jiraUrl + SERVER_URI, ArrayList.class);
            statusCache = (Map<Long, IssueStatus>) Objects.requireNonNull(jsa).stream().collect(Collectors.toMap(
                    (status) -> Long.parseLong(getField(status, "id")),
                    this::getStatus
            ));
        }

        return statusCache;
    }

    @Override
    public IssueStatus getStatusFor(Status status) {
        IssueStatus issueStatus = getStatusMappings().get(status.getId());
        if(issueStatus == null) {
            LOGGER.warn("IssueStatus not found for {} with id {}", status.getName(), status.getId());
        }
        return issueStatus;
    }
}
