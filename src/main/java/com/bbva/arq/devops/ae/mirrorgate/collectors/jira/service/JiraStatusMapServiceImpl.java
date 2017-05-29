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

/**
 * Created by alfonso on 26/05/17.
 */

import com.bbva.arq.devops.ae.mirrorgate.collectors.jira.config.Config;
import com.bbva.arq.devops.ae.mirrorgate.core.utils.IssueStatus;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestTemplate;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Component
public class JiraStatusMapServiceImpl implements StatusMapService {

    RestTemplate restTemplate;
    private static final String SERVER_URI="/rest/api/2/status/";

    @Value("${jira.url}")
    private String jiraUrl;

    private Map<String, IssueStatus> statusCache;

    private static final Map<String, IssueStatus> STATUS_DEFAULTS = new HashMap<String, IssueStatus>(){{
        put("To Do", IssueStatus.BACKLOG);
        put("Blocked", IssueStatus.IMPEDED);
        put("In Progress", IssueStatus.IN_PROGRESS);
        put("Done", IssueStatus.DONE);
        put("Waiting", IssueStatus.WAITING);
    }};

    private static IssueStatus getStatus(Object status) {
        String name = getName(status);
        if(STATUS_DEFAULTS.containsKey(name)) {
            return STATUS_DEFAULTS.get(name);
        } else {
            return STATUS_DEFAULTS.get(getName(((Map) status).get("statusCategory")));
        }
    }

    private static String getName(Object map) {
        return (String)((Map) map).get("name");
    }

    @Autowired
    public JiraStatusMapServiceImpl(
            @Qualifier(Config.JIRA_REST_TEMPLATE) RestTemplate restTemplate
    ) {
        this.restTemplate = restTemplate;
    }

    @Override
    public synchronized Map<String, IssueStatus> getStatusMappings() {

        if(statusCache == null) {
            List jsa = restTemplate.getForObject(jiraUrl + SERVER_URI, ArrayList.class);

            statusCache = (Map) jsa.stream().collect(Collectors.toMap(
                    (map) -> getName(map),
                    (map) -> getStatus(map)
            ));
        }

        return statusCache;
    }
}
