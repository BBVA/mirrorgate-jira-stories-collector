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

import com.bbva.arq.devops.ae.mirrorgate.collectors.jira.Main;
import com.fasterxml.jackson.databind.JsonNode;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RestController;

import java.util.Arrays;
import java.util.Optional;

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
        Unknown ("?"),
        ;


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
    private static final String WEB_HOOK_JIRA_ID_FIELD = "/issue/id";
    private static final String WEB_HOOK_JIRA_SPRINT_ID_FIELD = "/sprint/id";
    private static final Logger LOGGER = LoggerFactory.getLogger(WebHookController.class);

    @Autowired
    private Main main;

    private void processIssueEvent(JsonNode event) {
        Long id = event.at(WEB_HOOK_JIRA_ID_FIELD).asLong();
        main.updateIssuesOnDemand(Arrays.asList(id));
    }

    private void processStringEvent(JsonNode event) {
        String id = event.at(WEB_HOOK_JIRA_SPRINT_ID_FIELD).asText();
        main.updateSprint(id);
    }

    @RequestMapping(value="", method = RequestMethod.POST)
    public void receiveJiraEvent(@RequestBody JsonNode event) {

        String eventType = event.get(WEB_HOOK_EVENT_FIELD).asText();
        switch (JiraEvent.fromName(eventType)) {
            case IssueCreated:
            case IssueUpdated:
            case IssueDeleted:
                processIssueEvent(event);
                break;

            case SprintClosed:
            case SprintDeleted:
            case SprintOpened:
            case SprintUpdated:
                processStringEvent(event);
                break;
            case SprintCreated:
                //NOOP;
                break;

            default:
                LOGGER.info("Unhandled event type: {}", eventType);
        }

    }

}
