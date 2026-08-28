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

import org.apache.ambari.server.orm.entities.DatasourceEntity;
import org.junit.Assert;
import org.junit.Test;

import com.fasterxml.jackson.databind.JsonNode;

public class DatasourceResponseMapperTest {
  private final DatasourceResponseMapper mapper = new DatasourceResponseMapper();

  @Test
  public void testResponseDoesNotExposeAuthenticationJson() {
    DatasourceEntity entity = datasource();
    entity.setAuth("{\"username\":\"operator\",\"password\":\"super-secret\"}");

    DatasourceResponse response = mapper.toResponse(entity);

    Assert.assertTrue(response.isAuthConfigured());
    Assert.assertFalse(hasMethod(response, "getAuth"));
  }

  @Test
  public void testUnknownSettingsSurviveWhileSecretsAreRedacted() {
    DatasourceEntity entity = datasource();
    entity.setSettings("{\"future_plugin_option\":{\"enabled\":true},\"api-token\":\"secret\"}");
    entity.setHttp("{\"url\":\"https://user:pass@example.test/api?token=secret&tenant=west\","
        + "\"headers\":{\"X-Scope\":\"primary\",\"X-API-Key\":\"secret\"}}");

    DatasourceResponse response = mapper.toResponse(entity);

    Assert.assertTrue(response.getSettings().path("future_plugin_option").path("enabled").asBoolean());
    Assert.assertEquals("[redacted]", response.getSettings().path("api-token").asText());
    Assert.assertEquals("primary", response.getHttp().path("headers").path("X-Scope").asText());
    Assert.assertEquals("[redacted]", response.getHttp().path("headers").path("X-API-Key").asText());
    String safeUrl = response.getHttp().path("url").asText();
    Assert.assertFalse(safeUrl.contains("user:pass"));
    Assert.assertFalse(safeUrl.contains("token=secret"));
    Assert.assertTrue(safeUrl.contains("tenant=west"));
  }

  @Test
  public void testMalformedJsonReturnsSafeEmptyObject() {
    JsonNode result = mapper.parseAndRedact("not-json-with-a-password");

    Assert.assertTrue(result.isObject());
    Assert.assertTrue(result.isEmpty());
  }

  private DatasourceEntity datasource() {
    DatasourceEntity entity = new DatasourceEntity();
    entity.setId(17L);
    entity.setName("prometheus-west");
    entity.setClusterName("west");
    entity.setPluginType("prometheus");
    entity.setPluginTypeName("Prometheus");
    entity.setCategory("prometheus");
    entity.setSettings("{}");
    entity.setHttp("{}");
    entity.setAuth("{}");
    entity.setCreatedAt(1L);
    entity.setUpdatedAt(2L);
    return entity;
  }

  private boolean hasMethod(Object object, String methodName) {
    try {
      object.getClass().getMethod(methodName);
      return true;
    } catch (NoSuchMethodException e) {
      return false;
    }
  }
}
