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

import com.amazonaws.services.lambda.runtime.Context;
import com.amazonaws.services.lambda.runtime.RequestHandler;
import org.springframework.boot.SpringApplication;
import org.springframework.context.ConfigurableApplicationContext;

public class LambdaHandler implements RequestHandler<Object, String> {

    private static ConfigurableApplicationContext ctx;

    private static synchronized ConfigurableApplicationContext getContext() {
        if(ctx == null) {
            ctx = SpringApplication.run(MirrorgateJiraStoriesCollectorApplication.class);
        }
        return ctx;
    }

    @Override
    public String handleRequest(Object input, Context context) {
        getContext().getBean(Main.class).run();
        return null;
    }
}
