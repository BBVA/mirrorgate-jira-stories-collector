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

package com.bbva.arq.devops.ae.mirrorgate.collectors.jira.model;

import java.io.Serializable;
import java.util.Date;
import java.util.List;

/**
 * Created by alfonso on 26/05/17.
 */
public class Issue implements Serializable {

    private Long id;
    private String name;
    private String type;
    private IssueStatus status;
    private Double estimate=0.0;

    private Sprint sprint;
    private Project project;
    private Date updatedDate;

    private List<String> keywords = null;

    public Long getId() {
        return id;
    }

    public Issue setId(Long id) {
        this.id = id;
        return this;
    }

    public String getName() {
        return name;
    }

    public Issue setName(String name) {
        this.name = name;
        return this;
    }

    public String getType() {
        return type;
    }

    public Issue setType(String type) {
        this.type = type;
        return this;
    }

    public IssueStatus getStatus() {
        return status;
    }

    public Issue setStatus(IssueStatus status) {
        this.status = status;
        return this;
    }

    public Double getEstimate() {
        return estimate;
    }

    public Issue setEstimate(Double estimate) {
        this.estimate = estimate;
        return this;
    }

    public List<String> getKeywords() {
        return keywords;
    }

    public Issue setKeywords(List<String> keyword) {
        this.keywords = keyword;
        return this;
    }

    public Sprint getSprint() {
        return sprint;
    }

    public Issue setSprint(Sprint sprint) {
        this.sprint = sprint;
        return this;
    }

    public String toString() {
        return name + " - " + getEstimate();
    }

    public Project getProject() {
        return project;
    }

    public Issue setProject(Project project) {
        this.project = project;
        return this;
    }

    public Date getUpdatedDate() {
        return updatedDate;
    }

    public Issue setUpdatedDate(Date updatedDate) {
        this.updatedDate = updatedDate;
        return this;
    }
}
