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

import java.util.Collection;
import java.util.Collections;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

import org.apache.ambari.server.events.HostsRemovedEvent;
import org.apache.ambari.server.events.publishers.AmbariEventPublisher;

import com.google.common.eventbus.AllowConcurrentEvents;
import com.google.common.eventbus.Subscribe;
import com.google.inject.Inject;
import com.google.inject.Singleton;

@Singleton
public class AgentEncryptionCapabilities {
  public static final String AES256_GCM = "aes256_gcm";

  private final ConcurrentHashMap<Long, Set<String>> capabilitiesByHost = new ConcurrentHashMap<>();

  @Inject
  public AgentEncryptionCapabilities(AmbariEventPublisher eventPublisher) {
    eventPublisher.register(this);
  }

  public boolean update(Long hostId, Collection<String> capabilities) {
    Set<String> normalized = capabilities != null && capabilities.contains(AES256_GCM)
        ? Collections.singleton(AES256_GCM)
        : Collections.emptySet();
    Set<String> previous = capabilitiesByHost.put(hostId, normalized);
    return !normalized.equals(previous);
  }

  public boolean supportsAesGcm(Long hostId) {
    return capabilitiesByHost.getOrDefault(hostId, Collections.emptySet()).contains(AES256_GCM);
  }

  public void remove(Long hostId) {
    capabilitiesByHost.remove(hostId);
  }

  @Subscribe
  @AllowConcurrentEvents
  public void onHostsRemoved(HostsRemovedEvent event) {
    event.getHostIds().forEach(this::remove);
  }
}
