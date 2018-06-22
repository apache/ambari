/**
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 * <p/>
 * http://www.apache.org/licenses/LICENSE-2.0
 * <p/>
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.apache.ambari.server.state;

import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;

import org.apache.ambari.server.AmbariException;
import org.apache.ambari.server.api.services.ServiceGroupKey;
import org.apache.ambari.server.controller.ServiceGroupDependencyResponse;
import org.apache.ambari.server.controller.ServiceGroupResponse;
import org.apache.ambari.server.events.ServiceGroupInstalledEvent;
import org.apache.ambari.server.events.ServiceGroupRemovedEvent;
import org.apache.ambari.server.events.publishers.AmbariEventPublisher;
import org.apache.ambari.server.orm.dao.ClusterDAO;
import org.apache.ambari.server.orm.dao.ServiceGroupDAO;
import org.apache.ambari.server.orm.dao.StackDAO;
import org.apache.ambari.server.orm.entities.ClusterEntity;
import org.apache.ambari.server.orm.entities.ServiceGroupDependencyEntity;
import org.apache.ambari.server.orm.entities.ServiceGroupEntity;
import org.apache.ambari.server.orm.entities.StackEntity;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.common.base.MoreObjects;
import com.google.inject.assistedinject.Assisted;
import com.google.inject.assistedinject.AssistedInject;
import com.google.inject.persist.Transactional;

public class ServiceGroupImpl implements ServiceGroup {

  private static final Logger LOG = LoggerFactory.getLogger(ServiceGroupImpl.class);

  private final Cluster cluster;

  private final ClusterDAO clusterDAO;
  private final ServiceGroupDAO serviceGroupDAO;
  private final ServiceFactory serviceFactory;
  private final AmbariEventPublisher eventPublisher;

  private Long mpackId;
  private Long serviceGroupId;
  private String serviceGroupName;
  private StackId stackId;
  private Set<ServiceGroupKey> serviceGroupDependencies;

  @AssistedInject
  public ServiceGroupImpl(@Assisted Cluster cluster,
                          @Assisted("serviceGroupName") String serviceGroupName,
                          @Assisted("stackId") StackId stackId,
                          @Assisted("serviceGroupDependencies") Set<ServiceGroupKey> serviceGroupDependencies,
                          ClusterDAO clusterDAO,
                          StackDAO stackDAO,
                          ServiceGroupDAO serviceGroupDAO,
                          ServiceFactory serviceFactory,
                          AmbariEventPublisher eventPublisher,
                          Clusters clusters) throws AmbariException {

    this.cluster = cluster;
    this.clusterDAO = clusterDAO;
    this.serviceGroupDAO = serviceGroupDAO;
    this.serviceFactory = serviceFactory;
    this.eventPublisher = eventPublisher;

    this.serviceGroupName = serviceGroupName;
    this.stackId = stackId;

    ServiceGroupEntity serviceGroupEntity = new ServiceGroupEntity();
    serviceGroupEntity.setClusterId(cluster.getClusterId());
    serviceGroupEntity.setServiceGroupId(serviceGroupId);
    serviceGroupEntity.setServiceGroupName(serviceGroupName);

    StackEntity stackEntity = stackDAO.find(stackId);

    mpackId = stackEntity.getMpackId();
    serviceGroupEntity.setStack(stackEntity);
    if (serviceGroupDependencies == null) {
      this.serviceGroupDependencies = new HashSet<>();
    } else {
      this.serviceGroupDependencies = serviceGroupDependencies;
    }

    persist(serviceGroupEntity);
  }

  @AssistedInject
  public ServiceGroupImpl(@Assisted Cluster cluster,
                          @Assisted ServiceGroupEntity serviceGroupEntity,
                          ClusterDAO clusterDAO,
                          StackDAO stackDAO,
                          ServiceGroupDAO serviceGroupDAO,
                          ServiceFactory serviceFactory,
                          AmbariEventPublisher eventPublisher,
                          Clusters clusters) throws AmbariException {
    this.cluster = cluster;
    this.clusterDAO = clusterDAO;
    this.serviceGroupDAO = serviceGroupDAO;
    this.serviceFactory = serviceFactory;
    this.eventPublisher = eventPublisher;

    serviceGroupId = serviceGroupEntity.getServiceGroupId();
    serviceGroupName = serviceGroupEntity.getServiceGroupName();
    StackEntity stack = serviceGroupEntity.getStack();
    mpackId = stack.getMpackId();
    stackId = new StackId(stack.getStackName(), stack.getStackVersion());
    serviceGroupDependencies = getServiceGroupDependencies(serviceGroupEntity.getServiceGroupDependencies());

  }

  @Override
  public Long getServiceGroupId() {
    return serviceGroupId;
  }

  @Override
  public String getServiceGroupName() {
    return serviceGroupName;
  }

  @Override
  public void setServiceGroupName(String serviceGroupName) {
    ServiceGroupEntity entity = getServiceGroupEntity();
    entity.setServiceGroupName(serviceGroupName);
    serviceGroupDAO.merge(entity);
    this.serviceGroupName = serviceGroupName;
  }

  @Override
  public long getClusterId() {
    return cluster.getClusterId();
  }

  @Override
  public StackId getStackId() {
    return stackId;
  }

  @Override
  public Set<ServiceGroupKey> getServiceGroupDependencies() {
    return serviceGroupDependencies;
  }

  @Override
  public void setServiceGroupDependencies(Set<ServiceGroupKey> serviceGroupDependencies) {
    this.serviceGroupDependencies = serviceGroupDependencies;
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public Long getMpackId() {
    ServiceGroupEntity serviceGroupEntity = getServiceGroupEntity();
    return serviceGroupEntity.getStack().getMpackId();
  }

  @Override
  public ServiceGroupResponse convertToResponse() {
    ServiceGroupResponse r = new ServiceGroupResponse(cluster.getClusterId(),
        cluster.getClusterName(), mpackId, stackId, getServiceGroupId(), getServiceGroupName());

    return r;
  }

  @Override
  public Set<ServiceGroupDependencyResponse> getServiceGroupDependencyResponses() {
    Set<ServiceGroupDependencyResponse> responses = new HashSet<>();
    if (getServiceGroupDependencies() != null) {
      for (ServiceGroupKey sgk : getServiceGroupDependencies()) {
        responses.add(new ServiceGroupDependencyResponse(cluster.getClusterId(), cluster.getClusterName(),
                serviceGroupId, serviceGroupName, sgk.getClusterId(), sgk.getClusterName(), sgk.getServiceGroupId(), sgk.getServiceGroupName(), sgk.getDependencyId()));
      }
    }
    return responses;
  }

  public Set<ServiceGroupKey> getServiceGroupDependencies(List<ServiceGroupDependencyEntity> serviceGroupDependencies) {
    Set<ServiceGroupKey> serviceGroupDependenciesList = new HashSet<>();
    if (serviceGroupDependencies != null) {
      for (ServiceGroupDependencyEntity sgde : serviceGroupDependencies) {
        ServiceGroupKey serviceGroupKey = new ServiceGroupKey();
        ServiceGroupEntity dependencyServiceGroupEntity = sgde.getServiceGroupDependency();
        String clusterName = "";
        Long clusterId = null;
        if (dependencyServiceGroupEntity.getClusterId() == cluster.getClusterId()) {
          clusterName = cluster.getClusterName();
          clusterId = cluster.getClusterId();
        } else {
          ClusterEntity clusterEntity = clusterDAO.findById(dependencyServiceGroupEntity.getClusterId());
          if (clusterEntity != null) {
            clusterName = clusterEntity.getClusterName();
            clusterId = clusterEntity.getClusterId();
          } else {
            LOG.error("Unable to get cluster id for service group " + dependencyServiceGroupEntity.getServiceGroupName());
          }
        }

        ServiceGroupEntity serviceGroupEntity = serviceGroupDAO.findByPK(dependencyServiceGroupEntity.getServiceGroupId());
        String serviceGroupDependencyName = "";
        Long serviceGroupDependencId = null;
        if (serviceGroupEntity != null) {
          serviceGroupDependencyName = serviceGroupEntity.getServiceGroupName();
          serviceGroupDependencId = serviceGroupEntity.getServiceGroupId();
        } else {
          LOG.error("Unable to get service group entity for service group " + dependencyServiceGroupEntity.getServiceGroupName());
        }

        serviceGroupKey.setServiceGroupName(serviceGroupDependencyName);
        serviceGroupKey.setServiceGroupId(serviceGroupDependencId);
        serviceGroupKey.setClusterName(clusterName);
        serviceGroupKey.setClusterId(clusterId);
        serviceGroupKey.setDependencyId(sgde.getServiceGroupDependencyId());
        serviceGroupDependenciesList.add(serviceGroupKey);
      }
    }
    return serviceGroupDependenciesList;
  }

  @Override
  public Cluster getCluster() {
    return cluster;
  }

  @Override
  public void debugDump(StringBuilder sb) {
    sb.append("ServiceGroup={ serviceGroupName=").append(getServiceGroupName())
      .append(", clusterName=").append(cluster.getClusterName()).append(", clusterId=")
      .append(cluster.getClusterId()).append(", stackId=").append(getStackId());
    sb.append("}");
  }

  /**
   * This method uses Java locks and then delegates to internal methods which
   * perform the JPA merges inside of a transaction. Because of this, a
   * transaction is not necessary before this calling this method.
   */
  private ServiceGroupEntity persist(ServiceGroupEntity serviceGroupEntity) {
    ServiceGroupEntity createdServiceGroupEntity = persistEntities(serviceGroupEntity);
    refresh();

    cluster.addServiceGroup(this);

    // publish the service group installed event
    ServiceGroupInstalledEvent event = new ServiceGroupInstalledEvent(
      getClusterId(), getServiceGroupName());
    eventPublisher.publish(event);
    return createdServiceGroupEntity;
  }

  @Transactional
  protected ServiceGroupEntity persistEntities(ServiceGroupEntity serviceGroupEntity) {
    long clusterId = cluster.getClusterId();

    ClusterEntity clusterEntity = clusterDAO.findById(clusterId);
    serviceGroupEntity.setClusterEntity(clusterEntity);
    serviceGroupDAO.create(serviceGroupEntity);
    serviceGroupId = serviceGroupEntity.getServiceGroupId();
    clusterEntity.getServiceGroupEntities().add(serviceGroupEntity);
    clusterDAO.merge(clusterEntity);
    return serviceGroupDAO.merge(serviceGroupEntity);
  }

  @Override
  @Transactional
  public void refresh() {
    ServiceGroupEntity serviceGroupEntity = serviceGroupDAO.findByPK(getServiceGroupId());
    serviceGroupDAO.refresh(serviceGroupEntity);
  }

  @Override
  public boolean canBeRemoved() {
    // TODO: Add check for services in the service group
    return true;
  }

  @Override
  @Transactional
  public void delete() throws AmbariException {
    removeEntities();
    // publish the service removed event
    ServiceGroupRemovedEvent event = new ServiceGroupRemovedEvent(getClusterId(), getServiceGroupName());
    eventPublisher.publish(event);
  }

  @Override
  public ServiceGroupEntity addServiceGroupDependency(Long dependencyServiceGroupId) throws AmbariException {
    ServiceGroupEntity serviceGroupEntity = serviceGroupDAO.findByPK(getServiceGroupId());

    ServiceGroupEntity dependencyServiceGroupEntity = serviceGroupDAO.findByPK(dependencyServiceGroupId);

    ServiceGroupDependencyEntity newDependency = new ServiceGroupDependencyEntity();
    newDependency.setServiceGroup(serviceGroupEntity);
    newDependency.setServiceGroupClusterId(serviceGroupEntity.getClusterId());
    newDependency.setDependentServiceGroupClusterId(dependencyServiceGroupEntity.getClusterId());
    newDependency.setServiceGroupDependency(dependencyServiceGroupEntity);
    createServiceGroupDependency(newDependency);


    serviceGroupEntity.getServiceGroupDependencies().add(newDependency);
    serviceGroupEntity = serviceGroupDAO.merge(serviceGroupEntity);

    return serviceGroupEntity;
  }

  @Transactional
  public void createServiceGroupDependency(ServiceGroupDependencyEntity serviceGroupDependencyEntity) {
    serviceGroupDAO.createServiceGroupDependency(serviceGroupDependencyEntity);
  }

  @Override
  public ServiceGroupEntity deleteServiceGroupDependency(Long dependencyServiceGroupId) throws AmbariException {
    ServiceGroupEntity serviceGroupEntity = serviceGroupDAO.findByPK(getServiceGroupId());
    ServiceGroupDependencyEntity dependencyToRemove = null;
    for (ServiceGroupDependencyEntity dependency : serviceGroupEntity.getServiceGroupDependencies()) {
      if (dependency.getServiceGroupDependency().getServiceGroupId() == dependencyServiceGroupId) {
        dependencyToRemove = dependency;
        break;
      }
    }

    return removeServcieGroupDependencyEntity(serviceGroupEntity, dependencyToRemove);
  }

  @Transactional
  protected ServiceGroupEntity removeServcieGroupDependencyEntity(ServiceGroupEntity serviceGroupEntity,
                                                                  ServiceGroupDependencyEntity dependencyToRemove) {
    serviceGroupEntity.getServiceGroupDependencies().remove(dependencyToRemove);
    serviceGroupDAO.removeServiceGroupDependency(dependencyToRemove);
    serviceGroupEntity = serviceGroupDAO.merge(serviceGroupEntity);
    return serviceGroupEntity;
  }

  @Transactional
  protected void removeEntities() throws AmbariException {
    serviceGroupDAO.removeByPK(getServiceGroupId());
  }

  // Refresh the cached reference on setters
  private ServiceGroupEntity getServiceGroupEntity() {
    return serviceGroupDAO.findByPK(getServiceGroupId());
  }


  /**
   * {@inheritDoc}
   */
  @Override
  public void setStack(StackEntity stackEntity) {
    ServiceGroupEntity serviceGroupEntity = getServiceGroupEntity();
    serviceGroupEntity.setStack(stackEntity);
    serviceGroupEntity = serviceGroupDAO.merge(serviceGroupEntity);
    stackId = new StackId(stackEntity.getStackName(), stackEntity.getStackVersion());
  }

  @Override
  public Collection<Service> getServices() throws AmbariException {

    Cluster cluster = getCluster();
    ServiceGroupEntity serviceGroup = getServiceGroupEntity();

    // !!! not sure how performant this is going to be.  possible optimization point.
    return serviceGroup.getClusterServices().stream().map(entity -> {
      return serviceFactory.createExisting(cluster, this, entity);
    }).collect(Collectors.toList());
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public int hashCode() {
    return Objects.hash(mpackId, serviceGroupName);
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public boolean equals(Object object) {
    if (null == object) {
      return false;
    }

    if (this == object) {
      return true;
    }

    if (object.getClass() != getClass()) {
      return false;
    }

    ServiceGroupImpl that = (ServiceGroupImpl) object;

    return Objects.equals(mpackId, that.mpackId)
        && Objects.equals(serviceGroupName, that.serviceGroupName);
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public String toString() {
    return MoreObjects.toStringHelper(this)
        .add("mpackId", mpackId)
        .add("serviceGroupName", serviceGroupName).toString();
  }

}
