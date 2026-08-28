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
package org.apache.ambari.server.service.metrics;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import java.util.Map;

import org.apache.ambari.server.orm.entities.DatasourceEntity;
import org.junit.Assert;
import org.junit.Test;

import com.fasterxml.jackson.databind.ObjectMapper;

public class PrometheusQueryClientTest {
  @Test
  public void testRejectsProxyPathTraversal() {
    DatasourceService datasourceService = mock(DatasourceService.class);
    PrometheusQueryClient client = new PrometheusQueryClient(datasourceService);

    try {
      client.buildUri("https://metrics.example.test/prometheus", "../admin", Map.of());
      Assert.fail("Expected traversal path to be rejected");
    } catch (IllegalArgumentException expected) {
      Assert.assertTrue(expected.getMessage().contains("invalid"));
    }

    verifyNoInteractions(datasourceService);
  }

  @Test
  public void testRejectsOversizedProxyRequestBeforeSending() throws Exception {
    DatasourceService datasourceService = mock(DatasourceService.class);
    DatasourceEntity datasource = datasource("prometheus");
    when(datasourceService.requireQueryable(7L)).thenReturn(datasource);
    when(datasourceService.resolveAuth(datasource)).thenReturn(new ObjectMapper().createObjectNode());
    PrometheusQueryClient client = new PrometheusQueryClient(datasourceService);

    try {
      client.proxy(7L, "api/v1/query", Map.of(), "POST", "x".repeat(8 * 1024 * 1024 + 1), "text/plain");
      Assert.fail("Expected oversized proxy request to be rejected");
    } catch (IllegalArgumentException expected) {
      Assert.assertTrue(expected.getMessage().contains("8 MiB"));
    }
  }

  @Test
  public void testPrometheusEndpointRejectsOtherPlugins() throws Exception {
    DatasourceService datasourceService = mock(DatasourceService.class);
    when(datasourceService.requireQueryable(9L)).thenReturn(datasource("elasticsearch"));
    PrometheusQueryClient client = new PrometheusQueryClient(datasourceService);

    try {
      client.get(9L, "api/v1/query", Map.of());
      Assert.fail("Expected non-Prometheus plugin to be rejected");
    } catch (IllegalArgumentException expected) {
      Assert.assertTrue(expected.getMessage().contains("not a Prometheus datasource"));
    }
  }

  private DatasourceEntity datasource(String pluginType) {
    DatasourceEntity datasource = new DatasourceEntity();
    datasource.setId(7L);
    datasource.setPluginType(pluginType);
    datasource.setCategory(pluginType);
    datasource.setHttp("{\"url\":\"https://metrics.example.test\"}");
    datasource.setAuth("{}");
    datasource.setStatus(DatasourceEntity.STATUS_ENABLED);
    return datasource;
  }
}
