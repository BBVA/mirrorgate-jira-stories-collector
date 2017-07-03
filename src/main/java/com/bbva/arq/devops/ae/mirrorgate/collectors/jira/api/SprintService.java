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
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Component;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.util.UriComponentsBuilder;

import java.util.Arrays;
import java.util.List;

/**
 * Created by alfonso on 26/05/17.
 */
@Component
public class SprintService {

    private static final Logger LOGGER = LoggerFactory.getLogger(SprintService.class);

    @Value("${mirrorgate.url}")
    private String mirrorGateUrl;

    @Value("${spring.application.name}")
    private String collectorId;

    private static final String MIRROR_GATE_SEND_ISSUES_ENDPOINT="/api/issues";
    private static final String MIRROR_GATE_HANDLE_ISSUE_ENDPOINT ="/api/issues/{id}";
    private static final String MIRROR_GATE_GET_SPRINT_SAMPLE_ENDPOINT="/api/sprints/changing-sample";
    private static final String MIRROR_GATE_GET_SPRINT_ISSUES_ENDPOINT="/api/sprints/{id}";

    @Autowired
    @Qualifier(Config.MIRRORGATE_REST_TEMPLATE)
    RestTemplate restTemplate;

    public ResponseEntity<List> sendIssues(List<IssueDTO> issues) {
        MultiValueMap<String, String> params = new LinkedMultiValueMap<String, String>();
        params.set("collectorId", collectorId);

        UriComponentsBuilder builder = UriComponentsBuilder.fromHttpUrl(mirrorGateUrl + MIRROR_GATE_SEND_ISSUES_ENDPOINT).queryParams(params);

        return restTemplate.postForEntity(builder.toUriString(), issues, List.class);
    }

    public void deleteIssue(Long issueId) {
        MultiValueMap<String, String> params = new LinkedMultiValueMap<String, String>();
        params.set("collectorId", collectorId);

        UriComponentsBuilder builder = UriComponentsBuilder.fromHttpUrl(mirrorGateUrl + MIRROR_GATE_HANDLE_ISSUE_ENDPOINT).queryParams(params);

        restTemplate.delete(builder.toUriString(), issueId);
    }

    public List<SprintDTO> getSprintSamples() {
        return Arrays.asList(restTemplate.getForObject(mirrorGateUrl + MIRROR_GATE_GET_SPRINT_SAMPLE_ENDPOINT,SprintDTO[].class));
    }

    public SprintDTO getSprint(String name) {
        try{
            return restTemplate.getForObject(mirrorGateUrl + MIRROR_GATE_GET_SPRINT_ISSUES_ENDPOINT, SprintDTO.class, name);
        }catch(Exception e){
            LOGGER.warn("Error getting sprint {}", name, e);
            return null;
        }
    }

}
