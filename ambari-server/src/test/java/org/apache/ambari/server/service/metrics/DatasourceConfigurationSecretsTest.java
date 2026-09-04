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

import org.junit.Assert;
import org.junit.Test;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

public class DatasourceConfigurationSecretsTest {
  private final ObjectMapper objectMapper = new ObjectMapper();
  private final DatasourceConfigurationSecrets secrets = new DatasourceConfigurationSecrets(objectMapper);

  @Test
  public void testSeparatesAndRestoresNestedSecretsAndSensitiveHeaders() throws Exception {
    JsonNode original = objectMapper.readTree("{"
        + "\"url\":\"https://metrics.example.test\","
        + "\"headers\":["
        + "{\"key\":\"Authorization\",\"value\":\"Bearer top-secret\",\"enabled\":true},"
        + "{\"key\":\"X-Tenant\",\"value\":\"west\"}],"
        + "\"options\":{\"client_secret\":\"hidden\",\"retries\":3},"
        + "\"replicas\":[{\"token\":\"replica-secret\"},{\"region\":\"east\"}]}" );

    DatasourceConfigurationSecrets.Split split = secrets.split(original);

    String databaseValue = split.sanitized().toString();
    Assert.assertFalse(databaseValue.contains("top-secret"));
    Assert.assertFalse(databaseValue.contains("hidden"));
    Assert.assertFalse(databaseValue.contains("replica-secret"));
    Assert.assertEquals("west", split.sanitized().path("headers").get(1).path("value").asText());
    Assert.assertEquals(original, secrets.restore(split.sanitized(), split.secrets()));
  }

  @Test
  public void testConfigurationWithoutSecretsDoesNotProduceCredentialPayload() throws Exception {
    JsonNode original = objectMapper.readTree(
        "{\"url\":\"http://metrics.example.test:8428\",\"timeout\":10000}");

    DatasourceConfigurationSecrets.Split split = secrets.split(original);

    Assert.assertFalse(secrets.hasSecrets(split.secrets()));
    Assert.assertEquals(original, split.sanitized());
  }
}
