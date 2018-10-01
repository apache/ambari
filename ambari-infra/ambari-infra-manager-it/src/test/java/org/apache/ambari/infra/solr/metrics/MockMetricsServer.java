package org.apache.ambari.infra.solr.metrics;

/*
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */

import static java.util.Collections.singletonList;
import static spark.Spark.get;
import static spark.Spark.port;
import static spark.Spark.post;

import java.util.Set;
import java.util.concurrent.ConcurrentSkipListSet;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.gson.Gson;

import spark.Request;
import spark.Response;
import spark.servlet.SparkApplication;

public class MockMetricsServer implements SparkApplication {
  private static final Logger LOG = LoggerFactory.getLogger(MockMetricsServer.class);
  private static final String HOST_NAME = "metrics_collector";

  private Set<String> expectedMetrics;

  @Override
  public void init() {
    port(6188);
    get("/ping", (req, resp) -> "pong");
    get("/ws/v1/timeline/metrics/livenodes", this::queryState);
    post("/ws/v1/timeline/metrics", this::logBody);
  }

  private Object queryState(Request request, Response response) {
    LOG.info("Sending hostname {}", HOST_NAME);
    response.type("application/json");
    return new Gson().toJson(singletonList(HOST_NAME));
  }

  private Object logBody(Request req, Response resp) {
    String body = req.body();
    LOG.info("Incoming metrics {}", body);

    expectedMetrics.removeIf(body::contains);

    return "OK";
  }

  public void addExpectedMetrics(Set<String> expectedMetrics) {
    this.expectedMetrics = new ConcurrentSkipListSet<>(expectedMetrics);
  }

  public Set<String> getNotReceivedMetrics() {
    return expectedMetrics;
  }
}
