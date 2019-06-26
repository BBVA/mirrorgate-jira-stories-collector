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
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.util.UriComponentsBuilder;

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

    private static final String MIRRORGATE_COLLECTOR_ENDPOINT="/api/collectors/{id}";

    @Autowired
    @Qualifier(Config.MIRRORGATE_REST_TEMPLATE)
    private RestTemplate restTemplate;

    public void update(final Date date) {
        final MultiValueMap<String, String> params = new LinkedMultiValueMap<>();
        params.set("collectorId", appName);

        final UriComponentsBuilder builder = UriComponentsBuilder.fromHttpUrl(mirrorGateUrl + MIRRORGATE_COLLECTOR_ENDPOINT).queryParams(params);

        restTemplate.put(builder.build().toUriString(), date, appName);
    }

    public Date getUpdatedDate() {
        try {
            final MultiValueMap<String, String> params = new LinkedMultiValueMap<>();
            params.set("collectorId", appName);

            final UriComponentsBuilder builder = UriComponentsBuilder.fromHttpUrl(mirrorGateUrl + MIRRORGATE_COLLECTOR_ENDPOINT).queryParams(params);

            return restTemplate.getForObject(builder.build().toUriString(), Date.class, appName);
        }
        catch (final HttpClientErrorException e) {
            if(e.getStatusCode() == HttpStatus.NOT_FOUND) {
                LOGGER.info("Not previous execution date found. Running from the very beginning so this could take a while");
            } else {
                LOGGER.error("Error requesting previous collector status", e);
                throw e;
            }
        }
        return null;
    }

}
