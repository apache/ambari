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

package org.apache.ambari.server.state;

import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.locks.ReadWriteLock;
import java.util.concurrent.locks.ReentrantReadWriteLock;

import org.apache.ambari.server.orm.dao.ClusterDAO;
import org.apache.ambari.server.orm.dao.ServiceConfigDAO;
import org.apache.ambari.server.orm.entities.ClusterConfigEntity;
import org.apache.ambari.server.orm.entities.ClusterEntity;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.gson.Gson;
import com.google.inject.Inject;
import com.google.inject.Injector;
import com.google.inject.assistedinject.Assisted;
import com.google.inject.assistedinject.AssistedInject;
import com.google.inject.persist.Transactional;

public class ConfigImpl implements Config {
  /**
   * Logger.
   */
  private final static Logger LOG = LoggerFactory.getLogger(ConfigImpl.class);

  public static final String GENERATED_TAG_PREFIX = "generatedTag_";

  private final ReadWriteLock readWriteLock = new ReentrantReadWriteLock();

  private Cluster cluster;
  private StackId stackId;
  private String type;
  private volatile String tag;
  private volatile Long version;
  private volatile Map<String, String> properties;
  private volatile Map<String, Map<String, String>> propertiesAttributes;
  private ClusterConfigEntity entity;
  private volatile Map<PropertyInfo.PropertyType, Set<String>> propertiesTypes;

  @Inject
  private ClusterDAO clusterDAO;

  @Inject
  private Gson gson;

  @Inject
  private ServiceConfigDAO serviceConfigDAO;

  @AssistedInject
  public ConfigImpl(@Assisted Cluster cluster, @Assisted String type, @Assisted Map<String, String> properties,
      @Assisted Map<String, Map<String, String>> propertiesAttributes, Injector injector) {
    this.cluster = cluster;
    this.type = type;
    this.properties = properties;
    this.propertiesAttributes = propertiesAttributes;

    // when creating a brand new config without a backing entity, use the
    // cluster's desired stack as the config's stack
    stackId = cluster.getDesiredStackVersion();

    injector.injectMembers(this);
    propertiesTypes = cluster.getConfigPropertiesTypes(type);
  }


  @AssistedInject
  public ConfigImpl(@Assisted Cluster cluster, @Assisted ClusterConfigEntity entity, Injector injector) {
    this.cluster = cluster;
    type = entity.getType();
    tag = entity.getTag();
    version = entity.getVersion();

    // when using an existing entity, use the actual value of the entity's stack
    stackId = new StackId(entity.getStack());

    this.entity = entity;
    injector.injectMembers(this);
    propertiesTypes = cluster.getConfigPropertiesTypes(type);
  }

  /**
   * Constructor for clients not using factory.
   */
  public ConfigImpl(String type) {
    this.type = type;
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public StackId getStackId() {
    readWriteLock.readLock().lock();
    try {
      return stackId;
    } finally {
      readWriteLock.readLock().unlock();
    }

  }

  @Override
  public Map<PropertyInfo.PropertyType, Set<String>> getPropertiesTypes() {
    readWriteLock.readLock().lock();
    try {
      return propertiesTypes;
    } finally {
      readWriteLock.readLock().unlock();
    }
  }

  @Override
  public void setPropertiesTypes(Map<PropertyInfo.PropertyType, Set<String>> propertiesTypes) {
    readWriteLock.writeLock().lock();
    try {
      this.propertiesTypes = propertiesTypes;
    } finally {
      readWriteLock.writeLock().unlock();
    }
  }

  @Override
  public void setStackId(StackId stackId) {
    readWriteLock.writeLock().lock();
    try {
      this.stackId = stackId;
    } finally {
      readWriteLock.writeLock().unlock();
    }

  }

  @Override
  public String getType() {
    readWriteLock.readLock().lock();
    try {
      return type;
    } finally {
      readWriteLock.readLock().unlock();
    }

  }

  @Override
  public String getTag() {
    if (tag == null) {
      readWriteLock.writeLock().lock();
      try {
        if (tag == null) {
          tag = GENERATED_TAG_PREFIX + getVersion();
        }
      } finally {
        readWriteLock.writeLock().unlock();
      }
    }

    readWriteLock.readLock().lock();
    try {

      return tag;
    } finally {
      readWriteLock.readLock().unlock();
    }

  }

  @Override
  public Long getVersion() {
    if (version == null && cluster != null) {
      readWriteLock.writeLock().lock();
      try {
        if (version == null) {
          version = cluster.getNextConfigVersion(type); //pure DB calculation call, no cluster locking required
        }
      } finally {
        readWriteLock.writeLock().unlock();
      }
    }

    readWriteLock.readLock().lock();
    try {
      return version;
    } finally {
      readWriteLock.readLock().unlock();
    }

  }

  @Override
  public Map<String, String> getProperties() {
    if (null != entity && null == properties) {
      readWriteLock.writeLock().lock();
      try {
        if (properties == null) {
          properties = gson.<Map<String, String>>fromJson(entity.getData(), Map.class);
        }
      } finally {
        readWriteLock.writeLock().unlock();
      }
    }

    readWriteLock.readLock().lock();
    try {
      return null == properties ? new HashMap<String, String>()
          : new HashMap<String, String>(properties);
    } finally {
      readWriteLock.readLock().unlock();
    }

  }

  @Override
  public Map<String, Map<String, String>> getPropertiesAttributes() {
    if (null != entity && null == propertiesAttributes) {
      readWriteLock.writeLock().lock();
      try {
        if (propertiesAttributes == null) {
          propertiesAttributes = gson.<Map<String, Map<String, String>>>fromJson(entity.getAttributes(), Map.class);
        }
      } finally {
        readWriteLock.writeLock().unlock();
      }
    }

    readWriteLock.readLock().lock();
    try {
      return null == propertiesAttributes ? null : new HashMap<String, Map<String, String>>(propertiesAttributes);
    } finally {
      readWriteLock.readLock().unlock();
    }

  }

  @Override
  public void setTag(String tag) {
    readWriteLock.writeLock().lock();
    try {
      this.tag = tag;
    } finally {
      readWriteLock.writeLock().unlock();
    }

  }

  @Override
  public void setVersion(Long version) {
    readWriteLock.writeLock().lock();
    try {
      this.version = version;
    } finally {
      readWriteLock.writeLock().unlock();
    }

  }

  @Override
  public void setProperties(Map<String, String> properties) {
    readWriteLock.writeLock().lock();
    try {
      this.properties = properties;
    } finally {
      readWriteLock.writeLock().unlock();
    }

  }

  @Override
  public void setPropertiesAttributes(Map<String, Map<String, String>> propertiesAttributes) {
    readWriteLock.writeLock().lock();
    try {
      this.propertiesAttributes = propertiesAttributes;
    } finally {
      readWriteLock.writeLock().unlock();
    }

  }

  @Override
  public void updateProperties(Map<String, String> properties) {
    readWriteLock.writeLock().lock();
    try {
      this.properties.putAll(properties);
    } finally {
      readWriteLock.writeLock().unlock();
    }

  }

  @Override
  public List<Long> getServiceConfigVersions() {
    readWriteLock.readLock().lock();
    try {
      if (cluster == null || type == null || version == null) {
        return Collections.emptyList();
      }
      return serviceConfigDAO.getServiceConfigVersionsByConfig(cluster.getClusterId(), type, version);
    } finally {
      readWriteLock.readLock().unlock();
    }

  }

  @Override
  public void deleteProperties(List<String> properties) {
    readWriteLock.writeLock().lock();
    try {
      for (String key : properties) {
        this.properties.remove(key);
      }
    } finally {
      readWriteLock.writeLock().unlock();
    }

  }

  @Override
  public void persist() {
    persist(true);
  }

  @Override
  @Transactional
  public void persist(boolean newConfig) {
    cluster.getClusterGlobalLock().writeLock().lock(); //null cluster is not expected, NPE anyway later in code
    try {
      readWriteLock.writeLock().lock();
      try {
        ClusterEntity clusterEntity = clusterDAO.findById(cluster.getClusterId());

        if (newConfig) {
          ClusterConfigEntity entity = new ClusterConfigEntity();
          entity.setClusterEntity(clusterEntity);
          entity.setClusterId(cluster.getClusterId());
          entity.setType(getType());
          entity.setVersion(getVersion());
          entity.setTag(getTag());
          entity.setTimestamp(new Date().getTime());
          entity.setStack(clusterEntity.getDesiredStack());
          entity.setData(gson.toJson(getProperties()));

          if (null != getPropertiesAttributes()) {
            entity.setAttributes(gson.toJson(getPropertiesAttributes()));
          }

          clusterDAO.createConfig(entity);
          clusterEntity.getClusterConfigEntities().add(entity);

          // save the entity, forcing a flush to ensure the refresh picks up the
          // newest data
          clusterDAO.merge(clusterEntity, true);
          cluster.refresh();
        } else {
          // only supporting changes to the properties
          ClusterConfigEntity entity = null;

          // find the existing configuration to update
          for (ClusterConfigEntity cfe : clusterEntity.getClusterConfigEntities()) {
            if (getTag().equals(cfe.getTag()) &&
                getType().equals(cfe.getType()) &&
                getVersion().equals(cfe.getVersion())) {
              entity = cfe;
              break;
            }
          }

          // if the configuration was found, then update it
          if (null != entity) {
            LOG.debug(
                "Updating {} version {} with new configurations; a new version will not be created",
                getType(), getVersion());

            entity.setData(gson.toJson(getProperties()));

            // save the entity, forcing a flush to ensure the refresh picks up the
            // newest data
            clusterDAO.merge(clusterEntity, true);
            cluster.refresh();
          }
        }
      } finally {
        readWriteLock.writeLock().unlock();
      }
    } finally {
      cluster.getClusterGlobalLock().writeLock().unlock();
    }

  }

}
