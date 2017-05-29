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

package com.bbva.arq.devops.ae.mirrorgate.collectors.jira.api;

import com.bbva.arq.devops.ae.mirrorgate.collectors.jira.config.Config;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.RestTemplate;

import java.util.Date;

/**
 * Created by alfonso on 26/05/17.
 */
@Component
public class CollectorService {

    private static final Logger LOGGER = LoggerFactory.getLogger(CollectorService.class);

    @Value("${mirrorgate.url}")
    private String mirrorGateUrl;

    @Value("${spring.application.name}")
    private String appName;

    private static final String MIRROR_GATE_COLLECTOR_ENDPOINT="/api/collectors/{id}";

    @Autowired
    @Qualifier(Config.MIRRORGATE_REST_TEMPLATE)
    RestTemplate restTemplate;

    public void update(Date date) {
        restTemplate.put(mirrorGateUrl + MIRROR_GATE_COLLECTOR_ENDPOINT, date, appName);
    }

    public Date getUpdatedDate() {
        try {
            return restTemplate.getForObject(mirrorGateUrl + MIRROR_GATE_COLLECTOR_ENDPOINT, Date.class, appName);
        }
        catch (final HttpClientErrorException e) {
            if(e.getStatusCode() == HttpStatus.NOT_FOUND) {
                LOGGER.info("Not previous execution date found. Running from the very beginning so this could take a while");
                return null;
            } else {
                LOGGER.error("Error requesting previous collector status", e);
                throw e;
            }
        }
    }

}
