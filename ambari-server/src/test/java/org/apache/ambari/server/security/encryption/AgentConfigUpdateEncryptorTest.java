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
package org.apache.ambari.server.security.encryption;

import static org.junit.Assert.assertEquals;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.util.Collections;
import java.util.EnumMap;
import java.util.Map;
import java.util.Set;
import java.util.SortedMap;
import java.util.TreeMap;

import org.apache.ambari.server.agent.stomp.dto.ClusterConfigs;
import org.apache.ambari.server.events.AgentConfigsUpdateEvent;
import org.apache.ambari.server.events.publishers.AmbariEventPublisher;
import org.apache.ambari.server.security.credential.Credential;
import org.apache.ambari.server.state.Cluster;
import org.apache.ambari.server.state.Clusters;
import org.apache.ambari.server.state.PropertyInfo;
import org.apache.ambari.server.state.StackId;
import org.apache.ambari.server.utils.TextEncoding;
import org.junit.Test;

import com.google.inject.Provider;

public class AgentConfigUpdateEncryptorTest {
  @Test
  public void selectsEncryptionEnvelopeFromRegisteredHostCapability() throws Exception {
    EncryptionService encryptionService = mock(EncryptionService.class);
    when(encryptionService.encrypt(anyString(), anyString(), org.mockito.ArgumentMatchers.eq(TextEncoding.BIN_HEX)))
        .thenReturn("v1-ciphertext");
    when(encryptionService.encryptGcm(anyString(), anyString(), org.mockito.ArgumentMatchers.eq(TextEncoding.BIN_HEX)))
        .thenReturn("v2-ciphertext");

    Credential credential = mock(Credential.class);
    when(credential.toValue()).thenReturn("agent-key".toCharArray());
    CredentialStoreService credentialStore = mock(CredentialStoreService.class);
    when(credentialStore.containsCredential(isNull(), anyString())).thenReturn(true);
    when(credentialStore.getCredential(isNull(), anyString())).thenReturn(credential);

    StackId stackId = new StackId("BIGTOP", "3.2.0");
    Cluster cluster = mock(Cluster.class);
    when(cluster.getClusterId()).thenReturn(1L);
    when(cluster.getCurrentStackVersion()).thenReturn(stackId);
    when(cluster.getDesiredStackVersion()).thenReturn(stackId);
    Map<PropertyInfo.PropertyType, Set<String>> propertyTypes =
        new EnumMap<>(PropertyInfo.PropertyType.class);
    propertyTypes.put(PropertyInfo.PropertyType.PASSWORD, Collections.singleton("password"));
    when(cluster.getConfigPropertiesTypes("core-site", stackId)).thenReturn(propertyTypes);

    Clusters clusters = mock(Clusters.class);
    when(clusters.getCluster(1L)).thenReturn(cluster);
    @SuppressWarnings("unchecked")
    Provider<Clusters> clustersProvider = mock(Provider.class);
    when(clustersProvider.get()).thenReturn(clusters);

    AgentEncryptionCapabilities capabilities = new AgentEncryptionCapabilities(
        mock(AmbariEventPublisher.class));
    capabilities.update(10L, Collections.singleton(AgentEncryptionCapabilities.AES256_GCM));
    AgentConfigUpdateEncryptor encryptor = new AgentConfigUpdateEncryptor(
        encryptionService, credentialStore, clustersProvider, capabilities);

    AgentConfigsUpdateEvent modernEvent = event(10L);
    encryptor.encryptSensitiveData(modernEvent);
    assertEquals("${enc=aes256_gcm_hex, value=v2-ciphertext}", password(modernEvent));

    AgentConfigsUpdateEvent legacyEvent = event(11L);
    encryptor.encryptSensitiveData(legacyEvent);
    assertEquals("${enc=aes256_hex, value=v1-ciphertext}", password(legacyEvent));
  }

  private AgentConfigsUpdateEvent event(Long hostId) {
    SortedMap<String, String> properties = new TreeMap<>();
    properties.put("password", "secret");
    SortedMap<String, SortedMap<String, String>> configurations = new TreeMap<>();
    configurations.put("core-site", properties);
    SortedMap<String, ClusterConfigs> clusters = new TreeMap<>();
    clusters.put("1", new ClusterConfigs(configurations, new TreeMap<>()));
    return new AgentConfigsUpdateEvent(hostId, clusters);
  }

  private String password(AgentConfigsUpdateEvent event) {
    return event.getClustersConfigs().get("1").getConfigurations()
        .get("core-site").get("password");
  }
}
