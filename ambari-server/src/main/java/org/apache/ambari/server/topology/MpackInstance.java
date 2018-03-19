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

package org.apache.ambari.server.topology;

import java.util.ArrayList;
import java.util.Collection;

import org.apache.ambari.server.controller.internal.Stack;
import org.apache.ambari.server.orm.entities.BlueprintEntity;
import org.apache.ambari.server.orm.entities.BlueprintMpackInstanceEntity;
import org.apache.ambari.server.orm.entities.MpackInstanceConfigEntity;
import org.apache.ambari.server.orm.entities.MpackInstanceEntity;
import org.apache.ambari.server.orm.entities.MpackInstanceServiceEntity;
import org.apache.ambari.server.orm.entities.MpackServiceConfigEntity;
import org.apache.ambari.server.orm.entities.TopologyRequestEntity;
import org.apache.ambari.server.orm.entities.TopologyRequestMpackInstanceEntity;
import org.apache.ambari.server.state.StackId;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonProperty;

public class MpackInstance implements Configurable {
  @JsonProperty("name")
  private String mpackName;
  @JsonProperty("version")
  private String mpackVersion;
  @JsonProperty("url")
  private String url;

  private Stack stack;
  private Configuration configuration = new Configuration();

  @JsonProperty("service_instances")
  private Collection<ServiceInstance> serviceInstances = new ArrayList<>();

  public MpackInstance(String mpackName, String mpackVersion, String url, Stack stack, Configuration configuration) {
    this.mpackName = mpackName;
    this.mpackVersion = mpackVersion;
    this.url = url;
    this.stack = stack;
    this.configuration = configuration;
  }

  public MpackInstance() { }

  public String getMpackName() {
    return mpackName;
  }

  public void setMpackName(String mpackName) {
    this.mpackName = mpackName;
  }

  public String getMpackVersion() {
    return mpackVersion;
  }

  public void setMpackVersion(String mpackVersion) {
    this.mpackVersion = mpackVersion;
  }

  @JsonIgnore
  public StackId getStackId() {
    return new StackId(getMpackName(), getMpackVersion());
  }

  @JsonIgnore
  public Stack getStack() {
    return stack;
  }

  public void setStack(Stack stack) {
    this.stack = stack;
  }

  @JsonIgnore
  public Configuration getConfiguration() {
    return configuration;
  }

  @JsonIgnore
  public void setConfiguration(Configuration configuration) {
    this.configuration = configuration;
  }

  public Collection<ServiceInstance> getServiceInstances() {
    return serviceInstances;
  }

  public void setServiceInstances(Collection<ServiceInstance> serviceInstances) {
    this.serviceInstances = serviceInstances;
    serviceInstances.forEach(si -> si.setMpackInstance(this));
  }

  public void addServiceInstance(ServiceInstance serviceInstance) {
    serviceInstances.add(serviceInstance);
    serviceInstance.setMpackInstance(this);
  }

  public String getUrl() {
    return url;
  }

  public void setUrl(String url) {
    this.url = url;
  }

  /**
   * Converts the mpack instance to a {@link BlueprintMpackInstanceEntity}
   * @param blueprintEntity the blueprint entity will be associated to
   * @return the resulting entity
   */
  public BlueprintMpackInstanceEntity toMpackInstanceEntity(BlueprintEntity blueprintEntity) {
    BlueprintMpackInstanceEntity mpackInstanceEntity = new BlueprintMpackInstanceEntity();
    mpackInstanceEntity.setBlueprint(blueprintEntity);
    setCommonProperties(mpackInstanceEntity);
    return mpackInstanceEntity;
  }

  /**
   * Converts the mpack instance to a {@link TopologyRequestMpackInstanceEntity}
   * @param topologyRequestEntity the topology request entity will be associated to
   * @return the resulting entity
   */
  public TopologyRequestMpackInstanceEntity toMpackInstanceEntity(TopologyRequestEntity topologyRequestEntity) {
    TopologyRequestMpackInstanceEntity mpackInstanceEntity = new TopologyRequestMpackInstanceEntity();
    mpackInstanceEntity.setTopologyRequest(topologyRequestEntity);
    setCommonProperties(mpackInstanceEntity);
    return mpackInstanceEntity;
  }

  /**
   * Used during conversion to {@link MpackInstanceEntity}. Sets the common properties that are shared across all
   * {@link MpackInstanceEntity} subclasses. Bidirectional relations are handled for {@link  MpackInstanceConfigEntity},
   * {@link MpackInstanceServiceEntity} and {@link MpackServiceConfigEntity} entites.
   * @param mpackInstanceEntity the entity to set the properties on
   */
  private void setCommonProperties(MpackInstanceEntity mpackInstanceEntity) {
    mpackInstanceEntity.setMpackUri(url);
    mpackInstanceEntity.setMpackName(mpackName);
    mpackInstanceEntity.setMpackVersion(mpackVersion);
    Collection<MpackInstanceConfigEntity> mpackConfigEntities =
      BlueprintImpl.toConfigEntities(configuration, MpackInstanceConfigEntity::new);
    mpackConfigEntities.forEach( configEntity -> configEntity.setMpackInstance(mpackInstanceEntity) );
    mpackInstanceEntity.setConfigurations(mpackConfigEntities);

    getServiceInstances().forEach(serviceInstance -> {
      MpackInstanceServiceEntity serviceEntity = new MpackInstanceServiceEntity();
      serviceEntity.setName(serviceInstance.getName());
      serviceEntity.setType(serviceInstance.getType());
      Collection<MpackServiceConfigEntity> serviceConfigEntities =
        BlueprintImpl.toConfigEntities(serviceInstance.getConfiguration(), MpackServiceConfigEntity::new);
      serviceConfigEntities.forEach( configEntity -> configEntity.setService(serviceEntity) );
      serviceEntity.setConfigurations(serviceConfigEntities);
      mpackInstanceEntity.getServiceInstances().add(serviceEntity);
      serviceEntity.setMpackInstance(mpackInstanceEntity);
    });
  }

  public static MpackInstance fromEntity(MpackInstanceEntity entity) {
    MpackInstance mpack = new MpackInstance();
    mpack.setUrl(entity.getMpackUri());
    mpack.setMpackName(entity.getMpackName());
    mpack.setMpackVersion(entity.getMpackVersion());
    mpack.setConfiguration(BlueprintImpl.fromConfigEntities(entity.getConfigurations()));
    for (MpackInstanceServiceEntity serviceEntity: entity.getServiceInstances()) {
      ServiceInstance serviceInstance = new ServiceInstance();
      serviceInstance.setName(serviceEntity.getName());
      serviceInstance.setType(serviceEntity.getType());
      serviceInstance.setMpackInstance(mpack);
      serviceInstance.setConfiguration(BlueprintImpl.fromConfigEntities(serviceEntity.getConfigurations()));
      mpack.getServiceInstances().add(serviceInstance);
    }
    return mpack;
  }


}
