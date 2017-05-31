/**
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.apache.ambari.metrics.alertservice.methods.ema;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.gson.Gson;
import org.apache.ambari.metrics.alertservice.common.TimelineMetric;

import java.io.*;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.HashMap;
import java.util.Map;
import java.util.TreeMap;

public class TestEmaModel {

    public static void main(String[] args) throws IOException {

        long now = System.currentTimeMillis();
        TimelineMetric metric1 = new TimelineMetric();
        metric1.setMetricName("dummy_metric");
        metric1.setHostName("dummy_host");
        metric1.setTimestamp(now);
        metric1.setStartTime(now - 1000);
        metric1.setAppId("HOST");
        metric1.setType("Integer");

        TreeMap<Long, Double> metricValues = new TreeMap<Long, Double>();

        for (int i = 0; i<20;i++) {
            double metric = 9 + Math.random();
            metricValues.put(now - i*100, metric);
        }
        metric1.setMetricValues(metricValues);

        EmaModel emaModel = new EmaModel();

        emaModel.train(metric1, 0.8, 3);
    }

    /*
     {{
            put(now - 100, 1.20);
            put(now - 200, 1.25);
            put(now - 300, 1.30);
            put(now - 400, 4.50);
            put(now - 500, 1.35);
            put(now - 400, 5.50);
        }}
     */
}
