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

package com.bbva.arq.devops.ae.mirrorgate.collectors.jira.support;

/**
 * Created by alfonso on 26/05/17.
 */
public class Counter {

    private int value;
    private int increment;

    public Counter() {
        this(0,1);
    }

    public Counter(int increment) {
        this(0,increment);
    }

    public Counter(int value, int increment) {
        this.value = value;
        this.increment = increment;
    }

    public int get() {
        return value;
    }

    public int inc() {
        int val = value;
        value += increment;
        return val;
    }

}
