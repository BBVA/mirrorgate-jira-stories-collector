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
import com.atlassian.jira.rest.client.api.domain.IssueLink;
import com.atlassian.jira.rest.client.api.domain.IssueLinkType.Direction;
import com.bbva.arq.devops.ae.mirrorgate.collectors.jira.config.FieldsConfig;
import com.bbva.arq.devops.ae.mirrorgate.collectors.jira.service.StatusMapService;
import com.bbva.arq.devops.ae.mirrorgate.core.dto.IssueDTO;
import com.bbva.arq.devops.ae.mirrorgate.core.dto.ProjectDTO;
import com.bbva.arq.devops.ae.mirrorgate.core.dto.SprintDTO;
import com.bbva.arq.devops.ae.mirrorgate.core.utils.IssuePriority;
import com.bbva.arq.devops.ae.mirrorgate.core.utils.SprintStatus;
import java.lang.reflect.Array;
import java.net.URI;
import java.util.function.Function;
import java.util.stream.StreamSupport;
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

    @Autowired
    private StatusMapService statusMapService;

    public static class JiraIssueField<T>{

        T value;

        protected JiraIssueField(Object value) {
            this.value = (T) value;
        }

        public T get() {
            return value;
        }

    }

    private static class SprintDateComparator implements Comparator<SprintDTO> {

        @Override
        public int compare(SprintDTO o1, SprintDTO o2) {
            if(o1.getStatus() != o2.getStatus()) {
                if (o1.getStatus() == SprintStatus.ACTIVE) {
                    return -1;
                } else if (o2.getStatus() == SprintStatus.ACTIVE) {
                    return 1;
                } else if (o1.getStatus() == SprintStatus.FUTURE) {
                    return -1;
                } else if (o2.getStatus() == SprintStatus.FUTURE) {
                    return 1;
                }
            }
            return o1.getEndDate() != null ? o1.getEndDate().compareTo(o2.getEndDate()) : 1;
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

    public List<String> objectToStringList(Object o){
        if(o == null) {
            return null;
        }

        List<String> stringList = null;

        if(o instanceof JSONArray) {
            JSONArray array = (JSONArray) o;
            stringList = new ArrayList<>(array.length());

            for (int i = 0; i < array.length(); i++) {
                try {
                    stringList.add((String) array.get(i));
                } catch (JSONException e) {
                    LOGGER.error("Error parsing sprint field", e);
                }
            }
        } else if(o instanceof List) {
            stringList = (List<String>) o;
        }

        return stringList;
    }

    public SprintDTO getPriorSprint(Object o) {
        if(o == null) {
            return null;
        }

        return getPriorSprint(objectToStringList(o));
    }

    public SprintDTO getPriorSprint(List<String> data) {
        List<SprintDTO> sprints = getSprintList(data);
        SprintDTO latest = null;
        if(sprints != null && sprints.size() > 0) {
            sprints.sort(new SprintDateComparator());
            latest = sprints.get(0);
        }
        return latest;
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

    public String getParentIssueKey(Issue issue){
        Optional<IssueLink> issueLink = getInboundLinks(issue);

        String parentKey = null;
        if(issueLink.isPresent()){
            parentKey = issueLink.map(IssueLink::getTargetIssueKey).get();
        }
        return parentKey;
    }

    public String getParentIssueId(Issue issue){
        Optional<IssueLink> issueLink = getInboundLinks(issue);

        Optional<URI> uri = issueLink.map(IssueLink::getTargetIssueUri);

        String parentIssueId = null;
        if(uri.isPresent()){
            String[] pathParts = uri.get().getPath().split("/");
            parentIssueId = pathParts[pathParts.length-1];
        }

        return parentIssueId;
    }

    private Optional<IssueLink> getInboundLinks(Issue issue){

        return StreamSupport
            .stream(issue.getIssueLinks().spliterator(), false)
            .filter(i -> i.getIssueLinkType().getDirection().equals(Direction.INBOUND))
            .findFirst();
    }

    public IssueDTO map(Issue issue) {
        return new IssueDTO()
                .setId(issue.getId())
                .setName(issue.getSummary())
                .setJiraKey(issue.getKey())
                .setPiNames(objectToStringList(getField(issue, JiraIssueFields.PI, List.class).get()))
                .setParentKey(getParentIssueKey(issue))
                .setParentId(getParentIssueId(issue))
                //Why create JiraIssueFields with an attached class type when we have to pass it in this method?
                .setEstimate(getField(issue, JiraIssueFields.STORY_POINTS, Double.class).get())
                .setType(issue.getIssueType().getName())
                .setStatus(statusMapService.getStatusFor(issue.getStatus().getName()))
                .setPriority(issue.getPriority() != null ? IssuePriority.fromName(issue.getPriority().getName()): null)
                .setSprint(getPriorSprint(getField(issue, JiraIssueFields.SPRINT).get()))
                .setType(issue.getIssueType().getName())
                .setUpdatedDate(issue.getUpdateDate().toDate())
                .setProject(issue.getProject() == null ? null :
                        new ProjectDTO()
                                .setId(issue.getProject().getId())
                                .setName(issue.getProject().getName())
                                .setKey(issue.getProject().getKey())
                )
                .setKeywords(buildKeywords(issue));
    }

}
