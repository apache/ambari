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
import java.util.Objects;

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
import com.google.common.base.MoreObjects;

public class MpackInstance implements Configurable {
  @JsonProperty("name")
  private String mpackName;
  @JsonProperty("version")
  private String mpackVersion;
  @JsonProperty("url")
  private String url;

  @JsonProperty("type")
  private String mpackType;

  private Configuration configuration = new Configuration();

  @JsonProperty("service_instances")
  private Collection<ServiceInstance> serviceInstances = new ArrayList<>();

  public MpackInstance(StackId stackId) {
    this(null, stackId.getStackName(), stackId.getStackVersion(), null, Configuration.createEmpty());
  }

  public MpackInstance(String mpackName, String mpackType, String mpackVersion, String url, Configuration configuration) {
    this.mpackName = mpackName;
    this.mpackType = mpackType;
    this.mpackVersion = mpackVersion;
    this.url = url;
    this.configuration = configuration;
  }

  public MpackInstance(String mpackName, String mpackType, String mpackVersion, Collection<ServiceInstance> serviceInstances) {
    this.mpackName = mpackName;
    this.mpackType = mpackType;
    this.mpackVersion = mpackVersion;
    this.serviceInstances = serviceInstances;
  }

  public MpackInstance() { }

  public String getMpackName() {
    return mpackName != null ? mpackName : mpackType;
  }

  public String getMpackType() {
    return mpackType != null ? mpackType : mpackName;
  }

  public String getMpackVersion() {
    return mpackVersion;
  }

  @JsonIgnore
  public StackId getStackId() {
    return new StackId(getMpackType(), getMpackVersion());
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

  public Key getKey() {
    return new Key(this);
  }

  /**
   * Represents the 'business key' of (all properties that uniquely identifies) an mpack instance.
   */
  public static class Key {
    public final String name;
    public final String type;
    public final String version;

    Key(MpackInstance instance) {
      this(instance.getMpackName(), instance.getMpackType(), instance.getMpackVersion());
    }

    Key(String name, String type, String version) {
      this.name = name;
      this.type = type;
      this.version = version;
    }

    @Override
    public boolean equals(Object o) {
      if (this == o) return true;
      if (o == null || getClass() != o.getClass()) return false;
      Key key = (Key) o;
      return Objects.equals(name, key.name) &&
        Objects.equals(type, key.type) &&
        Objects.equals(version, key.version);
    }

    @Override
    public int hashCode() {
      return Objects.hash(name, type, version);
    }

    @Override
    public String toString() {
      return MoreObjects.toStringHelper(this)
        .add("name", name)
        .add("type", type)
        .add("version", version)
        .toString();
    }
  }

  /**
   * Used during conversion to {@link MpackInstanceEntity}. Sets the common properties that are shared across all
   * {@link MpackInstanceEntity} subclasses. Bidirectional relations are handled for {@link  MpackInstanceConfigEntity},
   * {@link MpackInstanceServiceEntity} and {@link MpackServiceConfigEntity} entites.
   * @param mpackInstanceEntity the entity to set the properties on
   */
  private void setCommonProperties(MpackInstanceEntity mpackInstanceEntity) {
    mpackInstanceEntity.setMpackUri(url);
    mpackInstanceEntity.setMpackName(getMpackName());
    mpackInstanceEntity.setMpackType(getMpackType());
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
    MpackInstance mpack = new MpackInstance(entity.getMpackName(), entity.getMpackType(), entity.getMpackVersion(), entity.getMpackUri(), BlueprintImpl.fromConfigEntities(entity.getConfigurations()));
    for (MpackInstanceServiceEntity serviceEntity : entity.getServiceInstances()) {
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
