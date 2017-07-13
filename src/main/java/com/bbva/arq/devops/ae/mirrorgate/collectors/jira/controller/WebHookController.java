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

/**
 * Created by alfonso on 13/07/17.
 */

@RestController
@RequestMapping("/webhook")
public class WebHookController {

    private static final String WEB_HOOK_EVENT_FIELD = "webhookEvent";
    private static final String WEB_HOOK_JIRA_ID_FIELD = "/issue/id";
    private static final String JIRA_ISSUE_CREATED = "jira:issue_created";
    private static final String JIRA_ISSUE_UPDATED = "jira:issue_updated";
    private static final String JIRA_ISSUE_DELETED = "jira:issue_deleted";
    private static final Logger LOGGER = LoggerFactory.getLogger(WebHookController.class);

    @Autowired
    private Main main;

    private void processIssueEvent(JsonNode event) {
        Long id = event.at(WEB_HOOK_JIRA_ID_FIELD).asLong();
        main.updateIssuesOnDemand(Arrays.asList(id));
    }

    @RequestMapping(value="", method = RequestMethod.POST)
    public void receiveJiraEvent(@RequestBody JsonNode event) {

        String eventType = event.get(WEB_HOOK_EVENT_FIELD).asText();
        switch (eventType) {
            case JIRA_ISSUE_CREATED:
            case JIRA_ISSUE_UPDATED:
            case JIRA_ISSUE_DELETED:
                processIssueEvent(event);
                break;
            default:
                LOGGER.info("Unhandled event type: {}", eventType);
        }

    }

}
