package com.bbva.arq.devops.ae.mirrorgate.collectors.jira.service;

import com.atlassian.jira.rest.client.api.MetadataRestClient;
import com.atlassian.util.concurrent.Promise;
import com.bbva.arq.devops.ae.mirrorgate.collectors.jira.exception.IssueMapException;
import com.bbva.arq.devops.ae.mirrorgate.core.utils.IssueType;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ExecutionException;
import javax.annotation.PostConstruct;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

@Component
public class JiraIssueTypeMapServiceImpl implements IssueTypeMapService {

    private static final Logger LOGGER = LoggerFactory.getLogger(JiraStatusMapServiceImpl.class);

    //TODO: Allow configurable Mappings
    private static final Map<String, IssueType> ISSUE_TYPE_DEFAULTS = new HashMap<String, IssueType>(){{
        put("Bug", IssueType.BUG);
        put("Epic", IssueType.EPIC);
        put("Feature", IssueType.FEATURE);
        put("Story", IssueType.STORY);
        put("Task", IssueType.TASK);
    }};

    private Map<Long, String> issueTypeCache = new HashMap<>();

    private MetadataRestClient metadataRestClient;

    @Autowired
    public JiraIssueTypeMapServiceImpl(MetadataRestClient metadataRestClient) {

        this.metadataRestClient = metadataRestClient;
    }


    @Override
    public String getIssueTypeFor(Long id) {

        return ISSUE_TYPE_DEFAULTS.get(issueTypeCache.get(id)).getName();
    }

    @PostConstruct
    private void createJiraIssueTypeMap(){
        Promise<Iterable<com.atlassian.jira.rest.client.api.domain.IssueType>> issueTypesPromise =
            metadataRestClient.getIssueTypes();

        try {
            Iterable<com.atlassian.jira.rest.client.api.domain.IssueType> issueTypes = issueTypesPromise.get();
            issueTypes.forEach(
               issueType -> issueTypeCache.put(issueType.getId(), issueType.getName())
            );
        } catch (InterruptedException e) {
            LOGGER.error("Interrupted Exception while trying to recover issue types");
            throw new IssueMapException(e);
        } catch (ExecutionException e) {
            LOGGER.error("Execution Exception while trying to recover issue types");
            throw new IssueMapException(e);
        }
    }

}
