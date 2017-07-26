package com.bbva.arq.devops.ae.mirrorgate.collectors.jira.exception;

public class IssueMapException extends RuntimeException {

    public IssueMapException(Exception e){
        super(" Exception while mapping Jira defined Issue Types ", e);
    }

}
