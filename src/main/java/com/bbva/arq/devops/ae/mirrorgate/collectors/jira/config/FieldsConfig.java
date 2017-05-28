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

import com.bbva.arq.devops.ae.mirrorgate.collectors.jira.support.JiraIssueFields;
import com.bbva.arq.devops.ae.mirrorgate.collectors.jira.support.JiraIssueUtils;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * Created by alfonso on 28/05/17.
 */

@Configuration
public class FieldsConfig {

    public static final String KEYWORDS_FIELD_BEAN = "KeywordsFieldsList";
    public static final String JIRA_FIELDS_BEAN = "JiraFieldIds";

    @Value("${jira.fields.storyPoints}")
    private String storyPointsField;

    @Value("${jira.fields.sprint}")
    private String sprintField;

    @Value("${jira.fields.keywordList}")
    private String keywordsList;

    @Bean(JIRA_FIELDS_BEAN)
    public Map<JiraIssueFields, String> getFieldIds() {
        Map<JiraIssueFields, String> fields = new HashMap<>();

        fields.put(JiraIssueFields.STORY_POINTS, storyPointsField);
        fields.put(JiraIssueFields.SPRINT, sprintField);
        fields.put(JiraIssueFields.KEYWORDS, keywordsList);

        return fields;
    }

    @Bean(KEYWORDS_FIELD_BEAN)
    public List<String> getKeywordsFields() {
        return Arrays.asList(getFieldIds().get(JiraIssueFields.KEYWORDS).split(",")).stream()
                .map(String::trim)
                .filter((s) -> s.length() > 0)
                .collect(Collectors.toList());
    }

}
