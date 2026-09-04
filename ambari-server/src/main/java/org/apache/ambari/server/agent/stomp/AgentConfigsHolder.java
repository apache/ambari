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
package org.apache.ambari.server.agent.stomp;

import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutionException;
import java.util.stream.Collectors;

import org.apache.ambari.server.AmbariException;
import org.apache.ambari.server.events.AgentConfigsUpdateEvent;
import org.apache.ambari.server.events.publishers.AmbariEventPublisher;
import org.apache.ambari.server.security.encryption.Encryptor;
import org.apache.ambari.server.state.Clusters;
import org.apache.ambari.server.state.ConfigHelper;
import org.apache.ambari.server.state.DesiredConfig;
import org.apache.ambari.server.state.Host;
import org.apache.ambari.server.utils.ThreadPools;
import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.inject.Inject;
import com.google.inject.Provider;
import com.google.inject.Singleton;
import com.google.inject.name.Named;

@Singleton
public class AgentConfigsHolder extends AgentHostDataHolder<AgentConfigsUpdateEvent> {
  public static final Logger LOG = LoggerFactory.getLogger(AgentConfigsHolder.class);
  private final Encryptor<AgentConfigsUpdateEvent> encryptor;

  @Inject
  private ConfigHelper configHelper;

  @Inject
  private Provider<Clusters> clusters;

  @Inject
  private ThreadPools threadPools;

  @Inject
  public AgentConfigsHolder(AmbariEventPublisher ambariEventPublisher, @Named("AgentConfigEncryptor") Encryptor<AgentConfigsUpdateEvent> encryptor) {
    this.encryptor = encryptor;
    ambariEventPublisher.register(this);
  }

  @Override
  public AgentConfigsUpdateEvent getCurrentData(Long hostId) throws AmbariException {
    return configHelper.getHostActualConfigs(hostId);
  }

  @Override
  public AgentConfigsUpdateEvent getCurrentData(Long hostId,
      Map<Long, Map<String, DesiredConfig>> cachedClustersDesiredConfigs) throws AmbariException {
    return configHelper.getHostActualConfigs(hostId, cachedClustersDesiredConfigs);
  }

  public AgentConfigsUpdateEvent getCurrentDataExcludeCluster(Long hostId, Long clusterId) throws AmbariException {
    return configHelper.getHostActualConfigsExcludeCluster(hostId, clusterId);
  }

  public AgentConfigsUpdateEvent getCurrentDataExcludeCluster(Long hostId, Long clusterId,
      Map<Long, Map<String, DesiredConfig>> cachedClustersDesiredConfigs) throws AmbariException {
    return configHelper.getHostActualConfigsExcludeCluster(hostId, clusterId, cachedClustersDesiredConfigs);
  }

  @Override
  protected AgentConfigsUpdateEvent handleUpdate(AgentConfigsUpdateEvent current, AgentConfigsUpdateEvent update) {
    return update;
  }

  public void updateData(Long clusterId, List<Long> hostIds) throws AmbariException {
    if (CollectionUtils.isEmpty(hostIds)) {
      // TODO cluster configs will be created before hosts assigning
      Collection<Host> hosts = clusters.get().getCluster(clusterId).getHosts();
      if (CollectionUtils.isEmpty(hosts)) {
        hostIds = clusters.get().getHosts().stream().map(Host::getHostId).collect(Collectors.toList());
      } else {
        hostIds = hosts.stream().map(Host::getHostId).collect(Collectors.toList());
      }
    }

    final List<Long> targetHostIds = hostIds;
    // Resolve each host's full (all-cluster) config so a host belonging to more than one cluster does
    // not lose the other clusters' configs when its cached event is replaced. The shared cache
    // (concurrent for the parallel path) resolves each cluster's desired configs at most once.
    Map<Long, Map<String, DesiredConfig>> cachedClustersDesiredConfigs = new ConcurrentHashMap<>();
    // IMPORTANT - DO NOT MOVE THIS READ INTO THE PARALLEL BLOCK BELOW.
    // Pre-resolve the changed cluster's desired configs here, on the calling (request) thread, while
    // that thread's write transaction is still open. The per-host recompute below runs on ForkJoinPool
    // workers, and ConfigHelper.getHostActualConfigsExcludeCluster resolves a cluster's desired configs
    // lazily via:  cachedClustersDesiredConfigs.computeIfAbsent(clusterId, id -> cl.getDesiredConfigs(false))
    // A pool worker is a DIFFERENT thread with no inherited persistence context/transaction (the
    // EntityManager/transaction is bound to the writing thread), so that lazy read sees only the last
    // COMMITTED snapshot - never this request's still-uncommitted change - and pushes the PREVIOUS
    // config value to agents. Because .get() keeps this transaction waiting for the workers, the worker
    // read is always pre-commit, producing a deterministic off-by-one: each change lands on agents one
    // change late, and the host stays stale until an ambari-server/agent restart re-resolves it.
    // Seeding the cache here (read-your-writes on the request thread => fresh value) makes computeIfAbsent
    // a no-op for this cluster, so the workers never call getDesiredConfigs themselves.
    cachedClustersDesiredConfigs.put(clusterId, clusters.get().getCluster(clusterId).getDesiredConfigs(false));
    // Process every host so successful updates are still delivered even if some fail,
    // but collect failures and rethrow afterwards so the operator is notified.
    Map<Long, AmbariException> failures = new ConcurrentHashMap<>();
    try {
      // run on the shared default ForkJoinPool so per-host recomputation is parallelized
      // without leaking a pool; .get() waits for all hosts to be processed before returning.
      threadPools.getDefaultForkJoinPool().submit(() ->
        targetHostIds.parallelStream().forEach(hostId -> {
          try {
            updateData(configHelper.getHostActualConfigs(hostId, cachedClustersDesiredConfigs));
          } catch (AmbariException e) {
            LOG.error("Agent configs update was failed for host {}", hostId, e);
            failures.put(hostId, e);
          }
        })
      ).get();
    } catch (InterruptedException e) {
      Thread.currentThread().interrupt();
      throw new AmbariException("Agent configs update was interrupted", e);
    } catch (ExecutionException e) {
      throw new AmbariException("Agent configs update was failed", e);
    }
    if (!failures.isEmpty()) {
      throw new AmbariException(String.format("Agent configs update failed for %d of %d host(s): %s",
          failures.size(), targetHostIds.size(), failures.keySet()), failures.values().iterator().next());
    }
  }

  @Override
  public AgentConfigsUpdateEvent getUpdateIfChanged(String agentHash, Long hostId) throws AmbariException {
    AgentConfigsUpdateEvent update = super.getUpdateIfChanged(agentHash, hostId);
    if (update.getClustersConfigs() == null) {
      update.setTimestamp(getData(hostId).getTimestamp());
    }
    return update;
  }

  @Override
  protected void regenerateDataIdentifiers(AgentConfigsUpdateEvent data) {
    data.setHash(getHash(data, encryptor.getEncryptionKey()));
    encryptor.encryptSensitiveData(data);
    data.setTimestamp(System.currentTimeMillis());
  }

  @Override
  protected boolean isIdentifierValid(AgentConfigsUpdateEvent data) {
    return StringUtils.isNotEmpty(data.getHash()) && data.getTimestamp() != null;
  }

  @Override
  protected void setIdentifiersToEventUpdate(AgentConfigsUpdateEvent update, AgentConfigsUpdateEvent hostData) {
    super.setIdentifiersToEventUpdate(update, hostData);
    update.setTimestamp(hostData.getTimestamp());
  }

  @Override
  protected AgentConfigsUpdateEvent getEmptyData() {
    return AgentConfigsUpdateEvent.emptyUpdate();
  }

  public void onEncryptionCapabilitiesChanged(Long hostId) {
    onHostRemoved(hostId);
  }
}
