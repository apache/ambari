/*
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
package org.apache.ambari.server.api.services.metrics;

import jakarta.ws.rs.core.Response;

import org.apache.ambari.server.service.metrics.MetricsScopeException;
import org.junit.Assert;
import org.junit.Test;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

public class PrometheusApiServiceTest {
  private static final ObjectMapper OBJECT_MAPPER = new ObjectMapper();

  @Test
  public void testUnsupportedScopedOperationUsesStableWireError() throws Exception {
    Response response = new PrometheusApiService().error(
        new MetricsScopeException("This metrics operation is unavailable for cluster-scoped access"),
        "Prometheus request failed");

    Assert.assertEquals(422, response.getStatus());
    JsonNode body = OBJECT_MAPPER.readTree((String) response.getEntity());
    Assert.assertEquals("METRICS_SCOPE_UNSUPPORTED", body.path("code").asText());
    Assert.assertEquals("This metrics operation is unavailable for cluster-scoped access",
        body.path("message").asText());
    Assert.assertEquals(2, body.size());
  }
}
