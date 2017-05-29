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

import com.atlassian.jira.rest.client.api.domain.Issue;
import com.atlassian.jira.rest.client.api.domain.IssueField;
import com.bbva.arq.devops.ae.mirrorgate.collectors.jira.config.FieldsConfig;
import com.bbva.arq.devops.ae.mirrorgate.core.dto.SprintDTO;
import com.bbva.arq.devops.ae.mirrorgate.core.utils.SprintStatus;
import org.codehaus.jettison.json.JSONArray;
import org.codehaus.jettison.json.JSONException;
import org.codehaus.jettison.json.JSONObject;
import org.joda.time.DateTime;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Component;

import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

/**
 * Created by alfonso on 26/05/17.
 */

@Component
public class JiraIssueUtils {

    private static Logger LOGGER = LoggerFactory.getLogger(JiraIssueUtils.class);

    @Autowired
    @Qualifier(FieldsConfig.KEYWORDS_FIELD_BEAN)
    private List<String> keywordsFields;

    @Autowired
    @Qualifier(FieldsConfig.JIRA_FIELDS_BEAN)
    private Map<JiraIssueFields, String> jiraFields;

    public static class JiraIssueField<T>{

        T value;

        protected JiraIssueField(Object value) {
            this.value = (T) value;
        }

        public T get() {
            return value;
        }

    }

    public static Object getFieldValue(Issue issue, String field) {
        Object out = null;
        IssueField iField = issue.getField(field);

        if(iField != null ) {
            out = iField.getValue();
        }

        return out;
    }

    public JiraIssueField getField(Issue issue, JiraIssueFields outputType) {
        return new JiraIssueField<>(getFieldValue(issue, jiraFields.get(outputType)));
    }

    public <T> JiraIssueField<T> getField(Issue issue, JiraIssueFields field, Class<T> outputType) {
        return new JiraIssueField<>(getFieldValue(issue, jiraFields.get(field)));
    }

    private <T> T parse(String s, Class<T> type) {
        if(s == null || s.equals("<null>")) {
            return null;
        } else if(type.isEnum()) {
            Class<? extends Enum> enumType = (Class<? extends Enum>) type;
            return (T) Enum.valueOf(enumType, s);
        } else if(type == Date.class){
            return (T) DateTime.parse(s).toDate();
        } else if(type == String.class){
            return (T) s;
        }
        return null;
    }

    public SprintDTO getPriorSprint(Object o) {
        if(o == null) {
            return null;
        }

        List<String> sprints = null;

        if(o instanceof JSONArray) {
            JSONArray array = (JSONArray) o;
            sprints = new ArrayList<>(array.length());

            for (int i = 0; i < array.length(); i++) {
                try {
                    sprints.add((String) array.get(i));
                } catch (JSONException e) {
                    LOGGER.error("Error parsing sprint field", e);
                }
            }
        } else if(o instanceof List) {
            sprints = (List<String>) o;
        }

        return getPriorSprint(sprints);
    }

    public SprintDTO getPriorSprint(List<String> data) {
        List<SprintDTO> sprints = getSprintList(data);
        return sprints == null || sprints.size() == 0?
                null:
                sprints.get(0);
    }

    public List<SprintDTO> getSprintList(List<String> data) {
        return data == null ?
                null:
                data.stream().map(this::parseSprint).collect(Collectors.toList());
    }

    public SprintDTO parseSprint(String data) {
        if(data == null || !data.startsWith("com.atlassian.greenhopper.service.sprint.Sprint")){
            return null;
        }

        Matcher match = Pattern.compile("([^=\\[,]*)=([^,\\]]*)").matcher(data);
        Map<String,String> fieldsAndValue= new HashMap<>();

        while(match.find()) {
            fieldsAndValue.put(match.group(1), match.group(2));
        }
        return new SprintDTO()
                .setId(fieldsAndValue.get("id"))
                .setStatus(parse(fieldsAndValue.get("state"),SprintStatus.class))
                .setName(fieldsAndValue.get("name"))
                .setStartDate(parse(fieldsAndValue.get("startDate"),Date.class))
                .setEndDate(parse(fieldsAndValue.get("endDate"),Date.class))
                .setCompleteDate(parse(fieldsAndValue.get("completeDate"),Date.class));
    }

    public List<String> buildKeywords(Issue issue) {
        List<String> keywords = new ArrayList<>();

        if(issue.getProject() != null) {
            if(issue.getProject().getName() != null) {
                keywords.add(issue.getProject().getName());
            }
            if(issue.getProject().getKey() != null) {
                keywords.add(issue.getProject().getKey());
            }
        }

        keywords.addAll(keywordsFields
                .stream()
                .map((f) -> issue.getField(f))
                .filter((v) -> v != null)
                .map(IssueField::getValue)
                .filter((v) -> v != null)
                .map((v) -> {
                    if(v instanceof JSONObject) {
                        try {
                            return ((JSONObject) v).get("value");
                        } catch (JSONException e) {
                            LOGGER.error("Error parsing customfield: " + v, e);
                        }
                    }
                    return v;
                })
                .filter((v) -> v != null)
                .map(Object::toString)
                .collect(Collectors.toList()));

        return keywords;
    }

}
