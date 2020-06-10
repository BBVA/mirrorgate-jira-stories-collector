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
import com.bbva.arq.devops.ae.mirrorgate.collectors.jira.dto.IssueDTO;
import com.bbva.arq.devops.ae.mirrorgate.collectors.jira.dto.SprintDTO;
import java.util.Arrays;
import java.util.List;
import java.util.Objects;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Component;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.util.UriComponentsBuilder;

@Component
public class SprintService {

    private static final Logger LOGGER = LoggerFactory.getLogger(SprintService.class);

    @Value("${mirrorgate.url}")
    private String mirrorGateUrl;

    @Value("${spring.application.name}")
    private String appName;

    private static final String MIRRORGATE_SEND_ISSUES_ENDPOINT = "/api/issues";
    private static final String MIRRORGATE_DELETE_ISSUE_ENDPOINT = "/api/issues/{id}";
    private static final String MIRRORGATE_GET_SPRINT_SAMPLE_ENDPOINT = "/api/sprints/changing-sample";
    private static final String MIRRORGATE_GET_SPRINT_ISSUES_ENDPOINT = "/api/sprints/{id}";

    @Autowired
    @Qualifier(Config.MIRRORGATE_REST_TEMPLATE)
    private RestTemplate restTemplate;

    public ResponseEntity<List> sendIssues(final List<IssueDTO> issues) {
        final MultiValueMap<String, String> params = new LinkedMultiValueMap<>();
        params.set("collectorId", appName);

        final UriComponentsBuilder builder = UriComponentsBuilder
            .fromHttpUrl(mirrorGateUrl + MIRRORGATE_SEND_ISSUES_ENDPOINT)
            .queryParams(params);

        return restTemplate.postForEntity(builder.build().toUriString(), issues, List.class);
    }

    public void deleteIssue(final Long issueId) {
        final MultiValueMap<String, String> params = new LinkedMultiValueMap<>();
        params.set("collectorId", appName);

        final UriComponentsBuilder builder = UriComponentsBuilder
            .fromHttpUrl(mirrorGateUrl + MIRRORGATE_DELETE_ISSUE_ENDPOINT).queryParams(params);

        try {
            restTemplate.delete(builder.build().toUriString(), issueId);
        } catch (final HttpClientErrorException e) {
            if (e.getStatusCode() == HttpStatus.NOT_FOUND) {
                LOGGER.warn("Issue {} already deleted", issueId);
            } else {
                LOGGER.error("Error trying to delete issue {}", issueId, e);
                throw e;
            }
        }

    }

    public List<SprintDTO> getSprintSamples() {
        final MultiValueMap<String, String> params = new LinkedMultiValueMap<>();
        params.set("collectorId", appName);

        final UriComponentsBuilder builder = UriComponentsBuilder
            .fromHttpUrl(mirrorGateUrl + MIRRORGATE_GET_SPRINT_SAMPLE_ENDPOINT)
            .queryParams(params);

        return Arrays.asList(
            Objects.requireNonNull(
                restTemplate.getForObject(builder.build().toUriString(), SprintDTO[].class)
            )
        );
    }

    public SprintDTO getSprint(final String name) {
        try {
            final MultiValueMap<String, String> params = new LinkedMultiValueMap<>();
            params.set("collectorId", appName);

            final UriComponentsBuilder builder = UriComponentsBuilder
                .fromHttpUrl(mirrorGateUrl + MIRRORGATE_GET_SPRINT_ISSUES_ENDPOINT)
                .queryParams(params);

            return restTemplate.getForObject(builder.build().toUriString(), SprintDTO.class, name);
        } catch (HttpClientErrorException e) {
            if (e.getStatusCode() == HttpStatus.NOT_FOUND) {
                LOGGER.warn("Sprint {} does not exist", name);
            } else {
                LOGGER.error("Error getting sprint {}", name, e);
                throw e;
            }
        }
        return null;
    }

}
