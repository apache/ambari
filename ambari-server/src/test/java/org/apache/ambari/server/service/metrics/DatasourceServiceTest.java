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
import static org.mockito.Mockito.when;

import org.apache.ambari.server.api.services.metrics.DatasourceRequest;
import org.apache.ambari.server.api.services.metrics.DatasourceResponseMapper;
import org.apache.ambari.server.orm.dao.BoardPayloadDAO;
import org.apache.ambari.server.orm.dao.DatasourceDAO;
import org.apache.ambari.server.orm.entities.DatasourceEntity;
import org.apache.ambari.server.security.authorization.AuthorizationHelper;
import org.apache.ambari.server.security.credential.Credential;
import org.apache.ambari.server.security.credential.PrincipalKeyCredential;
import org.apache.ambari.server.security.encryption.CredentialStoreService;
import org.apache.ambari.server.security.encryption.CredentialStoreType;
import org.apache.ambari.server.state.Cluster;
import org.apache.ambari.server.state.Clusters;
import org.junit.Assert;
import org.junit.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.MockedStatic;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.inject.Provider;

public class DatasourceServiceTest {
  private final ObjectMapper objectMapper = new ObjectMapper();

  @Test
  public void testMergePreservesUnknownSettingsAndRestoresRedactedSecrets() throws Exception {
    DatasourceService service = service();
    String existing = "{\"future\":{\"mode\":\"keep\",\"nested\":{\"alpha\":1}},"
        + "\"token\":\"original-secret\",\"headers\":["
        + "{\"key\":\"Authorization\",\"value\":\"Bearer original\",\"future\":true},"
        + "{\"key\":\"X-Tenant\",\"value\":\"west\"}]}";
    JsonNode patch = objectMapper.readTree("{\"future\":{\"nested\":{\"beta\":2}},"
        + "\"token\":\"[redacted]\",\"headers\":["
        + "{\"key\":\"Authorization\",\"value\":\"[redacted]\",\"future\":false},"
        + "{\"key\":\"X-New\",\"value\":\"present\"}]}");

    JsonNode merged = service.mergeJson(existing, patch);

    Assert.assertEquals("keep", merged.path("future").path("mode").asText());
    Assert.assertEquals(1, merged.path("future").path("nested").path("alpha").asInt());
    Assert.assertEquals(2, merged.path("future").path("nested").path("beta").asInt());
    Assert.assertEquals("original-secret", merged.path("token").asText());
    Assert.assertEquals("Bearer original", merged.path("headers").get(0).path("value").asText());
    Assert.assertFalse(merged.path("headers").get(0).path("future").asBoolean());
    Assert.assertEquals("present", merged.path("headers").get(1).path("value").asText());
  }

  @Test
  @SuppressWarnings("unchecked")
  public void testCreatePersistsSensitiveConfigurationOnlyInCredentialStore() throws Exception {
    DatasourceDAO datasourceDAO = mock(DatasourceDAO.class);
    BoardPayloadDAO boardPayloadDAO = mock(BoardPayloadDAO.class);
    CredentialStoreService credentialStore = mock(CredentialStoreService.class);
    Provider<Clusters> clustersProvider = mock(Provider.class);
    Clusters clusters = mock(Clusters.class);
    when(clustersProvider.get()).thenReturn(clusters);
    when(clusters.getCluster("west")).thenReturn(mock(Cluster.class));
    when(credentialStore.isInitialized(CredentialStoreType.PERSISTED)).thenReturn(true);
    DatasourceService service = new DatasourceService(datasourceDAO, boardPayloadDAO,
        new DatasourceResponseMapper(), credentialStore, mock(BuiltinDatasourceProvisioner.class), clustersProvider);
    DatasourceRequest request = new DatasourceRequest();
    request.setName("VictoriaMetrics");
    request.setClusterName("west");
    request.setPluginType("prometheus");
    request.setHttp(objectMapper.readTree("{\"url\":\"http://metrics:8428\",\"headers\":["
        + "{\"key\":\"Authorization\",\"value\":\"Bearer header-secret\"}]}"));
    request.setSettings(objectMapper.readTree("{\"tenant_token\":\"settings-secret\",\"mode\":\"single\"}"));
    request.setAuth(objectMapper.readTree(
        "{\"basic_auth_user\":\"reader\",\"basic_auth_password\":\"auth-secret\"}"));

    try (MockedStatic<AuthorizationHelper> authorization = org.mockito.Mockito.mockStatic(AuthorizationHelper.class)) {
      authorization.when(() -> AuthorizationHelper.getAuthenticatedName("")).thenReturn("admin");
      service.create(request);
    }

    ArgumentCaptor<DatasourceEntity> entityCaptor = ArgumentCaptor.forClass(DatasourceEntity.class);
    org.mockito.Mockito.verify(datasourceDAO).create(entityCaptor.capture());
    DatasourceEntity stored = entityCaptor.getValue();
    Assert.assertFalse(stored.getHttp().contains("header-secret"));
    Assert.assertFalse(stored.getSettings().contains("settings-secret"));
    Assert.assertFalse(stored.getAuth().contains("auth-secret"));
    Assert.assertTrue(stored.getAuth().contains("credential_alias"));

    ArgumentCaptor<Credential> credentialCaptor = ArgumentCaptor.forClass(Credential.class);
    org.mockito.Mockito.verify(credentialStore).setCredential(
        org.mockito.ArgumentMatchers.eq("west"),
        org.mockito.ArgumentMatchers.startsWith("metrics.datasource."),
        credentialCaptor.capture(),
        org.mockito.ArgumentMatchers.eq(CredentialStoreType.PERSISTED));
    char[] key = ((PrincipalKeyCredential) credentialCaptor.getValue()).getKey();
    String credentialPayload = new String(key);
    java.util.Arrays.fill(key, '\0');
    Assert.assertTrue(credentialPayload.contains("header-secret"));
    Assert.assertTrue(credentialPayload.contains("settings-secret"));
    Assert.assertTrue(credentialPayload.contains("auth-secret"));
  }

  @SuppressWarnings("unchecked")
  private DatasourceService service() {
    return new DatasourceService(
        mock(DatasourceDAO.class),
        mock(BoardPayloadDAO.class),
        new DatasourceResponseMapper(),
        mock(CredentialStoreService.class),
        mock(BuiltinDatasourceProvisioner.class),
        mock(Provider.class));
  }
}
