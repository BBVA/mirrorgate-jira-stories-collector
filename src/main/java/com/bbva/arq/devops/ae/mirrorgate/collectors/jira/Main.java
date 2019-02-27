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

package com.bbva.arq.devops.ae.mirrorgate.collectors.jira;

import com.bbva.arq.devops.ae.mirrorgate.collectors.jira.api.CollectorService;
import com.bbva.arq.devops.ae.mirrorgate.collectors.jira.api.SprintService;
import com.bbva.arq.devops.ae.mirrorgate.collectors.jira.dto.IssueDTO;
import com.bbva.arq.devops.ae.mirrorgate.collectors.jira.dto.SprintDTO;
import com.bbva.arq.devops.ae.mirrorgate.collectors.jira.service.IssuesService;
import com.bbva.arq.devops.ae.mirrorgate.collectors.jira.support.Pageable;
import java.util.*;
import java.util.stream.Collectors;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

@Component
public class Main implements Runnable {

    private static final Logger LOGGER = LoggerFactory.getLogger(Main.class);

    @Autowired
    private SprintService sprintApi;

    @Autowired
    private CollectorService collectorApi;

    @Autowired
    private IssuesService service;

    public void updateIssuesOnDemand(final List<IssueDTO> issues) {
        iterateAndSave(new Pageable<IssueDTO>() {
            boolean returned = false;

            @Override
            public List<IssueDTO> nextPage() {
                List<IssueDTO> value = returned ? new ArrayList<>() : issues;
                returned = true;
                return value;
            }
        }, false);
    }

    public void deleteIssue(Long id) {
        sprintApi.deleteIssue(id);
    }

    private void iterateAndSave(Pageable<IssueDTO> pagedIssues, boolean updateCollectorsDate) {
        List<IssueDTO> issues;

        while ((issues = pagedIssues.nextPage()).size() > 0) {
            LOGGER.info("-> Saving: {}", issues);
            sprintApi.sendIssues(issues);
            if(updateCollectorsDate) {
                collectorApi.update(issues.get(issues.size() - 1).getUpdatedDate());
            }
        }
    }

    private Pageable<IssueDTO> getIssuesByIdAndDeleteNotPresent(List<Long> ids) {
        final Set<Long> idSet = new HashSet<>(ids);
        final Pageable<IssueDTO> wrapped = service.getById(ids);
        return () -> {
            List<IssueDTO> result = wrapped.nextPage();
            for (IssueDTO issueDTO : result) {
                idSet.remove(issueDTO.getId());
            }
            if(result.size() == 0) {
                LOGGER.info("-> Deleting: {}", idSet);
                idSet.forEach(this::deleteIssue);
            }
            return result;
        };
    }

    private List<SprintDTO> getSprintsThatNeedUpdating() {
        final List<SprintDTO> sprints = sprintApi.getSprintSamples();

        final List<Long> ids = new ArrayList<>();
        final Map<Long, SprintDTO> idToSprint = new HashMap<>(ids.size() * 2);

        sprints.forEach((s) -> {
            for (IssueDTO issue : s.getIssues()) {
                idToSprint.put(issue.getId(), s);
                ids.add(issue.getId());
            }
        });

        Pageable<IssueDTO> samples = getIssuesByIdAndDeleteNotPresent(ids);

        List<SprintDTO> toUpdate = new ArrayList<>();
        List<IssueDTO> issues;
        while ((issues = samples.nextPage()).size() > 0) {
            LOGGER.info("-> Checking {}", issues.get(0));
            issues.forEach((i) -> {
                SprintDTO current = i.getSprint();
                if(current == null) {
                    current = idToSprint.get(i.getId());
                    LOGGER.info("-> New Sprint {} association for issue {}", current.getName(), i.getId());
                    toUpdate.add(current);
                } else if(!current.equals(idToSprint.get(i.getId()))) {
                    LOGGER.info("-> Sprint changed {} for issue {}", current.getName(), i.getId());
                    toUpdate.add(current);
                    toUpdate.add(idToSprint.get(i.getId()));
                }
            });
        }
        LOGGER.info("-> Needs updating: {}", toUpdate);
        return toUpdate;
    }

    public void updateSprint(String id) {
        SprintDTO sprint = sprintApi.getSprint(id);
        if(sprint != null && sprint.getIssues() != null) {
            List<Long> ids = sprint.getIssues().stream().map(IssueDTO::getId).collect(Collectors.toList());
            iterateAndSave(getIssuesByIdAndDeleteNotPresent(ids), false);
        } else {
            LOGGER.warn("-> Could not update the sprint {}", id);
        }
    }

    public synchronized void run() {

        LOGGER.info("Starting");
        iterateAndSave(service.getRecentIssues(), true);

        for(SprintDTO s : getSprintsThatNeedUpdating()) {
            updateSprint(s.getId());
        }

        LOGGER.info("Ending");
    }
}
