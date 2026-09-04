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

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static org.mockito.Mockito.mock;

import java.util.Collections;
import java.util.List;
import java.util.Set;

import org.apache.ambari.server.events.HostsRemovedEvent;
import org.apache.ambari.server.events.publishers.AmbariEventPublisher;
import org.junit.Test;

public class AgentEncryptionCapabilitiesTest {
  @Test
  public void tracksUpgradeDowngradeAndRemovalPerHost() {
    AgentEncryptionCapabilities capabilities = capabilities();

    assertTrue(capabilities.update(1L, Collections.emptyList()));
    assertFalse(capabilities.supportsAesGcm(1L));
    assertFalse(capabilities.update(1L, Collections.emptyList()));

    assertTrue(capabilities.update(1L, List.of(AgentEncryptionCapabilities.AES256_GCM)));
    assertTrue(capabilities.supportsAesGcm(1L));
    assertFalse(capabilities.supportsAesGcm(2L));

    assertTrue(capabilities.update(1L, null));
    assertFalse(capabilities.supportsAesGcm(1L));

    capabilities.update(1L, List.of(AgentEncryptionCapabilities.AES256_GCM));
    capabilities.remove(1L);
    assertFalse(capabilities.supportsAesGcm(1L));
  }

  @Test
  public void ignoresUnknownCapabilitiesAndRemovesDeletedHosts() {
    AgentEncryptionCapabilities capabilities = capabilities();

    capabilities.update(1L, List.of("unknown", AgentEncryptionCapabilities.AES256_GCM));
    capabilities.update(2L, Collections.singleton(AgentEncryptionCapabilities.AES256_GCM));
    capabilities.onHostsRemoved(new HostsRemovedEvent(Collections.emptySet(), Set.of(1L)));

    assertFalse(capabilities.supportsAesGcm(1L));
    assertTrue(capabilities.supportsAesGcm(2L));
  }

  private AgentEncryptionCapabilities capabilities() {
    return new AgentEncryptionCapabilities(mock(AmbariEventPublisher.class));
  }
}
