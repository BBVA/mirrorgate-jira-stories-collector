package com.bbva.arq.devops.ae.mirrorgate.collectors.jira.service;

import com.atlassian.jira.rest.client.api.MetadataRestClient;
import com.atlassian.util.concurrent.Promise;
import com.bbva.arq.devops.ae.mirrorgate.collectors.jira.config.Config;
import com.bbva.arq.devops.ae.mirrorgate.collectors.jira.exception.IssueMapException;
import com.bbva.arq.devops.ae.mirrorgate.core.utils.IssueType;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ExecutionException;
import javax.annotation.PostConstruct;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;

@Component
@Profile("!test")
public class JiraIssueTypeMapServiceImpl implements IssueTypeMapService {

    private static final Logger LOGGER = LoggerFactory.getLogger(JiraStatusMapServiceImpl.class);

    private Map<Long, String> issueTypeCache = new HashMap<>();

    private Map<String, IssueType> issueTypeMapping;

    private MetadataRestClient metadataRestClient;

    @Autowired
    public JiraIssueTypeMapServiceImpl(
            @Qualifier(Config.JIRA_TYPES_MAPPING)
            Map<String, IssueType> issueTypeMapping,
            MetadataRestClient metadataRestClient
    ) {
        this.issueTypeMapping = issueTypeMapping;
        this.metadataRestClient = metadataRestClient;
    }

    @Override
    public String getIssueTypeFor(Long id) {
        String pre = issueTypeCache.get(id);
        IssueType target = pre == null ? null : issueTypeMapping.get(pre);
        return target == null ? null : target.getName();
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
