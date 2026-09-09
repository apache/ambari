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

import static org.mockito.Mockito.inOrder;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import java.net.URI;
import java.util.List;
import java.util.Map;

import org.apache.ambari.server.orm.entities.DatasourceEntity;
import org.apache.ambari.server.security.authorization.AuthorizationException;
import org.junit.Assert;
import org.junit.Test;
import org.mockito.InOrder;

import com.fasterxml.jackson.databind.ObjectMapper;

public class PrometheusQueryClientTest {
  @Test
  public void testBuildUriEncodesPromqlParametersOnce() {
    PrometheusQueryClient client = new PrometheusQueryClient(mock(DatasourceService.class));
    String query = "topk(1,hadoop_CorruptBlocks{component=\"HadoopNameNode\", "
        + "name=\"Hadoop:service=NameNode,name=FSNamesystem\"})";

    URI uri = client.buildUri("http://metrics.example.test:8428", "api/v1/query",
        Map.of("query", List.of(query), "time", List.of("1787903460")));

    Assert.assertEquals("query=topk%281%2Chadoop_CorruptBlocks%7Bcomponent%3D%22HadoopNameNode%22%2C+"
        + "name%3D%22Hadoop%3Aservice%3DNameNode%2Cname%3DFSNamesystem%22%7D%29&time=1787903460",
        uri.getRawQuery());
    Assert.assertFalse(uri.toASCIIString().contains("%25"));
  }

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

  @Test
  public void testScopedQueryReplacesEveryClientScopeParameterWithClusterId() throws Exception {
    DatasourceService datasourceService = mock(DatasourceService.class);
    DatasourceEntity datasource = datasource("prometheus");
    when(datasourceService.requireManagedMetricsClusterId(
        datasource, "http://managed.example.test:8428")).thenReturn(42L);
    PrometheusQueryClient client = new PrometheusQueryClient(datasourceService);

    Map<String, List<String>> scoped = client.applyMetricsScope(datasource,
        "http://managed.example.test:8428", "/api/v1/query", "GET", Map.of(
            "query", List.of("up"),
            "extra_label", List.of("ambari_cluster_id=7"),
            "EXTRA_FILTERS", List.of("{cluster=~\".*\"}"),
            "extra_filters[]", List.of("{ambari_cluster_id!=\"42\"}")));

    Assert.assertEquals(List.of("up"), scoped.get("query"));
    Assert.assertEquals(List.of("ambari_cluster_id=42"), scoped.get("extra_label"));
    Assert.assertFalse(scoped.containsKey("EXTRA_FILTERS"));
    Assert.assertFalse(scoped.containsKey("extra_filters[]"));
  }

  @Test
  public void testScopedMetadataAndPostQueriesAreUnsupported() throws Exception {
    DatasourceService datasourceService = mock(DatasourceService.class);
    PrometheusQueryClient client = new PrometheusQueryClient(datasourceService);
    DatasourceEntity datasource = datasource("prometheus");

    assertUnsupported(() -> client.applyMetricsScope(datasource, "http://managed.example.test:8428",
        "api/v1/labels", "GET", Map.of()));
    assertUnsupported(() -> client.applyMetricsScope(datasource, "http://managed.example.test:8428",
        "api/v1/query", "POST", Map.of()));
  }

  @Test
  public void testGenericProxyAuthorizesDatasourceBeforeGlobalAccess() throws Exception {
    DatasourceService datasourceService = mock(DatasourceService.class);
    DatasourceEntity datasource = datasource("prometheus");
    when(datasourceService.requireQueryable(7L)).thenReturn(datasource);
    org.mockito.Mockito.doThrow(new AuthorizationException("global access required"))
        .when(datasourceService).verifyGlobalMetricsAccess();
    PrometheusQueryClient client = new PrometheusQueryClient(datasourceService);

    try {
      client.proxy(7L, "api/v1/query", Map.of(), "GET", null, null);
      Assert.fail("Expected generic proxy access to require global authority");
    } catch (AuthorizationException expected) {
      Assert.assertEquals("global access required", expected.getMessage());
    }

    InOrder authorizationOrder = inOrder(datasourceService);
    authorizationOrder.verify(datasourceService).requireQueryable(7L);
    authorizationOrder.verify(datasourceService).verifyGlobalMetricsAccess();
    verify(datasourceService, org.mockito.Mockito.never()).resolveHttp(datasource);
  }

  private void assertUnsupported(CheckedAction action) throws Exception {
    try {
      action.run();
      Assert.fail("Expected cluster-scoped metrics operation to be unsupported");
    } catch (MetricsScopeException expected) {
      Assert.assertTrue(expected.getMessage().contains("cluster-scoped"));
    }
  }

  @FunctionalInterface
  private interface CheckedAction {
    void run() throws Exception;
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
