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

import java.util.*;

import org.apache.ambari.server.orm.dao.ClusterDAO;
import org.apache.ambari.server.orm.dao.ServiceConfigDAO;
import org.apache.ambari.server.orm.entities.ClusterConfigEntity;
import org.apache.ambari.server.orm.entities.ClusterEntity;

import com.google.gson.Gson;
import com.google.inject.Inject;
import com.google.inject.Injector;
import com.google.inject.assistedinject.Assisted;
import com.google.inject.assistedinject.AssistedInject;
import com.google.inject.persist.Transactional;
import org.apache.ambari.server.orm.entities.ServiceConfigEntity;

public class ConfigImpl implements Config {
  public static final String GENERATED_TAG_PREFIX = "generatedTag_";

  private Cluster cluster;
  private String type;
  private String tag;
  private Long version;
  private Map<String, String> properties;
  private Map<String, Map<String, String>> propertiesAttributes;
  private ClusterConfigEntity entity;

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
    injector.injectMembers(this);
    
  }
  
  @AssistedInject
  public ConfigImpl(@Assisted Cluster cluster, @Assisted ClusterConfigEntity entity, Injector injector) {
    this.cluster = cluster;
    this.type = entity.getType();
    this.tag = entity.getTag();
    this.version = entity.getVersion();
    this.entity = entity;
    injector.injectMembers(this);
  }

  /**
   * Constructor for clients not using factory.
   */
  public ConfigImpl(String type) {
    this.type = type;
  }

  @Override
  public String getType() {
    return type;
  }

  @Override
  public synchronized String getTag() {
    if (this.tag == null) {
      tag = GENERATED_TAG_PREFIX + getVersion();
    }
    return tag;
  }

  @Override
  public synchronized Long getVersion() {
    if (this.version == null && cluster != null) {
      this.version = cluster.getNextConfigVersion(type);
    }
    return version;
  }

  @Override
  public synchronized Map<String, String> getProperties() {
    if (null != entity && null == properties) {
      
      properties = gson.<Map<String, String>>fromJson(entity.getData(), Map.class);
      
    }
    return null == properties ? new HashMap<String, String>()
        : new HashMap<String, String>(properties);
  }

  @Override
  public synchronized Map<String, Map<String, String>> getPropertiesAttributes() {
    if (null != entity && null == propertiesAttributes) {
      propertiesAttributes = gson.<Map<String, Map<String, String>>>fromJson(entity.getAttributes(), Map.class);
    }
    return null == propertiesAttributes ? null : new HashMap<String, Map<String, String>>(propertiesAttributes);
  }

  @Override
  public synchronized void setTag(String tag) {
    this.tag = tag;
  }

  @Override
  public synchronized void setVersion(Long version) {
    this.version = version;
  }

  @Override
  public synchronized void setProperties(Map<String, String> properties) {
    this.properties = properties;
  }

  @Override
  public void setPropertiesAttributes(Map<String, Map<String, String>> propertiesAttributes) {
    this.propertiesAttributes = propertiesAttributes;
  }

  @Override
  public synchronized void updateProperties(Map<String, String> properties) {
    this.properties.putAll(properties);
  }

  @Override
  public synchronized List<Long> getServiceConfigVersions() {
    if (cluster == null || type == null || version == null) {
      return Collections.emptyList();
    }
    return serviceConfigDAO.getServiceConfigVersionsByConfig(cluster.getClusterId(), type, version);
  }

  @Override
  public synchronized void deleteProperties(List<String> properties) {
    for (String key : properties) {
      this.properties.remove(key);
    }
  }
  
  @Transactional
  @Override
  public synchronized void persist() {
    
    ClusterEntity clusterEntity = clusterDAO.findById(cluster.getClusterId());

    ClusterConfigEntity entity = new ClusterConfigEntity();
    entity.setClusterEntity(clusterEntity);
    entity.setClusterId(cluster.getClusterId());
    entity.setType(getType());
    entity.setVersion(getVersion());
    entity.setTag(getTag());
    entity.setTimestamp(new Date().getTime());
    
    entity.setData(gson.toJson(getProperties()));
    if (null != getPropertiesAttributes()) {
      entity.setAttributes(gson.toJson(getPropertiesAttributes()));
    }
    clusterDAO.createConfig(entity);

    clusterEntity.getClusterConfigEntities().add(entity);
    clusterDAO.merge(clusterEntity);
    cluster.refresh();

  }



}
