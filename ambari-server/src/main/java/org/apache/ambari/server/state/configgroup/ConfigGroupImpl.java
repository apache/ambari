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

import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.locks.ReadWriteLock;
import java.util.concurrent.locks.ReentrantReadWriteLock;

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

import com.google.gson.Gson;
import com.google.inject.Inject;
import com.google.inject.Injector;
import com.google.inject.assistedinject.Assisted;
import com.google.inject.assistedinject.AssistedInject;
import com.google.inject.persist.Transactional;

public class ConfigGroupImpl implements ConfigGroup {
  private static final Logger LOG = LoggerFactory.getLogger(ConfigGroupImpl.class);
  private final ReadWriteLock readWriteLock = new ReentrantReadWriteLock();

  private Cluster cluster;
  private ConfigGroupEntity configGroupEntity;
  private Map<Long, Host> hosts;
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
                         @Assisted("hosts") Map<Long, Host> hosts,
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
      this.hosts = new HashMap<Long, Host>();
    }

    if (configs != null) {
      configurations = configs;
    } else {
      configurations = new HashMap<String, Config>();
    }
  }

  @AssistedInject
  public ConfigGroupImpl(@Assisted Cluster cluster,
                         @Assisted ConfigGroupEntity configGroupEntity,
                         Injector injector) {
    injector.injectMembers(this);
    this.cluster = cluster;

    this.configGroupEntity = configGroupEntity;
    configurations = new HashMap<String, Config>();
    hosts = new HashMap<Long, Host>();

    // Populate configs
    for (ConfigGroupConfigMappingEntity configMappingEntity : configGroupEntity
      .getConfigGroupConfigMappingEntities()) {

      Config config = cluster.getConfig(configMappingEntity.getConfigType(),
        configMappingEntity.getVersionTag());

      if (config != null) {
        configurations.put(config.getType(), config);
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
        HostEntity hostEntity = hostMappingEntity.getHostEntity();
        if (host != null && hostEntity != null) {
          hosts.put(hostEntity.getHostId(), host);
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
    readWriteLock.writeLock().lock();
    try {
      configGroupEntity.setGroupName(name);
    } finally {
      readWriteLock.writeLock().unlock();
    }

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
    readWriteLock.writeLock().lock();
    try {
      configGroupEntity.setTag(tag);
    } finally {
      readWriteLock.writeLock().unlock();
    }

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
    readWriteLock.writeLock().lock();
    try {
      configGroupEntity.setDescription(description);
    } finally {
      readWriteLock.writeLock().unlock();
    }

  }

  @Override
  public Map<Long, Host> getHosts() {
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
  public void setHosts(Map<Long, Host> hosts) {
    readWriteLock.writeLock().lock();
    try {
      this.hosts = hosts;
    } finally {
      readWriteLock.writeLock().unlock();
    }

  }

  /**
   * Helper method to recreate configs mapping
   * @param configs
   */
  @Override
  public void setConfigurations(Map<String, Config> configs) {
    readWriteLock.writeLock().lock();
    try {
      configurations = configs;
    } finally {
      readWriteLock.writeLock().unlock();
    }

  }

  @Override
  @Transactional
  public void removeHost(Long hostId) throws AmbariException {
    readWriteLock.writeLock().lock();
    try {
      if (hosts.containsKey(hostId)) {
        String hostName = hosts.get(hostId).getHostName();
        LOG.info("Removing host from config group, hostid = " + hostId + ", hostname = " + hostName);
        hosts.remove(hostId);
        try {
          ConfigGroupHostMappingEntityPK hostMappingEntityPK = new
            ConfigGroupHostMappingEntityPK();
          hostMappingEntityPK.setHostId(hostId);
          hostMappingEntityPK.setConfigGroupId(configGroupEntity.getGroupId());
          configGroupHostMappingDAO.removeByPK(hostMappingEntityPK);
        } catch (Exception e) {
          LOG.error("Failed to delete config group host mapping"
            + ", clusterName = " + getClusterName()
            + ", id = " + getId()
            + ", hostid = " + hostId
            + ", hostname = " + hostName, e);
          throw new AmbariException(e.getMessage());
        }
      }
    } finally {
      readWriteLock.writeLock().unlock();
    }
  }

  @Override
  public void persist() {
    cluster.getClusterGlobalLock().writeLock().lock();
    try {
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
    } finally {
      cluster.getClusterGlobalLock().writeLock().unlock();
    }
  }

  /**
   * Persist Config group with host mapping and configurations
   *
   * @throws Exception
   */
  @Transactional
  void persistEntities() {
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
  @Override
  @Transactional
  public void persistHostMapping() {
    if (isPersisted) {
      // Delete existing mappings and create new ones
      configGroupHostMappingDAO.removeAllByGroup(configGroupEntity.getGroupId());
      configGroupEntity.setConfigGroupHostMappingEntities(new HashSet<ConfigGroupHostMappingEntity>());
    }

    if (hosts != null && !hosts.isEmpty()) {
      for (Host host : hosts.values()) {
        HostEntity hostEntity = hostDAO.findById(host.getHostId());
        if (hostEntity != null) {
          ConfigGroupHostMappingEntity hostMappingEntity = new
            ConfigGroupHostMappingEntity();
          hostMappingEntity.setHostId(hostEntity.getHostId());
          hostMappingEntity.setHostEntity(hostEntity);
          hostMappingEntity.setConfigGroupEntity(configGroupEntity);
          hostMappingEntity.setConfigGroupId(configGroupEntity.getGroupId());
          configGroupEntity.getConfigGroupHostMappingEntities().add
                  (hostMappingEntity);
          configGroupHostMappingDAO.create(hostMappingEntity);
        } else {
          LOG.warn("Host seems to be deleted, cannot create host to config " +
            "group mapping, host = " + host.getHostName());
        }
      }
    }
    // TODO: Make sure this does not throw Nullpointer based on JPA docs
    configGroupEntity = configGroupDAO.merge(configGroupEntity);
  }

  /**
   * Persist config group config mapping and create configs if not in DB
   *
   * @param clusterEntity
   * @throws Exception
   */
  @Transactional
  void persistConfigMapping(ClusterEntity clusterEntity) {
    if (isPersisted) {
      configGroupConfigMappingDAO.removeAllByGroup(configGroupEntity.getGroupId());
      configGroupEntity.setConfigGroupConfigMappingEntities(new HashSet<ConfigGroupConfigMappingEntity>());
    }

    if (configurations != null && !configurations.isEmpty()) {
      for (Config config : configurations.values()) {
        ClusterConfigEntity clusterConfigEntity = clusterDAO.findConfig
          (cluster.getClusterId(), config.getType(), config.getTag());

        if (clusterConfigEntity == null) {
          config.setVersion(cluster.getNextConfigVersion(config.getType()));
          config.setStackId(cluster.getDesiredStackVersion());
          // Create configuration
          clusterConfigEntity = new ClusterConfigEntity();
          clusterConfigEntity.setClusterId(clusterEntity.getClusterId());
          clusterConfigEntity.setClusterEntity(clusterEntity);
          clusterConfigEntity.setStack(clusterEntity.getDesiredStack());
          clusterConfigEntity.setType(config.getType());
          clusterConfigEntity.setVersion(config.getVersion());
          clusterConfigEntity.setTag(config.getTag());
          clusterConfigEntity.setData(gson.toJson(config.getProperties()));
          if (null != config.getPropertiesAttributes()) {
            clusterConfigEntity.setAttributes(gson.toJson(config.getPropertiesAttributes()));
          }
          clusterConfigEntity.setTimestamp(System.currentTimeMillis());
          clusterDAO.createConfig(clusterConfigEntity);
          clusterEntity.getClusterConfigEntities().add(clusterConfigEntity);
          cluster.addConfig(config);
          clusterDAO.merge(clusterEntity);
          cluster.refresh();
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

        configGroupEntity = configGroupDAO.merge(configGroupEntity);
      }
    }
  }

  void saveIfPersisted() {
    if (isPersisted) {
      save(clusterDAO.findById(cluster.getClusterId()));
    }
  }

  @Transactional
  void save(ClusterEntity clusterEntity) {
    persistHostMapping();
    persistConfigMapping(clusterEntity);
  }

  @Override
  public void delete() {
    cluster.getClusterGlobalLock().writeLock().lock();
    try {
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
    } finally {
      cluster.getClusterGlobalLock().writeLock().unlock();
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
        HostEntity hostEntity = hostDAO.findByName(host.getHostName());
        if (hostEntity != null) {
          hosts.put(hostEntity.getHostId(), host);
        }
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
          if (c.getType().equals(config.getType()) && c.getTag().equals
            (config.getTag())) {
            throw new DuplicateResourceException("Config " + config.getType() +
              " with tag " + config.getTag() + " is already associated " +
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
    cluster.getClusterGlobalLock().readLock().lock();
    try {
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
            .CONFIGURATION_CONFIG_TAG_PROPERTY_ID, config.getTag());
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
    } finally {
      cluster.getClusterGlobalLock().readLock().unlock();
    }
  }

  @Override
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


  @Override
  public String getServiceName() {
    readWriteLock.readLock().lock();
    try {
      return configGroupEntity.getServiceName();
    } finally {
      readWriteLock.readLock().unlock();
    }

  }

  @Override
  public void setServiceName(String serviceName) {
    readWriteLock.writeLock().lock();
    try {
      configGroupEntity.setServiceName(serviceName);
    } finally {
      readWriteLock.writeLock().unlock();
    }

  }
}
