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

package com.bbva.arq.devops.ae.mirrorgate.collectors.jira.controller;

import com.atlassian.jira.rest.client.api.MetadataRestClient;
import com.atlassian.jira.rest.client.api.domain.Field;
import com.atlassian.jira.rest.client.internal.json.IssueJsonParser;
import com.atlassian.util.concurrent.Promise;
import com.bbva.arq.devops.ae.mirrorgate.collectors.jira.Main;
import com.bbva.arq.devops.ae.mirrorgate.collectors.jira.support.JiraIssueUtils;
import com.bbva.arq.devops.ae.mirrorgate.core.dto.IssueDTO;
import java.util.Arrays;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.ExecutionException;
import org.codehaus.jettison.json.JSONException;
import org.codehaus.jettison.json.JSONObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RestController;

/**
 * Created by alfonso on 13/07/17.
 */

@RestController
@RequestMapping("/webhook")
public class WebHookController {

    private enum JiraEvent {

        IssueCreated ("jira:issue_created"),
        IssueUpdated ("jira:issue_updated"),
        IssueDeleted ("jira:issue_deleted"),
        SprintCreated ("sprint_created"),
        SprintUpdated ("sprint_updated"),
        SprintDeleted ("sprint_deleted"),
        SprintOpened ("sprint_opened"),
        SprintClosed ("sprint_closed"),
        Unknown ("?");


        private String name;

        JiraEvent(String name) {
            this.name = name;
        }

        private String getName() {
            return name;
        }

        public static final JiraEvent fromName(String name) {
            Optional<JiraEvent> event = Arrays.stream(JiraEvent.values()).filter((e) -> e.getName().equals(name)).findFirst();
            return event.isPresent() ? event.get() : Unknown;
        }
    }

    private static final String WEB_HOOK_EVENT_FIELD = "webhookEvent";
    private static final String WEB_HOOK_JIRA_ID_FIELD = "id";
    private static final Logger LOGGER = LoggerFactory.getLogger(WebHookController.class);

    @Autowired
    private Main main;

    @Autowired
    private MetadataRestClient client;

    @Autowired
    private JiraIssueUtils utils;

    @Value("#{'${jira.issue.types:Epic,Feature,Story,Bug,Task}'.split(',')}")
    private Set<String> issueTypes;

    @RequestMapping(value="", method = RequestMethod.POST)
    public void receiveJiraEvent(@RequestBody String eventJson) throws JSONException {

        JSONObject event = new JSONObject(eventJson);

        String eventType = event.getString(WEB_HOOK_EVENT_FIELD);

        LOGGER.info("Event {} received", eventType);

        switch (JiraEvent.fromName(eventType)) {
            case IssueCreated:
            case IssueUpdated:
                processIssueEvent(event.getJSONObject("issue"));
                break;
            case IssueDeleted:
                processIssueDeleteEvent(event.getJSONObject("issue"));
                break;
            case SprintClosed:
            case SprintDeleted:
            case SprintOpened:
            case SprintUpdated:
                processStringEvent(event.getJSONObject("sprint"));
                break;
            case SprintCreated:
                //NOOP;
                break;

            default:
                LOGGER.info("Unhandled event type: {}", eventType);
        }

    }

    private IssueJsonParser issueParser;

    private synchronized IssueJsonParser getParser() {
        if(issueParser == null) {
            Promise<Iterable<Field>> fields = client.getFields();

            JSONObject names = new JSONObject();
            JSONObject schema = new JSONObject();

            try {
                fields.get().forEach((field) -> {
                    try {
                        names.put(field.getId(), field.getName());
                        schema.put(field.getId(), new JSONObject().put("type", field.getFieldType().name()));
                    } catch (JSONException e) {
                        LOGGER.error("Error reading field value from metadata", e);
                    }
                });
            } catch (InterruptedException e) {
                LOGGER.error("Error, interrupted while generating parser", e);
            } catch (ExecutionException e) {
                LOGGER.error("Error while generating parser", e);
            }

            issueParser = new IssueJsonParser(names, schema);
        }
        return issueParser;
    }

    private void processIssueEvent(JSONObject issue) throws JSONException {
        //Ugly hack for Jira not to fail due to missing field
        issue.put("expand","names,schema");
        IssueDTO issuebean = utils.map(getParser().parse(issue));

        if(issueTypes.contains(issuebean.getType())) {
            main.updateIssuesOnDemand(Arrays.asList(issuebean));
        }
    }

    private void processIssueDeleteEvent(JSONObject issue) throws JSONException {
        Long id = issue.getLong(WEB_HOOK_JIRA_ID_FIELD);
        main.deleteIssue(id);
    }

    private void processStringEvent(JSONObject event) throws JSONException{
        String id = event.getString(WEB_HOOK_JIRA_ID_FIELD);
        main.updateSprint(id);
    }

}
