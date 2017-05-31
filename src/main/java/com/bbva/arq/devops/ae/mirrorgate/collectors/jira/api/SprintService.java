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

package com.bbva.arq.devops.ae.mirrorgate.collectors.jira.api;

import com.bbva.arq.devops.ae.mirrorgate.collectors.jira.config.Config;
import com.bbva.arq.devops.ae.mirrorgate.core.dto.IssueDTO;
import com.bbva.arq.devops.ae.mirrorgate.core.dto.SprintDTO;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestTemplate;

import java.util.Arrays;
import java.util.List;

/**
 * Created by alfonso on 26/05/17.
 */
@Component
public class SprintService {

    @Value("${mirrorgate.url}")
    private String mirrorGateUrl;

    private static final String MIRROR_GATE_SEND_ISSUES_ENDPOINT="/api/issues";
    private static final String MIRROR_GATE_HANDLE_ISSUE_ENDPOINT ="/api/issues/{id}";
    private static final String MIRROR_GATE_GET_SPRINT_SAMPLE_ENDPOINT="/api/sprints/changing-sample";
    private static final String MIRROR_GATE_GET_SPRINT_ISSUES_ENDPOINT="/api/sprints/{id}";

    @Autowired
    @Qualifier(Config.MIRRORGATE_REST_TEMPLATE)
    RestTemplate restTemplate;

    public ResponseEntity<List> sendIssues(List<IssueDTO> issues) {
        return restTemplate.postForEntity(mirrorGateUrl + MIRROR_GATE_SEND_ISSUES_ENDPOINT, issues, List.class);
    }

    public void deleteIssue(Long issueId) {
        restTemplate.delete(mirrorGateUrl + MIRROR_GATE_HANDLE_ISSUE_ENDPOINT, issueId);
    }

    public List<SprintDTO> getSprintSamples() {
        return Arrays.asList(restTemplate.getForObject(mirrorGateUrl + MIRROR_GATE_GET_SPRINT_SAMPLE_ENDPOINT,SprintDTO[].class));
    }

    public SprintDTO getSprint(String name) {
        return restTemplate.getForObject(mirrorGateUrl + MIRROR_GATE_GET_SPRINT_ISSUES_ENDPOINT, SprintDTO.class, name);
    }

}
