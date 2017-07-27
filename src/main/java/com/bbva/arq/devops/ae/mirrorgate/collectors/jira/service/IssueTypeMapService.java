package com.bbva.arq.devops.ae.mirrorgate.collectors.jira.service;

import com.atlassian.jira.rest.client.api.domain.IssueType;

public interface IssueTypeMapService {

    String getIssueTypeFor(IssueType id);

}
