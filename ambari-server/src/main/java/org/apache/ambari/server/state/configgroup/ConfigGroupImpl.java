/**
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
package org.apache.ambari.server.state.configgroup;

import com.google.gson.Gson;
import com.google.inject.Inject;
import com.google.inject.Injector;
import com.google.inject.assistedinject.Assisted;
import com.google.inject.assistedinject.AssistedInject;
import com.google.inject.persist.Transactional;
import org.apache.ambari.server.AmbariException;
import org.apache.ambari.server.DuplicateResourceException;
import org.apache.ambari.server.controller.ConfigGroupResponse;
import org.apache.ambari.server.controller.internal.ConfigurationResourceProvider;
import org.apache.ambari.server.orm.dao.ClusterDAO;
import org.apache.ambari.server.orm.dao.ConfigGroupConfigMappingDAO;
import org.apache.ambari.server.orm.dao.ConfigGroupDAO;
import org.apache.ambari.server.orm.dao.ConfigGroupHostMappingDAO;
import org.apache.ambari.server.orm.dao.HostDAO;
import org.apache.ambari.server.orm.entities.ClusterConfigEntity;
import org.apache.ambari.server.orm.entities.ClusterConfigEntityPK;
import org.apache.ambari.server.orm.entities.ClusterEntity;
import org.apache.ambari.server.orm.entities.ConfigGroupConfigMappingEntity;
import org.apache.ambari.server.orm.entities.ConfigGroupEntity;
import org.apache.ambari.server.orm.entities.ConfigGroupHostMappingEntity;
import org.apache.ambari.server.orm.entities.ConfigGroupHostMappingEntityPK;
import org.apache.ambari.server.orm.entities.HostEntity;
import org.apache.ambari.server.state.Cluster;
import org.apache.ambari.server.state.Clusters;
import org.apache.ambari.server.state.Config;
import org.apache.ambari.server.state.ConfigFactory;
import org.apache.ambari.server.state.Host;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.locks.ReadWriteLock;
import java.util.concurrent.locks.ReentrantReadWriteLock;

public class ConfigGroupImpl implements ConfigGroup {
  private static final Logger LOG = LoggerFactory.getLogger(ConfigGroupImpl.class);
  private final ReadWriteLock readWriteLock = new ReentrantReadWriteLock();

  private Cluster cluster;
  private ConfigGroupEntity configGroupEntity;
  private Map<String, Host> hosts;
  private Map<String, Config> configurations;
  private volatile boolean isPersisted = false;

  @Inject
  private Gson gson;
  @Inject
  private ConfigGroupDAO configGroupDAO;
  @Inject
  private ConfigGroupConfigMappingDAO configGroupConfigMappingDAO;
  @Inject
  private ConfigGroupHostMappingDAO configGroupHostMappingDAO;
  @Inject
  private HostDAO hostDAO;
  @Inject
  private ClusterDAO clusterDAO;
  @Inject
  Clusters clusters;
  @Inject
  private ConfigFactory configFactory;

  @AssistedInject
  public ConfigGroupImpl(@Assisted("cluster") Cluster cluster,
                         @Assisted("name") String name,
                         @Assisted("tag") String tag,
                         @Assisted("description") String description,
                         @Assisted("configs") Map<String, Config> configs,
                         @Assisted("hosts") Map<String, Host> hosts,
                         Injector injector) {
    injector.injectMembers(this);
    this.cluster = cluster;

    configGroupEntity = new ConfigGroupEntity();
    configGroupEntity.setClusterId(cluster.getClusterId());
    configGroupEntity.setGroupName(name);
    configGroupEntity.setTag(tag);
    configGroupEntity.setDescription(description);

    if (hosts != null) {
      this.hosts = hosts;
    } else {
      this.hosts = new HashMap<String, Host>();
    }

    if (configs != null) {
      this.configurations = configs;
    } else {
      this.configurations = new HashMap<String, Config>();
    }
  }

  @AssistedInject
  public ConfigGroupImpl(@Assisted Cluster cluster,
                         @Assisted ConfigGroupEntity configGroupEntity,
                         Injector injector) {
    injector.injectMembers(this);
    this.cluster = cluster;

    this.configGroupEntity = configGroupEntity;
    this.configurations = new HashMap<String, Config>();
    this.hosts = new HashMap<String, Host>();

    // Populate configs
    for (ConfigGroupConfigMappingEntity configMappingEntity : configGroupEntity
      .getConfigGroupConfigMappingEntities()) {

      Config config = cluster.getConfig(configMappingEntity.getConfigType(),
        configMappingEntity.getVersionTag());

      if (config != null) {
        this.configurations.put(config.getType(), config);
      } else {
        LOG.warn("Unable to find config mapping for config group"
          + ", clusterName = " + cluster.getClusterName()
          + ", type = " + configMappingEntity.getConfigType()
          + ", tag = " + configMappingEntity.getVersionTag());
      }
    }

    // Populate Hosts
    for (ConfigGroupHostMappingEntity hostMappingEntity : configGroupEntity
      .getConfigGroupHostMappingEntities()) {

      try {
        Host host = clusters.getHost(hostMappingEntity.getHostname());
        if (host != null) {
          this.hosts.put(host.getHostName(), host);
        }
      } catch (AmbariException e) {
        String msg = "Host seems to be deleted but Config group mapping still " +
          "exists !";
        LOG.warn(msg);
        LOG.debug(msg, e);
      }
    }

    isPersisted = true;
  }

  @Override
  public Long getId() {
    return configGroupEntity.getGroupId();
  }

  @Override
  public String getName() {
    readWriteLock.readLock().lock();
    try {
      return configGroupEntity.getGroupName();
    } finally {
      readWriteLock.readLock().unlock();
    }
  }

  @Override
  public void setName(String name) {
    this.configGroupEntity.setGroupName(name);
  }

  @Override
  public String getClusterName() {
    return configGroupEntity.getClusterEntity().getClusterName();
  }

  @Override
  public String getTag() {
    readWriteLock.readLock().lock();
    try {
      return configGroupEntity.getTag();
    } finally {
      readWriteLock.readLock().unlock();
    }
  }

  @Override
  public void setTag(String tag) {
    this.configGroupEntity.setTag(tag);
  }

  @Override
  public String getDescription() {
    readWriteLock.readLock().lock();
    try {
      return configGroupEntity.getDescription();
    } finally {
      readWriteLock.readLock().unlock();
    }
  }

  @Override
  public void setDescription(String description) {
    this.configGroupEntity.setDescription(description);
  }

  @Override
  public Map<String, Host> getHosts() {
    readWriteLock.readLock().lock();
    try {
      return Collections.unmodifiableMap(hosts);
    } finally {
      readWriteLock.readLock().unlock();
    }
  }

  @Override
  public Map<String, Config> getConfigurations() {
    readWriteLock.readLock().lock();
    try {
      return Collections.unmodifiableMap(configurations);
    } finally {
      readWriteLock.readLock().unlock();
    }

  }

  /**
   * Helper method to recreate host mapping
   * @param hosts
   */
  @Override
  public void setHosts(Map<String, Host> hosts) {
    this.hosts = hosts;
  }

  /**
   * Helper method to recreate configs mapping
   * @param configs
   */
  @Override
  public void setConfigurations(Map<String, Config> configs) {
    this.configurations = configs;
  }

  @Override
  @Transactional
  public void removeHost(String hostname) throws AmbariException {
    readWriteLock.writeLock().lock();
    try {
      if (hosts.containsKey(hostname)) {
        LOG.info("Removing host from config group, hostname = " + hostname);
        hosts.remove(hostname);
        try {
          ConfigGroupHostMappingEntityPK hostMappingEntityPK = new
            ConfigGroupHostMappingEntityPK();
          hostMappingEntityPK.setHostname(hostname);
          hostMappingEntityPK.setConfigGroupId(configGroupEntity.getGroupId());
          configGroupHostMappingDAO.removeByPK(hostMappingEntityPK);
        } catch (Exception e) {
          LOG.error("Failed to delete config group host mapping"
            + ", clusterName = " + getClusterName()
            + ", id = " + getId()
            + ", hostname = " + hostname, e);
          throw new AmbariException(e.getMessage());
        }
      }
    } finally {
      readWriteLock.writeLock().unlock();
    }
  }

  @Override
  @Transactional
  public void persist() {
    readWriteLock.writeLock().lock();
    try {
      if (!isPersisted) {
        persistEntities();
        refresh();
        cluster.refresh();
        isPersisted = true;
      } else {
        saveIfPersisted();
      }
    } finally {
      readWriteLock.writeLock().unlock();
    }
  }

  /**
   * Persist Config group with host mapping and configurations
   *
   * @throws Exception
   */
  @Transactional
  private void persistEntities() {
    ClusterEntity clusterEntity = clusterDAO.findById(cluster.getClusterId());
    configGroupEntity.setClusterEntity(clusterEntity);
    configGroupEntity.setTimestamp(System.currentTimeMillis());
    configGroupDAO.create(configGroupEntity);

    persistConfigMapping(clusterEntity);
    persistHostMapping();
  }

  // TODO: Test rollback scenario

  /**
   * Persist host mapping
   *
   * @throws Exception
   */
  @Transactional
  private void persistHostMapping() {
    if (isPersisted) {
      // Delete existing mappings and create new ones
      configGroupHostMappingDAO.removeAllByGroup(configGroupEntity.getGroupId());
      configGroupEntity.getConfigGroupHostMappingEntities().clear();
    }

    if (hosts != null && !hosts.isEmpty()) {
      for (Host host : hosts.values()) {
        HostEntity hostEntity = hostDAO.findByName(host.getHostName());
        if (hostEntity != null) {
          ConfigGroupHostMappingEntity hostMappingEntity = new
            ConfigGroupHostMappingEntity();
          hostMappingEntity.setHostname(host.getHostName());
          hostMappingEntity.setHostEntity(hostEntity);
          hostMappingEntity.setConfigGroupEntity(configGroupEntity);
          hostMappingEntity.setConfigGroupId(configGroupEntity.getGroupId());
          configGroupHostMappingDAO.create(hostMappingEntity);
          // TODO: Make sure this does not throw Nullpointer based on JPA docs
          configGroupEntity.getConfigGroupHostMappingEntities().add
            (hostMappingEntity);
          configGroupDAO.merge(configGroupEntity);
        } else {
          LOG.warn("Host seems to be deleted, cannot create host to config " +
            "group mapping, host = " + host.getHostName());
        }
      }
    }
  }

  /**
   * Persist config group config mapping and create configs if not in DB
   *
   * @param clusterEntity
   * @throws Exception
   */
  @Transactional
  private void persistConfigMapping(ClusterEntity clusterEntity) {
    if (isPersisted) {
      configGroupConfigMappingDAO.removeAllByGroup(configGroupEntity.getGroupId());
      configGroupEntity.getConfigGroupConfigMappingEntities().clear();
    }

    if (configurations != null && !configurations.isEmpty()) {
      for (Config config : configurations.values()) {
        ClusterConfigEntityPK clusterConfigEntityPK = new ClusterConfigEntityPK();
        clusterConfigEntityPK.setClusterId(cluster.getClusterId());
        clusterConfigEntityPK.setTag(config.getVersionTag());
        clusterConfigEntityPK.setType(config.getType());
        ClusterConfigEntity clusterConfigEntity = clusterDAO.findConfig
          (clusterConfigEntityPK);

        if (clusterConfigEntity == null) {
          // Create configuration
          clusterConfigEntity = new ClusterConfigEntity();
          clusterConfigEntity.setClusterId(clusterEntity.getClusterId());
          clusterConfigEntity.setClusterEntity(clusterEntity);
          clusterConfigEntity.setType(config.getType());
          clusterConfigEntity.setTag(config.getVersionTag());
          clusterConfigEntity.setData(gson.toJson(config.getProperties()));
          clusterConfigEntity.setTimestamp(System.currentTimeMillis());

          // TODO: Is locking necessary and functional ?
          cluster.getClusterGlobalLock().writeLock().lock();
          try {
            clusterDAO.createConfig(clusterConfigEntity);
            clusterEntity.getClusterConfigEntities().add(clusterConfigEntity);
            cluster.addConfig(config);
            clusterDAO.merge(clusterEntity);
            cluster.refresh();
          } finally {
            cluster.getClusterGlobalLock().writeLock().unlock();
          }
        }

        ConfigGroupConfigMappingEntity configMappingEntity =
          new ConfigGroupConfigMappingEntity();
        configMappingEntity.setTimestamp(System.currentTimeMillis());
        configMappingEntity.setClusterId(clusterEntity.getClusterId());
        configMappingEntity.setClusterConfigEntity(clusterConfigEntity);
        configMappingEntity.setConfigGroupEntity(configGroupEntity);
        configMappingEntity.setConfigGroupId(configGroupEntity.getGroupId());
        configMappingEntity.setConfigType(clusterConfigEntity.getType());
        configMappingEntity.setVersionTag(clusterConfigEntity.getTag());
        configGroupConfigMappingDAO.create(configMappingEntity);
        configGroupEntity.getConfigGroupConfigMappingEntities().add
          (configMappingEntity);

        configGroupDAO.merge(configGroupEntity);
      }
    }
  }

  @Transactional
  private void saveIfPersisted() {
    ClusterEntity clusterEntity = clusterDAO.findById(cluster.getClusterId());

    if (isPersisted) {
      configGroupDAO.merge(configGroupEntity);
      persistHostMapping();
      persistConfigMapping(clusterEntity);
    }
  }

  @Override
  @Transactional
  public void delete() {
    readWriteLock.writeLock().lock();
    try {
      configGroupConfigMappingDAO.removeAllByGroup(configGroupEntity.getGroupId());
      configGroupHostMappingDAO.removeAllByGroup(configGroupEntity.getGroupId());
      configGroupDAO.removeByPK(configGroupEntity.getGroupId());
      cluster.refresh();
      isPersisted = false;
    } finally {
      readWriteLock.writeLock().unlock();
    }
  }

  @Override
  public void addHost(Host host) throws AmbariException {
    readWriteLock.writeLock().lock();
    try {
      if (hosts != null && !hosts.isEmpty()) {
        for (Host h : hosts.values()) {
          if (h.getHostName().equals(host.getHostName())) {
            throw new DuplicateResourceException("Host " + h.getHostName() +
              "is already associated with Config Group " +
              configGroupEntity.getGroupName());
          }
        }
        hosts.put(host.getHostName(), host);
      }
    } finally {
      readWriteLock.writeLock().unlock();
    }
  }

  @Override
  public void addConfiguration(Config config) throws AmbariException {
    readWriteLock.writeLock().lock();
    try {
      if (configurations != null && !configurations.isEmpty()) {
        for (Config c : configurations.values()) {
          if (c.getType().equals(config.getType()) && c.getVersionTag().equals
            (config.getVersionTag())) {
            throw new DuplicateResourceException("Config " + config.getType() +
              " with tag " + config.getVersionTag() + " is already associated " +
              "with Config Group " + configGroupEntity.getGroupName());
          }
        }
        configurations.put(config.getType(), config);
      }
    } finally {
      readWriteLock.writeLock().unlock();
    }
  }

  @Override
  public ConfigGroupResponse convertToResponse() throws AmbariException {
    readWriteLock.readLock().lock();
    try {
      Set<Map<String, Object>> hostnames = new HashSet<Map<String, Object>>();
      for (Host host : hosts.values()) {
        Map<String, Object> hostMap = new HashMap<String, Object>();
        hostMap.put("host_name", host.getHostName());
        hostnames.add(hostMap);
      }

      Set<Map<String, Object>> configObjMap = new HashSet<Map<String,
        Object>>();

      for (Config config : configurations.values()) {
        Map<String, Object> configMap = new HashMap<String, Object>();
        configMap.put(ConfigurationResourceProvider
          .CONFIGURATION_CONFIG_TYPE_PROPERTY_ID, config.getType());
        configMap.put(ConfigurationResourceProvider
          .CONFIGURATION_CONFIG_TAG_PROPERTY_ID, config.getVersionTag());
        configObjMap.add(configMap);
      }

      ConfigGroupResponse configGroupResponse = new ConfigGroupResponse(
        configGroupEntity.getGroupId(), cluster.getClusterName(),
        configGroupEntity.getGroupName(), configGroupEntity.getTag(),
        configGroupEntity.getDescription(),
        hostnames, configObjMap);
      return configGroupResponse;
    } finally {
      readWriteLock.readLock().unlock();
    }
  }

  @Transactional
  public void refresh() {
    readWriteLock.writeLock().lock();
    try {
      if (isPersisted) {
        ConfigGroupEntity groupEntity = configGroupDAO.findById
          (configGroupEntity.getGroupId());
        configGroupDAO.refresh(groupEntity);
        // TODO What other entities should refresh?
      }
    } finally {
      readWriteLock.writeLock().unlock();
    }
  }


}
