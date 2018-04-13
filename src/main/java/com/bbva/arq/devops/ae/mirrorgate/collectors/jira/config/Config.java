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

package com.bbva.arq.devops.ae.mirrorgate.collectors.jira.config;

import com.atlassian.jira.rest.client.api.JiraRestClient;
import com.atlassian.jira.rest.client.api.MetadataRestClient;
import com.atlassian.jira.rest.client.api.SearchRestClient;
import com.atlassian.jira.rest.client.internal.async.AsynchronousJiraRestClientFactory;
import com.bbva.arq.devops.ae.mirrorgate.collectors.jira.support.IssueStatus;
import com.bbva.arq.devops.ae.mirrorgate.collectors.jira.support.IssueType;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.*;
import java.util.stream.Collectors;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.http.client.support.BasicAuthorizationInterceptor;
import org.springframework.http.converter.json.MappingJackson2HttpMessageConverter;
import org.springframework.web.client.RestTemplate;

@org.springframework.context.annotation.Configuration
public class Config {

    private JiraRestClient restClientInstance;

    public static final String MIRRORGATE_REST_TEMPLATE = "MirrorGateRestTemplate";
    public static final String JIRA_REST_TEMPLATE = "JiraRestTemplate";
    public static final String JIRA_STATUS_MAPPING = "JiraStatusMapping";
    public static final String JIRA_TYPES_MAPPING = "JiraTypeMapping";
    public static final String JIRA_TYPES = "JiraTypes";

    @Value("${jira.url}")
    private String jiraUrl;

    @Value("${jira.userName}")
    private String jiraUserName;

    @Value("${jira.password}")
    private String jiraPassword;

    @Value("${mirrorgate.userName:}")
    private Optional<String> mirrorGateUserName;

    @Value("${mirrorgate.password:}")
    private Optional<String> mirrorGatePassword;

    @Value("${jira.timezone:}")
    private Optional<String> jiraTimeZone;

    @Value("#{'${jira.types.mappings.bug}'.split(',')}")
    private List<String> bugTypes;

    @Value("#{'${jira.types.mappings.epic}'.split(',')}")
    private List<String> epicTypes;

    @Value("#{'${jira.types.mappings.feature}'.split(',')}")
    private List<String> featureTypes;

    @Value("#{'${jira.types.mappings.story}'.split(',')}")
    private List<String> storyTypes;

    @Value("#{'${jira.types.mappings.task}'.split(',')}")
    private List<String> taskTypes;

    @Value("#{'${jira.status.mappings.backlog}'.split(',')}")
    private List<String> backlogStatus;

    @Value("#{'${jira.status.mappings.impeded}'.split(',')}")
    private List<String> impededStatus;

    @Value("#{'${jira.status.mappings.inProgress}'.split(',')}")
    private List<String> inProgressStatus;

    @Value("#{'${jira.status.mappings.done}'.split(',')}")
    private List<String> doneStatus;

    @Value("#{'${jira.status.mappings.waiting}'.split(',')}")
    private List<String> waitingStatus;

    @Bean
    public synchronized JiraRestClient getJiraRestClient() {
        if(restClientInstance == null) {
            URI jiraServerUri = null;
            try {
                jiraServerUri = new URI(jiraUrl);
            } catch (URISyntaxException e) {
                e.printStackTrace();
            }
            restClientInstance = new AsynchronousJiraRestClientFactory()
                    .createWithBasicHttpAuthentication(jiraServerUri,
                            jiraUserName,
                            jiraPassword);
        }
        return restClientInstance;
    }

    @Bean
    public SearchRestClient getSearchRestClient() {
        return this.getJiraRestClient().getSearchClient();
    }

    @Bean
    public MetadataRestClient getMetadataRestClient() {
        return this.getJiraRestClient().getMetadataClient();
    }

    @Bean(MIRRORGATE_REST_TEMPLATE)
    public RestTemplate getRestTemplate() {
        RestTemplate restTemplate = new RestTemplate();
        MappingJackson2HttpMessageConverter jsonHttpMessageConverter = new MappingJackson2HttpMessageConverter();
        restTemplate.getMessageConverters().add(jsonHttpMessageConverter);
        if(mirrorGateUserName.isPresent() && !mirrorGateUserName.get().isEmpty()) {
            restTemplate.getInterceptors().add(
                    new BasicAuthorizationInterceptor(mirrorGateUserName.get(), mirrorGatePassword.get()));
        }

        return restTemplate;
    }

    @Bean(JIRA_REST_TEMPLATE)
    public RestTemplate getJiraRestTemplate() {
        RestTemplate restTemplate = new RestTemplate();
        MappingJackson2HttpMessageConverter jsonHttpMessageConverter = new MappingJackson2HttpMessageConverter();
        restTemplate.getMessageConverters().add(jsonHttpMessageConverter);
        restTemplate.getInterceptors().add(
                new BasicAuthorizationInterceptor(jiraUserName, jiraPassword));
        return restTemplate;
    }

    @Bean(JIRA_TYPES_MAPPING)
    public Map<String, IssueType> getJiraTypeMapping() {
        Map<String, IssueType> issueTypeDefaults = new HashMap<>(10);

        bugTypes.forEach((t) -> issueTypeDefaults.put(t, IssueType.BUG));
        featureTypes.forEach((t) -> issueTypeDefaults.put(t, IssueType.FEATURE));
        storyTypes.forEach((t) -> issueTypeDefaults.put(t, IssueType.STORY));
        taskTypes.forEach((t) -> issueTypeDefaults.put(t, IssueType.TASK));
        epicTypes.forEach((t) -> issueTypeDefaults.put(t, IssueType.EPIC));

        return issueTypeDefaults;

    }

    @Bean(JIRA_TYPES)
    public String getJiraTypes() {

        Set<String> all = new HashSet<>();
        all.addAll(bugTypes.stream().filter((s) -> s.length() > 0).collect(Collectors.toList()));
        all.addAll(featureTypes.stream().filter((s) -> s.length() > 0).collect(Collectors.toList()));
        all.addAll(storyTypes.stream().filter((s) -> s.length() > 0).collect(Collectors.toList()));
        all.addAll(taskTypes.stream().filter((s) -> s.length() > 0).collect(Collectors.toList()));
        all.addAll(epicTypes.stream().filter((s) -> s.length() > 0).collect(Collectors.toList()));

        return String.join(",", all);
    }

    @Bean(JIRA_STATUS_MAPPING)
    public Map<String, IssueStatus> getJiraStatusMapping() {
        Map<String, IssueStatus> issueStatusDefaults = new HashMap<>(10);

        backlogStatus.forEach((t) -> issueStatusDefaults.put(t, IssueStatus.BACKLOG));
        impededStatus.forEach((t) -> issueStatusDefaults.put(t, IssueStatus.IMPEDED));
        inProgressStatus.forEach((t) -> issueStatusDefaults.put(t, IssueStatus.IN_PROGRESS));
        doneStatus.forEach((t) -> issueStatusDefaults.put(t, IssueStatus.DONE));
        waitingStatus.forEach((t) -> issueStatusDefaults.put(t, IssueStatus.WAITING));

        return issueStatusDefaults;

    }

    @Bean
    public TimeZone getTimeZone() {
        TimeZone tz = TimeZone.getDefault();
        if(jiraTimeZone.isPresent() && jiraTimeZone.get().length() > 0) {
            tz = TimeZone.getTimeZone(jiraTimeZone.get());
        }

        return tz;
    }
}
