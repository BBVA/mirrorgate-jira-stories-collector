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
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

@Component
public class Main implements Runnable {

    private static final Logger LOG = LoggerFactory.getLogger(Main.class);

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
                final List<IssueDTO> value = returned ? new ArrayList<>() : issues;
                returned = true;
                return value;
            }
        }, false);
    }

    public void deleteIssue(final Long id) {
        LOG.info("-> Deleting: {}", id);
        sprintApi.deleteIssue(id);
    }

    private void iterateAndSave(final Pageable<IssueDTO> pagedIssues, final boolean updateCollectorsDate) {
        List<IssueDTO> issues;

        while ((issues = pagedIssues.nextPage()).size() > 0) {
            LOG.info("-> Saving: {}", issues);
            sprintApi.sendIssues(issues);
            if(updateCollectorsDate) {
                collectorApi.update(issues.get(issues.size() - 1).getUpdatedDate());
            }
        }
    }

    private Pageable<IssueDTO> getIssuesByIdAndDeleteNotPresent(final List<Long> ids) {
        final Set<Long> idSet = new HashSet<>(ids);
        final Pageable<IssueDTO> wrapped = service.getById(ids);
        return () -> {
            List<IssueDTO> result = wrapped.nextPage();
            for (IssueDTO issueDTO : result) {
                idSet.remove(issueDTO.getId());
            }
            if(result.size() == 0) {
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

        final Pageable<IssueDTO> samples = getIssuesByIdAndDeleteNotPresent(ids);

        final List<SprintDTO> toUpdate = new ArrayList<>();
        List<IssueDTO> issues;
        while ((issues = samples.nextPage()).size() > 0) {
            LOG.info("-> Checking {}", issues.get(0));
            issues.forEach((i) -> {
                SprintDTO current = i.getSprint();
                if(current == null) {
                    current = idToSprint.get(i.getId());
                    LOG.info("-> New Sprint {} association for issue {}", current.getName(), i.getId());
                    toUpdate.add(current);
                } else if(!current.getId().equals(idToSprint.get(i.getId()).getId())) {
                    LOG.info("-> Sprint changed {} for issue {}", current.getName(), i.getId());
                    toUpdate.add(current);
                    toUpdate.add(idToSprint.get(i.getId()));
                }
            });
        }

        final List<String> springIdsToUpdate = toUpdate.stream().map(SprintDTO::getId).collect(Collectors.toList());
        LOG.info("-> Needs updating: {}", springIdsToUpdate);
        return toUpdate;
    }

    public void updateSprint(final String id) {
        final SprintDTO sprint = sprintApi.getSprint(id);
        if(sprint != null && sprint.getIssues() != null) {
            final List<Long> ids = sprint.getIssues().stream().map(IssueDTO::getId).collect(Collectors.toList());
            iterateAndSave(getIssuesByIdAndDeleteNotPresent(ids), false);
        } else {
            LOG.warn("-> Could not update the sprint {}", id);
        }
    }

    public synchronized void run() {

        LOG.info("Starting");
        iterateAndSave(service.getRecentIssues(), true);

        for (final SprintDTO s : getSprintsThatNeedUpdating()) {
            updateSprint(s.getId());
        }

        LOG.info("Ending");
    }
}
