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
import org.apache.ambari.server.orm.entities.BlueprintMpackConfigEntity;
import org.apache.ambari.server.orm.entities.BlueprintMpackInstanceEntity;
import org.apache.ambari.server.orm.entities.BlueprintServiceConfigEntity;
import org.apache.ambari.server.orm.entities.BlueprintServiceEntity;
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

  public BlueprintMpackInstanceEntity toEntity() {
    BlueprintMpackInstanceEntity mpackEntity = new BlueprintMpackInstanceEntity();
    mpackEntity.setMpackUri(url);
    mpackEntity.setMpackName(mpackName);
    mpackEntity.setMpackVersion(mpackVersion);
    Collection<BlueprintMpackConfigEntity> mpackConfigEntities =
      BlueprintImpl.toConfigEntities(configuration, BlueprintMpackConfigEntity::new);
    mpackConfigEntities.forEach( configEntity -> configEntity.setMpackInstance(mpackEntity) );
    mpackEntity.setConfigurations(mpackConfigEntities);

    getServiceInstances().forEach(serviceInstance -> {
      BlueprintServiceEntity serviceEntity = new BlueprintServiceEntity();
      serviceEntity.setName(serviceInstance.getName());
      serviceEntity.setType(serviceInstance.getType());
      Collection<BlueprintServiceConfigEntity> serviceConfigEntities =
        BlueprintImpl.toConfigEntities(serviceInstance.getConfiguration(), BlueprintServiceConfigEntity::new);
      serviceConfigEntities.forEach( configEntity -> configEntity.setService(serviceEntity) );
      serviceEntity.setConfigurations(serviceConfigEntities);
      mpackEntity.getServiceInstances().add(serviceEntity);
      serviceEntity.setMpackInstance(mpackEntity);
    });
    return mpackEntity;
  }

  public static MpackInstance fromEntity(BlueprintMpackInstanceEntity entity) {
    MpackInstance mpack = new MpackInstance();
    mpack.setUrl(entity.getMpackUri());
    mpack.setMpackName(entity.getMpackName());
    mpack.setMpackVersion(entity.getMpackVersion());
    mpack.setConfiguration(BlueprintImpl.fromConfigEntities(entity.getConfigurations()));
    for (BlueprintServiceEntity serviceEntity: entity.getServiceInstances()) {
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
